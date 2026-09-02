package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestPetHamsterPayload(int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestPetHamsterPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "request_pet_hamster"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPetHamsterPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestPetHamsterPayload::entityId,
            RequestPetHamsterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}