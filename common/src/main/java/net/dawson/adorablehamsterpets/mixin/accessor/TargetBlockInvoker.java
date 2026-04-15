package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.block.BlockState;
import net.minecraft.block.TargetBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TargetBlock.class)
public interface TargetBlockInvoker {
    @Invoker("trigger")
    static int adorablehamsterpets$callTrigger(WorldAccess world, BlockState state, BlockHitResult hitResult, Entity entity) {
        throw new AssertionError();
    }
}