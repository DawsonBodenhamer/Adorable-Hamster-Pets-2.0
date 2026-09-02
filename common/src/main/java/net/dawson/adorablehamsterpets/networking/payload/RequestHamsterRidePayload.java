package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestHamsterRidePayload(int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestHamsterRidePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "request_hamster_ride"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestHamsterRidePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestHamsterRidePayload::entityId,
            RequestHamsterRidePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}