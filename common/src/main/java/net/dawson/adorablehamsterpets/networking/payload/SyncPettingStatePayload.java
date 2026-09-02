package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncPettingStatePayload(boolean isPetting) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPettingStatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "sync_petting_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPettingStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncPettingStatePayload::isPetting,
            SyncPettingStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}