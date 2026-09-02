package net.dawson.adorablehamsterpets.mixin.server;

import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryBushBlockMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$preventHamsterDamage(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean flag, CallbackInfo ci) {
        if (entity instanceof HamsterEntity) {
            ci.cancel();
        }
    }
}