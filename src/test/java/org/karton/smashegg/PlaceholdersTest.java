package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceholdersTest {

    @Test
    void substitutesKnownTokensAndKeepsUnknownOnes() {
        assertEquals("мир world, моб ZOMBIE, {unknown}",
                Placeholders.apply("мир {world}, моб {entity}, {unknown}",
                        Map.of("world", "world", "entity", "ZOMBIE")));
    }

    @Test
    void leavesTextWithoutTokensUntouched() {
        assertEquals("просто текст", Placeholders.apply("просто текст", Map.of("world", "world")));
        assertEquals("", Placeholders.apply("", Map.of("world", "world")));
    }

    @Test
    void replacesRepeatedTokensEverywhere() {
        assertEquals("a-b-a", Placeholders.apply("{x}-{x2}-{x}", Map.of("x", "a", "x2", "b")));
    }

    @Test
    void replacementIsNotInterpretedAsARegex() {
        assertEquals("a$b\\c", Placeholders.apply("{value}", Map.of("value", "a$b\\c")));
    }
}
