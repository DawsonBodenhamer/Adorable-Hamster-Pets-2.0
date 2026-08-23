package net.dawson.adorablehamsterpets.util;

import net.minecraft.entity.SpawnReason;

/**
 * Pure Redstone Fever policy contracts shared by runtime code and private tests.
 */
final class RedstoneFeverPolicy {

    static boolean isEligibleFreshSpawnReason(SpawnReason spawnReason) {
        return spawnReason != SpawnReason.SPAWN_EGG;
    }

    static boolean isEligiblePlayerState(
            boolean alive,
            boolean removed,
            boolean creative,
            boolean spectator,
            boolean invulnerable) {
        return alive && !removed && !creative && !spectator && !invulnerable;
    }

    private RedstoneFeverPolicy() {}
}
