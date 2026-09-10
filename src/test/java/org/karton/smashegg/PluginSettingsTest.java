package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PluginSettingsTest {
    @Test
    void loadsDefaults() {
        PluginSettings settings = TestSupport.settings();
        assertTrue(settings.breakOnSpawner());
        assertEquals(30, settings.breakChance());
        assertEquals(70, settings.groundChance());
        assertFalse(settings.affectCreative());
        assertTrue(settings.blacklist().contains("WARDEN"));
        assertEquals(Key.key("block.glass.break"), settings.sounds().get("egg-break").name());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101, Integer.MAX_VALUE})
    void rejectsInvalidPercentages(int value) {
        for (String path : List.of("settings.egg-break-chance", "settings.ground-spawn-chance")) {
            YamlConfiguration config = TestSupport.config();
            config.set(path, value);
            assertTrue(assertThrows(IllegalArgumentException.class,
                    () -> PluginSettings.load(config, ignored -> {})).getMessage().contains(path));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void acceptsPercentageBoundaries(int value) {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.egg-break-chance", value);
        config.set("settings.ground-spawn-chance", value);
        PluginSettings settings = PluginSettings.load(config, ignored -> {});
        assertEquals(value, settings.breakChance());
        assertEquals(value, settings.groundChance());
    }

    @Test
    void rejectsWrongTypesInsteadOfSilentlyUsingDefaults() {
        for (Object[] entry : List.of(
                new Object[]{"settings", "not-a-section"},
                new Object[]{"sounds", 42},
                new Object[]{"messages", List.of("not-a-section")},
                new Object[]{"settings.egg-break-chance", "30"},
                new Object[]{"settings.ground-spawn-chance", 12.5},
                new Object[]{"settings.affect-creative", "false"},
                new Object[]{"settings.black-entities", "WARDEN"},
                new Object[]{"settings.black-entities", List.of(123)},
                new Object[]{"settings.black-entities", List.of("not an entity")},
                new Object[]{"messages.denied", 123},
                new Object[]{"sounds.failure", "NOT_A_SOUND"})) {
            YamlConfiguration config = TestSupport.config();
            config.set((String) entry[0], entry[1]);
            assertThrows(IllegalArgumentException.class, () -> PluginSettings.load(config, ignored -> {}),
                    entry[0].toString());
        }
    }

    @Test
    void normalizesBlacklistAndPreservesFutureNamesWithWarning() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.black-entities", List.of(" mushroom_cow ", "snowman", "FUTURE_MOB"));
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = PluginSettings.load(config, warnings::add);
        assertEquals(3, settings.blacklist().size());
        assertTrue(settings.blacklist().containsAll(List.of("MOOSHROOM", "SNOW_GOLEM", "FUTURE_MOB")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("FUTURE_MOB")));
        assertThrows(UnsupportedOperationException.class, () -> settings.blacklist().clear());
    }

    @Test
    void supportsEmptyMessagesAndSounds() {
        YamlConfiguration config = TestSupport.config();
        config.set("sounds.success", "");
        config.set("messages.denied", "");
        PluginSettings settings = PluginSettings.load(config, ignored -> {});
        assertFalse(settings.sounds().containsKey("success"));
        assertEquals(Component.empty(), settings.messages().get("denied"));
    }

    @Test
    void oldConfigInheritsNewKeysWithoutOverwritingUserChoices() {
        YamlConfiguration old = new YamlConfiguration();
        old.set("settings.ground-spawn-chance", 42);
        old.setDefaults(TestSupport.config());
        PluginSettings settings = PluginSettings.load(old, ignored -> {});
        assertEquals(42, settings.groundChance());
        assertFalse(settings.affectCreative());
        assertNotEquals(Component.empty(), settings.messages().get("reload-failure"));
    }

    @Test
    void namesAreIndependentOfSystemLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            YamlConfiguration config = TestSupport.config();
            config.set("sounds.failure", "entity_villager_no");
            config.set("settings.black-entities", List.of("pig"));
            List<String> warnings = new ArrayList<>();
            PluginSettings settings = PluginSettings.load(config, warnings::add);
            assertEquals(Key.key("entity.villager.no"), settings.sounds().get("failure").name());
            assertTrue(settings.blacklist().contains("PIG"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void acceptsNamespacedAndCustomSoundKeys() {
        YamlConfiguration config = TestSupport.config();
        config.set("sounds.success", "entity.player.levelup");
        config.set("sounds.failure", "minecraft:entity.villager.no");
        config.set("sounds.denied", "my_pack:custom_sound");
        PluginSettings settings = PluginSettings.load(config, ignored -> {});
        assertEquals(Key.key("entity.player.levelup"), settings.sounds().get("success").name());
        assertEquals(Key.key("minecraft", "entity.villager.no"), settings.sounds().get("failure").name());
        assertEquals(Key.key("my_pack", "custom_sound"), settings.sounds().get("denied").name());
    }

    @Test
    void keepsLegacyBukkitSoundNamesWorkingWithWarning() {
        YamlConfiguration config = TestSupport.config();
        config.set("sounds.egg-break", "BLOCK_GLASS_BREAK");
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = PluginSettings.load(config, warnings::add);
        assertEquals(Key.key("block.glass.break"), settings.sounds().get("egg-break").name());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("BLOCK_GLASS_BREAK")), warnings::toString);
    }
}
