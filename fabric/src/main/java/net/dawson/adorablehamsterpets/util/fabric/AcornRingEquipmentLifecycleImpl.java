package net.dawson.adorablehamsterpets.util.fabric;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.accessories.fabric.AccessoriesLifecycleAdapter;
import net.dawson.adorablehamsterpets.integration.trinkets.fabric.TrinketsLifecycleAdapter;
import net.dawson.adorablehamsterpets.util.AcornRingEquipmentLifecycle;
import net.dawson.adorablehamsterpets.util.AcornRingLifecycleUtil;
import net.dawson.adorablehamsterpets.util.AcornRingLocation;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AcornRingEquipmentLifecycleImpl {

    public static void registerPlatformCallbacks() {
        if (Platform.isModLoaded("trinkets")) {
            TrinketsLifecycleAdapter.registerCallbacks();
        }
        if (Platform.isModLoaded("accessories")) {
            AccessoriesLifecycleAdapter.registerCallbacks();
        }
    }

    public static boolean reconcilePlatform(
            ServerPlayerEntity player,
            @Nullable AcornRingLocation preferredLocation,
            Set<UUID> removedIdentities) {
        List<AcornRingLifecycleUtil.EquippedRing> equippedRings = new ArrayList<>();

        if (Platform.isModLoaded("trinkets")
                && !TrinketsLifecycleAdapter.collect(player, equippedRings)) {
            return false;
        }
        if (Platform.isModLoaded("accessories")
                && !AccessoriesLifecycleAdapter.collect(player, equippedRings)) {
            return false;
        }

        AcornRingLifecycleUtil.reconcile(player, equippedRings, removedIdentities, preferredLocation);
        return true;
    }

    private AcornRingEquipmentLifecycleImpl() {}
}
