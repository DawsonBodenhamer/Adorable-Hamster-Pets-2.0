package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets; // Import main mod class
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier; // Import Identifier

public record ThrowHamsterPayload() implements CustomPayload {
    public static final CustomPayload.Id<ThrowHamsterPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "throw_hamster"));
    public static final PacketCodec<RegistryByteBuf, ThrowHamsterPayload> CODEC = PacketCodec.unit(new ThrowHamsterPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}