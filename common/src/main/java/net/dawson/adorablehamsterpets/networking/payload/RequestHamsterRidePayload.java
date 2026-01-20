package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestHamsterRidePayload(int entityId) implements CustomPayload {
    public static final CustomPayload.Id<RequestHamsterRidePayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "request_hamster_ride"));

    public static final PacketCodec<RegistryByteBuf, RequestHamsterRidePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, RequestHamsterRidePayload::entityId,
            RequestHamsterRidePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}