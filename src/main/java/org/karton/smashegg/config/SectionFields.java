package org.karton.smashegg.config;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Warns about unknown keys inside a config section; a typo must not be silently ignored. */
public final class SectionFields {
    private SectionFields() {}

    public static void check(Map<String, Object> section, String path, Set<String> known, Consumer<String> warning) {
        for (String key : section.keySet()) {
            if (!known.contains(key)) warning.accept("unknown config key: " + path + "." + key);
        }
    }
}
