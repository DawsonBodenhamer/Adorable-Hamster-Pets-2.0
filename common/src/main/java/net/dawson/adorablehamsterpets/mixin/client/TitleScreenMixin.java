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
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconWidget;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.mixin.accessor.ScreenWidgetAdder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void adorablehamsterpets$onInit(CallbackInfo ci) {
        AdorableHamsterPets.LOGGER.trace("[AHP TitleScreen] Mixin init called. Scheduling manifest refresh.");

        // Asynchronously refresh the announcement manifest when the title screen loads.
        // This returns a CompletableFuture to prevent race conditions with the network request.
        AnnouncementManager.INSTANCE.refreshManifestOnce().thenAcceptAsync(v -> {
            // This code needs to run after the manifest is ready.
            AdorableHamsterPets.LOGGER.trace("[AHP TitleScreen] Manifest refresh future completed.");
            // Get notifications directly from the manager, not the stale client cache
            List<AnnouncementManager.PendingNotification> notifications = AnnouncementManager.INSTANCE.getPendingNotifications();
            AdorableHamsterPets.LOGGER.trace("[AHP TitleScreen] Pending notifications count: {}", notifications.size());

            // Only add the widget to the title screen if there is a pending "update available" notification.  
            // This code runs AFTER the manifest has been fetched/loaded.
            boolean shouldShowIcon = notifications.stream()
                    .anyMatch(n -> n.reason().equals(AnnouncementManager.PendingNotification.UPDATE_AVAILABLE));

            AdorableHamsterPets.LOGGER.trace("[AHP TitleScreen] Should show icon: {}", shouldShowIcon);

            if (shouldShowIcon && Configs.AHP.enableHudIcon.get()) {
                // The initial x/y and size don't matter much as they are controlled by the animator.
                // Pass the current screen instance as the parent.
                // Add widget if on title screen
                if (MinecraftClient.getInstance().currentScreen == (TitleScreen) (Object) this) {
                    AdorableHamsterPets.LOGGER.trace("[AHP TitleScreen] Adding AnnouncementIconWidget to the screen.");
                    // Use ScreenWidgetAdder accessor to add the widget for cross-loader compatibility
                    ((ScreenWidgetAdder)(Object)this).adorablehamsterpets$addWidget(new AnnouncementIconWidget(
                            0, 0, 16, 16,
                            button -> ((AnnouncementIconWidget) button).onPress(),
                            (Screen) (Object) this // 'this' is the TitleScreen instance
                    ));
                } else {
                    AdorableHamsterPets.LOGGER.trace("[AHP TitleScreen] Screen changed before widget could be added. Current screen: {}", MinecraftClient.getInstance().currentScreen);
                }
            }
        }, MinecraftClient.getInstance()); // Ensure callback runs on the render thread
    }
}