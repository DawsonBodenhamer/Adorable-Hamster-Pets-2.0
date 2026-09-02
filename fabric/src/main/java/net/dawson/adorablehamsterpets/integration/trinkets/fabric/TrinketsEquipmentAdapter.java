package net.dawson.adorablehamsterpets.integration.trinkets.fabric;

import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.Map.Entry;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.world.entity.player.Player;

/**
 * Isolates direct Trinkets API linkage so the class never loads when Trinkets is absent.
 */
public final class TrinketsEquipmentAdapter {

    public static boolean isAcornRingEquipped(Player player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> {
                    for (var groupEntry : component.getInventory().entrySet()) {
                        for (var slotEntry : groupEntry.getValue().entrySet()) {
                            if (!AcornRingUtil.isSupportedTrinketsSlot(groupEntry.getKey(), slotEntry.getKey())) {
                                continue;
                            }

                            var ringInventory = slotEntry.getValue();
                            for (int index = 0; index < ringInventory.getContainerSize(); index++) {
                                if (ringInventory.getItem(index).is(ModItems.ACORN_RING.get())) {
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
