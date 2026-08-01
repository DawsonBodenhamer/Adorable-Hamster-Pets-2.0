package net.dawson.adorablehamsterpets.integration.curios.forge;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import top.theillusivec4.curios.api.CuriosApi;

/** Isolates direct Curios API linkage so the class never loads when Curios is absent. */
public final class CuriosEquipmentAdapter {

    public static boolean isAcornRingEquipped(PlayerEntity player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findCurios(ModItems.ACORN_RING.get()).stream()
                        .anyMatch(result -> result.slotContext().identifier().equals("ring")))
                .orElse(false);
    }

    private CuriosEquipmentAdapter() {}
}
