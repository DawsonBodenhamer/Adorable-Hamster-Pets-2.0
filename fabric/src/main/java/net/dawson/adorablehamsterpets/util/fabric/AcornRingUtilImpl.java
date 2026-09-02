package net.dawson.adorablehamsterpets.util.fabric;

import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * 26.2 port: neither Trinkets nor Accessories has a 26.2 build, so the ring is
 * only ever tracked through the vanilla inventory paths in AcornRingUtil.
 */
public final class AcornRingUtilImpl {

    public static boolean isEquippedInOptionalSlot(Player player) {
        return AcornRingUtil.hasSupportedOptionalEquipment(false, false, false, false);
    }

    public static void registerPlatformCallbacks() {
        // no optional equipment mods on 26.2
    }

    public static boolean reconcilePlatform(
            ServerPlayer player,
            @Nullable AcornRingUtil.Location preferredLocation,
            Set<UUID> removedIdentities) {
        AcornRingUtil.reconcile(player, new ArrayList<>(), removedIdentities, preferredLocation);
        return true;
    }

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

    private AcornRingUtilImpl() {}
}
