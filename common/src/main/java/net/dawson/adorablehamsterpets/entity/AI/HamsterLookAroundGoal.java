package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LookAroundGoalAccessor;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.mob.MobEntity;

public class HamsterLookAroundGoal extends LookAroundGoal {

    // --- Fields ---
    private final MobEntity hamsterMob;

    // --- Constructor ---
    public HamsterLookAroundGoal(MobEntity mob) {
        super(mob);
        this.hamsterMob = mob;
    }

    // --- Overrides ---
    @Override
    public boolean canStart() {
        LookAroundGoalAccessor accessor = (LookAroundGoalAccessor) this;
        MobEntity mob = accessor.getMob();

        // Perform vanilla probability check
        if (mob.getRandom().nextFloat() >= 0.02F) {
            return false;
        }

        // Check Hamster State
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            return !hamster.isSitting() && !hamster.isSleeping() && !hamster.isKnockedOut() && !hamster.isSulking()
                    && !hamster.isHoldingMouthItem() && !hamster.isCelebratingRetrieval() && !hamster.isCelebratingDiamond()
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
        if (this.hamsterMob instanceof HamsterEntity he) {
            if (he.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
                he.setActiveCustomGoalDebugName("None");
            }
        }
    }

    @Override
    public void tick() {
        LookAroundGoalAccessor accessor = (LookAroundGoalAccessor) this;
        MobEntity mob = accessor.getMob();

        // Replicate the vanilla logic of decrementing the timer
        accessor.setLookTime(accessor.getLookTime() - 1);

        // Fast turn speed
        HamsterMovementUtil.facePosition(
                mob,
                mob.getX() + accessor.getDeltaX(),
                mob.getEyeY(),
                mob.getZ() + accessor.getDeltaZ()
        );
    }
}