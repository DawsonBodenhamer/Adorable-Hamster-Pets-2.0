package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class HamsterArmorLayer extends GeoRenderLayer<HamsterEntity> {

    public HamsterArmorLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (Configs.AHP.performanceMode) return;

        // Check global visual toggle
        if (!Configs.AHP.enableArmorVisuals) {
            return;
        }

        ItemStack armorStack = animatable.getArmorStack();

        if (armorStack.isEmpty() || !(armorStack.getItem() instanceof HamsterArmorItem armorItem)) {
            return;
        }

        Identifier armorTexture = armorItem.getEntityTexture();
        RenderLayer armorRenderType = RenderLayer.getEntityCutoutNoCull(armorTexture);

        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                armorRenderType,
                bufferSource.getBuffer(armorRenderType),
                partialTick,
                packedLight,
                OverlayTexture.DEFAULT_UV,
                ColorHelper.Argb.getArgb(255, 255, 255, 255)
        );
    }
}