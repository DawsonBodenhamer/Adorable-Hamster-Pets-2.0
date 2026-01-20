package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HamsterInputPayload(boolean jumpHeld, boolean sprintHeld) implements CustomPayload {
    public static final CustomPayload.Id<HamsterInputPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_input"));

    public static final PacketCodec<RegistryByteBuf, HamsterInputPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, HamsterInputPayload::jumpHeld,
            PacketCodecs.BOOL, HamsterInputPayload::sprintHeld,
            HamsterInputPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}