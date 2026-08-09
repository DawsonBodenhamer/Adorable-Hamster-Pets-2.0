package net.dawson.adorablehamsterpets.util.neoforge;

import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public final class AcornRingStackIdentityImpl {

    public static UUID getIdPlatform(ItemStack stack) {
        return stack.get(ModDataComponentTypes.ACORN_RING_IDENTITY.get());
    }

    public static void setIdPlatform(ItemStack stack, UUID id) {
        stack.set(ModDataComponentTypes.ACORN_RING_IDENTITY.get(), id);
    }

    public static String getLastLocationPlatform(ItemStack stack) {
        return stack.get(ModDataComponentTypes.ACORN_RING_LAST_LOCATION.get());
    }

    public static void setLastLocationPlatform(ItemStack stack, String serializedName) {
        if (serializedName.isEmpty()) {
            stack.remove(ModDataComponentTypes.ACORN_RING_LAST_LOCATION.get());
        } else {
            stack.set(ModDataComponentTypes.ACORN_RING_LAST_LOCATION.get(), serializedName);
        }
    }

    private AcornRingStackIdentityImpl() {}
}
