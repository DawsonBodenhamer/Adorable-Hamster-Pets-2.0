package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StartCrownTrialPayload(int themeOrdinal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StartCrownTrialPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "start_supporter_crown_trial"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartCrownTrialPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StartCrownTrialPayload::themeOrdinal,
            StartCrownTrialPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}