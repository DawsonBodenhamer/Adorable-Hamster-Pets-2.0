package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestGuidebookPayload() implements CustomPayload {
    public static final CustomPayload.Id<RequestGuidebookPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "request_guidebook"));
    public static final PacketCodec<RegistryByteBuf, RequestGuidebookPayload> CODEC = PacketCodec.unit(new RequestGuidebookPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}