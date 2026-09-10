package org.karton.smashegg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/** Validated, immutable snapshot. Reload replaces it only after every setting is valid. */
record PluginSettings(boolean breakOnSpawner, int breakChance, int groundChance,
                      boolean affectCreative, Set<String> blacklist,
                      Map<String, Sound> sounds, Map<String, Component> messages) {
    private static final List<String> SOUND_KEYS = List.of("success", "failure", "denied", "egg-break");
    private static final List<String> MESSAGE_KEYS = List.of("usage", "reload-success", "reload-failure",
            "no-permission", "egg-break", "ground-failure", "denied");

    PluginSettings {
        blacklist = Set.copyOf(blacklist);
        sounds = Map.copyOf(sounds);
        messages = Map.copyOf(messages);
    }

    static PluginSettings load(FileConfiguration config, Consumer<String> warning) {
        for (String section : List.of("settings", "sounds", "messages")) {
            if (!config.isConfigurationSection(section)) {
                throw invalid(section, "must be a YAML section");
            }
        }
        Set<String> blacklist = new HashSet<>();
        Object entries = config.get("settings.black-entities");
        if (!(entries instanceof List<?> list)) {
            throw invalid("settings.black-entities", "must be a list of entity names");
        }
        for (Object entry : list) {
            if (!(entry instanceof String text)) {
                throw invalid("settings.black-entities", "must contain only strings");
            }
            String name = EggTypes.normalize(text);
            if (!name.matches("[A-Z][A-Z0-9_]*")) {
                throw invalid("settings.black-entities", "invalid entity name: " + text);
            }
            blacklist.add(name);
            if (Material.getMaterial(name + "_SPAWN_EGG") == null) {
                warning.accept("settings.black-entities: " + name
                        + " has no spawn egg on this server; kept for compatibility with other versions.");
            }
        }

        Map<String, Sound> sounds = new HashMap<>();
        for (String key : SOUND_KEYS) {
            String path = "sounds." + key;
            String value = string(config, path);
            try {
                Sounds.parse(value, warning).ifPresent(sound -> sounds.put(key, sound));
            } catch (IllegalArgumentException e) {
                throw invalid(path, e.getMessage());
            }
        }

        Map<String, Component> messages = new HashMap<>();
        for (String key : MESSAGE_KEYS) {
            String path = "messages." + key;
            String value = string(config, path);
            try {
                messages.put(key, ColorUtil.parse(value));
            } catch (IllegalArgumentException e) {
                throw invalid(path, e.getMessage());
            }
        }
        return new PluginSettings(bool(config, "settings.egg-break-on-spawner"),
                percent(config, "settings.egg-break-chance"),
                percent(config, "settings.ground-spawn-chance"),
                bool(config, "settings.affect-creative"), blacklist, sounds, messages);
    }

    private static String string(FileConfiguration config, String path) {
        Object value = config.get(path);
        if (!(value instanceof String text)) throw invalid(path, "must be a string");
        return text;
    }

    private static boolean bool(FileConfiguration config, String path) {
        Object value = config.get(path);
        if (!(value instanceof Boolean flag)) throw invalid(path, "must be true or false");
        return flag;
    }

    private static int percent(FileConfiguration config, String path) {
        Object value = config.get(path);
        if (!(value instanceof Integer number) || number < 0 || number > 100) {
            throw invalid(path, "must be an integer between 0 and 100");
        }
        return number;
    }

    private static IllegalArgumentException invalid(String path, String reason) {
        return new IllegalArgumentException(path + ": " + reason);
    }
}
