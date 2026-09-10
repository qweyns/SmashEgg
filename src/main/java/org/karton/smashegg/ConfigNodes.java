package org.karton.smashegg;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Typed access to YAML nodes. Every failure carries the config path so the console message says
 * which line to fix. A section may arrive as a {@link ConfigurationSection} or as a plain map,
 * depending on whether it came from the file or from the bundled defaults.
 */
final class ConfigNodes {
    private ConfigNodes() {}

    /** @return the node as a String-keyed map; an absent node becomes an empty map */
    static Map<String, Object> section(Object value, String path) {
        if (value == null) return Map.of();
        if (value instanceof ConfigurationSection section) return section.getValues(false);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw invalid(path, "keys must be strings");
                result.put(key, entry.getValue());
            }
            return result;
        }
        throw invalid(path, "must be a YAML section");
    }

    static String string(Object value, String path) {
        if (!(value instanceof String text)) throw invalid(path, "must be a string");
        return text;
    }

    static boolean bool(Object value, String path) {
        if (!(value instanceof Boolean flag)) throw invalid(path, "must be true or false");
        return flag;
    }

    static int integer(Object value, String path, int min, int max) {
        if (!(value instanceof Integer number) || number < min || number > max) {
            throw invalid(path, "must be an integer between " + min + " and " + max);
        }
        return number;
    }

    /** Accepts any YAML number: an int arrives as Integer, a decimal as Double, a default as Float. */
    static double decimal(Object value, String path, double min, double max) {
        if (!(value instanceof Number number)) {
            throw invalid(path, "must be a number between " + min + " and " + max);
        }
        double result = number.doubleValue();
        if (result < min || result > max) throw invalid(path, "must be a number between " + min + " and " + max);
        return result;
    }

    /** Optional value: {@code null} stays {@code null} so an override can inherit from above. */
    static Boolean optionalBool(Object value, String path) {
        return value == null ? null : bool(value, path);
    }

    static Integer optionalPercent(Object value, String path) {
        return value == null ? null : integer(value, path, 0, 100);
    }

    static IllegalArgumentException invalid(String path, String reason) {
        return new IllegalArgumentException(path + ": " + reason);
    }
}
