package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.gui.AnnouncementScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

@Mixin(value = BookContents.class, remap = false)
public class BookContentsMixin {

    @Inject(method = "openLexiconGui", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$onOpenLexiconGui(GuiBook gui, boolean push, CallbackInfo ci) {
        if (gui instanceof GuiBookEntry entryGui) {
            ResourceLocation entryId = entryGui.getEntry().getId();
            if (entryId.getNamespace().equals(AdorableHamsterPets.MOD_ID) && entryId.getPath().startsWith("announcement_")) {
                String announcementId = entryId.getPath().substring("announcement_".length());

                Announcement announcement = AnnouncementManager.INSTANCE.getAnnouncementById(announcementId);

                if (announcement != null) {
                    // Use the canonical method to get a stable, context-independent reason.
                    String reason = AnnouncementManager.INSTANCE.getCanonicalReasonForAnnouncement(announcementId);

                    Screen parentScreen = Minecraft.getInstance().screen;
                    Minecraft.getInstance().setScreen(new AnnouncementScreen(announcement, reason, parentScreen, entryGui.getEntry()));
                }

                ci.cancel(); // Prevent Patchouli from opening its screen
            }
        }
    }
}