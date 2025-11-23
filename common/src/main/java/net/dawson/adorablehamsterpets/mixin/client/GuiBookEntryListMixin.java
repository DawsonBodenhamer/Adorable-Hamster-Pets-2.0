package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.mixin.accessor.ScreenWidgetAdder;
import net.dawson.adorablehamsterpets.mixin.client.accessor.GuiBookAccessor;
import net.dawson.adorablehamsterpets.mixin.client.accessor.ClickableWidgetAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntryList;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;
import vazkii.patchouli.common.book.Book;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Mixin(value = GuiBookEntryList.class, remap = false)
public abstract class GuiBookEntryListMixin extends GuiBook {

    // Shadowed fields from target class
    @Shadow @Final protected List<ButtonWidget> entryButtons;
    @Shadow @Final private List<BookEntry> visibleEntries;
    @Shadow private List<BookEntry> allEntries;
    @Shadow private TextFieldWidget searchField;

    // Abstract methods need to be able to call
    @Shadow protected abstract void addSubcategoryButtons();

    // Required constructor for the Mixin to compile
    public GuiBookEntryListMixin(Book book, Text title) {
        super(book, title);
    }

    /**
     * Helper method to check if the currently rendered book is Hamster Tips guide book.
     */
    private boolean isHamsterBook() {
        return this.book != null && this.book.id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
    }

    /**
     * Injects into the start of buildEntryButtons to completely replace its logic for Hamster Tips guide book.
     * This new implementation dynamically calculates page breaks based on the actual rendered height of each entry.
     */
    @Inject(method = "buildEntryButtons", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$buildWrappedEntryButtons(CallbackInfo ci) {
        if (!isHamsterBook()) {
            // --- SAFETY CHECK ---
            return; // If not the Hamster Tips guide book, let the original method run.
        }

        ci.cancel(); // Replacing the entire method.

        GuiBookAccessor accessor = (GuiBookAccessor) this;

        // --- 1. Replicate Initial Setup from Original Method ---
        this.removeDrawablesIn(this.entryButtons);
        this.entryButtons.clear();
        this.visibleEntries.clear();
        String query = this.searchField.getText().toLowerCase();
        Stream<BookEntry> stream = this.allEntries.stream().filter((e) -> e.isFoundByQuery(query));
        Objects.requireNonNull(this.visibleEntries);
        stream.forEach(this.visibleEntries::add);

        // --- 2. Dynamic Page Layout Simulation ---
        List<Integer> pageStartIndices = new ArrayList<>();
        if (!this.visibleEntries.isEmpty()) {
            pageStartIndices.add(0); // Page 0 always starts at entry 0

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int availableWidth = 116 - 12;
            int firstPageHeightLimit = 156 - 38; // Page height limit was 168. Reduced to create more bottom padding.
            int subsequentPageHeightLimit = 156 - 18;

            int currentEntryIndex = 0;
            boolean isFirstPage = true;

            while (currentEntryIndex < this.visibleEntries.size()) {
                int pageHeightLimit = isFirstPage ? firstPageHeightLimit : subsequentPageHeightLimit;
                int currentY = 0;

                // Left column
                while (currentEntryIndex < this.visibleEntries.size()) {
                    BookEntry entry = this.visibleEntries.get(currentEntryIndex);
                    MutableText name = entry.isLocked() ? Text.translatable("patchouli.gui.lexicon.locked") : entry.getName().copy();
                    int buttonHeight = textRenderer.wrapLines(name, availableWidth).size() * 10;

                    // This entry doesn't fit; starts the next page.
                    if (currentY + buttonHeight > pageHeightLimit) break;

                    currentY += buttonHeight + 1;
                    currentEntryIndex++;
                }

                if (isFirstPage) {
                    isFirstPage = false;
                } else {
                    // Right column (for spreads > 0)
                    currentY = 0;
                    while (currentEntryIndex < this.visibleEntries.size()) {
                        BookEntry entry = this.visibleEntries.get(currentEntryIndex);
                        MutableText name = entry.isLocked() ? Text.translatable("patchouli.gui.lexicon.locked") : entry.getName().copy();
                        int buttonHeight = textRenderer.wrapLines(name, availableWidth).size() * 10;

                        if (currentY + buttonHeight > pageHeightLimit) break;

                        currentY += buttonHeight + 1;
                        currentEntryIndex++;
                    }
                }

                if (currentEntryIndex < this.visibleEntries.size()) {
                    pageStartIndices.add(currentEntryIndex);
                }
            }
        }

        // --- 3. Calculate maxSpreads and Validate Current Spread ---
        int numPages = pageStartIndices.size();
        accessor.adorablehamsterpets$setMaxSpreads(1 + (int) Math.ceil((numPages - 1) / 2.0));
        if (accessor.adorablehamsterpets$getMaxSpreads() < 1) {
            accessor.adorablehamsterpets$setMaxSpreads(1);
        }

        // Ensure the current spread is not out of bounds if the number of pages changed
        if (accessor.adorablehamsterpets$getSpread() >= accessor.adorablehamsterpets$getMaxSpreads()) {
            accessor.adorablehamsterpets$setSpread(Math.max(0, accessor.adorablehamsterpets$getMaxSpreads() - 1));
        }

        // --- 4. Draw Buttons for the Current Page ---
        if (accessor.adorablehamsterpets$getSpread() == 0) {
            int start = pageStartIndices.isEmpty() ? 0 : pageStartIndices.get(0);
            int end = numPages > 1 ? pageStartIndices.get(1) : this.visibleEntries.size();
            addWrappedEntryButtons(141, 38, start, end - start);
            this.addSubcategoryButtons();
        } else {
            int leftPageIndex = accessor.adorablehamsterpets$getSpread() * 2 - 1;
            int rightPageIndex = accessor.adorablehamsterpets$getSpread() * 2;

            int leftStartIndex = numPages > leftPageIndex ? pageStartIndices.get(leftPageIndex) : this.visibleEntries.size();
            int rightStartIndex = numPages > rightPageIndex ? pageStartIndices.get(rightPageIndex) : this.visibleEntries.size();
            int leftCount = rightStartIndex - leftStartIndex;

            addWrappedEntryButtons(15, 18, leftStartIndex, leftCount);
            addWrappedEntryButtons(141, 18, rightStartIndex, this.visibleEntries.size() - rightStartIndex);
        }
    }

    /**
     * A helper method to add entry buttons with dynamic heights.
     */
    private void addWrappedEntryButtons(int x, int y, int start, int count) {
        GuiBookEntryList self = (GuiBookEntryList) (Object) this;
        GuiBookAccessor accessor = (GuiBookAccessor) self;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int bookLeft = accessor.adorablehamsterpets$getBookLeft();
        int bookTop = accessor.adorablehamsterpets$getBookTop();
        int availableWidth = 116 - 12;
        int yOffset = y;

        for (int i = 0; i < count; i++) {
            int entryIndex = start + i;
            if (entryIndex >= this.visibleEntries.size()) break;

            BookEntry entry = this.visibleEntries.get(entryIndex);
            MutableText name = entry.isLocked() ? Text.translatable("patchouli.gui.lexicon.locked") : entry.getName().copy();
            int buttonHeight = textRenderer.wrapLines(name, availableWidth).size() * 10;

            ButtonWidget button = new GuiButtonEntry(self, bookLeft + x, bookTop + yOffset, entry, self::handleButtonEntry);
            // Use accessor to set the height on 1.20.1
            ((ClickableWidgetAccessor) button).adorablehamsterpets$setHeight(buttonHeight);

            // Use ScreenWidgetAdder accessor to add the widget for cross-loader compatibility
            ((ScreenWidgetAdder)(Object)self).adorablehamsterpets$addWidget(button);
            this.entryButtons.add(button);

            yOffset += buttonHeight + 1;
        }
    }
}