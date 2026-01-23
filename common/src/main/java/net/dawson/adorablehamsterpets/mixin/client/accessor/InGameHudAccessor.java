package net.dawson.adorablehamsterpets.mixin.client.accessor;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
    @Accessor("overlayRemaining")
    void adorablehamsterpets$setOverlayRemaining(int value);
}