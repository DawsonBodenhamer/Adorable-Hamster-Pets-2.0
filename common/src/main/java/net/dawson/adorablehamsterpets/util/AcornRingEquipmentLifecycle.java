package net.dawson.adorablehamsterpets.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Defers cross-API reconciliation until optional inventories have completed their own lifecycle transfers.
 */
public final class AcornRingEquipmentLifecycle {

    private static final Set<UUID> DEFERRED_PLAYERS = new HashSet<>();
    private static final Map<UUID, Set<UUID>> DEFERRED_REMOVALS = new HashMap<>();

    public static void init() {
        registerPlatformCallbacks();
    }

    public static void defer(ServerPlayerEntity player, @Nullable ItemStack removedStack) {
        if (player.getWorld().isClient()) {
            return;
        }

        UUID playerId = player.getUuid();
        DEFERRED_PLAYERS.add(playerId);
        if (removedStack != null) {
            UUID removedIdentity = AcornRingStackIdentity.getId(removedStack);
            if (removedIdentity != null) {
                DEFERRED_REMOVALS.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(removedIdentity);
            }
        }
    }

    public static void reconcileImmediately(
            ServerPlayerEntity player,
            AcornRingLocation preferredLocation,
            @Nullable ItemStack currentStack) {
        UUID playerId = player.getUuid();
        if (currentStack != null) {
            UUID currentIdentity = AcornRingStackIdentity.getId(currentStack);
            if (currentIdentity != null) {
                Set<UUID> removals = DEFERRED_REMOVALS.get(playerId);
                if (removals != null) {
                    removals.remove(currentIdentity);
                    if (removals.isEmpty()) {
                        DEFERRED_REMOVALS.remove(playerId);
                    }
                }
            }
        }

        DEFERRED_PLAYERS.remove(playerId);
        if (!reconcilePlatform(player, preferredLocation, Set.of())) {
            DEFERRED_PLAYERS.add(playerId);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            Set<UUID> removedIdentities = DEFERRED_REMOVALS.getOrDefault(playerId, Set.of());
            if (reconcilePlatform(player, null, removedIdentities)) {
                DEFERRED_PLAYERS.remove(playerId);
                DEFERRED_REMOVALS.remove(playerId);
            }
        }

        Set<UUID> activePlayers = server.getPlayerManager().getPlayerList().stream()
                .map(ServerPlayerEntity::getUuid)
                .collect(Collectors.toSet());
        DEFERRED_PLAYERS.removeIf(playerId -> !activePlayers.contains(playerId));
        DEFERRED_REMOVALS.keySet().removeIf(playerId -> !activePlayers.contains(playerId));
    }

    @ExpectPlatform
    private static void registerPlatformCallbacks() {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static boolean reconcilePlatform(
            ServerPlayerEntity player,
            @Nullable AcornRingLocation preferredLocation,
            Set<UUID> removedIdentities) {
        throw new AssertionError();
    }

    private AcornRingEquipmentLifecycle() {}
}
