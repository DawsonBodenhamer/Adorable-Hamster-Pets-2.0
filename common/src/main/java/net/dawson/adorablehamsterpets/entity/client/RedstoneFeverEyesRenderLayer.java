package net.dawson.adorablehamsterpets.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.cache.object.BakedGeoModel;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders only the Redstone Fever eye mask with vanilla's fullbright eyes render type.
 */
public final class RedstoneFeverEyesRenderLayer extends GeoRenderLayer<HamsterEntity> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Identifier FEVER_EYES_TEXTURE = Identifier.fromNamespaceAndPath(
            "adorablehamsterpets",
            "textures/entity/hamster/appearance/conditions/redstone_fever/eyes.png");

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public RedstoneFeverEyesRenderLayer(HamsterRenderer renderer) {
        super(renderer);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void render(
            PoseStack poseStack,
            HamsterEntity animatable,
            BakedGeoModel bakedModel,
            @Nullable RenderType renderType,
            MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay) {
        if (!animatable.hasRedstoneFever() || animatable.isInvisible() || buffer == null) {
            return;
        }

        RenderType eyesRenderType = RenderType.eyes(FEVER_EYES_TEXTURE);
        VertexConsumer eyesBuffer = bufferSource.getBuffer(eyesRenderType);
        int renderColor = this.renderer.getRenderColor(animatable, partialTick, packedLight).argbInt();

        this.renderer.reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                eyesRenderType,
                eyesBuffer,
                partialTick,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                renderColor);
    }
}
