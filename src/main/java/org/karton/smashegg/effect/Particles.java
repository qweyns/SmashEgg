package org.karton.smashegg.effect;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Particle configuration read from config.yml. */
public class Particles {
    private final Map<String, Particle> particles = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, Float> spreads = new HashMap<>();
    private final Map<String, Float> speeds = new HashMap<>();
    private final Map<String, Double> offsets = new HashMap<>();

    public Particles(ConfigurationSection section) {
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object value = section.get(key);
                if (value instanceof String particleName) {
                    String trimmed = particleName.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            Particle particle = Particle.valueOf(trimmed.toUpperCase());
                            particles.put(key, particle);
                            // Read optional parameters with defaults
                            counts.put(key, readInt(section, key + ".count", 10));
                            spreads.put(key, readFloat(section, key + ".spread", 0.3f));
                            speeds.put(key, readFloat(section, key + ".speed", 0.0f));
                            offsets.put(key, readOffset(section, key + ".offset-y", 1.0));
                            // Also read offset-x and offset-z if specified
                            readOffset(section, key + ".offset-x", null);
                            readOffset(section, key + ".offset-z", null);
                        } catch (IllegalArgumentException e) {
                            // Skip invalid particle names
                        }
                    }
                }
            }
        }
    }

    public Particle get(String key) {
        return particles.get(key);
    }

    public int getCount(String key) {
        return counts.getOrDefault(key, 10);
    }

    public float getSpread(String key) {
        return spreads.getOrDefault(key, 0.3f);
    }

    public float getSpeed(String key) {
        return speeds.getOrDefault(key, 0.0f);
    }

    public double getOffsetY(String key) {
        return offsets.getOrDefault(key, 1.0);
    }

    private static int readInt(ConfigurationSection section, String path, int def) {
        Object val = section.get(path);
        if (val instanceof Integer i) return i;
        return def;
    }

    private static float readFloat(ConfigurationSection section, String path, float def) {
        Object val = section.get(path);
        if (val instanceof Number n) return n.floatValue();
        return def;
    }

    private static double readOffset(ConfigurationSection section, String path, double def) {
        Object val = section.get(path);
        if (val instanceof Double d) return d;
        if (val instanceof Number n) return n.doubleValue();
        return def;
    }

    public static Particles fromConfig(FileConfiguration config) {
        Particles result = new Particles();
        ConfigurationSection particlesSection = config.getConfigurationSection("particles");
        if (particlesSection != null) {
            for (String key : particlesSection.getKeys(false)) {
                Object value = particlesSection.get(key);
                if (value instanceof String particleName) {
                    String trimmed = particleName.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            Particle particle = Particle.valueOf(trimmed.toUpperCase());
                            result.particles.put(key, particle);
                            result.counts.put(key, readInt(particlesSection, key + ".count", 10));
                            result.spreads.put(key, readFloat(particlesSection, key + ".spread", 0.3f));
                            result.speeds.put(key, readFloat(particlesSection, key + ".speed", 0.0f));
                            result.offsets.put(key, readOffset(particlesSection, key + ".offset-y", 1.0));
                        } catch (IllegalArgumentException e) {
                            // Skip invalid
                        }
                    }
                }
            }
        }
        // Set defaults for required behavior
        result.defaults();
        return result;
    }

    private void defaults() {
        // Ensure at least one particle is configured
        if (particles.isEmpty()) {
            // Default particle: simply not used; plugin handles gracefully
        }
    }
}