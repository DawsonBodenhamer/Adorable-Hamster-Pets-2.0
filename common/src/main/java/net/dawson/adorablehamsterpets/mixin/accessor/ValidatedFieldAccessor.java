package net.dawson.adorablehamsterpets.mixin.accessor;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ValidatedField.class, remap = false)
public interface ValidatedFieldAccessor<T> {

    /**
     * Invokes the protected 'set' method on a ValidatedField.
     * @param value The new value to set.
     */
    @Invoker("set")
    void adorablehamsterpets$set(T value);
}