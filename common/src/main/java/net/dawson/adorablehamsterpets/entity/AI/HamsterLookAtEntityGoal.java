package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LookAtEntityGoalAccessor;
import net.dawson.adorablehamsterpets.util.EntityTargetingUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.mob.MobEntity;

public class HamsterLookAtEntityGoal extends LookAtEntityGoal {

    // --- Fields ---
    private final MobEntity hamsterMob;
    private final float chance;

    // --- Constructors ---
    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range) {
        super(mob, targetType, range);
        this.hamsterMob = mob;
        this.chance = 0.02F;
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance) {
        super(mob, targetType, range, chance);
        this.hamsterMob = mob;
        this.chance = chance;
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance, boolean lookForward) {
        super(mob, targetType, range, chance, lookForward);
        this.hamsterMob = mob;
        this.chance = chance;
    }

    // --- Overridden Methods ---
    @Override
    public boolean canStart() {
        // --- 1. Hamster State Check ---
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking()
                    || hamster.isHoldingMouthItem() || hamster.isCelebratingRetrieval() || hamster.isCelebratingDiamond()
                    || hamster.getActiveCustomGoalDebugName().equals(HamsterWanderAroundFarGoal.class.getSimpleName())) {
                return false;
            }
        }

        // --- 2. Defer to Superclass Logic ---
        boolean superCanStart = super.canStart();
        if (!superCanStart) {
            AdorableHamsterPets.LOGGER.trace("[LookAtGoal-{}] canStart FAILED: super.canStart() returned false (chance or no target).", this.hamsterMob.getId());
            return false;
        }

        // --- 3. Success ---
        AdorableHamsterPets.LOGGER.trace("[LookAtGoal-{}] canStart SUCCEEDED. All checks passed.", this.hamsterMob.getId());
        return true;
    }

    @Override
    public void start() {
        super.start();

        int baseDuration = Configs.AHP.lookAtDuration.get();
        // Logic: Base + (0 to 4 seconds) with a constant variance of 80 ticks
        int calculatedDuration = baseDuration + this.mob.getRandom().nextInt(80);

        // Adjust for tick rates if necessary
        ((LookAtEntityGoalAccessor) this).setLookTime(this.getTickCount(calculatedDuration));

        if (this.mob instanceof HamsterEntity hamster) {
            hamster.isLookAtEntityGoalActive = true;
            hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
            hamster.getDataTracker().set(HamsterEntity.CURRENT_LOOK_UP_ANIM_ID, hamster.getRandom().nextBetween(1, 3));
            AdorableHamsterPets.LOGGER.trace("[AI Goal Start] Hamster {} started LookAtEntityGoal with duration {} ticks (Base: {} + Random).", hamster.getId(), calculatedDuration, baseDuration);
        }
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Check Hamster State ---
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking()
                    || hamster.isHoldingMouthItem() || hamster.isCelebratingRetrieval() || hamster.isCelebratingDiamond()) {
                return false;
            }
        }
        return super.shouldContinue();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.mob instanceof HamsterEntity hamster) {
            hamster.isLookAtEntityGoalActive = false;
            if (hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
                hamster.setActiveCustomGoalDebugName("None");
            }
        }
    }

    @Override
    public void tick() {
        LookAtEntityGoalAccessor accessor = (LookAtEntityGoalAccessor) this;
        Entity target = accessor.getTarget();

        if (target != null && target.isAlive()) {
            double targetY = accessor.getLookForward() ? this.mob.getEyeY() : target.getEyeY();

            // Fast turn speed
            HamsterMovementUtil.facePosition(
                    this.mob,
                    target.getX(),
                    targetY,
                    target.getZ()
            );

            // --- Dynamic Gaze Logic ---
            // If player's crosshair is on hamster, sustain eye contact
            if (target instanceof LivingEntity livingTarget && EntityTargetingUtil.isLookingAt(livingTarget, this.mob, 5.0, 0.0)) {
                // Reset timer to 60 ticks to sustain gaze
                accessor.setLookTime(60);
            } else {
                // Count down normally
                accessor.setLookTime(accessor.getLookTime() - 1);
            }
        }
    }
}