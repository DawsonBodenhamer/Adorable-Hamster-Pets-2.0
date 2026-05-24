package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AcknowledgeGuidebookWarningPayload() implements CustomPayload {
    public static final CustomPayload.Id<AcknowledgeGuidebookWarningPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "acknowledge_guidebook_warning"));
    public static final PacketCodec<RegistryByteBuf, AcknowledgeGuidebookWarningPayload> CODEC = PacketCodec.unit(new AcknowledgeGuidebookWarningPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}