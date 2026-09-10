package org.karton.smashegg;

import java.util.Locale;

/** What happens to the egg when a check fails. */
enum FailureAction {
    /** Remove one egg from the hand. */
    CONSUME,
    /** Leave the egg in the hand; only the action is cancelled. */
    KEEP,
    /** Remove one egg and drop it as an item at the player. */
    DROP;

    static FailureAction parse(String value, String path) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (FailureAction action : values()) {
            if (action.name().equals(normalized)) return action;
        }
        throw ConfigNodes.invalid(path, "must be one of consume, keep, drop (found: " + value + ")");
    }
}
