package net.dawson.adorablehamsterpets.client.gui.widgets;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.announcements.Semver;
import net.dawson.adorablehamsterpets.client.gui.AnnouncementScreen;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.mixin.client.accessor.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import java.util.Comparator;
import java.util.List;

/**
 * An interactive widget representing the announcement icon, designed to be
 * displayed on top of GUI screens. It handles its own rendering, animations,
 * tooltips, and click actions.
 */
public class AnnouncementIconWidget extends ButtonWidget {
    private static final Identifier ICON_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/item/announcement_bell_icon.png");
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    private final Screen parentScreen;
    private int lastTargetX = -1;
    private int lastTargetY = -1;

    public AnnouncementIconWidget(int x, int y, int width, int height, PressAction onPress, Screen parentScreen) {
        super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.parentScreen = parentScreen;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        AnnouncementIconAnimator animator = AnnouncementIconAnimator.INSTANCE;

        // --- 1. Dynamic Position Calculation ---
        if (this.parentScreen instanceof HandledScreen<?> containerScreen) {
            // Logic for inventory screens (uses widget offsets)
            HandledScreenAccessor accessor = (HandledScreenAccessor) containerScreen;
            int guiLeft = accessor.getX();
            int guiTop = accessor.getY();
            int guiWidth = accessor.getBackgroundWidth();

            int targetX;
            int targetY;

            // NeoForge-specific horizontal offset to avoid overlap with top tabs
            int neoForgeCreativeModeOffset = Platform.isNeoForge() ? -26 : 0;

            // Position slightly outside the corner, with slightly different
            // offsets for creative and survival mode to accommodate their unique shapes.
            if (containerScreen instanceof net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen) {
                // Creative Inventory (above the top right corner, to avoid conflicting with inventory mods like JEI, EMI, and REI)
                targetX = guiLeft + guiWidth - 22 + + neoForgeCreativeModeOffset + Configs.AHP.creativeWidgetIconSettings.get().offsetX.get();
                targetY = guiTop - 47 + Configs.AHP.creativeWidgetIconSettings.get().offsetY.get();
            } else {
                // Survival Inventory (overlapping top right corner)
                targetX = guiLeft + guiWidth - this.width + 4 + Configs.AHP.survivalWidgetIconSettings.get().offsetX.get();
                targetY = guiTop - 4 + Configs.AHP.survivalWidgetIconSettings.get().offsetY.get();
            }

            // If the target position has changed (e.g., recipe book opened), start a new transition.
            if (targetX != this.lastTargetX || targetY != this.lastTargetY) {
                AnnouncementIconAnimator.INSTANCE.startTransition(targetX, targetY);
                this.lastTargetX = targetX;
                this.lastTargetY = targetY;
            }
        } else if (this.parentScreen instanceof TitleScreen) {
            // Logic for Title Screen (uses the global HUD config settings)
            animator.updateTargetPosition(this.parentScreen.width, this.parentScreen.height);
        }

        // --- 2. Get Animation State from Central Animator ---
        animator.setHovered(this.isHovered());

        float animScale = animator.getRenderScale(delta);
        float configScale = Configs.AHP.hudIconScale.get(); // Use HUD scale for title screen too
        float finalScale = animScale * configScale;
        float angle = animator.getRenderAngle(delta);
        double renderX = animator.getRenderX(delta);
        double renderY = animator.getRenderY(delta);

        // Update widget's logical bounds and position for click detection
        this.width = (int) (ICON_WIDTH * configScale);
        this.height = (int) (ICON_HEIGHT * configScale);
        this.setX((int) Math.round(renderX));
        this.setY((int) Math.round(renderY));

        // --- 3. Render the Icon ---
        context.getMatrices().push();
        // Use the precise double values for rendering to avoid pixel-snapping.
        context.getMatrices().translate(renderX + (this.width / 2.0), renderY + (this.height / 2.0), 0);
        context.getMatrices().scale(finalScale, finalScale, 1.0f);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        context.getMatrices().translate(-(ICON_WIDTH / 2.0), -(ICON_HEIGHT / 2.0), 0);

        context.drawTexture(ICON_TEXTURE, 0, 0, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);

        context.getMatrices().pop();

        // --- 4. Render Tooltip ---
        if (this.isHovered()) {
            List<AnnouncementManager.PendingNotification> notifications = AnnouncementManager.INSTANCE.getPendingNotifications();
            if (!notifications.isEmpty()) {
                List<Text> tooltipLines = new java.util.ArrayList<>();
                Text modNameText = Text.translatable("key.categories.adorablehamsterpets.main").formatted(Formatting.BLUE, Formatting.ITALIC);

                Text mainTooltipLine = null;
                if (this.parentScreen instanceof TitleScreen) {
                    mainTooltipLine = notifications.stream()
                            .filter(n -> n.reason().equals(AnnouncementManager.PendingNotification.UPDATE_AVAILABLE_ANNOUNCEMENT))
                            .findFirst()
                            .map(AnnouncementManager::getTooltipTextForNotification)
                            .orElse(null);
                } else {
                    AnnouncementManager.PendingNotification primary = notifications.get(0);
                    mainTooltipLine = AnnouncementManager.getTooltipTextForNotification(primary);
                }

                if (mainTooltipLine != null) {
                    tooltipLines.add(mainTooltipLine);
                    tooltipLines.add(modNameText);
                    context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltipLines, mouseX, mouseY);
                }
            }
        }
    }

    /**
     * Called when the widget is clicked.
     */
    @Override
    public void onPress() {
        // --- 1. Trigger Visual & Audio Feedback ---
        AnnouncementIconAnimator.INSTANCE.triggerClickAnimation();
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        // --- 2. Execute Click Logic ---
        // Get notifications directly from the manager so the icon can appear on the title screen
        List<AnnouncementManager.PendingNotification> notifications = AnnouncementManager.INSTANCE.getPendingNotifications();
        if (notifications.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Identifier bookId = Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book");

        if (this.parentScreen instanceof TitleScreen) {
            // --- 1. Title Screen Logic ---
            // Find the single LATEST "update available" notification to display. This prevents ambiguity if
            // multiple are pending and avoids opening the Patchouli book GUI, which would crash from the title screen.
            notifications.stream()
                    .filter(n -> n.reason().equals(AnnouncementManager.PendingNotification.UPDATE_AVAILABLE_ANNOUNCEMENT))
                    .max(Comparator.comparing(n -> Semver.parse(n.announcement().semver()))) // Find the highest version
                    .ifPresent(notification -> {
                        Announcement announcement = notification.announcement();
                        Book book = BookRegistry.INSTANCE.books.get(bookId);
                        if (book != null) {
                            Identifier entryId = Identifier.of(AdorableHamsterPets.MOD_ID, "announcement_" + announcement.id());
                            JsonObject json = new JsonObject();
                            json.addProperty("name", announcement.title());
                            json.addProperty("icon", "minecraft:writable_book");
                            json.addProperty("category", "adorablehamsterpets:update_notes");
                            json.add("pages", new JsonArray());
                            BookEntry virtualEntry = new BookEntry(json, entryId, book, AdorableHamsterPets.MOD_ID);

                            // Open the screen with the TitleScreen as its parent
                            client.setScreen(new AnnouncementScreen(announcement, notification.reason(), this.parentScreen, virtualEntry));
                        }
                    });
        } else {
            if (notifications.size() == 1) {
                // --- 2. Direct Open Logic ---
                // Open directly to the custom GUI if only one message is available
                AnnouncementManager.PendingNotification notification = notifications.get(0);
                Announcement announcement = notifications.get(0).announcement();
                Book book = BookRegistry.INSTANCE.books.get(bookId);
                if (book != null) {
                    // Get the "real" virtual entry from the book's contents
                    Identifier entryId = Identifier.of(AdorableHamsterPets.MOD_ID, "announcement_" + announcement.id());
                    BookEntry realVirtualEntry = book.getContents().entries.get(entryId);

                    if (realVirtualEntry != null) {
                        // Open the screen with the real entry and a null parent
                        // Passing null tells the screen to return to the game HUD on close.
                        client.setScreen(new AnnouncementScreen(announcement, notification.reason(), null, realVirtualEntry));
                    } else {
                        AdorableHamsterPets.LOGGER.error("[AHP] Could not find virtual entry '{}' in book contents to open announcement screen.", entryId);
                    }
                }
            } else {
                // --- Multiple Pending Notifications Logic ---
                // If multiple, open the Patchouli book to the main landing page
                Book book = BookRegistry.INSTANCE.books.get(bookId);
                if (book != null) {
                    // By setting the book's current GUI instance to null, we force Patchouli's
                    // internal logic to create a new GuiBookLanding instance upon opening.
                    book.getContents().currentGui = null;
                    // Clear the GUI history to prevent the back button from navigating to the previous entry.
                    book.getContents().guiStack.clear();
                }

                // Use the client-side method that only takes the book's ID.
                // The user will land on the book's main page.
                PatchouliAPI.get().openBookGUI(bookId);
            }
        }
    }
}