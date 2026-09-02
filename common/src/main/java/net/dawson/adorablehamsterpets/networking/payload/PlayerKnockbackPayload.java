package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerKnockbackPayload(double velocityX, double velocityY, double velocityZ) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerKnockbackPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "player_knockback"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerKnockbackPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, PlayerKnockbackPayload::velocityX,
            ByteBufCodecs.DOUBLE, PlayerKnockbackPayload::velocityY,
            ByteBufCodecs.DOUBLE, PlayerKnockbackPayload::velocityZ,
            PlayerKnockbackPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}