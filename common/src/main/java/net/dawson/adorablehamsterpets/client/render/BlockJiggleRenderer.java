package net.dawson.adorablehamsterpets.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
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

    public static void render(Minecraft client, PoseStack matrices, MultiBufferSource vertexConsumers, Vec3 cameraPos, float tickDelta) {
        if (client.level == null) return;

        long worldTime = client.level.getGameTime();
        var blockRenderManager = client.getBlockRenderer();

        for (var entry : BlockJiggleManager.INSTANCE.getActiveJiggles()) {
            long posLong = entry.getLongKey();
            BlockPos pos = BlockPos.of(posLong);

            // Prevent rendering ghost blocks if player breaks it mid jiggle
            if (!client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;

            BlockState state = client.level.getBlockState(pos);

            // Mixin handles animated block entity deformation
            if (state.getRenderShape() != RenderShape.MODEL) continue;

            matrices.pushPose();

            // Translate to block position relative to camera
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            // Apply jiggle math
            applyJiggleTransform(matrices, pos, tickDelta, worldTime);

            // Use getLightmapCoordinates to make fake block match real block's lighting
            int light = LevelRenderer.getLightColor(client.level, state, pos);

            BakedModel model = blockRenderManager.getBlockModel(state);
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.cutoutMipped());

            // Use ModelRenderer directly so renderer can query BiomeColors
            blockRenderManager.getModelRenderer().tesselateBlock(
                    client.level,
                    model,
                    state,
                    pos,
                    matrices,
                    buffer,
                    false,
                    RandomSource.create(),
                    state.getSeed(pos),
                    light
            );

            matrices.popPose();
        }
    }

    /**
     * Standalone jiggle transformation logic; can be shared with BlockEntityRenderers.
     * Assumes the MatrixStack is currently translated to the block's local origin (0, 0, 0).
     */
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