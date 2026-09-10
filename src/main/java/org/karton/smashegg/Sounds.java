package org.karton.smashegg;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Sounds are namespaced keys on Paper: {@code org.bukkit.Sound} is a registry interface there, so
 * {@code Sound.valueOf(name)} no longer exists and cannot be resolved without a running server.
 * Keys also allow sounds from datapacks and other plugins, which the old enum never covered.
 */
final class Sounds {
    private static final float VOLUME = 1.0f;
    private static final float PITCH = 1.0f;
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
     * @param value raw config value; an empty value disables the sound
     * @return the resolved sound, or empty when the value disables it
     * @throws IllegalArgumentException if the value is neither a key nor a known legacy name
     */
    static Optional<Sound> parse(String value, Consumer<String> warning) {
        String raw = value.trim();
        if (raw.isEmpty()) return Optional.empty(); // An empty sound explicitly disables it.
        if (isKey(raw)) return Optional.of(sound(raw));
        String legacy = LEGACY_NAMES.get(raw.toUpperCase(Locale.ROOT));
        if (legacy != null) {
            warning.accept("sounds: " + raw + " is a Bukkit 1.x sound name; prefer the key " + legacy + ".");
            return Optional.of(sound(legacy));
        }
        throw new IllegalArgumentException("unknown sound: " + value
                + " (use a namespaced key such as entity.player.levelup)");
    }

    /**
     * A value is treated as a key when it carries a namespace or a separator; plain words such as
     * {@code NOT_A_SOUND} are legacy enum names and never valid vanilla keys.
     */
    private static boolean isKey(String raw) {
        return raw.indexOf(':') >= 0 || raw.indexOf('.') >= 0;
    }

    private static Sound sound(String key) {
        try {
            return Sound.sound(Key.key(key), Sound.Source.MASTER, VOLUME, PITCH);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("invalid sound key: " + key + " (" + e.getMessage() + ")");
        }
    }
}
