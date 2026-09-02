package net.dawson.adorablehamsterpets.client.announcements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconAnimator;
import net.dawson.adorablehamsterpets.config.AhpUiConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.List;

/**
 * Renders the non-interactive announcement icon on the main game HUD.
 * This class is responsible for calculating the icon's position based on config settings
 * and drawing it using animation values from the central AnnouncementIconAnimator.
 */
public class AnnouncementHudRenderer {
    private static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/item/announcement_bell_icon.png");
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    public void render(GuiGraphics context, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        final AhpUiConfig config = Configs.AHP_UI;

        // --- 1. Pre-render Checks ---
        // Do not render if the config disables it, a GUI is open, or there are no notifications
        if (!config.enableHudIcon.get() || config.serverDisableAnnouncements || client.screen != null) {
            return;
        }
        List<AnnouncementManager.PendingNotification> notifications = AdorableHamsterPetsClient.getPendingNotifications();
        if (notifications.isEmpty()) {
            return;
        }

        // --- 2. Get Animation State from Central Animator ---
        AnnouncementIconAnimator animator = AnnouncementIconAnimator.INSTANCE;
        animator.setHovered(false); // The HUD icon is never hovered

        // --- 3. Calculate Position and Update Animator ---
        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        // This now calculates and sets the target position inside the animator
        animator.updateTargetPosition(screenWidth, screenHeight);

        // This snaps the current position to the target for the non-interactive HUD icon
        animator.updateHudPosition(animator.getTargetX(), animator.getTargetY());

        // --- 4. Get Animation State from Central Animator ---
        float animScale = animator.getRenderScale(tickDelta);
        float configScale = Configs.AHP_UI.hudIconScale.get();
        float finalScale = animScale * configScale;
        float angle = animator.getRenderAngle(tickDelta);
        double renderX = animator.getRenderX(tickDelta);
        double renderY = animator.getRenderY(tickDelta);

        // --- 5. Render the Icon ---
        context.pose().pushPose();
        // Use the interpolated renderX and renderY values
        // Translate to the icon's center for proper scaling and rotation
        try {
            context.pose().translate(renderX + (ICON_WIDTH / 2.0), renderY + (ICON_HEIGHT / 2.0), 0);
            context.pose().scale(finalScale, finalScale, 1.0f);
            context.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            // Translate back to the top-left corner to draw the texture
            context.pose().translate(-(ICON_WIDTH / 2.0), -(ICON_HEIGHT / 2.0), 0);

            RenderSystem.enableBlend();
            context.blit(ICON_TEXTURE, 0, 0, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
        } finally {
            context.pose().popPose();
        }

        // --- 6. Render Tooltip on Hover ---
        double mouseX = client.mouseHandler.xpos();
        double mouseY = client.mouseHandler.ypos();

        if (mouseX >= renderX && mouseX <= renderX + (ICON_WIDTH * finalScale) &&
                mouseY >= renderY && mouseY <= renderY + (ICON_HEIGHT * finalScale)) {

            List<Component> tooltipLines = new java.util.ArrayList<>();
            Component modNameText = Component.translatable("key.categories.adorablehamsterpets.main").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);

            AnnouncementManager.PendingNotification primary = notifications.get(0);
            // Use centralized helper method to get the main tooltip line
            Component mainTooltipLine = AnnouncementManager.getTooltipTextForNotification(primary);

            tooltipLines.add(mainTooltipLine);
            tooltipLines.add(modNameText);
            context.renderComponentTooltip(client.font, tooltipLines, (int)mouseX, (int)mouseY);
        }
    }
}