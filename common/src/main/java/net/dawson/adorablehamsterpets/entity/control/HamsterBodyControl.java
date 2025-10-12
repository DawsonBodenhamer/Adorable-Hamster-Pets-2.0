package net.dawson.adorablehamsterpets.entity.control;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.mob.MobEntity;

/**
 * A custom BodyControl that overrides the vanilla logic to ensure instant, unified rotation
 * for the GeckoLib-based Hamster model, while still respecting the difference between
 * movement-based rotation and look-based rotation.
 * <p>
 * This implementation forces the body to sync with the movement yaw when walking, and
 * instantly sync with the head yaw when standing still. This allows LookControl to
 * function correctly without the undesirable slow interpolation of the vanilla BodyControl.
 */
public class HamsterBodyControl extends BodyControl {
    private final MobEntity entity;

    public HamsterBodyControl(MobEntity entity) {
        super(entity);
        this.entity = entity;
    }

    /**
     * Overrides the default body rotation logic to force an immediate sync based on entity state.
     */
    @Override
    public void tick() {
        // If the hamster is moving (pathfinding), its body should face the direction of movement.
        if (this.isMoving()) {
            this.entity.bodyYaw = this.entity.getYaw();
        } else {
            // If the hamster is standing still, its body should instantly face where its head is looking.
            // This allows LookControl and AI goals to turn the hamster in place without a slow delay.
            this.entity.bodyYaw = this.entity.headYaw;
        }
    }

    /**
     * Checks if the entity has moved significantly since the last tick.
     * This logic is copied directly from the vanilla BodyControl class.
     * @return True if the entity is moving, false otherwise.
     */
    private boolean isMoving() {
        double d = this.entity.getX() - this.entity.prevX;
        double e = this.entity.getZ() - this.entity.prevZ;
        // A very small threshold to detect any horizontal movement.
        return d * d + e * e > 2.5000003E-7F;
    }
}