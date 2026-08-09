package net.dawson.adorablehamsterpets.integration.trinkets.fabric;

import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import dev.emi.trinkets.api.event.TrinketEquipCallback;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingEquipment;
import net.dawson.adorablehamsterpets.util.AcornRingEquipmentLifecycle;
import net.dawson.adorablehamsterpets.util.AcornRingLifecycleUtil;
import net.dawson.adorablehamsterpets.util.AcornRingLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Isolates direct Trinkets lifecycle linkage so the class never loads when Trinkets is absent.
 */
public final class TrinketsLifecycleAdapter {

    public static boolean collect(
            ServerPlayerEntity player,
            List<AcornRingLifecycleUtil.EquippedRing> equippedRings) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> {
                    var group = component.getInventory().get("hand");
                    if (group == null) {
                        return true;
                    }

                    var ringInventory = group.get("ring");
                    if (ringInventory == null) {
                        return true;
                    }

                    for (int index = 0; index < ringInventory.size(); index++) {
                        var stack = ringInventory.getStack(index);
                        if (stack.isOf(ModItems.ACORN_RING.get())) {
                            int slotIndex = index;
                            equippedRings.add(new AcornRingLifecycleUtil.EquippedRing(
                                    AcornRingLocation.TRINKETS_HAND_RING,
                                    stack,
                                    replacement -> ringInventory.setStack(slotIndex, replacement)));
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    public static void registerCallbacks() {
        TrinketEquipCallback.EVENT.register((stack, slot, entity) -> {
            if (isSupportedRing(slot) && entity instanceof ServerPlayerEntity player
                    && stack.isOf(ModItems.ACORN_RING.get())) {
                AcornRingEquipmentLifecycle.reconcileImmediately(
                        player,
                        AcornRingLocation.TRINKETS_HAND_RING,
                        stack);
            }
        });
        TrinketUnequipCallback.EVENT.register((stack, slot, entity) -> {
            if (isSupportedRing(slot) && entity instanceof ServerPlayerEntity player
                    && stack.isOf(ModItems.ACORN_RING.get())) {
                AcornRingEquipmentLifecycle.defer(player, stack);
            }
        });
        TrinketDropCallback.EVENT.register((rule, stack, reference, entity) -> {
            if (isSupportedRing(reference) && entity instanceof ServerPlayerEntity player
                    && stack.isOf(ModItems.ACORN_RING.get())) {
                AcornRingEquipmentLifecycle.defer(player, stack);
            }
            return rule;
        });
    }

    private static boolean isSupportedRing(dev.emi.trinkets.api.SlotReference reference) {
        var slotType = reference.inventory().getSlotType();
        return AcornRingEquipment.isSupportedTrinketsSlot(slotType.getGroup(), slotType.getName());
    }

    private TrinketsLifecycleAdapter() {}
}
