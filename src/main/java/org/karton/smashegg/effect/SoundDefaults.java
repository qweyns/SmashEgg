package org.karton.smashegg.effect;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.sound.Sound;
import org.karton.smashegg.config.ConfigNodes;
import org.karton.smashegg.config.SectionFields;

/** Global fallbacks for a sound that is written as a plain key string or omits volume/pitch/source. */
public record SoundDefaults(float volume, float pitch, Sound.Source source) {
    public static final SoundDefaults BUILTIN = new SoundDefaults(1.0f, 1.0f, Sound.Source.MASTER);
    private static final Set<String> FIELDS = Set.of("volume", "pitch", "source");

    public static SoundDefaults parse(Object node, String path, Consumer<String> warning) {
        Map<String, Object> section = ConfigNodes.section(node, path);
        SectionFields.check(section, path, FIELDS, warning);
        float volume = (float) ConfigNodes.decimal(section.getOrDefault("volume", BUILTIN.volume()),
                path + ".volume", 0.0, 10.0);
        float pitch = (float) ConfigNodes.decimal(section.getOrDefault("pitch", BUILTIN.pitch()),
                path + ".pitch", 0.0, 2.0);
        Sound.Source source = Sounds.source(section.getOrDefault("source", BUILTIN.source().name()), path + ".source");
        return new SoundDefaults(volume, pitch, source);
    }
}
