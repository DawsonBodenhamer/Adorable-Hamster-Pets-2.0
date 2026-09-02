package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HamsterRenderUtil {

    private static final float SNOW_SURFACE_GAP = 1.0F / 8.0F;

    private HamsterRenderUtil() {}

    /**
     * Calculates the visual lift needed to keep an in-world hamster on top of lowered block surfaces.
     * Snow already positions entities against its collision layers, leaving only one visual layer to restore.
     */
    public static float getGroundSurfaceOffset(HamsterEntity hamster) {
        if (hamster.isShoulderPet() || hamster.isProjectileDummy) {
            return 0.0F;
        }

        BlockPos pos = hamster.blockPosition();
        BlockState state = hamster.level().getBlockState(pos);
        if (state.is(Blocks.SNOW)) {
            return SNOW_SURFACE_GAP;
        }
        if (!state.is(Blocks.MUD)) {
            return 0.0F;
        }

        VoxelShape collisionShape = state.getCollisionShape(hamster.level(), pos);
        if (collisionShape.isEmpty()) {
            return 0.0F;
        }

        float surfaceOffset = (float) Math.max(0.0, 1.0 - collisionShape.max(Direction.Axis.Y));
        BlockState stateAbove = hamster.level().getBlockState(pos.above());
        if (stateAbove.is(Blocks.SNOW) && stateAbove.getValue(SnowLayerBlock.LAYERS) == 1) {
            surfaceOffset += SNOW_SURFACE_GAP;
        }

        return surfaceOffset;
    }
}
