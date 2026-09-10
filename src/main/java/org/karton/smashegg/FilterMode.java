package org.karton.smashegg;

import java.util.Locale;

/** How the configured entity list is applied to spawners. */
enum FilterMode {
    /** Listed mobs are forbidden, everything else is allowed. */
    BLACKLIST,
    /** Listed mobs are allowed, everything else is forbidden. */
    WHITELIST;

    static FilterMode parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (FilterMode mode : values()) {
            if (mode.name().equals(normalized)) return mode;
        }
        throw new IllegalArgumentException("must be blacklist or whitelist (found: " + value + ")");
    }

    /** @return true when the mob may not be put into a spawner */
    boolean blocks(boolean listed) {
        return this == WHITELIST ? !listed : listed;
    }
}
