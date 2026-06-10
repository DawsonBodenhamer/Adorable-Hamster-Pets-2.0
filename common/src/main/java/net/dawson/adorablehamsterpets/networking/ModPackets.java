package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.ValidatedFieldAccessor;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterInteractionUtil;
import net.dawson.adorablehamsterpets.util.HamsterPhysicsUtil;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

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
        NetworkManager.registerS2CPayloadType(SyncPettingStatePayload.ID, SyncPettingStatePayload.CODEC);
        NetworkManager.registerS2CPayloadType(PlayDistantSoundPayload.ID, PlayDistantSoundPayload.CODEC);
        NetworkManager.registerS2CPayloadType(PlayShoulderMountSoundPayload.ID, PlayShoulderMountSoundPayload.CODEC);
    }

    /**
     * Registers all C2S (Client-to-Server) packet HANDLERS.
     * This is safe to call on the server (and client, though the handlers only run on the server).
     */
    public static void registerC2SPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ThrowHamsterPayload.ID, ThrowHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.getPlayer()))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, HamsterAnimationSoundPayload.ID, HamsterAnimationSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    Entity entity = context.getPlayer().getWorld().getEntityById(payload.hamsterEntityId());
                    if (entity instanceof HamsterEntity hamster) {
                        // Ensure sender is >16 blocks away (squared)
                        if (hamster.squaredDistanceTo(context.getPlayer()) > 256.0) {
                            if ("hamster_thump_sound".equals(payload.soundId())) {

                                // Ignore duplicate packets
                                if (hamster.interactionCooldown <= 0) {
                                    float thumpPitch = 1.0F + hamster.getRandom().nextFloat() * 0.4F;
                                    HamsterPhysicsUtil.broadcastImpactSound(hamster, ModSounds.HAMSTER_THUMP.get(), thumpPitch);

                                    // 5-tick cooldown
                                    hamster.interactionCooldown = 5;
                                }
                            }
                        }
                    }
                })
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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, AcknowledgeGuidebookWarningPayload.ID, AcknowledgeGuidebookWarningPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    String username = context.getPlayer().getGameProfile().getName();
                    if (!Configs.AHP_UI.playersWhoHaveSeenGuidebookWarning.contains(username)) {
                        Configs.AHP_UI.playersWhoHaveSeenGuidebookWarning.add(username);
                        Configs.AHP_MAIN.save();
                    }
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
                    if (!Configs.AHP_MAIN.enableMountableHamsters.get()) {
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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RequestPetHamsterPayload.ID, RequestPetHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$startPettingHamster(payload.entityId());
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, HamsterInputPayload.ID, HamsterInputPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!Configs.AHP_MAIN.enableMountableHamsters.get()) return;

                    if (context.getPlayer().getVehicle() instanceof HamsterEntity hamster) {
                        if (hamster.getControllingPassenger() == context.getPlayer()) {
                            hamster.setRiderInput(payload.jumpHeld(), payload.sprintHeld());
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RenameHamsterPayload.ID, RenameHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!Configs.AHP_MAIN.enableGuiRenaming) return;

                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        Entity entity = player.getWorld().getEntityById(payload.entityId());
                        // Ensure player owns hamster and is close enough
                        if (entity instanceof HamsterEntity hamster && hamster.isOwner(player) && hamster.squaredDistanceTo(player) < 64.0) {
                            String newName = payload.newName().trim();
                            boolean canRename = true;

                            // Process name tag sacrifice if configured
                            if (Configs.AHP_UI.consumeNameTagForGuiRename) {
                                canRename = HamsterInteractionUtil.consumeNameTag(player, hamster);
                            }

                            if (canRename) {
                                if (newName.isEmpty()) {
                                    hamster.setCustomName(null);
                                } else {
                                    hamster.setCustomName(Text.literal(newName));

                                    // Trigger Sweet Potato easter egg effects after GUI closes
                                    if (hamster.isSweetPotato()) {
                                        Runnable easterEggTask = new Runnable() {
                                            @Override
                                            public void run() {
                                                if (player.currentScreenHandler != player.playerScreenHandler) {
                                                    hamster.scheduleTask(hamster.getWorld().getTime() + 5, "sweet_potato_delay", this);
                                                } else {
                                                    player.server.getCommandManager().executeWithPrefix(player.getCommandSource(), "function adorablehamsterpets:technical/sweet_potato_effects");
                                                }
                                            }
                                        };
                                        easterEggTask.run();
                                    }
                                }
                            }
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UpdateCrownThemePayload.ID, UpdateCrownThemePayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof PlayerEntityAccessor player) {
                        player.ahp$setSupporterCrownTheme(payload.themeOrdinal());
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, StartCrownTrialPayload.ID, StartCrownTrialPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;

                        // Prevent users from requesting multiple trials per server by checking the NBT flag
                        if (!accessor.ahp$hasUsedSupporterCrownTrial()) {
                            accessor.ahp$setHasUsedSupporterCrownTrial(true);
                            accessor.ahp$setSupporterCrownTrialTicks(600); // 30 seconds
                            accessor.ahp$setSupporterCrownTheme(payload.themeOrdinal());
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, AdjustGeneticsConfigPayload.ID, AdjustGeneticsConfigPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        if (player.hasPermissionLevel(2)) { // OP required to modify server config
                            if (payload.isVariance()) {
                                double current = Configs.AHP_MAIN.geneticVariance.get();
                                double next = MathHelper.clamp(current + (payload.increase() ? 0.05 : -0.05), 0.0, 1.0);
                                @SuppressWarnings("unchecked")
                                ValidatedFieldAccessor<Double> accessor = (ValidatedFieldAccessor<Double>) (Object) Configs.AHP_MAIN.geneticVariance;
                                accessor.adorablehamsterpets$set(next);
                                player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.genetic_variance_updated", String.format("%.2f", next)).formatted(Formatting.WHITE), true);
                            } else {
                                double current = Configs.AHP_MAIN.geneticMutationRate.get();
                                double next = MathHelper.clamp(current + (payload.increase() ? 0.1 : -0.1), 0.0, 2.0);
                                @SuppressWarnings("unchecked")
                                ValidatedFieldAccessor<Double> accessor = (ValidatedFieldAccessor<Double>) (Object) Configs.AHP_MAIN.geneticMutationRate;
                                accessor.adorablehamsterpets$set(next);
                                player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.genetics_mutation_rate_updated", String.format("%.1f", next)).formatted(Formatting.WHITE), true);
                            }
                            Configs.AHP_MAIN.save();
                        } else {
                            player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.no_permission").formatted(Formatting.RED), true);
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CancelPettingPayload.ID, CancelPettingPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$cancelPettingHamster();
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
                        // Check if the entity is a player and has AHP accessor
                        if (entity instanceof PlayerEntity player && entity instanceof PlayerEntityAccessor accessor) {
                            accessor.adorablehamsterpets$setRawHamsterState(payload.data());
                            ClientShoulderHamsterData.REPLAY_CACHE.put(player.getUuid(), payload.data());
                        }
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PlayDistantSoundPayload.ID, PlayDistantSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handlePlayDistantSound(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncPettingStatePayload.ID, SyncPettingStatePayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (payload.isPetting()) {
                        // Sync client state for 10 seconds
                        AdorableHamsterPetsClient.clientPettingTicks = 200;
                    } else {
                        AdorableHamsterPetsClient.clientPettingTicks = 0;
                    }
                })
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PlayShoulderMountSoundPayload.ID, PlayShoulderMountSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handlePlayMountSound(payload.soundId(), payload.pitch(), payload.delay()))
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