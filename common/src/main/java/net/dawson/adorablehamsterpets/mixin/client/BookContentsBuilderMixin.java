package net.dawson.adorablehamsterpets.mixin.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.mixin.accessor.BookContentsBuilderAccessor;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mixin to inject virtual, dynamically-loaded announcement categories and entries
 * into the Hamster Tips guide book.
 * <p>
 * Requires careful interaction with Patchouli's internal book-building lifecycle.
 * The injection happens at the HEAD of the `build` method to ensure my virtual
 * content is present before Patchouli finalizes its data structures into
 * immutable maps for the GUI.
 */
@Mixin(value = BookContentsBuilder.class, remap = false)
public class BookContentsBuilderMixin {

    /**
     * Injects virtual content at the beginning of the book-building process.
     * The sequence of operations is critical:
     * 1. Create virtual categories and add them to the builder's master map.
     * 2. Create virtual entries.
     * 3. Link each entry to its parent category using {@link BookEntry#initCategory}. This is the
     *    step that populates the category's internal list of entries.
     * 4. Add the linked entries to the builder's master map.
     * 5. Build the entries to process their (empty) pages.
     * 6. Build the categories to resolve their parentage and call {@link BookCategory#updateLockStatus}
     *    to ensure they are visible in the GUI.
     *
     * @param level The world instance, required by the build methods.
     * @param cir   The mixin callback info.
     */
    @Inject(method = "build", at = @At("HEAD"))
    private void adorablehamsterpets$onBuild(World level, CallbackInfoReturnable<BookContents> cir) {
        // --- 1. Initial Setup & Safety Check ---
        BookContentsBuilderAccessor accessor = (BookContentsBuilderAccessor) this;
        Book hamsterBook = accessor.getBook();

        // Only modify the Hamster Tips guide book
        if (hamsterBook == null || !hamsterBook.id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"))) {
            return;
        }

        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] Found target book. Starting virtual content injection.");

        // --- 2. Get mutable maps from the builder ---
        // Inject at HEAD to ensure these maps are still mutable HashMaps.
        Map<Identifier, BookCategory> categories = accessor.getCategories();
        Map<Identifier, BookEntry> entries = accessor.getEntries();
        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] Before injection: {} categories, {} entries.", categories.size(), entries.size());

        // --- 3. Create and Add Virtual Categories to the Master Map ---
        // Categories must exist in the master map *before* entries try to link to them.
        Identifier updatesId = Identifier.of(AdorableHamsterPets.MOD_ID, "update_notes");
        if (!categories.containsKey(updatesId)) {
            BookCategory updatesCategory = createVirtualCategory(hamsterBook, "book.adorablehamsterpets.category.update_notes", "minecraft:writable_book", 99, updatesId);
            categories.put(updatesId, updatesCategory);
        }

        Identifier announcementsId = Identifier.of(AdorableHamsterPets.MOD_ID, "announcements");
        if (!categories.containsKey(announcementsId)) {
            BookCategory announcementsCategory = createVirtualCategory(hamsterBook, "book.adorablehamsterpets.category.announcements", "adorablehamsterpets:announcement_bell_icon", 98, announcementsId);
            categories.put(announcementsId, announcementsCategory);
        }

        // --- 4. Create, Link, and Build Virtual Entries ---
        BookContentsBuilder self = (BookContentsBuilder)(Object)this;
        List<Announcement> allMessages = AnnouncementManager.INSTANCE.getAllManifestMessages();

        allMessages.stream()
                .sorted(Comparator.comparing(Announcement::published, Comparator.reverseOrder()))
                .forEach(announcement -> {
                    Identifier entryId = Identifier.of(AdorableHamsterPets.MOD_ID, "announcement_" + announcement.id());

                    // Do not re-process an entry if it's already in the map.
                    if (!entries.containsKey(entryId)) {
                        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] -> Creating virtual entry '{}'", entryId);
                        boolean isUpdate = "update".equals(announcement.kind());
                        Identifier categoryId = isUpdate ? updatesId : announcementsId;
                        String icon = isUpdate ? "minecraft:writable_book" : "adorablehamsterpets:announcement_bell_icon";

                        BookEntry entry = createVirtualEntry(hamsterBook, announcement.title(), icon, isUpdate, categoryId, entryId);

                        // Critical Step: Link the entry to its category.
                        // This calls `category.addEntry()` internally, populating the category's own list.
                        // I provide `categories::get` as a lookup function for the entry to find its parent.
                        entry.initCategory(entryId, categories::get);

                        // Add the now-linked entry to the book's master map.
                        entries.put(entryId, entry);

                        // Build the entry to process its pages and other properties.
                        try {
                            entry.build(level, self);
                        } catch (Exception e) {
                            AdorableHamsterPets.LOGGER.error("[AHP Mixin] Failed to build virtual entry {}", entryId, e);
                        }
                    }
                });

        // --- 5. Build and Finalize Virtual Categories ---
        // This must be done AFTER all entries have been created and linked.
        if (categories.containsKey(updatesId)) {
            BookCategory cat = categories.get(updatesId);
            cat.build(self); // Resolves parentage (important for root categories).
            cat.updateLockStatus(true); // Initializes lock state to ensure it's not hidden.
            AdorableHamsterPets.LOGGER.debug("[AHP Mixin] Built 'update_notes' category with {} entries.", cat.getEntries().size());
        }
        if (categories.containsKey(announcementsId)) {
            BookCategory cat = categories.get(announcementsId);
            cat.build(self);
            cat.updateLockStatus(true);
            AdorableHamsterPets.LOGGER.debug("[AHP Mixin] Built 'announcements' category with {} entries.", cat.getEntries().size());
        }

        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] After injection: {} categories, {} entries.", categories.size(), entries.size());
    }

    /**
     * Helper method to construct a virtual {@link BookCategory} from a JSON definition.
     *
     * @param book The parent book object.
     * @param nameKey The translation key for the category's name.
     * @param icon The resource location string for the category's icon.
     * @param sortnum The sorting number for ordering.
     * @param id The unique identifier for the category.
     * @return A new {@code BookCategory} instance.
     */
    private BookCategory createVirtualCategory(Book book, String nameKey, String icon, int sortnum, Identifier id) {
        JsonObject json = new JsonObject();
        json.addProperty("name", nameKey);
        json.addProperty("description", nameKey + ".desc");
        json.addProperty("icon", icon);
        json.addProperty("sortnum", sortnum);
        // Critical: An empty "parent" string designates this as a root category, making it appear on the landing page.
        json.addProperty("parent", "");
        return new BookCategory(json, id, book);
    }

    /**
     * Helper method to construct a virtual {@link BookEntry} from a JSON definition.
     *
     * @param book The parent book object.
     * @param name The display name of the entry.
     * @param icon The resource location string for the entry's icon.
     * @param priority Whether the entry should have a priority flag (star icon).
     * @param categoryId The ID of the parent category.
     * @param entryId The unique identifier for the entry.
     * @return A new {@code BookEntry} instance.
     */
    private BookEntry createVirtualEntry(Book book, String name, String icon, boolean priority, Identifier categoryId, Identifier entryId) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("icon", icon);
        json.addProperty("priority", priority);
        json.addProperty("category", categoryId.toString());
        // The pages array is empty because I'm intercepting the click event to show my own GUI.
        json.add("pages", new JsonArray());
        return new BookEntry(json, entryId, book, AdorableHamsterPets.MOD_ID);
    }
}