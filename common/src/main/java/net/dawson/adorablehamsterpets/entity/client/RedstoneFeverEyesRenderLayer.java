package net.dawson.adorablehamsterpets.entity.client;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders only the Redstone Fever eye mask with vanilla's fullbright eyes render type.
 */
public final class RedstoneFeverEyesRenderLayer extends GeoRenderLayer<HamsterEntity> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Identifier FEVER_EYES_TEXTURE = Identifier.of(
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
            MatrixStack poseStack,
            HamsterEntity animatable,
            BakedGeoModel bakedModel,
            @Nullable RenderLayer renderType,
            VertexConsumerProvider bufferSource,
            @Nullable VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay) {
        if (!animatable.hasRedstoneFever() || animatable.isInvisible() || buffer == null) {
            return;
        }

        RenderLayer eyesRenderType = RenderLayer.getEyes(FEVER_EYES_TEXTURE);
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
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                packedOverlay,
                renderColor);
    }
}
