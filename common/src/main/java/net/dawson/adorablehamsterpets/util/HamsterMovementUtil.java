package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Encapsulates movement mathematics for fleeing and taunting behaviors.
 */
public final class HamsterMovementUtil {

    private HamsterMovementUtil() {}

    /**
     * Calculates a precise point on a circle around a center entity and finds the nearest safe,
     * reachable block.
     * By varying the angle steps and radius, it can produce semi-smooth circular orbits or erratic zig-zags across the center.
     *
     * @param hamster         The hamster entity.
     * @param centerEntity    The entity to orbit around.
     * @param minRadius       The minimum distance from the center entity.
     * @param maxRadius       The maximum distance from the center entity.
     * @param minAngleDegrees The minimum angle step in degrees.
     * @param maxAngleDegrees The maximum angle step in degrees.
     * @return An Optional containing the safe BlockPos, or empty if none is found.
     */
    public static Optional<BlockPos> findOrbitingTarget(HamsterEntity hamster, Entity centerEntity, double minRadius, double maxRadius, int minAngleDegrees, int maxAngleDegrees) {
        // --- Circular/Erratic Pathing Logic ---
        double lastAngle = hamster.getLastZoomiesAngle();
        boolean isClockwise = hamster.getZoomiesIsClockwise();

        // Calculate the next angle step (degrees in radians)
        double angleStep = Math.toRadians(hamster.getRandom().nextIntBetweenInclusive(minAngleDegrees, maxAngleDegrees));
        double newAngle = isClockwise ? lastAngle + angleStep : lastAngle - angleStep;
        hamster.setLastZoomiesAngle(newAngle); // Persist the new angle on the entity

        // Calculate a new random point on the circumference of a circle whose radius is bounded
        double radius = minRadius + (hamster.getRandom().nextDouble() * (maxRadius - minRadius));
        double targetX = centerEntity.getX() + radius * Math.cos(newAngle);
        double targetZ = centerEntity.getZ() + radius * Math.sin(newAngle);

        // --- Precise Position Finding ---
        BlockPos idealPos = new BlockPos((int)targetX, (int)hamster.getY(), (int)targetZ);

        // Use findSafeSpawnPosition to locate a valid block near the ideal point
        Optional<BlockPos> finalTargetPos = HamsterPlacementUtil.findSafeSpawnPosition(idealPos, hamster.level(), 2, hamster);

        // --- LOGGING ---
        AdorableHamsterPets.LOGGER.trace(
                "[HamsterMovementUtil] findOrbitingTarget:\n  - IsClockwise: {}\n  - LastAngle(rad): {}\n  - AngleStep(rad): {}\n  - NewAngle(rad): {}\n  - Radius: {}\n  - IdealPos: {}\n  - FinalTarget: {}",
                isClockwise,
                String.format("%.2f", lastAngle),
                String.format("%.2f", angleStep),
                String.format("%.2f", newAngle),
                String.format("%.2f", radius),
                idealPos,
                finalTargetPos.map(BlockPos::toString).orElse("null")
        );

        return finalTargetPos;
    }

    /**
     * Scans for a nearby block that is safe, solid land (not water).
     *
     * @param world    The world.
     * @param startPos The position to search from.
     * @param radius   The search radius in blocks.
     * @param hamster  The hamster entity for collision contexts.
     * @return An Optional containing the nearest safe land BlockPos.
     */
    public static Optional<BlockPos> findNearbyLand(Level world, BlockPos startPos, int radius, HamsterEntity hamster) {
        BlockPos nearestLand = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.withinManhattan(startPos, radius, radius, radius)) {
            // Check that block is not submerged and the space above is air/empty
            if (world.getFluidState(checkPos).isEmpty() && world.getFluidState(checkPos.above()).isEmpty()) {
                // Check if valid surface
                if (HamsterPlacementUtil.isSafeSpawnLocation(checkPos, world, hamster)) {
                    double distSq = checkPos.distSqr(startPos);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearestLand = checkPos.immutable();
                    }
                }
            }
        }
        return Optional.ofNullable(nearestLand);
    }

    /**
     * Forces the mob to look at the target entity using this mod's fast rotation speed.
     *
     * @param mob    The observer.
     * @param target The entity to look at.
     */
    public static void faceEntity(Mob mob, Entity target) {
        if (mob != null && target != null) {
            mob.getLookControl().setLookAt(target, 25.0f, 25.0f);
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
    public static void facePosition(Mob mob, double x, double y, double z) {
        if (mob != null) {
            mob.getLookControl().setLookAt(x, y, z, 25.0f, 25.0f);
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
        return runner.distanceToSqr(chaser) < minFleeDist * minFleeDist;
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
        return runner.distanceToSqr(chaser) > maxFleeDist * maxFleeDist;
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
    public static Vec3 findFleePosition(PathfinderMob runner, Entity chaser, double minDistance, double maxDistance) {
        // Find a position away from the chaser
        // FuzzyTargeting.findFrom creates a target vector away from the provided start pos (the chaser)
        // arg 2: horizontal spread, arg 3: vertical spread
        return LandRandomPos.getPosAway(runner, (int) maxDistance, 7, chaser.position());
    }

    /**
     * Determines if a hamster is in a state that universally forbids self-directed locomotion.
     *
     * @param hamster The hamster to check.
     * @return True if the hamster should not move under its own AI.
     */
    public static boolean shouldNotMove(HamsterEntity hamster) {
        return shouldNotMove(
                hamster.isNoAi(),
                hamster.isOrderedToSit(),
                hamster.isFrozenMovement(),
                hamster.isCelebratingBaby(),
                hamster.isCelebratingDiamond());
    }

    static boolean shouldNotMove(
            boolean aiDisabled,
            boolean sitting,
            boolean frozenMovement,
            boolean celebratingBaby,
            boolean celebratingDiamond) {
        return aiDisabled || sitting || frozenMovement || celebratingBaby || celebratingDiamond;
    }

    /**
     * Determines if the hamster is in a state that forbids following another entity.
     *
     * @param hamster The hamster to check.
     * @return True if the hamster should not follow, false otherwise.
     */
    public static boolean shouldNotFollow(HamsterEntity hamster) {
        return shouldNotFollow(
                shouldNotMove(hamster),
                hamster.isPlayingTag(),
                hamster.isWanderModeActive());
    }

    static boolean shouldNotFollow(
            boolean shouldNotMove,
            boolean playingTag,
            boolean wanderModeActive) {
        return shouldNotMove || playingTag || wanderModeActive;
    }

    /**
     * Determines if the hamster should teleport to the target.
     * Checks if the hamster is not leashed, not a passenger, and is far enough away.
     * Dynamically adjusts for certain states.
     *
     * @param hamster The hamster to check.
     * @param target  The target entity to follow.
     * @return True if the hamster should teleport.
     */
    public static boolean shouldTeleportTo(HamsterEntity hamster, Entity target) {
        // Fast Fail
        if (hamster.isLeashed() || hamster.isPassenger()) {
            return false;
        }

        // Use pre-squared values for performance
        double maxDistSq = 144.0;

        // +5 blocks for certain states
        if (hamster.hasGreenBeanBuff() || hamster.getAggressionState() == HamsterEntity.AggressionState.MENACE) {
            maxDistSq = 289.0;
        }

        // Final distance check
        return hamster.distanceToSqr(target) > maxDistSq;
    }

    /**
     * Attempts to safely teleport the hamster to the target entity using a safe placement algorithm.
     * Intercepts long-distance AI teleports to prevent vanilla chunk tracking race conditions causing
     * server/client desync.
     *
     * @param hamster The hamster to teleport.
     * @param target The target entity to teleport to.
     * @return Result of the teleport attempt.
     */
    public static TeleportResult tryTeleportTo(HamsterEntity hamster, Entity target) {
        return tryTeleportTo(hamster, target, true);
    }

    /**
     * Attempts to safely teleport the hamster to the target entity.
     *
     * @param hamster The hamster to teleport.
     * @param target The target entity to teleport to.
     * @param allowPocketRescue Whether long-distance pocket rescue may replace direct teleportation.
     * @return Result of the teleport attempt.
     */
    public static TeleportResult tryTeleportTo(
            HamsterEntity hamster, Entity target, boolean allowPocketRescue) {
        Level world = hamster.level();
        if (world.isClientSide()) {
            return TeleportResult.FAILED;
        }

        // --- Sledgehammer Server/Client Sync ---
        // Force Pocket Rescue Protocol for teleports more than 32 blocks
        if (allowPocketRescue
                && Configs.AHP_MAIN.enableTeleportRescue
                && hamster.distanceToSqr(target) > 1024.0) {
            Player ownerPlayer = null;

            if (target instanceof Player playerTarget) {
                ownerPlayer = playerTarget;
            } else if (target instanceof HamsterEntity parentHamster && parentHamster.getOwner() instanceof Player parentOwner) {
                ownerPlayer = parentOwner;
            }

            if (ownerPlayer instanceof PlayerEntityAccessor accessor) {
                CompoundTag nbt = new CompoundTag();
                hamster.saveWithoutId(nbt); // Save full state

                // Save target (parent or player)
                nbt.putUUID("AHPTransitTargetUuid", target.getUUID());

                accessor.ahp$getInTransitHamsters().add(nbt);
                accessor.ahp$setTransitTimer(15); // Wait 15 ticks for client to load
                hamster.discard();
                AdorableHamsterPets.LOGGER.debug(
                        "[Teleport Rescue Protocol] Hamster {} intercepted. (Without this, any babies currently "
                                + "following {} would now be invisible).",
                        hamster.getId(),
                        hamster.getId());
                return TeleportResult.QUEUED_POCKET_RESCUE;
            }
        }

        // --- Standard Vanilla Teleport ---
        // Apply a random offset so multiple hamsters don't all teleport into the exact same BlockPos and cause massive collision lag
        int offsetX = hamster.getRandom().nextIntBetweenInclusive(-2, 2);
        int offsetZ = hamster.getRandom().nextIntBetweenInclusive(-2, 2);
        BlockPos searchStart = target.blockPosition().offset(offsetX, 0, offsetZ);

        Optional<BlockPos> safePos =
                HamsterPlacementUtil.findSafeSpawnPosition(searchStart, world, 3, hamster);
        if (safePos.isEmpty()) {
            return TeleportResult.FAILED;
        }

        BlockPos destination = safePos.get();
        hamster.moveTo(
                destination.getX() + 0.5,
                destination.getY(),
                destination.getZ() + 0.5,
                hamster.getYRot(),
                hamster.getXRot());
        hamster.getNavigation().stop();
        return TeleportResult.TELEPORTED;
    }

    public enum TeleportResult {
        TELEPORTED,
        QUEUED_POCKET_RESCUE,
        FAILED
    }
}
