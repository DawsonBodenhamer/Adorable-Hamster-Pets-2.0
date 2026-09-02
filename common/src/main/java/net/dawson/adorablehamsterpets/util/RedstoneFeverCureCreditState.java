package net.dawson.adorablehamsterpets.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Cure credits owed to players who were offline when their hamster was cured.
 *
 * <p>26.2 port: SavedData no longer has save/load hooks; persistence is a
 * {@link SavedDataType} with a {@link Codec}. The NBT shape stays the same
 * (a "Players" list of UUIDs), only the plumbing changed.
 */
public final class RedstoneFeverCureCreditState extends SavedData {

    private static final Codec<RedstoneFeverCureCreditState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC_SET.fieldOf("Players").forGetter(state -> state.pendingPlayers)
    ).apply(instance, RedstoneFeverCureCreditState::new));

    private static final SavedDataType<RedstoneFeverCureCreditState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "redstone_fever_cure_credit"),
            RedstoneFeverCureCreditState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Set<UUID> pendingPlayers;

    public RedstoneFeverCureCreditState() {
        this(new HashSet<>());
    }

    private RedstoneFeverCureCreditState(Set<UUID> pendingPlayers) {
        this.pendingPlayers = new HashSet<>(pendingPlayers);
    }

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
        RedstoneFeverCureCreditState state = get((ServerLevel) player.level());
        if (state.pendingPlayers.remove(player.getUUID())) {
            ModCriteria.SUNSHINE_CURING.get().trigger(player);
            state.setDirty();
        }
    }

    private static RedstoneFeverCureCreditState get(ServerLevel world) {
        // Overworld manager shares one queue across every cure dimension
        return world.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
