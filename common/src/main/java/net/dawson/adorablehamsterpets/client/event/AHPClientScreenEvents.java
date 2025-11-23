package net.dawson.adorablehamsterpets.client.event;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.hooks.client.screen.ScreenAccess;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconWidget;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

/**
 * Handles client-side screen events for dynamically injecting the announcement icon widget.
 */
public final class AHPClientScreenEvents {
    private static final int ICON_SIZE = 16;

    public static void register() {
        // Register a listener for the post-initialization event.
        ClientGuiEvent.INIT_POST.register(AHPClientScreenEvents::onScreenInitPost);
    }

    private static void onScreenInitPost(Screen screen, ScreenAccess access) {
        // --- 1. Pre-condition Checks ---
        if (!Configs.AHP.enableWidgetIcon.get()) {
            return;
        }
        if (AdorableHamsterPetsClient.getPendingNotifications().isEmpty()) {
            return;
        }
        if (!(screen instanceof HandledScreen<?>)) {
            return;
        }

        // --- 2. Create and Add the Widget ---
        // The widget's initial position is temporary; its render method will calculate the true position.
        AnnouncementIconWidget icon = new AnnouncementIconWidget(
                0, 0, ICON_SIZE, ICON_SIZE,
                button -> ((AnnouncementIconWidget) button).onPress(),
                screen // Pass the parent screen to the widget
        );

        access.addRenderableWidget(icon);
    }
}