package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.karton.smashegg.config.PluginSettings;

class ReloadTest {
    @TempDir Path directory;

    private SmashEgg plugin() {
        SmashEgg plugin = mock(SmashEgg.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getResource("config.yml")).thenAnswer(call -> getClass().getResourceAsStream("/config.yml"));
        when(plugin.getResource("lang/ru_RU.yml"))
                .thenAnswer(call -> getClass().getResourceAsStream("/lang/ru_RU.yml"));
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.reloadSettings()).thenCallRealMethod();
        when(plugin.settings()).thenCallRealMethod();
        return plugin;
    }

    @Test
    void reloadIsAtomicForInvalidYamlAndInvalidValues() throws Exception {
        SmashEgg plugin = plugin();
        Path file = directory.resolve("config.yml");

        Files.writeString(file, "settings:\n  ground-spawn-chance: 42\n");
        assertTrue(plugin.reloadSettings());
        PluginSettings original = plugin.settings();
        assertEquals(42, original.rules().groundChance());
        assertTrue(original.messages().get("egg-break").enabled());

        Files.writeString(file, "settings: [unterminated\n");
        assertFalse(plugin.reloadSettings());
        assertSame(original, plugin.settings());

        Files.writeString(file, "settings:\n  ground-spawn-chance: 101\n");
        assertFalse(plugin.reloadSettings());
        assertSame(original, plugin.settings());

        Files.writeString(file, "settings:\n  ground-spawn-chance: 100\n");
        assertTrue(plugin.reloadSettings());
        assertEquals(100, plugin.settings().rules().groundChance());
        assertNotSame(original, plugin.settings());
        assertEquals("settings:\n  ground-spawn-chance: 100\n", Files.readString(file));
    }

    @Test
    void missingFileDoesNotReplaceWorkingSettings() {
        SmashEgg plugin = plugin();
        assertFalse(plugin.reloadSettings());
    }

    @Test
    void invalidLanguageNameIsRejectedInsteadOfBeingUsedInAPath() throws Exception {
        SmashEgg plugin = plugin();
        Files.writeString(directory.resolve("config.yml"), "settings:\n  language: \"../../evil\"\n");
        assertFalse(plugin.reloadSettings());
    }

    @Test
    void unknownLanguageFallsBackToTheBundledDefault() throws Exception {
        SmashEgg plugin = plugin();
        Files.writeString(directory.resolve("config.yml"), "settings:\n  language: xx_XX\n");
        assertTrue(plugin.reloadSettings());
        assertTrue(plugin.settings().messages().get("egg-break").enabled());
    }

    @Test
    void customLangDirectoryIsReadFromTheDataFolder() throws Exception {
        SmashEgg plugin = plugin();
        Path langDirectory = directory.resolve("i18n");
        Files.createDirectories(langDirectory);
        Files.writeString(langDirectory.resolve("ru_RU.yml"),
                "messages:\n  egg-break: \"<red>из i18n\"\n");
        Files.writeString(directory.resolve("config.yml"),
                "files:\n  lang-directory: i18n\nsettings:\n  language: ru_RU\n");
        assertTrue(plugin.reloadSettings());
        assertEquals("i18n", plugin.settings().langDirectory());
        assertEquals("<red>из i18n", plugin.settings().messages().get("egg-break").text());
    }

    @Test
    void languageFileFromTheDataFolderWinsOverTheBundledCopy() throws Exception {
        SmashEgg plugin = plugin();
        Path langDirectory = directory.resolve("lang");
        Files.createDirectories(langDirectory);
        Files.writeString(langDirectory.resolve("ru_RU.yml"),
                "messages:\n  egg-break: \"<red>свой перевод\"\n");
        Files.writeString(directory.resolve("config.yml"), "settings:\n  ground-spawn-chance: 70\n");
        assertTrue(plugin.reloadSettings());
        assertEquals("<red>свой перевод", plugin.settings().messages().get("egg-break").text());
    }
}
