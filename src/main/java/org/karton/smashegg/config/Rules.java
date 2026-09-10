package org.karton.smashegg.config;

/**
 * Effective egg rules for one world/entity combination: the global settings with the world
 * overrides applied, then the entity overrides.
 */
public record Rules(boolean breakOnSpawner, int breakChance, int groundChance, boolean affectCreative) {

    /** Partial override; a {@code null} field means "inherit from the less specific level". */
    public record Overrides(Boolean breakOnSpawner, Integer breakChance, Integer groundChance, Boolean affectCreative) {
        public boolean isEmpty() {
            return breakOnSpawner == null && breakChance == null && groundChance == null && affectCreative == null;
        }
    }

    public Rules apply(Overrides override) {
        return new Rules(
                override.breakOnSpawner() == null ? breakOnSpawner : override.breakOnSpawner(),
                override.breakChance() == null ? breakChance : override.breakChance(),
                override.groundChance() == null ? groundChance : override.groundChance(),
                override.affectCreative() == null ? affectCreative : override.affectCreative());
    }
}
