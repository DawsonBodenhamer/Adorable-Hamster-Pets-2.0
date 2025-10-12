package net.dawson.adorablehamsterpets.client.announcements;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import com.mojang.blaze3d.systems.RenderSystem;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconAnimator;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

/**
 * Renders the non-interactive announcement icon on the main game HUD.
 * This class is responsible for calculating the icon's position based on config settings
 * and drawing it using animation values from the central AnnouncementIconAnimator.
 */
public class AnnouncementHudRenderer {
    private static final Identifier ICON_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/item/announcement_bell_icon.png");
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    public void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        final AhpConfig config = Configs.AHP;

        // --- 1. Pre-render Checks ---
        // Do not render if the config disables it, a GUI is open, or there are no notifications.
        if (!config.enableHudIcon.get() || client.currentScreen != null) {
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
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // This now calculates and sets the target position inside the animator
        animator.updateTargetPosition(screenWidth, screenHeight);

        // This snaps the current position to the target for the non-interactive HUD icon
        animator.updateHudPosition(animator.getTargetX(), animator.getTargetY());

        // --- 4. Get Animation State from Central Animator ---
        float animScale = animator.getRenderScale(tickDelta);
        float configScale = Configs.AHP.hudIconScale.get();
        float finalScale = animScale * configScale;
        float angle = animator.getRenderAngle(tickDelta);
        double renderX = animator.getRenderX(tickDelta);
        double renderY = animator.getRenderY(tickDelta);

        // --- 5. Render the Icon ---
        context.getMatrices().push();
        // Use the interpolated renderX and renderY values
        // Translate to the icon's center for proper scaling and rotation
        try {
            context.getMatrices().translate(renderX + (ICON_WIDTH / 2.0), renderY + (ICON_HEIGHT / 2.0), 0);
            context.getMatrices().scale(finalScale, finalScale, 1.0f);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            // Translate back to the top-left corner to draw the texture
            context.getMatrices().translate(-(ICON_WIDTH / 2.0), -(ICON_HEIGHT / 2.0), 0);

            RenderSystem.enableBlend();
            context.drawTexture(ICON_TEXTURE, 0, 0, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
        } finally {
            context.getMatrices().pop();
        }

        // --- 6. Render Tooltip on Hover ---
        double mouseX = client.mouse.getX();
        double mouseY = client.mouse.getY();

        if (mouseX >= renderX && mouseX <= renderX + (ICON_WIDTH * finalScale) &&
                mouseY >= renderY && mouseY <= renderY + (ICON_HEIGHT * finalScale)) {

            List<Text> tooltipLines = new java.util.ArrayList<>();
            Text modNameText = Text.translatable("key.categories.adorablehamsterpets.main").formatted(Formatting.BLUE, Formatting.ITALIC);

            AnnouncementManager.PendingNotification primary = notifications.get(0);
            // Use centralized helper method to get the main tooltip line
            Text mainTooltipLine = AnnouncementManager.getTooltipTextForNotification(primary);

            tooltipLines.add(mainTooltipLine);
            tooltipLines.add(modNameText);
            context.drawTooltip(client.textRenderer, tooltipLines, (int)mouseX, (int)mouseY);
        }
    }
}