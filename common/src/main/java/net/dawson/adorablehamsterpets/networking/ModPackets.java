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
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

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
        NetworkManager.registerS2CPayloadType(SpawnBeddingParticlesPayload.ID, SpawnBeddingParticlesPayload.CODEC);
        NetworkManager.registerS2CPayloadType(PlayGuidebookEffectsPayload.ID, PlayGuidebookEffectsPayload.CODEC);
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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RequestGuidebookPayload.ID, RequestGuidebookPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
                    ItemStack bookStack = new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get());
                    @SuppressWarnings("unchecked")
                    ComponentType<Identifier> bookComponent = (ComponentType<Identifier>) Registries.DATA_COMPONENT_TYPE.get(Identifier.of("patchouli", "book"));
                    if (bookComponent != null) {
                        bookStack.set(bookComponent, Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
                        player.getInventory().offerOrDrop(bookStack);

                        // Send effects packet back to the player
                        NetworkManager.sendToPlayer(player, new PlayGuidebookEffectsPayload());
                    }
                })
        );
    }

    /**
     * Registers all S2C (Server-to-Client) packet HANDLERS.
     * This method MUST ONLY be called on the client side.
     */
    public static void registerS2CPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SpawnBeddingParticlesPayload.ID, SpawnBeddingParticlesPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleSpawnBeddingParticles(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PlayGuidebookEffectsPayload.ID, PlayGuidebookEffectsPayload.CODEC,
                (payload, context) -> context.queue(AdorableHamsterPetsClient::handlePlayGuidebookEffects)
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