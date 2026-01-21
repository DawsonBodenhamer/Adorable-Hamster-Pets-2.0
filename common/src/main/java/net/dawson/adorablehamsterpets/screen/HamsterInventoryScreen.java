package net.dawson.adorablehamsterpets.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HamsterInventoryScreen extends HandledScreen<HamsterInventoryScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/gui/hamster_inventory_gui.png");

    public HamsterInventoryScreen(HamsterInventoryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        this.backgroundHeight = 222;

        // Adjust player inventory label Y position
        this.playerInventoryTitleY = 139 - 11; // Position just above the player inventory (Y=139 - approx text height)
    }

    @Override
    protected void init() {
        super.init();
        // Restore default title centering
        this.titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        this.titleY = 6; // Default Y position near the top

        // Set player inventory title position explicitly
        this.playerInventoryTitleX = 7;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2; // Centered X
        int y = (this.height - this.backgroundHeight) / 2; // Centered Y
        // Draw background texture
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background and slots
        super.render(context, mouseX, mouseY, delta);

        // --- Draw the Hamster Entity ---
        int boxX = this.x + 8;
        int boxY = this.y + 12;
        int boxWidth = 59 - 8;
        int boxHeight = 69 - 18;
        int size = 60;

        // Get entity instance directly from the handler
        HamsterEntity hamster = this.handler.getHamsterEntity();

        if (hamster != null) {
            // Call static helper method using the box coordinates
            InventoryScreen.drawEntity(
                    context,
                    boxX,
                    boxY,
                    boxX + boxWidth,
                    boxY + boxHeight,
                    size,
                    0.0625F,
                    (float)mouseX,
                    (float)mouseY,
                    hamster // Pass the entity instance
            );
        }

        // Draw tooltips last
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Restore drawing the screen title using default positioning fields
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);

        // Draw the player inventory title
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);

        int labelColor = 4210752; // Dark Gray

        // --- "Left Cheek" and "Right Cheek" Text ---
        Text customTextLeft = Text.translatable("entity.adorablehamsterpets.hamster.inventory_left_cheek_title");
        drawCenteredLabel(context, customTextLeft, 52, 80, labelColor);

        Text customTextRight = Text.translatable("entity.adorablehamsterpets.hamster.inventory_right_cheek_title");
        drawCenteredLabel(context, customTextRight, 124, 80, labelColor);

        // --- "Bling" and "Armor" Text ---
        Text blingText = Text.translatable("entity.adorablehamsterpets.hamster.inventory_bling_title");
        drawCenteredLabel(context, blingText, 90, 29, labelColor);

        Text armorText = Text.translatable("entity.adorablehamsterpets.hamster.inventory_armor_title");
        drawCenteredLabel(context, armorText, 142, 29, labelColor);
    }

    /**
     * Helper to draw text centered horizontally around a specific X coordinate.
     */
    private void drawCenteredLabel(DrawContext context, Text text, int centerX, int y, int color) {
        int width = this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, centerX - (width / 2), y, color, false);
    }
}