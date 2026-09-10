package org.karton.smashegg;

import java.util.Locale;

/** Where a message is shown. Console senders always fall back to {@link #CHAT}. */
enum MessageOutput {
    CHAT, ACTIONBAR, TITLE, NONE;

    static MessageOutput parse(String value, String path) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (MessageOutput output : values()) {
            if (output.name().equals(normalized)) return output;
        }
        throw ConfigNodes.invalid(path, "must be one of chat, actionbar, title, none (found: " + value + ")");
    }
}
