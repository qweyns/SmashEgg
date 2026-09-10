package org.karton.smashegg.effect;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Sound configuration read from config.yml. */
public class Sounds {
    private final Map<String, Sound> sounds = new HashMap<>();
    private final Map<String, Float> volumes = new HashMap<>();
    private final Map<String, Float> pitches = new HashMap<>();
    private final Map<String, String> sources = new HashMap<>();

    public Sounds(ConfigurationSection section) {
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object value = section.get(key);
                if (value instanceof String soundName) {
                    String trimmed = soundName.trim();
                    if (trimmed.isEmpty()) {
                        // Empty string disables the sound
                        sounds.put(key, null);
                    } else {
                        try {
                            Sound sound = Sound.valueOf(trimmed.toUpperCase());
                            sounds.put(key, sound);
                        } catch (IllegalArgumentException e) {
                            // Keep invalid as null; plugin will handle gracefully
                            sounds.put(key, null);
                        }
                    }
                }
            }
        }
    }

    public Sound get(String key) {
        return sounds.get(key);
    }

    public float getVolume(String key) {
        return volumes.getOrDefault(key, 1.0f);
    }

    public float getPitch(String key) {
        return pitches.getOrDefault(key, 1.0f);
    }

    public String getSource(String key) {
        return sources.getOrDefault(key, "MASTER");
    }

    public static Sounds fromConfig(FileConfiguration config) {
        Sounds result = new Sounds();
        ConfigurationSection soundsSection = config.getConfigurationSection("sounds");
        if (soundsSection != null) {
            for (String key : soundsSection.getKeys(false)) {
                Object value = soundsSection.get(key);
                if (value instanceof String soundName) {
                    String trimmed = soundName.trim();
                    if (trimmed.isEmpty()) {
                        result.sounds.put(key, null);
                        result.volumes.put(key, 0.0f);
                        result.pitches.put(key, 1.0f);
                        result.sources.put(key, "MASTER");
                    } else {
                        try {
                            Sound sound = Sound.valueOf(trimmed.toUpperCase());
                            result.sounds.put(key, sound);
                            // Default volume/pitch/source if not specified
                            result.volumes.put(key, readVolume(soundsSection, key + ".volume"));
                            result.pitches.put(key, readPitch(soundsSection, key + ".pitch"));
                            result.sources.put(key, readSource(soundsSection, key + ".source"));
                        } catch (IllegalArgumentException e) {
                            result.sounds.put(key, null);
                            result.volumes.put(key, 1.0f);
                            result.pitches.put(key, 1.0f);
                            result.sources.put(key, "MASTER");
                        }
                    }
                }
            }
        }
        // Set defaults for required keys
        result.defaults();
        return result;
    }

    private static float readVolume(ConfigurationSection section, String path) {
        Object val = section.get(path);
        if (val instanceof Number n) return n.floatValue();
        return 1.0f;
    }

    private static float readPitch(ConfigurationSection section, String path) {
        Object val = section.get(path);
        if (val instanceof Number n) return n.floatValue();
        return 1.0f;
    }

    private static String readSource(ConfigurationSection section, String path) {
        Object val = section.get(path);
        if (val instanceof String s) return s.trim().toUpperCase();
        return "MASTER";
    }

    private void defaults() {
        // Defaults for the four required sound keys
        if (!sounds.containsKey("success")) sounds.put("success", Sound.ENTITY_PLAYER_LEVELUP);
        if (!sounds.containsKey("failure")) sounds.put("failure", Sound.ENTITY_VILLAGER_NO);
        if (!sounds.containsKey("denied")) sounds.put("denied", Sound.BLOCK_ANVIL_LAND);
        if (!sounds.containsKey("egg-break")) sounds.put("egg-break", Sound.BLOCK_GLASS_BREAK);

        if (!volumes.containsKey("success")) volumes.put("success", 1.0f);
        if (!volumes.containsKey("failure")) volumes.put("failure", 1.0f);
        if (!volumes.containsKey("denied")) volumes.put("denied", 1.0f);
        if (!volumes.containsKey("egg-break")) volumes.put("egg-break", 1.0f);

        if (!pitches.containsKey("success")) pitches.put("success", 1.0f);
        if (!pitches.containsKey("failure")) pitches.put("failure", 1.0f);
        if (!pitches.containsKey("denied")) pitches.put("denied", 1.0f);
        if (!pitches.containsKey("egg-break")) pitches.put("egg-break", 1.0f);

        if (!sources.containsKey("success")) sources.put("success", "MASTER");
        if (!sources.containsKey("failure")) sources.put("failure", "MASTER");
        if (!sources.containsKey("denied")) sources.put("denied", "MASTER");
        if (!sources.containsKey("egg-break")) sources.put("egg-break", "MASTER");
    }
}