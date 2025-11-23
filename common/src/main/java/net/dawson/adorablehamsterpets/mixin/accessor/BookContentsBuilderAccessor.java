package net.dawson.adorablehamsterpets.mixin.accessor;

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