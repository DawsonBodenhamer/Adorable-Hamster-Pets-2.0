package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookIndex;
import vazkii.patchouli.common.book.Book;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(value = GuiBookIndex.class, remap = false)
public abstract class GuiBookIndexMixin {

    /**
     * Intercepts getEntries() to hide the virtual entries from the main entry index.
     * This prevents the "all entries" list from being cluttered with announcements and update notes.
     */
    @Inject(method = "getEntries", at = @At("RETURN"), cancellable = true)
    private void adorablehamsterpets$filterVirtualEntries(CallbackInfoReturnable<Collection<BookEntry>> cir) {
        // --- 1. Get Context and Perform Safety Check ---
        // This cast gives us access to the 'book' field on the parent GuiBook.
        GuiBook self = (GuiBook) (Object) this;
        Book thisBook = self.book;

        // Only filter entries for our Hamster Tips guide book.
        Identifier hamsterBookId = Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book");
        if (thisBook == null || !hamsterBookId.equals(thisBook.id)) {
            return;
        }

        // --- 2. Filter and Replace the Return Value ---
        // Get the original list of all entries that Patchouli was about to return.
        Collection<BookEntry> originalEntries = cir.getReturnValue();

        // Use a stream to create a new list, excluding any entry whose category is one of our virtual ones.
        Collection<BookEntry> filteredEntries = originalEntries.stream()
                .filter(entry -> {
                    Identifier categoryId = entry.getCategory().getId();
                    boolean isVirtualCategory = categoryId.getNamespace().equals(AdorableHamsterPets.MOD_ID) &&
                            (categoryId.getPath().equals("update_notes") || categoryId.getPath().equals("announcements"));
                    // Keep the entry only if it's NOT in a virtual category.
                    return !isVirtualCategory;
                })
                .collect(Collectors.toList());

        // Set the return value of the getEntries() method to our new, filtered list.
        cir.setReturnValue(filteredEntries);
    }
}