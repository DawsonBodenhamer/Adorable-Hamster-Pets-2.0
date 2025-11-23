package net.dawson.adorablehamsterpets.mixin.client.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;

@Mixin(value = GuiButtonEntry.class, remap = false)
public interface GuiButtonEntryAccessor {
    @Invoker("getColor")
    int adorablehamsterpets$invokeGetColor();
}