package net.dawson.adorablehamsterpets.integration.trinkets.fabric;

import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Isolates direct Trinkets lifecycle linkage so the class never loads when Trinkets is absent.
 */
public final class TrinketsLifecycleAdapter {

    public static boolean collect(
            ServerPlayerEntity player,
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

                    for (int index = 0; index < ringInventory.size(); index++) {
                        var stack = ringInventory.getStack(index);
                        if (stack.isOf(ModItems.ACORN_RING.get())) {
                            int slotIndex = index;
                            equippedRings.add(new AcornRingUtil.EquippedRing(
                                    AcornRingUtil.Location.TRINKETS_HAND_RING,
                                    stack,
                                    replacement -> ringInventory.setStack(slotIndex, replacement)));
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    public static void registerCallbacks() {
        TrinketDropCallback.EVENT.register((rule, stack, reference, entity) -> {
            if (isSupportedRing(reference) && entity instanceof ServerPlayerEntity player
                    && stack.isOf(ModItems.ACORN_RING.get())) {
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
