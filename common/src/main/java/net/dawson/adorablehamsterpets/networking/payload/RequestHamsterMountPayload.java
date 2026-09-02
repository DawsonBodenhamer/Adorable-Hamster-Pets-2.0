package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestHamsterMountPayload(int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestHamsterMountPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "request_hamster_mount"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestHamsterMountPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestHamsterMountPayload::entityId,
            RequestHamsterMountPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}