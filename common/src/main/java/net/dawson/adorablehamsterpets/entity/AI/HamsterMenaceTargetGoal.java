package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;

public class HamsterMenaceTargetGoal extends ActiveTargetGoal<LivingEntity> {
    private final HamsterEntity hamster;

    public HamsterMenaceTargetGoal(HamsterEntity hamster) {
        // Scan all LivingEntities and filter using ConfigDataCache predicate
        super(hamster, LivingEntity.class, 10, true, false, ConfigDataCache::isMenaceTarget);  // Interval of 10 for performance
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        if (this.hamster.getAggressionState() != HamsterEntity.AggressionState.MENACE) return false;
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isSulking()) return false;

        boolean canStart = super.canStart();
        if (canStart && this.targetEntity != null && this.hamster.getOwner() != null) {
            // Prevent targeting mobs outside follow radius
            double maxRadius = 12.0;
            if (this.hamster.hasGreenBeanBuff()) maxRadius += 5.0;
            maxRadius *= 2.0; // Menace mode doubles the range

            if (this.targetEntity.squaredDistanceTo(this.hamster.getOwner()) > maxRadius * maxRadius) {
                return false;
            }
        }
        return canStart;
    }

    @Override
    public boolean shouldContinue() {
        boolean shouldContinue = super.shouldContinue();
        if (shouldContinue && this.targetEntity != null && this.hamster.getOwner() != null) {
            // Prevent chasing mobs that run outside the extended follow radius
            double maxRadius = 12.0;
            if (this.hamster.hasGreenBeanBuff()) maxRadius += 5.0;
            maxRadius *= 2.0; // Menace mode doubles the range

            if (this.targetEntity.squaredDistanceTo(this.hamster.getOwner()) > maxRadius * maxRadius) {
                return false;
            }
        }
        return shouldContinue;
    }
}