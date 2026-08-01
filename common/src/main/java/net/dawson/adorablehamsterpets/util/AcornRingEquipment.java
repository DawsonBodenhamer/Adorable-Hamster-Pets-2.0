package net.dawson.adorablehamsterpets.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Resolves the Acorn Ring from vanilla and optional loader-specific equipment slots.
 */
public final class AcornRingEquipment {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isEquipped(PlayerEntity player) {
        return player.getOffHandStack().isOf(ModItems.ACORN_RING.get())
                || isEquippedInOptionalSlot(player);
    }

    public static boolean hasMutualContract(PlayerEntity first, PlayerEntity second) {
        return !first.getUuid().equals(second.getUuid())
                && hasMutualEquipment(isEquipped(first), isEquipped(second));
    }

    static boolean hasMutualEquipment(boolean firstEquipped, boolean secondEquipped) {
        return firstEquipped && secondEquipped;
    }

    @ExpectPlatform
    private static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        throw new AssertionError();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private AcornRingEquipment() {}
}
