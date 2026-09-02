package net.dawson.adorablehamsterpets.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/**
 * Shared offsets for rendering items held in the hamster's mouth.
 */
public final class HamsterMouthItemOffsets {

    private HamsterMouthItemOffsets() {}

    /**
     * Applies the specific translation, scaling, and rotation required to position
     * an item correctly in the hamster's mouth (anchored to the nose bone).
     *
     * @param matrices The MatrixStack currently anchored to the nose bone.
     */
    public static void applyMouthItemTransforms(PoseStack matrices) {
        // --- 1. Translation ---
        // X: Positive values move it to the hamster's right. Negative to the left
        // Y: Positive values move it up. Negative moves it down
        // Z: Positive values move it towards the hamster's tail. Negative values move it forward, away from the tail
        matrices.translate(0.0F, 0.125F, -0.14F); // Negative Z so it sticks out of the mouth

        // --- 2. Scaling ---
        // Scale item down slightly so it fits better
        matrices.scale(0.7F, 0.7F, 0.7F);

        // --- 3. Rotation ---
        // Rotates the item -90 degrees on its X-axis.
        // This makes the item lay flat, as if the hamster is holding the bottom part
        // of the item in its mouth, with the top of the item sticking out forward
        matrices.mulPose(Axis.XN.rotationDegrees(90.0F));
    }
}