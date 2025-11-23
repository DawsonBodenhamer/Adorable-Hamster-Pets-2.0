package net.dawson.adorablehamsterpets.integration.patchouli;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.function.UnaryOperator;

/**
 * Draws a clickable “Open Config” label in a Patchouli page and uses Fzzy‑Config
 * to open the Adorable Hamster Pets config.
 */
public class OpenConfigComponent implements ICustomComponent {
    private static final Text LABEL = Text.translatable(
            "book.adorablehamsterpets.entry.config_heaven.page2.link_text");

    private transient ButtonWidget button; // created on first display
    private transient boolean added; // tracks whether the widget is in the GUI
    private int pageIndex;

    // Patchouli gives the page index here, so storing it for addWidget(...)
    @Override
    public void build(int componentX, int componentY, int pageNum) {
        this.pageIndex = pageNum;
        this.added = false;
    }

    @Override
    public void onDisplayed(IComponentRenderContext ctx) {
        // When the page becomes visible, force a fresh widget next frame.
        button = null;
        added = false;
    }

    @Override
    public void render(DrawContext gfx, IComponentRenderContext ctx,
                       float pt, int mouseX, int mouseY) {
        if (button == null) {
            button = ButtonWidget.builder(
                            LABEL,
                            b -> {
                                added = false;
                                // Open Config
                                ConfigApiJava.INSTANCE.openScreen("adorablehamsterpets");
                            }
                    )
                    // Template needs a slightly lower baseline than PageText’s 121
                    .position(8, 136)
                    .size(100, 20)
                    .build();
        }
        if (!added) {
            ctx.addWidget(button, pageIndex); // anchor to the correct page
            added = true;
        }
    }

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup,
                                     RegistryWrapper.WrapperLookup registries) {
        // Not using Patchouli template variables here, so nothing to do.
    }
}