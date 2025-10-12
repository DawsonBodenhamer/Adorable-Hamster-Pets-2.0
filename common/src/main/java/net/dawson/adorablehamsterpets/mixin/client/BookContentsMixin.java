package net.dawson.adorablehamsterpets.mixin.client;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.gui.AnnouncementScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
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
            Identifier entryId = entryGui.getEntry().getId();
            if (entryId.getNamespace().equals(AdorableHamsterPets.MOD_ID) && entryId.getPath().startsWith("announcement_")) {
                String announcementId = entryId.getPath().substring("announcement_".length());

                Announcement announcement = AnnouncementManager.INSTANCE.getAnnouncementById(announcementId);

                if (announcement != null) {
                    // Use the canonical method to get a stable, context-independent reason.
                    String reason = AnnouncementManager.INSTANCE.getCanonicalReasonForAnnouncement(announcementId);

                    Screen parentScreen = MinecraftClient.getInstance().currentScreen;
                    MinecraftClient.getInstance().setScreen(new AnnouncementScreen(announcement, reason, parentScreen, entryGui.getEntry()));
                }

                ci.cancel(); // Prevent Patchouli from opening its screen
            }
        }
    }
}