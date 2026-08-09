package net.dawson.adorablehamsterpets.util.neoforge;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.accessories.neoforge.AccessoriesEquipmentAdapter;
import net.dawson.adorablehamsterpets.integration.curios.neoforge.CuriosEquipmentAdapter;
import net.dawson.adorablehamsterpets.util.AcornRingEquipment;
import net.minecraft.entity.player.PlayerEntity;

public final class AcornRingEquipmentImpl {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        boolean curiosAvailable = Platform.isModLoaded("curios");
        boolean curiosEquipped = curiosAvailable
                && CuriosEquipmentAdapter.isAcornRingEquipped(player);
        boolean accessoriesAvailable = Platform.isModLoaded("accessories");
        boolean accessoriesEquipped = accessoriesAvailable
                && AccessoriesEquipmentAdapter.isAcornRingEquipped(player);
        return AcornRingEquipment.hasSupportedOptionalEquipment(
                curiosAvailable,
                curiosEquipped,
                accessoriesAvailable,
                accessoriesEquipped);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private AcornRingEquipmentImpl() {}
}
