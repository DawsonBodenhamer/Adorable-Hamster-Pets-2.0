package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Provides a cross-loader way to call Screen#addDrawableChild (Fabric/Yarn) or
 * Screen#addRenderableWidget (NeoForge/Mojang). The method name is remapped by
 * Architectury when building for each loader.
 */
@Mixin(Screen.class)
public interface ScreenWidgetAdder {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T adorablehamsterpets$addWidget(T widget);
}