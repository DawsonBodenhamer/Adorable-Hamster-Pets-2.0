package net.dawson.adorablehamsterpets.entity.AI;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.EnumSet;

public class HamsterSleepGoal extends Goal {

    // --- 1. Constants and Static Utilities ---
    private static final int CHECK_INTERVAL = 20; // Check for threats every second

    // --- 2. Fields ---
    private final HamsterEntity hamster;
    private int checkTimer = 0;

    // --- 3. Constructors ---
    public HamsterSleepGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        // Control movement and look to prevent interference
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    // --- 4. Public Methods (Overrides from Goal) ---
    @Override
    public boolean canStart() {
        // Only wild hamsters sleep via this goal.
        if (this.hamster.isTamed() ||
                this.hamster.isSleeping() ||
                this.hamster.isSitting() ||
                this.hamster.isKnockedOut()) {
            return false;
        }
        if (!this.hamster.getWorld().isDay()) {
            return false;
        }
        if (!this.hamster.isOnGround()) {
            return false;
        }
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = CHECK_INTERVAL;
        double radius = 5.0;
        boolean threatNearby = !this.hamster.getWorld().getOtherEntities(
                this.hamster,
                this.hamster.getBoundingBox().expand(radius),
                this::isThreat
        ).isEmpty();
        return !threatNearby;
    }

    /**
     * Called when the goal starts. Sets the hamster to a sleeping state,
     * plays a sleep sound, and triggers the wild settle sleep animation.
     */
    @Override
    public void start() {
        // --- Stop Movement and Targeting ---
        this.hamster.getNavigation().stop();
        this.hamster.setTarget(null);

        // --- Set Sleep State ---
        this.hamster.setSleeping(true);
        this.hamster.setInSittingPose(true); // Vanilla flag to prevent other AI movement

        // --- Trigger Wild Settle Sleep Animation ---
        if (!this.hamster.getWorld().isClient()) { // Ensure server-side
            // 1. Randomly select a sleep pose (1, 2, or 3)
            int choice = this.hamster.getRandom().nextInt(3);
            String settleAnimId;
            String deepSleepAnimIdForTracker;

            switch (choice) {
                case 0 -> {
                    settleAnimId = "anim_hamster_stand_settle_sleep1";
                    deepSleepAnimIdForTracker = "anim_hamster_sleep_pose1";
                }
                case 1 -> {
                    settleAnimId = "anim_hamster_stand_settle_sleep2";
                    deepSleepAnimIdForTracker = "anim_hamster_sleep_pose2";
                }
                default -> { // case 2
                    settleAnimId = "anim_hamster_stand_settle_sleep3";
                    deepSleepAnimIdForTracker = "anim_hamster_sleep_pose3";
                }
            }

            // 2. Store the chosen deep sleep animation name in the DataTracker
            this.hamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, deepSleepAnimIdForTracker);

            // 3. Trigger the corresponding settle animation
            this.hamster.triggerAnimOnServer("mainController", settleAnimId);
        }

        // --- Play Sound ---
        if (!this.hamster.getWorld().isClient()) {
            SoundEvent sleepSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SLEEP_SOUNDS, this.hamster.getRandom());
            if (sleepSound != null) {
                this.hamster.getWorld().playSound(
                        null,
                        this.hamster.getBlockPos(),
                        sleepSound,
                        SoundCategory.NEUTRAL,
                        1.0F,
                        1.0F
                );
            }
        }
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.isTamed() || !this.hamster.getWorld().isDay()) {
            return false;
        }
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return true;
        }
        this.checkTimer = CHECK_INTERVAL;
        double radius = 5.0;
        boolean threatNearby = !this.hamster.getWorld().getOtherEntities(
                this.hamster,
                this.hamster.getBoundingBox().expand(radius),
                this::isThreat
        ).isEmpty();
        return !threatNearby;
    }

    @Override
    public void stop() {
        this.hamster.setSleeping(false);
        this.hamster.setInSittingPose(false);
        this.checkTimer = 0;

        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    // --- 5. Private Helper Methods ---

    /**
     * Determines if the given entity is considered a threat to a sleeping wild hamster,
     * which would cause it to wake up.
     *
     * @param entity The entity to check.
     * @return True if the entity is a threat, false otherwise.
     */
    private boolean isThreat(Entity entity) {
        if (entity instanceof HostileEntity) {
            return true;
        }
        if (entity instanceof PlayerEntity) {
            return true;
        }
        return false;
    }
}