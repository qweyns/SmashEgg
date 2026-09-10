package org.karton.smashegg.effect;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.karton.smashegg.config.ConfigNodes;
import org.karton.smashegg.config.SectionFields;

/**
 * Sounds are namespaced keys on Paper: {@code org.bukkit.Sound} is a registry interface there, so
 * {@code Sound.valueOf(name)} does not exist and cannot be resolved without a running server.
 * Keys also allow sounds from datapacks and other plugins, which the old enum never covered.
 */
public final class Sounds {
    private static final Set<String> FIELDS = Set.of("key", "volume", "pitch", "source", "audience", "radius");
    /**
     * Bukkit enum names from configs written for SmashEgg 2.x, kept so an upgrade does not need a
     * config edit. Anything else has to be written as a key.
     */
    private static final Map<String, String> LEGACY_NAMES = Map.of(
            "ENTITY_PLAYER_LEVELUP", "entity.player.levelup",
            "ENTITY_VILLAGER_NO", "entity.villager.no",
            "BLOCK_ANVIL_LAND", "block.anvil.land",
            "BLOCK_GLASS_BREAK", "block.glass.break");

    private Sounds() {}

    public static Optional<Sound> parse(Object value, String path, Consumer<String> warning) {
        return parseCue(value, path, SoundDefaults.BUILTIN, warning).map(SoundCue::sound);
    }

    public static Optional<Sound> parse(Object value, String path, SoundDefaults defaults, Consumer<String> warning) {
        return parseCue(value, path, defaults, warning).map(SoundCue::sound);
    }

    public static Optional<SoundCue> parseCue(Object value, String path, Consumer<String> warning) {
        return parseCue(value, path, SoundDefaults.BUILTIN, warning);
    }

    /**
     * Accepts either a plain key string or a section with key/volume/pitch/source/audience/radius.
     *
     * @param value raw config value; an empty string disables the sound
     * @param path  config path, used in error messages
     * @return the resolved cue, or empty when the value disables it
     */
    public static Optional<SoundCue> parseCue(Object value, String path, SoundDefaults defaults,
                                              Consumer<String> warning) {
        if (value instanceof String text) {
            return key(text, path, warning)
                    .map(name -> new SoundCue(build(name, defaults.volume(), defaults.pitch(), defaults.source()),
                            SoundAudience.SELF, SoundCue.DEFAULT_RADIUS));
        }
        Map<String, Object> section = ConfigNodes.section(value, path);
        SectionFields.check(section, path, FIELDS, warning);
        Key name = key(ConfigNodes.string(section.get("key"), path + ".key"), path + ".key", warning)
                .orElseThrow(() -> ConfigNodes.invalid(path + ".key", "must not be empty"));
        float volume = (float) ConfigNodes.decimal(section.getOrDefault("volume", defaults.volume()),
                path + ".volume", 0.0, 10.0);
        float pitch = (float) ConfigNodes.decimal(section.getOrDefault("pitch", defaults.pitch()),
                path + ".pitch", 0.0, 2.0);
        Sound.Source source = source(section.getOrDefault("source", defaults.source().name()), path + ".source");
        SoundAudience audience = section.get("audience") == null ? SoundAudience.SELF
                : SoundAudience.parse(ConfigNodes.string(section.get("audience"), path + ".audience"),
                path + ".audience");
        double radius = ConfigNodes.decimal(section.getOrDefault("radius", SoundCue.DEFAULT_RADIUS),
                path + ".radius", 0.0, 256.0);
        return Optional.of(new SoundCue(build(name, volume, pitch, source), audience, radius));
    }

    private static Sound build(Key name, float volume, float pitch, Sound.Source source) {
        return Sound.sound(name, source, volume, pitch);
    }

    static Sound.Source source(Object value, String path) {
        String name = ConfigNodes.string(value instanceof Sound.Source named ? named.name() : value, path)
                .trim().toUpperCase(Locale.ROOT);
        for (Sound.Source candidate : Sound.Source.values()) {
            if (candidate.name().equals(name)) return candidate;
        }
        throw ConfigNodes.invalid(path,
                "must be one of master, music, record, weather, block, hostile, neutral, player, ambient, voice");
    }

    private static Optional<Key> key(String raw, String path, Consumer<String> warning) {
        String value = raw.trim();
        if (value.isEmpty()) return Optional.empty(); // An empty sound explicitly disables it.
        if (isKey(value)) return Optional.of(toKey(value, path));
        String legacy = LEGACY_NAMES.get(value.toUpperCase(Locale.ROOT));
        if (legacy != null) {
            warning.accept(path + ": " + value + " is a Bukkit 1.x sound name; prefer the key " + legacy + ".");
            return Optional.of(toKey(legacy, path));
        }
        throw ConfigNodes.invalid(path, "unknown sound: " + raw
                + " (use a namespaced key such as entity.player.levelup)");
    }

    /**
     * A value is treated as a key when it carries a namespace or a separator; plain words such as
     * {@code NOT_A_SOUND} are legacy enum names and never valid vanilla keys.
     */
    private static boolean isKey(String raw) {
        return raw.indexOf(':') >= 0 || raw.indexOf('.') >= 0;
    }

    private static Key toKey(String value, String path) {
        try {
            return Key.key(value);
        } catch (InvalidKeyException e) {
            throw ConfigNodes.invalid(path, "invalid sound key: " + value + " (" + e.getMessage() + ")");
        }
    }
}
