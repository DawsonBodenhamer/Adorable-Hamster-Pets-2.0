package net.dawson.adorablehamsterpets.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.mixin.accessor.ScreenWidgetAdder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookLanding;
import vazkii.patchouli.client.book.gui.button.GuiButtonCategory;
import vazkii.patchouli.common.book.Book;

/**
 * Mixin to customize the Patchouli {@link GuiBookLanding} screen specifically for the
 * Adorable Hamster Pets guide book. This class implements two key visual overrides:
 * <p>
 * 1.  <b>Custom Header Rendering:</b> It replaces the default header drawing logic to render a
 *     custom-positioned, multi-line wrapped subtitle that would otherwise overflow onto the opposite page.
 * 2.  <b>Centered Category Grid:</b> It intercepts the category button layout logic to
 *     recalculate icon positions, ensuring that the final row of the category grid is
 *     centered on the page for a more balanced appearance.
 * <p>
 * All modifications are gated behind a check to ensure they only apply to the Hamster Tips guide book.
 */
@Mixin(value = GuiBookLanding.class, remap = false)
public abstract class GuiBookLandingMixin extends GuiBook {

    // This constructor is required by the compiler because GuiBook has a constructor.
    public GuiBookLandingMixin(Book book, Text title) {
        super(book, title);
    }

    /**
     * A simple helper method to check if the currently rendered book is the
     * Adorable Hamster Pets guide book. All mixin modifications will be gated
     * behind this check to prevent them from affecting other mods' books.
     *
     * @return True if the current book is the hamster guide, false otherwise.
     */
    private boolean isHamsterBook() {
        // The 'book' field is inherited from the parent GuiBook class.
        return this.book != null && this.book.id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
    }

    /**
     * Computes the total number of category buttons (visible categories + index).
     * This is used to calculate the centered grid layout.
     */
    private int getTotalIcons() {
        // Do not alter pamphlet (non-category) mode
        if (this.book.getContents().pamphletCategory != null) {
            return 0;
        }
        int count = 0;
        for (BookCategory cat : this.book.getContents().categories.values()) {
            if (cat.getParentCategory() == null && !cat.shouldHide()) {
                count++;
            }
        }
        // +1 for the index button
        return count + 1;
    }

    /**
     * Injects at the beginning of the drawHeader method to completely replace its rendering logic
     * for the Adorable Hamster Pets guide book. This allows for custom positioning and text wrapping
     * of the title and subtitle without affecting other Patchouli books.
     *
     * @param graphics The DrawContext for rendering.
     * @param ci       The CallbackInfo, used to cancel the original method.
     */
    @Inject(method = "drawHeader", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$onDrawHeader(DrawContext graphics, CallbackInfo ci) {
        // --- SAFETY CHECK ---
        // If this is not the Hamster Tips guide book, do nothing and let the original method run.
        if (!isHamsterBook()) {
            return;
        }

        // --- 1. Set Render Color ---
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // --- 2. Render the Header Ribbon Texture ---
        // We use a taller texture to fit the wrapped subtitle.
        int ribbonX = -8;
        int ribbonY = 12;
        int ribbonU = 0;
        int ribbonV = 180;
        int ribbonWidth = 140;
        int ribbonHeight = 43; // MODIFIED: Was 31
        drawFromTexture(graphics, this.book, ribbonX, ribbonY, ribbonU, ribbonV, ribbonWidth, ribbonHeight);

        // --- 3. Render the Main Book Title ---
        int titleColor = this.book.nameplateColor;
        Text titleText = this.book.getBookItem().getName();
        int titleX = 13 + 7; // MODIFIED: Shifted right by 7
        int titleY = 16;
        graphics.drawText(MinecraftClient.getInstance().textRenderer, titleText, titleX, titleY, titleColor, false);

        // --- 4. Render the Wrapped Subtitle ---
        Text subtitleText = this.book.getSubtitle().fillStyle(this.book.getFontStyle());
        int subtitleX = 24 - 5; // MODIFIED: Shifted left by 5
        int subtitleY = 24 + 2; // MODIFIED: Shifted down by 2
        int wrapWidth = 100;    // The available width for the text
        graphics.drawTextWrapped(MinecraftClient.getInstance().textRenderer, subtitleText, subtitleX, subtitleY, wrapWidth, titleColor);

        // --- 5. Cancel the Original Method ---
        ci.cancel();
    }

    /**
     * Intercepts category button placement to center the last row.
     */
    @Inject(method = "addCategoryButton", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$onAddCategoryButton(int i, BookCategory category, CallbackInfo ci) {
        // Only affect the Hamster guide and normal category pages
        if (!isHamsterBook() || this.book.getContents().pamphletCategory != null) {
            return;
        }

        int total = getTotalIcons();
        if (total == 0) {
            return;
        }

        int columns = 4;
        int row = i / columns;
        int lastRow = (total - 1) / columns;
        int rowItems = (row == lastRow) ? (total - lastRow * columns) : columns;
        int shift = (columns - rowItems) * 12; // 12px = half of a column (24px)

        int x = RIGHT_PAGE_X + 10 + shift + (i % columns) * 24;
        int y = TOP_PADDING + 25 + row * 24;

        // Create the appropriate button
        GuiButtonCategory button;
        GuiBookLanding self = (GuiBookLanding) (Object) this;
        if (category == null) {
            button = new GuiButtonCategory(this, x, y, this.book.getIcon(), Text.translatable("patchouli.gui.lexicon.index"), self::handleButtonIndex);
        } else {
            button = new GuiButtonCategory(this, x, y, category, self::handleButtonCategory);
        }
        // Use ScreenWidgetAdder accessor to add the widget for cross-loader compatibility
        ((ScreenWidgetAdder)(Object)this).adorablehamsterpets$addWidget(button);

        // Cancel original positioning
        ci.cancel();
    }
}