package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateHamsterArmorVisibilityPayload(int entityId, boolean visible)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateHamsterArmorVisibilityPayload> ID =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "update_hamster_armor_visibility"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateHamsterArmorVisibilityPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UpdateHamsterArmorVisibilityPayload::entityId,
                    ByteBufCodecs.BOOL,
                    UpdateHamsterArmorVisibilityPayload::visible,
                    UpdateHamsterArmorVisibilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
