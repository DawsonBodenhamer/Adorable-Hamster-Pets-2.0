package net.dawson.adorablehamsterpets.block.client;

import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HamsterBedRenderer extends GeoBlockRenderer<HamsterBedBlockEntity> {
    public HamsterBedRenderer(BlockEntityRendererFactory.Context context) {
        super(new HamsterBedModel());
    }

    @Override
    public RenderLayer getRenderType(HamsterBedBlockEntity animatable, Identifier texture, @org.jetbrains.annotations.Nullable VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityCutout(getTextureLocation(animatable));
    }

    @Override
    protected Direction getFacing(HamsterBedBlockEntity block) {
        BlockState state = block.getCachedState();
        // fall back to super if the property isn’t present
        return state.contains(HamsterBedBlock.ORIENTATION)
                ? state.get(HamsterBedBlock.ORIENTATION)
                : super.getFacing(block);
    }

    @Override
    public void render(HamsterBedBlockEntity blockEntity, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
        BlockState blockState = blockEntity.getCachedState();
        if (blockState.get(HamsterBedBlock.UPSIDE_DOWN)) {
            poseStack.push();
            // Translate to the center of the block to rotate around it
            poseStack.translate(0.5, 0.5, 0.5);
            // Rotate 180 degrees around the X-axis
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            // Translate back
            poseStack.translate(-0.5, -0.5, -0.5);
        }

        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        if (blockState.get(HamsterBedBlock.UPSIDE_DOWN)) {
            poseStack.pop();
        }
    }
}