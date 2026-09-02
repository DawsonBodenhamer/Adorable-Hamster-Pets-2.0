package net.dawson.adorablehamsterpets.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    // Apply the translation right before shadow is drawn
    @Inject(method = "renderShadow", at = @At("HEAD"))
    private static void adorablehamsterpets$shadowOffsetStart(PoseStack matrices, MultiBufferSource vertexConsumers, Entity entity, float opacity, float tickDelta, LevelReader world, float radius, CallbackInfo ci) {
        if (entity instanceof HamsterEntity hamster) {
            double offset = hamster.getRollShadowOffset(tickDelta);
            if (offset > 0.0) {
                // Calculate local backward direction based on body yaw
                float bodyYaw = Mth.lerp(tickDelta, hamster.yBodyRotO, hamster.yBodyRot);
                float yawRadians = (float) Math.toRadians(bodyYaw);

                double xOffset = Math.sin(yawRadians) * offset;
                double zOffset = -Math.cos(yawRadians) * offset;

                matrices.translate(xOffset, 0, zOffset);
            }
        }
    }

    // Clean up matrix right after shadow finishes drawing
    @Inject(method = "renderShadow", at = @At("RETURN"))
    private static void adorablehamsterpets$shadowOffsetEnd(PoseStack matrices, MultiBufferSource vertexConsumers, Entity entity, float opacity, float tickDelta, LevelReader world, float radius, CallbackInfo ci) {
        if (entity instanceof HamsterEntity hamster) {
            double offset = hamster.getRollShadowOffset(tickDelta);
            if (offset > 0.0) {
                float bodyYaw = Mth.lerp(tickDelta, hamster.yBodyRotO, hamster.yBodyRot);
                float yawRadians = (float) Math.toRadians(bodyYaw);

                double xOffset = Math.sin(yawRadians) * offset;
                double zOffset = -Math.cos(yawRadians) * offset;

                // Inverse translation
                matrices.translate(-xOffset, 0, -zOffset);
            }
        }
    }
}