package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AdjustGeneticsConfigPayload(boolean isVariance, boolean increase) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AdjustGeneticsConfigPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "adjust_genetics_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdjustGeneticsConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AdjustGeneticsConfigPayload::isVariance,
            ByteBufCodecs.BOOL, AdjustGeneticsConfigPayload::increase,
            AdjustGeneticsConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}