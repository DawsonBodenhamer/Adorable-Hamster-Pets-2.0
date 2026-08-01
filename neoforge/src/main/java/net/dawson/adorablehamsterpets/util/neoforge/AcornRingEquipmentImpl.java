package net.dawson.adorablehamsterpets.util.neoforge;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.accessories.neoforge.AccessoriesEquipmentAdapter;
import net.dawson.adorablehamsterpets.integration.curios.neoforge.CuriosEquipmentAdapter;
import net.minecraft.entity.player.PlayerEntity;

public final class AcornRingEquipmentImpl {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        // Prefer the loader-native view; the boolean result inherently deduplicates bridged slots.
        if (Platform.isModLoaded("curios")
                && CuriosEquipmentAdapter.isAcornRingEquipped(player)) {
            return true;
        }
        return Platform.isModLoaded("accessories")
                && AccessoriesEquipmentAdapter.isAcornRingEquipped(player);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private AcornRingEquipmentImpl() {}
}
