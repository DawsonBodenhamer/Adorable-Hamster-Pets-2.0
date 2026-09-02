package net.dawson.adorablehamsterpets.mixin.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.announcements.Semver;
import net.dawson.adorablehamsterpets.mixin.accessor.BookContentsBuilderAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
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
     * <p>
     * <b>The sequence of operations is critical:</b>
     * <ul>
     *     <li> 1. Create virtual categories and add them to the builder's master map.</li>
     *     <li> 2. Create virtual entries.</li>
     *     <li> 3. Link each entry to its parent category using {@link BookEntry#initCategory}. This is the
     *    step that populates the category's internal list of entries.</li>
     *     <li> 4. Add the linked entries to the builder's master map.</li>
     *     <li> 5. Build the entries to process their (empty) pages.</li>
     *     <li> 6. Build the categories to resolve their parentage and call {@link BookCategory#updateLockStatus}
     *    to ensure they are visible in the GUI.</li>
     * <p>
     * <b>Filtering & Sorting Logic:</b>
     * <ul>
     *     <li><b>Filter:</b> 'Update Available' messages are hidden if the user is already on that version.
     *         'Patch Notes' are hidden if the user hasn't updated to that version yet.</li>
     *     <li><b>Sort 1 (Updates):</b> 'Update Available' messages are pinned to the very top.</li>
     *     <li><b>Sort 2 (Current Notes):</b> 'Patch Notes' for the <i>current installed version</i> are pinned second.</li>
     *     <li><b>Sort 3 (Date):</b> All other entries are sorted by date (newest first).</li>
     *
     * @param level The world instance, required by the build methods.
     * @param cir   The mixin callback info.
     */
    @Inject(method = "build", at = @At("HEAD"))
    private void adorablehamsterpets$onBuild(Level level, CallbackInfoReturnable<BookContents> cir) {
        // --- 1. Initial Setup & Safety Check ---
        BookContentsBuilderAccessor accessor = (BookContentsBuilderAccessor) this;
        Book hamsterBook = accessor.getBook();

        // Only modify the Hamster Tips guide book
        if (hamsterBook == null || !hamsterBook.id.equals(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"))) {
            return;
        }

        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] Found target book. Starting virtual content injection.");

        // --- 2. Get mutable maps from the builder ---
        Map<ResourceLocation, BookCategory> categories = accessor.getCategories();
        Map<ResourceLocation, BookEntry> entries = accessor.getEntries();
        HolderLookup.Provider registries = level.registryAccess();

        // --- 3. Create and Add Virtual Categories ---
        ResourceLocation updatesId = ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "update_notes");
        if (!categories.containsKey(updatesId)) {
            BookCategory updatesCategory = createVirtualCategory(hamsterBook, "book.adorablehamsterpets.category.update_notes", "minecraft:writable_book", 99, updatesId, registries);
            categories.put(updatesId, updatesCategory);
        }

        ResourceLocation announcementsId = ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "announcements");
        if (!categories.containsKey(announcementsId)) {
            BookCategory announcementsCategory = createVirtualCategory(hamsterBook, "book.adorablehamsterpets.category.announcements", "adorablehamsterpets:announcement_bell_icon", 98, announcementsId, registries);
            categories.put(announcementsId, announcementsCategory);
        }

        // --- 4. Create, Filter, Sort, Link, and Build Virtual Entries ---
        BookContentsBuilder self = (BookContentsBuilder)(Object)this;
        List<Announcement> allMessages = AnnouncementManager.INSTANCE.getAllManifestMessages();
        Semver installedVersion = Semver.parse(Platform.getMod(AdorableHamsterPets.MOD_ID).getVersion().toString());

        List<Announcement> sortedMessages = allMessages.stream()
                // --- 4a. Normalize Data (Backwards Compatibility Shim) ---
                // If JSON says "update", resolve it to specific types based on version comparison
                .map(a -> {
                    if ("update".equals(a.kind())) {
                        Semver msgVer = Semver.parse(a.semver());
                        String newKind;

                        // If the ID identifies it as an update notification (e.g. "update-3.5.0"), force it to be 'update_available'
                        if (a.id().startsWith("update-")) {
                            newKind = "update_available";
                        } else {
                            // Fallback for non-standard IDs: use version comparison
                            newKind = msgVer.compareTo(installedVersion) > 0 ? "update_available" : "patch_notes";
                        }

                        return new Announcement(a.id(), newKind, a.semver(), a.title(), a.markdown(), a.published());
                    }
                    return a;
                })
                .filter(a -> {
                    // --- Filtering Rule ---
                    Semver msgVer = Semver.parse(a.semver());
                    if ("update_available".equals(a.kind())) {
                        // Only show "Update Available" if it is actually newer than what user has installed
                        return msgVer.compareTo(installedVersion) > 0;
                    }
                    if ("patch_notes".equals(a.kind())) {
                        // Only show "Patch Notes" if user is on equal or older version
                        return msgVer.compareTo(installedVersion) <= 0;
                    }
                    return true; // Always show regular announcements
                })
                .sorted((a1, a2) -> {
                    // --- Sorting Rule 1: Pin "Update Available" to Top ---
                    boolean u1 = "update_available".equals(a1.kind());
                    boolean u2 = "update_available".equals(a2.kind());
                    if (u1 != u2) return u1 ? -1 : 1;

                    // --- Sorting Rule 2: Pin "Current Patch Notes" Second ---
                    Semver v1 = Semver.parse(a1.semver());
                    Semver v2 = Semver.parse(a2.semver());
                    boolean c1 = "patch_notes".equals(a1.kind()) && v1.equals(installedVersion);
                    boolean c2 = "patch_notes".equals(a2.kind()) && v2.equals(installedVersion);
                    if (c1 != c2) return c1 ? -1 : 1;

                    // --- Sorting Rule 3: Date (Newest First) ---
                    return a2.published().compareTo(a1.published());
                })
                .toList();

        // Iterate using index to assign explicit sortnum
        for (int i = 0; i < sortedMessages.size(); i++) {
            Announcement announcement = sortedMessages.get(i);
            ResourceLocation entryId = ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "announcement_" + announcement.id());

            if (!entries.containsKey(entryId)) {
                String kind = announcement.kind();
                boolean isUpdateRelated = "update_available".equals(kind) || "patch_notes".equals(kind);
                ResourceLocation categoryId = isUpdateRelated ? updatesId : announcementsId;

                String icon;
                boolean priority = false;

                if ("update_available".equals(kind)) {
                    icon = "minecraft:nether_star"; // Distinct icon for the call-to-action
                    priority = true; // Star on the entry icon
                } else if ("patch_notes".equals(kind)) {
                    icon = "minecraft:writable_book";
                    // Highlight the notes for the version the user is currently playing on
                    if (Semver.parse(announcement.semver()).equals(installedVersion)) {
                        priority = true;
                    }
                } else {
                    icon = "adorablehamsterpets:announcement_bell_icon";
                }

                // Pass loop index 'i' as the sortnum to enforce the sorted order in Patchouli
                BookEntry entry = createVirtualEntry(hamsterBook, announcement.title(), icon, priority, categoryId, entryId, i, registries);

                entry.initCategory(entryId, categories::get);
                entries.put(entryId, entry);

                try {
                    entry.build(level, self);
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.error("[AHP Mixin] Failed to build virtual entry {}", entryId, e);
                }
            }
        }

        // --- 5. Build and Finalize Virtual Categories ---
        // Must be done after all entries have been created and linked.
        if (categories.containsKey(updatesId)) {
            BookCategory cat = categories.get(updatesId);
            cat.build(self); // Resolves parentage (important for root categories)
            cat.updateLockStatus(true); // Initializes lock state to ensure it's not hidden
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
     * @param registries The registry wrapper passed from the main build loop.
     * @return A new {@code BookCategory} instance.
     */
    private BookCategory createVirtualCategory(Book book, String nameKey, String icon, int sortnum, ResourceLocation id, HolderLookup.Provider registries) {
        JsonObject json = new JsonObject();
        json.addProperty("name", nameKey);
        json.addProperty("description", nameKey + ".desc");
        json.addProperty("icon", icon);
        json.addProperty("sortnum", sortnum);
        // Critical: An empty "parent" string designates this as a root category, making it appear on the landing page.
        json.addProperty("parent", "");
        return new BookCategory(json, id, book, registries);
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
     * @param sortnum The explicit sorting index for the entry within the category.
     * @param registries The registry wrapper passed from the main build loop.
     * @return A new {@code BookEntry} instance.
     */
    private BookEntry createVirtualEntry(Book book, String name, String icon, boolean priority, ResourceLocation categoryId, ResourceLocation entryId, int sortnum, HolderLookup.Provider registries) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("icon", icon);
        json.addProperty("priority", priority);
        json.addProperty("sortnum", sortnum);
        json.addProperty("category", categoryId.toString());
        // Pages array is empty because I'm intercepting the click event to show my own GUI
        json.add("pages", new JsonArray());
        return new BookEntry(json, entryId, book, AdorableHamsterPets.MOD_ID, registries);
    }
}