package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.util.HamsterTextureUtil;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class HamsterWildFurOverlayLayer extends GeoRenderLayer<HamsterEntity> {

    public HamsterWildFurOverlayLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (Configs.AHP.performanceMode) return;

        if (animatable.isSweetPotato()) return;

        HamsterGenome genome = animatable.getGenome();

        if (genome.wildOverlayPattern() > 0 && genome.wildOverlayPaletteId() != null) {
            String patternName = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.get(genome.wildOverlayPattern());
            Identifier overlayTexture = HamsterTextureUtil.getOrCreateDynamicTexture("overlays/fur_overlay_pattern/" + patternName, genome.wildOverlayPaletteId());
            RenderLayer overlayRenderType = RenderLayer.getEntityTranslucent(overlayTexture);

            getRenderer().reRender(
                    bakedModel, poseStack, bufferSource, animatable,
                    overlayRenderType, bufferSource.getBuffer(overlayRenderType),
                    partialTick, packedLight, OverlayTexture.DEFAULT_UV,
                    1.0F, 1.0F, 1.0F, 1.0F // R, G, B, A
            );
        }
    }
}