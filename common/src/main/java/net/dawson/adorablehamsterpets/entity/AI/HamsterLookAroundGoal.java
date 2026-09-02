package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LookAroundGoalAccessor;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

public class HamsterLookAroundGoal extends RandomLookAroundGoal {

    // --- Fields ---
    private final Mob hamsterMob;

    // --- Constructor ---
    public HamsterLookAroundGoal(Mob mob) {
        super(mob);
        this.hamsterMob = mob;
    }

    // --- Overrides ---
    @Override
    public boolean canUse() {
        LookAroundGoalAccessor accessor = (LookAroundGoalAccessor) this;
        Mob mob = accessor.getMob();

        // Perform vanilla probability check
        if (mob.getRandom().nextFloat() >= 0.02F) {
            return false;
        }

        // Check Hamster State
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            return !hamster.isOrderedToSit()
                    && !hamster.isSleeping()
                    && !hamster.isKnockedOut()
                    && !hamster.isSulking()
                    && !hamster.isHoldingMouthItem()
                    && !hamster.isFrozenMovement()
                    && !hamster.isCelebratingDiamond()
                    && !hamster.isCelebratingBaby()
                    && !hamster.getActiveCustomGoalName().equals(HamsterWanderAroundFarGoal.class.getSimpleName())
                    && !hamster.getActiveCustomGoalName().equals("Escaping Water");
        }
        return true;
    }

    @Override
    public void start() {
        super.start();
        if (this.hamsterMob instanceof HamsterEntity he) {
            he.setActiveCustomGoalName(this.getClass().getSimpleName());
            AdorableHamsterPets.LOGGER.trace("[AI Goal Start] Hamster {} started LookAroundGoal.", he.getId());
        }
    }

    @Override
    public boolean canContinueToUse() {
        // --- 1. Check Hamster State ---
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isOrderedToSit()
                    || hamster.isSleeping()
                    || hamster.isKnockedOut()
                    || hamster.isSulking()
                    || hamster.isHoldingMouthItem()
                    || hamster.isFrozenMovement()
                    || hamster.isCelebratingDiamond()
                    || hamster.isCelebratingBaby()
                    || hamster.getActiveCustomGoalName().equals("Escaping Water")) {
                return false;
            }
        }
        return super.canContinueToUse();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamsterMob instanceof HamsterEntity he) {
            if (he.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
                he.setActiveCustomGoalName("None");
            }
        }
    }

    @Override
    public void tick() {
        LookAroundGoalAccessor accessor = (LookAroundGoalAccessor) this;
        Mob mob = accessor.getMob();

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