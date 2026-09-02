package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;

public class HamsterAttackWithOwnerGoal extends OwnerHurtTargetGoal {
    private final HamsterEntity hamster;

    public HamsterAttackWithOwnerGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = this.hamster.getOwner();
        LivingEntity target = owner == null ? null : owner.getLastHurtMob();
        return target != null
                && HamsterCombatUtil.canAttackWithOwner(this.hamster, target, owner)
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return HamsterCombatUtil.canContinueTarget(this.hamster, this.hamster.getTarget())
                && super.canContinueToUse();
    }
}
