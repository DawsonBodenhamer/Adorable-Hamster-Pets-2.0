package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * If the Piglin is holding the Cheese Music Disc, cancelS normal loot generation and manually drops the Parmesan Music Disc instead.
 */
@Mixin(PiglinBrain.class)
public class PiglinBrainMixin {

    // --- Intercept Offhand Consumption ---
    @Inject(method = "consumeOffHandItem", at = @At("HEAD"), cancellable = true)
    private static void adorablehamsterpets$onConsumeOffHandItem(PiglinEntity piglin, boolean dropLoot, CallbackInfo ci) {
        ItemStack offHandStack = piglin.getStackInHand(Hand.OFF_HAND);

        if (offHandStack.isOf(ModItems.MUSIC_DISC_CHEESE.get()) || offHandStack.isOf(ModItems.MUSIC_DISC_BLUE_CHEESE.get())) {
            // Remove cheese disc
            piglin.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);

            if (dropLoot) {
                // Prepare Parmesan disc reward
                ItemStack reward = new ItemStack(ModItems.MUSIC_DISC_PARMESAN.get(), offHandStack.getCount());

                // Toss item up and away from Piglin
                ItemEntity itemEntity = new ItemEntity(
                        piglin.getWorld(),
                        piglin.getX(),
                        piglin.getY() + 1.0,
                        piglin.getZ(),
                        reward
                );
                itemEntity.setVelocity(
                        (piglin.getWorld().random.nextDouble() - 0.5) * 0.2,
                        0.2,
                        (piglin.getWorld().random.nextDouble() - 0.5) * 0.2
                );
                piglin.getWorld().spawnEntity(itemEntity);
            }

            // Cancel vanilla bartering logic
            ci.cancel();
        }
    }
}