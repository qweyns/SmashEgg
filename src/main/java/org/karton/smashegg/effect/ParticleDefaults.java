package org.karton.smashegg.effect;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.karton.smashegg.config.ConfigNodes;
import org.karton.smashegg.config.SectionFields;

/** Global fallbacks for a particle written as a plain name or omitting count/spread/speed/offset. */
public record ParticleDefaults(int count, double spread, double speed,
                               double offsetX, double offsetY, double offsetZ) {
    public static final ParticleDefaults BUILTIN = new ParticleDefaults(10, 0.3, 0.0, 0.0, 1.0, 0.0);
    static final Set<String> FIELDS = Set.of("count", "spread", "speed", "offset-x", "offset-y", "offset-z");

    public static ParticleDefaults parse(Object node, String path, Consumer<String> warning) {
        Map<String, Object> section = ConfigNodes.section(node, path);
        SectionFields.check(section, path, FIELDS, warning);
        return new ParticleDefaults(
                ConfigNodes.integer(section.getOrDefault("count", BUILTIN.count()), path + ".count", 1, 1000),
                ConfigNodes.decimal(section.getOrDefault("spread", BUILTIN.spread()), path + ".spread", 0.0, 8.0),
                ConfigNodes.decimal(section.getOrDefault("speed", BUILTIN.speed()), path + ".speed", 0.0, 10.0),
                ConfigNodes.decimal(section.getOrDefault("offset-x", BUILTIN.offsetX()), path + ".offset-x", -16.0, 16.0),
                ConfigNodes.decimal(section.getOrDefault("offset-y", BUILTIN.offsetY()), path + ".offset-y", -16.0, 16.0),
                ConfigNodes.decimal(section.getOrDefault("offset-z", BUILTIN.offsetZ()), path + ".offset-z", -16.0, 16.0));
    }
}
