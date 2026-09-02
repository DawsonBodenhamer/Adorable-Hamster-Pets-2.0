package net.dawson.adorablehamsterpets.client.gui.widgets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.math.Axis;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.announcements.Semver;
import net.dawson.adorablehamsterpets.client.gui.AnnouncementScreen;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.mixin.client.accessor.HandledScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.Comparator;
import java.util.List;

/**
 * An interactive widget representing the announcement icon, designed to be
 * displayed on top of GUI screens. It handles its own rendering, animations,
 * tooltips, and click actions.
 */
public class AnnouncementIconWidget extends Button {
    private static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/item/announcement_bell_icon.png");
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    private final Screen parentScreen;
    private int lastTargetX = -1;
    private int lastTargetY = -1;

    public AnnouncementIconWidget(int x, int y, int width, int height, OnPress onPress, Screen parentScreen) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.parentScreen = parentScreen;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        AnnouncementIconAnimator animator = AnnouncementIconAnimator.INSTANCE;

        // --- 1. Dynamic Position Calculation ---
        if (this.parentScreen instanceof AbstractContainerScreen<?> containerScreen) {
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
            if (containerScreen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) {
                // Creative Inventory (above the top right corner, to avoid conflicting with inventory mods like JEI, EMI, and REI)
                targetX = guiLeft + guiWidth - 22 + + neoForgeCreativeModeOffset + Configs.AHP_UI.creativeWidgetIconSettings.get().offsetX.get();
                targetY = guiTop - 47 + Configs.AHP_UI.creativeWidgetIconSettings.get().offsetY.get();
            } else {
                // Survival Inventory (overlapping top right corner)
                targetX = guiLeft + guiWidth - this.width + 4 + Configs.AHP_UI.survivalWidgetIconSettings.get().offsetX.get();
                targetY = guiTop - 4 + Configs.AHP_UI.survivalWidgetIconSettings.get().offsetY.get();
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
        float configScale = Configs.AHP_UI.hudIconScale.get(); // Use HUD scale for title screen too
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
        context.pose().pushPose();
        // Use the precise double values for rendering to avoid pixel-snapping.
        context.pose().translate(renderX + (this.width / 2.0), renderY + (this.height / 2.0), 0);
        context.pose().scale(finalScale, finalScale, 1.0f);
        context.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        context.pose().translate(-(ICON_WIDTH / 2.0), -(ICON_HEIGHT / 2.0), 0);

        context.blit(ICON_TEXTURE, 0, 0, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);

        context.pose().popPose();

        // --- 4. Render Tooltip ---
        if (this.isHovered()) {
            List<AnnouncementManager.PendingNotification> notifications = AnnouncementManager.INSTANCE.getPendingNotifications();
            if (!notifications.isEmpty()) {
                List<Component> tooltipLines = new java.util.ArrayList<>();
                Component modNameText = Component.translatable("key.categories.adorablehamsterpets.main").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);

                Component mainTooltipLine = null;
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
                    context.renderComponentTooltip(Minecraft.getInstance().font, tooltipLines, mouseX, mouseY);
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
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        // --- 2. Execute Click Logic ---
        // Get notifications directly from the manager so the icon can appear on the title screen
        List<AnnouncementManager.PendingNotification> notifications = AnnouncementManager.INSTANCE.getPendingNotifications();
        if (notifications.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Identifier bookId = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book");

        if (this.parentScreen instanceof TitleScreen) {
            // --- 1. Title Screen Logic ---
            // Find the single LATEST "update available" notification to display. This prevents ambiguity if
            // multiple are pending and avoids opening the Patchouli book GUI, which would crash from the title screen.
            notifications.stream()
                    .filter(n -> n.reason().equals(AnnouncementManager.PendingNotification.UPDATE_AVAILABLE_ANNOUNCEMENT))
                    .max(Comparator.comparing(n -> Semver.parse(n.announcement().semver()))) // Find the highest version
                    .ifPresent(notification -> {
                        Announcement announcement = notification.announcement();
                        // 26.2 port: a Patchouli virtual entry used to be built here purely
                        // to hand to the screen; without the book there is nothing to pass.
                        client.setScreen(new AnnouncementScreen(announcement, notification.reason(), this.parentScreen, null));
                    });
        } else {
            if (notifications.size() == 1) {
                // --- 2. Direct Open Logic ---
                // Open directly to the custom GUI if only one message is available
                AnnouncementManager.PendingNotification notification = notifications.get(0);
                Announcement announcement = notifications.get(0).announcement();
                // Passing null as the parent tells the screen to return to the game HUD on close.
                client.setScreen(new AnnouncementScreen(announcement, notification.reason(), null, null));
            } else {
                // --- Multiple Pending Notifications Logic ---
                // 26.2 port: this used to open the guide book's landing page. With no
                // book, show the most recent notification instead of dropping the click.
                AnnouncementManager.PendingNotification latest = notifications.get(notifications.size() - 1);
                client.setScreen(new AnnouncementScreen(latest.announcement(), latest.reason(), null, null));
            }
        }
    }
}