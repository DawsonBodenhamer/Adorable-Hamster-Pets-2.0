package net.dawson.adorablehamsterpets.mixin.accessor;


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
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;

import java.util.Map;
import net.minecraft.util.Identifier;

@Mixin(value = BookContentsBuilder.class, remap = false)
public interface BookContentsBuilderAccessor {
    @Accessor("book")
    Book getBook();

    @Accessor("categories")
    Map<Identifier, BookCategory> getCategories();

    @Accessor("entries")
    Map<Identifier, BookEntry> getEntries();
}