package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayShoulderMountSoundPayload(Identifier soundId, float pitch, int delay) implements CustomPayload {
    public static final CustomPayload.Id<PlayShoulderMountSoundPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "play_mount_sound"));

    public static final PacketCodec<RegistryByteBuf, PlayShoulderMountSoundPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, PlayShoulderMountSoundPayload::soundId,
            PacketCodecs.FLOAT, PlayShoulderMountSoundPayload::pitch,
            PacketCodecs.INTEGER, PlayShoulderMountSoundPayload::delay,
            PlayShoulderMountSoundPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}