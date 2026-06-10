package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.mixin.client.accessor.InGameHudAccessor;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "setOverlayMessage", at = @At("TAIL"))
    private void adorablehamsterpets$modifyDuration(Text message, boolean tinted, CallbackInfo ci) {
        // Overwrite vanilla at the tail with configured value
        int customDuration = Configs.AHP_UI.actionBarDuration.get();

        // Only modify if custom duration is different from vanilla's default, otherwise respect vanilla/other mods
        if (customDuration != 60) {
            ((InGameHudAccessor) this).adorablehamsterpets$setOverlayRemaining(customDuration);
        }
    }
}