package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestHamsterMountPayload(int entityId) implements CustomPayload {
    public static final CustomPayload.Id<RequestHamsterMountPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "request_hamster_mount"));
    public static final PacketCodec<RegistryByteBuf, RequestHamsterMountPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, RequestHamsterMountPayload::entityId,
            RequestHamsterMountPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}