package net.dawson.adorablehamsterpets.mixin.client.accessor;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface InGameHudAccessor {
    @Accessor("overlayMessageTime")
    void adorablehamsterpets$setOverlayRemaining(int value);
}