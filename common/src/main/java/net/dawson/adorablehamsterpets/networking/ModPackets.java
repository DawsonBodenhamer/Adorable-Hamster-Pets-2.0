package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.util.HamsterInteractionUtil;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
        NetworkManager.registerS2CPayloadType(SyncHamsterStatePayload.ID, SyncHamsterStatePayload.CODEC);
        NetworkManager.registerS2CPayloadType(PlayDistantSoundPayload.ID, PlayDistantSoundPayload.CODEC);
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

                    // Deliver guidebook: no advancement, no fallback message, play effect, close the config screen
                    AdorableHamsterPets.deliverGuidebook(player, false, false, true, true);

                    // Set cache
                    ((PlayerEntityAccessor) player).ahp$initGuideBookTracking(true);
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RequestHamsterMountPayload.ID, RequestHamsterMountPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    PlayerEntity player = context.getPlayer();
                    Entity entity = player.getWorld().getEntityById(payload.entityId());
                    if (entity instanceof HamsterEntity hamster && hamster.isOwner(player)) {
                        // Distance check for security
                        if (hamster.squaredDistanceTo(player) < 64.0) {
                            HamsterInteractionUtil.executeShoulderMount(hamster, player, ItemStack.EMPTY); // Pass empty stack for force-mount
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ResetHeistHistoryPayload.ID, ResetHeistHistoryPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$clearHeistHistory();
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RequestHamsterRidePayload.ID, RequestHamsterRidePayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!Configs.AHP.enableMountableHamsters.get()) {
                        return;
                    }

                    PlayerEntity player = context.getPlayer();
                    Entity entity = player.getWorld().getEntityById(payload.entityId());

                    if (entity instanceof HamsterEntity hamster) {
                        if (hamster.squaredDistanceTo(player) < 64.0) {
                            hamster.putPlayerOnBack(player);
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, HamsterInputPayload.ID, HamsterInputPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!Configs.AHP.enableMountableHamsters.get()) return;

                    if (context.getPlayer().getVehicle() instanceof HamsterEntity hamster) {
                        if (hamster.getControllingPassenger() == context.getPlayer()) {
                            hamster.setRiderInput(payload.jumpHeld(), payload.sprintHeld());
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RenameHamsterPayload.ID, RenameHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!Configs.AHP.enableGuiRenaming) return;

                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        Entity entity = player.getWorld().getEntityById(payload.entityId());
                        // Ensure player owns hamster and is close enough
                        if (entity instanceof HamsterEntity hamster && hamster.isOwner(player) && hamster.squaredDistanceTo(player) < 64.0) {
                            String newName = payload.newName().trim();
                            boolean canRename = true;

                            // Process name tag sacrifice if configured
                            if (Configs.AHP.consumeNameTagForGuiRename) {
                                canRename = HamsterInteractionUtil.consumeNameTag(player, hamster);
                            }

                            if (canRename) {
                                if (newName.isEmpty()) {
                                    hamster.setCustomName(null);
                                } else {
                                    hamster.setCustomName(Text.literal(newName));

                                    // Manually grant Sweet Potato advancement for easter egg
                                    if (hamster.isSweetPotato()) {
                                        PlayerAdvancementTracker tracker = player.getAdvancementTracker();
                                        Identifier advId = Identifier.of(AdorableHamsterPets.MOD_ID, "technical/sweet_potato_named");
                                        net.minecraft.advancement.AdvancementEntry adv = player.server.getAdvancementLoader().get(advId);

                                        if (adv != null) {
                                            for (String criterion : adv.value().criteria().keySet()) {
                                                tracker.grantCriterion(adv, criterion);
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.queueGuidebookEffects(payload))
        );

        // Handle the Shoulder Data Sync
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncHamsterStatePayload.ID, SyncHamsterStatePayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    // Client-side logic to apply the NBT
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.world != null) {
                        Entity entity = client.world.getEntityById(payload.entityId());
                        // Check if the entity is a player and has my accessor
                        if (entity instanceof PlayerEntity && entity instanceof PlayerEntityAccessor accessor) {
                            accessor.adorablehamsterpets$setRawHamsterState(payload.data());
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PlayDistantSoundPayload.ID, PlayDistantSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handlePlayDistantSound(payload))
        );
    }

    private static void handleUpdateRenderState(UpdateHamsterRenderStatePayload payload, NetworkManager.PacketContext context) {
        for (int id : payload.hamsterEntityIds()) {
            if (payload.isRendering()) {
                HamsterRenderTracker.addPlayer(id, context.getPlayer().getUuid());
            } else {
                HamsterRenderTracker.removePlayer(id, context.getPlayer().getUuid());
            }
        }
    }
}