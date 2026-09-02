package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

public class HamsterRevengeGoal extends HurtByTargetGoal {
    private final HamsterEntity hamster;

    public HamsterRevengeGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canUse() {
        return this.hamster.getLastHurtByMob() != null
                && HamsterCombatUtil.canAcquireTarget(this.hamster, this.hamster.getLastHurtByMob())
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return HamsterCombatUtil.canContinueTarget(this.hamster, this.hamster.getTarget())
                && super.canContinueToUse();
    }

    @Override
    public HamsterRevengeGoal setAlertOthers(Class<?>... noHelpTypes) {
        super.setAlertOthers(noHelpTypes);
        return this;
    }
}
