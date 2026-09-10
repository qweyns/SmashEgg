package org.karton.smashegg.effect;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/** A parsed sound plus who should hear it. */
public record SoundCue(Sound sound, SoundAudience audience, double radius) {
    public static final double DEFAULT_RADIUS = 16.0;

    public SoundCue {
        if (sound == null) throw new NullPointerException("sound");
        audience = audience == null ? SoundAudience.SELF : audience;
        radius = radius < 0 ? 0 : radius;
    }

    public Key name() {
        return sound.name();
    }

    public float volume() {
        return sound.volume();
    }

    public float pitch() {
        return sound.pitch();
    }

    public Sound.Source source() {
        return sound.source();
    }
}
