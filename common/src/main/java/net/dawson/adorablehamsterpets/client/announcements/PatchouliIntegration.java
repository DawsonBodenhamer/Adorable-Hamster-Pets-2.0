package net.dawson.adorablehamsterpets.client.announcements;

import net.minecraft.resources.Identifier;

/**
 * 26.2 port: Patchouli has no 26.2 build, so the in-game guide book is gone.
 * <p>
 * This is a no-op stand-in. The announcement system still tracks read state on
 * its own; these methods used to mirror that state into the guide book, which
 * no longer exists. Keeping the class means the call sites stay untouched, so
 * restoring the real integration later is a one-file change.
 */
public final class PatchouliIntegration {
    private PatchouliIntegration() {}

    /** No-op: there is no book to mark. Returns false so callers treat it as "not mirrored". */
    public static boolean setEntryAsRead(Object entry) {
        return false;
    }

    /** No-op counterpart to {@link #setEntryAsRead}. */
    public static boolean setEntryAsUnread(Identifier entryId) {
        return false;
    }

    /** No-op: nothing keeps a virtual entry history without the book. */
    public static void clearAllVirtualEntriesFromHistory() {
    }
}
