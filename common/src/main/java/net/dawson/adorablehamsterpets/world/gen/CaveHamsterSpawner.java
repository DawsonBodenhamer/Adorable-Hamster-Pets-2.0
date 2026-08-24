package net.dawson.adorablehamsterpets.world.gen;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.tag.ModBiomeTags;
import net.dawson.adorablehamsterpets.util.HamsterPlacementUtil;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

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
        if (server.getTicks() % ATTEMPT_INTERVAL_TICKS != 0) return;

        for (ServerWorld world : server.getWorlds()) {
            List<ServerPlayerEntity> players = world.getPlayers(player -> !player.isSpectator());
            for (ServerPlayerEntity player : selectPlayerRepresentatives(players)) {
                attemptSpawn(world, player);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static List<ServerPlayerEntity> selectPlayerRepresentatives(List<ServerPlayerEntity> players) {
        List<ServerPlayerEntity> representatives = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
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

    private static void attemptSpawn(ServerWorld world, ServerPlayerEntity player) {
        // --- Attempt Gate ---
        Random random = world.getRandom();
        if (!CaveHamsterSpawnPolicy.shouldAttempt(Configs.AHP_WORLDGEN.spawnWeight.get(), random.nextInt(100))) return;
        int nearbyCaveHamsters = countNearbyCaveHamsters(world, player);
        if (CaveHamsterSpawnPolicy.limitGroupSize(nearbyCaveHamsters, 1) == 0) return;

        // --- Candidate Search ---
        BlockPos candidate = findCavePosition(world, player, random);
        if (candidate == null) return;

        // --- Group Spawning ---
        int requestedGroupSize = random.nextBetween(1, Configs.AHP_WORLDGEN.maxGroupSize.get());
        int groupSize = CaveHamsterSpawnPolicy.limitGroupSize(nearbyCaveHamsters, requestedGroupSize);
        for (int index = 0; index < groupSize; index++) {
            BlockPos groupPosition = index == 0
                    ? candidate
                    : candidate.add(random.nextBetween(-2, 2), 0, random.nextBetween(-2, 2));
            spawnAt(world, player, groupPosition, random);
        }
    }

    private static BlockPos findCavePosition(
            ServerWorld world, ServerPlayerEntity player, Random random) {
        double angle = random.nextDouble() * MathHelper.TAU;
        double radiusSquared = MathHelper.lerp(
                random.nextDouble(),
                MINIMUM_PLAYER_DISTANCE * MINIMUM_PLAYER_DISTANCE,
                MAXIMUM_PLAYER_DISTANCE * MAXIMUM_PLAYER_DISTANCE);
        double radius = Math.sqrt(radiusSquared);
        int x = MathHelper.floor(player.getX() + Math.cos(angle) * radius);
        int z = MathHelper.floor(player.getZ() + Math.sin(angle) * radius);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;

        int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        BlockPos selected = null;
        int validFloors = 0;
        for (int y = world.getBottomY() + 1; y < surfaceY; y++) {
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
            ServerWorld world, ServerPlayerEntity player, BlockPos position, Random random) {
        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world);
        if (hamster == null || !isValidSpawnPosition(world, player, position, hamster)) return false;

        hamster.refreshPositionAndAngles(
                position.getX() + 0.5,
                position.getY(),
                position.getZ() + 0.5,
                random.nextFloat() * 360.0F,
                0.0F);
        if (!world.isSpaceEmpty(hamster)) return false;

        hamster.initializeCaveSpawn(world, world.getLocalDifficulty(position));
        return world.spawnEntity(hamster);
    }

    private static boolean isValidSpawnPosition(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos position,
            HamsterEntity hamster) {
        if (!world.isChunkLoaded(position.getX() >> 4, position.getZ() >> 4)) return false;
        if (!world.getWorldBorder().contains(position)) return false;
        if (!isWithinPlayerDistance(player, position)) return false;
        if (world.isPlayerInRange(
                position.getX() + 0.5,
                position.getY(),
                position.getZ() + 0.5,
                MINIMUM_PLAYER_DISTANCE)) return false;
        if (!ModEntitySpawns.isBiomeAllowed(world.getBiome(position))) return false;
        if (!world.getFluidState(position).isEmpty() || !world.getFluidState(position.up()).isEmpty()) return false;
        if (!hasSpawnHeadroom(world, position)) return false;
        if (!ModEntitySpawns.isValidHamsterNaturalSpawn(
                ModEntities.HAMSTER.get(), world, SpawnReason.NATURAL, position, world.getRandom())) return false;
        if (hamster != null && !HamsterPlacementUtil.isSafeSpawnLocation(position, world, hamster)) return false;
        return isCaveEnvironment(
                world,
                position,
                world.getTopY(Heightmap.Type.MOTION_BLOCKING, position.getX(), position.getZ()));
    }

    private static boolean isWithinPlayerDistance(ServerPlayerEntity player, BlockPos position) {
        double distanceSquared = position.getSquaredDistance(player.getX(), player.getY(), player.getZ());
        return distanceSquared >= MINIMUM_PLAYER_DISTANCE * MINIMUM_PLAYER_DISTANCE
                && distanceSquared <= MAXIMUM_PLAYER_DISTANCE * MAXIMUM_PLAYER_DISTANCE;
    }

    private static boolean isCaveEnvironment(ServerWorld world, BlockPos position, int surfaceY) {
        return CaveHamsterSpawnPolicy.isCavePosition(
                world.getBiome(position).isIn(ModBiomeTags.IS_CAVE),
                world.isSkyVisible(position),
                position.getY(),
                surfaceY);
    }

    private static boolean hasSpawnHeadroom(ServerWorld world, BlockPos position) {
        return world.getBlockState(position).getCollisionShape(world, position).isEmpty()
                && world.getBlockState(position.up()).getCollisionShape(world, position.up()).isEmpty();
    }

    private static int countNearbyCaveHamsters(ServerWorld world, ServerPlayerEntity player) {
        Box searchBox = player.getBoundingBox().expand(SPAWN_RADIUS);
        return world.getEntitiesByClass(
                        HamsterEntity.class,
                        searchBox,
                        hamster -> !hamster.isTamed()
                                && hamster.squaredDistanceTo(player) <= SPAWN_RADIUS * SPAWN_RADIUS
                                && isCaveHamster(world, hamster))
                .size();
    }

    private static boolean isCaveHamster(ServerWorld world, HamsterEntity hamster) {
        BlockPos position = hamster.getBlockPos();
        if (!world.isChunkLoaded(position.getX() >> 4, position.getZ() >> 4)) return false;

        int surfaceY = world.getTopY(
                Heightmap.Type.MOTION_BLOCKING, position.getX(), position.getZ());
        return isCaveEnvironment(world, position, surfaceY);
    }
}
