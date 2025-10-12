package net.dawson.adorablehamsterpets.mixin.accessor;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LookAtEntityGoal.class)
public interface LookAtEntityGoalAccessor {
    @Accessor("target")
    Entity getTarget();

    @Accessor("lookTime")
    int getLookTime();

    @Accessor("lookTime")
    void setLookTime(int lookTime);

    @Accessor("lookForward")
    boolean getLookForward();
}