package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Encapsulates movement mathematics for fleeing and taunting behaviors.
 */
public final class HamsterMovementUtil {

    private HamsterMovementUtil() {}

    /**
     * Forces the mob to look at the target entity using this mod's fast rotation speed.
     *
     * @param mob    The observer.
     * @param target The entity to look at.
     */
    public static void faceEntity(MobEntity mob, Entity target) {
        if (mob != null && target != null) {
            mob.getLookControl().lookAt(target, 25.0f, 25.0f);
        }
    }

    /**
     * Forces the mob to look at a specific world coordinate using this mod's fast rotation speed.
     *
     * @param mob The observer.
     * @param x   Target X.
     * @param y   Target Y.
     * @param z   Target Z.
     */
    public static void facePosition(MobEntity mob, double x, double y, double z) {
        if (mob != null) {
            mob.getLookControl().lookAt(x, y, z, 25.0f, 25.0f);
        }
    }

    /**
     * Determines if the runner is too close to the chaser and needs to flee.
     *
     * @param runner      The entity running away.
     * @param chaser      The entity chasing.
     * @param minFleeDist The minimum safe distance.
     * @return True if the runner is too close.
     */
    public static boolean shouldFlee(Entity runner, Entity chaser, double minFleeDist) {
        return runner.squaredDistanceTo(chaser) < minFleeDist * minFleeDist;
    }

    /**
     * Determines if the runner has reached a safe distance and should stop fleeing (start taunting).
     * Uses hysteresis (maxFleeDist) to prevent jittery start/stop behavior.
     *
     * @param runner      The entity running away.
     * @param chaser      The entity chasing.
     * @param maxFleeDist The distance at which the runner feels safe enough to stop.
     * @return True if the runner is safe.
     */
    public static boolean shouldStopFleeing(Entity runner, Entity chaser, double maxFleeDist) {
        return runner.squaredDistanceTo(chaser) > maxFleeDist * maxFleeDist;
    }

    /**
     * Finds a valid position for the runner to flee to, away from the chaser.
     *
     * @param runner      The entity running away.
     * @param chaser      The entity chasing.
     * @param minDistance Minimum distance for the generated path point.
     * @param maxDistance Maximum distance for the generated path point.
     * @return A Vec3d coordinate to run to, or null if no path found.
     */
    @Nullable
    public static Vec3d findFleePosition(PathAwareEntity runner, Entity chaser, double minDistance, double maxDistance) {
        // Find a position away from the chaser
        // FuzzyTargeting.findFrom creates a target vector away from the provided start pos (the chaser)
        // arg 2: horizontal spread, arg 3: vertical spread
        return FuzzyTargeting.findFrom(runner, (int) maxDistance, 7, chaser.getPos());
    }

    /**
     * Determines if the hamster is in a state that forbids following another entity.
     *
     * @param hamster The hamster to check.
     * @return True if the hamster should not follow, false otherwise.
     */
    public static boolean shouldNotFollow(HamsterEntity hamster) {
        return hamster.isSitting() ||
                hamster.isSleeping() ||
                hamster.isKnockedOut() ||
                hamster.isSulking() ||
                hamster.isCelebratingDiamond() ||
                hamster.isCelebratingRetrieval() ||
                hamster.isPlayingTag() ||
                hamster.isCelebratingBaby() ||
                hamster.isWanderModeActive();
    }

    /**
     * Determines if the hamster should teleport to the target.
     * Checks if the hamster is not leashed, not a passenger, and is far enough away.
     *
     * @param hamster The hamster to check.
     * @param target  The target entity to follow.
     * @return True if the hamster should teleport.
     */
    public static boolean shouldTeleportTo(HamsterEntity hamster, Entity target) {
        return !hamster.isLeashed() && !hamster.hasVehicle() && hamster.squaredDistanceTo(target) > 144.0;
    }

    /**
     * Attempts to safely teleport the hamster to the target entity using a safe placement algorithm.
     * Intercepts long-distance AI teleports to prevent vanilla chunk tracking race conditions causing Server/Client desync.
     *
     * @param hamster The hamster to teleport.
     * @param target  The target entity to teleport to.
     */
    public static void tryTeleportTo(HamsterEntity hamster, Entity target) {
        World world = hamster.getWorld();
        if (world.isClient()) return;

        // --- Sledgehammer Server/Client Sync ---
        // Force Pocket Rescue Protocol for teleports more than 32 blocks
        if (Configs.AHP.enableTeleportRescue && hamster.squaredDistanceTo(target) > 1024.0) {
            PlayerEntity ownerPlayer = null;

            if (target instanceof PlayerEntity playerTarget) {
                ownerPlayer = playerTarget;
            } else if (target instanceof HamsterEntity parentHamster && parentHamster.getOwner() instanceof PlayerEntity parentOwner) {
                ownerPlayer = parentOwner;
            }

            if (ownerPlayer instanceof PlayerEntityAccessor accessor) {
                NbtCompound nbt = new NbtCompound();
                hamster.writeNbt(nbt); // Save full state

                // Save target (parent or player)
                nbt.putUuid("AHPTransitTargetUuid", target.getUuid());

                accessor.ahp$getInTransitHamsters().add(nbt);
                accessor.ahp$setTransitTimer(15); // Wait 15 ticks for client to load
                hamster.discard();
                AdorableHamsterPets.LOGGER.debug("[Teleport Rescue Protocol] Hamster {} intercepted. (Without this, any babies currently following {} would now be invisible).", hamster.getId(), hamster.getId());
                return; // Abort vanilla teleport
            }
        }

        // --- Standard Vanilla Teleport ---
        // Apply a random offset so multiple hamsters don't all teleport into the exact same BlockPos and cause massive collision lag
        int offsetX = hamster.getRandom().nextBetween(-2, 2);
        int offsetZ = hamster.getRandom().nextBetween(-2, 2);
        BlockPos searchStart = target.getBlockPos().add(offsetX, 0, offsetZ);

        Optional<BlockPos> safePosOpt = HamsterPlacementUtil.findSafeSpawnPosition(searchStart, world, 3, hamster);

        safePosOpt.ifPresent(safePos -> {
            hamster.refreshPositionAndAngles(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, hamster.getYaw(), hamster.getPitch());
            hamster.getNavigation().stop();
        });
    }
}