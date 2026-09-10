package org.karton.smashegg.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.karton.smashegg.SmashEgg;
import org.karton.smashegg.TestSupport;
import org.karton.smashegg.effect.MessageOutput;
import org.karton.smashegg.effect.MessageSpec;
import org.karton.smashegg.effect.ParticleSpec;

class PluginSettingsTest {

    private static PluginSettings load(YamlConfiguration config) {
        return PluginSettings.load(config, TestSupport.lang(), ignored -> {});
    }

    private static PluginSettings load(YamlConfiguration config, List<String> warnings) {
        return PluginSettings.load(config, TestSupport.lang(), warnings::add);
    }

    @Test
    void loadsDefaults() {
        PluginSettings settings = TestSupport.settings();
        assertEquals(PluginSettings.CONFIG_VERSION, settings.configVersion());
        assertEquals("ru_RU", settings.language());
        assertTrue(settings.rules().breakOnSpawner());
        assertEquals(30, settings.rules().breakChance());
        assertEquals(70, settings.rules().groundChance());
        assertFalse(settings.rules().affectCreative());
        assertEquals(FilterMode.BLACKLIST, settings.filterMode());
        assertTrue(settings.filteredEntities().contains("WARDEN"));
        assertEquals(1, settings.cooldownTicks());
        assertEquals(FailureAction.CONSUME, settings.failureAction());
        assertFalse(settings.logEvents());
        assertTrue(settings.disabledWorlds().isEmpty());
        assertEquals(Key.key("block.glass.break"), settings.sounds().get("egg-break").name());
        assertEquals(2, settings.particles().size());
        assertEquals(MessageOutput.CHAT, settings.messages().get("egg-break").output());
        for (String key : PluginSettings.MESSAGE_KEYS) {
            assertTrue(settings.messages().get(key).enabled(), key);
        }
        assertEquals("lang", settings.langDirectory());
        assertEquals("stats.yml", settings.statsFile());
        assertEquals(1.0f, settings.soundDefaults().volume());
        assertEquals(1.0, settings.particleDefaults().offsetY());
        assertEquals("used", settings.stats().used());
        assertEquals("broken", settings.stats().effects().get("egg-break"));
        assertEquals("вкл", settings.words().spawnerOn());
        assertEquals("чёрный список", settings.words().filterBlacklist());
    }

    @Test
    void snapshotCollectionsAreImmutable() {
        PluginSettings settings = TestSupport.settings();
        assertThrows(UnsupportedOperationException.class, () -> settings.filteredEntities().clear());
        assertThrows(UnsupportedOperationException.class, () -> settings.messages().clear());
        assertThrows(UnsupportedOperationException.class, () -> settings.sounds().clear());
        assertThrows(UnsupportedOperationException.class, () -> settings.worlds().clear());
    }

    @Test
    void worldAndEntityOverridesMergeWithEntityWinning() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.worlds.world_nether.ground-spawn-chance", 100);
        config.set("settings.worlds.world_nether.egg-break-chance", 5);
        config.set("settings.entities.zombie.ground-spawn-chance", 10);
        PluginSettings settings = load(config);

        assertEquals(70, settings.rulesFor("world", "PIG").groundChance());
        assertEquals(30, settings.rulesFor("world", "PIG").breakChance());
        assertEquals(100, settings.rulesFor("world_nether", "PIG").groundChance());
        assertEquals(5, settings.rulesFor("world_nether", "PIG").breakChance());
        assertEquals(10, settings.rulesFor("world_nether", "ZOMBIE").groundChance());
        assertEquals(5, settings.rulesFor("world_nether", "ZOMBIE").breakChance());
        assertEquals(10, settings.rulesFor("world", "ZOMBIE").groundChance());
        assertEquals(30, settings.rulesFor("world", "ZOMBIE").breakChance());
        // Inherited fields keep their meaning and world lookup ignores case.
        assertTrue(settings.rulesFor("WORLD_NETHER", "PIG").breakOnSpawner());
        assertFalse(settings.rulesFor("world", "PIG").affectCreative());
    }

    @Test
    void overridesAcceptTheSameFieldsAsGlobalSettings() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.worlds.world.affect-creative", true);
        config.set("settings.worlds.world.egg-break-on-spawner", false);
        config.set("settings.entities.mushroom_cow.egg-break-chance", 100);
        PluginSettings settings = load(config);
        assertTrue(settings.worlds().containsKey("world"));
        assertTrue(settings.rulesFor("world", "PIG").affectCreative());
        assertFalse(settings.rulesFor("world", "PIG").breakOnSpawner());
        assertTrue(settings.entities().containsKey("MOOSHROOM"));
        assertEquals(100, settings.rulesFor("world", "MOOSHROOM").breakChance());
    }

    @Test
    void whitelistInvertsTheEntityList() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.black-entities", List.of("ZOMBIE"));
        config.set("settings.entity-filter", "whitelist");
        PluginSettings whitelist = load(config);
        assertFalse(whitelist.blocksEntity("ZOMBIE"));
        assertTrue(whitelist.blocksEntity("ENDER_DRAGON"));

        config.set("settings.entity-filter", "blacklist");
        PluginSettings blacklist = load(config);
        assertTrue(blacklist.blocksEntity("ZOMBIE"));
        assertFalse(blacklist.blocksEntity("ENDER_DRAGON"));
    }

    @Test
    void whitelistPrefersTheAllowedEntitiesKey() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.entity-filter", "whitelist");
        config.set("settings.allowed-entities", List.of("PIG"));
        PluginSettings whitelist = load(config);
        assertFalse(whitelist.blocksEntity("PIG"));
        assertTrue(whitelist.blocksEntity("ZOMBIE"));
        assertEquals(Set.of("PIG"), whitelist.filteredEntities());
    }

    @Test
    void blacklistIgnoresAllowedEntitiesAndUsesBlackEntities() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.allowed-entities", List.of("PIG"));
        PluginSettings blacklist = load(config);
        assertEquals(FilterMode.BLACKLIST, blacklist.filterMode());
        assertTrue(blacklist.blocksEntity("WARDEN"));
        assertFalse(blacklist.blocksEntity("PIG"));
    }

    @Test
    void disabledWorldsAreCaseInsensitive() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.disabled-worlds", List.of("World_Nether"));
        PluginSettings settings = load(config);
        assertTrue(settings.isDisabled("world_nether"));
        assertTrue(settings.isDisabled("WORLD_NETHER"));
        assertFalse(settings.isDisabled("world"));
    }

    @Test
    void soundSectionCarriesVolumePitchAndSource() {
        YamlConfiguration config = TestSupport.config();
        config.set("sounds.denied.key", "entity.enderman.teleport");
        config.set("sounds.denied.volume", 0.5);
        config.set("sounds.denied.pitch", 1.5);
        config.set("sounds.denied.source", "player");
        Sound sound = load(config).sounds().get("denied");
        assertEquals(Key.key("entity.enderman.teleport"), sound.name());
        assertEquals(0.5f, sound.volume());
        assertEquals(1.5f, sound.pitch());
        assertEquals(Sound.Source.PLAYER, sound.source());
    }

    @Test
    void renamedSoundKeyKeepsWorking() {
        YamlConfiguration config = TestSupport.config();
        config.set("sounds.ground-failure", null);
        config.set("sounds.failure", "entity.villager.no");
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = load(config, warnings);
        assertEquals(Key.key("entity.villager.no"), settings.sounds().get("ground-failure").name());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("renamed")), warnings::toString);
    }

    @Test
    void legacyBukkitSoundNamesStillResolveWithWarning() {
        YamlConfiguration config = TestSupport.config();
        config.set("sounds.egg-break", "BLOCK_GLASS_BREAK");
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = load(config, warnings);
        assertEquals(Key.key("block.glass.break"), settings.sounds().get("egg-break").name());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("BLOCK_GLASS_BREAK")), warnings::toString);
    }

    @Test
    void messageCanChangeOutputChannelAndBeDisabled() {
        YamlConfiguration config = TestSupport.config();
        config.set("messages.denied.text", "<red>нельзя");
        config.set("messages.denied.output", "actionbar");
        config.set("messages.egg-break", "");
        PluginSettings settings = load(config);
        assertEquals(MessageOutput.ACTIONBAR, settings.messages().get("denied").output());
        assertEquals("<red>нельзя", settings.messages().get("denied").text());
        assertFalse(settings.messages().get("egg-break").enabled());
    }

    @Test
    void overridingOnlyTheOutputKeepsTheTranslatedText() {
        YamlConfiguration config = TestSupport.config();
        config.set("messages.denied.output", "actionbar");
        MessageSpec denied = load(config).messages().get("denied");
        assertEquals(MessageOutput.ACTIONBAR, denied.output());
        assertEquals(TestSupport.lang().getString("messages.denied"), denied.text());
        assertTrue(denied.enabled());
    }

    @Test
    void configMessagesWinOverTheLanguageFile() {
        YamlConfiguration config = TestSupport.config();
        config.set("messages.denied", "<red>свой текст");
        assertEquals("<red>свой текст", load(config).messages().get("denied").text());
    }

    @Test
    void messageMissingFromBothSourcesIsDisabledWithWarning() {
        YamlConfiguration lang = TestSupport.lang();
        lang.set("messages.denied", null);
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = PluginSettings.load(TestSupport.config(), lang, warnings::add);
        assertFalse(settings.messages().get("denied").enabled());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("messages.denied")), warnings::toString);
    }

    @Test
    void particlesAreParsedAndCanBeDisabled() {
        PluginSettings defaults = TestSupport.settings();
        ParticleSpec spec = defaults.particles().get("egg-break");
        assertEquals("item_slime", spec.name());
        assertEquals(12, spec.count());
        assertEquals(0.3, spec.spread());
        assertEquals(0.05, spec.speed());
        assertEquals(0.0, spec.offsetX());
        assertEquals(1.0, spec.offsetY());
        assertEquals(0.0, spec.offsetZ());

        YamlConfiguration config = TestSupport.config();
        config.set("particles.egg-break", "");
        config.set("particles.ground-failure.name", "crit");
        config.set("particles.ground-failure.count", 3);
        PluginSettings settings = load(config);
        assertFalse(settings.particles().containsKey("egg-break"));
        assertEquals("crit", settings.particles().get("ground-failure").name());
        assertEquals(3, settings.particles().get("ground-failure").count());
    }

    @Test
    void unknownKeysWarnInsteadOfBeingIgnored() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.ground-spawn-chancee", 50);
        config.set("sounds.egg-break.volme", 1);
        config.set("defaults.sounds.loudness", 2);
        config.set("settings.worlds.world.ground-spawn-chancee", 50);
        config.set("nope", 1);
        List<String> warnings = new ArrayList<>();
        load(config, warnings);
        for (String expected : List.of("settings.ground-spawn-chancee", "sounds.egg-break.volme",
                "defaults.sounds.loudness", "settings.worlds.world.ground-spawn-chancee", "nope")) {
            assertTrue(warnings.stream().anyMatch(w -> w.contains(expected)), expected + " in " + warnings);
        }
    }

    @Test
    void extraLanguageMessageKeyIsLoadedWithoutWarning() {
        YamlConfiguration lang = TestSupport.lang();
        lang.set("messages.custom-tip", "<gray>tip");
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = PluginSettings.load(TestSupport.config(), lang, warnings::add);
        assertTrue(warnings.stream().noneMatch(w -> w.contains("custom-tip")), warnings::toString);
        assertEquals("<gray>tip", settings.messages().get("custom-tip").text());
    }

    @Test
    void extraMessageSoundAndParticleKeysAreLoadedWithoutWarning() {
        YamlConfiguration config = TestSupport.config();
        config.set("sounds.egg_break", "block.glass.break");
        config.set("particles.nope.name", "smoke");
        config.set("messages.eggbreak", "hello");
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = load(config, warnings);
        assertTrue(warnings.stream().noneMatch(w -> w.contains("sounds.egg_break")
                || w.contains("particles.nope") || w.contains("messages.eggbreak")), warnings::toString);
        assertEquals(Key.key("block.glass.break"), settings.sounds().get("egg_break").name());
        assertEquals("smoke", settings.particles().get("nope").name());
        assertEquals("hello", settings.messages().get("eggbreak").text());
    }

    @Test
    void defaultsApplyToPlainSoundAndParticleEntries() {
        YamlConfiguration config = TestSupport.config();
        config.set("defaults.sounds.volume", 0.25);
        config.set("defaults.sounds.pitch", 0.5);
        config.set("defaults.sounds.source", "player");
        config.set("defaults.particles.count", 4);
        config.set("defaults.particles.spread", 1.5);
        config.set("defaults.particles.speed", 0.2);
        config.set("defaults.particles.offset-y", 2.5);
        config.set("sounds.success", "entity.player.levelup");
        config.set("particles.success", "crit");
        PluginSettings settings = load(config);
        Sound success = settings.sounds().get("success");
        assertEquals(0.25f, success.volume());
        assertEquals(0.5f, success.pitch());
        assertEquals(Sound.Source.PLAYER, success.source());
        ParticleSpec spec = settings.particles().get("success");
        assertEquals("crit", spec.name());
        assertEquals(4, spec.count());
        assertEquals(1.5, spec.spread());
        assertEquals(0.2, spec.speed());
        assertEquals(2.5, spec.offsetY());
    }

    @Test
    void particleOffsetsCanBeSetPerEffect() {
        YamlConfiguration config = TestSupport.config();
        config.set("particles.egg-break.offset-x", 0.5);
        config.set("particles.egg-break.offset-y", 2.0);
        config.set("particles.egg-break.offset-z", -0.25);
        ParticleSpec spec = load(config).particles().get("egg-break");
        assertEquals(0.5, spec.offsetX());
        assertEquals(2.0, spec.offsetY());
        assertEquals(-0.25, spec.offsetZ());
    }

    @Test
    void statsMappingAcceptsExtraCountersAndEffectKeys() {
        YamlConfiguration config = TestSupport.config();
        config.set("stats.used", "processed");
        config.set("stats.counters", List.of("processed", "smashed", "blocked"));
        config.set("stats.effects.egg-break", "smashed");
        config.set("stats.effects.denied", "blocked");
        config.set("stats.effects.custom", "blocked");
        PluginSettings settings = load(config);
        assertEquals("processed", settings.stats().used());
        assertEquals("smashed", settings.stats().effects().get("egg-break"));
        assertEquals("blocked", settings.stats().effects().get("custom"));
        assertTrue(settings.stats().counters().containsAll(List.of("processed", "smashed", "blocked")));
    }

    @Test
    void placeholderWordsComeFromTheLanguageFile() {
        assertEquals("основная", TestSupport.settings().words().handMain());
        assertEquals("вторая", TestSupport.settings().words().handOff());
        PluginSettings english = PluginSettings.load(TestSupport.config(), TestSupport.langEn(), ignored -> {});
        assertEquals("main", english.words().handMain());
        assertEquals("off", english.words().handOff());
        assertEquals("yes", english.words().creativeYes());
        assertEquals("-", english.words().unset());
    }

    @Test
    void filesSectionRejectsPathTraversal() {
        YamlConfiguration config = TestSupport.config();
        config.set("files.lang-directory", "../evil");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> load(config));
        assertTrue(e.getMessage().contains("files.lang-directory"), e::getMessage);
    }

    @Test
    void rejectsConfigFromANewerVersion() {
        YamlConfiguration config = TestSupport.config();
        config.set("config-version", PluginSettings.CONFIG_VERSION + 1);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> load(config));
        assertTrue(e.getMessage().contains("config-version"), e::getMessage);
    }

    @Test
    void normalizesFilterListAndPreservesFutureNamesWithWarning() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.black-entities", List.of(" mushroom_cow ", "snowman", "FUTURE_MOB"));
        List<String> warnings = new ArrayList<>();
        PluginSettings settings = load(config, warnings);
        assertEquals(3, settings.filteredEntities().size());
        assertTrue(settings.filteredEntities().containsAll(List.of("MOOSHROOM", "SNOW_GOLEM", "FUTURE_MOB")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("FUTURE_MOB")), warnings::toString);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101, Integer.MAX_VALUE})
    void rejectsInvalidPercentages(int value) {
        for (String path : List.of("settings.egg-break-chance", "settings.ground-spawn-chance")) {
            YamlConfiguration config = TestSupport.config();
            config.set(path, value);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> load(config));
            assertTrue(e.getMessage().contains(path), e::getMessage);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void acceptsPercentageBoundaries(int value) {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.egg-break-chance", value);
        config.set("settings.ground-spawn-chance", value);
        PluginSettings settings = load(config);
        assertEquals(value, settings.rules().breakChance());
        assertEquals(value, settings.rules().groundChance());
    }

    @Test
    void rejectsWrongTypesInsteadOfSilentlyUsingDefaults() {
        for (Object[] entry : List.of(
                new Object[]{"settings", "not-a-section"},
                new Object[]{"sounds", 42},
                new Object[]{"messages", List.of("not-a-section")},
                new Object[]{"particles", 42},
                new Object[]{"settings.egg-break-chance", "30"},
                new Object[]{"settings.ground-spawn-chance", 12.5},
                new Object[]{"settings.affect-creative", "false"},
                new Object[]{"settings.cooldown-ticks", -1},
                new Object[]{"settings.cooldown-ticks", "1"},
                new Object[]{"settings.failure-action", "eat"},
                new Object[]{"settings.entity-filter", "greylist"},
                new Object[]{"settings.log-events", "no"},
                new Object[]{"settings.language", 42},
                new Object[]{"settings.black-entities", "WARDEN"},
                new Object[]{"settings.black-entities", List.of(123)},
                new Object[]{"settings.black-entities", List.of("not an entity")},
                new Object[]{"settings.disabled-worlds", "world"},
                new Object[]{"settings.worlds.world", 5},
                new Object[]{"settings.worlds.world.ground-spawn-chance", 101},
                new Object[]{"settings.entities.zombie.affect-creative", "true"},
                new Object[]{"messages.denied", 123},
                new Object[]{"messages.denied.output", "telepathy"},
                new Object[]{"sounds.ground-failure", "NOT_A_SOUND"},
                new Object[]{"sounds.egg-break.pitch", 5},
                new Object[]{"sounds.egg-break.volume", -1},
                new Object[]{"sounds.egg-break.source", "everywhere"},
                new Object[]{"sounds.egg-break", 42},
                new Object[]{"particles.egg-break.count", 0},
                new Object[]{"particles.egg-break.name", "not a particle"},
                new Object[]{"particles.egg-break.spread", -1},
                new Object[]{"particles.egg-break.offset-y", 99},
                new Object[]{"files.lang-directory", "../evil"},
                new Object[]{"files.stats-file", "/tmp/stats.yml"},
                new Object[]{"stats.used", "Used"},
                new Object[]{"defaults.sounds.volume", 11})) {
            YamlConfiguration config = TestSupport.config();
            config.set((String) entry[0], entry[1]);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> load(config),
                    entry[0].toString());
            assertTrue(e.getMessage().contains(entry[0].toString()), entry[0] + " -> " + e.getMessage());
        }
    }

    @Test
    void namesAreIndependentOfSystemLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            YamlConfiguration config = TestSupport.config();
            config.set("sounds.ground-failure", "entity_villager_no");
            config.set("settings.black-entities", List.of("pig"));
            config.set("settings.entities.pig.egg-break-chance", 1);
            config.set("settings.worlds.world.egg-break-chance", 2);
            PluginSettings settings = load(config);
            assertEquals(Key.key("entity.villager.no"), settings.sounds().get("ground-failure").name());
            assertTrue(settings.filteredEntities().contains("PIG"));
            assertTrue(settings.entities().containsKey("PIG"));
            assertEquals(1, settings.rulesFor("WORLD", "PIG").breakChance());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void rejectsInvalidEntityOverrideNames() {
        YamlConfiguration config = TestSupport.config();
        config.set("settings.entities.not a mob.egg-break-chance", 5);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> load(config));
        assertTrue(e.getMessage().contains("settings.entities.not a mob"), e::getMessage);
    }

    @Test
    void oldConfigInheritsNewKeysWithoutOverwritingUserChoices() {
        YamlConfiguration old = new YamlConfiguration();
        old.set("settings.ground-spawn-chance", 42);
        PluginSettings settings = PluginSettings.load(
                SmashEgg.withDefaults(old, TestSupport.config()), TestSupport.lang(), ignored -> {});
        assertEquals(42, settings.rules().groundChance());
        assertFalse(settings.rules().affectCreative());
        assertEquals(1, settings.cooldownTicks());
        assertEquals(FailureAction.CONSUME, settings.failureAction());
        assertTrue(settings.messages().get("reload-failure").enabled());
    }

    @Test
    void withDefaultsFillsMissingKeysAndKeepsUserValues() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("settings.ground-spawn-chance", 42);
        user.set("settings.language", "en_US");
        YamlConfiguration merged = SmashEgg.withDefaults(user, TestSupport.config());
        assertEquals(42, merged.getInt("settings.ground-spawn-chance"));
        assertEquals("en_US", merged.getString("settings.language"));
        assertEquals(1, merged.getInt("settings.cooldown-ticks"));
        assertEquals(true, merged.getBoolean("settings.egg-break-on-spawner"));
        assertEquals("block.glass.break", merged.getString("sounds.egg-break.key"));
        assertEquals(List.of("ENDER_DRAGON", "WITHER", "WARDEN"), merged.getStringList("settings.black-entities"));
    }
}
