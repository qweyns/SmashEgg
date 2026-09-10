package org.karton.smashegg.text;

import static org.junit.jupiter.api.Assertions.*;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ColorUtilTest {
    @Test
    void emptyInputProducesEmptyComponent() {
        assertEquals(Component.empty(), ColorUtil.parse(null));
        assertEquals(Component.empty(), ColorUtil.parse(""));
    }

    @Test
    void supportsHexAndGradientMiniMessage() {
        assertDoesNotThrow(() -> ColorUtil.parse("<#E43A96>Привет <gradient:red:blue>мир</gradient>"));
        assertNotEquals(Component.text("<red>text"), ColorUtil.parse("<red>text"));
    }
}
