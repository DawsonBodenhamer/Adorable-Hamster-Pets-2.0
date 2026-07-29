package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
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

    private static final int DROWNING_COLUMN_RADIUS = 1;
    private static final int DROWNING_RING_RADIUS = 4;
    private static final int DROWNING_VERTICAL_RANGE = 16;

    /**
     * Checks if the hamster needs to be rescued from suffocation and performs the rescue if possible.
     *
     * @param hamster The hamster to check.
     */
    public static void trySuffocationRescue(HamsterEntity hamster) {
        // Only run if grace period active and inside wall
        if (hamster.suffocationGracePeriod > 0 && hamster.isInsideWall()) {
            World world = hamster.getWorld();
            BlockPos currentPos = hamster.getBlockPos();

            // Small radius
            Optional<BlockPos> safePosOpt = findSafeSpawnPosition(currentPos, world, 3, hamster);

            safePosOpt.ifPresent(safePos -> {
                // Found a safe spot, request teleport to sync with client
                hamster.requestTeleport(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

                // Stop momentum
                hamster.setVelocity(0, 0, 0);
                hamster.velocityDirty = true;

                AdorableHamsterPets.LOGGER.debug("[HamsterSelfRescue] Hamster {} rescued from {} to safe location {}.",
                        hamster.getId(), currentPos, safePos);

                // End grace period immediately
                hamster.suffocationGracePeriod = 0;

                // Force explicit delayed positional update to prevent Server/Client desync
                long currentWorldTime = world.getTime();
                hamster.scheduleTask(currentWorldTime + 5, "sledgehammer_teleport_sync", () -> {
                    if (hamster.isAlive() && !hamster.isRemoved()) {
                        hamster.requestTeleport(hamster.getX(), hamster.getY(), hamster.getZ());
                    }
                });
            });
        }
    }

    /**
     * Attempts to move a drowning hamster directly to a nearby dry and collision-safe position.
     *
     * @param hamster The drowning hamster.
     * @return True only when direct teleportation reaches the verified destination.
     */
    public static boolean tryDrowningRescue(HamsterEntity hamster) {
        if (hamster.getWorld().isClient()) {
            return false;
        }

        World world = hamster.getWorld();
        BlockPos origin = hamster.getBlockPos();
        Optional<BlockPos> safePos = findDrowningRescuePosition(origin, world, hamster);
        if (safePos.isEmpty()) {
            return false;
        }

        BlockPos destination = safePos.get();
        hamster.requestTeleport(
                destination.getX() + 0.5,
                destination.getY(),
                destination.getZ() + 0.5);
        if (!hamster.getBlockPos().equals(destination)
                || !isSafeDryRescueLocation(destination, world, hamster)) {
            return false;
        }

        hamster.setVelocity(0.0, 0.0, 0.0);
        hamster.velocityDirty = true;
        hamster.getNavigation().stop();
        hamster.setSwimming(false);
        hamster.setAir(hamster.getMaxAir());
        AdorableHamsterPets.LOGGER.debug(
                "[HamsterDrowningRescue] Hamster {} rescued from {} to dry location {}.",
                hamster.getId(),
                origin,
                destination);
        return true;
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
        // --- 1. Fast Collision Checks (Cheapest) ---
        ShapeContext entityContext = ShapeContext.of(hamster);

        // Ensure physical surface to stand on first
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);
        if (floorState.getCollisionShape(world, floorPos).isEmpty()) {
            return false;
        }

        // Ensure body and head space are empty
        BlockState bodyState = world.getBlockState(pos);
        if (!bodyState.getCollisionShape(world, pos, entityContext).isEmpty()) {
            return false;
        }

        BlockState headState = world.getBlockState(pos.up());
        if (!headState.getCollisionShape(world, pos.up(), entityContext).isEmpty()) {
            return false;
        }

        // --- 2. Hazard Checks ---
        // Fluid: unsafe
        if (!bodyState.getFluidState().isEmpty()) {
            return false;
        }

        // Explicit block hazards
        Block bodyBlock = bodyState.getBlock();
        if (bodyBlock == Blocks.FIRE || bodyBlock == Blocks.SOUL_FIRE || bodyBlock == Blocks.LAVA || bodyBlock == Blocks.POWDER_SNOW || bodyBlock == Blocks.SWEET_BERRY_BUSH || bodyBlock == Blocks.WITHER_ROSE || bodyBlock == Blocks.CACTUS || bodyBlock == Blocks.WATER) {
            return false;
        }

        // Floor hazards
        Block floorBlock = floorState.getBlock();
        if (floorBlock == Blocks.MAGMA_BLOCK || floorBlock == Blocks.CACTUS || floorBlock == Blocks.CAMPFIRE || floorBlock == Blocks.SOUL_CAMPFIRE || floorBlock == Blocks.LAVA || floorBlock == Blocks.FIRE || floorBlock == Blocks.SOUL_FIRE) {
            return false;
        }

        return true;
    }

    private static Optional<BlockPos> findDrowningRescuePosition(
            BlockPos origin, World world, HamsterEntity hamster) {
        int maximumY =
                Math.min(
                        world.getBottomY() + world.getHeight() - 2,
                        origin.getY() + DROWNING_VERTICAL_RANGE);

        Optional<BlockPos> nearbyColumnResult =
                searchDrowningColumns(
                        origin,
                        world,
                        hamster,
                        maximumY,
                        0,
                        DROWNING_COLUMN_RADIUS);
        if (nearbyColumnResult.isPresent()) {
            return nearbyColumnResult;
        }

        return searchDrowningColumns(
                origin,
                world,
                hamster,
                maximumY,
                DROWNING_COLUMN_RADIUS + 1,
                DROWNING_RING_RADIUS);
    }

    private static Optional<BlockPos> searchDrowningColumns(
            BlockPos origin,
            World world,
            HamsterEntity hamster,
            int maximumY,
            int minimumRadius,
            int maximumRadius) {
        for (int radius = minimumRadius; radius <= maximumRadius; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (radius > 0
                            && Math.abs(offsetX) != radius
                            && Math.abs(offsetZ) != radius) {
                        continue;
                    }

                    for (int y = origin.getY(); y <= maximumY; y++) {
                        BlockPos candidate =
                                new BlockPos(
                                        origin.getX() + offsetX,
                                        y,
                                        origin.getZ() + offsetZ);
                        if (isSafeDryRescueLocation(candidate, world, hamster)) {
                            return Optional.of(candidate);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isSafeDryRescueLocation(
            BlockPos pos, World world, HamsterEntity hamster) {
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)
                && world.getWorldBorder().contains(pos)
                && world.getFluidState(pos).isEmpty()
                && world.getFluidState(pos.up()).isEmpty()
                && isSafeSpawnLocation(pos, world, hamster);
    }
}
