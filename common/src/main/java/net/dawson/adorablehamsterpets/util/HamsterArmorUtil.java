package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Handles hamster armor eligibility, durability damage, and hit feedback.
 */
public final class HamsterArmorUtil {

    private HamsterArmorUtil() {}

    public static boolean shouldAbsorbDamage(HamsterEntity hamster, DamageSource source, ItemStack armorStack) {
        if (hamster.getWorld().isClient || source.isIn(DamageTypeTags.BYPASSES_WOLF_ARMOR)) {
            return false;
        }
        if (armorStack.isEmpty() || !(armorStack.getItem() instanceof HamsterArmorItem)) {
            return false;
        }
        return !source.isIn(DamageTypeTags.IS_FIRE) || getFireProtectionLevel(hamster, armorStack) > 0;
    }

    /**
     * Damages the equipped armor and emits the matching feedback.
     *
     * @return {@code true} when the armor broke and its inventory slot must be cleared next tick
     */
    public static boolean absorbDamage(HamsterEntity hamster, ItemStack armorStack, float amount) {
        ItemStack particleStack = armorStack.copy();
        int armorDamage = (int) Math.ceil(amount);
        armorStack.damage(armorDamage, hamster, EquipmentSlot.BODY);

        boolean armorBroke = armorStack.isEmpty();
        if (armorBroke) {
            hamster.playSound(SoundEvents.ITEM_WOLF_ARMOR_BREAK, 0.5f, 1.2f);
            spawnArmorParticles(hamster, particleStack, 15, 0.1);
        } else {
            hamster.playSound(SoundEvents.ITEM_WOLF_ARMOR_DAMAGE, 0.5f, 1.2f);
            spawnArmorParticles(hamster, particleStack, 5, 0.05);
        }
        return armorBroke;
    }

    private static void spawnArmorParticles(HamsterEntity hamster, ItemStack particleStack, int count, double speed) {
        ParticleEffectsUtil.spawnParticles(
                hamster.getWorld(),
                new Vec3d(hamster.getX(), hamster.getBodyY(0.5), hamster.getZ()),
                new ItemStackParticleEffect(ParticleTypes.ITEM, particleStack),
                count,
                new Vec3d(0.2, 0.2, 0.2),
                speed
        );
    }

    private static int getFireProtectionLevel(HamsterEntity hamster, ItemStack stack) {
        RegistryWrapper.Impl<Enchantment> registry = hamster.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> fireProtection = registry.getOrThrow(Enchantments.FIRE_PROTECTION);
        return EnchantmentHelper.getLevel(fireProtection, stack);
    }
}
