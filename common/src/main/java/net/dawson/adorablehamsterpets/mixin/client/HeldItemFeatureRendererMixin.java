package net.dawson.adorablehamsterpets.mixin.client;

import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the Acorn Ring when it sits in a player's off-hand in third person.
 *
 * <p>26.2 port: the layer no longer receives the entity; it draws from an
 * {@link ArmedEntityRenderState}. The player check becomes an
 * {@link AvatarRenderState} check and the main arm is read off the state.
 */
@Mixin(ItemInHandLayer.class)
public abstract class HeldItemFeatureRendererMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$hideAcornRingInOffhand(ArmedEntityRenderState state, ItemStackRenderState itemRenderState,
                                                            ItemStack stack, HumanoidArm arm, PoseStack poseStack,
                                                            SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (state instanceof AvatarRenderState && arm != state.mainArm && stack.is(ModItems.ACORN_RING.get())) {
            ci.cancel();
        }
    }
}
