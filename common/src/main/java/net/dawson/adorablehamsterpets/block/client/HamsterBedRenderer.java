package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HamsterBedRenderer extends GeoBlockRenderer<HamsterBedBlockEntity> {
    public HamsterBedRenderer(BlockEntityRendererFactory.Context context) {
        super(new HamsterBedModel());
    }

    @Override
    public RenderLayer getRenderType(HamsterBedBlockEntity animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityCutout(getTextureLocation(animatable));
    }

    // On 1.20.1, use preRender to apply the upside-down rotation logic. This avoids the method signature clash in the 'render' method on 1.20.1.
    @Override
    public void preRender(MatrixStack poseStack, HamsterBedBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        BlockState blockState = animatable.getCachedState();
        boolean isUpsideDown = blockState.contains(HamsterBedBlock.UPSIDE_DOWN) && blockState.get(HamsterBedBlock.UPSIDE_DOWN);

        if (isUpsideDown) {
            poseStack.push(); // Push for rotation
            // Translate to the center of the block to rotate around it
            poseStack.translate(0.5, 0.5, 0.5);
            // Rotate 180 degrees around the X-axis
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            // Translate back
            poseStack.translate(-0.5, -0.5, -0.5);
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void postRender(MatrixStack poseStack, HamsterBedBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        BlockState blockState = animatable.getCachedState();
        boolean isUpsideDown = blockState.contains(HamsterBedBlock.UPSIDE_DOWN) && blockState.get(HamsterBedBlock.UPSIDE_DOWN);

        if (isUpsideDown) {
            poseStack.pop(); // Pop to restore state
        }
    }
}