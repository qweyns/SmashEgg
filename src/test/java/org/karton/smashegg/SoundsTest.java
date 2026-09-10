package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.junit.jupiter.api.Test;

class SoundsTest {
    private final List<String> warnings = new ArrayList<>();

    @Test
    void emptyValueDisablesTheSound() {
        assertTrue(Sounds.parse("", warnings::add).isEmpty());
        assertTrue(Sounds.parse("   ", warnings::add).isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void resolvesPlainNamespacedAndCustomKeys() {
        assertEquals(Key.key("entity.player.levelup"),
                Sounds.parse("entity.player.levelup", warnings::add).orElseThrow().name());
        assertEquals(Key.key("minecraft", "block.anvil.land"),
                Sounds.parse("minecraft:block.anvil.land", warnings::add).orElseThrow().name());
        assertEquals(Key.key("my_pack", "custom_sound"),
                Sounds.parse("my_pack:custom_sound", warnings::add).orElseThrow().name());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void playsAtFullVolumeThroughTheMasterSource() {
        Sound sound = Sounds.parse("entity.villager.no", warnings::add).orElseThrow();
        assertEquals(Sound.Source.MASTER, sound.source());
        assertEquals(1.0f, sound.volume());
        assertEquals(1.0f, sound.pitch());
    }

    @Test
    void translatesKnownBukkitNamesAndWarns() {
        Sound sound = Sounds.parse("block_glass_break", warnings::add).orElseThrow();
        assertEquals(Key.key("block.glass.break"), sound.name());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("block.glass.break"), warnings::toString);
    }

    @Test
    void rejectsUnknownBukkitNamesWithAnActionableReason() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Sounds.parse("NOT_A_SOUND", warnings::add));
        assertTrue(e.getMessage().contains("NOT_A_SOUND"), e::getMessage);
        assertTrue(e.getMessage().contains("entity.player.levelup"), e::getMessage);
    }

    @Test
    void rejectsMalformedKeys() {
        assertThrows(IllegalArgumentException.class, () -> Sounds.parse("minecraft:BAD KEY!", warnings::add));
    }
}
