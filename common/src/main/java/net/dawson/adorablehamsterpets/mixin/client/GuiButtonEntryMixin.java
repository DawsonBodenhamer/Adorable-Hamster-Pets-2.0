package net.dawson.adorablehamsterpets.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.mixin.client.accessor.GuiButtonEntryAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;

import java.util.List;

@Mixin(value = GuiButtonEntry.class)
public abstract class GuiButtonEntryMixin extends ButtonWidget {

    @Shadow(remap = false) @Final private GuiBook parent;
    @Shadow(remap = false) @Final private BookEntry entry;
    @Shadow(remap = false) private float timeHovered;

    // Required constructor Mixin to compile
    public GuiButtonEntryMixin(int x, int y, int width, int height, Text message, PressAction onPress, NarrationSupplier narrationSupplier) {
        super(x, y, width, height, message, onPress, narrationSupplier);
    }

    /**
     * Helper method to determine if this button belongs to the Hamster Tips guide book.
     * @return True if the parent book is the hamster guide, false otherwise.
     */
    private boolean isHamsterBook() {
        return this.parent.book != null && this.parent.book.id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
    }

    /**
     * Injects into the start of the renderButton method to replace its logic, but ONLY for the Hamster Tips guide book.
     * This new implementation draws the entry title with text wrapping.
     */
    @Inject(method = "renderButton", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$renderWrappedWidget(DrawContext graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // --- SAFETY CHECK ---
        // If this button is not the Hamster Tips guide book, do nothing and let the original method run.
        if (!isHamsterBook()) {
            return;
        }

        ci.cancel(); // Replacing the entire method.

        if (this.active) {
            // --- 1. Replicate Hover Animation Logic ---
            if (this.isSelected()) {
                this.timeHovered = Math.min(5.0F, this.timeHovered + ClientTicker.delta);
            } else {
                this.timeHovered = Math.max(0.0F, this.timeHovered - ClientTicker.delta);
            }
            float time = Math.max(0.0F, Math.min(5.0F, this.timeHovered + (this.isSelected() ? partialTicks : -partialTicks)));
            float widthFract = time / 5.0F;

            // --- 2. Replicate Background, Icon, and Lock Rendering ---
            boolean locked = this.entry.isLocked();
            graphics.getMatrices().scale(0.5F, 0.5F, 0.5F);
            graphics.fill(this.getX() * 2, this.getY() * 2, (this.getX() + (int)((float)this.width * widthFract)) * 2, (this.getY() + this.height) * 2, 570425344);
            RenderSystem.enableBlend();
            if (locked) {
                graphics.setShaderColor(1.0F, 1.0F, 1.0F, 0.7F);
                GuiBook.drawLock(graphics, this.parent.book, this.getX() * 2 + 2, this.getY() * 2 + 2);
            } else {
                this.entry.getIcon().render(graphics, this.getX() * 2 + 2, this.getY() * 2 + 2);
            }
            graphics.getMatrices().scale(2.0F, 2.0F, 2.0F);

            // --- 3. Prepare Text for Wrapping ---
            MutableText name = locked
                    ? Text.translatable("patchouli.gui.lexicon.locked")
                    : this.entry.getName().copy();
            if (!locked && this.entry.isPriority()) {
                name.formatted(Formatting.ITALIC);
            }
            name.fillStyle(this.entry.getBook().getFontStyle());

            // --- 4. Wrap and Render Text ---
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int availableWidth = this.width - 12; // Width of button minus icon/padding
            List<OrderedText> lines = textRenderer.wrapLines(name, availableWidth);

            for (int i = 0; i < lines.size(); i++) {
                OrderedText line = lines.get(i);
                // Calculate Y position for each line, adding a small top margin
                int lineY = this.getY() + 1 + (i * 10);
                graphics.drawText(textRenderer, line, this.getX() + 12, lineY, ((GuiButtonEntryAccessor) this).adorablehamsterpets$invokeGetColor(), false);
            }

            // --- 5. Replicate Read-State Marking ---
            if (!this.entry.isLocked()) {
                GuiBook.drawMarking(graphics, this.parent.book, this.getX() + this.width - 5, this.getY() + 1, this.entry.hashCode(), this.entry.getReadState());
            }
        }
    }
}