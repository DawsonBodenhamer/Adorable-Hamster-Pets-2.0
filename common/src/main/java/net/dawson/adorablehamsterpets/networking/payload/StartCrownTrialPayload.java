package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartCrownTrialPayload(int themeOrdinal) implements CustomPayload {
    public static final CustomPayload.Id<StartCrownTrialPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "start_supporter_crown_trial"));

    public static final PacketCodec<RegistryByteBuf, StartCrownTrialPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, StartCrownTrialPayload::themeOrdinal,
            StartCrownTrialPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}