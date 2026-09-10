package org.karton.smashegg.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerProgressTest {
    private static final Logger LOGGER = Logger.getAnonymousLogger();

    @Test
    void pityCountsUntilResetAndSurvivesYaml(@TempDir java.nio.file.Path directory) {
        UUID player = UUID.randomUUID();
        PlayerProgress progress = new PlayerProgress();
        assertFalse(progress.isReady(player, "ZOMBIE", false, 2));
        progress.recordFail(player, "ZOMBIE", false);
        assertFalse(progress.isReady(player, "ZOMBIE", false, 2));
        progress.recordFail(player, "ZOMBIE", false);
        assertTrue(progress.isReady(player, "ZOMBIE", false, 2));
        progress.recordSuccess(player, "ZOMBIE", false);
        assertFalse(progress.isReady(player, "ZOMBIE", false, 2));

        progress.recordFail(player, "ZOMBIE", true);
        progress.recordFail(player, "PIG", true);
        assertTrue(progress.isReady(player, "ZOMBIE", true, 1));
        assertFalse(progress.isReady(player, "PIG", true, 2));

        File file = directory.resolve("progress.yml").toFile();
        progress.consumeGrace(player);
        progress.save(file, LOGGER);
        PlayerProgress restored = new PlayerProgress();
        restored.load(file, LOGGER);
        assertEquals(1, restored.graceUsed(player));
        assertEquals(1, restored.pity(player, "ZOMBIE", true));
        assertEquals(1, restored.pity(player, "PIG", true));
    }

    @Test
    void graceExpiresAfterTheConfiguredFirstClicks() {
        UUID player = UUID.randomUUID();
        PlayerProgress progress = new PlayerProgress();
        assertTrue(progress.hasGrace(player, 2));
        progress.consumeGrace(player);
        assertTrue(progress.hasGrace(player, 2));
        progress.consumeGrace(player);
        assertFalse(progress.hasGrace(player, 2));
    }

    @Test
    void missingFileStartsEmpty(@TempDir java.nio.file.Path directory) {
        PlayerProgress progress = new PlayerProgress();
        progress.load(directory.resolve("absent.yml").toFile(), LOGGER);
        assertEquals(0, progress.pity(UUID.randomUUID(), "ZOMBIE", false));
        assertFalse(progress.dirty());
    }
}
