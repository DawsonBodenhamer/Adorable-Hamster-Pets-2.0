package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload sent from Server to Client to trigger the Guidebook "discovery" effects
 * (Sound, Particles, Action Bar Message).
 *
 * @param closeScreen If true, the client will close the current screen (used for the Config button).
 *                    If false, the screen remains open (used for Creative Menu/general acquisition).
 */
public record PlayGuidebookEffectsPayload(boolean closeScreen) implements CustomPayload {
    public static final CustomPayload.Id<PlayGuidebookEffectsPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "play_guidebook_effects"));

    public static final PacketCodec<RegistryByteBuf, PlayGuidebookEffectsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, PlayGuidebookEffectsPayload::closeScreen,
            PlayGuidebookEffectsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}