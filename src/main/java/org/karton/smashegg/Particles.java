package org.karton.smashegg;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;

/**
 * Particle lookup that survives Paper turning {@code org.bukkit.Particle} into a plain registry
 * type: the registry is queried first and the enum constant is only a fallback. Paper documents
 * that enums implementing {@code Keyed} may stop being enums, so {@code valueOf} alone is unsafe.
 */
final class Particles {
    static final Set<String> FIELDS = Set.of("name", "count", "spread", "speed");
    private static final Map<String, Optional<Particle>> CACHE = new ConcurrentHashMap<>();

    private Particles() {}

    /** Called on reload so a particle added by a datapack becomes visible without a restart. */
    static void resetCache() {
        CACHE.clear();
    }

    /** @return the particle, or {@code null} when this server has no particle with that name */
    static Particle resolve(String name) {
        return CACHE.computeIfAbsent(name.toLowerCase(Locale.ROOT), Particles::lookup).orElse(null);
    }

    /** @return the spec, or {@code null} when the value disables particles for that effect */
    static ParticleSpec parse(Object value, String path, Consumer<String> warning) {
        if (value instanceof String text) return spec(text, 10, 0.3, 0.0, path);
        Map<String, Object> section = ConfigNodes.section(value, path);
        SectionFields.check(section, path, FIELDS, warning);
        return spec(ConfigNodes.string(section.get("name"), path + ".name"),
                ConfigNodes.integer(section.getOrDefault("count", 10), path + ".count", 1, 1000),
                ConfigNodes.decimal(section.getOrDefault("spread", 0.3), path + ".spread", 0.0, 8.0),
                ConfigNodes.decimal(section.getOrDefault("speed", 0.0), path + ".speed", 0.0, 10.0),
                path);
    }

    private static ParticleSpec spec(String name, int count, double spread, double speed, String path) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null; // An empty name explicitly disables particles.
        if (!trimmed.matches("[A-Za-z0-9_.\\-]+")) {
            throw ConfigNodes.invalid(path + ".name", "invalid particle name: " + name);
        }
        return new ParticleSpec(trimmed, count, spread, speed);
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
