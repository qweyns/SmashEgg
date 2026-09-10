package org.karton.smashegg.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.FileConfiguration;

/** Validated, immutable snapshot. Reload replaces it only after every setting is valid. */
public record PluginSettings(boolean breakOnSpawner, int breakChance, int groundChance,
                      boolean affectCreative, Set<String> blacklist,
                      Map<String, Sound> sounds, Map<String, Component> messages) {

    private static final Set<String> DEFAULT_SOUND_KEYS = Set.of("success", "failure", "denied", "egg-break");
    private static final Set<String> DEFAULT_MESSAGE_KEYS = Set.of("usage", "reload-success", "reload-failure",
            "no-permission", "denied", "egg-break", "ground-failure",
            "info", "stats", "stats-reset", "unknown");

    public PluginSettings {
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
        } else {
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
        }

        // Load sounds - now extensible: all keys under "sounds." are allowed,
        // not just a fixed set. Unknown keys won't trigger "unknown config key" warning.
        Map<String, Sound> sounds = new HashMap<>();
        ConfigurationSection soundsSection = config.getConfigurationSection("sounds");
        if (soundsSection != null) {
            for (String key : soundsSection.getKeys(false)) {
                Object value = soundsSection.get(key);
                if (value instanceof String soundName) {
                    String trimmed = soundName.trim();
                    if (trimmed.isEmpty()) {
                        // Empty string explicitly disables the sound
                        sounds.put(key, null);
                    } else {
                        try {
                            sounds.put(key, Sound.valueOf(trimmed.toUpperCase(Locale.ROOT)));
                        } catch (IllegalArgumentException e) {
                            throw invalid("sounds." + key, "unknown sound: " + value);
                        }
                    }
                }
            }
        }

        // Load messages - now extensible: all keys under "messages." are allowed,
        // not just a fixed set. Unknown keys won't trigger "unknown config key" warning.
        Map<String, Component> messages = new HashMap<>();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                Object value = messagesSection.get(key);
                if (value instanceof String messageText) {
                    try {
                        messages.put(key, ColorUtil.parse(messageText));
                    } catch (IllegalArgumentException e) {
                        throw invalid("messages." + key, e.getMessage());
                    }
                }
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