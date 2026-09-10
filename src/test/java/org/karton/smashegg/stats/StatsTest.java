package org.karton.smashegg.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatsTest {
    private static final Logger LOGGER = Logger.getAnonymousLogger();

    @Test
    void countsEachEffectUnderItsOwnCounter() {
        Stats stats = new Stats();
        stats.recordUsed();
        stats.recordUsed();
        stats.recordEffect("egg-break");
        stats.recordEffect("ground-failure");
        stats.recordEffect("denied");
        stats.recordEffect("success");
        stats.recordEffect("no-permission"); // has no counter of its own
        assertEquals(2, stats.get("used"));
        assertEquals(1, stats.get("broken"));
        assertEquals(1, stats.get("failed"));
        assertEquals(1, stats.get("denied"));
        assertEquals(1, stats.get("succeeded"));
        assertEquals(0, stats.get("unknown-counter"));
    }

    @Test
    void exposesEveryKeyAsPlaceholder() {
        Map<String, String> values = new Stats().placeholders();
        assertEquals(Stats.DEFAULT_KEYS.size(), values.size());
        for (String key : Stats.DEFAULT_KEYS) assertEquals("0", values.get(key), key);
    }

    @Test
    void resetClearsAllCounters() {
        Stats stats = new Stats();
        stats.recordUsed();
        stats.recordEffect("egg-break");
        stats.reset();
        for (String key : Stats.DEFAULT_KEYS) assertEquals(0, stats.get(key), key);
    }

    @Test
    void survivesARoundTripThroughStatsYaml(@TempDir java.nio.file.Path directory) {
        File file = directory.resolve("stats.yml").toFile();
        Stats stats = new Stats();
        stats.recordUsed();
        stats.recordEffect("egg-break");
        stats.recordEffect("denied");
        stats.save(file, LOGGER);
        assertTrue(file.isFile());

        Stats restored = new Stats();
        restored.load(file, LOGGER);
        assertEquals(1, restored.get("used"));
        assertEquals(1, restored.get("broken"));
        assertEquals(1, restored.get("denied"));
        assertEquals(0, restored.get("failed"));
    }

    @Test
    void missingFileStartsFromZero(@TempDir java.nio.file.Path directory) {
        Stats stats = new Stats();
        stats.load(directory.resolve("absent.yml").toFile(), LOGGER);
        for (String key : Stats.DEFAULT_KEYS) assertEquals(0, stats.get(key), key);
    }

    @Test
    void applyAddsCustomCountersAndRemapsEffects() {
        Stats stats = new Stats();
        stats.apply(new Stats.StatsMapping("processed",
                List.of("processed", "smashed"),
                Map.of("egg-break", "smashed", "denied", "blocked")));
        stats.recordUsed();
        stats.recordEffect("egg-break");
        stats.recordEffect("denied");
        stats.recordEffect("success"); // no longer mapped
        assertEquals(1, stats.get("processed"));
        assertEquals(1, stats.get("smashed"));
        assertEquals(1, stats.get("blocked"));
        assertEquals(0, stats.get("succeeded"));
        assertTrue(stats.placeholders().containsKey("blocked"));
        assertEquals("1", stats.placeholders().get("processed"));
    }
}
