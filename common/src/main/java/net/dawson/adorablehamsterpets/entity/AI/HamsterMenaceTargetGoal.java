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
        return super.canStart();
    }
}