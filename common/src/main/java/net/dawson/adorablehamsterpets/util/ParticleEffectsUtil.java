package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Centralized utility for spawning particle effects.
 * <p>
 * Handles the logic difference between ServerWorld (broadcasting packets) and ClientWorld (rendering locally).
 * Includes overloads for convenience to avoid creating temporary Vec3d objects for spreads.
 */
public class ParticleEffectsUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Core Spawning Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Spawns particles with a random spread around a central point.
     * <p>
     * On <b>Server</b>: Sends a packet to all nearby players.
     * <br>
     * On <b>Client</b>: Iterates and spawns individual particles with random offsets.
     *
     * @param world    The world to spawn in.
     * @param center   The center position.
     * @param particle The particle type/data.
     * @param count    Number of particles.
     * @param spread   The maximum random offset (radius) on each axis (X, Y, Z).
     * @param speed    The speed/velocity multiplier for the particles.
     */
    public static <T extends ParticleEffect> void spawnParticles(World world, Vec3d center, T particle, int count, Vec3d spread, double speed) {
        spawnParticles(world, center, particle, count, spread.x, spread.y, spread.z, speed);
    }

    /**
     * Spawns particles with a random spread around a central point.
     * (8 Arguments: Uses individual doubles for spread).
     */
    public static <T extends ParticleEffect> void spawnParticles(World world, Vec3d center, T particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(particle, center.x, center.y, center.z, count, spreadX, spreadY, spreadZ, speed);
        } else {
            Random random = world.getRandom();
            for (int i = 0; i < count; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 2.0 * spreadX;
                double offsetY = (random.nextDouble() - 0.5) * 2.0 * spreadY;
                double offsetZ = (random.nextDouble() - 0.5) * 2.0 * spreadZ;

                // For client-side addParticle, roughly map 'speed' to velocity magnitude with random direction
                double vx = (random.nextDouble() - 0.5) * speed;
                double vy = (random.nextDouble() - 0.5) * speed;
                double vz = (random.nextDouble() - 0.5) * speed;

                world.addParticle(particle, center.x + offsetX, center.y + offsetY, center.z + offsetZ, vx, vy, vz);
            }
        }
    }

    /**
     * Spawns particles at the **center** of a BlockPos.
     * (8 Arguments).
     */
    public static <T extends ParticleEffect> void spawnParticles(World world, BlockPos pos, T particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        spawnParticles(world, Vec3d.ofCenter(pos), particle, count, spreadX, spreadY, spreadZ, speed);
    }

    /**
     * Spawns particles at a specific **Y-offset** from the bottom-center of a BlockPos.
     * (9 Arguments).
     */
    public static <T extends ParticleEffect> void spawnParticles(World world, BlockPos pos, double yOffset, T particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        spawnParticles(world, Vec3d.ofBottomCenter(pos).add(0, yOffset, 0), particle, count, spreadX, spreadY, spreadZ, speed);
    }

    /**
     * Spawns particles centered on a specific Entity, scaling the spread to match the entity's size.
     *
     * @param entity      The target entity.
     * @param particle    The particle effect.
     * @param count       Number of particles.
     * @param widthScale  Multiplier for the entity's width (spread X/Z).
     * @param heightScale Multiplier for the entity's height (spread Y).
     * @param yOffset     Vertical offset from the center of the entity.
     * @param speed       Particle speed.
     */
    public static <T extends ParticleEffect> void spawnParticlesOnEntity(Entity entity, T particle, int count, double widthScale, double heightScale, double speed, double yOffset) {
        // Calculate spread based on entity dimensions
        double spreadX = (entity.getWidth() * widthScale) / 2.0;
        double spreadY = (entity.getHeight() * heightScale) / 2.0;
        double spreadZ = (entity.getWidth() * widthScale) / 2.0;

        // Center Y = middle of the body
        double centerY = entity.getY() + (entity.getHeight() / 2.0) + yOffset;
        Vec3d center = new Vec3d(entity.getX(), centerY, entity.getZ());

        spawnParticles(entity.getWorld(), center, particle, count, spreadX, spreadY, spreadZ, speed);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Specialized Effect Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Spawns a "Motion Trail" effect behind a moving entity.
     * <p>
     * This calculates a spawn position behind the entity based on its velocity and applies
     * a calculated velocity to the particles to create a trailing effect.
     *
     * @param entity            The entity leaving the trail.
     * @param particle          The particle effect to spawn.
     * @param countPerTick      Number of particles to spawn per call.
     * @param offsetMultiplier  Multiplies the entity's velocity to determine spawn offset behind the entity. Higher values spawn particles further back.
     * @param scatter           Random spread applied to both spawn position and particle velocity.
     * @param velocityScale     Multiplier for the backward velocity of the particles relative to the entity's movement.
     * @param downwardVelocity  Constant downward velocity applied to particles (useful for dust/smoke).
     */
    public static <T extends ParticleEffect> void spawnMotionTrail(Entity entity, T particle, int countPerTick, double offsetMultiplier, double scatter, double velocityScale, double downwardVelocity) {
        World world = entity.getWorld();
        Vec3d velocity = entity.getVelocity();

        // Only spawn if moving
        if (velocity.horizontalLengthSquared() < 1.0E-6) return;

        Random random = world.getRandom();

        for (int i = 0; i < countPerTick; ++i) {
            // 1. Calculate base spawn position (Behind the entity)
            double baseX = entity.getX() - (velocity.x * offsetMultiplier);
            double baseY = entity.getY() + (entity.getHeight() / 2.0) - (velocity.y * offsetMultiplier);
            double baseZ = entity.getZ() - (velocity.z * offsetMultiplier);

            // 2. Apply random scatter
            double spawnX = baseX + (random.nextDouble() - 0.5) * (entity.getWidth() * 0.8);
            double spawnY = baseY + (random.nextDouble() - 0.5) * (entity.getHeight() * 0.05);
            double spawnZ = baseZ + (random.nextDouble() - 0.5) * (entity.getWidth() * 0.8);

            // 3. Calculate particle velocity (Opposite to entity movement)
            Vec3d backwardsVel = velocity.multiply(-1.0 * velocityScale);
            double finalVelX = backwardsVel.x + (random.nextGaussian() * scatter);
            double finalVelY = backwardsVel.y + (random.nextGaussian() * scatter) - downwardVelocity;
            double finalVelZ = backwardsVel.z + (random.nextGaussian() * scatter);

            world.addParticle(particle, spawnX, spawnY, spawnZ, finalVelX, finalVelY, finalVelZ);
        }
    }

    /**
     * Spawns "Breadcrumb" particles along a navigation path.
     * Useful for visualizing AI paths or leading players to objectives.
     *
     * @param world The ServerWorld to spawn particles in.
     * @param path  The navigation path to visualize. If null, nothing happens.
     */
    public static void spawnBreadcrumbs(ServerWorld world, @Nullable Path path) {
        if (path == null) return;

        int currentNodeIndex = path.getCurrentNodeIndex();
        int pathLength = path.getLength();

        // Iterate from the current node to the end of the path
        for (int i = currentNodeIndex; i < pathLength; i++) {
            PathNode node = path.getNode(i);
            Vec3d directionVector = Vec3d.ZERO;

            // 1. Determine the direction to the next node in the path.
            if (i + 1 < pathLength) {
                PathNode nextNode = path.getNode(i + 1);
                // Create a normalized (length of 1) vector pointing from the current node to the next.
                directionVector = new Vec3d(nextNode.x - node.x, 0, nextNode.z - node.z).normalize();
            }
            // For the very last node, directionVector will remain (0,0,0), so particles will cluster around it.

            // Loop to spawn multiple particles with randomized origins
            for (int p = 0; p < 3; p++) {
                // 2. Calculate a random distance to spread the particle along the direction vector.
                double distanceAlongPath = world.getRandom().nextDouble();
                Vec3d pathOffset = directionVector.multiply(distanceAlongPath);
                double offsetY = (world.getRandom().nextDouble() - 0.5) * 0.1;

                // Use flat-double overload
                spawnParticles(world, new Vec3d(node.x + 0.5 + pathOffset.x, (node.y + 0.5) - 0.38 + offsetY, node.z + 0.5 + pathOffset.z),
                        ParticleTypes.MYCELIUM, 1, 0.2, 0.0, 0.2, 3);
            }
        }
    }

    /**
     * Spawns "Breadcrumb" particles along a navigation path.
     * Useful for visualizing AI paths or leading players to objectives.
     *
     * @param world    The ServerWorld to spawn particles in.
     * @param path     The navigation path to visualize. If null, nothing happens.
     * @param particle The particle type to spawn.
     * @param count    The number of particles per randomized point.
     * @param spreadX  Horizontal X spread.
     * @param spreadY  Vertical spread.
     * @param spreadZ  Horizontal Z spread.
     * @param speed    Particle speed/velocity.
     */
    public static <T extends ParticleEffect> void spawnBreadcrumbs(ServerWorld world, @Nullable Path path, T particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        if (path == null) return;

        int currentNodeIndex = path.getCurrentNodeIndex();
        int pathLength = path.getLength();

        // Iterate from current node to end of path
        for (int i = currentNodeIndex; i < pathLength; i++) {
            PathNode node = path.getNode(i);
            Vec3d directionVector = Vec3d.ZERO;
            double distance = 1.0;

            // 1. Determine direction to next node in path
            if (i + 1 < pathLength) {
                PathNode nextNode = path.getNode(i + 1);
                Vec3d diff = new Vec3d(nextNode.x - node.x, nextNode.y - node.y, nextNode.z - node.z);
                distance = diff.length();
                if (distance > 0) {
                    directionVector = diff.normalize();
                }
            }

            // Loop to spawn multiple particles with randomized origins
            // Number of particles scales with distance to ensure consistent density along long path segments
            int particlesToSpawn = Math.max(3, (int) (3 * distance));
            for (int p = 0; p < particlesToSpawn; p++) {
                // 2. Calculate random distance to spread particle along direction vector
                double distanceAlongPath = world.getRandom().nextDouble() * distance;
                Vec3d pathOffset = directionVector.multiply(distanceAlongPath);
                double offsetY = (world.getRandom().nextDouble() - 0.5) * 0.1;

                spawnParticles(world, new Vec3d(node.x + 0.5 + pathOffset.x, (node.y + 0.5) - 0.38 + offsetY, node.z + 0.5 + pathOffset.z),
                        particle, count, spreadX, spreadY, spreadZ, speed);
            }
        }
    }

    /**
     * Spawns a ring of particles that spins around a center point and bobs up and down.
     * Compatible with both Client and Server worlds.
     *
     * @param world          World to spawn in.
     * @param centerPos      Block position to center ring on.
     * @param particle       Particle effect to spawn.
     * @param count          How many particles per tick.
     * @param radius         Radius of the ring in blocks.
     * @param ringThickness  The random horizontal spread around the radius. 0.0 for perfect line, higher for "cloud" ring.
     * @param rotationSpeed  How fast the ring spins (multiplier for world time). Recommended: 0.1 - 0.8.
     * @param bobbingHeight  How high the ring bobs up and down. 0.0 to disable.
     * @param upwardVelocity The vertical velocity applied to the particles.
     * @param yOffset        Global vertical offset for the entire effect.
     */
    public static void spawnSpinningRing(World world, BlockPos centerPos, ParticleEffect particle,
                                         int count, double radius, double ringThickness, double rotationSpeed,
                                         double bobbingHeight, double upwardVelocity, double yOffset) {

        // Calculate the base time offset for horizontal rotation
        double timeOffset = world.getTime() * rotationSpeed;

        // Calculate a separate, slower time offset for vertical bobbing, so the "peak" and
        // "trough" of the wave precess around the circle rather than staying locked to one side
        double bobbingTimeOffset = world.getTime() * (rotationSpeed / 1.5); // 50% speed

        // Calculate the angle step to distribute particles evenly around the circle
        // (2 * PI) represents a full 360-degree circle in radians
        double angleStep = (Math.PI * 2) / count;

        for (int i = 0; i < count; i++) {
            // Calculate the angle for this specific particle
            double angle = timeOffset + (i * angleStep);

            // Calculate the specific radius for this particle (Base + Random Spread)
            // (random - 0.5) * thickness gives a spread centered on the radius line
            double currentRadius = radius + (world.getRandom().nextDouble() - 0.5) * ringThickness;

            // Calculate X and Z (Horizontal position) using the fast rotation
            // Math.cos/sin gives us a value between -1 and 1. Multiply by radius to size the ring.
            // Add 0.5 to center it in the block.
            double x = centerPos.getX() + 0.5 + Math.cos(angle) * currentRadius;
            double z = centerPos.getZ() + 0.5 + Math.sin(angle) * currentRadius;

            // Calculate Y (Vertical position + Bobbing) using the slower bobbing speed
            // Math.sin(timeOffset) creates a wave between -1 and 1 over time.
            double y = centerPos.getY() + 0.5 + yOffset + (Math.sin(bobbingTimeOffset) * bobbingHeight);

            if (world instanceof ServerWorld serverWorld) {
                // Server-side spawning
                serverWorld.spawnParticles(particle, x, y, z, 0, 0.0, upwardVelocity, 0.0, 1.0);
            } else {
                // Client-side spawning
                world.addParticle(particle, x, y, z, 0.0, upwardVelocity, 0.0);
            }
        }
    }

    /**
     * Spawns a spinning ring of particles that is oriented in 3D space based on a given rotation.
     * <p>
     * Useful for tying particle rings to an entity's head or body rotation,
     * allowing the ring to tilt and pan naturally with the entity.
     */
    public static void spawnOrientedSpinningRing(World world, Vec3d centerPos, Quaternionf rotation, ParticleEffect particle,
                                                 int count, double radius, double horizontalRingThickness, double verticalRingThickness,
                                                 double rotationSpeed, double bobbingHeight, double upwardVelocity, double localYOffset) {

        double timeOffset = world.getTime() * rotationSpeed;
        double bobbingTimeOffset = world.getTime() * (rotationSpeed / 1.5);
        double angleStep = (Math.PI * 2) / count;

        for (int i = 0; i < count; i++) {
            double angle = timeOffset + (i * angleStep);
            double currentRadius = radius + (world.getRandom().nextDouble() - 0.5) * horizontalRingThickness;
            double verticalScatter = (world.getRandom().nextDouble() - 0.5) * verticalRingThickness;

            // 1. Calculate offset in local, un-rotated space
            float xOffset = (float) (Math.cos(angle) * currentRadius);
            // Apply local Y offset, bobbing, and vertical scatter to the local Y axis
            float yOffset = (float) (localYOffset + Math.sin(bobbingTimeOffset) * bobbingHeight + verticalScatter);
            float zOffset = (float) (Math.sin(angle) * currentRadius);

            Vector3f offset = new Vector3f(xOffset, yOffset, zOffset);

            // 2. Apply the 3D rotation (Tilts the ring to match head pitch/yaw)
            offset.rotate(rotation);

            // 3. Add to world center position
            double x = centerPos.getX() + offset.x();
            double y = centerPos.getY() + offset.y();
            double z = centerPos.getZ() + offset.z();

            // 4. Rotate velocity vector so "upward" matches the oriented "up"
            Vector3f vel = new Vector3f(0.0f, (float) upwardVelocity, 0.0f);
            if (upwardVelocity != 0.0) {
                vel.rotate(rotation);
            }

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(particle, x, y, z, 0, vel.x(), vel.y(), vel.z(), 1.0);
            } else {
                world.addParticle(particle, x, y, z, vel.x(), vel.y(), vel.z());
            }
        }
    }

    /**
     * Spawns particles in a spherical shell pattern around a center point.
     * Useful for highlighting block boundaries or creating magic shields.
     *
     * @param world          The world to spawn in.
     * @param center         The center position.
     * @param particle       The particle effect.
     * @param count          Number of particles to spawn.
     * @param baseRadius     The base radius of the shell.
     * @param radiusVariance The variance in radius (random 0.0 to variance added to base).
     */
    public static <T extends ParticleEffect> void spawnSphericalShell(World world, Vec3d center, T particle, int count, double baseRadius, double radiusVariance) {
        Random random = world.getRandom();
        for (int i = 0; i < count; i++) {
            // Generate random direction vector
            double rX = random.nextDouble() - 0.5;
            double rY = random.nextDouble() - 0.5;
            double rZ = random.nextDouble() - 0.5;

            // Normalize
            double dist = Math.sqrt(rX * rX + rY * rY + rZ * rZ);
            if (dist < 0.0001) dist = 1.0;

            // Calculate radius (Base + Random variance)
            double radius = baseRadius + (random.nextDouble() * radiusVariance);

            // Calculate final offset
            double offsetX = (rX / dist) * radius;
            double offsetY = (rY / dist) * radius;
            double offsetZ = (rZ / dist) * radius;

            // Spawn using centralized logic
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(particle,
                        center.x + offsetX,
                        center.y + offsetY,
                        center.z + offsetZ,
                        1, 0, 0, 0, 0);
            } else {
                world.addParticle(particle,
                        center.x + offsetX,
                        center.y + offsetY,
                        center.z + offsetZ,
                        0, 0, 0);
            }
        }
    }

    /**
     * Spawns a cloud of particles around a hamster that steadily decays in count over time.
     *
     * @param entity        The hamster entity to spawn particles around.
     * @param particle      The particle effect to spawn.
     * @param durationTicks How long the effect should last.
     * @param startCount    The number of particles to spawn on the first tick.
     * @param spreadXZ      The horizontal spread radius.
     * @param spreadY       The vertical spread radius.
     * @param yOffset       The vertical offset from the entity's feet.
     */
    public static <T extends ParticleEffect> void spawnDecayingParticleCloud(HamsterEntity entity, T particle, int durationTicks, int startCount, double spreadXZ, double spreadY, double yOffset) {
        if (entity.getWorld().isClient()) return;

        long currentTime = entity.getWorld().getTime();

        for (int i = 0; i < durationTicks; i++) {
            int ticksFromNow = i;
            int countForTick = (int) Math.round(startCount * (1.0 - ((double) i / durationTicks)));

            if (countForTick > 0) {
                entity.scheduleTask(currentTime + ticksFromNow, "decay_cloud_" + particle.getClass().getSimpleName(), () -> {
                    if (entity.isAlive() && !entity.isRemoved()) {
                        Vec3d center = new Vec3d(entity.getX(), entity.getY() + yOffset, entity.getZ());
                        spawnParticles(entity.getWorld(), center, particle, countForTick, spreadXZ, spreadY, spreadXZ, 0.0);
                    }
                });
            }
        }
    }

    /**
     * Spawns a cloud of particles representing the genetic probability distribution
     * between two parent hamster colors in the 3D color space.
     *
     * @param world The world to spawn in.
     * @param parentAPos The 3D position of the first parent.
     * @param parentBPos The 3D position of the second parent.
     * @param countPerTick How many particles to spawn per tick.
     */
    public static void spawnGeneticProbabilityCloud(World world, Vec3d parentAPos, Vec3d parentBPos, int countPerTick) {
        for (int i = 0; i < countPerTick; i++) {
            // Utilize exact same math from breeding system
            Vec3d point = ColorSpaceUtil.calculateGeneticMidpoint(parentAPos, parentBPos, world.getRandom());

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.WAX_ON, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            } else {
                world.addParticle(ParticleTypes.WAX_ON, point.x, point.y, point.z, 0, 0, 0);
            }
        }
    }
}