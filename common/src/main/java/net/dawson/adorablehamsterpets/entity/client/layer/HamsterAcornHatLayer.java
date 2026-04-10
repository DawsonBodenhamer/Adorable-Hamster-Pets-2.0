package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
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

public class HamsterAcornHatLayer extends GeoRenderLayer<HamsterEntity> {

    private static final Identifier ACORN_HAT_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/armor/acorn_hat.png");

    public HamsterAcornHatLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (Configs.AHP.performanceMode) return;

        boolean shouldRender = false;

        // 1. Check Bling Slot (Slot 6) - Highest Priority
        // This is always rendered if present, regardless of config.
        ItemStack blingStack = animatable.getAccessoryStack();
        if (blingStack.isOf(ModItems.ACORN_HAT.get())) {
            shouldRender = true;
        }

        // 2. Check Armor Slot (Slot 7) - Conditional Priority
        // Only render if the config allows it AND we haven't already decided to render (though drawing it twice wouldn't hurt visually, it's inefficient)
        if (!shouldRender) {
            ItemStack armorStack = animatable.getArmorStack();
            if (armorStack.isOf(ModItems.HAMSTER_ARMOR_ACORN.get()) && Configs.AHP.renderAcornHat.get()) {
                shouldRender = true;
            }
        }

        if (shouldRender) {
            RenderLayer hatRenderType = RenderLayer.getEntityCutoutNoCull(ACORN_HAT_TEXTURE);

            getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    hatRenderType,
                    bufferSource.getBuffer(hatRenderType),
                    partialTick,
                    packedLight,
                    OverlayTexture.DEFAULT_UV,
                    ColorHelper.Argb.getArgb(255, 255, 255, 255)
            );
        }
    }
}