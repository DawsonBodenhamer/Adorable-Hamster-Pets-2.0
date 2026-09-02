package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.dawson.adorablehamsterpets.util.HamsterLureUtil;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;

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
    public boolean canUse() {
        // --- 1. Protected States ---
        if (!HamsterLureUtil.canFollowLure(this.hamster)) {
            return false;
        }

        // --- 2. Superclass Logic ---
        if (!super.canUse()) {
            return false;
        }

        // --- 3. Ownership-Aware Candidate Selection ---
        this.player = HamsterLureUtil.resolveTemptingPlayer(this.hamster, this.player);
        return this.player != null;
    }

    @Override
    public boolean canContinueToUse() {
        // --- 1. Protected States ---
        if (!HamsterLureUtil.canFollowLure(this.hamster)) {
            return false;
        }

        // --- 2. Superclass Logic ---
        return super.canContinueToUse();
    }

    @Override
    public void tick() {
        if (this.hamster.hasRedstoneFever()) {
            this.hamster.setBegging(false);
            return;
        }

        // --- Combat Target Maintenance ---
        HamsterCombatUtil.clearInvalidTarget(this.hamster);
        super.tick();

        // --- Begging State Logic ---
        if (this.recheckTimer > 0) {
            this.recheckTimer--;
            return;
        }
        this.recheckTimer = 5; // Re-check begging state roughly every 5 ticks.

        Player temptingPlayer = this.player;

        if (temptingPlayer != null
                && temptingPlayer.isAlive()
                && this.hamster.distanceToSqr(temptingPlayer) < 64.0) {
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
