package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Server-wide pending advancement credit for players offline when sunlight curing completes.
 */
public final class RedstoneFeverCureCreditState extends PersistentState {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ─────────────────────────────────────────────────────────────────────────────*/

    private static final String STORAGE_KEY = "adorablehamsterpets_redstone_fever_cure_credit";
    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ─────────────────────────────────────────────────────────────────────────────*/

    private final Set<UUID> pendingPlayers = new HashSet<>();

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ─────────────────────────────────────────────────────────────────────────────*/

    public static void awardOrQueue(ServerWorld world, UUID playerUuid) {
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerUuid);
        if (player != null) {
            // Online credit resolves immediately and never enters persistent queue
            ModCriteria.SUNSHINE_CURING.trigger(player);
            return;
        }
        RedstoneFeverCureCreditState state = get(world);
        if (state.pendingPlayers.add(playerUuid)) state.markDirty();
    }

    public static void consume(ServerPlayerEntity player) {
        // Set removal makes reconnect consumption idempotent
        RedstoneFeverCureCreditState state = get(player.getServerWorld());
        if (state.pendingPlayers.remove(player.getUuid())) {
            ModCriteria.SUNSHINE_CURING.trigger(player);
            state.markDirty();
        }
    }

    /* ────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ───────────────────────────────────────────────────────────────────────────────*/

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList players = new NbtList();
        for (UUID uuid : this.pendingPlayers) players.add(NbtString.of(uuid.toString()));
        nbt.put("Players", players);
        return nbt;
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────────*/

    private static RedstoneFeverCureCreditState get(ServerWorld world) {
        // Overworld manager shares one queue across every cure dimension
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                RedstoneFeverCureCreditState::fromNbt,
                RedstoneFeverCureCreditState::new,
                STORAGE_KEY
        );
    }

    private static RedstoneFeverCureCreditState fromNbt(NbtCompound nbt) {
        RedstoneFeverCureCreditState state = new RedstoneFeverCureCreditState();
        NbtList players = nbt.getList("Players", NbtElement.STRING_TYPE);
        for (int index = 0; index < players.size(); index++) {
            try {
                state.pendingPlayers.add(UUID.fromString(players.getString(index)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy entries without blocking remaining credits
            }
        }
        return state;
    }
}
