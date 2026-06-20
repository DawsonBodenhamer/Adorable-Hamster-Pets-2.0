package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
    private void adorablehamsterpets$onEndPortalCollision(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!world.isClient() && world.getRegistryKey() == World.END) {

            // --- 1. Music Disc Logic ---
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getStack();

                boolean isCheese = stack.isOf(ModItems.MUSIC_DISC_CHEESE.get());
                boolean isBlueCheese = stack.isOf(ModItems.MUSIC_DISC_BLUE_CHEESE.get());
                boolean isParmesan = stack.isOf(ModItems.MUSIC_DISC_PARMESAN.get());

                if (isCheese || isBlueCheese || isParmesan) {
                    if (isCheese || isParmesan) {
                        // Convert regular or parmesan cheese into blue cheese
                        itemEntity.setStack(new ItemStack(ModItems.MUSIC_DISC_BLUE_CHEESE.get(), stack.getCount()));
                        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.2f);
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
    private void adorablehamsterpets$bounceEntity(Entity entity, World world, BlockPos pos) {
        // Shoot back out to prevent falling back into portal
        double vx = (world.random.nextDouble() - 0.5) * 0.5;
        double vy = 0.5 + world.random.nextDouble() * 0.2;
        double vz = (world.random.nextDouble() - 0.5) * 0.5;

        entity.setVelocity(vx, vy, vz);
        entity.velocityModified = true;
        entity.velocityDirty = true;

        // Visual and audio feedback
        ParticleEffectsUtil.spawnParticles(
                world,
                entity.getPos(),
                ParticleTypes.END_ROD,
                15,
                new Vec3d(0.2, 0.2, 0.2),
                0
        );
        world.playSound(null, pos, SoundEvents.ENTITY_DOLPHIN_JUMP, SoundCategory.BLOCKS, 1.0f, 1.2f);
    }
}