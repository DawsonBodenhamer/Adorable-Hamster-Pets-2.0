package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FollowOwnerGoal.class)
public interface FollowOwnerGoalAccessor {

    @Accessor("owner")
    LivingEntity getOwner();

    @Accessor("startDistance")
    float getMinDistance();

    @Accessor("stopDistance")
    float getMaxDistance();

    @Accessor("speedModifier")
    double getSpeed();

    @Accessor("timeToRecalcPath")
    int getUpdateCountdownTicks();

    @Accessor("timeToRecalcPath")
    void setUpdateCountdownTicks(int value);
}