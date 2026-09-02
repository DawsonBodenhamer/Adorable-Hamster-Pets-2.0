package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CancelPettingPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CancelPettingPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "cancel_petting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelPettingPayload> CODEC = StreamCodec.unit(new CancelPettingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}