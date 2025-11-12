package net.dawson.adorablehamsterpets.util;

import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class for spawning a trail of mycelium particles along the future nodes of a given path.
 */
public class ParticleBreadcrumbHelper {

    public static void spawnBreadcrumbs(ServerWorld world, @Nullable Path path) {
        if (path == null) {
            return;
        }

        int currentNodeIndex = path.getCurrentNodeIndex();
        int pathLength = path.getLength();

        // Iterate from the current node to the end of the path
        for (int i = currentNodeIndex; i < pathLength; i++) {
            PathNode node = path.getNode(i);
            Vec3d directionVector = Vec3d.ZERO; // Default to no direction

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
                double distanceAlongPath = world.getRandom().nextDouble(); // Random value from 0.0 to 1.0
                Vec3d pathOffset = directionVector.multiply(distanceAlongPath);

                // 3. Calculate limited vertical offset.
                double offsetY = (world.getRandom().nextDouble() - 0.5) * 0.1;

                world.spawnParticles(
                        ParticleTypes.MYCELIUM,
                        node.x + 0.5 + pathOffset.x,      // Center X + directional offset X
                        (node.y + 0.5) - 0.38 + offsetY,     // Center Y + limited vertical offset
                        node.z + 0.5 + pathOffset.z,         // Center Z + directional offset Z
                        1,                                   // Count is 1
                        0.2, 0.0, 0.2,          // Vertical Spread is 0
                        3                                    // Speed
                );
            }
        }
    }
}