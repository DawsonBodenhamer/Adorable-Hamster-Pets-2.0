package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class HamsterMenaceTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final HamsterEntity hamster;

    public HamsterMenaceTargetGoal(HamsterEntity hamster) {
        // Scan all LivingEntities and filter using ConfigDataCache predicate
        super(hamster, LivingEntity.class, 10, true, false, (target, level) -> ConfigDataCache.isMenaceTarget(target));  // Interval of 10 for performance
        this.hamster = hamster;
    }

    @Override
    public boolean canUse() {
        if (this.hamster.getAggressionState() != HamsterEntity.AggressionState.MENACE) return false;
        if (this.hamster.isOrderedToSit() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isSulking()) return false;

        boolean canStart = super.canUse();
        if (canStart && this.target != null && this.hamster.getOwner() != null) {
            // Prevent targeting mobs outside follow radius
            double maxRadius = 12.0;
            if (this.hamster.hasGreenBeanBuff()) maxRadius += 5.0;
            maxRadius *= 2.0; // Menace mode doubles the range

            if (this.target.distanceToSqr(this.hamster.getOwner()) > maxRadius * maxRadius) {
                return false;
            }
        }
        return canStart;
    }

    @Override
    public boolean canContinueToUse() {
        boolean shouldContinue = super.canContinueToUse();
        if (shouldContinue && this.target != null && this.hamster.getOwner() != null) {
            // Prevent chasing mobs that run outside the extended follow radius
            double maxRadius = 12.0;
            if (this.hamster.hasGreenBeanBuff()) maxRadius += 5.0;
            maxRadius *= 2.0; // Menace mode doubles the range

            if (this.target.distanceToSqr(this.hamster.getOwner()) > maxRadius * maxRadius) {
                return false;
            }
        }
        return shouldContinue;
    }
}