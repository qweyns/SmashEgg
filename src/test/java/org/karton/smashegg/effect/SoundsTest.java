package org.karton.smashegg.effect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.junit.jupiter.api.Test;

class SoundsTest {
    private static final String PATH = "sounds.test";
    private final List<String> warnings = new ArrayList<>();

    private Sound parse(Object value) {
        return Sounds.parse(value, PATH, warnings::add).orElseThrow();
    }

    @Test
    void emptyValueDisablesTheSound() {
        assertTrue(Sounds.parse("", PATH, warnings::add).isEmpty());
        assertTrue(Sounds.parse("   ", PATH, warnings::add).isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void resolvesPlainNamespacedAndCustomKeys() {
        assertEquals(Key.key("entity.player.levelup"), parse("entity.player.levelup").name());
        assertEquals(Key.key("minecraft", "block.anvil.land"), parse("minecraft:block.anvil.land").name());
        assertEquals(Key.key("my_pack", "custom_sound"), parse("my_pack:custom_sound").name());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void playsAtFullVolumeThroughTheMasterSourceByDefault() {
        Sound sound = parse("entity.villager.no");
        assertEquals(Sound.Source.MASTER, sound.source());
        assertEquals(1.0f, sound.volume());
        assertEquals(1.0f, sound.pitch());
    }

    @Test
    void sectionFormOverridesVolumePitchAndSource() {
        Sound sound = parse(Map.of("key", "block.glass.break", "volume", 0.25, "pitch", 2, "source", "RECORD"));
        assertEquals(Key.key("block.glass.break"), sound.name());
        assertEquals(0.25f, sound.volume());
        assertEquals(2.0f, sound.pitch());
        assertEquals(Sound.Source.RECORD, sound.source());
    }

    @Test
    void sectionDefaultsAreAppliedPerField() {
        Sound sound = parse(Map.of("key", "block.glass.break"));
        assertEquals(1.0f, sound.volume());
        assertEquals(1.0f, sound.pitch());
        assertEquals(Sound.Source.MASTER, sound.source());
    }

    @Test
    void configuredDefaultsApplyToPlainKeys() {
        SoundDefaults defaults = new SoundDefaults(0.2f, 1.5f, Sound.Source.PLAYER);
        Sound sound = Sounds.parse("entity.player.levelup", PATH, defaults, warnings::add).orElseThrow();
        assertEquals(0.2f, sound.volume());
        assertEquals(1.5f, sound.pitch());
        assertEquals(Sound.Source.PLAYER, sound.source());
    }

    @Test
    void translatesKnownBukkitNamesAndWarns() {
        assertEquals(Key.key("block.glass.break"), parse("block_glass_break").name());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("block.glass.break"), warnings::toString);
    }

    @Test
    void rejectsUnknownBukkitNamesWithAnActionableReason() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parse("NOT_A_SOUND"));
        assertTrue(e.getMessage().contains(PATH), e::getMessage);
        assertTrue(e.getMessage().contains("NOT_A_SOUND"), e::getMessage);
        assertTrue(e.getMessage().contains("entity.player.levelup"), e::getMessage);
    }

    @Test
    void sectionFormCanTargetNearbyPlayers() {
        SoundCue cue = Sounds.parseCue(
                Map.of("key", "block.glass.break", "audience", "nearby", "radius", 24),
                PATH, warnings::add).orElseThrow();
        assertEquals(SoundAudience.NEARBY, cue.audience());
        assertEquals(24.0, cue.radius());
        assertEquals(Key.key("block.glass.break"), cue.name());
    }

    @Test
    void plainKeyDefaultsToSelfAudience() {
        SoundCue cue = Sounds.parseCue("entity.player.levelup", PATH, warnings::add).orElseThrow();
        assertEquals(SoundAudience.SELF, cue.audience());
        assertEquals(SoundCue.DEFAULT_RADIUS, cue.radius());
    }

    @Test
    void rejectsMalformedKeysAndWrongTypes() {
        assertThrows(IllegalArgumentException.class, () -> parse("minecraft:BAD KEY!"));
        assertThrows(IllegalArgumentException.class, () -> parse(42));
        assertThrows(IllegalArgumentException.class, () -> parse(Map.of("volume", 1)));
        assertThrows(IllegalArgumentException.class, () -> parse(Map.of("key", "block.glass.break", "pitch", 3)));
        assertThrows(IllegalArgumentException.class, () -> parse(Map.of("key", "block.glass.break", "source", "loud")));
    }
}
