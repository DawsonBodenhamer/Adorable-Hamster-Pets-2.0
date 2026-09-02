package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DismountHamsterPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DismountHamsterPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "dismount_hamster"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DismountHamsterPayload> CODEC = StreamCodec.unit(new DismountHamsterPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}