package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateHamsterRenderStatePayload(int hamsterEntityId, boolean isRendering) implements CustomPayload {
    public static final CustomPayload.Id<UpdateHamsterRenderStatePayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "update_hamster_render_state"));

    public static final PacketCodec<RegistryByteBuf, UpdateHamsterRenderStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, UpdateHamsterRenderStatePayload::hamsterEntityId,
            PacketCodecs.BOOL, UpdateHamsterRenderStatePayload::isRendering,
            UpdateHamsterRenderStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}