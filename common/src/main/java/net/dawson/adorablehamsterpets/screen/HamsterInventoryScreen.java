package net.dawson.adorablehamsterpets.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.RenameIconPlacement;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.payload.RenameHamsterPayload;
import net.dawson.adorablehamsterpets.networking.payload.UpdateHamsterArmorVisibilityPayload;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HamsterInventoryScreen extends AbstractContainerScreen<HamsterInventoryScreenHandler> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/gui/hamster_inventory_gui.png");
    private static final Identifier PENCIL_ICON = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/gui/pencil_icon_ui.png");
    private static final Identifier ARMOR_VISIBILITY_CHECKBOX = Identifier.fromNamespaceAndPath(
            AdorableHamsterPets.MOD_ID, "textures/gui/armor_visibility_checkbox.png");
    private static final Identifier ARMOR_VISIBILITY_CHECK_MARK = Identifier.fromNamespaceAndPath(
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

    public HamsterInventoryScreen(HamsterInventoryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageHeight = 222;
        this.inventoryLabelY = 139 - 11; // Position just above player inventory
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void init() {
        super.init();

        // Restore default title centering (used as fallback if renaming is disabled)
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
        this.titleLabelY = 6;

        this.inventoryLabelX = 7;

        // Initialize naming fields based on actual entity's data
        HamsterEntity hamster = this.menu.getHamsterEntity();
        if (hamster != null && !this.hasUnsavedName) {
            Component customName = hamster.getCustomName();
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
        HamsterEntity hamster = this.menu.getHamsterEntity();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && hamster != null
                && isArmorVisibilityToggleHovered(mouseX, mouseY)) {
            NetworkManager.sendToServer(
                    new UpdateHamsterArmorVisibilityPayload(
                            hamster.getId(), !hamster.isArmorVisible()));
            return true;
        }

        if (Configs.AHP_MAIN.enableGuiRenaming) {
            int boxX = this.leftPos + RENAME_BOX_X;
            int boxY = this.topPos + RENAME_BOX_Y;

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
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
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
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.isRenaming) {
            // Filter out unprintable/illegal characters
            if (StringUtil.isAllowedChatCharacter(chr)) {
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
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        context.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // --- Draw Hamster Entity ---
        int boxX = this.leftPos + 8;
        int boxY = this.topPos + 12;
        int boxWidth = 59 - 8;
        int boxHeight = 69 - 18;
        int size = 60;

        HamsterEntity hamster = this.menu.getHamsterEntity();
        if (hamster != null) {
            // Flag renderer to hide nameplate during this specific draw call
            HamsterRenderer.IS_RENDERING_IN_GUI.set(true);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        context,
                        boxX,
                        boxY,
                        boxX + boxWidth,
                        boxY + boxHeight,
                        size,
                        0.0625F,
                        (float)mouseX,
                        (float)mouseY,
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

        renderTooltip(context, mouseX, mouseY);
        renderArmorVisibilityToggle(context, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        // Only draw standard static title if custom dynamic renaming is disabled
        if (!Configs.AHP_MAIN.enableGuiRenaming) {
            context.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        }

        context.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        int labelColor = 4210752;
        drawCenteredLabel(context, Component.translatable("entity.adorablehamsterpets.hamster.inventory_left_cheek_title"), 52, 80, labelColor);
        drawCenteredLabel(context, Component.translatable("entity.adorablehamsterpets.hamster.inventory_right_cheek_title"), 124, 80, labelColor);
        drawCenteredLabel(context, Component.translatable("entity.adorablehamsterpets.hamster.inventory_bling_title"), 90, 29, labelColor);
        drawCenteredLabel(context, Component.translatable("entity.adorablehamsterpets.hamster.inventory_armor_title"), 142, 29, labelColor);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Renders the dynamic renaming GUI elements, handling scaling, horizontal centering,
     * the blinking cursor, hover states, and dynamic tooltips.
     */
    private void renderRenameBox(GuiGraphics context, int mouseX, int mouseY) {
        int boxX = this.leftPos + RENAME_BOX_X;
        int boxY = this.topPos + RENAME_BOX_Y;
        int boxWidth = RENAME_BOX_WIDTH;
        int boxHeight = RENAME_BOX_HEIGHT;
        boolean hovered = mouseX >= boxX
                && mouseX <= boxX + boxWidth
                && mouseY >= boxY
                && mouseY <= boxY + boxHeight
                && !isArmorVisibilityToggleHovered(mouseX, mouseY);

        // Base text is either custom name, or configured default if no name has been set
        String defaultName = Component.translatable(Configs.AHP_MAIN.useHampterName ? "entity.adorablehamsterpets.hampter" : "entity.adorablehamsterpets.hamster").getString();
        String baseText = this.currentName.isEmpty() && !this.isRenaming ? defaultName : this.currentName;

        // Append a blinking underscore if currently actively typing
        String displayText = baseText + (this.isRenaming && (Util.getMillis() / 500 % 2 == 0) ? "_" : "");

        int textWidth = this.font.width(displayText);
        int unscaledWidth = 8 + 3 + textWidth; // Icon (8) + Margin (3) + Text

        // Downscale before reaching the checkbox's reserved header space.
        float scale = Math.min(1.0f, boxWidth / (float) unscaledWidth);
        int scaledWidth = (int) (unscaledWidth * scale);

        // Calculate starting X to ensure always perfectly centered
        int startX = boxX + (boxWidth / 2) - (scaledWidth / 2);

        context.pose().pushPose();
        context.pose().translate(startX, boxY, 0);
        context.pose().scale(scale, scale, 1.0f);

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
        context.blit(PENCIL_ICON, iconX, 0, 0, 0, 8, 8, 8, 8);

        // Draw text
        context.drawString(this.font, displayText, textX, 0, 4210752, false);

        context.pose().popPose();

        // Render dynamic tooltips if hovered and not actively typing
        if (hovered && !this.isRenaming) {
            List<Component> tooltip = new ArrayList<>();
            if (Configs.AHP_UI.consumeNameTagForGuiRename && !hasNameTag()) {
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.rename.missing_tag").withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable("tooltip.adorablehamsterpets.rename").withStyle(ChatFormatting.GOLD));
                if (Configs.AHP_UI.consumeNameTagForGuiRename) {
                    tooltip.add(Component.translatable("tooltip.adorablehamsterpets.rename.consume").withStyle(ChatFormatting.GRAY));
                }
            }
            context.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Checks the player's inventory and the hamster's cheek pouches for a Name Tag.
     */
    private boolean hasNameTag() {
        if (this.minecraft == null || this.minecraft.player == null) return false;
        Player player = this.minecraft.player;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.NAME_TAG)) return true;
        }

        HamsterEntity hamster = this.menu.getHamsterEntity();
        if (hamster != null) {
            for (int i = 0; i < HamsterInventoryUtil.CHEEK_POUCH_SIZE; i++) {
                if (hamster.getItems().get(i).is(Items.NAME_TAG)) return true;
            }
        }
        return false;
    }

    private void renderArmorVisibilityToggle(
            GuiGraphics context, int mouseX, int mouseY) {
        HamsterEntity hamster = this.menu.getHamsterEntity();
        if (hamster == null) {
            return;
        }

        context.blit(
                ARMOR_VISIBILITY_CHECKBOX,
                this.leftPos + ARMOR_VISIBILITY_CHECKBOX_X,
                this.topPos + ARMOR_VISIBILITY_CHECKBOX_Y,
                0,
                0,
                ARMOR_VISIBILITY_CHECKBOX_SIZE,
                ARMOR_VISIBILITY_CHECKBOX_SIZE,
                ARMOR_VISIBILITY_CHECKBOX_SIZE,
                ARMOR_VISIBILITY_CHECKBOX_SIZE);

        if (!hamster.isArmorVisible()) {
            context.blit(
                    ARMOR_VISIBILITY_CHECK_MARK,
                    this.leftPos + ARMOR_VISIBILITY_TOGGLE_X,
                    this.topPos + ARMOR_VISIBILITY_TOGGLE_Y,
                    0,
                    0,
                    ARMOR_VISIBILITY_TOGGLE_SIZE,
                    ARMOR_VISIBILITY_TOGGLE_SIZE,
                    ARMOR_VISIBILITY_TOGGLE_SIZE,
                    ARMOR_VISIBILITY_TOGGLE_SIZE);
        }

        if (isArmorVisibilityToggleHovered(mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            String actionKey = hamster.isArmorVisible()
                    ? "tooltip.adorablehamsterpets.armor_visibility.hide"
                    : "tooltip.adorablehamsterpets.armor_visibility.show";
            tooltip.add(Component.translatable(actionKey).withStyle(ChatFormatting.GOLD));
            String globalKey = Configs.AHP_MAIN.enableArmorVisuals
                    ? "tooltip.adorablehamsterpets.armor_visibility.global_override"
                    : "tooltip.adorablehamsterpets.armor_visibility.globally_disabled";
            tooltip.add(Component.translatable(globalKey).withStyle(ChatFormatting.GRAY));
            context.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private boolean isArmorVisibilityToggleHovered(double mouseX, double mouseY) {
        int toggleX = this.leftPos + ARMOR_VISIBILITY_TOGGLE_X;
        int toggleY = this.topPos + ARMOR_VISIBILITY_TOGGLE_Y;
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
     * Transmits the RenameHamsterPayload to the server if changes were actually made.
     */
    private void sendRenamePacket() {
        if (this.hasUnsavedName && !this.currentName.equals(this.initialName)) {
            HamsterEntity hamster = this.menu.getHamsterEntity();
            if (hamster != null) {
                NetworkManager.sendToServer(new RenameHamsterPayload(hamster.getId(), this.currentName.trim()));
                this.initialName = this.currentName; // Update initial state to prevent duplicate packets
                this.hasUnsavedName = false;
            }
        }
    }

    private void drawCenteredLabel(GuiGraphics context, Component text, int centerX, int y, int color) {
        int width = this.font.width(text);
        context.drawString(this.font, text, centerX - (width / 2), y, color, false);
    }
}
