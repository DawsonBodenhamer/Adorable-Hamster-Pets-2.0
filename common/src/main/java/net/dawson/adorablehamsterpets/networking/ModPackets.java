package net.dawson.adorablehamsterpets.networking;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.networking.NetworkChannel;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static net.dawson.adorablehamsterpets.AdorableHamsterPets.MOD_ID;

public class ModPackets {

    // --- 1. Create the Network Channel ---
    public static final NetworkChannel CHANNEL = NetworkChannel.create(new Identifier(MOD_ID, "main"));

    // --- 2. Define Packet Data as Records ---
    // C2S (Client-to-Server)
    public record ThrowHamsterC2SPacket() {}
    public record DismountHamsterC2SPacket() {}
    public record UpdateRenderStateC2SPacket(int entityId, boolean isRendering) {}
    public record RequestGuidebookC2SPacket() {}

    // S2C (Server-to-Client)
    public record PlayGuidebookEffectsS2CPacket() {}
    public record SpawnBeddingParticlesS2CPacket(BlockPos pos, Direction direction, WoodVariant variant) {}

    /**
     * Registers all packet types and their handlers using the NetworkChannel API.
     * This method is safe to call from the common initializer, as the API handles
     * client/server separation internally.
     */
    public static void register() {
        // --- C2S Packet Registrations ---
        CHANNEL.register(ThrowHamsterC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new ThrowHamsterC2SPacket(),
                (packet, context) -> context.get().queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.get().getPlayer()))
        );

        CHANNEL.register(DismountHamsterC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new DismountHamsterC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof ServerPlayerEntity player) {
                        ((PlayerEntityAccessor) player).adorablehamsterpets$dismountShoulderHamster(false);
                    }
                })
        );

        CHANNEL.register(UpdateRenderStateC2SPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.entityId());
                    buf.writeBoolean(packet.isRendering());
                },
                (buf) -> new UpdateRenderStateC2SPacket(buf.readInt(), buf.readBoolean()),
                (packet, context) -> context.get().queue(() -> {
                    if (packet.isRendering()) {
                        HamsterRenderTracker.addPlayer(packet.entityId(), context.get().getPlayer().getUuid());
                    } else {
                        HamsterRenderTracker.removePlayer(packet.entityId(), context.get().getPlayer().getUuid());
                    }
                })
        );

        CHANNEL.register(RequestGuidebookC2SPacket.class,
                (packet, buf) -> {}, // No data to encode
                (buf) -> new RequestGuidebookC2SPacket(), // No data to decode
                (packet, context) -> context.get().queue(() -> {
                    ServerPlayerEntity player = (ServerPlayerEntity) context.get().getPlayer();
                    ItemStack bookStack = new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get());

                    // In 1.20.1, add the Patchouli ID to NBT
                    NbtCompound nbt = bookStack.getOrCreateNbt();
                    nbt.putString("patchouli:book", "adorablehamsterpets:hamster_tips_guide_book");

                    player.getInventory().offerOrDrop(bookStack);

                    // Send effects packet back to the player
                    CHANNEL.sendToPlayer(player, new PlayGuidebookEffectsS2CPacket());
                })
        );

        // --- S2C Packet Registrations ---
        CHANNEL.register(SpawnBeddingParticlesS2CPacket.class,
                (packet, buf) -> {
                    buf.writeBlockPos(packet.pos());
                    buf.writeEnumConstant(packet.direction());
                    buf.writeEnumConstant(packet.variant());
                },
                (buf) -> new SpawnBeddingParticlesS2CPacket(
                        buf.readBlockPos(),
                        buf.readEnumConstant(Direction.class),
                        buf.readEnumConstant(WoodVariant.class)
                ),
                (packet, context) -> context.get().queue(() -> AdorableHamsterPetsClient.handleSpawnBeddingParticles(packet))
        );

        // Guidebook Effects Handler for 1.20.1
        CHANNEL.register(PlayGuidebookEffectsS2CPacket.class,
                (packet, buf) -> {}, // No data
                (buf) -> new PlayGuidebookEffectsS2CPacket(), // No data
                (packet, context) -> context.get().queue(AdorableHamsterPetsClient::handlePlayGuidebookEffects)
        );
    }
}