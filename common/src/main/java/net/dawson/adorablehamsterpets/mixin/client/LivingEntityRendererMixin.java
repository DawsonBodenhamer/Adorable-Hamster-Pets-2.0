package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to cancel vanilla passenger rendering when the passenger is riding a hamster.
 * Prevents double-rendering, allowing custom bone-locked rendering in HamsterRenderer to work properly.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$cancelDuplicateRender(LivingEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        // 1. Check if this entity is riding a Hamster
        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof HamsterEntity) {

            // 2. Check if we are currently inside the HamsterRenderer's specific passenger render call.
            // If TRUE: This call came from HamsterRenderer.renderFinal -> renderPassengersForBone. Allow it.
            // If FALSE: This is the vanilla main render loop attempting to draw the passenger normally. Cancel it.
            if (!HamsterRenderer.IS_RENDERING_PASSENGER.get()) {
                ci.cancel();
            }
        }
    }
}