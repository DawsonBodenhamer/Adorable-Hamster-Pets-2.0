package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterTextureUtil;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders the hamster's armor trim on a separate emissive layer.
 * Utilizing RenderLayer.getEyes() ensures the trim glows in the dark in vanilla
 * and signals external shader mods like Iris to apply bloom/emissive effects.
 */
public class HamsterTrimRenderLayer extends GeoRenderLayer<HamsterEntity> {

    public HamsterTrimRenderLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        Identifier trimTexture = HamsterTextureUtil.getHamsterTrimTexture(animatable);

        if (trimTexture != null) {
            RenderLayer trimRenderLayer;
            int lightCoord;

            if (Configs.AHP.emissiveArmorTrims.get()) {
                // Shaders recognize getEyes() as emissive
                trimRenderLayer = RenderLayer.getEntityTranslucentEmissive(trimTexture);
                lightCoord = LightmapTextureManager.MAX_LIGHT_COORDINATE;
            } else {
                // Standard render layer, boring, ick
                trimRenderLayer = RenderLayer.getEntityCutoutNoCull(trimTexture);
                lightCoord = packedLight;
            }

            VertexConsumer trimBuffer = bufferSource.getBuffer(trimRenderLayer);

            this.getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    trimRenderLayer,
                    trimBuffer,
                    partialTick,
                    lightCoord,
                    packedOverlay,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}