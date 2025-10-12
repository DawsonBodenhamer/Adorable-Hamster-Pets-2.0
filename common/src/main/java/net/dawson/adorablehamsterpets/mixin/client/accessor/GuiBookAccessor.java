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
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.patchouli.client.book.gui.GuiBook;

@Mixin(value = GuiBook.class, remap = false)
public interface GuiBookAccessor {
    @Accessor("bookLeft")
    int adorablehamsterpets$getBookLeft();

    @Accessor("bookTop")
    int adorablehamsterpets$getBookTop();

    @Accessor("spread")
    int adorablehamsterpets$getSpread();

    @Accessor("maxSpreads")
    int adorablehamsterpets$getMaxSpreads();

    @Accessor("spread")
    void adorablehamsterpets$setSpread(int spread);

    @Accessor("maxSpreads")
    void adorablehamsterpets$setMaxSpreads(int maxSpreads);
}