package net.dawson.adorablehamsterpets.client.announcements;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.util.Identifier;
import vazkii.patchouli.client.base.PersistentData;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import java.util.List;

public class PatchouliIntegration {

    /**
     * Marks a specific entry as "read" in Patchouli's data, preventing duplicates.
     *
     * @param entry The BookEntry to mark as read.
     */
    public static void setEntryAsRead(BookEntry entry) {
        if (entry == null) return;

        PersistentData.BookData data = PersistentData.data.getBookData(entry.getBook());
        Identifier entryId = entry.getId();

        // Check if the entry is already in the list before adding
        if (!data.viewedEntries.contains(entryId)) {
            data.viewedEntries.add(entryId);
            entry.markReadStateDirty();
            PersistentData.save();
            AdorableHamsterPets.LOGGER.debug("[Announcements] Marked Patchouli entry '{}' as read.", entryId);
        }
    }

    /**
     * Removes an entry from Patchouli's "viewedEntries" list, making it appear as "unread" again.
     *
     * @param entryId The full Identifier of the entry to mark as unread.
     */
    public static void setEntryAsUnread(Identifier entryId) {
        Identifier bookId = Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book");
        Book book = BookRegistry.INSTANCE.books.get(bookId);
        if (book == null) return;

        PersistentData.BookData data = PersistentData.data.getBookData(book);
        if (data.viewedEntries.remove(entryId.toString())) { // Use remove on the collection
            BookEntry entry = book.getContents().entries.get(entryId);
            if (entry != null) {
                entry.markReadStateDirty(); // Tell Patchouli its visual state needs an update
            }
            PersistentData.save(); // Save the changes to patchouli_data.json
            AdorableHamsterPets.LOGGER.debug("[Announcements] Marked Patchouli entry '{}' as unread.", entryId);
        }
    }

    /**
     * Clears all virtual announcement and update entries from Patchouli's history.
     */
    public static void clearAllVirtualEntriesFromHistory() {
        Identifier bookId = Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book");
        Book book = BookRegistry.INSTANCE.books.get(bookId);
        if (book == null) return;

        PersistentData.BookData data = PersistentData.data.getBookData(book);
        List<Identifier> viewed = data.viewedEntries;

        // Use removeIf to efficiently remove all entries that match our virtual entry prefix
        boolean removed = viewed.removeIf(id ->
                id.getNamespace().equals(AdorableHamsterPets.MOD_ID) &&
                        id.getPath().startsWith("announcement_")
        );

        if (removed) {
            // If any entries were removed, mark all book contents as dirty to force a UI refresh
            if (book.getContents() != null) {
                book.getContents().entries.values().forEach(BookEntry::markReadStateDirty);
            }
            PersistentData.save();
            AdorableHamsterPets.LOGGER.debug("[Announcements] Cleared all virtual announcement entries from Patchouli history.");
        }
    }
}