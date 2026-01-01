package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ResetHeistHistoryPayload() implements CustomPayload {
    public static final CustomPayload.Id<ResetHeistHistoryPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "reset_heist_history"));
    public static final PacketCodec<RegistryByteBuf, ResetHeistHistoryPayload> CODEC = PacketCodec.unit(new ResetHeistHistoryPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}