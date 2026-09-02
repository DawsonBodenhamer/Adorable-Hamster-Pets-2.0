package net.dawson.adorablehamsterpets.block.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import com.geckolib.renderer.GeoBlockRenderer;

public class HamsterBedRenderer extends GeoBlockRenderer<HamsterBedBlockEntity> {
    public HamsterBedRenderer(BlockEntityRendererProvider.Context context) {
        super(new HamsterBedModel());
    }

    @Override
    public RenderType getRenderType(HamsterBedBlockEntity animatable, Identifier texture, @org.jetbrains.annotations.Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(getTextureLocation(animatable));
    }

    @Override
    protected Direction getFacing(HamsterBedBlockEntity block) {
        BlockState state = block.getBlockState();
        // fall back to super if the property isn’t present
        return state.hasProperty(HamsterBedBlock.ORIENTATION)
                ? state.getValue(HamsterBedBlock.ORIENTATION)
                : super.getFacing(block);
    }

    @Override
    public void render(HamsterBedBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getValue(HamsterBedBlock.UPSIDE_DOWN)) {
            poseStack.pushPose();
            // Translate to the center of the block to rotate around it
            poseStack.translate(0.5, 0.5, 0.5);
            // Rotate 180 degrees around the X-axis
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            // Translate back
            poseStack.translate(-0.5, -0.5, -0.5);
        }

        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        if (blockState.getValue(HamsterBedBlock.UPSIDE_DOWN)) {
            poseStack.popPose();
        }
    }
}