package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayerKnockbackPayload(double velocityX, double velocityY, double velocityZ) implements CustomPayload {
    public static final CustomPayload.Id<PlayerKnockbackPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "player_knockback"));

    public static final PacketCodec<RegistryByteBuf, PlayerKnockbackPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, PlayerKnockbackPayload::velocityX,
            PacketCodecs.DOUBLE, PlayerKnockbackPayload::velocityY,
            PacketCodecs.DOUBLE, PlayerKnockbackPayload::velocityZ,
            PlayerKnockbackPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}