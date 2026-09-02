package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RenameHamsterPayload(int entityId, String newName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RenameHamsterPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "rename_hamster"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameHamsterPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RenameHamsterPayload::entityId,
            ByteBufCodecs.STRING_UTF8, RenameHamsterPayload::newName,
            RenameHamsterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}