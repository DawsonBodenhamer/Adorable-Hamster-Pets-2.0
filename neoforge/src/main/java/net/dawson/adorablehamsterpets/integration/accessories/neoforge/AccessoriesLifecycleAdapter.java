package net.dawson.adorablehamsterpets.integration.accessories.neoforge;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotReference;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.AcornRingLifecycleUtil;
import net.dawson.adorablehamsterpets.util.AcornRingLocation;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Isolates direct Accessories lifecycle linkage so the class never loads when Accessories is absent.
 */
public final class AccessoriesLifecycleAdapter {

    public static boolean collect(
            ServerPlayerEntity player,
            List<AcornRingLifecycleUtil.EquippedRing> equippedRings) {
        return AccessoriesCapability.getOptionally(player)
                .map(capability -> {
                    var container = capability.getContainers().get("ring");
                    if (container == null) {
                        return true;
                    }

                    for (int index = 0; index < container.getSize(); index++) {
                        SlotReference reference = container.createReference(index);
                        var stack = reference.getStack();
                        if (stack != null && stack.isOf(ModItems.ACORN_RING.get())) {
                            equippedRings.add(new AcornRingLifecycleUtil.EquippedRing(
                                    AcornRingLocation.ACCESSORIES_RING,
                                    stack,
                                    reference::setStack));
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    private AccessoriesLifecycleAdapter() {}
}
