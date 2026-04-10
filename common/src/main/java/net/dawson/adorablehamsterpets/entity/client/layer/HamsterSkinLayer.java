package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders skin elements (nose, feet, inner ears) over the fur base.
 */
public class HamsterSkinLayer extends GeoRenderLayer<HamsterEntity> {

    private static final Identifier SKIN_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/overlays/skin/skin.png");
    private static final RenderLayer SKIN_RENDER_TYPE = RenderLayer.getEntityCutoutNoCull(SKIN_TEXTURE);

    public HamsterSkinLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (Configs.AHP.performanceMode) return;

        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                SKIN_RENDER_TYPE,
                bufferSource.getBuffer(SKIN_RENDER_TYPE),
                partialTick,
                packedLight,
                OverlayTexture.DEFAULT_UV,
                ColorHelper.Argb.getArgb(255, 255, 255, 255)
        );
    }
}