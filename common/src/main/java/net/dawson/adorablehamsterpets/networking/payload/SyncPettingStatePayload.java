package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncPettingStatePayload(boolean isPetting) implements CustomPayload {
    public static final CustomPayload.Id<SyncPettingStatePayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "sync_petting_state"));

    public static final PacketCodec<RegistryByteBuf, SyncPettingStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, SyncPettingStatePayload::isPetting,
            SyncPettingStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}