package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker interface to grant access to the protected static method
 * {@code getCommonNodeType} in {@link LandPathNodeMaker}.
 * This allows the use of vanilla's internal logic for determining a block's
 * pathfinding type without altering any vanilla code.
 */
@Mixin(LandPathNodeMaker.class)
public interface LandPathNodeMakerInvoker {

    /**
     * Invokes the protected static method {@code getCommonNodeType}.
     * @param world The world view.
     * @param pos The position to check.
     * @return The determined PathNodeType.
     */
    @Invoker("getCommonNodeType")
    static PathNodeType callGetCommonNodeType(BlockView world, BlockPos pos) {
        // This is a Mixin Invoker; the body is empty and will be implemented at runtime.
        throw new AssertionError();
    }
}