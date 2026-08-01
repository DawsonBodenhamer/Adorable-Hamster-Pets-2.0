package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.dawson.adorablehamsterpets.util.HamsterLureUtil;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;

public class HamsterTemptGoal extends TemptGoal {

    // --- 1. Fields ---
    private final HamsterEntity hamster;
    private int recheckTimer = 0; // Frequency of begging state updates

    // --- 2. Constructors ---
    public HamsterTemptGoal(HamsterEntity hamster, double speed, boolean canBeScared) {
        super(hamster, speed, HamsterLureUtil::isTemptingItem, canBeScared);
        this.hamster = hamster;
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    // --- 3. Public Methods (Overrides from TemptGoal/Goal) ---
    @Override
    public boolean canStart() {
        // --- 1. Protected States ---
        if (!HamsterLureUtil.canFollowLure(this.hamster)) {
            return false;
        }

        // --- 2. Superclass Logic ---
        if (!super.canStart()) {
            return false;
        }

        // --- 3. Ownership-Aware Candidate Selection ---
        this.closestPlayer = HamsterLureUtil.resolveTemptingPlayer(this.hamster, this.closestPlayer);
        return this.closestPlayer != null;
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Protected States ---
        if (!HamsterLureUtil.canFollowLure(this.hamster)) {
            return false;
        }

        // --- 2. Superclass Logic ---
        return super.shouldContinue();
    }

    @Override
    public void tick() {
        // --- Combat Target Maintenance ---
        HamsterCombatUtil.clearInvalidTarget(this.hamster);
        super.tick();

        // --- Begging State Logic ---
        if (this.recheckTimer > 0) {
            this.recheckTimer--;
            return;
        }
        this.recheckTimer = 5; // Re-check begging state roughly every 5 ticks.

        PlayerEntity temptingPlayer = this.closestPlayer;

        if (temptingPlayer != null
                && temptingPlayer.isAlive()
                && this.hamster.squaredDistanceTo(temptingPlayer) < 64.0) {
            this.hamster.setBegging(HamsterLureUtil.isHoldingBeggingItem(temptingPlayer));
        } else {
            this.hamster.setBegging(false);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
        this.hamster.setBegging(false);
        this.recheckTimer = 0;
    }
}
