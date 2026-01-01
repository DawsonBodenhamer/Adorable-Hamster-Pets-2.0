package net.dawson.adorablehamsterpets.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class LeafJiggleRenderer {

    // --- Tuning Variables ---
    private static final float AMPLITUDE = 0.05f;            // Translation distance in blocks
    private static final float ROTATION_AMPLITUDE = 4.0f;    // Rotation in degrees
    private static final float OSCILLATION_CYCLES = 6.0f;    // How many wiggles in 20 ticks

    public static void render(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d cameraPos, float tickDelta) {
        if (client.world == null) return;

        long worldTime = client.world.getTime();
        var blockRenderManager = client.getBlockRenderManager();

        for (var entry : LeafJiggleManager.INSTANCE.getActiveJiggles()) {
            long posLong = entry.getLongKey();
            BlockPos pos = BlockPos.fromLong(posLong);

            // Safety check: Prevent rendering ghost blocks if player breaks it mid-jiggle
            if (!client.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;

            BlockState state = client.world.getBlockState(pos);
            if (!state.isIn(BlockTags.LEAVES)) continue;

            LeafJiggleManager.Jiggle jiggle = entry.getValue();

            // Calculate age (including partial ticks for smoothness)
            float age = (worldTime - jiggle.startTick()) + tickDelta;

            if (age < 0 || age > LeafJiggleManager.DURATION_TICKS) continue;

            // --- Physics Math ---
            // 1. Envelope: 0 -> 1 -> 0 cosine curve
            // p goes from 0.0 to 1.0
            float p = age / LeafJiggleManager.DURATION_TICKS;
            // envelope starts at 0, peaks at 1, ends at 0
            float envelope = 0.5f - 0.5f * MathHelper.cos((float)(Math.PI * 2.0 * p));

            // 2. Oscillation Frequency
            float w = (float)(Math.PI * 2.0 * (OSCILLATION_CYCLES / LeafJiggleManager.DURATION_TICKS));

            // 3. Randomize phases based on seed so every leaf jiggles differently
            Random r = Random.create(jiggle.seed());
            float phaseX = r.nextFloat() * (float)(Math.PI * 2.0);
            float phaseZ = r.nextFloat() * (float)(Math.PI * 2.0);
            float phaseRotX = r.nextFloat() * (float)(Math.PI * 2.0);
            float phaseRotY = r.nextFloat() * (float)(Math.PI * 2.0);
            float phaseRotZ = r.nextFloat() * (float)(Math.PI * 2.0);

            // 4. Calculate Offsets
            float dx = envelope * AMPLITUDE * MathHelper.cos(w * age + phaseX);
            float dy = 0f; // Keep Y stable for now, or add small bounce if desired
            float dz = envelope * AMPLITUDE * MathHelper.sin(w * age + phaseZ);

            float rotX = envelope * ROTATION_AMPLITUDE * MathHelper.sin(w * age + phaseRotX);
            float rotY = envelope * ROTATION_AMPLITUDE * MathHelper.cos(w * age + phaseRotY);
            float rotZ = envelope * ROTATION_AMPLITUDE * MathHelper.sin(w * age + phaseRotZ);

            // --- Rendering ---
            matrices.push();

            // Translate to block position relative to camera
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            // Center pivot, apply transforms, un-center
            matrices.translate(0.5, 0.5, 0.5);
            matrices.translate(dx, dy, dz);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
            matrices.translate(-0.5, -0.5, -0.5);

            // Render
            // Use getLightmapCoordinates to ensure it matches the real block's lighting
            int light = WorldRenderer.getLightmapCoordinates(client.world, state, pos);

            // Get the baked model for the current state
            BakedModel model = blockRenderManager.getModel(state);

            // Get the specific buffer for CutoutMipped
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());

            // Use the ModelRenderer directly.
            // passing 'client.world' and 'pos' enables the renderer to query BiomeColors.
            blockRenderManager.getModelRenderer().render(
                    client.world,
                    model,
                    state,
                    pos,
                    matrices,
                    buffer,
                    false, // cull
                    Random.create(),
                    state.getRenderingSeed(pos),
                    OverlayTexture.DEFAULT_UV
            );

            matrices.pop();
        }
    }
}