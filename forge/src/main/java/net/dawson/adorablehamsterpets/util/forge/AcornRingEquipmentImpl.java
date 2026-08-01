package net.dawson.adorablehamsterpets.util.forge;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.curios.forge.CuriosEquipmentAdapter;
import net.minecraft.entity.player.PlayerEntity;

public final class AcornRingEquipmentImpl {

    public static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        return Platform.isModLoaded("curios")
                && CuriosEquipmentAdapter.isAcornRingEquipped(player);
    }

    private AcornRingEquipmentImpl() {}
}
