package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CancelPettingPayload() implements CustomPayload {
    public static final CustomPayload.Id<CancelPettingPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "cancel_petting"));
    public static final PacketCodec<RegistryByteBuf, CancelPettingPayload> CODEC = PacketCodec.unit(new CancelPettingPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}