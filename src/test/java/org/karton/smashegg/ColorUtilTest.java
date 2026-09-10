package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class ColorUtilTest {
    @Test
    void emptyInputProducesEmptyComponent() {
        assertEquals(Component.empty(), ColorUtil.parse(null));
        assertEquals(Component.empty(), ColorUtil.parse(""));
        assertEquals(Component.empty(), ColorUtil.parse(null, Map.of("a", "b")));
        assertEquals(Component.empty(), ColorUtil.parse("", Map.of("a", "b")));
    }

    @Test
    void supportsHexAndGradientMiniMessage() {
        assertDoesNotThrow(() -> ColorUtil.parse("<#E43A96>Привет <gradient:red:blue>мир</gradient>"));
        assertNotEquals(Component.text("<red>text"), ColorUtil.parse("<red>text"));
    }

    @Test
    void substitutesPlaceholders() {
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(ColorUtil.parse("мир {world}, моб {entity}", Map.of("world", "world", "entity", "ZOMBIE")));
        assertEquals("мир world, моб ZOMBIE", plain);
    }

    @Test
    void placeholderValuesCannotInjectFormatting() {
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(ColorUtil.parse("<red>{name}", Map.of("name", "<blue>зло")));
        assertEquals("<blue>зло", plain);
    }
}
