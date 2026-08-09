package net.dawson.adorablehamsterpets.integration.curios.neoforge;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingEquipmentLifecycle;
import net.dawson.adorablehamsterpets.util.AcornRingLifecycleUtil;
import net.dawson.adorablehamsterpets.util.AcornRingLocation;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.List;

/**
 * Isolates direct Curios lifecycle linkage so the class never loads when Curios is absent.
 */
public final class CuriosLifecycleAdapter {

    public static boolean collect(
            ServerPlayerEntity player,
            List<AcornRingLifecycleUtil.EquippedRing> equippedRings) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    var ringHandler = handler.getCurios().get("ring");
                    if (ringHandler == null) {
                        return true;
                    }

                    var stacks = ringHandler.getStacks();
                    for (int index = 0; index < stacks.getSlots(); index++) {
                        var stack = stacks.getStackInSlot(index);
                        if (stack.isOf(ModItems.ACORN_RING.get())) {
                            int slotIndex = index;
                            equippedRings.add(new AcornRingLifecycleUtil.EquippedRing(
                                    AcornRingLocation.CURIOS_RING,
                                    stack,
                                    replacement -> handler.setEquippedCurio("ring", slotIndex, replacement)));
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    public static void registerCallbacks() {
        NeoForge.EVENT_BUS.addListener((CurioChangeEvent event) -> {
            if (!event.getIdentifier().equals("ring") || !(event.getEntity() instanceof ServerPlayerEntity player)) {
                return;
            }

            if (event.getTo().isOf(ModItems.ACORN_RING.get())) {
                AcornRingEquipmentLifecycle.reconcileImmediately(
                        player,
                        AcornRingLocation.CURIOS_RING,
                        event.getTo());
            } else if (event.getFrom().isOf(ModItems.ACORN_RING.get())) {
                AcornRingEquipmentLifecycle.defer(player, event.getFrom());
            }
        });
    }

    private CuriosLifecycleAdapter() {}
}
