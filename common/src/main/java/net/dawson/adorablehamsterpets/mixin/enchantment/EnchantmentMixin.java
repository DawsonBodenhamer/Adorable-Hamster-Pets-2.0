package net.dawson.adorablehamsterpets.mixin.enchantment;

import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backport Logic for 1.20.1:
 * Manually permits specific enchantments (Frost Walker) on Hamster Armor,
 * bypassing the hardcoded category checks present in 1.20.1.
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$allowFrostWalkerOnHamsterArmor(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) (Object) this;

        // Check if this enchantment instance is Frost Walker
        if (self == Enchantments.FROST_WALKER) {
            // Check if the item is Hamster Armor
            if (stack.getItem() instanceof HamsterArmorItem) {
                // Allow it
                cir.setReturnValue(true);
            }
        }
    }
}