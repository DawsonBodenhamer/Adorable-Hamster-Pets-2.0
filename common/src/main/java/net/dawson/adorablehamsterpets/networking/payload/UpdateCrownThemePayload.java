package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateCrownThemePayload(int themeOrdinal) implements CustomPayload {
    public static final CustomPayload.Id<UpdateCrownThemePayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "update_supporter_crown_theme"));

    public static final PacketCodec<RegistryByteBuf, UpdateCrownThemePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, UpdateCrownThemePayload::themeOrdinal,
            UpdateCrownThemePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}