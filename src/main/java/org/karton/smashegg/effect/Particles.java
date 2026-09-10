package org.karton.smashegg.effect;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.karton.smashegg.config.ConfigNodes;
import org.karton.smashegg.config.SectionFields;

/**
 * Particle lookup that survives Paper turning {@code org.bukkit.Particle} into a plain registry
 * type: the registry is queried first and the enum constant is only a fallback. Paper documents
 * that enums implementing {@code Keyed} may stop being enums, so {@code valueOf} alone is unsafe.
 */
public final class Particles {
    public static final Set<String> FIELDS;
    private static final Map<String, Optional<Particle>> CACHE = new ConcurrentHashMap<>();

    static {
        Set<String> fields = new HashSet<>(ParticleDefaults.FIELDS);
        fields.add("name");
        FIELDS = Set.copyOf(fields);
    }

    private Particles() {}

    /** Called on reload so a particle added by a datapack becomes visible without a restart. */
    public static void resetCache() {
        CACHE.clear();
    }

    /** @return the particle, or {@code null} when this server has no particle with that name */
    public static Particle resolve(String name) {
        return CACHE.computeIfAbsent(name.toLowerCase(Locale.ROOT), Particles::lookup).orElse(null);
    }

    public static ParticleSpec parse(Object value, String path, Consumer<String> warning) {
        return parse(value, path, ParticleDefaults.BUILTIN, warning);
    }

    /** @return the spec, or {@code null} when the value disables particles for that effect */
    public static ParticleSpec parse(Object value, String path, ParticleDefaults defaults, Consumer<String> warning) {
        if (value instanceof String text) {
            return spec(text, defaults.count(), defaults.spread(), defaults.speed(),
                    defaults.offsetX(), defaults.offsetY(), defaults.offsetZ(), path);
        }
        Map<String, Object> section = ConfigNodes.section(value, path);
        SectionFields.check(section, path, FIELDS, warning);
        return spec(ConfigNodes.string(section.get("name"), path + ".name"),
                ConfigNodes.integer(section.getOrDefault("count", defaults.count()), path + ".count", 1, 1000),
                ConfigNodes.decimal(section.getOrDefault("spread", defaults.spread()), path + ".spread", 0.0, 8.0),
                ConfigNodes.decimal(section.getOrDefault("speed", defaults.speed()), path + ".speed", 0.0, 10.0),
                ConfigNodes.decimal(section.getOrDefault("offset-x", defaults.offsetX()), path + ".offset-x", -16.0, 16.0),
                ConfigNodes.decimal(section.getOrDefault("offset-y", defaults.offsetY()), path + ".offset-y", -16.0, 16.0),
                ConfigNodes.decimal(section.getOrDefault("offset-z", defaults.offsetZ()), path + ".offset-z", -16.0, 16.0),
                path);
    }

    private static ParticleSpec spec(String name, int count, double spread, double speed,
                                     double offsetX, double offsetY, double offsetZ, String path) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null; // An empty name explicitly disables particles.
        if (!trimmed.matches("[A-Za-z0-9_.\\-]+")) {
            throw ConfigNodes.invalid(path + ".name", "invalid particle name: " + name);
        }
        return new ParticleSpec(trimmed, count, spread, speed, offsetX, offsetY, offsetZ);
    }

    private static Optional<Particle> lookup(String name) {
        try {
            Particle fromRegistry = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(name));
            if (fromRegistry != null) return Optional.of(fromRegistry);
        } catch (LinkageError | RuntimeException ignored) {
            // A Paper build without the particle registry: fall back to the enum constant below.
        }
        try {
            return Optional.of(Particle.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | LinkageError e) {
            return Optional.empty();
        }
    }
}
