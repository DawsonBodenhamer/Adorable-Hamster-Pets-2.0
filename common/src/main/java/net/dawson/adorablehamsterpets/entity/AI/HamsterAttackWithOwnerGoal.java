package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;

public class HamsterAttackWithOwnerGoal extends AttackWithOwnerGoal {
    private final HamsterEntity hamster;

    public HamsterAttackWithOwnerGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        LivingEntity owner = this.hamster.getOwner();
        LivingEntity target = owner == null ? null : owner.getAttacking();
        return target != null
                && HamsterCombatUtil.canAttackWithOwner(this.hamster, target, owner)
                && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return HamsterCombatUtil.canContinueTarget(this.hamster, this.hamster.getTarget())
                && super.shouldContinue();
    }
}
