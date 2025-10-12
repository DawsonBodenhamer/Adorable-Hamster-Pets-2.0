package net.dawson.adorablehamsterpets.entity.AI;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class HamsterWanderAroundFarGoal extends WanderAroundFarGoal {
    private final HamsterEntity hamster;
    private static final double BUFFED_WANDER_SPEED = 0.9D;

    public HamsterWanderAroundFarGoal(HamsterEntity hamster, double speed) {
        super(hamster, speed);
        this.hamster = hamster;
        this.setChance(110); // Chance for non-buffed state
    }

    public HamsterWanderAroundFarGoal(HamsterEntity hamster, double speed, float probability) {
        super(hamster, speed, probability);
        this.hamster = hamster;
        this.setChance(110); // Chance for non-buffed state
    }

    @Override
    public boolean canStart() {
        // --- 1. Initial State Checks ---
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isSulking() || this.hamster.isCelebratingDiamond() || this.hamster.isCelebratingChase()) {
            return false;
        }

        // --- 2. "Zoomies" vs. Normal Activation Logic ---
        if (this.hamster.hasGreenBeanBuff()) {

            // For zoomies, use a high-frequency check and bypass the superclass's internal cooldown.
            if (this.mob.getRandom().nextInt(3) != 0) {
                AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Zoomies): FAILED - On cooldown.", this.hamster.getId());
                return false;
            }
            // We must manually find a target here because we are not calling super.canStart().
            Vec3d target = getWanderTarget();
            if (target == null) {
                AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Zoomies): FAILED - No valid target found.", this.hamster.getId());
                return false;
            }
            // Set the target coordinates that the superclass would normally set.
            this.targetX = target.x;
            this.targetY = target.y;
            this.targetZ = target.z;
            AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Zoomies): SUCCEEDED. Target: ({}, {}, {})", this.hamster.getId(), String.format("%.2f", target.x), String.format("%.2f", target.y), String.format("%.2f", target.z));
            return true; // A valid target was found.
        } else {
            // For normal wandering, defer to the superclass, which includes the 120-tick cooldown.
            boolean canStartNormal = super.canStart();
            AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Normal): Result: {}", this.hamster.getId(), canStartNormal);
            return canStartNormal;
        }
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.hasGreenBeanBuff()) {
            // For zoomies, the goal should now stop if it's interrupted OR if it has reached its destination.
            // This allows the canStart() cooldown to be checked again.
            return !(this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut())
                    && !this.mob.getNavigation().isIdle();
        } else {
            // For normal wandering, use the default behavior.
            return super.shouldContinue();
        }
    }

    @Override
    public void tick() {
        // For "zoomies" mode, if the hamster reaches its destination, immediately find a new one.
        if (this.hamster.hasGreenBeanBuff() && this.mob.getNavigation().isIdle()) {
            AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] tick (Zoomies): Navigation is idle. Finding new target.", this.hamster.getId());
            Vec3d newTarget = this.getWanderTarget();
            if (newTarget != null) {
                this.mob.getNavigation().startMovingTo(newTarget.x, newTarget.y, newTarget.z, BUFFED_WANDER_SPEED);
            }
        }
        // For normal wandering, the superclass tick is empty, so we don't need to call it.
    }

    @Override
    public void start() {
        // --- Determine Speed Dynamically ---
        double currentSpeed = this.hamster.hasGreenBeanBuff() ? BUFFED_WANDER_SPEED : this.speed;
        this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, currentSpeed);

        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
        AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] start: Goal has started. IsBuffed: {}", this.hamster.getId(), this.hamster.hasGreenBeanBuff());
    }

    @Nullable
    @Override
    protected Vec3d getWanderTarget() {
        if (this.hamster.hasGreenBeanBuff()) {
            // Convert the BlockPos from our precise helper to a Vec3d for the goal.
            return getPreciseZoomiesTarget().map(Vec3d::ofCenter).orElse(null);
        } else {
            return super.getWanderTarget();
        }
    }

    /**
     * Calculates a precise point on a circle around the owner and finds the nearest safe,
     * reachable block. This avoids the imprecision of FuzzyTargeting.
     *
     * @return An Optional containing the safe BlockPos, or empty if none is found.
     */
    private Optional<BlockPos> getPreciseZoomiesTarget() {
        if (!(this.hamster.getOwner() instanceof PlayerEntity owner)) {
            return Optional.empty();
        }

        // --- Circular Pathing Logic ---
        double lastAngle = this.hamster.getLastZoomiesAngle();
        boolean isClockwise = this.hamster.getZoomiesIsClockwise();

        // Calculate the next angle step (degrees in radians).
        double angleStep = Math.toRadians(this.hamster.getRandom().nextBetween(40, 70));
        double newAngle = isClockwise ? lastAngle + angleStep : lastAngle - angleStep;
        this.hamster.setLastZoomiesAngle(newAngle); // Persist the new angle on the entity.

        // Calculate a new random point on the circumference of a circle whose radius is also randomized.
        int radiusModifier = this.hamster.getZoomiesRadiusModifier();
        double radius = this.hamster.getRandom().nextBetween(3 + radiusModifier, 5 + radiusModifier);
        double targetX = owner.getX() + radius * Math.cos(newAngle);
        double targetZ = owner.getZ() + radius * Math.sin(newAngle);

        // --- Precise Position Finding ---
        BlockPos idealPos = new BlockPos((int)targetX, (int)this.hamster.getY(), (int)targetZ);
        // Use the hamster's own safe spawn finder to locate a valid spot near our ideal point.

        // --- LOGGING ---
        Optional<BlockPos> finalTargetPos = this.hamster.findSafeSpawnPosition(idealPos, this.hamster.getWorld(), 2);
        AdorableHamsterPets.LOGGER.trace(
                "[WanderGoal-{}] getPreciseZoomiesTarget:\n  - IsClockwise: {}\n  - LastAngle(rad): {}\n  - AngleStep(rad): {}\n  - NewAngle(rad): {}\n  - Radius: {}\n  - IdealPos: {}\n  - FinalTarget: {}",
                this.hamster.getId(),
                isClockwise,
                String.format("%.2f", lastAngle),
                String.format("%.2f", angleStep),
                String.format("%.2f", newAngle),
                String.format("%.2f", radius),
                idealPos,
                finalTargetPos.map(BlockPos::toString).orElse("null")
        );

        return this.hamster.findSafeSpawnPosition(idealPos, this.hamster.getWorld(), 2);
    }


    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
        AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] stop: Goal has stopped.", this.hamster.getId());
    }
}