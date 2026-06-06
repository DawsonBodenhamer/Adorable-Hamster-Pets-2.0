package net.dawson.adorablehamsterpets.client.render;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

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

    public static void render(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d cameraPos, float tickDelta) {
        if (client.world == null) return;

        long worldTime = client.world.getTime();
        var blockRenderManager = client.getBlockRenderManager();

        for (var entry : BlockJiggleManager.INSTANCE.getActiveJiggles()) {
            long posLong = entry.getLongKey();
            BlockPos pos = BlockPos.fromLong(posLong);

            // Prevent rendering ghost blocks if player breaks it mid jiggle
            if (!client.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;

            BlockState state = client.world.getBlockState(pos);

            // Mixin handles animated block entity deformation
            if (state.getRenderType() != BlockRenderType.MODEL) continue;

            matrices.push();

            // Translate to block position relative to camera
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            // Apply jiggle math
            applyJiggleTransform(matrices, pos, tickDelta, worldTime);

            // Use getLightmapCoordinates to make fake block match real block's lighting
            int light = WorldRenderer.getLightmapCoordinates(client.world, state, pos);

            BakedModel model = blockRenderManager.getModel(state);
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());

            // Use ModelRenderer directly so renderer can query BiomeColors
            blockRenderManager.getModelRenderer().render(
                    client.world,
                    model,
                    state,
                    pos,
                    matrices,
                    buffer,
                    false,
                    Random.create(),
                    state.getRenderingSeed(pos),
                    light
            );

            matrices.pop();
        }
    }

    /**
     * Standalone jiggle transformation logic; can be shared with BlockEntityRenderers.
     * Assumes the MatrixStack is currently translated to the block's local origin (0, 0, 0).
     */
    public static void applyJiggleTransform(MatrixStack matrices, BlockPos pos, float tickDelta, long worldTime) {
        BlockJiggleManager.Jiggle jiggle = BlockJiggleManager.INSTANCE.getJiggle(pos.asLong());
        if (jiggle == null) return;

        BlockJiggleManager.JiggleConfig config = jiggle.config();

        // Calculate age including partial ticks for smoothness
        float age = (worldTime - jiggle.startTick()) + tickDelta;

        if (age < 0 || age > config.duration()) return;

        // --- Physics Math ---
        // Envelope goes from 0.0 to 1.0
        float p = age / config.duration();
        float envelope = 0.5f - 0.5f * MathHelper.cos((float)(Math.PI * 2.0 * p));

        // Oscillation frequency
        float w = (float)(Math.PI * 2.0 * (config.oscillationCycles() / config.duration()));

        // Randomize phases based on seed so every block jiggles differently
        Random r = Random.create(jiggle.seed());
        float phaseX = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseZ = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseRotX = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseRotY = r.nextFloat() * (float)(Math.PI * 2.0);
        float phaseRotZ = r.nextFloat() * (float)(Math.PI * 2.0);

        // Calculate offsets using custom values for each feature
        float dx = envelope * config.amplitude() * MathHelper.cos(w * age + phaseX);
        float dy = 0f;
        float dz = envelope * config.amplitude() * MathHelper.sin(w * age + phaseZ);

        float rotX = envelope * config.rotationAmplitude() * MathHelper.sin(w * age + phaseRotX);
        float rotY = envelope * config.rotationAmplitude() * MathHelper.cos(w * age + phaseRotY);
        float rotZ = envelope * config.rotationAmplitude() * MathHelper.sin(w * age + phaseRotZ);

        // --- Transformation Application ---
        // Center pivot, apply transforms, un-center
        matrices.translate(0.5 + dx, 0.5 + dy, 0.5 + dz);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
        matrices.translate(-0.5, -0.5, -0.5);
    }
}