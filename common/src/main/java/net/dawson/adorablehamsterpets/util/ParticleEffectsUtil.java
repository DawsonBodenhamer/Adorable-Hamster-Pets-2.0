package net.dawson.adorablehamsterpets.util;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Centralized utility for spawning fancy particle formations.
 */
public class ParticleEffectsUtil {

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
}