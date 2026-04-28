package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestPetHamsterPayload(int entityId) implements CustomPayload {
    public static final CustomPayload.Id<RequestPetHamsterPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "request_pet_hamster"));

    public static final PacketCodec<RegistryByteBuf, RequestPetHamsterPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, RequestPetHamsterPayload::entityId,
            RequestPetHamsterPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}