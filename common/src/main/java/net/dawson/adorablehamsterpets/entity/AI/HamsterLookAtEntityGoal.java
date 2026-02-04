package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LookAtEntityGoalAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.mob.MobEntity;

public class HamsterLookAtEntityGoal extends LookAtEntityGoal {

    // --- 1. Fields ---
    private final MobEntity hamsterMob;
    private final float chance;

    // --- 2. Constructors ---
    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range) {
        super(mob, targetType, range);
        this.hamsterMob = mob;
        this.chance = 0.02F;
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance) {
        super(mob, targetType, range, chance);
        this.hamsterMob = mob;
        this.chance = chance; // Store the chance
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance, boolean lookForward) {
        super(mob, targetType, range, chance, lookForward);
        this.hamsterMob = mob; // Initialize our reference
        this.chance = chance; // Initialize the chance
    }

    // --- 3. Overridden Methods ---
    @Override
    public boolean canStart() {
        // --- 1. Hamster State Check ---
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking() || hamster.isHoldingInterestItem()
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

        if (this.mob instanceof HamsterEntity he) {
            he.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
            he.getDataTracker().set(HamsterEntity.CURRENT_LOOK_UP_ANIM_ID, he.getRandom().nextBetween(1, 3));
            AdorableHamsterPets.LOGGER.trace("[AI Goal Start] Hamster {} started LookAtEntityGoal with duration {} ticks (Base: {} + Random).", he.getId(), calculatedDuration, baseDuration);
        }
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Check Hamster State ---
        // Use our stored 'hamsterMob' reference
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking() || hamster.isHoldingInterestItem()) {
                return false;
            }
        }
        return super.shouldContinue();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.mob instanceof HamsterEntity he) {
            if (he.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
                he.setActiveCustomGoalDebugName("None");
            }
        }
    }

    @Override
    public void tick() {
        LookAtEntityGoalAccessor accessor = (LookAtEntityGoalAccessor) this;
        Entity target = accessor.getTarget();

        if (target != null && target.isAlive()) {
            double targetY = accessor.getLookForward() ? this.mob.getEyeY() : target.getEyeY();
            // Use our centralized constants for rotation speed
            this.mob.getLookControl().lookAt(target.getX(), targetY, target.getZ(), HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);
            accessor.setLookTime(accessor.getLookTime() - 1);
        }
    }
}