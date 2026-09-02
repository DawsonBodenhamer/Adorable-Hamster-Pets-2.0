package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.client.shoulder.ShoulderHamsterExtractor;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fills the shoulder-hamster data while the player entity is still in reach. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void adorablehamsterpets$extractShoulderHamsters(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ShoulderHamsterExtractor.extract(avatar, state, partialTick);
    }
}
