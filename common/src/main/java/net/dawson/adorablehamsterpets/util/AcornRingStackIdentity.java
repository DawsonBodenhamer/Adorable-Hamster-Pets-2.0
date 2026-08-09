package net.dawson.adorablehamsterpets.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.item.ItemStack;

import java.util.UUID;

/**
 * Stores the physical-stack identity used while an Acorn Ring is equipped through an optional API.
 */
public final class AcornRingStackIdentity {

    public static UUID getId(ItemStack stack) {
        return getIdPlatform(stack);
    }

    public static AcornRingLocation getLastLocation(ItemStack stack) {
        String serializedName = getLastLocationPlatform(stack);
        return serializedName == null ? null : AcornRingLocation.fromSerializedName(serializedName);
    }

    public static UUID ensureId(ItemStack stack) {
        UUID id = getId(stack);
        if (id == null) {
            id = UUID.randomUUID();
            setId(stack, id);
        }
        return id;
    }

    public static void setId(ItemStack stack, UUID id) {
        setIdPlatform(stack, id);
    }

    public static void setLastLocation(ItemStack stack, AcornRingLocation location) {
        setLastLocationPlatform(stack, location == null ? "" : location.serializedName());
    }

    @ExpectPlatform
    private static UUID getIdPlatform(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static void setIdPlatform(ItemStack stack, UUID id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static String getLastLocationPlatform(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static void setLastLocationPlatform(ItemStack stack, String serializedName) {
        throw new AssertionError();
    }

    private AcornRingStackIdentity() {}
}
