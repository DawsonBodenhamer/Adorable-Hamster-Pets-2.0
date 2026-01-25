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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Shadows
     * ────────────────────────────────────────────────────────────────────────────*/

    @Shadow @Final protected List<ButtonWidget> entryButtons;
    @Shadow @Final private List<BookEntry> visibleEntries;
    @Shadow private List<BookEntry> allEntries;
    @Shadow private TextFieldWidget searchField;

    @Shadow protected abstract void addSubcategoryButtons();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public GuiBookEntryListMixin(Book book, Text title) {
        super(book, title);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Injections
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Completely replaces the entry button generation logic for the Hamster Tips guide book.
     * This implementation simulates text wrapping to dynamically calculate page breaks,
     * allowing for entry titles that span multiple lines without overflowing the GUI.
     */
    @Inject(method = "buildEntryButtons", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$buildWrappedEntryButtons(CallbackInfo ci) {
        // --- 1. Validation & Setup ---
        if (!isHamsterBook()) return;

        ci.cancel();
        GuiBookAccessor accessor = (GuiBookAccessor) this;

        // --- 2. Filter Entries by Query ---
        this.removeDrawablesIn(this.entryButtons);
        this.entryButtons.clear();
        this.visibleEntries.clear();

        String query = this.searchField.getText().toLowerCase();
        Stream<BookEntry> stream = this.allEntries.stream().filter((e) -> e.isFoundByQuery(query));
        Objects.requireNonNull(this.visibleEntries);
        stream.forEach(this.visibleEntries::add);

        // --- 3. Dynamic Page Layout Simulation ---
        // Track the starting entry index for every single page column
        List<Integer> pageStartIndices = new ArrayList<>();
        pageStartIndices.add(0);

        if (!this.visibleEntries.isEmpty()) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int availableWidth = 116 - 12; // Button width minus padding

            // Define height limits (Top Y + Limit = Bottom Y ~156)
            int firstPageHeightLimit = 156 - 38; // Patchouli default was 168 but I like more padding
            int subsequentPageHeightLimit = 156 - 18;

            int currentEntryIndex = 0;
            boolean isFirstPage = true;

            while (currentEntryIndex < this.visibleEntries.size()) {
                int pageHeightLimit = isFirstPage ? firstPageHeightLimit : subsequentPageHeightLimit;
                int currentY = 0;

                // Simulate filling the current page column
                while (currentEntryIndex < this.visibleEntries.size()) {
                    BookEntry entry = this.visibleEntries.get(currentEntryIndex);
                    MutableText name = entry.isLocked() ? Text.translatable("patchouli.gui.lexicon.locked") : entry.getName().copy();

                    // Calculate wrapped height
                    int buttonHeight = textRenderer.wrapLines(name, availableWidth).size() * 10;

                    // Break if adding this button overflows the page
                    if (currentY > 0 && currentY + buttonHeight > pageHeightLimit) {
                        break;
                    }

                    currentY += buttonHeight + 1; // +1 for spacing
                    currentEntryIndex++;
                }

                // Mark the start of the next page if entries remain
                if (currentEntryIndex < this.visibleEntries.size()) {
                    pageStartIndices.add(currentEntryIndex);
                }

                isFirstPage = false;
            }
        }

        // --- 4. Configure Spreads ---
        int numPages = pageStartIndices.size();
        // Ceiling division to convert total pages to total spreads (2 pages per spread)
        accessor.adorablehamsterpets$setMaxSpreads(1 + (int) Math.ceil((numPages - 1) / 2.0));

        if (accessor.adorablehamsterpets$getMaxSpreads() < 1) {
            accessor.adorablehamsterpets$setMaxSpreads(1);
        }

        // Clamp current spread index if page count reduced
        if (accessor.adorablehamsterpets$getSpread() >= accessor.adorablehamsterpets$getMaxSpreads()) {
            accessor.adorablehamsterpets$setSpread(Math.max(0, accessor.adorablehamsterpets$getMaxSpreads() - 1));
        }

        // --- 5. Render Buttons ---
        if (accessor.adorablehamsterpets$getSpread() == 0) {
            // Spread 0: Right Page Only
            int start = pageStartIndices.isEmpty() ? 0 : pageStartIndices.get(0);
            int end = numPages > 1 ? pageStartIndices.get(1) : this.visibleEntries.size();

            addWrappedEntryButtons(141, 38, start, end - start);
            this.addSubcategoryButtons();
        } else {
            // Subsequent Spreads: Left and Right Pages
            int leftPageIndex = accessor.adorablehamsterpets$getSpread() * 2 - 1;
            int rightPageIndex = accessor.adorablehamsterpets$getSpread() * 2;

            int leftStartIndex = numPages > leftPageIndex ? pageStartIndices.get(leftPageIndex) : this.visibleEntries.size();
            int leftEndIndex = numPages > rightPageIndex ? pageStartIndices.get(rightPageIndex) : this.visibleEntries.size();
            int leftCount = leftEndIndex - leftStartIndex;

            // Right page starts where left ended
            int rightStartIndex = leftEndIndex;
            int rightEndIndex = (numPages > rightPageIndex + 1) ? pageStartIndices.get(rightPageIndex + 1) : this.visibleEntries.size();
            int rightCount = rightEndIndex - rightStartIndex;

            if (leftCount > 0) {
                addWrappedEntryButtons(15, 18, leftStartIndex, leftCount);
            }
            if (rightCount > 0) {
                addWrappedEntryButtons(141, 18, rightStartIndex, rightCount);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean isHamsterBook() {
        return this.book != null && this.book.id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
    }

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

            // Calculate height dynamically
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