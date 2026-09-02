package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ResetHeistHistoryPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResetHeistHistoryPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "reset_tree_economy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResetHeistHistoryPayload> CODEC = StreamCodec.unit(new ResetHeistHistoryPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}