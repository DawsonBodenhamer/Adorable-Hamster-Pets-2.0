package net.dawson.adorablehamsterpets.util.forge;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.integration.curios.forge.CuriosLifecycleAdapter;
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
        if (Platform.isModLoaded("curios")) {
            CuriosLifecycleAdapter.registerCallbacks();
        }
    }

    public static boolean reconcilePlatform(
            ServerPlayerEntity player,
            @Nullable AcornRingLocation preferredLocation,
            Set<UUID> removedIdentities) {
        List<AcornRingLifecycleUtil.EquippedRing> equippedRings = new ArrayList<>();

        if (Platform.isModLoaded("curios")
                && !CuriosLifecycleAdapter.collect(player, equippedRings)) {
            return false;
        }

        AcornRingLifecycleUtil.reconcile(player, equippedRings, removedIdentities, preferredLocation);
        return true;
    }

    private AcornRingEquipmentLifecycleImpl() {}
}
