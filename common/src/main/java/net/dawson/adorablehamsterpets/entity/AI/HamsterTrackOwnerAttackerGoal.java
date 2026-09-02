package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;

public class HamsterTrackOwnerAttackerGoal extends OwnerHurtByTargetGoal {
    private final HamsterEntity hamster;

    public HamsterTrackOwnerAttackerGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = this.hamster.getOwner();
        LivingEntity attacker = owner == null ? null : owner.getLastHurtByMob();
        return attacker != null
                && HamsterCombatUtil.canAttackWithOwner(this.hamster, attacker, owner)
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return HamsterCombatUtil.canContinueTarget(this.hamster, this.hamster.getTarget())
                && super.canContinueToUse();
    }
}
