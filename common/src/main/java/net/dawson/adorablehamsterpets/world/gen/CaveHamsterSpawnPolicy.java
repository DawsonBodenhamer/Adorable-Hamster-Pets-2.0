package net.dawson.adorablehamsterpets.world.gen;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic policy for cave-spawn timing, classification, and player overlap.
 */
public final class CaveHamsterSpawnPolicy {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double SPAWN_RADIUS = 128.0;
    private static final double OVERLAP_DISTANCE_SQUARED = 4.0 * SPAWN_RADIUS * SPAWN_RADIUS;
    private static final int MINIMUM_CAVE_DEPTH = 6;
    private static final int CAVE_POPULATION_LIMIT = 10;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Evaluates the configured cave-attempt percentage against a zero-based roll.
     */
    public static boolean shouldAttempt(int spawnWeight, int percentageRoll) {
        return percentageRoll < spawnWeight;
    }

    /**
     * Classifies tagged cave biomes or sufficiently deep covered positions as caves.
     */
    public static boolean isCavePosition(
            boolean caveBiome, boolean skyVisible, int positionY, int surfaceY) {
        return caveBiome || (!skyVisible && positionY <= surfaceY - MINIMUM_CAVE_DEPTH);
    }

    /**
     * Classifies a spawning hamster.
     */
    public static boolean isInitializationCaveEnvironment(
            boolean supplementalCaveSpawn, boolean caveBiome) {
        return supplementalCaveSpawn || caveBiome;
    }

    /**
     * Selects one representative for each non-overlapping cave-spawn area.
     */
    public static List<PlayerPosition> selectRepresentatives(List<PlayerPosition> players) {
        List<PlayerPosition> representatives = new ArrayList<>();
        for (PlayerPosition player : players) {
            boolean overlaps = representatives.stream()
                    .anyMatch(representative -> representative.squaredDistanceTo(player) <= OVERLAP_DISTANCE_SQUARED);
            if (!overlaps) {
                representatives.add(player);
            }
        }
        return List.copyOf(representatives);
    }

    /**
     * Checks whether two players' 128-block cave-spawn areas intersect.
     */
    public static boolean spawnAreasOverlap(PlayerPosition first, PlayerPosition second) {
        return first.squaredDistanceTo(second) <= OVERLAP_DISTANCE_SQUARED;
    }

    /**
     * Restricts a requested group to the remaining nearby cave-population allowance.
     */
    public static int limitGroupSize(int nearbyCaveHamsters, int requestedGroupSize) {
        return Math.min(requestedGroupSize, Math.max(0, CAVE_POPULATION_LIMIT - nearbyCaveHamsters));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private CaveHamsterSpawnPolicy() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ────────────────────────────────────────────────────────────────────────────*/

    public record PlayerPosition(double x, double z) {

        private double squaredDistanceTo(PlayerPosition other) {
            double offsetX = this.x - other.x;
            double offsetZ = this.z - other.z;
            return offsetX * offsetX + offsetZ * offsetZ;
        }
    }
}
