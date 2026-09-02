package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestGuidebookPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestGuidebookPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "request_guidebook"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestGuidebookPayload> CODEC = StreamCodec.unit(new RequestGuidebookPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}