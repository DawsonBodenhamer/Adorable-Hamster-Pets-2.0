package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LookAroundGoalAccessor;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.mob.MobEntity;

public class HamsterLookAroundGoal extends LookAroundGoal {

    // --- 1. Fields ---
    private final MobEntity hamsterMob; // Store our own reference
    // --- End 1. Fields ---

    // --- 2. Constructor ---
    public HamsterLookAroundGoal(MobEntity mob) {
        super(mob);
        this.hamsterMob = mob; // Initialize our reference
    }
    // --- End 2. Constructor ---

    // --- 3. Overridden Methods ---
    @Override
    public boolean canStart() {
        // Use the accessor to get the mob field
        LookAroundGoalAccessor accessor = (LookAroundGoalAccessor) this;
        MobEntity mob = accessor.getMob();

        // First, perform the vanilla probability check.
        if (mob.getRandom().nextFloat() >= 0.02F) { // Vanilla's hardcoded chance
            return false;
        }
        // Check Hamster State
        // Use our stored 'hamsterMob' reference
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            return !hamster.isSitting() && !hamster.isSleeping() && !hamster.isKnockedOut() && !hamster.isSulking()
                    && !hamster.isStealingDiamond() && !hamster.isCelebratingChase()
                    && !hamster.getActiveCustomGoalDebugName().equals(HamsterWanderAroundFarGoal.class.getSimpleName());
        }
        return true;
    }

    @Override
    public void start() {
        super.start();
        if (this.hamsterMob instanceof HamsterEntity he) {
            he.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
            AdorableHamsterPets.LOGGER.trace("[AI Goal Start] Hamster {} started LookAroundGoal.", he.getId());
        }
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Check Hamster State ---
        // Use our stored 'hamsterMob' reference
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking() || hamster.isStealingDiamond() || hamster.isCelebratingChase()) {
                return false;
            }
        }
        // --- End 1. Check Hamster State ---
        return super.shouldContinue();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamsterMob instanceof HamsterEntity he) {
            if (he.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
                he.setActiveCustomGoalDebugName("None");
            }
        }
    }

    @Override
    public void tick() {
        LookAroundGoalAccessor accessor = (LookAroundGoalAccessor) this;
        MobEntity mob = accessor.getMob(); // Get the mob via accessor

        // Replicate the vanilla logic of decrementing the timer
        accessor.setLookTime(accessor.getLookTime() - 1);

        // Use our centralized constants for rotation speed
        mob.getLookControl().lookAt(
                mob.getX() + accessor.getDeltaX(),
                mob.getEyeY(),
                mob.getZ() + accessor.getDeltaZ(),
                HamsterEntity.FAST_YAW_CHANGE,
                HamsterEntity.FAST_PITCH_CHANGE
        );
    }
    // --- End 3. Overridden Methods ---
}