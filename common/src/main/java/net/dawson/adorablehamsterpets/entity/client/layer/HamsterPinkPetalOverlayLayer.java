package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class HamsterPinkPetalOverlayLayer extends GeoRenderLayer<HamsterEntity> {

    // Single shared texture for all 3D petal positions
    private static final Identifier PETAL_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/overlay_pink_petal.png");

    public HamsterPinkPetalOverlayLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        // 1. Get the petal type (0 = none, 1 = head, 2 = side, 3 = lower back)
        int petalType = animatable.getDataTracker().get(HamsterEntity.PINK_PETAL_TYPE);
        if (petalType == 0) return;

        // 2. Locate the bones
        CoreGeoBone headBone = getRenderer().getGeoModel().getAnimationProcessor().getBone("pink_petal_head");
        CoreGeoBone sideBone = getRenderer().getGeoModel().getAnimationProcessor().getBone("pink_petal_side");
        CoreGeoBone backBone = getRenderer().getGeoModel().getAnimationProcessor().getBone("pink_petal_lower_back");

        // 3. Determine which bone to unhide
        CoreGeoBone targetBone = switch (petalType) {
            case 1 -> headBone;
            case 2 -> sideBone;
            case 3 -> backBone;
            default -> null;
        };

        if (targetBone != null) {
            // 4. Temporarily unhide the target bone for this render pass
            targetBone.setHidden(false);

            // 5. Render the model using the petal texture
            RenderLayer petalRenderType = RenderLayer.getEntityCutoutNoCull(PETAL_TEXTURE);

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
                    1.0F, 1.0F, 1.0F, 1.0F // R, G, B, A
            );

            // 6. Re-hide the bone immediately to keep the main render pass clean
            targetBone.setHidden(true);
        }
    }
}