package net.dawson.adorablehamsterpets.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.RenameIconPlacement;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HamsterInventoryScreen extends HandledScreen<HamsterInventoryScreenHandler> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Identifier TEXTURE = new Identifier(AdorableHamsterPets.MOD_ID, "textures/gui/hamster_inventory_gui.png");
    private static final Identifier PENCIL_ICON = new Identifier(AdorableHamsterPets.MOD_ID, "textures/gui/pencil_icon_ui.png");

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean isRenaming = false;
    private String currentName = "";
    private String initialName = "";
    private boolean hasUnsavedName = false;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterInventoryScreen(HamsterInventoryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 222;
        this.playerInventoryTitleY = 139 - 11; // Position just above player inventory
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void init() {
        super.init();

        // Restore default title centering (used as fallback if renaming is disabled)
        this.titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        this.titleY = 6;

        this.playerInventoryTitleX = 7;

        // Initialize naming fields based on actual entity's data
        HamsterEntity hamster = this.handler.getHamsterEntity();
        if (hamster != null && !this.hasUnsavedName) {
            Text customName = hamster.getCustomName();
            this.currentName = customName != null ? customName.getString() : "";
            this.initialName = this.currentName;
        }
    }

    @Override
    public void removed() {
        super.removed();
        // Save name and send packet as screen closes
        sendRenamePacket();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Input Handling
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Configs.AHP.enableGuiRenaming) {
            int boxX = this.x + 7;
            int boxY = this.y + 6;

            // Check if click occurred inside 162x10 header bounding box
            if (mouseX >= boxX && mouseX <= boxX + 162 && mouseY >= boxY && mouseY <= boxY + 10) {
                if (!this.isRenaming) {
                    if (!Configs.AHP.consumeNameTagForGuiRename || hasNameTag()) {
                        this.isRenaming = true;
                    } else {
                        // Play failure sound if they lack required name tag
                        if (this.client != null && this.client.player != null) {
                            this.client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                        }
                    }
                }
                return true;
            } else if (this.isRenaming) {
                // Clicked outside box, save and stop renaming
                saveAndStopRenaming();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isRenaming) {
            // Unfocus keys
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveAndStopRenaming();
                return true;
            }
            // Backspace handling
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.currentName.isEmpty()) {
                    this.currentName = this.currentName.substring(0, this.currentName.length() - 1);
                    this.hasUnsavedName = true;
                }
                return true;
            }
            // Intercept inventory key so screen doesn't close when typing 'e'
            if (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.isRenaming) {
            // Filter out unprintable/illegal characters
            if (SharedConstants.isValidChar(chr)) {
                this.currentName += chr;
                this.hasUnsavedName = true;
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Rendering Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // --- Draw Hamster Entity ---
        int boxX = this.x + 34;
        int boxY = this.y + 55;
        int size = 60;

        HamsterEntity hamster = this.handler.getHamsterEntity();
        if (hamster != null) {
            // Flag renderer to hide nameplate during this specific draw call
            HamsterRenderer.IS_RENDERING_IN_GUI.set(true);
            try {
                // 1.20.1 drawEntity signature
                InventoryScreen.drawEntity(
                        context,
                        boxX,
                        boxY,
                        size,
                        (float)boxX - mouseX,
                        (float)boxY - 30 - mouseY,
                        hamster
                );
            } finally {
                HamsterRenderer.IS_RENDERING_IN_GUI.set(false);
            }
        }

        // --- Render Dynamic Rename Box ---
        if (Configs.AHP.enableGuiRenaming) {
            renderRenameBox(context, mouseX, mouseY);
        }

        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Only draw standard static title if custom dynamic renaming is disabled
        if (!Configs.AHP.enableGuiRenaming) {
            context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        }

        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);

        int labelColor = 4210752;
        drawCenteredLabel(context, Text.translatable("entity.adorablehamsterpets.hamster.inventory_left_cheek_title"), 52, 80, labelColor);
        drawCenteredLabel(context, Text.translatable("entity.adorablehamsterpets.hamster.inventory_right_cheek_title"), 124, 80, labelColor);
        drawCenteredLabel(context, Text.translatable("entity.adorablehamsterpets.hamster.inventory_bling_title"), 90, 29, labelColor);
        drawCenteredLabel(context, Text.translatable("entity.adorablehamsterpets.hamster.inventory_armor_title"), 142, 29, labelColor);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Renders the dynamic renaming GUI elements, handling scaling, horizontal centering,
     * the blinking cursor, hover states, and dynamic tooltips.
     */
    private void renderRenameBox(DrawContext context, int mouseX, int mouseY) {
        int boxX = this.x + 7;
        int boxY = this.y + 6;
        int boxWidth = 162;
        int boxHeight = 10;
        boolean hovered = mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight;

        // Base text is either custom name, or "Hamster" if no name has been set
        String baseText = this.currentName.isEmpty() && !this.isRenaming ? "Hamster" : this.currentName;

        // Append a blinking underscore if currently actively typing
        String displayText = baseText + (this.isRenaming && (Util.getMeasuringTimeMs() / 500 % 2 == 0) ? "_" : "");

        int textWidth = this.textRenderer.getWidth(displayText);
        int unscaledWidth = 8 + 3 + textWidth; // Icon (8) + Margin (3) + Text

        // Calculate dynamic downscaling to prevent overflowing 162px boundary
        float scale = Math.min(1.0f, boxWidth / (float) unscaledWidth);
        int scaledWidth = (int) (unscaledWidth * scale);

        // Calculate starting X to ensure always perfectly centered
        int startX = boxX + (boxWidth / 2) - (scaledWidth / 2);

        context.getMatrices().push();
        context.getMatrices().translate(startX, boxY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        RenameIconPlacement placement = Configs.AHP.renameIconPlacement.get();
        int iconX, textX;

        if (placement == RenameIconPlacement.LEFT) {
            iconX = 0;
            textX = 11; // 8 (icon) + 3 (margin)
        } else {
            textX = 0;
            iconX = textWidth + 3;
        }

        // Draw hover/active underline
        if (hovered || this.isRenaming) {
            int lineStartX = placement == RenameIconPlacement.LEFT ? 8 : -3;
            int lineEndX = placement == RenameIconPlacement.LEFT ? 14 + textWidth : textWidth + 11;
            context.fill(lineStartX, 7, lineEndX, 8, 0xFFADADAD);
        }

        // Draw pencil icon
        context.drawTexture(PENCIL_ICON, iconX, 0, 0, 0, 8, 8, 8, 8);

        // Draw text
        context.drawText(this.textRenderer, displayText, textX, 0, 4210752, false);

        context.getMatrices().pop();

        // Render dynamic tooltips if hovered and not actively typing
        if (hovered && !this.isRenaming) {
            List<Text> tooltip = new ArrayList<>();
            if (Configs.AHP.consumeNameTagForGuiRename && !hasNameTag()) {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.rename.missing_tag").formatted(Formatting.RED));
            } else {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.rename").formatted(Formatting.GOLD));
                if (Configs.AHP.consumeNameTagForGuiRename) {
                    tooltip.add(Text.translatable("tooltip.adorablehamsterpets.rename.consume").formatted(Formatting.GRAY));
                }
            }
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Checks the player's inventory and the hamster's cheek pouches for a Name Tag.
     */
    private boolean hasNameTag() {
        if (this.client == null || this.client.player == null) return false;
        PlayerEntity player = this.client.player;

        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(Items.NAME_TAG)) return true;
        }

        HamsterEntity hamster = this.handler.getHamsterEntity();
        if (hamster != null) {
            for (int i = 0; i < HamsterInventoryUtil.CHEEK_POUCH_SIZE; i++) {
                if (hamster.getItems().get(i).isOf(Items.NAME_TAG)) return true;
            }
        }
        return false;
    }

    private void saveAndStopRenaming() {
        this.isRenaming = false;
        // Dispatch the packet to consume name tag while screen is open
        this.sendRenamePacket();
    }

    /**
     * Transmits the RenameHamsterC2SPacket to the server if changes were actually made.
     */
    private void sendRenamePacket() {
        if (this.hasUnsavedName && !this.currentName.equals(this.initialName)) {
            HamsterEntity hamster = this.handler.getHamsterEntity();
            if (hamster != null) {
                ModPackets.CHANNEL.sendToServer(new ModPackets.RenameHamsterC2SPacket(hamster.getId(), this.currentName.trim()));
                this.initialName = this.currentName; // Update initial state to prevent duplicate packets
                this.hasUnsavedName = false;
            }
        }
    }

    private void drawCenteredLabel(DrawContext context, Text text, int centerX, int y, int color) {
        int width = this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, centerX - (width / 2), y, color, false);
    }
}