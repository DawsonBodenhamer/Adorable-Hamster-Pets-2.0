package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/**
 * Handles hamster armor eligibility, durability damage, and hit feedback.
 */
public final class HamsterArmorUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean shouldAbsorbDamage(
            HamsterEntity hamster, DamageSource source, ItemStack armorStack) {
        if (hamster.level().isClientSide() || source.is(DamageTypeTags.BYPASSES_WOLF_ARMOR)) {
            return false;
        }
        if (armorStack.isEmpty() || !(armorStack.getItem() instanceof HamsterArmorItem)) {
            return false;
        }
        return !source.is(DamageTypeTags.IS_FIRE)
                || getFireProtectionLevel(hamster, armorStack) > 0;
    }

    /**
     * Damages the equipped armor and emits the matching feedback.
     *
     * @return {@code true} when the armor broke and its inventory slot must be cleared next tick
     */
    public static boolean absorbDamage(HamsterEntity hamster, ItemStack armorStack, float amount) {
        ItemStack particleStack = armorStack.copy();
        int armorDamage = (int) Math.ceil(amount);
        armorStack.hurtAndBreak(armorDamage, hamster, EquipmentSlot.BODY);

        boolean armorBroke = armorStack.isEmpty();
        if (armorBroke) {
            hamster.playSound(SoundEvents.WOLF_ARMOR_BREAK.value(), 0.5f, 1.2f);
            spawnArmorParticles(hamster, particleStack, 15, 0.1);
        } else {
            hamster.playSound(SoundEvents.WOLF_ARMOR_DAMAGE, 0.5f, 1.2f);
            spawnArmorParticles(hamster, particleStack, 5, 0.05);
        }
        return armorBroke;
    }

    private static void spawnArmorParticles(
            HamsterEntity hamster, ItemStack particleStack, int count, double speed) {
        ParticleEffectsUtil.spawnParticles(
                hamster.level(),
                new Vec3(hamster.getX(), hamster.getY(0.5), hamster.getZ()),
                new ItemParticleOption(ParticleTypes.ITEM, particleStack.getItem()),
                count,
                new Vec3(0.2, 0.2, 0.2),
                speed);
    }

    private static int getFireProtectionLevel(HamsterEntity hamster, ItemStack stack) {
        HolderLookup.RegistryLookup<Enchantment> registry =
                hamster.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fireProtection =
                registry.getOrThrow(Enchantments.FIRE_PROTECTION);
        return EnchantmentHelper.getItemEnchantmentLevel(fireProtection, stack);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterArmorUtil() {}
}
