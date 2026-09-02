package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LookAtEntityGoalAccessor;
import net.dawson.adorablehamsterpets.util.EntityTargetingUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

public class HamsterLookAtEntityGoal extends LookAtPlayerGoal {

    // --- Fields ---
    private final Mob hamsterMob;
    private int gazeCheckTimer = 0;

    // --- Constructors ---
    public HamsterLookAtEntityGoal(Mob mob, Class<? extends LivingEntity> targetType, float range) {
        super(mob, targetType, range, 0.02F);
        this.hamsterMob = mob;
    }

    public HamsterLookAtEntityGoal(Mob mob, Class<? extends LivingEntity> targetType, float range, float chance) {
        super(mob, targetType, range, chance);
        this.hamsterMob = mob;
    }

    public HamsterLookAtEntityGoal(Mob mob, Class<? extends LivingEntity> targetType, float range, float chance, boolean lookForward) {
        super(mob, targetType, range, chance, lookForward);
        this.hamsterMob = mob;
    }

    // --- Overridden Methods ---
    @Override
    public boolean canUse() {
        // --- 1. Hamster State Check ---
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isOrderedToSit() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking()
                    || hamster.isHoldingMouthItem() || hamster.isFrozenMovement() || hamster.isCelebratingDiamond()
                    || hamster.isCelebratingBaby() || hamster.getActiveCustomGoalName().equals(HamsterWanderAroundFarGoal.class.getSimpleName())) {
                return false;
            }
        }

        // --- 2. Defer to Superclass Logic ---
        boolean superCanStart = super.canUse();
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

        int baseDuration = Configs.AHP_MAIN.lookAtDuration.get();
        // Logic: Base + (0 to 4 seconds) with a constant variance of 80 ticks
        int calculatedDuration = baseDuration + this.mob.getRandom().nextInt(80);

        // Adjust for tick rates if necessary
        ((LookAtEntityGoalAccessor) this).setLookTime(this.adjustedTickDelay(calculatedDuration));

        if (this.mob instanceof HamsterEntity hamster) {
            hamster.isLookAtEntityGoalActive = true;
            hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
            hamster.getEntityData().set(HamsterEntity.CURRENT_LOOK_UP_ANIM_ID, hamster.getRandom().nextIntBetweenInclusive(1, 3));
            AdorableHamsterPets.LOGGER.trace("[AI Goal Start] Hamster {} started LookAtEntityGoal with duration {} ticks (Base: {} + Random).", hamster.getId(), calculatedDuration, baseDuration);
        }
    }

    @Override
    public boolean canContinueToUse() {
        // --- 1. Check Hamster State ---
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isOrderedToSit() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking()
                    || hamster.isHoldingMouthItem() || hamster.isFrozenMovement() || hamster.isCelebratingDiamond() || hamster.isCelebratingBaby()) {
                return false;
            }
        }
        return super.canContinueToUse();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.mob instanceof HamsterEntity hamster) {
            hamster.isLookAtEntityGoalActive = false;
            hamster.hasMutualGaze = false;
            if (hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
                hamster.setActiveCustomGoalName("None");
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
            if (this.gazeCheckTimer-- <= 0) {
                this.gazeCheckTimer = 5;
                // If player's crosshair is on hamster, sustain eye contact
                if (target instanceof LivingEntity livingTarget && EntityTargetingUtil.isLookingAt(livingTarget, this.mob, 5.0, 0.0)) {
                    if (this.mob instanceof HamsterEntity hamster) hamster.hasMutualGaze = true;
                } else {
                    if (this.mob instanceof HamsterEntity hamster) hamster.hasMutualGaze = false;
                }
            }

            if (this.mob instanceof HamsterEntity hamster && hamster.hasMutualGaze) {
                // Reset timer to 60 ticks to sustain gaze
                accessor.setLookTime(60);
            } else {
                // Count down normally
                accessor.setLookTime(accessor.getLookTime() - 1);
            }
        }
    }
}