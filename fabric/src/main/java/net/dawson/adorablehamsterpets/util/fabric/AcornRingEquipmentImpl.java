package net.dawson.adorablehamsterpets.util.fabric;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.accessories.fabric.AccessoriesEquipmentAdapter;
import net.dawson.adorablehamsterpets.integration.trinkets.fabric.TrinketsEquipmentAdapter;
import net.dawson.adorablehamsterpets.util.AcornRingEquipment;
import net.minecraft.entity.player.PlayerEntity;

public final class AcornRingEquipmentImpl {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        boolean trinketsAvailable = Platform.isModLoaded("trinkets");
        boolean trinketsEquipped = trinketsAvailable
                && TrinketsEquipmentAdapter.isAcornRingEquipped(player);
        boolean accessoriesAvailable = Platform.isModLoaded("accessories");
        boolean accessoriesEquipped = accessoriesAvailable
                && AccessoriesEquipmentAdapter.isAcornRingEquipped(player);
        return AcornRingEquipment.hasSupportedOptionalEquipment(
                trinketsAvailable,
                trinketsEquipped,
                accessoriesAvailable,
                accessoriesEquipped);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private AcornRingEquipmentImpl() {}
}
