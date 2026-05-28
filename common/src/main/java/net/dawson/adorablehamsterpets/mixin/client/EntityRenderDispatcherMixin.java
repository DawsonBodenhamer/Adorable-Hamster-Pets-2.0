package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    // Apply the translation right before shadow is drawn
    @Inject(method = "renderShadow", at = @At("HEAD"))
    private static void adorablehamsterpets$shadowOffsetStart(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo ci) {
        if (entity instanceof HamsterEntity hamster) {
            double offset = hamster.getRollShadowOffset(tickDelta);
            if (offset > 0.0) {
                // Calculate local backward direction based on body yaw
                float bodyYaw = MathHelper.lerp(tickDelta, hamster.prevBodyYaw, hamster.bodyYaw);
                float yawRadians = (float) Math.toRadians(bodyYaw);

                double xOffset = Math.sin(yawRadians) * offset;
                double zOffset = -Math.cos(yawRadians) * offset;

                matrices.translate(xOffset, 0, zOffset);
            }
        }
    }

    // Clean up matrix right after shadow finishes drawing
    @Inject(method = "renderShadow", at = @At("RETURN"))
    private static void adorablehamsterpets$shadowOffsetEnd(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo ci) {
        if (entity instanceof HamsterEntity hamster) {
            double offset = hamster.getRollShadowOffset(tickDelta);
            if (offset > 0.0) {
                float bodyYaw = MathHelper.lerp(tickDelta, hamster.prevBodyYaw, hamster.bodyYaw);
                float yawRadians = (float) Math.toRadians(bodyYaw);

                double xOffset = Math.sin(yawRadians) * offset;
                double zOffset = -Math.cos(yawRadians) * offset;

                // Inverse translation
                matrices.translate(-xOffset, 0, -zOffset);
            }
        }
    }
}