package org.karton.smashegg.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.karton.smashegg.effect.MessageSpec;
import org.karton.smashegg.effect.Messages;
import org.karton.smashegg.effect.ParticleDefaults;
import org.karton.smashegg.effect.ParticleSpec;
import org.karton.smashegg.effect.Particles;
import org.karton.smashegg.effect.SoundDefaults;
import org.karton.smashegg.effect.Sounds;
import org.karton.smashegg.stats.Stats.StatsMapping;
import org.karton.smashegg.util.EggTypes;

/**
 * Validated, immutable snapshot of the configuration. Reload replaces it only after every setting
 * is valid, so a broken file can never leave the plugin half-configured.
 */
public record PluginSettings(int configVersion, String language, Rules rules,
                      Map<String, Rules.Overrides> worlds, Map<String, Rules.Overrides> entities,
                      FilterMode filterMode, Set<String> filteredEntities,
                      int cooldownTicks, FailureAction failureAction, boolean logEvents,
                      Set<String> disabledWorlds,
                      Map<String, Sound> sounds, Map<String, ParticleSpec> particles,
                      Map<String, MessageSpec> messages,
                      PlaceholderWords words, SoundDefaults soundDefaults, ParticleDefaults particleDefaults,
                      StatsMapping stats, String langDirectory, String statsFile) {

    public static final int CONFIG_VERSION = 4;
    public static final String DEFAULT_LANGUAGE = "ru_RU";
    public static final String DEFAULT_LANG_DIRECTORY = "lang";
    public static final String DEFAULT_STATS_FILE = "stats.yml";
    /** Keys the plugin itself sends; extra keys in config/lang are still loaded. */
    public static final List<String> EFFECT_KEYS = List.of("success", "ground-failure", "denied", "egg-break");
    public static final List<String> MESSAGE_KEYS = List.of("usage", "reload-success", "reload-failure",
            "no-permission", "egg-break", "ground-failure", "denied", "success",
            "info", "stats", "stats-reset", "unknown");
    /** Sound key renamed in 4.0; the old name keeps working. */
    private static final Map<String, String> RENAMED_SOUND_KEYS = Map.of("ground-failure", "failure");

    private static final Set<String> TOP_LEVEL_KEYS = Set.of("config-version", "settings", "sounds",
            "messages", "particles", "defaults", "stats", "files");
    private static final Set<String> SETTINGS_KEYS = Set.of("language", "egg-break-on-spawner",
            "egg-break-chance", "ground-spawn-chance", "affect-creative", "cooldown-ticks",
            "failure-action", "log-events", "entity-filter", "black-entities", "allowed-entities",
            "disabled-worlds",
            "worlds", "entities");
    private static final Set<String> RULE_KEYS = Set.of("egg-break-on-spawner", "egg-break-chance",
            "ground-spawn-chance", "affect-creative");
    private static final Set<String> DEFAULTS_KEYS = Set.of("sounds", "particles");
    private static final Set<String> FILES_KEYS = Set.of("lang-directory", "stats-file");
    private static final Set<String> STATS_KEYS = Set.of("used", "counters", "effects");

    public PluginSettings {
        worlds = Map.copyOf(worlds);
        entities = Map.copyOf(entities);
        filteredEntities = Set.copyOf(filteredEntities);
        disabledWorlds = Set.copyOf(disabledWorlds);
        sounds = Map.copyOf(sounds);
        particles = Map.copyOf(particles);
        messages = Map.copyOf(messages);
    }

    public static PluginSettings load(FileConfiguration config, FileConfiguration lang, Consumer<String> warning) {
        if (config.get("settings") == null) {
            throw ConfigNodes.invalid("settings", "must be a YAML section");
        }
        int configVersion = configVersion(config);
        Map<String, Object> settings = ConfigNodes.section(config.get("settings"), "settings");
        for (String key : config.getKeys(false)) {
            if (!TOP_LEVEL_KEYS.contains(key)) warning.accept("unknown config key: " + key);
        }
        SectionFields.check(settings, "settings", SETTINGS_KEYS, warning);

        Rules rules = new Rules(
                ConfigNodes.bool(settings.get("egg-break-on-spawner"), "settings.egg-break-on-spawner"),
                ConfigNodes.integer(settings.get("egg-break-chance"), "settings.egg-break-chance", 0, 100),
                ConfigNodes.integer(settings.get("ground-spawn-chance"), "settings.ground-spawn-chance", 0, 100),
                ConfigNodes.bool(settings.get("affect-creative"), "settings.affect-creative"));
        Map<String, Rules.Overrides> worlds = overrides(config.get("settings.worlds"), "settings.worlds",
                false, warning);
        Map<String, Rules.Overrides> entities = overrides(config.get("settings.entities"), "settings.entities",
                true, warning);

        FilterMode filterMode = FilterMode.parse(
                ConfigNodes.string(settings.get("entity-filter"), "settings.entity-filter"),
                "settings.entity-filter");
        // WHITELIST reads allowed-entities so the config reads naturally; black-entities is the
        // list for BLACKLIST and keeps working in WHITELIST for older configurations.
        Object entityListNode = filterMode == FilterMode.WHITELIST
                ? settings.get("allowed-entities") : null;
        String entityListPath = "settings.allowed-entities";
        if (entityListNode == null) {
            entityListNode = settings.get("black-entities");
            entityListPath = "settings.black-entities";
        }
        Set<String> filteredEntities = entityList(entityListNode, entityListPath, warning);
        Set<String> disabledWorlds = worldList(settings.get("disabled-worlds"), "settings.disabled-worlds");

        int cooldownTicks = ConfigNodes.integer(settings.get("cooldown-ticks"), "settings.cooldown-ticks", 0, 72000);
        FailureAction failureAction = FailureAction.parse(
                ConfigNodes.string(settings.get("failure-action"), "settings.failure-action"),
                "settings.failure-action");
        boolean logEvents = ConfigNodes.bool(settings.get("log-events"), "settings.log-events");

        Files files = files(config.get("files"), warning);
        EffectDefaults defaults = defaults(config.get("defaults"), warning);
        StatsMapping stats = stats(config.get("stats"), warning);
        PlaceholderWords words = PlaceholderWords.load(lang == null ? null : lang.get("placeholders"),
                "placeholders", warning);

        return new PluginSettings(configVersion,
                ConfigNodes.string(settings.get("language"), "settings.language"),
                rules, worlds, entities, filterMode, filteredEntities,
                cooldownTicks, failureAction, logEvents, disabledWorlds,
                sounds(config, defaults.sounds(), warning),
                particles(config, defaults.particles(), warning),
                messages(config, lang, warning),
                words, defaults.sounds(), defaults.particles(), stats,
                files.langDirectory(), files.statsFile());
    }

    /** Effective rules for a click: global settings, then the world, then the mob. */
    public Rules rulesFor(String world, String entity) {
        Rules result = rules;
        Rules.Overrides worldOverride = worlds.get(world.toLowerCase(Locale.ROOT));
        if (worldOverride != null) result = result.apply(worldOverride);
        Rules.Overrides entityOverride = entities.get(entity);
        if (entityOverride != null) result = result.apply(entityOverride);
        return result;
    }

    public boolean isDisabled(String world) {
        return disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }

    /** @return true when the mob may not be put into a spawner */
    public boolean blocksEntity(String entity) {
        return filterMode.blocks(filteredEntities.contains(entity));
    }

    private static int configVersion(FileConfiguration config) {
        Object value = config.get("config-version");
        if (value == null) return 0; // A config from before versioning; every key is still understood.
        int version = ConfigNodes.integer(value, "config-version", 0, Integer.MAX_VALUE);
        if (version > CONFIG_VERSION) {
            throw ConfigNodes.invalid("config-version", "this SmashEgg understands up to " + CONFIG_VERSION
                    + "; update the plugin or restore the config of your version");
        }
        return version;
    }

    private static Map<String, Rules.Overrides> overrides(Object node, String path, boolean entityNames,
                                                          Consumer<String> warning) {
        Map<String, Rules.Overrides> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : ConfigNodes.section(node, path).entrySet()) {
            String name = entry.getKey();
            String entryPath = path + "." + name;
            Map<String, Object> values = ConfigNodes.section(entry.getValue(), entryPath);
            SectionFields.check(values, entryPath, RULE_KEYS, warning);
            String key = entityNames ? EggTypes.normalize(name) : name.toLowerCase(Locale.ROOT);
            if (entityNames && !key.matches("[A-Z][A-Z0-9_]*")) {
                throw ConfigNodes.invalid(entryPath, "invalid entity name: " + name);
            }
            Rules.Overrides override = new Rules.Overrides(
                    ConfigNodes.optionalBool(values.get("egg-break-on-spawner"), entryPath + ".egg-break-on-spawner"),
                    ConfigNodes.optionalPercent(values.get("egg-break-chance"), entryPath + ".egg-break-chance"),
                    ConfigNodes.optionalPercent(values.get("ground-spawn-chance"), entryPath + ".ground-spawn-chance"),
                    ConfigNodes.optionalBool(values.get("affect-creative"), entryPath + ".affect-creative"));
            if (override.isEmpty()) warning.accept(entryPath + " sets nothing and can be removed");
            result.put(key, override);
        }
        return result;
    }

    private static Set<String> entityList(Object node, String path, Consumer<String> warning) {
        if (!(node instanceof List<?> list)) throw ConfigNodes.invalid(path, "must be a list of entity names");
        Set<String> names = new HashSet<>();
        for (Object entry : list) {
            if (!(entry instanceof String text)) throw ConfigNodes.invalid(path, "must contain only strings");
            String name = EggTypes.normalize(text);
            if (!name.matches("[A-Z][A-Z0-9_]*")) throw ConfigNodes.invalid(path, "invalid entity name: " + text);
            names.add(name);
            if (Material.getMaterial(name + "_SPAWN_EGG") == null) {
                warning.accept(path + ": " + name
                        + " has no spawn egg on this server; kept for compatibility with other versions.");
            }
        }
        return names;
    }

    private static Set<String> worldList(Object node, String path) {
        if (!(node instanceof List<?> list)) throw ConfigNodes.invalid(path, "must be a list of world names");
        Set<String> names = new LinkedHashSet<>();
        for (Object entry : list) {
            if (!(entry instanceof String text)) throw ConfigNodes.invalid(path, "must contain only strings");
            names.add(text.toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static Files files(Object node, Consumer<String> warning) {
        Map<String, Object> section = ConfigNodes.section(node, "files");
        SectionFields.check(section, "files", FILES_KEYS, warning);
        String langDirectory = section.get("lang-directory") == null
                ? DEFAULT_LANG_DIRECTORY
                : ConfigNodes.relativePath(section.get("lang-directory"), "files.lang-directory");
        String statsFile = section.get("stats-file") == null
                ? DEFAULT_STATS_FILE
                : ConfigNodes.relativePath(section.get("stats-file"), "files.stats-file");
        return new Files(langDirectory, statsFile);
    }

    private static EffectDefaults defaults(Object node, Consumer<String> warning) {
        Map<String, Object> section = ConfigNodes.section(node, "defaults");
        SectionFields.check(section, "defaults", DEFAULTS_KEYS, warning);
        return new EffectDefaults(
                SoundDefaults.parse(section.get("sounds"), "defaults.sounds", warning),
                ParticleDefaults.parse(section.get("particles"), "defaults.particles", warning));
    }

    private static StatsMapping stats(Object node, Consumer<String> warning) {
        Map<String, Object> section = ConfigNodes.section(node, "stats");
        if (section.isEmpty()) return StatsMapping.DEFAULT;
        SectionFields.check(section, "stats", STATS_KEYS, warning);
        String used = section.get("used") == null ? StatsMapping.DEFAULT.used()
                : counterName(section.get("used"), "stats.used");
        List<String> counters = counterList(section.get("counters"), "stats.counters");
        Map<String, String> effects = effectMap(section.get("effects"), "stats.effects");
        return new StatsMapping(used, counters, effects);
    }

    private static List<String> counterList(Object node, String path) {
        if (node == null) return List.of();
        if (!(node instanceof List<?> list)) throw ConfigNodes.invalid(path, "must be a list of counter names");
        List<String> names = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof String text)) throw ConfigNodes.invalid(path, "must contain only strings");
            names.add(counterName(text, path));
        }
        return names;
    }

    private static Map<String, String> effectMap(Object node, String path) {
        if (node == null) return StatsMapping.DEFAULT.effects();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : ConfigNodes.section(node, path).entrySet()) {
            result.put(entry.getKey(), counterName(entry.getValue(), path + "." + entry.getKey()));
        }
        return result;
    }

    private static String counterName(Object value, String path) {
        String name = ConfigNodes.string(value, path).trim();
        if (!name.matches("[a-z][a-z0-9_]*")) {
            throw ConfigNodes.invalid(path, "invalid counter name: " + value);
        }
        return name;
    }

    private static Map<String, Sound> sounds(FileConfiguration config, SoundDefaults defaults,
                                             Consumer<String> warning) {
        Map<String, Object> nodes = ConfigNodes.section(config.get("sounds"), "sounds");
        Set<String> keys = new LinkedHashSet<>(EFFECT_KEYS);
        for (String key : nodes.keySet()) {
            if (!RENAMED_SOUND_KEYS.containsValue(key)) keys.add(key);
        }
        Map<String, Sound> sounds = new HashMap<>();
        for (String key : keys) {
            Object value = nodes.get(key);
            if (value == null && RENAMED_SOUND_KEYS.containsKey(key)) {
                String legacy = RENAMED_SOUND_KEYS.get(key);
                value = nodes.get(legacy);
                if (value != null) {
                    warning.accept("sounds." + legacy + " was renamed to sounds." + key + "; the old name still works.");
                }
            }
            if (value == null) continue;
            Sounds.parse(value, "sounds." + key, defaults, warning).ifPresent(sound -> sounds.put(key, sound));
        }
        return sounds;
    }

    private static Map<String, ParticleSpec> particles(FileConfiguration config, ParticleDefaults defaults,
                                                       Consumer<String> warning) {
        Map<String, Object> nodes = ConfigNodes.section(config.get("particles"), "particles");
        Set<String> keys = new LinkedHashSet<>(EFFECT_KEYS);
        keys.addAll(nodes.keySet());
        Map<String, ParticleSpec> particles = new HashMap<>();
        for (String key : keys) {
            Object value = nodes.get(key);
            if (value == null) continue;
            ParticleSpec spec = Particles.parse(value, "particles." + key, defaults, warning);
            if (spec != null) particles.put(key, spec);
        }
        return particles;
    }

    private static Map<String, MessageSpec> messages(FileConfiguration config, FileConfiguration lang,
                                                     Consumer<String> warning) {
        Map<String, Object> nodes = ConfigNodes.section(config.get("messages"), "messages");
        Map<String, Object> translations = ConfigNodes.section(lang == null ? null : lang.get("messages"),
                "messages (language file)");
        Set<String> keys = new LinkedHashSet<>(MESSAGE_KEYS);
        keys.addAll(nodes.keySet());
        keys.addAll(translations.keySet());
        Map<String, MessageSpec> messages = new HashMap<>();
        for (String key : keys) {
            Object value = nodes.get(key);
            String path = "messages." + key;
            if (value == null) {
                value = translations.get(key);
                path = path + " in the language file";
            }
            if (value == null) {
                if (MESSAGE_KEYS.contains(key)) {
                    warning.accept("messages." + key + " is missing from config.yml and the language file;"
                            + " the message is disabled.");
                }
                messages.put(key, MessageSpec.disabled());
                continue;
            }
            messages.put(key, Messages.parse(value, translations.get(key), path, warning));
        }
        return messages;
    }

    private record Files(String langDirectory, String statsFile) {}

    private record EffectDefaults(SoundDefaults sounds, ParticleDefaults particles) {}
}
