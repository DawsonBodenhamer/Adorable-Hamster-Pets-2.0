package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookMarkRead;
import vazkii.patchouli.common.book.Book;

/**
 * Intercepts the 'onPress' action of Patchouli's 'Mark All as Read' button.
 * <p>
 * This mixin is necessary to prevent the button from clearing the 'unread' status
 * of Adorable Hamster Pets' virtual announcement and update note entries. It ensures that players
 * cannot dismiss these important notifications without viewing them in the custom
 * announcement GUI.
 * <p>
 */
@Mixin(value = GuiButtonBookMarkRead.class)
public abstract class GuiButtonBookMarkReadMixin {

    @Shadow(remap = false) private Book book;

    /** Access the private markEntry() in the target class. */
    @Invoker(value = "markEntry", remap = false)
    abstract void adorablehamsterpets$markEntry(BookEntry entry);

    /** Access the private markCategoryAsRead() in the target class. */
    @Invoker(value = "markCategoryAsRead", remap = false)
    abstract void adorablehamsterpets$markCategoryAsRead(BookEntry entry, BookCategory category, int maxRecursion);

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$onPress(CallbackInfo ci) {
        // Only intercept for the Hamster Tips book
        Identifier hamsterBookId = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book");
        if (!this.book.id.equals(hamsterBookId)) {
            return; // not our book; fall back to original logic
        }

        // Determine if this is the “mark all” (main page) action
        boolean onMainPage = !this.book.getContents().currentGui.canSeeBackButton();

        // Iterate over all entries in the book
        for (BookEntry entry : this.book.getContents().entries.values()) {
            Identifier entryId = entry.getId();

            // --- Announcement Logic: Handle Virtual Announcement Entries ---
            if (entryId.getNamespace().equals(AdorableHamsterPets.MOD_ID)
                    && entryId.getPath().startsWith("announcement_")) {

                String announcementId = entryId.getPath().substring("announcement_".length());
                Announcement announcement = AnnouncementManager.INSTANCE.getAnnouncementById(announcementId);

                if (announcement != null) {
                    // Mark as seen in my system
                    AnnouncementManager.INSTANCE.markAsSeen(announcement.id());
                    // Acknowledge in my system if it's an update-related message
                    if ("update".equals(announcement.kind())) {
                        AnnouncementManager.INSTANCE.setLastAcknowledgedUpdate(announcement.semver());
                    }
                }
            }

            // --- Universal Logic: Mark entry as read in Patchouli's system ---
            if (onMainPage) {
                adorablehamsterpets$markEntry(entry);
            } else {
                adorablehamsterpets$markCategoryAsRead(entry, entry.getCategory(), this.book.getContents().entries.size());
            }
        }

        // Cancel the default Patchouli logic
        ci.cancel();
    }
}