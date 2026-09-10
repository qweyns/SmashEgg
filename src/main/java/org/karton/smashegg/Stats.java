package org.karton.smashegg;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Counters kept in memory and mirrored to stats.yml so they survive a restart. */
final class Stats {
    static final List<String> KEYS = List.of("used", "broken", "failed", "denied", "succeeded");
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    Stats() {
        KEYS.forEach(key -> counters.put(key, new AtomicLong()));
    }

    void recordUsed() {
        counters.get("used").incrementAndGet();
    }

    /** Maps an effect key to its counter; keys without a counter are ignored. */
    void recordEffect(String effect) {
        String counter = switch (effect) {
            case "egg-break" -> "broken";
            case "ground-failure" -> "failed";
            case "denied" -> "denied";
            case "success" -> "succeeded";
            default -> null;
        };
        if (counter != null) counters.get(counter).incrementAndGet();
    }

    long get(String key) {
        AtomicLong counter = counters.get(key);
        return counter == null ? 0L : counter.get();
    }

    void reset() {
        counters.values().forEach(counter -> counter.set(0L));
    }

    /** Values for the {used}/{broken}/{failed}/{denied}/{succeeded} placeholders. */
    Map<String, String> placeholders() {
        Map<String, String> values = new HashMap<>();
        KEYS.forEach(key -> values.put(key, Long.toString(get(key))));
        return values;
    }

    void load(File file, Logger logger) {
        if (!file.isFile()) return;
        try {
            YamlConfiguration saved = new YamlConfiguration();
            saved.load(file);
            for (String key : KEYS) {
                long value = saved.getLong("stats." + key, 0L);
                if (value > 0) counters.get(key).set(value);
            }
        } catch (IOException | InvalidConfigurationException e) {
            logger.warning("Cannot read stats.yml, starting from zero: " + e.getMessage());
        }
    }

    void save(File file, Logger logger) {
        YamlConfiguration saved = new YamlConfiguration();
        for (String key : KEYS) saved.set("stats." + key, get(key));
        try {
            saved.save(file);
        } catch (IOException e) {
            logger.warning("Cannot save stats.yml: " + e.getMessage());
        }
    }
}
