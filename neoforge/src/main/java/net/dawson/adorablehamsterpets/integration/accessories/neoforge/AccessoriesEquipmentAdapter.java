package net.dawson.adorablehamsterpets.integration.accessories.neoforge;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Isolates direct Accessories API linkage so the class never loads when Accessories is absent.
 */
public final class AccessoriesEquipmentAdapter {

    public static boolean isAcornRingEquipped(PlayerEntity player) {
        return AccessoriesCapability.getOptionally(player)
                .map(capability -> capability.getEquipped(ModItems.ACORN_RING.get()).stream()
                        .anyMatch(entry -> entry.reference().slotName().equals("ring")))
                .orElse(false);
    }

    private AccessoriesEquipmentAdapter() {}
}
