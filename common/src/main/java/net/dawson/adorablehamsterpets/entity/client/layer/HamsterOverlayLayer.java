package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;


public class HamsterOverlayLayer extends GeoRenderLayer<HamsterEntity> {

    public HamsterOverlayLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    // Helper method to construct the overlay texture identifier
    @Nullable
    private Identifier getOverlayTexture(HamsterEntity entity) {
        HamsterVariant variant = HamsterVariant.byId(entity.getVariant());
        String overlayName = variant.getOverlayTextureName();

        if (overlayName != null) {
            return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/" + overlayName + ".png");
        }
        return null; // No overlay for this variant
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        Identifier overlayTexture = getOverlayTexture(animatable);

        // If there is no overlayTexture, do nothing.
        if (overlayTexture != null) {
            RenderLayer overlayRenderType = RenderLayer.getEntityTranslucent(overlayTexture); // Use translucent so base shows through

            getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    overlayRenderType,
                    bufferSource.getBuffer(overlayRenderType),
                    partialTick,
                    packedLight,
                    OverlayTexture.DEFAULT_UV,
                    1.0F, 1.0F, 1.0F, 1.0F // R, G, B, A
            );
        }
    }
}