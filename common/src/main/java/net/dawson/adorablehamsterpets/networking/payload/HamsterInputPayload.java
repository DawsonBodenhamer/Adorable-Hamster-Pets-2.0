package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HamsterInputPayload(boolean jumpHeld, boolean sprintHeld) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HamsterInputPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HamsterInputPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HamsterInputPayload::jumpHeld,
            ByteBufCodecs.BOOL, HamsterInputPayload::sprintHeld,
            HamsterInputPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}