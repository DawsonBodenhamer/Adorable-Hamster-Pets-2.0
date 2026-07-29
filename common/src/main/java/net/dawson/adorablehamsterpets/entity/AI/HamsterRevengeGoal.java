package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.minecraft.entity.ai.goal.RevengeGoal;

public class HamsterRevengeGoal extends RevengeGoal {
    private final HamsterEntity hamster;

    public HamsterRevengeGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        return this.hamster.getAttacker() != null
                && HamsterCombatUtil.canAcquireTarget(this.hamster, this.hamster.getAttacker())
                && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return HamsterCombatUtil.canContinueTarget(this.hamster, this.hamster.getTarget())
                && super.shouldContinue();
    }

    @Override
    public HamsterRevengeGoal setGroupRevenge(Class<?>... noHelpTypes) {
        super.setGroupRevenge(noHelpTypes);
        return this;
    }
}
