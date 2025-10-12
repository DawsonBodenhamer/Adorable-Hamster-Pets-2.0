package net.dawson.adorablehamsterpets.mixin.accessor;



/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


/**
 * Accessor interface to expose the protected 'cooldown' field from MeleeAttackGoal.
 * This allows us to set the cooldown from our custom goal class in a cross-platform way.
 */
@Mixin(MeleeAttackGoal.class)
public interface MeleeAttackGoalAccessor {
    /**
     * Provides write access to the cooldown field in MeleeAttackGoal.
     * @param cooldown The new value for the cooldown timer.
     */
    @Accessor("cooldown")
    void setCooldown(int cooldown);
}