package org.karton.smashegg;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Sounds are namespaced keys on Paper: {@code org.bukkit.Sound} is a registry interface there, so
 * {@code Sound.valueOf(name)} does not exist and cannot be resolved without a running server.
 * Keys also allow sounds from datapacks and other plugins, which the old enum never covered.
 */
final class Sounds {
    private static final float DEFAULT_VOLUME = 1.0f;
    private static final float DEFAULT_PITCH = 1.0f;
    private static final Sound.Source DEFAULT_SOURCE = Sound.Source.MASTER;
    private static final Set<String> FIELDS = Set.of("key", "volume", "pitch", "source");
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

    /**
     * Accepts either a plain key string or a section with key/volume/pitch/source.
     *
     * @param value raw config value; an empty string disables the sound
     * @param path  config path, used in error messages
     * @return the resolved sound, or empty when the value disables it
     * @throws IllegalArgumentException if the value is neither a key nor a known legacy name
     */
    static Optional<Sound> parse(Object value, String path, Consumer<String> warning) {
        if (value instanceof String text) {
            return key(text, path, warning)
                    .map(name -> build(name, DEFAULT_VOLUME, DEFAULT_PITCH, DEFAULT_SOURCE));
        }
        Map<String, Object> section = ConfigNodes.section(value, path);
        SectionFields.check(section, path, FIELDS, warning);
        Key name = key(ConfigNodes.string(section.get("key"), path + ".key"), path + ".key", warning)
                .orElseThrow(() -> ConfigNodes.invalid(path + ".key", "must not be empty"));
        float volume = (float) ConfigNodes.decimal(section.getOrDefault("volume", DEFAULT_VOLUME),
                path + ".volume", 0.0, 10.0);
        float pitch = (float) ConfigNodes.decimal(section.getOrDefault("pitch", DEFAULT_PITCH),
                path + ".pitch", 0.0, 2.0);
        Sound.Source source = source(section.getOrDefault("source", DEFAULT_SOURCE.name()), path + ".source");
        return Optional.of(build(name, volume, pitch, source));
    }

    private static Sound build(Key name, float volume, float pitch, Sound.Source source) {
        return Sound.sound(name, source, volume, pitch);
    }

    private static Sound.Source source(Object value, String path) {
        String name = ConfigNodes.string(value, path).trim().toUpperCase(Locale.ROOT);
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
