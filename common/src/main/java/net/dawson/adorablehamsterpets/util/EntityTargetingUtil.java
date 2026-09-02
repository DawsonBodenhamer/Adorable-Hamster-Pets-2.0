package net.dawson.adorablehamsterpets.util;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

        Vec3 observerLook = observer.getViewVector(1.0F).normalize();
        Vec3 vecToTarget = target.getEyePosition().subtract(observer.getEyePosition()).normalize();

        return observerLook.dot(vecToTarget) > dotThreshold;
    }

    /**
     * Checks if the observer's crosshair intersects the target's hitbox, with an optional padding
     * to make the detection more forgiving.
     * <p>
     * <b>Use Case:</b> Player interaction checks (e.g., is Player looking directly at Mob?).
     *
     * @param observer    The entity doing the looking.
     * @param target      The entity being looked at.
     * @param maxDistance The maximum distance to check (e.g., 4.0 for interaction range, 32.0 for line of sight).
     * @param padding     The amount to expand the target's bounding box by.
     * @return True if the observer's look ray intersects the target's bounding box.
     */
    public static boolean isLookingAt(LivingEntity observer, LivingEntity target, double maxDistance, double padding) {
        if (observer == null || target == null) return false;

        Vec3 eyePos = observer.getEyePosition();
        Vec3 lookVec = observer.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

        // Get target box and apply optional padding
        AABB targetBox = target.getBoundingBox();
        if (padding > 0.0) {
            targetBox = targetBox.inflate(padding);
        }

        // Calculate intersection
        Optional<Vec3> hit = targetBox.clip(eyePos, endPos);

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

        Vec3 look1 = entity1.getViewVector(1.0F).normalize();
        Vec3 look2 = entity2.getViewVector(1.0F).normalize();

        // If dot product is close to -1, vectors are opposite (facing each other)
        return look1.dot(look2) < -dotThreshold;
    }
}