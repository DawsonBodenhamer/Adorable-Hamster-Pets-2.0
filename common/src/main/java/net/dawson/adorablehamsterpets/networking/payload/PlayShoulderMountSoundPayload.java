package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayShoulderMountSoundPayload(Identifier soundId, float pitch, int delay) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayShoulderMountSoundPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "play_mount_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayShoulderMountSoundPayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PlayShoulderMountSoundPayload::soundId,
            ByteBufCodecs.FLOAT, PlayShoulderMountSoundPayload::pitch,
            ByteBufCodecs.INT, PlayShoulderMountSoundPayload::delay,
            PlayShoulderMountSoundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}