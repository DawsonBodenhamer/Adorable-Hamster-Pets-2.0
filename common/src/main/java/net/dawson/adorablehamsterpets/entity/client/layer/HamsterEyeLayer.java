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
 * Renders the eyes over the fur base, reading genetic data to determine color.
 */
public class HamsterEyeLayer extends GeoRenderLayer<HamsterEntity> {

    private static final Identifier BLACK_EYE_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/overlays/eyes/black_eyes.png");
    private static final Identifier RED_EYE_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/overlays/eyes/red_eyes.png");

    public HamsterEyeLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        // Check if recessive red eye gene (rr = 2) is active and the config is enabled
        boolean isRed = animatable.getGenome().eyeGenotype() == 2 && Configs.AHP.enableRedEyes && !animatable.isSweetPotato(); // Sweet Potato easter egg forces black eyes
        Identifier eyeTexture = isRed ? RED_EYE_TEXTURE : BLACK_EYE_TEXTURE;
        RenderLayer eyeRenderType = RenderLayer.getEntityCutoutNoCull(eyeTexture);

        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                eyeRenderType,
                bufferSource.getBuffer(eyeRenderType),
                partialTick,
                packedLight,
                OverlayTexture.DEFAULT_UV,
                1.0F, 1.0F, 1.0F, 1.0F // R, G, B, A)
        );
    }
}