package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class TreeHeistUtil {

    // --- Constants ---
    private static final int MAX_TRUNK_SEARCH_DIST = 10; // Max radius to look for a log from the hit leaf
    private static final int MAX_LOG_COUNT = 128;        // Max logs in a single tree (prevent giant tree lag)
    private static final int MAX_CANOPY_COUNT = 300;     // Max leaves to map
    private static final int MAX_BUSH_COUNT = 48;        // Max leaves for a log-less bush
    private static final int MAX_CANOPY_DISTANCE = 5;    // Max distance property for a leaf to be considered connected

    /**
     * A record to store the results of a tree scan.
     */
    public record TreeScanResult(BlockPos treeId, Set<BlockPos> validCanopyPositions) {}

    public record HeistRecord(BlockPos pos, long timestamp) {}

    /**
     * Scans the structure connected to the given start position to identify a unique Tree ID and its canopy.
     * Uses a robust "Leaf -> Log -> Tree" algorithm similar to tree chopper mods.
     */
    public static TreeScanResult scanForTree(World world, BlockPos startPos) {
        BlockState startState = world.getBlockState(startPos);
        if (!startState.isOf(Blocks.OAK_LEAVES)) {
            // Fallback for non-leaf start
            if (Configs.AHP.debugTreeDetection) {
                AdorableHamsterPets.LOGGER.warn("[TreeHeist-Scan] Aborted: Start pos {} is not an Oak Leaf block (Found: {}).", startPos.toShortString(), startState.getBlock());
            }
            return new TreeScanResult(startPos, Collections.singleton(startPos));
        }

        // --- Step A: Find the Trunk ---
        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist-Scan] Starting scan at HitPos: {}. Searching for connected logs...", startPos.toShortString());
        }

        BlockPos foundLog = findConnectedLog(world, startPos);

        if (foundLog != null) {
            // --- Step B: Trunk Found -> Map Full Tree ---
            if (Configs.AHP.debugTreeDetection) {
                AdorableHamsterPets.LOGGER.info("[TreeHeist-Scan] LOG FOUND at {}. Switching to Tree Map Mode.", foundLog.toShortString());
            }
            return mapTreeFromLog(world, foundLog);
        } else {
            // --- Step C: No Trunk -> Map as Floating Bush ---
            if (Configs.AHP.debugTreeDetection) {
                AdorableHamsterPets.LOGGER.info("[TreeHeist-Scan] NO LOG FOUND within range. Switching to Floating Bush Mode.");
            }
            return mapFloatingBush(world, startPos);
        }
    }

    /**
     * Helper Step A: Performs a gradient-descent BFS to find the nearest log connected to the leaves.
     */
    private static BlockPos findConnectedLog(World world, BlockPos startNode) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startNode);
        visited.add(startNode);

        int startDist = getLeafDistance(world.getBlockState(startNode));

        // Safety cap for search iterations
        int iterations = 0;
        int maxIterations = 64;

        while (!queue.isEmpty() && iterations < maxIterations) {
            BlockPos current = queue.poll();
            iterations++;

            // Check all 6 neighbors
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (!visited.add(neighbor)) continue;

                BlockState state = world.getBlockState(neighbor);

                // Found a log. Return immediately.
                if (state.isIn(BlockTags.LOGS)) {
                    return neighbor;
                }

                // If Oak Leaf, traverse only if it gets closer to a log or maintains proximity.
                if (state.isOf(Blocks.OAK_LEAVES)) {
                    int dist = getLeafDistance(state);
                    // Standard vanilla leaves max out at 7.
                    // Follow the path of decreasing distance (towards trunk).
                    if (dist <= startDist || dist < 7) {
                        // Don't wander too far from origin
                        if (neighbor.getManhattanDistance(startNode) <= MAX_TRUNK_SEARCH_DIST) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return null; // No log found within range
    }

    /**
     * Helper Step B: Maps an entire tree (logs + canopy) starting from a known log block.
     * Calculates the Tree ID from the bottom-most log.
     */
    private static TreeScanResult mapTreeFromLog(World world, BlockPos initialLog) {
        // 1. Scan Logs (Trunk & Branches)
        Set<BlockPos> treeLogs = new HashSet<>();
        Queue<BlockPos> logQueue = new ArrayDeque<>();
        logQueue.add(initialLog);
        treeLogs.add(initialLog);

        BlockPos bottomMostLog = initialLog;

        while (!logQueue.isEmpty() && treeLogs.size() < MAX_LOG_COUNT) {
            BlockPos current = logQueue.poll();

            // Track lowest Y for ID
            if (current.getY() < bottomMostLog.getY()) {
                bottomMostLog = current;
            } else if (current.getY() == bottomMostLog.getY()) {
                // Tie-breaker: Deterministic coordinate sort (min X then min Z)
                if (current.getX() < bottomMostLog.getX() || (current.getX() == bottomMostLog.getX() && current.getZ() < bottomMostLog.getZ())) {
                    bottomMostLog = current;
                }
            }

            // Scan 3x3x3 area for connected logs (handles diagonal branches)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos neighbor = current.add(x, y, z);
                        if (!treeLogs.contains(neighbor)) {
                            if (world.getBlockState(neighbor).isIn(BlockTags.LOGS)) {
                                treeLogs.add(neighbor);
                                logQueue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist-Scan] Tree Skeleton Mapped. Total Logs: {}. Anchor Established: {}", treeLogs.size(), bottomMostLog.toShortString());
            if (treeLogs.size() >= MAX_LOG_COUNT) AdorableHamsterPets.LOGGER.warn("[TreeHeist-Scan] Log scan hit MAX limit ({})! Tree might be truncated.", MAX_LOG_COUNT);
        }

        // 2. Scan Canopy (Leaves connected to any found log)
        Set<BlockPos> validCanopy = new HashSet<>();
        Queue<BlockPos> leafQueue = new ArrayDeque<>();
        Set<BlockPos> visitedLeaves = new HashSet<>(); // optimization to avoid checking logs again

        // Seed with neighbors of all logs
        for (BlockPos log : treeLogs) {
            for (Direction dir : Direction.values()) {
                BlockPos n = log.offset(dir);
                // Mark visited immediately to prevent adding same leaf multiple times
                if (!treeLogs.contains(n) && visitedLeaves.add(n)) {
                    BlockState nState = world.getBlockState(n);
                    if (nState.isOf(Blocks.OAK_LEAVES)) {
                        int nDist = getLeafDistance(nState);
                        // Only add valid, connected leaves (dist 1 to log is valid)
                        if (nDist <= MAX_CANOPY_DISTANCE) {
                            leafQueue.add(n);
                        }
                    }
                }
            }
        }

        while (!leafQueue.isEmpty() && validCanopy.size() < MAX_CANOPY_COUNT) {
            BlockPos current = leafQueue.poll();
            BlockState state = world.getBlockState(current);

            // Double check validity (should be valid from queue, but safe)
            if (state.isIn(BlockTags.LEAVES)) {
                int dist = getLeafDistance(state);

                // Add to result set
                validCanopy.add(current);

                // Expand to neighbors
                for (Direction dir : Direction.values()) {
                    BlockPos n = current.offset(dir);
                    if (!treeLogs.contains(n) && visitedLeaves.add(n)) {
                        BlockState nState = world.getBlockState(n);
                        if (nState.isOf(Blocks.OAK_LEAVES)) {
                            int nDist = getLeafDistance(nState);

                            // Must be a valid connected leaf (<= 6) and moving away from the trunk or laterally (nDist >= dist).
                            if (nDist <= MAX_CANOPY_DISTANCE && nDist >= dist) {
                                leafQueue.add(n);
                            }
                        }
                    }
                }
            }
        }

        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist-Scan] Canopy Mapped. Valid Oak Leaves: {}", validCanopy.size());
            if (validCanopy.size() >= MAX_CANOPY_COUNT) AdorableHamsterPets.LOGGER.warn("[TreeHeist-Scan] Canopy scan hit MAX limit ({})!", MAX_CANOPY_COUNT);
        }

        return new TreeScanResult(bottomMostLog, validCanopy);
    }

    /**
     * Helper Step C: Maps a "floating bush" (no logs).
     * Calculates Tree ID from the spatially lowest/first block.
     */
    private static TreeScanResult mapFloatingBush(World world, BlockPos startPos) {
        Set<BlockPos> validLeaves = new HashSet<>(); // The Result Set: Only confirmed leaves
        Set<BlockPos> visited = new HashSet<>();     // The Checked Set: Leaves + Neighbors
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(startPos);
        visited.add(startPos);
        validLeaves.add(startPos); // startPos is checked in scanForTree

        BlockPos lowestLeaf = startPos;

        while (!queue.isEmpty() && validLeaves.size() < MAX_BUSH_COUNT) {
            BlockPos current = queue.poll();

            // Track Anchor
            if (current.getY() < lowestLeaf.getY() ||
                    (current.getY() == lowestLeaf.getY() && current.getX() < lowestLeaf.getX()) ||
                    (current.getY() == lowestLeaf.getY() && current.getX() == lowestLeaf.getX() && current.getZ() < lowestLeaf.getZ())) {
                lowestLeaf = current;
            }

            for (Direction dir : Direction.values()) {
                BlockPos n = current.offset(dir);
                // Mark as visited to prevent re-processing
                if (visited.add(n)) {
                    // Check if it is a valid leaf block before adding to queue or result
                    if (world.getBlockState(n).isOf(Blocks.OAK_LEAVES)) {
                        queue.add(n);
                        validLeaves.add(n); // Only add to result if it is a leaf
                    }
                }
            }
        }
        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist-Scan] Bush Mapped. Size: {}. Anchor: {}", validLeaves.size(), lowestLeaf.toShortString());
        }
        return new TreeScanResult(lowestLeaf, validLeaves);
    }

    /**
     * Gets the 'distance' property from a leaf block state safely.
     */
    private static int getLeafDistance(BlockState state) {
        if (state.contains(LeavesBlock.DISTANCE)) {
            return state.get(LeavesBlock.DISTANCE);
        }
        return 7; // Treat as far/decayable if property missing
    }

    /**
     * Helper to bridge between the Entity's stored data and the particle logic.
     */
    public static void spawnDebugParticles(World world, BlockPos anchor, List<Long> leafLongs) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        // 1. Anchor Visualization
        // Spawns a rotating ring of particles around the trunk anchor
        if (anchor != null) {
            ParticleEffectsUtil.spawnSpinningRing(
                    world,
                    anchor,
                    // Using WAX_ON for 1.20.1
                    ParticleTypes.WAX_ON,
                    10,
                    0.85,
                    0.0,
                    0.7,
                    0.55,
                    3.0,
                    0.0
            );
        }

        // 2. Canopy Visualization
        // Select random leaves from the list to highlight each tick
        if (!leafLongs.isEmpty()) {
            int particleCount = 200; // Spawn 200 sparkles per tick
            for (int i = 0; i < particleCount; i++) {
                long leafPosLong = leafLongs.get(world.getRandom().nextInt(leafLongs.size()));
                BlockPos leafPos = BlockPos.fromLong(leafPosLong);

                ParticleEffectsUtil.spawnSphericalShell(
                        world,
                        Vec3d.ofCenter(leafPos),
                        // Using WAX_OFF for 1.20.1
                        ParticleTypes.WAX_OFF,
                        1,
                        0.7,
                        0.2
                );
            }
        }
    }

    /**
     * Adapter for TreeScanResult object.
     */
    public static void spawnDebugParticles(World world, TreeScanResult result) {
        if (world.isClient) return;
        List<Long> longs = new ArrayList<>();
        for (BlockPos p : result.validCanopyPositions()) longs.add(p.asLong());
        spawnDebugParticles(world, result.treeId(), longs);
    }

    public static BlockPos findExitPosition(World world, BlockPos startPos) {
        // 1. Primary Strategy: Gravity (Downwards)
        for (int y = 1; y <= 15; y++) {
            BlockPos check = startPos.down(y);
            if (isSafeExit(world, check)) {
                return check;
            }
        }

        // 2. Secondary Strategy: Ejection (Horizontal)
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos check = startPos.offset(dir);
            if (isSafeExit(world, check)) {
                return check;
            }
        }

        // 3. Tertiary Strategy: Escape (Upwards)
        for (int y = 1; y <= 5; y++) {
            BlockPos check = startPos.up(y);
            if (isSafeExit(world, check)) {
                return check;
            }
        }

        // 4. Fallback
        return startPos;
    }

    /**
     * Determines if a position is a valid, safe exit point for the hamster.
     * Checks for collision and ensures the spot isn't a 1x1 enclosed pocket.
     */
    private static boolean isSafeExit(World world, BlockPos pos) {
        // 1. Must be non-colliding (Air, Grass, Water, etc.)
        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
            return false;
        }

        // 2. Vertical Check: Is there a floor?
        BlockPos below = pos.down();
        if (world.getBlockState(below).getCollisionShape(world, below).isEmpty()) {
            // The block below is non-colliding (Air/Passable).
            // Consider this SAFE because it prevents getting stuck in a 1x1x1 gap.
            return true;
        }

        // 3. Horizontal Check: Standing on a floor (Leaf/Log/Ground).
        // If in a tiny enclosed room (1x1 or 2x2 pocket), consider unsafe.
        return !isSmallPocket(world, pos);
    }

    /**
     * Performs a limited flood fill to determine if the given position is part of a small enclosed space.
     * @return true if the space is smaller than the threshold (5 blocks), indicating a pocket.
     */
    private static boolean isSmallPocket(World world, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(startPos);
        visited.add(startPos);

        int airCount = 0;
        int limit = 5; // If we find 5 connected air blocks, we assume it's open enough to not be a "trap".

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            airCount++;

            if (airCount >= limit) {
                return false; // Found enough space, it's not a pocket.
            }

            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = current.offset(dir);
                if (!visited.contains(neighbor)) {
                    // If neighbor is air (no collision), add to queue
                    if (world.getBlockState(neighbor).getCollisionShape(world, neighbor).isEmpty()) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        // If we finished the loop without hitting the limit, we are in a small enclosed space.
        return true;
    }

    /**
     * Sends the appropriate start message to the player based on the calculated profitability of the area.
     */
    public static void sendHeistStartMessage(PlayerEntity player, float profitability) {
        if (!Configs.AHP.enableTreeHeistStartMessage) return;

        String key = "message.adorablehamsterpets.tree_heist_start_high";
        Formatting color = Formatting.WHITE;

        if (profitability < 0.4f) {
            key = "message.adorablehamsterpets.tree_heist_start_low";
        } else if (profitability < 0.9f) {
            key = "message.adorablehamsterpets.tree_heist_start_medium";
        }

        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist] Starting heist for player {}. Profitability: {} ({}%). Selected Message Key: {}",
                    player.getName().getString(),
                    String.format("%.2f", profitability),
                    (int) (profitability * 100),
                    key);
        }

        player.sendMessage(Text.translatable(key).formatted(color), true);
    }
}