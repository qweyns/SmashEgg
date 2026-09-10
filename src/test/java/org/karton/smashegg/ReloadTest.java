package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.karton.smashegg.SmashEgg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReloadTest {
    @TempDir Path directory;

    @Test
    void reloadIsAtomicForInvalidYamlAndInvalidValues() throws Exception {
        SmashEgg plugin = mock(SmashEgg.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getResource("config.yml")).thenAnswer(call -> getClass().getResourceAsStream("/config.yml"));
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.reloadSettings()).thenCallRealMethod();
        when(plugin.settings()).thenCallRealMethod();
        Path file = directory.resolve("config.yml");

        Files.writeString(file, "settings:\n  ground-spawn-chance: 42\n");
        assertTrue(plugin.reloadSettings());
        PluginSettings original = plugin.settings();
        assertEquals(42, original.groundChance());

        Files.writeString(file, "settings: [unterminated\n");
        assertFalse(plugin.reloadSettings());
        assertSame(original, plugin.settings());

        Files.writeString(file, "settings:\n  ground-spawn-chance: 101\n");
        assertFalse(plugin.reloadSettings());
        assertSame(original, plugin.settings());

        Files.writeString(file, "settings:\n  ground-spawn-chance: 100\n");
        assertTrue(plugin.reloadSettings());
        assertEquals(100, plugin.settings().groundChance());
        assertNotSame(original, plugin.settings());
        assertEquals("settings:\n  ground-spawn-chance: 100\n", Files.readString(file));
    }

    @Test
    void missingFileDoesNotReplaceWorkingSettings() {
        SmashEgg plugin = mock(SmashEgg.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getResource("config.yml")).thenAnswer(call -> getClass().getResourceAsStream("/config.yml"));
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.reloadSettings()).thenCallRealMethod();
        assertFalse(plugin.reloadSettings());
    }
}
