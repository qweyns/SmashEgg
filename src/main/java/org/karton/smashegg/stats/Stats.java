package org.karton.smashegg.stats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Counters kept in memory and mirrored to stats.yml so they survive a restart. */
public final class Stats {
    public static final List<String> DEFAULT_KEYS = List.of("used", "broken", "failed", "denied", "succeeded");
    public static final Map<String, String> DEFAULT_EFFECTS = Map.of(
            "egg-break", "broken",
            "ground-failure", "failed",
            "denied", "denied",
            "success", "succeeded");
    public static final String DEFAULT_USED = "used";

    private volatile List<String> keys = DEFAULT_KEYS;
    private volatile Map<String, String> effects = DEFAULT_EFFECTS;
    private volatile String usedCounter = DEFAULT_USED;
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public Stats() {
        apply(StatsMapping.DEFAULT);
    }

    public void apply(StatsMapping mapping) {
        this.keys = List.copyOf(mapping.counters());
        this.effects = Map.copyOf(mapping.effects());
        this.usedCounter = mapping.used();
        for (String key : keys) counters.putIfAbsent(key, new AtomicLong());
    }

    public void recordUsed() {
        increment(usedCounter);
    }

    /** Maps an effect key to its counter; keys without a counter are ignored. */
    public void recordEffect(String effect) {
        String counter = effects.get(effect);
        if (counter != null) increment(counter);
    }

    private void increment(String key) {
        counters.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
    }

    public long get(String key) {
        AtomicLong counter = counters.get(key);
        return counter == null ? 0L : counter.get();
    }

    public void reset() {
        for (String key : keys) {
            AtomicLong counter = counters.get(key);
            if (counter != null) counter.set(0L);
        }
    }

    /** Values for every configured counter, used as {@code {used}}/{@code {broken}}/... placeholders. */
    public Map<String, String> placeholders() {
        Map<String, String> values = new HashMap<>();
        keys.forEach(key -> values.put(key, Long.toString(get(key))));
        return values;
    }

    public void load(File file, Logger logger) {
        if (!file.isFile()) return;
        try {
            YamlConfiguration saved = new YamlConfiguration();
            saved.load(file);
            for (String key : keys) {
                long value = saved.getLong("stats." + key, 0L);
                if (value > 0) counters.computeIfAbsent(key, ignored -> new AtomicLong()).set(value);
            }
        } catch (IOException | InvalidConfigurationException e) {
            logger.warning("Cannot read " + file.getName() + ", starting from zero: " + e.getMessage());
        }
    }

    public void save(File file, Logger logger) {
        YamlConfiguration saved = new YamlConfiguration();
        for (String key : keys) saved.set("stats." + key, get(key));
        try {
            saved.save(file);
        } catch (IOException e) {
            logger.warning("Cannot save " + file.getName() + ": " + e.getMessage());
        }
    }

    /** Effect → counter mapping plus the list of counters that are persisted and shown. */
    public record StatsMapping(String used, List<String> counters, Map<String, String> effects) {
        public static final StatsMapping DEFAULT = new StatsMapping(DEFAULT_USED, DEFAULT_KEYS, DEFAULT_EFFECTS);

        public StatsMapping {
            used = used == null || used.isEmpty() ? DEFAULT_USED : used;
            Set<String> names = new LinkedHashSet<>();
            names.add(used);
            if (counters != null) names.addAll(counters);
            if (effects != null) names.addAll(effects.values());
            counters = List.copyOf(names);
            effects = effects == null ? Map.of() : Map.copyOf(effects);
        }
    }
}
