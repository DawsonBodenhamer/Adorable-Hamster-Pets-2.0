package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncHamsterStatePayload(int entityId, CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncHamsterStatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "sync_shoulder_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHamsterStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncHamsterStatePayload::entityId,
            ByteBufCodecs.COMPOUND_TAG, SyncHamsterStatePayload::data,
            SyncHamsterStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}