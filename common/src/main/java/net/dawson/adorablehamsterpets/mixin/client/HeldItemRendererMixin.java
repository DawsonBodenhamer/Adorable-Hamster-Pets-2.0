package net.dawson.adorablehamsterpets.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the Acorn Ring in the first-person off-hand. 26.2: renderFirstPersonItem became submitArmWithItem. */
@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$hideAcornRingInOffhand(AbstractClientPlayer player, float tickDelta, float pitch,
                                                            InteractionHand hand, float swingProgress, ItemStack stack,
                                                            float equipProgress, PoseStack matrices,
                                                            SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (hand == InteractionHand.OFF_HAND && stack.is(ModItems.ACORN_RING.get())) {
            ci.cancel();
        }
    }
}
