package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects when a Cheese or Blue Cheese music disc touches portal to convert it to the Blue Cheese disc and/or bounce it.
 * Also prevents hamsters from getting lost by bouncing them out of the portal.
 */
@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {

    // --- Intercept End Portal Collision ---
    // Check specifically for end dimension to ensure portal active after defeating dragon
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$onEndPortalCollision(BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!world.isClientSide() && world.dimension() == Level.END) {

            // --- 1. Music Disc Logic ---
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getItem();

                boolean isCheese = stack.is(ModItems.MUSIC_DISC_CHEESE.get());
                boolean isBlueCheese = stack.is(ModItems.MUSIC_DISC_BLUE_CHEESE.get());
                boolean isParmesan = stack.is(ModItems.MUSIC_DISC_PARMESAN.get());

                if (isCheese || isBlueCheese || isParmesan) {
                    if (isCheese || isParmesan) {
                        // Convert regular or parmesan cheese into blue cheese
                        itemEntity.setItem(new ItemStack(ModItems.MUSIC_DISC_BLUE_CHEESE.get(), stack.getCount()));
                        world.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.2f);
                    }

                    this.adorablehamsterpets$bounceEntity(itemEntity, world, pos);
                    ci.cancel();
                    return;
                }
            }

            // --- 2. Hamster Logic ---
            if (entity instanceof HamsterEntity && Configs.AHP_MAIN.preventHamsterEndPortalTravel) {
                this.adorablehamsterpets$bounceEntity(entity, world, pos);
                ci.cancel();
            }
        }
    }

    @Unique
    private void adorablehamsterpets$bounceEntity(Entity entity, Level world, BlockPos pos) {
        // Shoot back out to prevent falling back into portal
        double vx = (world.random.nextDouble() - 0.5) * 0.5;
        double vy = 0.5 + world.random.nextDouble() * 0.2;
        double vz = (world.random.nextDouble() - 0.5) * 0.5;

        entity.setDeltaMovement(vx, vy, vz);
        entity.hurtMarked = true;
        entity.hasImpulse = true;

        // Visual and audio feedback
        ParticleEffectsUtil.spawnParticles(
                world,
                entity.position(),
                ParticleTypes.END_ROD,
                15,
                new Vec3(0.2, 0.2, 0.2),
                0
        );
        world.playSound(null, pos, SoundEvents.BREEZE_JUMP, SoundSource.BLOCKS, 1.0f, 1.2f);
    }
}