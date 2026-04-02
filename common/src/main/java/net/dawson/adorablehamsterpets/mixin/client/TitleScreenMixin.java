package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconWidget;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Inject(method = "init", at = @At("TAIL"))
    private void adorablehamsterpets$onInit(CallbackInfo ci) {
        // Trigger genetics report once title screen loads
        HamsterPaletteManager.triggerInitialReport();

        // Refresh announcement manifest asynchronously
        AnnouncementManager.INSTANCE.refreshManifestOnce().thenAcceptAsync(v -> {

            // Get notifications directly from manager
            List<AnnouncementManager.PendingNotification> notifications = AnnouncementManager.INSTANCE.getPendingNotifications();

            // Check for pending update notification
            boolean shouldShowIcon = notifications.stream()
                    .anyMatch(n -> n.reason().equals(AnnouncementManager.PendingNotification.UPDATE_AVAILABLE_ANNOUNCEMENT));

            if (shouldShowIcon && Configs.AHP.enableHudIcon.get()) {
                // Add widget if title screen active
                if (MinecraftClient.getInstance().currentScreen == (TitleScreen) (Object) this) {
                    // Use accessor to add widget for cross-loader compatibility
                    // Initial bounds controlled by animator
                    ((ScreenWidgetAdder) (Object) this).adorablehamsterpets$addWidget(new AnnouncementIconWidget(
                            0, 0, 16, 16,
                            button -> ((AnnouncementIconWidget) button).onPress(),
                            (Screen) (Object) this
                    ));
                }
            }
        }, MinecraftClient.getInstance());
    }
}