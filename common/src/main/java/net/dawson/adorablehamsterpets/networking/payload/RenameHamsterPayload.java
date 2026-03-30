package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RenameHamsterPayload(int entityId, String newName) implements CustomPayload {
    public static final CustomPayload.Id<RenameHamsterPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "rename_hamster"));

    public static final PacketCodec<RegistryByteBuf, RenameHamsterPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, RenameHamsterPayload::entityId,
            PacketCodecs.STRING, RenameHamsterPayload::newName,
            RenameHamsterPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}