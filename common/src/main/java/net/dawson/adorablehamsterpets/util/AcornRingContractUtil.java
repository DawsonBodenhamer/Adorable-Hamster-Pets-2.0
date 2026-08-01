package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Applies the Acorn Ring contract consistently to hamsters and conventionally owned pets. */
public final class AcornRingContractUtil {

    public static boolean protects(LivingEntity attackingPet, LivingEntity target) {
        if (!(attackingPet.getWorld() instanceof ServerWorld serverWorld)
                || !isEligiblePet(attackingPet)) {
            return false;
        }

        UUID attackingOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(attackingPet);
        UUID targetOwnerUuid = PetOwnershipUtil.resolveTargetOwnerUuid(target);
        return protects(serverWorld, attackingOwnerUuid, targetOwnerUuid);
    }

    public static boolean isEligiblePet(LivingEntity entity) {
        return isEligiblePet(
                Configs.AHP_MAIN.acornRingOnlyProtectsHamsters,
                isHamster(entity),
                PetOwnershipUtil.resolveOwnerUuid(entity) != null);
    }

    private static boolean isHamster(LivingEntity entity) {
        var entityId = Registries.ENTITY_TYPE.getId(entity.getType());
        return entityId.getNamespace().equals(AdorableHamsterPets.MOD_ID)
                && entityId.getPath().equals("hamster");
    }

    static boolean isEligiblePet(boolean onlyHamsters, boolean hamster, boolean hasOwner) {
        return hasOwner && (!onlyHamsters || hamster);
    }

    private static boolean protects(
            ServerWorld world, @Nullable UUID attackingOwnerUuid, @Nullable UUID targetOwnerUuid) {
        if (attackingOwnerUuid == null
                || targetOwnerUuid == null
                || attackingOwnerUuid.equals(targetOwnerUuid)) {
            return false;
        }

        ServerPlayerEntity attackingOwner =
                PetOwnershipUtil.resolveOnlineOwner(world, attackingOwnerUuid);
        ServerPlayerEntity targetOwner = PetOwnershipUtil.resolveOnlineOwner(world, targetOwnerUuid);
        return attackingOwner != null
                && targetOwner != null
                && isContractProtected(
                        attackingOwnerUuid,
                        targetOwnerUuid,
                        AcornRingEquipment.isEquipped(attackingOwner),
                        AcornRingEquipment.isEquipped(targetOwner));
    }

    static boolean isContractProtected(
            UUID attackingOwnerUuid,
            UUID targetOwnerUuid,
            boolean attackingOwnerEquipped,
            boolean targetOwnerEquipped) {
        return !attackingOwnerUuid.equals(targetOwnerUuid)
                && AcornRingEquipment.hasMutualEquipment(
                        attackingOwnerEquipped, targetOwnerEquipped);
    }

    /** Returns whether an equipped ring prevents its wearer from directly attacking this pet. */
    public static boolean blocksDirectPlayerAttack(PlayerEntity attacker, LivingEntity target) {
        UUID targetOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(target);
        return blocksDirectPlayerAttack(
                Configs.AHP_MAIN.acornRingPreventsDamageToOwnPets,
                Configs.AHP_MAIN.acornRingPreventsDamageToOtherPets,
                attacker.getUuid(),
                targetOwnerUuid,
                AcornRingEquipment.isEquipped(attacker),
                isEquippedOnlineOwner(attacker, targetOwnerUuid));
    }

    static boolean blocksDirectPlayerAttack(
            boolean preventOwnPetDamage,
            boolean preventOtherPetDamage,
            UUID attackerUuid,
            @Nullable UUID targetOwnerUuid,
            boolean attackerEquipped,
            boolean targetOwnerEquipped) {
        if (!attackerEquipped || targetOwnerUuid == null) {
            return false;
        }
        return attackerUuid.equals(targetOwnerUuid)
                ? preventOwnPetDamage
                : preventOtherPetDamage && targetOwnerEquipped;
    }

    private static boolean isEquippedOnlineOwner(
            PlayerEntity attacker, @Nullable UUID targetOwnerUuid) {
        if (targetOwnerUuid == null
                || targetOwnerUuid.equals(attacker.getUuid())
                || !(attacker.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }
        ServerPlayerEntity targetOwner =
                PetOwnershipUtil.resolveOnlineOwner(serverWorld, targetOwnerUuid);
        return targetOwner != null && AcornRingEquipment.isEquipped(targetOwner);
    }

    @Nullable
    public static LivingEntity responsiblePet(@Nullable Entity attacker) {
        if (!(attacker instanceof LivingEntity living) || attacker instanceof PlayerEntity) {
            return null;
        }
        return isEligiblePet(living) ? living : null;
    }

    private AcornRingContractUtil() {}
}
