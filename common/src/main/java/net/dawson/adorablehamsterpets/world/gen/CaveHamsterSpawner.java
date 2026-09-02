package net.dawson.adorablehamsterpets.world.gen;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.tag.ModBiomeTags;
import net.dawson.adorablehamsterpets.util.HamsterPlacementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplements vanilla surface spawning with periodic cave-spawn attempts.
 */
public final class CaveHamsterSpawner {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double SPAWN_RADIUS = 128.0;
    private static final int ATTEMPT_INTERVAL_TICKS = 400;
    private static final int MINIMUM_PLAYER_DISTANCE = 24;
    private static final int MAXIMUM_PLAYER_DISTANCE = 128;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private CaveHamsterSpawner() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Event Handlers and Callbacks
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Evaluates eligible players once per cave-spawn cycle on the server thread.
     */
    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % ATTEMPT_INTERVAL_TICKS != 0) return;

        for (ServerLevel world : server.getAllLevels()) {
            List<ServerPlayer> players = world.getPlayers(player -> !player.isSpectator());
            for (ServerPlayer player : selectPlayerRepresentatives(players)) {
                attemptSpawn(world, player);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static List<ServerPlayer> selectPlayerRepresentatives(List<ServerPlayer> players) {
        List<ServerPlayer> representatives = new ArrayList<>();
        for (ServerPlayer player : players) {
            CaveHamsterSpawnPolicy.PlayerPosition position =
                    new CaveHamsterSpawnPolicy.PlayerPosition(player.getX(), player.getZ());
            boolean overlaps = representatives.stream().anyMatch(representative -> {
                CaveHamsterSpawnPolicy.PlayerPosition existing =
                        new CaveHamsterSpawnPolicy.PlayerPosition(representative.getX(), representative.getZ());
                return CaveHamsterSpawnPolicy.spawnAreasOverlap(existing, position);
            });
            if (!overlaps) {
                representatives.add(player);
            }
        }
        return representatives;
    }

    private static void attemptSpawn(ServerLevel world, ServerPlayer player) {
        // --- Attempt Gate ---
        RandomSource random = world.getRandom();
        if (!CaveHamsterSpawnPolicy.shouldAttempt(Configs.AHP_WORLDGEN.spawnWeight.get(), random.nextInt(100))) return;
        int nearbyCaveHamsters = countNearbyCaveHamsters(world, player);
        if (CaveHamsterSpawnPolicy.limitGroupSize(nearbyCaveHamsters, 1) == 0) return;

        // --- Candidate Search ---
        BlockPos candidate = findCavePosition(world, player, random);
        if (candidate == null) return;

        // --- Group Spawning ---
        int requestedGroupSize = random.nextIntBetweenInclusive(1, Configs.AHP_WORLDGEN.maxGroupSize.get());
        int groupSize = CaveHamsterSpawnPolicy.limitGroupSize(nearbyCaveHamsters, requestedGroupSize);
        for (int index = 0; index < groupSize; index++) {
            BlockPos groupPosition = index == 0
                    ? candidate
                    : candidate.offset(random.nextIntBetweenInclusive(-2, 2), 0, random.nextIntBetweenInclusive(-2, 2));
            spawnAt(world, player, groupPosition, random);
        }
    }

    private static BlockPos findCavePosition(
            ServerLevel world, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double radiusSquared = Mth.lerp(
                random.nextDouble(),
                MINIMUM_PLAYER_DISTANCE * MINIMUM_PLAYER_DISTANCE,
                MAXIMUM_PLAYER_DISTANCE * MAXIMUM_PLAYER_DISTANCE);
        double radius = Math.sqrt(radiusSquared);
        int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
        if (!world.hasChunk(x >> 4, z >> 4)) return null;

        int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos selected = null;
        int validFloors = 0;
        for (int y = world.getMinY() + 1; y < surfaceY; y++) {
            BlockPos position = new BlockPos(x, y, z);
            if (!isCaveEnvironment(world, position, surfaceY)) continue;
            if (!isValidSpawnPosition(world, player, position, null)) continue;

            validFloors++;
            if (random.nextInt(validFloors) == 0) {
                selected = position;
            }
        }
        return selected;
    }

    private static boolean spawnAt(
            ServerLevel world, ServerPlayer player, BlockPos position, RandomSource random) {
        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world, EntitySpawnReason.LOAD);
        if (hamster == null || !isValidSpawnPosition(world, player, position, hamster)) return false;

        hamster.snapTo(
                position.getX() + 0.5,
                position.getY(),
                position.getZ() + 0.5,
                random.nextFloat() * 360.0F,
                0.0F);
        if (!world.noCollision(hamster)) return false;

        hamster.initializeCaveSpawn(world, world.getCurrentDifficultyAt(position));
        return world.addFreshEntity(hamster);
    }

    private static boolean isValidSpawnPosition(
            ServerLevel world,
            ServerPlayer player,
            BlockPos position,
            HamsterEntity hamster) {
        if (!world.hasChunk(position.getX() >> 4, position.getZ() >> 4)) return false;
        if (!world.getWorldBorder().isWithinBounds(position)) return false;
        if (!isWithinPlayerDistance(player, position)) return false;
        if (world.hasNearbyAlivePlayer(
                position.getX() + 0.5,
                position.getY(),
                position.getZ() + 0.5,
                MINIMUM_PLAYER_DISTANCE)) return false;
        if (!ModEntitySpawns.isBiomeAllowed(world.getBiome(position))) return false;
        if (!world.getFluidState(position).isEmpty() || !world.getFluidState(position.above()).isEmpty()) return false;
        if (!hasSpawnHeadroom(world, position)) return false;
        if (!ModEntitySpawns.isValidHamsterNaturalSpawn(
                ModEntities.HAMSTER.get(), world, EntitySpawnReason.NATURAL, position, world.getRandom())) return false;
        if (hamster != null && !HamsterPlacementUtil.isSafeSpawnLocation(position, world, hamster)) return false;
        return isCaveEnvironment(
                world,
                position,
                world.getHeight(Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ()));
    }

    private static boolean isWithinPlayerDistance(ServerPlayer player, BlockPos position) {
        double distanceSquared = position.distToLowCornerSqr(player.getX(), player.getY(), player.getZ());
        return distanceSquared >= MINIMUM_PLAYER_DISTANCE * MINIMUM_PLAYER_DISTANCE
                && distanceSquared <= MAXIMUM_PLAYER_DISTANCE * MAXIMUM_PLAYER_DISTANCE;
    }

    private static boolean isCaveEnvironment(ServerLevel world, BlockPos position, int surfaceY) {
        return CaveHamsterSpawnPolicy.isCavePosition(
                world.getBiome(position).is(ModBiomeTags.IS_CAVE),
                world.canSeeSky(position),
                position.getY(),
                surfaceY);
    }

    private static boolean hasSpawnHeadroom(ServerLevel world, BlockPos position) {
        return world.getBlockState(position).getCollisionShape(world, position).isEmpty()
                && world.getBlockState(position.above()).getCollisionShape(world, position.above()).isEmpty();
    }

    private static int countNearbyCaveHamsters(ServerLevel world, ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(SPAWN_RADIUS);
        return world.getEntitiesOfClass(
                        HamsterEntity.class,
                        searchBox,
                        hamster -> !hamster.isTame()
                                && hamster.distanceToSqr(player) <= SPAWN_RADIUS * SPAWN_RADIUS
                                && isCaveHamster(world, hamster))
                .size();
    }

    private static boolean isCaveHamster(ServerLevel world, HamsterEntity hamster) {
        BlockPos position = hamster.blockPosition();
        if (!world.hasChunk(position.getX() >> 4, position.getZ() >> 4)) return false;

        int surfaceY = world.getHeight(
                Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ());
        return isCaveEnvironment(world, position, surfaceY);
    }
}
