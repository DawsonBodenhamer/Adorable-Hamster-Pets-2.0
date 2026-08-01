package net.dawson.adorablehamsterpets.integration.trinkets.fabric;

import dev.emi.trinkets.api.TrinketsApi;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Isolates direct Trinkets API linkage so the class never loads when Trinkets is absent.
 */
public final class TrinketsEquipmentAdapter {

    public static boolean isAcornRingEquipped(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.getEquipped(ModItems.ACORN_RING.get()).stream()
                        .anyMatch(entry -> {
                            var slotType = entry.getLeft().inventory().getSlotType();
                            return slotType.getGroup().equals("hand")
                                    && slotType.getName().equals("ring");
                        }))
                .orElse(false);
    }

    private TrinketsEquipmentAdapter() {}
}
