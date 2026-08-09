package net.dawson.adorablehamsterpets.util.fabric;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public final class AcornRingStackIdentityImpl {

    private static final String ID_KEY = "adorablehamsterpets:acorn_ring_identity";
    private static final String LOCATION_KEY = "adorablehamsterpets:acorn_ring_last_location";

    public static UUID getIdPlatform(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.containsUuid(ID_KEY) ? nbt.getUuid(ID_KEY) : null;
    }

    public static void setIdPlatform(ItemStack stack, UUID id) {
        stack.getOrCreateNbt().putUuid(ID_KEY, id);
    }

    public static String getLastLocationPlatform(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(LOCATION_KEY) ? nbt.getString(LOCATION_KEY) : null;
    }

    public static void setLastLocationPlatform(ItemStack stack, String serializedName) {
        if (serializedName.isEmpty()) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(LOCATION_KEY);
            }
        } else {
            stack.getOrCreateNbt().putString(LOCATION_KEY, serializedName);
        }
    }

    private AcornRingStackIdentityImpl() {}
}
