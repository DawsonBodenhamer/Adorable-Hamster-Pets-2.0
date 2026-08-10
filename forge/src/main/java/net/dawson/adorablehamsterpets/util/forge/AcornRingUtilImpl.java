package net.dawson.adorablehamsterpets.util.forge;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.curios.forge.CuriosEquipmentAdapter;
import net.dawson.adorablehamsterpets.integration.curios.forge.CuriosLifecycleAdapter;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AcornRingUtilImpl {

    private static final String ID_KEY = "adorablehamsterpets:acorn_ring_identity";
    private static final String LOCATION_KEY = "adorablehamsterpets:acorn_ring_last_location";

    public static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        boolean curiosAvailable = Platform.isModLoaded("curios");
        boolean curiosEquipped = curiosAvailable
                && CuriosEquipmentAdapter.isAcornRingEquipped(player);
        return AcornRingUtil.hasSupportedOptionalEquipment(
                curiosAvailable,
                curiosEquipped,
                false,
                false);
    }

    public static void registerPlatformCallbacks() {
        if (Platform.isModLoaded("curios")) {
            CuriosLifecycleAdapter.registerCallbacks();
        }
    }

    public static boolean reconcilePlatform(
            ServerPlayerEntity player,
            @Nullable AcornRingUtil.Location preferredLocation,
            Set<UUID> removedIdentities) {
        List<AcornRingUtil.EquippedRing> equippedRings = new ArrayList<>();

        if (Platform.isModLoaded("curios")
                && !CuriosLifecycleAdapter.collect(player, equippedRings)) {
            return false;
        }

        AcornRingUtil.reconcile(player, equippedRings, removedIdentities, preferredLocation);
        return true;
    }

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

    private AcornRingUtilImpl() {}
}
