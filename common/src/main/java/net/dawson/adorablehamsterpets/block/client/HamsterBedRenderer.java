package net.dawson.adorablehamsterpets.block.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public class HamsterBedRenderer extends GeoBlockRenderer<HamsterBedBlockEntity, HamsterBedRenderState> {

    /** Captured during extraction; the pose pass cannot see the block state. */
    private static final DataTicket<Boolean> UPSIDE_DOWN =
            DataTicket.create("adorablehamsterpets:bed_upside_down", Boolean.class);

    public HamsterBedRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new HamsterBedModel());
    }

    @Override
    public HamsterBedRenderState createRenderState() {
        return new HamsterBedRenderState();
    }


    @Override
    protected Direction getBlockStateDirection(HamsterBedBlockEntity block) {
        BlockState state = block.getBlockState();
        // fall back to super if the property isn't present
        return state.hasProperty(HamsterBedBlock.ORIENTATION)
                ? state.getValue(HamsterBedBlock.ORIENTATION)
                : super.getBlockStateDirection(block);
    }

    @Override
    public void addRenderData(HamsterBedBlockEntity blockEntity, Void relatedObject, HamsterBedRenderState state, float partialTick) {
        super.addRenderData(blockEntity, relatedObject, state, partialTick);
        BlockState blockState = blockEntity.getBlockState();
        state.addGeckolibData(UPSIDE_DOWN,
                blockState.hasProperty(HamsterBedBlock.UPSIDE_DOWN) && blockState.getValue(HamsterBedBlock.UPSIDE_DOWN));
    }

    /** Flip the whole model when the bed is placed upside down. */
    @Override
    public void adjustRenderPose(RenderPassInfo<HamsterBedRenderState> renderPass) {
        super.adjustRenderPose(renderPass);
        if (renderPass.renderState().getOrDefaultGeckolibData(UPSIDE_DOWN, false)) {
            PoseStack poseStack = renderPass.poseStack();
            // Rotate 180 degrees around the X-axis about the block centre
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.translate(-0.5, -0.5, -0.5);
        }
    }
}
