package org.karton.smashegg.effect;

import java.util.Locale;
import org.karton.smashegg.config.ConfigNodes;

/** Who hears a SmashEgg sound. Particles are already spawned in the world and visible nearby. */
public enum SoundAudience {
    SELF, NEARBY, WORLD;

    public static SoundAudience parse(String value, String path) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (SoundAudience audience : values()) {
            if (audience.name().equals(normalized)) return audience;
        }
        throw ConfigNodes.invalid(path, "must be one of self, nearby, world (found: " + value + ")");
    }
}
