package org.karton.smashegg;

/**
 * Particles spawned at the player when an effect fires. The name is kept as a string and resolved
 * against the running server on first use: {@code org.bukkit.Particle} implements {@code Keyed} and
 * may stop being an enum, so the registry is the portable lookup.
 */
record ParticleSpec(String name, int count, double spread, double speed) {}
