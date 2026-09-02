package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets; // Import main mod class
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ThrowHamsterPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ThrowHamsterPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "throw_hamster"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThrowHamsterPayload> CODEC = StreamCodec.unit(new ThrowHamsterPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}