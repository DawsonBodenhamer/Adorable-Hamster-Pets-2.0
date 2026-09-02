package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HamsterAnimationSoundPayload(int hamsterEntityId, String soundId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HamsterAnimationSoundPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_animation_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HamsterAnimationSoundPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HamsterAnimationSoundPayload::hamsterEntityId,
            ByteBufCodecs.STRING_UTF8, HamsterAnimationSoundPayload::soundId,
            HamsterAnimationSoundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}