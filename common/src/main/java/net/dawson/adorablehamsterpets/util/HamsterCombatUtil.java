package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.UUID;

/**
 * Handles hamster-specific melee range and owner-combat exclusions.
 */
public final class HamsterCombatUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double ATTACK_BOX_EXPANSION = 0.70D;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isInAttackRange(HamsterEntity hamster, LivingEntity target) {
        Box attackBox =
                hamster.getBoundingBox().expand(ATTACK_BOX_EXPANSION, 0.0D, ATTACK_BOX_EXPANSION);
        return attackBox.intersects(target.getBoundingBox());
    }

    public static boolean canAttackWithOwner(
            HamsterEntity hamster, LivingEntity target, LivingEntity owner) {
        // --- 1. Aggression and Target Freshness ---
        if (hamster.getAggressionState() == HamsterEntity.AggressionState.PACIFIST) {
            return false;
        }
        if (target == owner.getAttacking() && owner.age - owner.getLastAttackTime() > 100) {
            return false;
        }
        if (target == owner.getAttacker() && owner.age - owner.getLastAttackedTime() > 100) {
            return false;
        }

        // --- 2. Owner and Hamster Exclusions ---
        UUID ownerUuid = owner.getUuid();
        AdorableHamsterPets.LOGGER.trace(
                "[canAttackWithOwner] Hamster: {}, Target: {}, Owner: {}",
                hamster.getName().getString(),
                target.getName().getString(),
                owner.getName().getString());

        if (target == hamster || target == owner) {
            return false;
        }
        if (target instanceof PlayerEntity && target.getUuid().equals(ownerUuid)) {
            return false;
        }
        if (target instanceof CreeperEntity || target instanceof ArmorStandEntity) {
            return false;
        }
        // --- 3. Shared-Owner Pet Exclusions ---
        if (target instanceof TameableEntity tameablePet) {
            UUID petOwnerUuid = tameablePet.getOwnerUuid();
            if (petOwnerUuid != null && petOwnerUuid.equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[canAttackWithOwner] Target is a TameableEntity owned by the same player."
                                + " Preventing attack.");
                return false;
            }
        } else if (target instanceof AbstractHorseEntity horsePet) {
            Entity horseOwnerEntity = horsePet.getOwner();
            if (horseOwnerEntity != null && horseOwnerEntity.getUuid().equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[canAttackWithOwner] Target is an AbstractHorseEntity owned by the same"
                                + " player. Preventing attack.");
                return false;
            }
        } else if (target instanceof Ownable ownableFallback) {
            Entity fallbackOwnerEntity = ownableFallback.getOwner();
            if (fallbackOwnerEntity != null && fallbackOwnerEntity.getUuid().equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[canAttackWithOwner] Target is an Ownable (fallback) owned by the same"
                                + " player. Preventing attack.");
                return false;
            }
        }
        return true;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterCombatUtil() {}
}
