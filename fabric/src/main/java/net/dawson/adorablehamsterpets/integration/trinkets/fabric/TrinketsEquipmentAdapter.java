package net.dawson.adorablehamsterpets.integration.trinkets.fabric;

import dev.emi.trinkets.api.TrinketsApi;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingEquipment;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Isolates direct Trinkets API linkage so the class never loads when Trinkets is absent.
 */
public final class TrinketsEquipmentAdapter {

    public static boolean isAcornRingEquipped(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> {
                    for (var groupEntry : component.getInventory().entrySet()) {
                        for (var slotEntry : groupEntry.getValue().entrySet()) {
                            if (!AcornRingEquipment.isSupportedTrinketsSlot(groupEntry.getKey(), slotEntry.getKey())) {
                                continue;
                            }

                            var ringInventory = slotEntry.getValue();
                            for (int index = 0; index < ringInventory.size(); index++) {
                                if (ringInventory.getStack(index).isOf(ModItems.ACORN_RING.get())) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    private TrinketsEquipmentAdapter() {}
}
