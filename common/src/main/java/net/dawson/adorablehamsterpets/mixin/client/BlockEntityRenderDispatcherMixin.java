package net.dawson.adorablehamsterpets.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dawson.adorablehamsterpets.client.render.BlockJiggleManager;
import net.dawson.adorablehamsterpets.client.render.BlockJiggleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wobbles a block entity's model while its block is "jiggling".
 *
 * <p>26.2 port: the dispatcher submits render states rather than rendering
 * block entities directly, so the pose is pushed and popped around
 * {@code submit}. The position comes off the state; the tick delta is not a
 * parameter any more and is read from the client's delta tracker instead.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "submit", at = @At("HEAD"))
    private <S extends BlockEntityRenderState> void adorablehamsterpets$pushJiggle(S state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (BlockJiggleManager.INSTANCE.hasJiggle(state.blockPos.asLong())) {
            Minecraft client = Minecraft.getInstance();
            matrices.pushPose();
            BlockJiggleRenderer.applyJiggleTransform(matrices, state.blockPos,
                    client.getDeltaTracker().getGameTimeDeltaPartialTick(true),
                    client.level != null ? client.level.getGameTime() : 0L);
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private <S extends BlockEntityRenderState> void adorablehamsterpets$popJiggle(S state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (BlockJiggleManager.INSTANCE.hasJiggle(state.blockPos.asLong())) {
            matrices.popPose();
        }
    }
}
