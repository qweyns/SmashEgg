package org.karton.smashegg.util;

import java.util.Locale;
import org.bukkit.Material;

/** Material names are available at runtime, including eggs added after our compile API. */
final class EggTypes {
    private static final String SUFFIX = "_SPAWN_EGG";

    private EggTypes() {}

    static boolean isEgg(Material material) {
        return material != null && material.name().endsWith(SUFFIX);
    }

    static String fromMaterial(Material material) {
        if (!isEgg(material)) throw new IllegalArgumentException("Not a spawn egg: " + material);
        String name = material.name();
        return normalize(name.substring(0, name.length() - SUFFIX.length()));
    }

    static String normalize(String name) {
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "MUSHROOM_COW" -> "MOOSHROOM";
            case "SNOWMAN" -> "SNOW_GOLEM";
            default -> name.trim().toUpperCase(Locale.ROOT);
        };
    }
}
