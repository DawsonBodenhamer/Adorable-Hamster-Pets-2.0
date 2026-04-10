package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdjustGeneticsConfigPayload(boolean isVariance, boolean increase) implements CustomPayload {
    public static final CustomPayload.Id<AdjustGeneticsConfigPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "adjust_genetics_config"));

    public static final PacketCodec<RegistryByteBuf, AdjustGeneticsConfigPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, AdjustGeneticsConfigPayload::isVariance,
            PacketCodecs.BOOL, AdjustGeneticsConfigPayload::increase,
            AdjustGeneticsConfigPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}