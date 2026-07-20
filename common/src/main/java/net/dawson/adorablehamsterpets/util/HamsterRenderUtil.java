package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

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

        BlockPos pos = hamster.getBlockPos();
        BlockState state = hamster.getWorld().getBlockState(pos);
        if (state.isOf(Blocks.SNOW)) {
            return SNOW_SURFACE_GAP;
        }
        if (!state.isOf(Blocks.MUD)) {
            return 0.0F;
        }

        VoxelShape collisionShape = state.getCollisionShape(hamster.getWorld(), pos);
        if (collisionShape.isEmpty()) {
            return 0.0F;
        }

        float surfaceOffset = (float) Math.max(0.0, 1.0 - collisionShape.getMax(Direction.Axis.Y));
        BlockState stateAbove = hamster.getWorld().getBlockState(pos.up());
        if (stateAbove.isOf(Blocks.SNOW) && stateAbove.get(SnowBlock.LAYERS) == 1) {
            surfaceOffset += SNOW_SURFACE_GAP;
        }

        return surfaceOffset;
    }
}
