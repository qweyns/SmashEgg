package org.karton.smashegg;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class EggTypesTest {
    @Test
    void recognizesEveryEggInRuntimeApiWithoutAHandMaintainedList() {
        int eggs = 0;
        for (Material material : Material.values()) {
            if (material.name().endsWith("_SPAWN_EGG")) {
                assertTrue(EggTypes.isEgg(material));
                assertFalse(EggTypes.fromMaterial(material).isEmpty());
                eggs++;
            }
        }
        assertTrue(eggs > 50);
    }

    @Test
    void rejectsOtherItems() {
        assertFalse(EggTypes.isEgg(null));
        assertFalse(EggTypes.isEgg(Material.EGG));
        assertFalse(EggTypes.isEgg(Material.STONE));
        assertThrows(IllegalArgumentException.class, () -> EggTypes.fromMaterial(Material.EGG));
    }

    @Test
    void resolvesOldAndNewEntityAliases() {
        assertEquals("MOOSHROOM", EggTypes.fromMaterial(Material.MOOSHROOM_SPAWN_EGG));
        assertEquals("MOOSHROOM", EggTypes.normalize("MUSHROOM_COW"));
        assertEquals("SNOW_GOLEM", EggTypes.normalize("SNOWMAN"));
        assertEquals("ZOMBIE", EggTypes.fromMaterial(Material.ZOMBIE_SPAWN_EGG));
    }
}
