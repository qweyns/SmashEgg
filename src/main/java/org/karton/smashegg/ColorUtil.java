package org.karton.smashegg;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {}

    public static Component parse(String message) {
        return message == null || message.isEmpty() ? Component.empty() : MINI_MESSAGE.deserialize(message);
    }

    /**
     * Parses a template with <code>{placeholder}</code> values substituted. Values are
     * MiniMessage-escaped first, so a world or player name cannot inject formatting.
     */
    static Component parse(String message, Map<String, String> values) {
        if (message == null || message.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(Placeholders.apply(message, escaped(values)));
    }

    private static Map<String, String> escaped(Map<String, String> values) {
        if (values.isEmpty()) return values;
        Map<String, String> escaped = new HashMap<>(values.size() * 2);
        values.forEach((key, value) -> escaped.put(key, value == null ? "" : MINI_MESSAGE.escapeTags(value)));
        return escaped;
    }
}
