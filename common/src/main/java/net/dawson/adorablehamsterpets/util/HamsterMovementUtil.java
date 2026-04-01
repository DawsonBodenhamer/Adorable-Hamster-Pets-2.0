package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates movement mathematics for fleeing and taunting behaviors.
 */
public final class HamsterMovementUtil {

    private HamsterMovementUtil() {}

    /**
     * Forces the mob to look at the target entity using this mod's fast rotation speed.
     *
     * @param mob    The observer.
     * @param target The entity to look at.
     */
    public static void faceEntity(MobEntity mob, Entity target) {
        if (mob != null && target != null) {
            mob.getLookControl().lookAt(target, 25.0f, 25.0f);
        }
    }

    /**
     * Forces the mob to look at a specific world coordinate using this mod's fast rotation speed.
     *
     * @param mob The observer.
     * @param x   Target X.
     * @param y   Target Y.
     * @param z   Target Z.
     */
    public static void facePosition(MobEntity mob, double x, double y, double z) {
        if (mob != null) {
            mob.getLookControl().lookAt(x, y, z, 25.0f, 25.0f);
        }
    }

    /**
     * Determines if the runner is too close to the chaser and needs to flee.
     *
     * @param runner      The entity running away.
     * @param chaser      The entity chasing.
     * @param minFleeDist The minimum safe distance.
     * @return True if the runner is too close.
     */
    public static boolean shouldFlee(Entity runner, Entity chaser, double minFleeDist) {
        return runner.squaredDistanceTo(chaser) < minFleeDist * minFleeDist;
    }

    /**
     * Determines if the runner has reached a safe distance and should stop fleeing (start taunting).
     * Uses hysteresis (maxFleeDist) to prevent jittery start/stop behavior.
     *
     * @param runner      The entity running away.
     * @param chaser      The entity chasing.
     * @param maxFleeDist The distance at which the runner feels safe enough to stop.
     * @return True if the runner is safe.
     */
    public static boolean shouldStopFleeing(Entity runner, Entity chaser, double maxFleeDist) {
        return runner.squaredDistanceTo(chaser) > maxFleeDist * maxFleeDist;
    }

    /**
     * Finds a valid position for the runner to flee to, away from the chaser.
     *
     * @param runner      The entity running away.
     * @param chaser      The entity chasing.
     * @param minDistance Minimum distance for the generated path point.
     * @param maxDistance Maximum distance for the generated path point.
     * @return A Vec3d coordinate to run to, or null if no path found.
     */
    @Nullable
    public static Vec3d findFleePosition(PathAwareEntity runner, Entity chaser, double minDistance, double maxDistance) {
        // Find a position away from the chaser
        // FuzzyTargeting.findFrom creates a target vector away from the provided start pos (the chaser)
        // arg 2: horizontal spread, arg 3: vertical spread
        return FuzzyTargeting.findFrom(runner, (int) maxDistance, 7, chaser.getPos());
    }

    /**
     * Determines if the hamster is in a state that forbids following another entity.
     *
     * @param hamster The hamster to check.
     * @return True if the hamster should not follow, false otherwise.
     */
    public static boolean shouldNotFollow(HamsterEntity hamster) {
        return hamster.isSitting() ||
                hamster.isSleeping() ||
                hamster.isKnockedOut() ||
                hamster.isSulking() ||
                hamster.isCelebratingDiamond() ||
                hamster.isCelebratingRetrieval() ||
                hamster.isPlayingTag() ||
                hamster.isCelebratingBaby() ||
                hamster.isWanderModeActive();
    }
}