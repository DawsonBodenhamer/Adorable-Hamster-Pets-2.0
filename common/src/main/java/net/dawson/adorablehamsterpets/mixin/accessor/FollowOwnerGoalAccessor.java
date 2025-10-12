package net.dawson.adorablehamsterpets.mixin.accessor;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

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