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
 * Manually permits specific enchantments (Frost Walker, Fire Protection, Soul Speed)
 * on Hamster Armor, bypassing the hardcoded category checks present in 1.20.1.
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$allowSpecificEnchantsOnHamsterArmor(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // If Hamster Armor
        if (!(stack.getItem() instanceof HamsterArmorItem)) {
            return;
        }

        Enchantment self = (Enchantment) (Object) this;

        // Whitelist specific enchantments
        // We use '==' comparison because Enchantments are singletons in 1.20.1
        if (self == Enchantments.FIRE_PROTECTION ||
                self == Enchantments.SOUL_SPEED ||
                self == Enchantments.FROST_WALKER) {
            cir.setReturnValue(true);
        }
    }
}