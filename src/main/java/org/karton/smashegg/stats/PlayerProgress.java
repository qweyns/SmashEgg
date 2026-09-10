package org.karton.smashegg.stats;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Per-player pity and newbie-grace counters. Kept in memory and flushed when dirty so a click
 * never waits on disk.
 */
public final class PlayerProgress {
    private final ConcurrentHashMap<String, Integer> pity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> grace = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    public int pity(UUID player, String entity, boolean perEntity) {
        return pity.getOrDefault(key(player, entity, perEntity), 0);
    }

    public boolean isReady(UUID player, String entity, boolean perEntity, int after) {
        return pity(player, entity, perEntity) >= after;
    }

    public void recordFail(UUID player, String entity, boolean perEntity) {
        pity.merge(key(player, entity, perEntity), 1, Integer::sum);
        dirty = true;
    }

    public void recordSuccess(UUID player, String entity, boolean perEntity) {
        String key = key(player, entity, perEntity);
        if (pity.remove(key) != null) dirty = true;
    }

    public int graceUsed(UUID player) {
        return grace.getOrDefault(player, 0);
    }

    public boolean hasGrace(UUID player, int first) {
        return graceUsed(player) < first;
    }

    public void consumeGrace(UUID player) {
        grace.merge(player, 1, Integer::sum);
        dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public void load(File file, Logger logger) {
        if (!file.isFile()) return;
        try {
            YamlConfiguration saved = new YamlConfiguration();
            saved.load(file);
            readInts(saved.get("pity"), pity);
            Object graceNode = saved.get("grace");
            if (graceNode instanceof ConfigurationSection section) {
                for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                    if (entry.getValue() instanceof Number number && number.intValue() > 0) {
                        try {
                            grace.put(UUID.fromString(entry.getKey()), number.intValue());
                        } catch (IllegalArgumentException ignored) {
                            logger.warning("progress.yml: skip malformed grace id " + entry.getKey());
                        }
                    }
                }
            }
            dirty = false;
        } catch (IOException | InvalidConfigurationException e) {
            logger.warning("Cannot read " + file.getName() + ", starting empty: " + e.getMessage());
        }
    }

    public void save(File file, Logger logger) {
        YamlConfiguration saved = new YamlConfiguration();
        pity.forEach((key, value) -> {
            if (value > 0) saved.set("pity." + key, value);
        });
        grace.forEach((id, value) -> {
            if (value > 0) saved.set("grace." + id, value);
        });
        try {
            saved.save(file);
            dirty = false;
        } catch (IOException e) {
            logger.warning("Cannot save " + file.getName() + ": " + e.getMessage());
        }
    }

    private static void readInts(Object node, Map<String, Integer> into) {
        if (!(node instanceof ConfigurationSection section)) return;
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            if (entry.getValue() instanceof Number number && number.intValue() > 0) {
                into.put(entry.getKey(), number.intValue());
            }
        }
    }

    private static String key(UUID player, String entity, boolean perEntity) {
        return perEntity ? player + "/" + entity : player.toString();
    }
}
