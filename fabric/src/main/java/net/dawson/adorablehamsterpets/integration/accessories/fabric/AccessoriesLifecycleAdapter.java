package net.dawson.adorablehamsterpets.integration.accessories.fabric;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.events.AccessoryChangeCallback;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Isolates direct Accessories lifecycle linkage so the class never loads when Accessories is absent.
 */
public final class AccessoriesLifecycleAdapter {

    public static boolean collect(
            ServerPlayer player,
            List<AcornRingUtil.EquippedRing> equippedRings) {
        return AccessoriesCapability.getOptionally(player)
                .map(capability -> {
                    var container = capability.getContainers().get("ring");
                    if (container == null) {
                        return true;
                    }

                    for (int index = 0; index < container.getSize(); index++) {
                        SlotReference reference = container.createReference(index);
                        var stack = reference.getStack();
                        if (stack != null && stack.is(ModItems.ACORN_RING.get())) {
                            equippedRings.add(new AcornRingUtil.EquippedRing(
                                    AcornRingUtil.Location.ACCESSORIES_RING,
                                    stack,
                                    reference::setStack));
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    public static void registerCallbacks() {
        AccessoryChangeCallback.EVENT.register((previous, current, reference, stateChange) -> {
            if (!reference.slotName().equals("ring") || !(reference.entity() instanceof ServerPlayer player)) {
                return;
            }

            if (current.is(ModItems.ACORN_RING.get())) {
                AcornRingUtil.reconcileImmediately(
                        player,
                        AcornRingUtil.Location.ACCESSORIES_RING,
                        current);
            } else if (previous.is(ModItems.ACORN_RING.get())) {
                AcornRingUtil.defer(player, previous);
            }
        });
        OnDropCallback.EVENT.register((rule, stack, reference, damageSource) -> {
            if (reference.slotName().equals("ring") && reference.entity() instanceof ServerPlayer player
                    && stack.is(ModItems.ACORN_RING.get())) {
                AcornRingUtil.defer(player, stack);
            }
            return rule;
        });
    }

    private AccessoriesLifecycleAdapter() {}
}
