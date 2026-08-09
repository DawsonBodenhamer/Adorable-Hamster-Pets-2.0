package net.dawson.adorablehamsterpets.util.forge;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.curios.forge.CuriosEquipmentAdapter;
import net.dawson.adorablehamsterpets.util.AcornRingEquipment;
import net.minecraft.entity.player.PlayerEntity;

public final class AcornRingEquipmentImpl {

    public static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        boolean curiosAvailable = Platform.isModLoaded("curios");
        boolean curiosEquipped = curiosAvailable
                && CuriosEquipmentAdapter.isAcornRingEquipped(player);
        return AcornRingEquipment.hasSupportedOptionalEquipment(
                curiosAvailable,
                curiosEquipped,
                false,
                false);
    }

    private AcornRingEquipmentImpl() {}
}
