package net.dawson.adorablehamsterpets.mixin.accessor;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LookAroundGoal.class)
public interface LookAroundGoalAccessor {
    @Accessor("mob")
    MobEntity getMob();

    @Accessor("deltaX")
    double getDeltaX();

    @Accessor("deltaZ")
    double getDeltaZ();

    @Accessor("lookTime")
    int getLookTime();

    @Accessor("lookTime")
    void setLookTime(int lookTime);
}