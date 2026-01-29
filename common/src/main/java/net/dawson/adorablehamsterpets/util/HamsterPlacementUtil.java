package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LandPathNodeMakerInvoker;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * A centralized utility for handling the safe placement, spawning, and rescuing of Hamster entities.
 * <p>
 * This class abstracts the logic for validating world coordinates against hamster-specific
 * collision and hazard rules, ensuring consistent behavior across spawning, dismounting,
 * bed exiting, and anti-suffocation mechanics.
 */
public class HamsterPlacementUtil {

    private static final Set<PathNodeType> HAZARDOUS_FLOOR_TYPES = EnumSet.of(
            PathNodeType.LAVA,
            PathNodeType.DAMAGE_FIRE,
            PathNodeType.DANGER_FIRE,
            PathNodeType.POWDER_SNOW,
            PathNodeType.DAMAGE_OTHER,
            PathNodeType.DANGER_OTHER,
            PathNodeType.DAMAGE_CAUTIOUS,
            PathNodeType.WATER
    );

    /**
     * Checks if the hamster needs to be rescued from suffocation and performs the rescue if possible.
     *
     * @param hamster The hamster to check.
     */
    public static void trySuffocationRescue(HamsterEntity hamster) {
        // Only run if grace period is active AND the hamster is actually inside a wall
        if (hamster.suffocationGracePeriod > 0 && hamster.isInsideWall()) {
            World world = hamster.getWorld();
            BlockPos currentPos = hamster.getBlockPos();

            // Use findSafeSpawnPosition with a small radius (3 blocks)
            Optional<BlockPos> safePosOpt = findSafeSpawnPosition(currentPos, world, 3, hamster);

            safePosOpt.ifPresent(safePos -> {
                // Found a safe spot, teleport hamster
                hamster.refreshPositionAndAngles(
                        safePos.getX() + 0.5,
                        safePos.getY(),
                        safePos.getZ() + 0.5,
                        hamster.getYaw(),
                        hamster.getPitch()
                );

                // Stop momentum
                hamster.setVelocity(0, 0, 0);
                hamster.velocityDirty = true;

                AdorableHamsterPets.LOGGER.debug("[HamsterSelfRescue] Hamster {} rescued from {} to safe location {}.",
                        hamster.getId(), currentPos, safePos);

                // End grace period immediately
                hamster.suffocationGracePeriod = 0;
            });
        }
    }

    /**
     * Finds a safe spawn position for the hamster near an initial target position.
     * The search is performed in stages for efficiency and logical placement:
     * 1. Checks the initial target position itself.
     * 2. Checks a few blocks directly above the target.
     * 3. Performs a horizontal spiral search outwards on the same Y-level.
     *
     * @param initialTarget The desired starting point for the search.
     * @param world         The world where the search is performed.
     * @param searchRadius  The maximum horizontal radius for the spiral search.
     * @param hamster       The hamster entity (used for collision context).
     * @return An Optional containing the first safe BlockPos found.
     */
    public static Optional<BlockPos> findSafeSpawnPosition(BlockPos initialTarget, World world, int searchRadius, HamsterEntity hamster) {
        return findSafeSpawnPosition(initialTarget, world, searchRadius, Collections.emptySet(), hamster);
    }

    /**
     * Overload that accepts a set of positions to ignore.
     */
    public static Optional<BlockPos> findSafeSpawnPosition(BlockPos initialTarget, World world, int searchRadius, Set<BlockPos> occupiedPositions, HamsterEntity hamster) {
        // --- Stage 1: Initial Target Check ---
        if (isSafeSpawnLocation(initialTarget, world, hamster) && !occupiedPositions.contains(initialTarget)) {
            return Optional.of(initialTarget);
        }

        // --- Stage 2: Vertical Vicinity Check (Upwards) ---
        for (int i = 1; i <= 3; i++) {
            BlockPos abovePos = initialTarget.up(i);
            if (isSafeSpawnLocation(abovePos, world, hamster) && !occupiedPositions.contains(abovePos)) {
                return Optional.of(abovePos);
            }
        }

        // --- Stage 3: Horizontal Spiral Search ---
        for (int r = 1; r <= searchRadius; r++) {
            for (int i = -r; i <= r; i++) {
                for (int j = -r; j <= r; j++) {
                    // Only check "ring" of the spiral
                    if (Math.abs(i) != r && Math.abs(j) != r) {
                        continue;
                    }
                    BlockPos checkPos = initialTarget.add(i, 0, j);
                    if (isSafeSpawnLocation(checkPos, world, hamster) && !occupiedPositions.contains(checkPos)) {
                        return Optional.of(checkPos);
                    }
                }
            }
        }

        // --- Stage 4: Failure ---
        return Optional.empty();
    }

    /**
     * Checks if a given block position is a safe location for a hamster to exist.
     * A location is safe if:
     * 1. The block itself is not a hazard.
     * 2. The block below is not a hazard.
     * 3. The block below has a collision shape to stand on.
     * 4. The two blocks at the spawn position (for feet and head) have no collision shape *for this specific hamster*.
     */
    public static boolean isSafeSpawnLocation(BlockPos pos, World world, HamsterEntity hamster) {
        // --- 1. Check body position for hazards ---
        PathNodeType bodyType = LandPathNodeMakerInvoker.callGetCommonNodeType(world, pos);
        if (HAZARDOUS_FLOOR_TYPES.contains(bodyType)) {
            return false;
        }

        // --- 2. Check for a valid, non-hazardous floor ---
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);

        // Use invoker to get pathfinding node type of the floor.
        PathNodeType floorType = LandPathNodeMakerInvoker.callGetCommonNodeType(world, floorPos);
        if (HAZARDOUS_FLOOR_TYPES.contains(floorType)) {
            return false; // Floor is a known hazard
        }

        // Ensure physical surface to stand on
        if (floorState.getCollisionShape(world, floorPos).isEmpty()) {
            return false;
        }

        // --- 3. Check for empty body/head space using entity-specific context ---
        // The block is considered safe if it has no collision for the HamsterEntity
        ShapeContext entityContext = ShapeContext.of(hamster);
        return world.getBlockState(pos).getCollisionShape(world, pos, entityContext).isEmpty() &&
                world.getBlockState(pos.up()).getCollisionShape(world, pos.up(), entityContext).isEmpty();
    }
}