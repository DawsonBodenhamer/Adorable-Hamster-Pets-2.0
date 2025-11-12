package net.dawson.adorablehamsterpets.networking;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModPackets {

    /**
     * Registers all packet PAYLOAD TYPES. This is safe to call on both the client and server.
     * It ensures both sides know about the existence and structure (codec) of each packet.
     * The server needs this to know how to encode S2C packets for sending.
     * C2S packet types are registered implicitly when their receiver is registered.
     */
    public static void registerPayloads() {
        // --- S2C Payloads (Server-to-Client) ---
        // This is a crucial step for the server. It learns what these packets are.
        NetworkManager.registerS2CPayloadType(StartHamsterFlightSoundPayload.ID, StartHamsterFlightSoundPayload.CODEC);
        NetworkManager.registerS2CPayloadType(StartHamsterThrowSoundPayload.ID, StartHamsterThrowSoundPayload.CODEC);
        NetworkManager.registerS2CPayloadType(SpawnBeddingParticlesPayload.ID, SpawnBeddingParticlesPayload.CODEC);
    }

    /**
     * Registers all C2S (Client-to-Server) packet HANDLERS.
     * This is safe to call on the server (and client, though the handlers only run on the server).
     */
    public static void registerC2SPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ThrowHamsterPayload.ID, ThrowHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.getPlayer()))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UpdateHamsterRenderStatePayload.ID, UpdateHamsterRenderStatePayload.CODEC,
                (payload, context) -> context.queue(() -> handleUpdateRenderState(payload, context))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DismountHamsterPayload.ID, DismountHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        ((PlayerEntityAccessor) player).adorablehamsterpets$dismountShoulderHamster(false);
                    }
                })
        );
    }

    /**
     * Registers all S2C (Server-to-Client) packet HANDLERS.
     * This method MUST ONLY be called on the client side.
     */
    public static void registerS2CPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StartHamsterFlightSoundPayload.ID, StartHamsterFlightSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleStartFlightSound(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StartHamsterThrowSoundPayload.ID, StartHamsterThrowSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleStartThrowSound(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SpawnBeddingParticlesPayload.ID, SpawnBeddingParticlesPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleSpawnBeddingParticles(payload))
        );
    }

    private static void handleUpdateRenderState(UpdateHamsterRenderStatePayload payload, NetworkManager.PacketContext context) {
        if (payload.isRendering()) {
            HamsterRenderTracker.addPlayer(payload.hamsterEntityId(), context.getPlayer().getUuid());
        } else {
            HamsterRenderTracker.removePlayer(payload.hamsterEntityId(), context.getPlayer().getUuid());
        }
    }
}