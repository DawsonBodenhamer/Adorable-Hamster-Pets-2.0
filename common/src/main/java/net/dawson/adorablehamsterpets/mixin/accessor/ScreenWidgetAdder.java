package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Provides a cross-loader way to call Screen#addDrawableChild (Fabric/Yarn) or
 * Screen#addRenderableWidget (NeoForge/Mojang). The method name is remapped by
 * Architectury when building for each loader.
 */
@Mixin(Screen.class)
public interface ScreenWidgetAdder {
    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T adorablehamsterpets$addWidget(T widget);
}