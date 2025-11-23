package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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


public class HamsterPinkPetalOverlayLayer extends GeoRenderLayer<HamsterEntity> {

    public HamsterPinkPetalOverlayLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Nullable
    private Identifier getPetalTexture(HamsterEntity entity) {
        int petalType = entity.getDataTracker().get(HamsterEntity.PINK_PETAL_TYPE);
        if (petalType > 0 && petalType <= 3) {
            return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/overlay_pink_petal" + petalType + ".png");
        }
        return null; // No petal or invalid type
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        Identifier petalTexture = getPetalTexture(animatable);

        if (petalTexture != null) {
            RenderLayer petalRenderType = RenderLayer.getEntityTranslucent(petalTexture);

            getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    petalRenderType,
                    bufferSource.getBuffer(petalRenderType),
                    partialTick,
                    packedLight,
                    OverlayTexture.DEFAULT_UV,
                    ColorHelper.Argb.getArgb(255, 255, 255, 255)
            );
        }
    }
}