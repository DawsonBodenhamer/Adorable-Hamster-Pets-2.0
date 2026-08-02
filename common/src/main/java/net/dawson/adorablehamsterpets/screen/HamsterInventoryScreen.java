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
    private static final Identifier ARMOR_VISIBILITY_CHECKBOX = new Identifier(
            AdorableHamsterPets.MOD_ID, "textures/gui/armor_visibility_checkbox.png");
    private static final Identifier ARMOR_VISIBILITY_CHECK_MARK = new Identifier(
            AdorableHamsterPets.MOD_ID, "textures/gui/armor_visibility_check_mark.png");
    private static final int ARMOR_VISIBILITY_TOGGLE_X = 6;
    private static final int ARMOR_VISIBILITY_TOGGLE_Y = 5;
    private static final int ARMOR_VISIBILITY_TOGGLE_SIZE = 10;
    private static final int ARMOR_VISIBILITY_CHECKBOX_X = 8;
    private static final int ARMOR_VISIBILITY_CHECKBOX_Y = 7;
    private static final int ARMOR_VISIBILITY_CHECKBOX_SIZE = 6;
    private static final int RENAME_BOX_X = 18;
    private static final int RENAME_BOX_Y = 6;
    private static final int RENAME_BOX_WIDTH = 151;
    private static final int RENAME_BOX_HEIGHT = 10;

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
        HamsterEntity hamster = this.handler.getHamsterEntity();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && hamster != null
                && isArmorVisibilityToggleHovered(mouseX, mouseY)) {
            ModPackets.CHANNEL.sendToServer(
                    new ModPackets.UpdateHamsterArmorVisibilityC2SPacket(
                            hamster.getId(), !hamster.isArmorVisible()));
            return true;
        }

        if (Configs.AHP_MAIN.enableGuiRenaming) {
            int boxX = this.x + RENAME_BOX_X;
            int boxY = this.y + RENAME_BOX_Y;

            // Keep rename interaction out of the checkbox's reserved header space.
            if (mouseX >= boxX
                    && mouseX <= boxX + RENAME_BOX_WIDTH
                    && mouseY >= boxY
                    && mouseY <= boxY + RENAME_BOX_HEIGHT) {
                if (!this.isRenaming) {
                    if (!Configs.AHP_UI.consumeNameTagForGuiRename || hasNameTag()) {
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
        if (Configs.AHP_MAIN.enableGuiRenaming) {
            renderRenameBox(context, mouseX, mouseY);
        }

        drawMouseoverTooltip(context, mouseX, mouseY);
        renderArmorVisibilityToggle(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Only draw standard static title if custom dynamic renaming is disabled
        if (!Configs.AHP_MAIN.enableGuiRenaming) {
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
        int boxX = this.x + RENAME_BOX_X;
        int boxY = this.y + RENAME_BOX_Y;
        int boxWidth = RENAME_BOX_WIDTH;
        int boxHeight = RENAME_BOX_HEIGHT;
        boolean hovered = mouseX >= boxX
                && mouseX <= boxX + boxWidth
                && mouseY >= boxY
                && mouseY <= boxY + boxHeight
                && !isArmorVisibilityToggleHovered(mouseX, mouseY);

        // Base text is either custom name, or configured default if no name has been set
        String defaultName = Text.translatable(Configs.AHP_MAIN.useHampterName ? "entity.adorablehamsterpets.hampter" : "entity.adorablehamsterpets.hamster").getString();
        String baseText = this.currentName.isEmpty() && !this.isRenaming ? defaultName : this.currentName;

        // Append a blinking underscore if currently actively typing
        String displayText = baseText + (this.isRenaming && (Util.getMeasuringTimeMs() / 500 % 2 == 0) ? "_" : "");

        int textWidth = this.textRenderer.getWidth(displayText);
        int unscaledWidth = 8 + 3 + textWidth; // Icon (8) + Margin (3) + Text

        // Downscale before reaching the checkbox's reserved header space.
        float scale = Math.min(1.0f, boxWidth / (float) unscaledWidth);
        int scaledWidth = (int) (unscaledWidth * scale);

        // Calculate starting X to ensure always perfectly centered
        int startX = boxX + (boxWidth / 2) - (scaledWidth / 2);

        context.getMatrices().push();
        context.getMatrices().translate(startX, boxY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        RenameIconPlacement placement = Configs.AHP_UI.renameIconPlacement.get();
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
            if (Configs.AHP_UI.consumeNameTagForGuiRename && !hasNameTag()) {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.rename.missing_tag").formatted(Formatting.RED));
            } else {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.rename").formatted(Formatting.GOLD));
                if (Configs.AHP_UI.consumeNameTagForGuiRename) {
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

    private void renderArmorVisibilityToggle(
            DrawContext context, int mouseX, int mouseY) {
        HamsterEntity hamster = this.handler.getHamsterEntity();
        if (hamster == null) {
            return;
        }

        context.drawTexture(
                ARMOR_VISIBILITY_CHECKBOX,
                this.x + ARMOR_VISIBILITY_CHECKBOX_X,
                this.y + ARMOR_VISIBILITY_CHECKBOX_Y,
                0,
                0,
                ARMOR_VISIBILITY_CHECKBOX_SIZE,
                ARMOR_VISIBILITY_CHECKBOX_SIZE,
                ARMOR_VISIBILITY_CHECKBOX_SIZE,
                ARMOR_VISIBILITY_CHECKBOX_SIZE);

        if (!hamster.isArmorVisible()) {
            context.drawTexture(
                    ARMOR_VISIBILITY_CHECK_MARK,
                    this.x + ARMOR_VISIBILITY_TOGGLE_X,
                    this.y + ARMOR_VISIBILITY_TOGGLE_Y,
                    0,
                    0,
                    ARMOR_VISIBILITY_TOGGLE_SIZE,
                    ARMOR_VISIBILITY_TOGGLE_SIZE,
                    ARMOR_VISIBILITY_TOGGLE_SIZE,
                    ARMOR_VISIBILITY_TOGGLE_SIZE);
        }

        if (isArmorVisibilityToggleHovered(mouseX, mouseY)) {
            List<Text> tooltip = new ArrayList<>();
            String actionKey = hamster.isArmorVisible()
                    ? "tooltip.adorablehamsterpets.armor_visibility.hide"
                    : "tooltip.adorablehamsterpets.armor_visibility.show";
            tooltip.add(Text.translatable(actionKey).formatted(Formatting.GOLD));
            String globalKey = Configs.AHP_MAIN.enableArmorVisuals
                    ? "tooltip.adorablehamsterpets.armor_visibility.global_override"
                    : "tooltip.adorablehamsterpets.armor_visibility.globally_disabled";
            tooltip.add(Text.translatable(globalKey).formatted(Formatting.GRAY));
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private boolean isArmorVisibilityToggleHovered(double mouseX, double mouseY) {
        int toggleX = this.x + ARMOR_VISIBILITY_TOGGLE_X;
        int toggleY = this.y + ARMOR_VISIBILITY_TOGGLE_Y;
        return mouseX >= toggleX
                && mouseX < toggleX + ARMOR_VISIBILITY_TOGGLE_SIZE
                && mouseY >= toggleY
                && mouseY < toggleY + ARMOR_VISIBILITY_TOGGLE_SIZE;
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
