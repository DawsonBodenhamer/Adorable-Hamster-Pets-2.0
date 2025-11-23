package net.dawson.adorablehamsterpets.util;

import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;

/**
 * A client-side utility for robust indoor vs. outdoor detection.
 * This class combines three signals with hysteresis and a small shared cache to determine if a location
 * should be considered "outdoors" for visual effects like wind.
 * <ul>
 *     <li><b>Horizontal Openness:</b> Checks for open directions around the position.</li>
 *     <li><b>Roof Clearance:</b> Measures the distance to any covering above.</li>
 *     <li><b>Corrected Skylight:</b> Checks skylight levels while accounting for glass, which blocks wind.</li>
 * </ul>
 * The results are deterministic and cached per chunk region to ensure high performance. Hysteresis is used
 * to prevent visual flickering when near an indoor/outdoor threshold.
 */
public final class IndoorOutdoorDetector {
    private IndoorOutdoorDetector() {}

    // --- Constants ---
    // --- Sampling and Cache ---
    private static final int CACHE_SIZE_BUCKETS = 1024; // Must be a power of two
    private static final int CACHE_MAX_AGE_TICKS = 18;
    private static final int MIN_RECHECK_INTERVAL_TICKS = 8;
    private static final int MAX_RECHECK_INTERVAL_JITTER_TICKS = 6; // Results in a recheck every 8-14 ticks

    // --- Horizontal Openness (A roof with open sides should feel windy) ---
    private static final int HORIZONTAL_OPENNESS_RAY_COUNT = 12;
    private static final int HORIZONTAL_OPENNESS_RAY_MAX_DISTANCE_BLOCKS = 6;
    private static final int HORIZONTAL_OPENNESS_WALL_COLUMN_HEIGHT_BLOCKS = 2;
    private static final int HORIZONTAL_OPENNESS_STRONG_SKYLIGHT_THRESHOLD = 12;
    private static final float HORIZONTAL_OPENNESS_MIN_FRACTION_THRESHOLD = 0.35f;

    // --- Roof Clearance (Nearby cover reduces wind) ---
    private static final int ROOF_CLEARANCE_VERTICAL_SEARCH_RANGE_BLOCKS = 6;

    // --- Skylight (Corrected by a clear column test so glass blocks wind) ---
    private static final int SKYLIGHT_CLEAR_COLUMN_MAX_STEPS = 16;

    // --- Score Weights and Hysteresis ---
    private static final float WEIGHT_HORIZONTAL_OPENNESS = 0.50f;
    private static final float WEIGHT_ROOF_CLEARANCE = 0.30f;
    private static final float WEIGHT_SKYLIGHT = 0.20f;
    private static final float OUTDOOR_SCORE_THRESHOLD = 0.56f; // Must exceed to flip to OUTDOOR
    private static final float INDOOR_SCORE_THRESHOLD = 0.44f;  // Must go below to flip to INDOOR

    // --- Cache ---
    private static final long[] cacheKeys = new long[CACHE_SIZE_BUCKETS];
    private static final long[] cacheLastTick = new long[CACHE_SIZE_BUCKETS];
    private static final int[] cacheNextInterval = new int[CACHE_SIZE_BUCKETS];
    private static final float[] cacheLastScore = new float[CACHE_SIZE_BUCKETS];
    private static final byte[] cacheLastState = new byte[CACHE_SIZE_BUCKETS]; // -1 unknown, 0 indoor, 1 outdoor
    static {
        for (int i = 0; i < cacheLastState.length; i++) cacheLastState[i] = -1;
    }

    // --- Public API ---

    /**
     * Determines if a given world position should be considered "outdoors" for visual effects.
     *
     * @param world The client world.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @return True if the position is considered outdoors, false otherwise.
     */
    public static boolean isOutdoor(ClientWorld world, double x, double y, double z) {
        if (!world.getDimension().hasSkyLight()) return false; // Nether/End are always indoors for wind

        final long nowTick = world.getTime();
        final BlockPos basePos = BlockPos.ofFloored(x, y + 0.2, z);
        final long key = keyFor(world, basePos);
        final int idx = bucketIndex(key);

        // Create or refresh entry if missing or expired
        if (cacheKeys[idx] != key || (nowTick - cacheLastTick[idx]) > CACHE_MAX_AGE_TICKS) {
            cacheKeys[idx] = key;
            cacheLastTick[idx] = nowTick;
            cacheNextInterval[idx] = randomRecheckInterval(key);
            cacheLastState[idx] = -1; // unknown
        }

        // Recompute if interval has elapsed or state is unknown
        boolean needRecheck = (cacheLastState[idx] == -1) || ((nowTick - cacheLastTick[idx]) >= cacheNextInterval[idx]);
        if (needRecheck) {
            float score = computeOutdoorScore(world, basePos);
            cacheLastScore[idx] = score;
            cacheLastTick[idx] = nowTick;
            cacheNextInterval[idx] = randomRecheckInterval(key);

            // Hysteresis logic to prevent flickering at thresholds
            byte previous = cacheLastState[idx];
            boolean newIsOutdoor;
            if (previous == -1) { // Was unknown
                newIsOutdoor = score >= OUTDOOR_SCORE_THRESHOLD;
            } else if (previous == 1) { // Was outdoor
                newIsOutdoor = score >= INDOOR_SCORE_THRESHOLD; // Stay outdoor unless score is strongly indoor
            } else { // Was indoor
                newIsOutdoor = score >= OUTDOOR_SCORE_THRESHOLD; // Need a stronger score to flip to outdoor
            }
            cacheLastState[idx] = newIsOutdoor ? (byte)1 : (byte)0;
        }

        return cacheLastState[idx] == 1;
    }

    // --- Scoring Logic ---

    private static float computeOutdoorScore(ClientWorld world, BlockPos pos) {
        float horizontalOpenFrac = horizontalOpennessFraction(world, pos);
        float roofClearanceScore = roofClearanceFactor(world, pos);
        float skylightScore = skylightFactorCorrected(world, pos);

        // Weighted sum of all factors
        float score = WEIGHT_HORIZONTAL_OPENNESS * horizontalOpenFrac
                + WEIGHT_ROOF_CLEARANCE * roofClearanceScore
                + WEIGHT_SKYLIGHT * skylightScore;

        return MathHelper.clamp(score, 0f, 1f);
    }

    /** More open directions result in a score closer to 1.0. */
    private static float horizontalOpennessFraction(ClientWorld world, BlockPos base) {
        int openDirections = 0;
        for (int i = 0; i < HORIZONTAL_OPENNESS_RAY_COUNT; i++) {
            double ang = (MathHelper.TAU * i) / HORIZONTAL_OPENNESS_RAY_COUNT;
            double cos = Math.cos(ang), sin = Math.sin(ang);

            boolean reachedOpening = false;

            // March outward from the base position
            for (int d = 1; d <= HORIZONTAL_OPENNESS_RAY_MAX_DISTANCE_BLOCKS && !reachedOpening; d++) {
                int sx = base.getX() + (int)Math.round(cos * d);
                int sz = base.getZ() + (int)Math.round(sin * d);

                // A two-block non-porous column counts as a wall
                boolean hardBlocked = false;
                for (int h = 0; h < HORIZONTAL_OPENNESS_WALL_COLUMN_HEIGHT_BLOCKS; h++) {
                    BlockPos p = new BlockPos(sx, base.getY() + h, sz);
                    if (isNonPorousWindBlock(world.getBlockState(p), world, p)) {
                        hardBlocked = true;
                        break;
                    }
                }
                if (hardBlocked) break; // This direction is closed

                // If this column can see the sky or has strong skylight, it's an opening
                BlockPos check = new BlockPos(sx, base.getY() + 1, sz);
                if (world.isSkyVisible(check) || world.getLightLevel(LightType.SKY, check) >= HORIZONTAL_OPENNESS_STRONG_SKYLIGHT_THRESHOLD) {
                    reachedOpening = true;
                    break;
                }
            }
            if (reachedOpening) openDirections++;
        }
        float frac = openDirections / (float) HORIZONTAL_OPENNESS_RAY_COUNT;
        // Nudge score toward zero if extremely closed to avoid misclassifying tiny slits
        return frac < HORIZONTAL_OPENNESS_MIN_FRACTION_THRESHOLD ? frac * 0.8f : frac;
    }

    /** More clearance up to the search range results in a score closer to 1.0. */
    private static float roofClearanceFactor(ClientWorld world, BlockPos pos) {
        int clearance = 0;
        // Scan upwards from the particle's position.
        for (int i = 1; i <= ROOF_CLEARANCE_VERTICAL_SEARCH_RANGE_BLOCKS; i++) {
            BlockPos checkPos = pos.up(i);
            BlockState state = world.getBlockState(checkPos);
            // A non-porous block is considered a roof.
            if (isNonPorousWindBlock(state, world, checkPos)) {
                break; // Found the roof, stop counting.
            }
            clearance++; // The number of clear blocks above
        }
        return MathHelper.clamp(clearance / (float) ROOF_CLEARANCE_VERTICAL_SEARCH_RANGE_BLOCKS, 0f, 1f);
    }

    /** Skylight scaled to [0,1], but clamped to zero if a blocking or glass block is found above. */
    private static float skylightFactorCorrected(ClientWorld world, BlockPos pos) {
        if (!columnToSkyIsClear(world, pos, SKYLIGHT_CLEAR_COLUMN_MAX_STEPS)) return 0.0f;
        int sky = world.getLightLevel(LightType.SKY, pos);
        return MathHelper.clamp(sky / 15.0f, 0f, 1f);
    }

    // --- Block Property Checks ---

    /** Checks if there is no solid or glass-like block within maxSteps above the position. */
    private static boolean columnToSkyIsClear(ClientWorld world, BlockPos from, int maxSteps) {
        for (int dy = 1; dy <= maxSteps; dy++) {
            BlockPos p = from.up(dy);
            BlockState s = world.getBlockState(p);
            if (isGlassLike(s) || s.getBlock() instanceof PaneBlock) return false;
            if (!s.isAir() && !s.getCollisionShape(world, p).isEmpty()) return false;
        }
        return true;
    }

    /** A non-porous wind blocker has a solid collision shape and is not a porous block like a fence or leaves. */
    private static boolean isNonPorousWindBlock(BlockState s, ClientWorld world, BlockPos at) {
        if (s.isAir()) return false;
        Block block = s.getBlock();
        if (block instanceof FenceBlock || block instanceof LeavesBlock) return false;
        if (isGlassLike(s) || block instanceof PaneBlock) return true; // Glass blocks wind
        return !s.getCollisionShape(world, at).isEmpty();
    }

    /** Checks if a block is a solid glass-type block (clear, stained, or tinted). */
    private static boolean isGlassLike(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GLASS || block instanceof StainedGlassBlock || block instanceof TintedGlassBlock;
    }

    // --- Cache Management ---

    private static long keyFor(ClientWorld world, BlockPos pos) {
        int cellX = pos.getX() >> 1;
        int cellZ = pos.getZ() >> 1;
        int yQuart = pos.getY() >> 1;
        long dimHint = world.getDimension().hasSkyLight() ? 1L : 2L;
        long k = (dimHint << 48)
                ^ (((long)cellX & 0xFFFFL) << 32)
                ^ (((long)cellZ & 0xFFFFL) << 16)
                ^ ((long)yQuart & 0xFFFFL);
        return mix64(k, 0xD1B54A32D192ED03L);
    }

    private static int bucketIndex(long key) {
        return (int)(key & (CACHE_SIZE_BUCKETS - 1));
    }

    private static int randomRecheckInterval(long key) {
        long m = mix64(key, 0x9E3779B97F4A7C15L);
        int jitter = (int)((m >>> 57) & MAX_RECHECK_INTERVAL_JITTER_TICKS);
        return MIN_RECHECK_INTERVAL_TICKS + jitter;
    }

    private static long mix64(long seedA, long seedB) {
        long x = seedA * 0x9E3779B97F4A7C15L + seedB + 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }
}