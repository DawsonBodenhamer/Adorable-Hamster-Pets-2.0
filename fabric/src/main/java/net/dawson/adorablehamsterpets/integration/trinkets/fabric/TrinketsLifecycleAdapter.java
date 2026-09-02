package net.dawson.adorablehamsterpets.integration.trinkets.fabric;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import dev.emi.trinkets.api.event.TrinketEquipCallback;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Map;

/**
 * Isolates direct Trinkets lifecycle linkage so the class never loads when Trinkets is absent.
 */
public final class TrinketsLifecycleAdapter {

    public static boolean collect(
            ServerPlayer player,
            List<AcornRingUtil.EquippedRing> equippedRings) {
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

                    for (int index = 0; index < ringInventory.getContainerSize(); index++) {
                        var stack = ringInventory.getItem(index);
                        if (stack.is(ModItems.ACORN_RING.get())) {
                            int slotIndex = index;
                            equippedRings.add(new AcornRingUtil.EquippedRing(
                                    AcornRingUtil.Location.TRINKETS_HAND_RING,
                                    stack,
                                    replacement -> ringInventory.setItem(slotIndex, replacement)));
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    public static void registerCallbacks() {
        TrinketEquipCallback.EVENT.register((stack, slot, entity) -> {
            if (isSupportedRing(slot) && entity instanceof ServerPlayer player
                    && stack.is(ModItems.ACORN_RING.get())) {
                AcornRingUtil.reconcileImmediately(
                        player,
                        AcornRingUtil.Location.TRINKETS_HAND_RING,
                        stack);
            }
        });
        TrinketUnequipCallback.EVENT.register((stack, slot, entity) -> {
            if (isSupportedRing(slot) && entity instanceof ServerPlayer player
                    && stack.is(ModItems.ACORN_RING.get())) {
                AcornRingUtil.defer(player, stack);
            }
        });
        TrinketDropCallback.EVENT.register((rule, stack, reference, entity) -> {
            if (isSupportedRing(reference) && entity instanceof ServerPlayer player
                    && stack.is(ModItems.ACORN_RING.get())) {
                AcornRingUtil.defer(player, stack);
            }
            return rule;
        });
    }

    private static boolean isSupportedRing(dev.emi.trinkets.api.SlotReference reference) {
        var slotType = reference.inventory().getSlotType();
        return AcornRingUtil.isSupportedTrinketsSlot(slotType.getGroup(), slotType.getName());
    }

    private TrinketsLifecycleAdapter() {}
}
