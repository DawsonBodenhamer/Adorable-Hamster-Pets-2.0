package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayDistantSoundPayload(ResourceLocation soundId, float volume, float pitch) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayDistantSoundPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "play_distant_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayDistantSoundPayload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PlayDistantSoundPayload::soundId,
            ByteBufCodecs.FLOAT, PlayDistantSoundPayload::volume,
            ByteBufCodecs.FLOAT, PlayDistantSoundPayload::pitch,
            PlayDistantSoundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}