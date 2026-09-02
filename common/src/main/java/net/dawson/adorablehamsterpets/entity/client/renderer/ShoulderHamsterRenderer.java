package net.dawson.adorablehamsterpets.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * A specialized renderer for the shoulder-mounted hamster.
 * It extends the base HamsterRenderer but overrides methods to suppress
 * sounds, particles, and other world-interactive effects that are not
 * needed for a purely cosmetic render.
 */
public class ShoulderHamsterRenderer extends HamsterRenderer {

    public ShoulderHamsterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /**
     * Overrides the main render method to bypass logic that is not relevant
     * for a shoulder-mounted entity, such as cleaning sounds and snow offset.
     */
    @Override
    public void render(HamsterEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        this.shadowRadius = entity.isBaby() ? 0.1F : 0.2F;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}