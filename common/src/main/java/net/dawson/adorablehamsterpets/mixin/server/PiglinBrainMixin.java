package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * If the Piglin is holding the Cheese Music Disc, cancelS normal loot generation and manually drops the Parmesan Music Disc instead.
 */
@Mixin(PiglinAi.class)
public class PiglinBrainMixin {

    // --- Intercept Offhand Consumption ---
    @Inject(method = "consumeOffHandItem", at = @At("HEAD"), cancellable = true)
    private static void adorablehamsterpets$onConsumeOffHandItem(Piglin piglin, boolean dropLoot, CallbackInfo ci) {
        ItemStack offHandStack = piglin.getItemInHand(InteractionHand.OFF_HAND);

        if (offHandStack.is(ModItems.MUSIC_DISC_CHEESE.get()) || offHandStack.is(ModItems.MUSIC_DISC_BLUE_CHEESE.get())) {
            // Remove cheese disc
            piglin.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

            if (dropLoot) {
                // Prepare Parmesan disc reward
                ItemStack reward = new ItemStack(ModItems.MUSIC_DISC_PARMESAN.get(), offHandStack.getCount());

                // Toss item up and away from Piglin
                ItemEntity itemEntity = new ItemEntity(
                        piglin.level(),
                        piglin.getX(),
                        piglin.getY() + 1.0,
                        piglin.getZ(),
                        reward
                );
                itemEntity.setDeltaMovement(
                        (piglin.level().random.nextDouble() - 0.5) * 0.2,
                        0.2,
                        (piglin.level().random.nextDouble() - 0.5) * 0.2
                );
                piglin.level().addFreshEntity(itemEntity);
            }

            // Cancel vanilla bartering logic
            ci.cancel();
        }
    }
}