package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;

public class HamsterTrackOwnerAttackerGoal extends TrackOwnerAttackerGoal {
    private final HamsterEntity hamster;

    public HamsterTrackOwnerAttackerGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        LivingEntity owner = this.hamster.getOwner();
        LivingEntity attacker = owner == null ? null : owner.getAttacker();
        return attacker != null
                && HamsterCombatUtil.canAttackWithOwner(this.hamster, attacker, owner)
                && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return HamsterCombatUtil.canContinueTarget(this.hamster, this.hamster.getTarget())
                && super.shouldContinue();
    }
}
