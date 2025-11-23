package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HamsterAnimationSoundPayload(int hamsterEntityId, String soundId) implements CustomPayload {
    public static final CustomPayload.Id<HamsterAnimationSoundPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_animation_sound"));

    public static final PacketCodec<RegistryByteBuf, HamsterAnimationSoundPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, HamsterAnimationSoundPayload::hamsterEntityId,
            PacketCodecs.STRING, HamsterAnimationSoundPayload::soundId,
            HamsterAnimationSoundPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}