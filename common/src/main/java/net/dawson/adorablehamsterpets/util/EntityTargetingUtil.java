package net.dawson.adorablehamsterpets.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Utility for calculating entity orientation and line-of-sight relationships.
 */
public final class EntityTargetingUtil {

    private EntityTargetingUtil() {}

    /**
     * Checks if the observer is roughly facing the target entity based on head rotation.
     * <p>
     * <b>Use Case:</b> AI logic (e.g., Mob looking at Player).
     *
     * @param observer     The entity doing the looking.
     * @param target       The entity being looked at.
     * @param dotThreshold The strictness of the angle (0.0 to 1.0).
     *                     Higher values require the head to be centered more precisely on the target.
     *                     0.5 covers ~60 degrees FOV, 0.9 covers ~25 degrees.
     * @return True if the observer's look vector is aligned with the target within the threshold.
     */
    public static boolean isFacing(LivingEntity observer, LivingEntity target, double dotThreshold) {
        if (observer == null || target == null) return false;

        Vec3d observerLook = observer.getRotationVec(1.0F).normalize();
        Vec3d vecToTarget = target.getEyePos().subtract(observer.getEyePos()).normalize();

        return observerLook.dotProduct(vecToTarget) > dotThreshold;
    }

    /**
     * Checks if the observer's crosshair (look ray) physically intersects the target's hitbox.
     * <p>
     * <b>Use Case:</b> Player interaction checks (e.g., is Player looking directly at Mob?).
     *
     * @param observer    The entity doing the looking.
     * @param target      The entity being looked at.
     * @param maxDistance The maximum distance to check (e.g., 4.0 for interaction range, 32.0 for line of sight).
     * @return True if the observer's look ray intersects the target's bounding box.
     */
    public static boolean isLookingAt(LivingEntity observer, LivingEntity target, double maxDistance) {
        if (observer == null || target == null) return false;

        Vec3d eyePos = observer.getEyePos();
        Vec3d lookVec = observer.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));

        // Get target box
        Box targetBox = target.getBoundingBox();

        // Calculate intersection
        Optional<Vec3d> hit = targetBox.raycast(eyePos, endPos);

        return hit.isPresent();
    }

    /**
     * Checks if two entities are facing each other (looking in opposite directions).
     * This compares the look vectors of both entities.
     *
     * @param entity1      The first entity.
     * @param entity2      The second entity.
     * @param dotThreshold The threshold for "opposite" (positive value, e.g. 0.8).
     * @return True if the entities are facing each other.
     */
    public static boolean areFacingEachOther(LivingEntity entity1, LivingEntity entity2, double dotThreshold) {
        if (entity1 == null || entity2 == null) return false;

        Vec3d look1 = entity1.getRotationVec(1.0F).normalize();
        Vec3d look2 = entity2.getRotationVec(1.0F).normalize();

        // If dot product is close to -1, vectors are opposite (facing each other)
        return look1.dotProduct(look2) < -dotThreshold;
    }
}