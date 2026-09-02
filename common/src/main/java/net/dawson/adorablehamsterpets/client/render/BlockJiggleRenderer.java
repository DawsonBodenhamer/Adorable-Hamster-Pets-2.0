package net.dawson.adorablehamsterpets.client.render;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.LightLayer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockJiggleRenderer {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final float AMPLITUDE = 0.05f;            // Translation distance in blocks
    private static final float ROTATION_AMPLITUDE = 4.0f;    // Rotation in degrees
    private static final float OSCILLATION_CYCLES = 6.0f;    // How many wiggles in 20 ticks

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    // 26.2 port: the world-space jiggle pass that used to live here drew blocks
    // through BakedModel/BlockRenderDispatcher/MultiBufferSource, all removed in
    // 26.2 -- and nothing registered it, so it never ran. Only the transform
    // helper below is used (by BlockEntityRenderDispatcherMixin).
    /**
     * Standalone jiggle transformation logic; can be shared with BlockEntityRenderers.
     * Assumes the MatrixStack is currently translated to the block's local origin (0, 0, 0).
     */
    /**
     * 26.2 port of the old world-render pass: every jiggling block is re-submitted as a
     * "moving block" (the same path pistons use) with the wobble transform applied.
     * Called from the platform's level-render submit event with a camera-relative pose stack.
     */
    public static void collectSubmits(Minecraft client, PoseStack poseStack, SubmitNodeCollector collector, Vec3 cameraPos, float partialTick) {
        ClientLevel level = client.level;
        if (level == null) return;
        long worldTime = level.getGameTime();
        for (var entry : BlockJiggleManager.INSTANCE.getActiveJiggles()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getRenderShape() != RenderShape.MODEL) continue;

            MovingBlockRenderState block = new MovingBlockRenderState();
            block.blockPos = pos;
            block.randomSeedPos = pos;
            block.blockState = state;
            block.biome = level.getBiome(pos);
            block.cardinalLighting = level.cardinalLighting();
            block.lightEngine = level.getLightEngine();
            int blockLight = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(pos);
            int skyLight = level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos);
            int packedLight = (skyLight << 20) | (blockLight << 4);

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            applyJiggleTransform(poseStack, pos, partialTick, worldTime);
            collector.submitMovingBlock(poseStack, block, packedLight);
            poseStack.popPose();
        }
    }

    public static void applyJiggleTransform(PoseStack matrices, BlockPos pos, float tickDelta, long worldTime) {
        BlockJiggleManager.Jiggle jiggle = BlockJiggleManager.INSTANCE.getJiggle(pos.asLong());
        if (jiggle == null) return;

        BlockJiggleManager.JiggleConfig config = jiggle.config();

        // Calculate age including partial ticks for smoothness
        float age = (worldTime - jiggle.startTick()) + tickDelta;

        if (age < 0 || age > config.duration()) return;

        // --- Physics Math ---
        // Envelope goes from 0.0 to 1.0
        float p = age / config.duration();
        float envelope = 0.5f - 0.5f * Mth.cos((float)(Math.PI * 2.0 * p));

        // Oscillation frequency
        float w = (float)(Math.PI * 2.0 * (config.oscillationCycles() / config.duration()));

        // Randomize phases based on seed so every block jiggles differently
        RandomSource r = RandomSource.create(jiggle.seed());
        float phaseX = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseZ = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseRotX = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseRotY = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseRotZ = r.nextFloat() * (float)(Math.PI * 2.0);

        // Calculate offsets using custom values for each feature
        float dx = envelope * config.amplitude() * Mth.cos(w * age + phaseX);
        float dy = 0f;
        float dz = envelope * config.amplitude() * Mth.sin(w * age + phaseZ);

        float rotX = envelope * config.rotationAmplitude() * Mth.sin(w * age + phaseRotX);
        float rotY = envelope * config.rotationAmplitude() * Mth.cos(w * age + phaseRotY);
        float rotZ = envelope * config.rotationAmplitude() * Mth.sin(w * age + phaseRotZ);

        // --- Transformation Application ---
        // Center pivot, apply transforms, un-center
        matrices.translate(0.5 + dx, 0.5 + dy, 0.5 + dz);
        matrices.mulPose(Axis.XP.rotationDegrees(rotX));
        matrices.mulPose(Axis.YP.rotationDegrees(rotY));
        matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));
        matrices.translate(-0.5, -0.5, -0.5);
    }
}