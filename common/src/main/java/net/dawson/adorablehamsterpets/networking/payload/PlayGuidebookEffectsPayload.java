package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload sent from Server to Client to trigger the Guidebook "discovery" effects
 * (Sound, Particles, Action Bar Message).
 *
 * @param closeScreen If true, the client will close the current screen (used for the Config button).
 *                    If false, the screen remains open (used for Creative Menu/general acquisition).
 */
public record PlayGuidebookEffectsPayload(boolean closeScreen) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayGuidebookEffectsPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "play_guidebook_effects"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayGuidebookEffectsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PlayGuidebookEffectsPayload::closeScreen,
            PlayGuidebookEffectsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}