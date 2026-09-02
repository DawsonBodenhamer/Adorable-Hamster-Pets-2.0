package net.dawson.adorablehamsterpets.client.gui;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.announcements.ClientAnnouncementState;
import net.dawson.adorablehamsterpets.client.announcements.PatchouliIntegration;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.mixin.accessor.ScreenWidgetAdder;
import net.dawson.adorablehamsterpets.mixin.accessor.ValidatedFieldAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a scrollable announcement screen for displaying "What's New" updates, patch notes, or other
 * important information fetched from a remote source.
 *
 * <h2>UI Scaling and Layout</h2>
 * This screen employs a custom scaling strategy to ensure the entire UI, including a row of action
 * buttons below the main texture, fits gracefully within any window size.
 * <ul>
 *   <li><b>uiScale:</b> A dynamic downscaling factor (clamped ≤ 1.0) is calculated based on the
 *       available screen space minus a small {@link #EDGE_MARGIN} on all sides. This ensures the
 *       UI never feels cramped against the window edges.</li>
 *   <li><b>Centering:</b> The entire scaled UI (background + padding + buttons) is centered
 *       horizontally and vertically within the available space defined by the margins.</li>
 *   <li><b>Background Rendering:</b> The 256x256 background texture is drawn using a matrix
 *       transformation (translate and scale). This prevents the right and bottom edges of the
 *       texture from being cropped, a common issue when using DrawContext#drawTexture.
 *       with scaled region sizes.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>A scrollable content area that renders Markdown text.</li>
 *   <li>A custom scrollbar that appears when content overflows.</li>
 *   <li>A dynamically scaled title that adjusts its size to fit the header.</li>
 *   <li>A row of action buttons (e.g., "Mark as Read", "Changelog") that are dynamically generated,
 *       sized, and centered below the main panel.</li>
 * </ul>
 */
public class AnnouncementScreen extends Screen {

    // --- UI Constants ---
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/gui/announcement_ui.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/gui/announcement_ui_scroll_bar.png");
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 256;
    private static final int TOTAL_GUI_HEIGHT = 283; // BG height + padding (7) + button height (20)
    private static final int CONTENT_X_OFFSET = 17;
    private static final int CONTENT_Y_OFFSET = 35;
    private static final int CONTENT_WIDTH = 221;
    private static final int CONTENT_HEIGHT = 204;
    private static final int SCROLLBAR_X_OFFSET = 242;
    private static final int SCROLLBAR_START_Y = 28;
    private static final int SCROLLBAR_END_Y = 221;
    private static final int EDGE_MARGIN = 7;
    private static final int BUTTON_WIDTH = 100;

    // --- Fields ---
    private final Announcement announcement;
    private MarkdownRenderer markdownRenderer;
    private String markdownContent = "Loading...";
    private double scrollY = 0.0;
    private int guiLeft;
    private int guiTop;
    @Nullable private Style hoveredStyle = null;
    private final Screen parentScreen;
    private final BookEntry virtualEntry;
    private final String reason;
    private float uiScale = 1.0f;
    private boolean isDraggingScrollbar = false;

    // --- Scaled Layout Fields ---
    private int scaledBackgroundWidth;
    private int scaledBackgroundHeight;
    private int scaledTotalHeight;
    private int scaledContentXOffset;
    private int scaledContentYOffset;
    private int scaledContentWidth;
    private int scaledContentHeight;
    private int scaledScrollBarXOffset;
    private int scaledScrollBarStartY;
    private int scaledScrollBarEndY;
    private int scaledButtonHeight;
    private int scaledButtonPadding;

    public AnnouncementScreen(Announcement announcement, String reason, @Nullable Screen parentScreen, BookEntry virtualEntry) {
        super(Component.literal(announcement.title()));
        this.announcement = announcement;
        this.reason = reason;
        this.parentScreen = parentScreen;
        this.virtualEntry = virtualEntry;
    }

    /**
     * Initializes the screen, calculating all layout dimensions based on the available window size.
     * This method computes a {@code uiScale} to ensure the entire interface (background, padding, and
     * buttons) fits within the screen, leaving a small {@link #EDGE_MARGIN}. It then pre-calculates
     * all scaled dimensions and positions, and initializes all widgets.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void init() {
        super.init();

        // --- UI Layout Calculation ---
        // Reserve a margin on all sides for breathing room, similar to vanilla screens.
        int availableWidth = Math.max(0, this.width - 2 * EDGE_MARGIN);
        int availableHeight = Math.max(0, this.height - 2 * EDGE_MARGIN);

        // Determine a downscaling factor so the entire UI fits, clamped to 1.0 to prevent upscaling.
        int buttonRows = 2;
        int unscaledTotalHeight = TOTAL_GUI_HEIGHT + (buttonRows - 1) * (20 + 7);
        float scaleX = (float) availableWidth / BACKGROUND_WIDTH;
        float scaleY = (float) availableHeight / unscaledTotalHeight;
        this.uiScale = Math.min(1.0f, Math.min(scaleX, scaleY));

        // Pre-calculate scaled dimensions used throughout the screen.
        this.scaledBackgroundWidth = Math.round(BACKGROUND_WIDTH * this.uiScale);
        this.scaledBackgroundHeight = Math.round(BACKGROUND_HEIGHT * this.uiScale);
        this.scaledContentXOffset = Math.round(CONTENT_X_OFFSET * this.uiScale);
        this.scaledContentYOffset = Math.round(CONTENT_Y_OFFSET * this.uiScale);
        this.scaledContentWidth = Math.max(1, Math.round(CONTENT_WIDTH * this.uiScale));
        this.scaledContentHeight = Math.max(1, Math.round(CONTENT_HEIGHT * this.uiScale));
        this.scaledScrollBarXOffset = Math.round(SCROLLBAR_X_OFFSET * this.uiScale);
        this.scaledScrollBarStartY = Math.round(SCROLLBAR_START_Y * this.uiScale);
        this.scaledScrollBarEndY = Math.round(SCROLLBAR_END_Y * this.uiScale);

        // Scale button dimensions with sensible minimums to maintain usability.
        this.scaledButtonHeight = Math.max(12, Math.round(20 * this.uiScale));
        this.scaledButtonPadding = Math.max(2, Math.round(7 * this.uiScale));

        // The total scaled height is derived from individually scaled components to respect minimums.
        this.scaledTotalHeight = this.scaledBackgroundHeight + buttonRows * (this.scaledButtonHeight + this.scaledButtonPadding);

        // Center the entire UI within the available margin area.
        this.guiLeft = EDGE_MARGIN + (availableWidth - this.scaledBackgroundWidth) / 2;
        this.guiTop = EDGE_MARGIN + (availableHeight - this.scaledTotalHeight) / 2;

        // --- Markdown Renderer Initialization ---
        this.markdownRenderer = new MarkdownRenderer(
                markdownContent,
                this.guiLeft + this.scaledContentXOffset,
                this.guiTop + this.scaledContentYOffset,
                this.scaledContentWidth
        );

        AnnouncementManager.INSTANCE.fetchMarkdown(announcement.markdown()).thenAccept(content -> {
            this.minecraft.execute(() -> {
                this.markdownContent = content;
                this.markdownRenderer = new MarkdownRenderer(
                        content,
                        this.guiLeft + this.scaledContentXOffset,
                        this.guiTop + this.scaledContentYOffset,
                        this.scaledContentWidth
                );
            });
        });

        // --- "←" Close/Back Button ---
        int closeButtonX = this.guiLeft - 10; // Slightly overlapping the left side of the GUI
        int closeButtonY = this.guiTop + 5; // Slightly below the top of the GUI
        // Use ScreenWidgetAdder accessor to add the widget for cross-loader compatibility
        ((ScreenWidgetAdder)(Object)this).adorablehamsterpets$addWidget(Button.builder(Component.literal("←").withStyle(ChatFormatting.BOLD), button -> this.returnToBook())
                .bounds(closeButtonX, closeButtonY, 20, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.adorablehamsterpets.announcement.button.close.tooltip")))
                .build());

        // --- Dynamic Button Creation ---
        // Prepare two rows of buttons: primary actions on the first row and secondary actions on the second.
        int buttonWidth = BUTTON_WIDTH;
        int buttonPadding = this.scaledButtonPadding;

        List<Button.Builder> primaryBuilders = new ArrayList<>();
        List<Button.Builder> secondaryBuilders = new ArrayList<>();

        // 1. "Mark as Read" button (primary row)
        MutableComponent markAsReadTooltip = Component.translatable("gui.adorablehamsterpets.announcement.button.mark_as_read.tooltip")
                .append("\n\n")
                .append(Component.translatable("gui.adorablehamsterpets.announcement.button.mark_as_read.shift_tooltip").withStyle(ChatFormatting.GOLD));

        primaryBuilders.add(Button.builder(Component.translatable("gui.adorablehamsterpets.announcement.button.mark_as_read"), button -> {
            if (Screen.hasShiftDown()) {
                // --- Shift-Click Action: Mark ALL as read ---
                Book book = BookRegistry.INSTANCE.books.get(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
                if (book != null) {
                    AnnouncementManager.INSTANCE.getAllManifestMessages().forEach(msg -> {
                        // Mark the announcement as seen in my system
                        AnnouncementManager.INSTANCE.markAsSeen(msg.id());

                        // Acknowledge any update-related messages
                        if ("update".equals(msg.kind())) {
                            AnnouncementManager.INSTANCE.setLastAcknowledgedUpdate(msg.semver());
                        }

                        // Find and mark the corresponding virtual entry in Patchouli as read
                        ResourceLocation entryId = ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "announcement_" + msg.id());
                        BookEntry entry = book.getContents().entries.get(entryId);
                        if (entry != null) {
                            PatchouliIntegration.setEntryAsRead(entry);
                        }
                    });
                }
            } else {
                // --- Normal Click Action: Mark this one as read ---
                PatchouliIntegration.setEntryAsRead(this.virtualEntry);
                AnnouncementManager.INSTANCE.markAsSeen(announcement.id());
                // Only acknowledge if it's an update-related message
                if ("update".equals(announcement.kind())) {
                    AnnouncementManager.INSTANCE.setLastAcknowledgedUpdate(announcement.semver());
                }
            }
            this.returnToBook();
        }).tooltip(Tooltip.create(markAsReadTooltip)));

        // 2. "Disable These" button (primary row)
        primaryBuilders.add(Button.builder(Component.translatable("gui.adorablehamsterpets.announcement.button.disable_icons"), button -> {
            ((ValidatedFieldAccessor<Boolean>) Configs.AHP_UI.enableNotificationIcons).adorablehamsterpets$set(false);
            Configs.AHP_UI.save();
            this.onClose();
        }).tooltip(Tooltip.create(Component.translatable("gui.adorablehamsterpets.announcement.button.disable_icons.tooltip"))));

        // 3. "Snooze (Days)" button (primary row)
        MutableComponent remindLaterTooltip = Component.translatable("gui.adorablehamsterpets.announcement.button.snooze_days.tooltip", Configs.AHP_UI.snoozeUpdateReminderDays.get());
        ClientAnnouncementState state = AnnouncementManager.INSTANCE.getClientState();
        Instant snoozeUntil = state.snoozed_ids().get(announcement.id());
        if (snoozeUntil != null && snoozeUntil.isAfter(Instant.now())) {
            Duration remaining = Duration.between(Instant.now(), snoozeUntil);
            remindLaterTooltip.append("\n\n").append(formatDuration(remaining));
        }
        primaryBuilders.add(Button.builder(Component.translatable("gui.adorablehamsterpets.announcement.button.snooze_days"), button -> {
            AnnouncementManager.INSTANCE.setSnooze(announcement.id(), Configs.AHP_UI.snoozeUpdateReminderDays.get());
            if (this.parentScreen instanceof TitleScreen) {
                AnnouncementManager.INSTANCE.queueDeferredReadMark(this.virtualEntry.getId());
            } else {
                PatchouliIntegration.setEntryAsRead(this.virtualEntry);
            }
            this.returnToBook();
        }).tooltip(Tooltip.create(remindLaterTooltip)));

        // 4. "Snooze (Session)" button (primary row)
        primaryBuilders.add(Button.builder(Component.translatable("gui.adorablehamsterpets.announcement.button.snooze_session"), button -> {
            // Snooze this specific announcement for the current session.
            AnnouncementManager.INSTANCE.snoozeForSession(announcement.id());

            // Mark the entry as read in Patchouli's data.
            if (this.parentScreen instanceof TitleScreen) {
                AnnouncementManager.INSTANCE.queueDeferredReadMark(this.virtualEntry.getId());
            } else {
                PatchouliIntegration.setEntryAsRead(this.virtualEntry);
            }
            this.returnToBook();
        }).tooltip(Tooltip.create(Component.translatable("gui.adorablehamsterpets.announcement.button.snooze_session.tooltip"))));

        // 5. "Changelog" button (secondary row, only if this is an update note)
        if ("update".equals(announcement.kind())) {
            String changelogUrl = String.format("https://modrinth.com/mod/adorable-hamster-pets/version/%s-1.21.1+fabric", announcement.semver());
            secondaryBuilders.add(Button.builder(Component.translatable("gui.adorablehamsterpets.announcement.button.changelog"), button -> {
                Util.getPlatform().openUri(URI.create(changelogUrl));
            }).tooltip(Tooltip.create(Component.translatable("gui.adorablehamsterpets.announcement.button.changelog.tooltip"))));
        }

        // 6. Discord Button (secondary row, always present)
        secondaryBuilders.add(Button.builder(Component.translatable("gui.adorablehamsterpets.announcement.button.discord"), button -> {
            Util.getPlatform().openUri(URI.create("https://discord.gg/w54mk5bqdf"));
        }).tooltip(Tooltip.create(Component.translatable("config.adorablehamsterpets.main.helpAndResources.joinDiscord.desc"))));

        // --- Button Row Layout ---
        int availableWidthButtons = Math.max(0, this.width - 2 * EDGE_MARGIN);

        // Compute starting X positions for each row by centering the row within the available width.
        int primaryCount = primaryBuilders.size();
        int secondaryCount = secondaryBuilders.size();
        int totalPrimaryWidth = primaryCount * buttonWidth + Math.max(0, primaryCount - 1) * buttonPadding;
        int totalSecondaryWidth = secondaryCount * buttonWidth + Math.max(0, secondaryCount - 1) * buttonPadding;
        int startXRow1 = EDGE_MARGIN + (availableWidthButtons - totalPrimaryWidth) / 2;
        int startXRow2 = EDGE_MARGIN + (availableWidthButtons - totalSecondaryWidth) / 2;

        // Compute Y positions for each row.
        int firstRowY = this.guiTop + this.scaledBackgroundHeight + this.scaledButtonPadding;
        int secondRowY = firstRowY + this.scaledButtonHeight + this.scaledButtonPadding;

        // --- Build and Add Buttons ---
        // Add primary row buttons
        for (int i = 0; i < primaryCount; i++) {
            Button.Builder builder = primaryBuilders.get(i);
            int currentX = startXRow1 + i * (buttonWidth + buttonPadding);
            ((ScreenWidgetAdder)(Object)this).adorablehamsterpets$addWidget(
                    builder.bounds(currentX, firstRowY, buttonWidth, this.scaledButtonHeight).build()
            );
        }
        // Add secondary row buttons
        for (int j = 0; j < secondaryCount; j++) {
            Button.Builder builder = secondaryBuilders.get(j);
            int currentX = startXRow2 + j * (buttonWidth + buttonPadding);
            ((ScreenWidgetAdder)(Object)this).adorablehamsterpets$addWidget(
                    builder.bounds(currentX, secondRowY, buttonWidth, this.scaledButtonHeight).build()
            );
        }
    }

    /**
     * Formats a Duration into a human-readable string (e.g., "2 days, 5 hours").
     *
     * @param duration The duration to format.
     * @return A Text component representing the formatted duration.
     */
    private Component formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + (days == 1 ? " day" : " days"));
        }
        if (hours > 0) {
            parts.add(hours + (hours == 1 ? " hour" : " hours"));
        }
        // Only show minutes if the total duration is less than a day for brevity
        if (days == 0 && minutes > 0) {
            parts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        }

        String formattedString = String.join(", ", parts);
        if (formattedString.isEmpty()) {
            formattedString = "less than a minute";
        }

        return Component.translatable("tooltip.adorablehamsterpets.hud.snooze_remaining", formattedString)
                .withStyle(ChatFormatting.GRAY);
    }

    /**
     * Closes this screen and returns to the parent screen (typically the Patchouli book,
     * but not always).If the parent screen is null, it closes the screen, returning to the game.
     */
    private void returnToBook() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    /**
     * Renders the screen's main content, including the scrollable markdown area and title.
     * It uses a scissor rectangle to confine the markdown rendering to the content area, allowing
     * for proper scrolling and clipping. It also dynamically scales the title text to ensure it
     * fits within the designated header space.
     *
     * @param context The rendering context.
     * @param mouseX  The current X position of the mouse.
     * @param mouseY  The current Y position of the mouse.
     * @param delta   The time elapsed since the last frame.
     */
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // --- 1. Update Hovered Style ---
        Component textAtMouse = this.getTextAt(mouseX, mouseY);
        this.hoveredStyle = (textAtMouse != null && textAtMouse.getStyle().getClickEvent() != null) ? textAtMouse.getStyle() : null;

        // --- 2. Call super.render() ---
        super.render(context, mouseX, mouseY, delta);

        // --- 3. Render Markdown Content ---
        // Use a scissor to clip the markdown content to the designated scrollable area.
        // Parameters are in window-space coordinates: left, top, right, bottom.
        context.enableScissor(
                this.guiLeft + this.scaledContentXOffset,
                this.guiTop + this.scaledContentYOffset,
                this.guiLeft + this.scaledContentXOffset + this.scaledContentWidth,
                this.guiTop + this.scaledContentYOffset + this.scaledContentHeight
        );
        markdownRenderer.render(context, (int) scrollY, this.hoveredStyle);
        context.disableScissor();

        // --- 4. Render Scrollbar ---
        int maxScroll = Math.max(0, markdownRenderer.getTotalHeight() - this.scaledContentHeight);
        if (maxScroll > 0) {
            int scrollbarX = this.guiLeft + this.scaledScrollBarXOffset;
            int scrollbarTravel = this.scaledScrollBarEndY - this.scaledScrollBarStartY;
            double scrollPercent = this.scrollY / maxScroll;
            int scrollbarY = this.guiTop + this.scaledScrollBarStartY + (int) (scrollPercent * scrollbarTravel);
            // Draw the scrollbar at a fixed size (4x25) regardless of scaling for consistent interaction.
            context.blit(SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0, 0, 4, 25, 4, 25);
        }

        // --- 5. Render Dynamic Title ---
        PoseStack matrices = context.pose();
        matrices.pushPose();
        float defaultScale = 1.3f * this.uiScale; // Scale title proportionally with the UI.
        int maxWidth = Math.round(222 * this.uiScale); // Scale the maximum width of the title proportionally with the UI.
        Component boldTitle = this.title.copy().withStyle(ChatFormatting.BOLD);
        int titleWidth = Minecraft.getInstance().font.width(boldTitle);
        float finalScale = defaultScale;


        if (titleWidth * defaultScale > maxWidth) {
            finalScale = (float) maxWidth / titleWidth; // Shrink to fit if oversized.
        }

        // Translate down from the top of the GUI by 9 scaled pixels so the title sits within the header.
        matrices.translate(this.width / 2.0, this.guiTop + Math.round(9 * this.uiScale), 0);
        matrices.scale(finalScale, finalScale, 1.0f);
        context.drawString(Minecraft.getInstance().font, boldTitle, -titleWidth / 2, 0, 0x323232, false);
        matrices.popPose();
    }

    /**
     * Renders the screen's background. This method first draws the default dark overlay, then
     * renders the custom background texture using a matrix transformation.
     * <p>
     * The matrix scaling is crucial: instead of giving {@code drawTexture} scaled region dimensions
     * (which crops the right and bottom edges), we scale the matrix itself and draw the *entire*
     * 256x256 texture. This ensures the full texture is scaled down correctly.
     *
     * @param context The rendering context.
     * @param mouseX  The current X position of the mouse.
     * @param mouseY  The current Y position of the mouse.
     * @param delta   The time elapsed since the last frame.
     */
    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        // --- Render Custom Background Texture ---
        // Draw the entire texture and scale it via the matrix, not by region size, to avoid cropping.
        context.pose().pushPose();
        context.pose().translate((float) this.guiLeft, (float) this.guiTop, 0.0F);
        context.pose().scale(this.uiScale, this.uiScale, 1.0F);
        context.blit(
                BACKGROUND_TEXTURE,
                0, 0, 0, 0,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT
        );
        context.pose().popPose();
    }

    /**
     * Handles key presses. Closes the screen if the inventory key is pressed.
     *
     * @param keyCode The key code of the key that was pressed.
     * @param scanCode The scan code of the key that was pressed.
     * @param modifiers Any modifier keys that were held down.
     * @return {@code true} if the event was handled, {@code false} otherwise.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Check if the inventory key was pressed
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose(); // Close the screen
            return true;  // Mark the event as handled
        }
        // For any other key, use the default behavior (e.g., ESC key)
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Handles mouse scrolling to adjust the vertical position of the markdown content.
     * The scroll amount is clamped between 0 and the maximum scrollable height. The scroll speed
     * is scaled by {@code uiScale} to feel consistent at different UI sizes.
     *
     * @return {@code true} to indicate the event was handled.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, markdownRenderer.getTotalHeight() - this.scaledContentHeight);
        double scrollStep = 10 * this.uiScale; // Scale scroll speed with UI size.
        this.scrollY = Mth.clamp(this.scrollY - verticalAmount * scrollStep, 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // --- 1. Check for Scrollbar Drag ---
            int maxScroll = Math.max(0, markdownRenderer.getTotalHeight() - this.scaledContentHeight);
            if (maxScroll > 0) {
                int scrollbarX = this.guiLeft + this.scaledScrollBarXOffset;
                int scrollbarTravel = this.scaledScrollBarEndY - this.scaledScrollBarStartY;
                double scrollPercent = this.scrollY / maxScroll;
                int scrollbarY = this.guiTop + this.scaledScrollBarStartY + (int) (scrollPercent * scrollbarTravel);

                // Check if the click is within the scrollbar handle's bounds
                if (mouseX >= scrollbarX && mouseX < scrollbarX + 4 && mouseY >= scrollbarY && mouseY < scrollbarY + 25) {
                    this.isDraggingScrollbar = true;
                    return true; // Consume the click
                }
            }

            // --- 2. Check for Text Link Click ---
            Component clickedText = this.getTextAt(mouseX, mouseY);
            if (clickedText != null) {
                Style style = clickedText.getStyle();
                if (this.handleComponentClicked(style)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDraggingScrollbar) {
            int maxScroll = Math.max(0, markdownRenderer.getTotalHeight() - this.scaledContentHeight);
            int scrollbarTravel = this.scaledScrollBarEndY - this.scaledScrollBarStartY;

            // Calculate the scroll percentage based on the mouse's position relative to the scrollbar track
            double relativeMouseY = mouseY - (this.guiTop + this.scaledScrollBarStartY);
            double scrollPercent = relativeMouseY / scrollbarTravel;

            // Set the new scrollY value and clamp it
            this.scrollY = Mth.clamp(scrollPercent * maxScroll, 0, maxScroll);

            return true; // Consume the drag event
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        this.returnToBook();
    }

    /**
     * Performs a hit-test to find the clickable {@link Component} component at the given mouse coordinates.
     * This method accounts for the current scroll position and iterates through the pre-wrapped lines
     * of markdown content, checking the vertical and horizontal bounds of each line block to find a
     * match.
     *
     * @param x The mouse's X coordinate.
     * @param y The mouse's Y coordinate.
     * @return The {@link Component} with a clickable style at the given position, or {@code null} if none is found.
     */
    @Nullable
    public Component getTextAt(double x, double y) {
        // --- 1. Calculate Mouse Position Relative to Scrollable Content ---
        int scrollOffset = (int) (this.guiTop + this.scaledContentYOffset - this.scrollY);
        int relativeY = (int) y - scrollOffset;

        if (relativeY < 0) {
            return null; // Mouse is above the content area
        }

        int currentY = 0;
        for (String originalLine : markdownRenderer.lines) {
            String trimmedLine = originalLine.trim();
            int lineHeight;
            int startX = this.guiLeft + this.scaledContentXOffset;
            int contentWidth = markdownRenderer.width;
            String content = trimmedLine;
            boolean isListItem = trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.matches("^\\d+\\.\\s.*");

            // --- 2. Calculate the Height of the Current Line Block ---
            if (trimmedLine.isEmpty()) {
                lineHeight = font.lineHeight / 2;
            } else if (trimmedLine.startsWith("#")) {
                int level = 0;
                while (level < trimmedLine.length() && trimmedLine.charAt(level) == '#') level++;
                String text = trimmedLine.substring(level).trim();
                float scale = Math.max(1.0f, 2.0f - (level - 1) * 0.25f);
                int scaledWidth = (int) (markdownRenderer.width / scale);
                MutableComponent styledText = Component.literal(text).setStyle(Style.EMPTY.withBold(true));
                List<FormattedCharSequence> wrappedLines = Minecraft.getInstance().font.split(styledText, scaledWidth);
                int heightOfLines = wrappedLines.size() * (int) (font.lineHeight * scale);
                int totalSpacing = Math.max(0, wrappedLines.size() - 1) * MarkdownRenderer.LINE_SPACING;
                lineHeight = heightOfLines + totalSpacing + MarkdownRenderer.HEADING_BOTTOM_MARGIN;
            } else if (trimmedLine.equals("---")) {
                lineHeight = MarkdownRenderer.DIVIDER_HEIGHT;
            } else {
                int indentationLevel = MarkdownRenderer.getIndentationLevel(originalLine);
                if (isListItem) {
                    contentWidth = markdownRenderer.width - ((indentationLevel + 1) * MarkdownRenderer.LIST_INDENT);
                    startX += (indentationLevel * MarkdownRenderer.LIST_INDENT) + MarkdownRenderer.LIST_INDENT;
                    if (trimmedLine.matches("^\\d+\\.\\s.*")) {
                        content = trimmedLine.substring(trimmedLine.indexOf('.') + 1).trim();
                    } else {
                        content = trimmedLine.substring(1).trim();
                    }
                } else {
                    contentWidth = markdownRenderer.width - (indentationLevel * MarkdownRenderer.LIST_INDENT);
                    startX += indentationLevel * MarkdownRenderer.LIST_INDENT;
                    content = originalLine.substring(indentationLevel * MarkdownRenderer.SPACES_PER_INDENT_LEVEL);
                }
                MutableComponent styledText = markdownRenderer.parseLineToText(content, null);
                List<FormattedCharSequence> wrappedLines = font.split(styledText, contentWidth);
                lineHeight = wrappedLines.size() * (font.lineHeight + MarkdownRenderer.LINE_SPACING);
            }

            // --- 3. Check if Mouse is Within This Line Block's Vertical Bounds ---
            if (relativeY >= currentY && relativeY < currentY + lineHeight) {
                // --- 4. Find the Specific Style (for paragraphs and list items) ---
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#") && !trimmedLine.equals("---")) {
                    // Re-calculate contentWidth and startX for this specific block type to pass to the text handler
                    int indentationLevel = MarkdownRenderer.getIndentationLevel(originalLine);
                    if (isListItem) {
                        contentWidth = markdownRenderer.width - ((indentationLevel + 1) * MarkdownRenderer.LIST_INDENT);
                    } else {
                        contentWidth = markdownRenderer.width - (indentationLevel * MarkdownRenderer.LIST_INDENT);
                    }

                    MutableComponent styledText = markdownRenderer.parseLineToText(content, null);
                    List<FormattedCharSequence> wrappedLines = font.split(styledText, contentWidth);
                    int yInBlock = relativeY - currentY;
                    int lineIndex = yInBlock / (font.lineHeight + MarkdownRenderer.LINE_SPACING);

                    if (lineIndex >= 0 && lineIndex < wrappedLines.size()) {
                        FormattedCharSequence orderedText = wrappedLines.get(lineIndex);
                        int relativeX = (int) x - startX;
                        Style style = this.minecraft.font.getSplitter().componentStyleAtWidth(orderedText, relativeX);
                        if (style != null && style.getClickEvent() != null) {
                            return Component.literal("").setStyle(style);
                        }
                    }
                }
                return null; // Inside the block's height, but not on a clickable element
            }

            // --- 5. Move to the Next Line Block ---
            currentY += lineHeight;
        }
        return null;
    }
}