package org.karton.smashegg.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {}

    public static Component parse(String message) {
        return message == null || message.isEmpty() ? Component.empty() : MINI_MESSAGE.deserialize(message);
    }
}
