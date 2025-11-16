package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayGuidebookEffectsPayload() implements CustomPayload {
    public static final CustomPayload.Id<PlayGuidebookEffectsPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "play_guidebook_effects"));
    public static final PacketCodec<RegistryByteBuf, PlayGuidebookEffectsPayload> CODEC = PacketCodec.unit(new PlayGuidebookEffectsPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}