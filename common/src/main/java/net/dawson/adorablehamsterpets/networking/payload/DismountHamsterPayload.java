package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DismountHamsterPayload() implements CustomPayload {
    public static final CustomPayload.Id<DismountHamsterPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "dismount_hamster"));
    public static final PacketCodec<RegistryByteBuf, DismountHamsterPayload> CODEC = PacketCodec.unit(new DismountHamsterPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}