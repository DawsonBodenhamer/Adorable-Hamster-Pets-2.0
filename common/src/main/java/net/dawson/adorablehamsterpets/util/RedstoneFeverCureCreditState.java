package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Server-wide pending advancement credit for players offline when sunlight curing completes.
 */
public final class RedstoneFeverCureCreditState extends SavedData {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ─────────────────────────────────────────────────────────────────────────────*/

    private static final String STORAGE_KEY = "adorablehamsterpets_redstone_fever_cure_credit";
    private static final Factory<RedstoneFeverCureCreditState> TYPE = new Factory<>(
            RedstoneFeverCureCreditState::new,
            RedstoneFeverCureCreditState::fromNbt,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ─────────────────────────────────────────────────────────────────────────────*/

    private final Set<UUID> pendingPlayers = new HashSet<>();

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ─────────────────────────────────────────────────────────────────────────────*/

    public static void awardOrQueue(ServerLevel world, UUID playerUuid) {
        ServerPlayer player = world.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            // Online credit resolves immediately and never enters persistent queue
            ModCriteria.SUNSHINE_CURING.get().trigger(player);
            return;
        }
        RedstoneFeverCureCreditState state = get(world);
        if (state.pendingPlayers.add(playerUuid)) state.setDirty();
    }

    public static void consume(ServerPlayer player) {
        // Set removal makes reconnect consumption idempotent
        RedstoneFeverCureCreditState state = get(player.serverLevel());
        if (state.pendingPlayers.remove(player.getUUID())) {
            ModCriteria.SUNSHINE_CURING.get().trigger(player);
            state.setDirty();
        }
    }

    /* ────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ───────────────────────────────────────────────────────────────────────────────*/

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (UUID uuid : this.pendingPlayers) players.add(StringTag.valueOf(uuid.toString()));
        nbt.put("Players", players);
        return nbt;
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────────*/

    private static RedstoneFeverCureCreditState get(ServerLevel world) {
        // Overworld manager shares one queue across every cure dimension
        return world.getServer().overworld().getDataStorage().computeIfAbsent(TYPE, STORAGE_KEY);
    }

    private static RedstoneFeverCureCreditState fromNbt(
            CompoundTag nbt, HolderLookup.Provider registries) {
        RedstoneFeverCureCreditState state = new RedstoneFeverCureCreditState();
        ListTag players = nbt.getList("Players", Tag.TAG_STRING);
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
