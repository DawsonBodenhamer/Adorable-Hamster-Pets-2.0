package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FollowOwnerGoal.class)
public interface FollowOwnerGoalAccessor {

    @Accessor("owner")
    LivingEntity getOwner();

    @Accessor("minDistance")
    float getMinDistance();

    @Accessor("maxDistance")
    float getMaxDistance();

    @Accessor("speed")
    double getSpeed();

    @Accessor("updateCountdownTicks")
    int getUpdateCountdownTicks();

    @Accessor("updateCountdownTicks")
    void setUpdateCountdownTicks(int value);
}