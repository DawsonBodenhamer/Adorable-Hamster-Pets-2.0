package net.dawson.adorablehamsterpets.mixin.client.accessor;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;

@Mixin(value = GuiButtonEntry.class, remap = false)
public interface GuiButtonEntryAccessor {
    @Invoker("getColor")
    int adorablehamsterpets$invokeGetColor();
}