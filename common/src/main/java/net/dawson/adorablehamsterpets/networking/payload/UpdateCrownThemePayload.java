package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateCrownThemePayload(int themeOrdinal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateCrownThemePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "update_supporter_crown_theme"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCrownThemePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpdateCrownThemePayload::themeOrdinal,
            UpdateCrownThemePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}