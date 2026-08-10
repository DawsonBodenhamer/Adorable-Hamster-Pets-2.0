package net.dawson.adorablehamsterpets.util.neoforge;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.integration.accessories.neoforge.AccessoriesEquipmentAdapter;
import net.dawson.adorablehamsterpets.integration.accessories.neoforge.AccessoriesLifecycleAdapter;
import net.dawson.adorablehamsterpets.integration.curios.neoforge.CuriosEquipmentAdapter;
import net.dawson.adorablehamsterpets.integration.curios.neoforge.CuriosLifecycleAdapter;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AcornRingUtilImpl {

    public static boolean isEquippedInOptionalSlot(PlayerEntity player) {
        boolean curiosAvailable = Platform.isModLoaded("curios");
        boolean curiosEquipped = curiosAvailable
                && CuriosEquipmentAdapter.isAcornRingEquipped(player);
        boolean accessoriesAvailable = Platform.isModLoaded("accessories");
        boolean accessoriesEquipped = accessoriesAvailable
                && AccessoriesEquipmentAdapter.isAcornRingEquipped(player);
        return AcornRingUtil.hasSupportedOptionalEquipment(
                curiosAvailable,
                curiosEquipped,
                accessoriesAvailable,
                accessoriesEquipped);
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
        if (Platform.isModLoaded("accessories")
                && !AccessoriesLifecycleAdapter.collect(player, equippedRings)) {
            return false;
        }

        AcornRingUtil.reconcile(player, equippedRings, removedIdentities, preferredLocation);
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
