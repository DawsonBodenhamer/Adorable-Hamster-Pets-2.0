package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record UpdateHamsterRenderStatePayload(List<Integer> hamsterEntityIds, boolean isRendering) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateHamsterRenderStatePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "update_hamster_render_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateHamsterRenderStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), UpdateHamsterRenderStatePayload::hamsterEntityIds,
            ByteBufCodecs.BOOL, UpdateHamsterRenderStatePayload::isRendering,
            UpdateHamsterRenderStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}