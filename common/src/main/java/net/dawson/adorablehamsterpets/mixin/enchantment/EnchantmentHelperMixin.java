package net.dawson.adorablehamsterpets.mixin.enchantment;

import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    /**
     * Intercepts the generation of the available enchantment list for the table.
     * Fallback to ensure Fire Protection is offered for Hamster Armor
     * even if the standard checks fail.
     */
    @Inject(method = "getPossibleEntries", at = @At("RETURN"))
    private static void adorablehamsterpets$injectEnchantments(int power, ItemStack stack, boolean treasure, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        if (!(stack.getItem() instanceof HamsterArmorItem)) {
            return;
        }

        List<EnchantmentLevelEntry> entries = cir.getReturnValue();

        // Check if Fire Protection is already in the list
        boolean hasFireProt = false;
        for (EnchantmentLevelEntry entry : entries) {
            if (entry.enchantment == Enchantments.FIRE_PROTECTION) {
                hasFireProt = true;
                break;
            }
        }

        // If missing, calculate valid levels and add it manually
        if (!hasFireProt) {
            // Iterate through levels (Fire Prot max is 4)
            for (int level = 1; level <= Enchantments.FIRE_PROTECTION.getMaxLevel(); level++) {
                // Check if the enchanting power is sufficient for this level
                // Min cost for Fire Prot: 10 + (level - 1) * 8
                // Max cost for Fire Prot: Min + 8
                int minCost = Enchantments.FIRE_PROTECTION.getMinPower(level);
                int maxCost = Enchantments.FIRE_PROTECTION.getMaxPower(level);

                if (power >= minCost && power <= maxCost) {
                    entries.add(new EnchantmentLevelEntry(Enchantments.FIRE_PROTECTION, level));
                }
            }
        }
    }
}