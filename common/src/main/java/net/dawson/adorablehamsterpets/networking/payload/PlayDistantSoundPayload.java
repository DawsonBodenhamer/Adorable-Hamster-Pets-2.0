package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayDistantSoundPayload(Identifier soundId, float volume, float pitch) implements CustomPayload {
    public static final CustomPayload.Id<PlayDistantSoundPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "play_distant_sound"));

    public static final PacketCodec<RegistryByteBuf, PlayDistantSoundPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, PlayDistantSoundPayload::soundId,
            PacketCodecs.FLOAT, PlayDistantSoundPayload::volume,
            PacketCodecs.FLOAT, PlayDistantSoundPayload::pitch,
            PlayDistantSoundPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}