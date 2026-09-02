package net.dawson.adorablehamsterpets.util;

import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Pure Redstone Fever policy contracts shared by runtime code and private tests.
 */
final class RedstoneFeverPolicy {

    static boolean isEligibleFreshSpawnReason(EntitySpawnReason spawnReason) {
        return spawnReason != EntitySpawnReason.SPAWN_ITEM_USE;
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
