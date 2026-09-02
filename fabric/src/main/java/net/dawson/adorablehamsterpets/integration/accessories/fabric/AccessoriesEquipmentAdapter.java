package net.dawson.adorablehamsterpets.integration.accessories.fabric;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.world.entity.player.Player;

/**
 * Isolates direct Accessories API linkage so the class never loads when Accessories is absent.
 */
public final class AccessoriesEquipmentAdapter {

    public static boolean isAcornRingEquipped(Player player) {
        return AccessoriesCapability.getOptionally(player)
                .map(capability -> capability.getEquipped(ModItems.ACORN_RING.get()).stream()
                        .anyMatch(entry -> entry.reference().slotName().equals("ring")))
                .orElse(false);
    }

    private AccessoriesEquipmentAdapter() {}
}
