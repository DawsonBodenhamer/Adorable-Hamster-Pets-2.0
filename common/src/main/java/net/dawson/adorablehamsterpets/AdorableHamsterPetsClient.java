package net.dawson.adorablehamsterpets;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import io.netty.buffer.Unpooled;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.client.HamsterBedRenderer;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementHudRenderer;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.command.ModClientCommands;
import net.dawson.adorablehamsterpets.client.event.AHPClientScreenEvents;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconAnimator;
import net.dawson.adorablehamsterpets.client.link.RemoteLinkManager;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.dawson.adorablehamsterpets.client.perk.PlayerPerkManager;
import net.dawson.adorablehamsterpets.client.render.BlockJiggleManager;
import net.dawson.adorablehamsterpets.client.sound.HamsterTreeLoopSoundInstance;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.custom.HamsterBlockHiderEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.integration.iris.IrisIntegration;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.mixin.accessor.ValidatedFieldAccessor;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.*;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.*;

public class AdorableHamsterPetsClient {

    // --- Rendering ---
    private static final Set<Integer> renderedHamsterIdsThisTick = new HashSet<>();
    private static final Set<Integer> renderedHamsterIdsLastTick = new HashSet<>();

    // --- Compatibility ---
    private static final boolean IS_SKIN_LAYERS_3D_LOADED = Platform.isModLoaded("skinlayers3d");

    // --- Guidebook ---
    private static int clientSessionTimer = 0;
    private static boolean pendingGuidebookEffects = false;
    private static int pendingGuidebookEffectsTimer = 0;

    // --- Input & Dismount ---
    private static int doubleTapTimer = 0;
    private static boolean isWaitingForSecondTap = false;
    private static boolean hadShoulderHamsterLastTick = false;
    private static int dismountDebounceTicks = 0;
    private static final int DISMOUNT_DEBOUNCE_DEFAULT = 5;
    private static int dismountKeyHeldTicks = 0;
    private static int crownDoubleTapTimer = 0;
    private static boolean isWaitingForCrownSecondTap = false;

    // --- Petting State ---
    public static int clientPettingTicks = 0;

    // --- Shoulder Mount SFX State ---
    private static int mountSoundDelayTicks = 0;
    private static Identifier pendingMountSoundId = null;
    private static float pendingMountSoundPitch = 1.0f;

    // --- Throw Queue State ---
    public static final int THROW_QUEUE_REQUIRED_TICKS = 15;
    public static boolean isQueuingThrow = false;
    public static int throwQueueTicks = 0;

    // --- Performance Mode State ---
    public static boolean isPerformanceModeEnabled = false;

    // --- Announcement System ---
    private static final AnnouncementHudRenderer announcementHudRenderer = new AnnouncementHudRenderer();
    private static List<AnnouncementManager.PendingNotification> pendingNotifications = Collections.emptyList();
    private static int nextRefreshTicks = 6000; // 5 minutes

    // --- Tree Heist ---
    private static final Map<Integer, HamsterTreeLoopSoundInstance> activeTreeSounds = new HashMap<>();

    // --- Hamster Riding ---
    private static boolean lastJumpDown = false;
    private static boolean lastSprintDown = false;


    /* ──────────────────────────────────────────────────────────────────────────────
     *                       1. Initialization & Registration
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Initializes general client-side features.
     * This includes RenderTypes, Config Events, Item Colors, Packet Receivers,
     * Screen Events, Tick Events, and Keybind Interactions.
     */
    public static void init() {
        // --- Block Render Types ---
        RenderTypeRegistry.register(RenderLayer.getCutout(),
                ModBlocks.GREEN_BEANS_CROP.get(),
                ModBlocks.CUCUMBER_CROP.get(),
                ModBlocks.SUNFLOWER_BLOCK.get(),
                ModBlocks.WILD_CUCUMBER_BUSH.get(),
                ModBlocks.WILD_GREEN_BEAN_BUSH.get(),
                ModBlocks.HAMSTER_BED.get());

        // --- Mod Compatibility Logging ---
        if (IS_SKIN_LAYERS_3D_LOADED) {
            AdorableHamsterPets.LOGGER.info("[AHP Client] 3D Skin Layers detected. Adjusting Supporter Crown radius.");
        }

        // --- Config Reload Listener ---
        ConfigApiJava.event().onUpdateClient((id, config) -> {
            if (id.getNamespace().equals(AdorableHamsterPets.MOD_ID)) {
                // Re-parse cached tags and rules if configs change
                ConfigDataCache.parseConfig();
                ModEntitySpawns.parseConfig();
                ModWorldGeneration.parseConfig();

                // Clear dynamic texture caches
                HamsterTextureUtil.clearCaches();

                // Sync supporter crown theme preference to server
                if (MinecraftClient.getInstance().player != null) {
                    int payloadTheme = Configs.AHP_SUPPORTER.showMyCrown ? Configs.AHP_SUPPORTER.crownTheme.get().ordinal() : -1;
                    ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateCrownThemeC2SPacket(payloadTheme));
                }

                AdorableHamsterPets.LOGGER.info("Reloaded Adorable Hamster Pets config caches on client.");
            }
        });

        // --- Resource Reload Listener ---
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, (SynchronousResourceReloader) manager -> {
            HamsterTextureUtil.clearCaches();
            AdorableHamsterPets.LOGGER.info("Cleared Hamster Texture caches on resource reload.");
        });

        // --- Item Colors ---
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> -1, ModItems.HAMSTER_SPAWN_EGG.get());

        // --- Networking Registration ---
        // On 1.20.1, register all packets on both sides using safe common method
        ModPackets.registerCommonPackets();

        // --- Announcement System ---
        AHPClientScreenEvents.register();

        // --- Event Registrations ---
        ClientTickEvent.CLIENT_POST.register(AdorableHamsterPetsClient::onEndClientTick);
        ClientGuiEvent.RENDER_HUD.register((context, tickDelta) -> announcementHudRenderer.render(context, tickDelta));

        // --- Register Client Commands ---
        ClientCommandRegistrationEvent.EVENT.register(ModClientCommands::register);

        // --- Timers Reset & Sync ---
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            clientSessionTimer = 0;
            ClientParticleManager.INSTANCE.clear();
            ClientShoulderHamsterData.REPLAY_CACHE.clear();
            pendingGuidebookEffects = false;

            // Sync initial supporter crown theme preference to server
            int payloadTheme = Configs.AHP_SUPPORTER.showMyCrown ? Configs.AHP_SUPPORTER.crownTheme.get().ordinal() : -1;
            ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateCrownThemeC2SPacket(payloadTheme));
        });

        // --- Register Tree Heist Sound & Jiggle Logic ---
        EntityEvent.ADD.register((entity, world) -> {
            if (world.isClient()) {
                if (entity instanceof HamsterTreeSearcherEntity hider) {
                    MinecraftClient client = MinecraftClient.getInstance();

                    // Audio feedback
                    HamsterTreeLoopSoundInstance existingSound = activeTreeSounds.get(hider.getId());

                    if (existingSound == null || existingSound.isDone()) {
                        HamsterTreeLoopSoundInstance newSound = new HamsterTreeLoopSoundInstance(hider);
                        client.getSoundManager().play(newSound);
                        activeTreeSounds.put(hider.getId(), newSound);
                    }

                    // Visual feedback
                    BlockJiggleManager.INSTANCE.onHiddenEntityAdded(hider);
                } else if (entity instanceof HamsterBlockHiderEntity hider) {
                    BlockJiggleManager.INSTANCE.onHiddenEntityAdded(hider);
                }
            }
            return EventResult.pass();
        });

        // --- Custom Keybind Interaction ---
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            // Ensure we are on client and main hand to avoid double firing
            if (player.getWorld().isClient && hand == net.minecraft.util.Hand.MAIN_HAND && entity instanceof HamsterEntity hamster) {

                // 1. Force Shoulder Mount
                if (Configs.AHP_MAIN.enableShoulderMountKeybind && ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY.isPressed()) {
                    if (hamster.isTamed() && hamster.isOwner(player)) {
                        // Send a typed packet for 1.20.1
                        ModPackets.CHANNEL.sendToServer(new ModPackets.RequestHamsterMountC2SPacket(hamster.getId()));
                        return EventResult.interruptTrue(); // Cancel default interaction to prevent sitting
                    }
                }

                // 2. Hamster Riding
                if (Configs.AHP_MAIN.enableMountableHamsters.get() && ModKeyBindings.RIDE_HAMSTER_KEY.isPressed()) {
                    // Prevent mounting if already riding
                    if (!hamster.hasPassenger(player)) {
                        // Send a typed packet for 1.20.1
                        ModPackets.CHANNEL.sendToServer(new ModPackets.RequestHamsterRideC2SPacket(hamster.getId()));
                        return EventResult.interruptTrue(); // Cancel default interaction
                    }
                }
            }
            return EventResult.pass();
        });

        // --- Perk & Remote Link Systems ---
        PlayerPerkManager.INSTANCE.refreshManifestOnce();
        RemoteLinkManager.INSTANCE.refreshLinksOnce();

        // --- Iris Integration ---
        IrisIntegration.init();
    }

    /**
     * Registers the block entities. Separate because Forge needs to call it natively.
     */
    public static void initBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), HamsterBedRenderer::new);
    }

    /**
     * Registers the screen factory. Separate because Forge needs to call it natively.
     */
    public static void initScreenHandlers() {
        MenuRegistry.registerScreenFactory(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), HamsterInventoryScreen::new);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                       2. Event Listeners (Tick & Render)
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * The main client-tick event handler.
     * Manages announcement animations, manifest refreshing, custom keybind logic (throwing/dismounting),
     * and render state cleanup to determine if entities are off-screen.
     *
     * @param client The Minecraft client instance.
     */
    private static void onEndClientTick(MinecraftClient client) {
        // --- 1. Block Jiggle Manager ---
        BlockJiggleManager.INSTANCE.clientTick(client);

        // --- 2. Announcement System Logic ---
        boolean isGuiOpen = client.currentScreen != null;
        AnnouncementIconAnimator.INSTANCE.tick(isGuiOpen);

        // Sync Patchouli State (once per session after world load)
        if (client.world != null && !AnnouncementManager.INSTANCE.isPatchouliStateSynced()) {
            AnnouncementManager.INSTANCE.syncPatchouliReadState();
            // Once the sync is successful, also process any deferred read marks from the session.
            if (AnnouncementManager.INSTANCE.isPatchouliStateSynced()) {
                AnnouncementManager.INSTANCE.processDeferredReadMarks();
            }
        }

        if (client.world != null) {
            // Update the cached list of pending notifications once per tick.
            pendingNotifications = AnnouncementManager.INSTANCE.getPendingNotifications();
        }

        // Periodic Manifest Refresh
        if (--nextRefreshTicks <= 0) {
            nextRefreshTicks = 6000; // Reset timer
            AnnouncementManager.INSTANCE.refreshManifest(); // Fire and forget
            AdorableHamsterPets.LOGGER.debug("[AHP Client Tick] Triggered periodic manifest refresh.");
        }

        // --- 3. Input & Game Logic ---
        if (client.player == null || client.world == null) {
            renderedHamsterIdsThisTick.clear();
            renderedHamsterIdsLastTick.clear();
            return;
        }

        // Hamster riding inputs
        boolean ridingHamster = client.player != null && client.player.getVehicle() instanceof HamsterEntity;

        // Only process if enabled and riding
        if (ridingHamster && Configs.AHP_MAIN.enableMountableHamsters.get()) {
            boolean jumpDown = client.options.jumpKey.isPressed();
            boolean sprintDown = client.options.sprintKey.isPressed();

            // If either input changed, send update
            if (jumpDown != lastJumpDown || sprintDown != lastSprintDown) {
                lastJumpDown = jumpDown;
                lastSprintDown = sprintDown;

                // 1. Send Packet
                // Send a typed packet for 1.20.1
                ModPackets.CHANNEL.sendToServer(new ModPackets.HamsterInputC2SPacket(jumpDown, sprintDown));

                // 2. Client-Side Prediction
                HamsterEntity hamster = (HamsterEntity) client.player.getVehicle();
                hamster.setRiderInput(jumpDown, sprintDown);
            }
        } else if (lastJumpDown || lastSprintDown) {
            // Reset state if dismounted while holding buttons
            lastJumpDown = false;
            lastSprintDown = false;
            // Send a typed packet for 1.20.1
            ModPackets.CHANNEL.sendToServer(new ModPackets.HamsterInputC2SPacket(false, false));
        }

        // Handle Genetics Visualizer Config Adjustments
        if (client.player.getMainHandStack().isOf(ModItems.HAMSTER_GUIDE_BOOK.get()) || client.player.getOffHandStack().isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            while (ModKeyBindings.GENETICS_VISUALIZER_VAR_UP_KEY.wasPressed()) {
                // Send typed packets for 1.20.1
                ModPackets.CHANNEL.sendToServer(new ModPackets.AdjustGeneticsConfigC2SPacket(true, true));
            }
            while (ModKeyBindings.GENETICS_VISUALIZER_VAR_DOWN_KEY.wasPressed()) {
                ModPackets.CHANNEL.sendToServer(new ModPackets.AdjustGeneticsConfigC2SPacket(true, false));
            }
            while (ModKeyBindings.GENETICS_VISUALIZER_MUT_UP_KEY.wasPressed()) {
                ModPackets.CHANNEL.sendToServer(new ModPackets.AdjustGeneticsConfigC2SPacket(false, true));
            }
            while (ModKeyBindings.GENETICS_VISUALIZER_MUT_DOWN_KEY.wasPressed()) {
                ModPackets.CHANNEL.sendToServer(new ModPackets.AdjustGeneticsConfigC2SPacket(false, false));
            }
        }

        // Handle Throw Hamster Keybind
        if (ModKeyBindings.THROW_HAMSTER_KEY.isPressed()) {
            final AhpMainConfig currentConfig = AdorableHamsterPets.MAIN_CONFIG;
            if (!currentConfig.enableHamsterThrowing) {
                if (!isQueuingThrow) {
                    client.player.sendMessage(Text.translatable("message.adorablehamsterpets.throwing_disabled"), true);
                    isQueuingThrow = true; // Use flag to debounce message
                }
            } else {
                boolean lookingAtSolidBlock = false;
                if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hitResult = (BlockHitResult) client.crosshairTarget;
                    BlockState targetState = client.world.getBlockState(hitResult.getBlockPos());

                    // Cancel throw if looking directly at a block with collision
                    if (!targetState.getCollisionShape(client.world, hitResult.getBlockPos()).isEmpty()) {
                        lookingAtSolidBlock = true;
                    }
                }

                boolean hasShoulderHamsterClient = ((PlayerEntityAccessor) client.player).hasAnyShoulderHamster();

                if (!lookingAtSolidBlock && hasShoulderHamsterClient) {
                    // --- Check Throw Cooldown ---
                    boolean cooldownActive = false;
                    NbtCompound hamsterData = HamsterInteractionUtil.getNextHamsterToDismountData(client.player);

                    if (hamsterData != null && hamsterData.contains("throwCooldownEndTick")) {
                        long cooldownEnd = hamsterData.getLong("throwCooldownEndTick");
                        if (cooldownEnd > client.world.getTime()) {
                            cooldownActive = true;
                            if (!isQueuingThrow) {
                                long remainingTicks = cooldownEnd - client.world.getTime();
                                long totalSecondsRemaining = Math.max(1L, remainingTicks / 20L);
                                client.player.sendMessage(Text.translatable("message.adorablehamsterpets.throw_cooldown", totalSecondsRemaining).formatted(Formatting.RED), true);
                                isQueuingThrow = true; // Use flag to debounce message
                            }
                        }
                    }

                    if (!cooldownActive) {
                        if (!isQueuingThrow) {
                            isQueuingThrow = true;
                            throwQueueTicks = 0;
                        }
                        throwQueueTicks++;
                    } else {
                        // Prevent queue increment if cooldown active
                        throwQueueTicks = 0;
                    }
                } else {
                    // Reset if they look at a solid block while charging (prioritize tree heist)
                    isQueuingThrow = false;
                    throwQueueTicks = 0;
                }
            }
        } else {
            // Key was released
            if (isQueuingThrow) {
                if (throwQueueTicks >= THROW_QUEUE_REQUIRED_TICKS && AdorableHamsterPets.MAIN_CONFIG.enableHamsterThrowing) {
                    ModPackets.CHANNEL.sendToServer(new ModPackets.ThrowHamsterC2SPacket());
                } else if (throwQueueTicks > 0 && AdorableHamsterPets.MAIN_CONFIG.enableHamsterThrowing && Configs.AHP_UI.enableThrowCancellationWarning) {
                    // --- Trigger Premature Release Warning ---
                    if (client.player != null) {
                        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.2f, 0.5f);

                        MutableText msg = Text.literal("\n").append(Text.translatable("message.adorablehamsterpets.throw_warning.1").formatted(Formatting.RED));

                        // Only show Punchy recommendation if they don't already have it
                        if (!MiscUtil.ModCompatUtil.hasRequiredPunchyVersion()) {
                            msg.append("\n\n").append(Text.translatable("message.adorablehamsterpets.throw_warning.2").formatted(Formatting.WHITE));
                            msg.append("\n\n").append(Text.translatable("message.adorablehamsterpets.throw_warning.punchy_link")
                                    .setStyle(Style.EMPTY
                                            .withColor(Formatting.GOLD)
                                            .withBold(true)
                                            .withUnderline(true)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, RemoteLinkManager.INSTANCE.getLink("punchy_showcase", "https://www.youtube.com/watch?v=YGRdjOTCMHo"))) // Fallback URL
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("message.adorablehamsterpets.throw_warning.punchy_hover")))
                                    ));
                        }

                        msg.append("  ").append(Text.translatable("message.adorablehamsterpets.throw_warning.disable_link")
                                .setStyle(Style.EMPTY
                                        .withColor(Formatting.GRAY)
                                        .withBold(true)
                                        .withUnderline(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ahp_disable_throw_warning"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("message.adorablehamsterpets.throw_warning.disable_hover")))
                                ));
                        msg.append("\n");

                        client.player.sendMessage(msg, false);
                    }
                }
                isQueuingThrow = false;
                throwQueueTicks = 0;
            }
            // Consume buffer to prevent old presses from triggering later
            while (ModKeyBindings.THROW_HAMSTER_KEY.wasPressed()) {}
        }

        // Handle Toggle Performance Mode Keybind
        while (ModKeyBindings.TOGGLE_PERFORMANCE_MODE_KEY.wasPressed()) {
            isPerformanceModeEnabled = !isPerformanceModeEnabled;

            Text message = Text.translatable(
                    isPerformanceModeEnabled ? "message.adorablehamsterpets.performance_mode_enabled" : "message.adorablehamsterpets.performance_mode_disabled"
            ).formatted(isPerformanceModeEnabled ? Formatting.GREEN : Formatting.RED);

            client.player.sendMessage(message, false);
        }

        // Handle Petting Keybind
        boolean cancelTap = client.options.attackKey.wasPressed() || client.options.useKey.wasPressed();
        boolean cancelHeld = client.options.attackKey.isPressed() || client.options.useKey.isPressed();

        int petKeyPresses = 0;
        while (ModKeyBindings.PET_HAMSTER_KEY.wasPressed()) {
            petKeyPresses++;
        }

        if (clientPettingTicks > 0) {
            clientPettingTicks--;

            // If player clicks, attacks, or presses pet key again, cancel immediately
            if (cancelTap || cancelHeld || petKeyPresses > 0) {
                // Typed packet for 1.20.1
                ModPackets.CHANNEL.sendToServer(new ModPackets.CancelPettingC2SPacket());
                clientPettingTicks = 0;
            }
        } else if (petKeyPresses > 0) {
            if (!Platform.isModLoaded("punchy")) {
                client.player.sendMessage(Text.translatable("message.adorablehamsterpets.punchy_missing").formatted(Formatting.RED), true);
            } else if (Configs.AHP_MAIN.enablePetting) {
                // Find nearby tamed hamsters that fit criteria
                Box searchBox = client.player.getBoundingBox().expand(5.0);
                List<HamsterEntity> nearbyHamsters = client.world.getEntitiesByClass(
                        HamsterEntity.class,
                        searchBox,
                        hamster ->
                                hamster.isTamed()
                                        && hamster.isOwner(client.player)
                                        && !hamster.isShoulderPet()
                                        && !hamster.isAiDisabled()
                                        && !hamster.isSleeping()
                                        && !hamster.isKnockedOut()
                                        && !hamster.isSulking()
                                        && !hamster.isCelebratingBaby()
                                        && !hamster.isCelebratingDiamond()
                                        && !hamster.isFrozenMovement()
                );

                for (HamsterEntity hamster : nearbyHamsters) {
                    // Use targeting utility to see if player is looking at it
                    if (EntityTargetingUtil.isLookingAt(client.player, hamster, 5.0, 0)) {
                        // Typed packet for 1.20.1
                        ModPackets.CHANNEL.sendToServer(new ModPackets.RequestPetHamsterC2SPacket(hamster.getId()));

                        // Immediately cancel right after starting if multiple presses registered in single tick
                        if (petKeyPresses > 1) {
                            ModPackets.CHANNEL.sendToServer(new ModPackets.CancelPettingC2SPacket());
                        }

                        break; // Only pet one at a time
                    }
                }
            }
        }

        // --- Handle Toggle Supporter Crown Keybind ---
        if (crownDoubleTapTimer > 0) {
            crownDoubleTapTimer--;
            if (crownDoubleTapTimer == 0 && isWaitingForCrownSecondTap) {
                // --- Single Tap: Cycle Color ---
                isWaitingForCrownSecondTap = false;

                boolean hasPerk = PlayerPerkManager.INSTANCE.hasPerk(client.player.getGameProfile().getName(), "supporter_crown");
                int trialTicks = ((PlayerEntityAccessor) client.player).ahp$getSupporterCrownTrialTicks();
                boolean hasUsedTrial = ((PlayerEntityAccessor) client.player).ahp$hasUsedSupporterCrownTrial();

                if (hasPerk || trialTicks > 0) {
                    PixieDustParticleTheme[] themes = PixieDustParticleTheme.values();
                    int nextOrdinal = (Configs.AHP_SUPPORTER.crownTheme.get().ordinal() + 1) % themes.length;
                    PixieDustParticleTheme nextTheme = themes[nextOrdinal];

                    // Update config using the accessor
                    @SuppressWarnings("unchecked")
                    ValidatedFieldAccessor<PixieDustParticleTheme> accessor = (ValidatedFieldAccessor<PixieDustParticleTheme>) (Object) Configs.AHP_SUPPORTER.crownTheme;
                    accessor.adorablehamsterpets$set(nextTheme);
                    Configs.AHP_SUPPORTER.save();

                    // Broadcast to server if currently visible or in trial
                    if (Configs.AHP_SUPPORTER.showMyCrown || trialTicks > 0) {
                        ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateCrownThemeC2SPacket(nextOrdinal));
                    }

                    client.player.sendMessage(Text.translatable("message.adorablehamsterpets.supporter_crown_color_changed", Text.translatable(nextTheme.translationKey())).formatted(Formatting.WHITE), true);
                } else {
                    if (hasUsedTrial) {
                        client.player.sendMessage(Text.translatable("message.adorablehamsterpets.crown_trial_used").formatted(Formatting.RED), true);
                    } else {
                        client.player.sendMessage(Text.translatable("message.adorablehamsterpets.crown_trial_prompt").formatted(Formatting.GOLD), true);
                    }
                }
            }
        }

        // Consume all presses from buffer to handle rapid clicking
        int crownPresses = 0;
        while (ModKeyBindings.TOGGLE_SUPPORTER_CROWN_KEY.wasPressed()) {
            crownPresses++;
        }

        if (crownPresses > 0) {
            if (crownPresses >= 2 || (isWaitingForCrownSecondTap && crownDoubleTapTimer > 0)) {
                // --- Double Tap: Toggle Visibility ---
                isWaitingForCrownSecondTap = false;
                crownDoubleTapTimer = 0;

                boolean hasPerk = PlayerPerkManager.INSTANCE.hasPerk(client.player.getGameProfile().getName(), "supporter_crown");
                PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) client.player;
                int trialTicks = playerAccessor.ahp$getSupporterCrownTrialTicks();
                boolean hasUsedTrial = playerAccessor.ahp$hasUsedSupporterCrownTrial();

                if (hasPerk) {
                    Configs.AHP_SUPPORTER.showMyCrown = !Configs.AHP_SUPPORTER.showMyCrown;
                    Configs.AHP_SUPPORTER.save();

                    int payloadTheme = Configs.AHP_SUPPORTER.showMyCrown ? Configs.AHP_SUPPORTER.crownTheme.get().ordinal() : -1;
                    ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateCrownThemeC2SPacket(payloadTheme));

                    client.player.sendMessage(Text.translatable(Configs.AHP_SUPPORTER.showMyCrown ? "message.adorablehamsterpets.supporter_crown_enabled" : "message.adorablehamsterpets.supporter_crown_disabled").formatted(Formatting.GOLD), true);
                } else {
                    if (trialTicks > 0) {
                        // Allow user to hide supporter crown manually during trial period
                        ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateCrownThemeC2SPacket(-1)); // Typed packet for 1.20.1
                        client.player.sendMessage(Text.translatable("message.adorablehamsterpets.supporter_crown_disabled").formatted(Formatting.GOLD), true);
                    } else if (hasUsedTrial) {
                        client.player.sendMessage(Text.translatable("message.adorablehamsterpets.crown_trial_used").formatted(Formatting.RED), true);
                    } else {
                        // Start trial
                        Configs.AHP_SUPPORTER.showMyCrown = true;
                        Configs.AHP_SUPPORTER.save();
                        ModPackets.CHANNEL.sendToServer(new ModPackets.StartCrownTrialC2SPacket(Configs.AHP_SUPPORTER.crownTheme.get().ordinal()));
                        client.player.sendMessage(Text.translatable("message.adorablehamsterpets.crown_trial_started").formatted(Formatting.WHITE), true);
                    }
                }
            } else {
                // First tap detected: Start double-tap listening window (10 ticks = 0.5 seconds)
                isWaitingForCrownSecondTap = true;
                crownDoubleTapTimer = 10;
            }
        }

        // Supporter crown trial period countdown
        int trialTicks = ((PlayerEntityAccessor) client.player).ahp$getSupporterCrownTrialTicks();
        if (trialTicks > 0 && trialTicks % 60 == 0) {
            int seconds = trialTicks / 20;
            client.player.sendMessage(Text.translatable("message.adorablehamsterpets.crown_trial_countdown", seconds).formatted(Formatting.WHITE), false);
        }

        // --- 4. Render State Tracking ---
        // Determine which hamsters started and stopped rendering this tick
        Set<Integer> startedRendering = new HashSet<>(renderedHamsterIdsThisTick);
        startedRendering.removeAll(renderedHamsterIdsLastTick);

        Set<Integer> stoppedRendering = new HashSet<>(renderedHamsterIdsLastTick);
        stoppedRendering.removeAll(renderedHamsterIdsThisTick);

        for (Integer entityId : startedRendering) {
            // Send typed packet for 1.20.1
            ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateHamsterRenderStateC2SPacket(new ArrayList<>(startedRendering), true));
        }

        for (Integer entityId : stoppedRendering) {
            // Architectury 1.20.1 helper logic for update packet uses ByteBuf
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(entityId);
            buf.writeBoolean(false); // isRendering = false
            // Send typed packet for 1.20.1
            ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateHamsterRenderStateC2SPacket(new ArrayList<>(stoppedRendering), false));
        }

        renderedHamsterIdsLastTick.clear();
        renderedHamsterIdsLastTick.addAll(renderedHamsterIdsThisTick);
        renderedHamsterIdsThisTick.clear();

        // --- 5. Hamster Dismount From Shoulder Logic ---
        handleDismountKeyPress(client);

        // --- 6. Guidebook Warning Logic ---
        handleGuidebookWarning(client);

        // --- 7. Tick Particle Manager ---
        if (client.world != null && !client.isPaused()) {
            ClientParticleManager.INSTANCE.tick(client.world);
        }

        // --- 8. Deferred Guidebook Effects ---
        if (pendingGuidebookEffects) {
            pendingGuidebookEffectsTimer--;
            if (client.currentScreen == null) {
                // GUI closed in time, play effects
                playGuidebookEffects(client);
                pendingGuidebookEffects = false;
            } else if (pendingGuidebookEffectsTimer <= 0) {
                // Took too long, cancel effects
                pendingGuidebookEffects = false;
            }
        }

        // --- 9. Supporter Crown Rendering ---
        if (client.world != null && !client.isPaused() && Configs.AHP_SUPPORTER.enableSupporterCrown) {
            boolean isFirstPerson = client.options.getPerspective().isFirstPerson();

            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (!player.isAlive() || player.isSpectator()) continue;

                // Hide from local player if in first person and config is off
                if (player == client.player && !Configs.AHP_SUPPORTER.showCrownInFirstPerson && isFirstPerson) continue;

                // Get theme from synced DataTracker
                int themeOrdinal = ((PlayerEntityAccessor) player).ahp$getSupporterCrownTheme();

                // If themeOrdinal is < 0, it means the player toggled their supporter crown off
                if (themeOrdinal < 0) continue;

                boolean hasPerk = PlayerPerkManager.INSTANCE.hasPerk(player.getGameProfile().getName(), "supporter_crown");
                boolean inTrial = ((PlayerEntityAccessor) player).ahp$getSupporterCrownTrialTicks() > 0;

                if (hasPerk || inTrial) {

                    // --- Audio ---
                    PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
                    int audioTimer = accessor.ahp$getSupporterCrownAudioTimer();

                    if (audioTimer > 0) {
                        accessor.ahp$setSupporterCrownAudioTimer(audioTimer - 1);
                    } else {
                        if (Configs.AHP_SUPPORTER.enableCrownAudio) {
                            float volume = Configs.AHP_SUPPORTER.crownAudioVolume.get();
                            SoundEvent sound = ModSounds.getRandomSoundFrom(ModSounds.CROWN_SPARKLE_SOUNDS, client.world.random);
                            if (sound != null) {
                                client.world.playSound(player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS, volume, 1.0f + client.world.random.nextFloat() * 0.2f, false);
                            }
                        }
                        // Reset timer to ~3 seconds +/- 20 ticks for randomness
                        accessor.ahp$setSupporterCrownAudioTimer(60 + client.world.random.nextBetween(-20, 20));
                    }

                    // --- 1.20.1 Polyfill for getScale() ---
                    // Dynamically calculate scale based on current height vs expected vanilla pose height
                    float playerScale = player.getHeight() / (player.isSneaking() ? 1.5F : 1.8F);

                    // Use player's lerped neck position as pivot point for rotation
                    double pivotOffset = (player.isSneaking() ? 1.2375 : 1.5) * playerScale;
                    Vec3d pivotPos = player.getLerpedPos(1.0f).add(0, pivotOffset, 0);

                    // Create a 3D rotation based on the player's head yaw and pitch
                    Quaternionf headRotation = new Quaternionf()
                            .rotateY(-player.headYaw * MathHelper.RADIANS_PER_DEGREE)
                            .rotateX(player.getPitch() * MathHelper.RADIANS_PER_DEGREE);

                    PixieDustParticleTheme theme = PixieDustParticleTheme.values()[MathHelper.clamp(themeOrdinal, 0, PixieDustParticleTheme.values().length - 1)];
                    DefaultParticleType particleType = ModParticles.PIXIE_DUST.get(theme).get();

                    // Add distance between the eyes and the neck to config offset
                    double adjustedYOffset = Configs.AHP_SUPPORTER.crownYOffset.get() + (player.getStandingEyeHeight() - pivotOffset);

                    // --- Helmet Multiplier ---
                    ItemStack helmet = player.getInventory().getArmorStack(3);
                    double helmetMultiplier = !helmet.isEmpty() ? 1.15 : 1.0;

                    // --- 3D Skin Layers Compat ---
                    double adjustedRadius = (Configs.AHP_SUPPORTER.crownRadius.get() + (IS_SKIN_LAYERS_3D_LOADED ? 0.1 : 0.0)) * helmetMultiplier;

                    ParticleEffectsUtil.spawnOrientedSpinningRing(
                            client.world,
                            pivotPos,
                            headRotation,
                            particleType,
                            Configs.AHP_SUPPORTER.crownParticleCount.get(),
                            adjustedRadius,
                            Configs.AHP_SUPPORTER.crownHorizontalThickness.get(),
                            Configs.AHP_SUPPORTER.crownVerticalThickness.get(),
                            0.3,
                            0.03,
                            0.007,
                            adjustedYOffset
                    );
                }
            }
        }

        // --- 10. Delayed Shoulder Mount Sound ---
        if (mountSoundDelayTicks > 0) {
            mountSoundDelayTicks--;
            if (mountSoundDelayTicks == 0 && pendingMountSoundId != null) {
                if (client.player != null) {
                    client.getSoundManager().play(new PositionedSoundInstance(
                            SoundEvent.of(pendingMountSoundId), SoundCategory.PLAYERS,
                            1.0f, pendingMountSoundPitch, client.player.getRandom(),
                            client.player.getX(), client.player.getY(), client.player.getZ()
                    ));
                }
                pendingMountSoundId = null;
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            3. Logic Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Checks if the player has the guidebook. If they don't have it after a configured time,
     * sends a dramatic warning message.
     */
    private static void handleGuidebookWarning(MinecraftClient client) {
        if (client.player == null) return;

        final AhpUiConfig config = AdorableHamsterPets.UI_CONFIG;
        String username = client.player.getGameProfile().getName();

        // Fast exit if globally disabled via secret key ("john_wayne"), or if already seen by this player
        if (config.playersWhoHaveSeenGuidebookWarning.contains("john_wayne") ||
                config.playersWhoHaveSeenGuidebookWarning.contains(username)) {
            return;
        }

        int warningTime = config.guidebookWarningTimer.get();

        if (clientSessionTimer > warningTime + 145) {
            clientSessionTimer = 0;
        }

        clientSessionTimer++;

        // Check 1: 1 second in (20 ticks) - Silent Check
        // If they spawn with the book (or get it from auto-delivery), mark as seen silently.
        if (clientSessionTimer == 20) {
            if (hasGuideBook(client.player)) {
                markGuidebookWarningSeen(config, username);
            }
        }

        // Check 2: Configured time - Warning Part 1
        if (clientSessionTimer == warningTime) {
            if (!hasGuideBook(client.player)) {
                sendWarningPart1(client.player);
            } else {
                // If they have the book now, mark as seen and don't proceed to Part 2
                markGuidebookWarningSeen(config, username);
            }
        }

        // Check 3: 5 seconds later - Warning Part 2
        if (clientSessionTimer == warningTime + 140) {
            if (!hasGuideBook(client.player)) {
                sendWarningPart2(client.player);
            }
            // Mark as seen regardless to prevent spamming next session
            markGuidebookWarningSeen(config, username);
        }
    }

    private static void markGuidebookWarningSeen(AhpUiConfig config, String username) {
        if (!config.playersWhoHaveSeenGuidebookWarning.contains(username)) {
            config.playersWhoHaveSeenGuidebookWarning.add(username);
            config.save();
        }
    }

    private static boolean hasGuideBook(net.minecraft.entity.player.PlayerEntity player) {
        // Iterate and check item type to ignore NBT/Components
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
                return true;
            }
        }
        return false;
    }

    private static void sendWarningPart1(net.minecraft.entity.player.PlayerEntity player) {
        // 1. Once Only Disclaimer
        MutableText message = Text.literal("\n")
                .append(Text.translatable("message.adorablehamsterpets.warning.only_once").formatted(Formatting.RED, Formatting.BOLD))
                .append("\n\n");

        // 2. Header
        message.append(Text.translatable("message.adorablehamsterpets.warning.header_prefix").formatted(Formatting.GOLD))
                .append(Text.translatable("message.adorablehamsterpets.warning.header_title").formatted(Formatting.RED, Formatting.BOLD))
                .append("\n\n");

        // 3. Context
        // Calculate minutes. Round up to 1 if less than a minute
        int ticks = AdorableHamsterPets.UI_CONFIG.guidebookWarningTimer.get();
        int minutes = Math.max(1, ticks / 1200);

        String key = (minutes == 1)
                ? "message.adorablehamsterpets.warning.context.singular"
                : "message.adorablehamsterpets.warning.context.plural";

        message.append(Text.translatable(key, minutes).formatted(Formatting.GRAY));

        player.sendMessage(message, false);
        player.playSound(ModSounds.HAMSTER_DING.get(), 1.0f, 0.8f);
    }

    private static void sendWarningPart2(net.minecraft.entity.player.PlayerEntity player) {
        // 4. The Oath
        MutableText message = Text.literal("\n")
                .append(Text.translatable("message.adorablehamsterpets.warning.oath_label").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(" ")
                .append(Text.translatable("message.adorablehamsterpets.warning.oath_text").formatted(Formatting.RED, Formatting.ITALIC))
                .append("\n\n");

        // 5. Action (Clickable Command)
        message.append(Text.translatable("message.adorablehamsterpets.warning.action_button")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.GREEN)
                        .withBold(true)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ahp_open_config_screen"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("message.adorablehamsterpets.warning.action_hover")))
                )).append("\n\n");

        // 6. Crafting Instructions
        message.append(Text.translatable("message.adorablehamsterpets.warning.crafting_help").formatted(Formatting.GRAY));

        player.sendMessage(message, false);

        // Play a notification sound
        player.playSound(ModSounds.HAMSTER_DING.get(), 1.0f, 1.0f);
    }

    /**
     * Handles the complex client-side logic for a hamster dismounting from the player's
     * shoulder. Uses an unbound fallback strategy: if the custom dismount key is
     * unbound, defaults to the vanilla Sneak key.
     *
     * @param client The MinecraftClient instance.
     */
    private static void handleDismountKeyPress(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // --- 1. Check Shoulder State ---
        boolean hasShoulderHamster = false;
        try {
            hasShoulderHamster = ((PlayerEntityAccessor) client.player).hasAnyShoulderHamster();
        } catch (RuntimeException e) {
            // If the player entity's data tracker is corrupted (missing entries due to mod conflicts),
            // assume no hamster is present to prevent a crash
            hasShoulderHamster = false;
        }

        // --- 2. Determine Active Keybind ---
        // If custom dismount key unbound, fall back to sneak key
        boolean isCustomKeyBound = !ModKeyBindings.DISMOUNT_HAMSTER_KEY.isUnbound();
        KeyBinding keyToListenFor = isCustomKeyBound
                ? ModKeyBindings.DISMOUNT_HAMSTER_KEY
                : client.options.sneakKey;

        if (keyToListenFor == null) return;

        // --- 3. Handle Mount Transition ---
        if (hasShoulderHamster && !hadShoulderHamsterLastTick) {
            // Player just mounted a hamster this tick
            dismountDebounceTicks = DISMOUNT_DEBOUNCE_DEFAULT;
            isWaitingForSecondTap = false;
            doubleTapTimer = 0;

            // Flush buffer to prevent accumulated presses from triggering instant/accidental dismounts
            while (keyToListenFor.wasPressed()) {}
        }
        hadShoulderHamsterLastTick = hasShoulderHamster;

        // --- 4. Decrement Timers ---
        if (dismountDebounceTicks > 0) {
            dismountDebounceTicks--;
        }
        if (doubleTapTimer > 0) {
            doubleTapTimer--;
            if (doubleTapTimer == 0) {
                isWaitingForSecondTap = false; // Double tap window expired
            }
        }

        // --- 5. Early Exit if No Hamster ---
        if (!hasShoulderHamster) {
            return;
        }

        // --- 6. Count Hardware Presses & Filter OS Repeats ---
        boolean isCurrentlyPressed = keyToListenFor.isPressed();

        // Track how long key has been held continuously
        if (isCurrentlyPressed) {
            dismountKeyHeldTicks++;
        } else {
            dismountKeyHeldTicks = 0;
        }

        // Consume all presses from vanilla buffer
        int bufferCount = 0;
        while (keyToListenFor.wasPressed()) {
            bufferCount++;
        }

        int validTaps = 0;
        if (bufferCount > 0) {
            // If key has been held down continuously for more than 5 ticks,
            // any new presses appearing in the buffer are fake OS auto-repeats
            if (isCurrentlyPressed && dismountKeyHeldTicks > 5) {
                validTaps = 0;
            } else {
                validTaps = bufferCount;
            }
        }

        if (validTaps == 0) {
            return; // No valid inputs to process this tick
        }

        // Ignore valid inputs during initial mount debounce window
        if (dismountDebounceTicks > 0) {
            return;
        }

        // --- 7. Apply Logic Based on Config ---
        final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;
        DismountButtonPressBehavior activePressType = config.dismountButtonPressBehavior.get();

        // Apply override if custom key bound and override toggle enabled
        if (isCustomKeyBound && config.singlePressOverrideForCustomKey) {
            activePressType = DismountButtonPressBehavior.SINGLE_PRESS;
        }

        if (activePressType == DismountButtonPressBehavior.SINGLE_PRESS) {
            // Send a typed packet for 1.20.1
            ModPackets.CHANNEL.sendToServer(new ModPackets.DismountHamsterC2SPacket());
        } else { // DOUBLE_TAP
            // Handle edge case where player double-tapped so fast it occurred within a single tick
            if (validTaps >= 2) {
                ModPackets.CHANNEL.sendToServer(new ModPackets.DismountHamsterC2SPacket());
                isWaitingForSecondTap = false;
                doubleTapTimer = 0;
            } else {
                // Standard single press detected
                if (isWaitingForSecondTap) {
                    // Second tap
                    ModPackets.CHANNEL.sendToServer(new ModPackets.DismountHamsterC2SPacket());
                    isWaitingForSecondTap = false;
                    doubleTapTimer = 0;
                } else {
                    // First tap. Start window
                    isWaitingForSecondTap = true;
                    doubleTapTimer = config.doubleTapDelayTicks.get();
                }
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                       4. Network Packet Handlers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Handles the PlayMountSoundPayload packet.
     * Determines whether to delay the mount sound based on the player's active perspective.
     */
    public static void handlePlayMountSound(Identifier soundId, float pitch, int delay) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (MiscUtil.ModCompatUtil.hasRequiredPunchyVersion() && client.options.getPerspective().isFirstPerson()) {
            pendingMountSoundId = soundId;
            pendingMountSoundPitch = pitch;
            mountSoundDelayTicks = delay;
        } else {
            if (client.player != null) {
                client.getSoundManager().play(new PositionedSoundInstance(
                        SoundEvent.of(soundId), SoundCategory.PLAYERS,
                        1.0f, pitch, client.player.getRandom(),
                        client.player.getX(), client.player.getY(), client.player.getZ()
                ));
            }
        }
    }

    /**
     * Handles the SyncHamsterState packet on the client.
     */
    public static void handleSyncHamsterState(int entityId, net.minecraft.nbt.NbtCompound data) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            net.minecraft.entity.Entity entity = client.world.getEntityById(entityId);
            if (entity instanceof net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor accessor) {
                accessor.adorablehamsterpets$setRawHamsterState(data);
            }
        }
    }

    /**
     * Handles the {@link ModPackets.SpawnBeddingParticlesS2CPacket} packet.
     * Spawns a burst of "floaty" leaf particles at the specified location.
     * Used by dispensers and the Hamster Bedding item.
     *
     * @param packet The packet data containing position, direction, and wood variant.
     */
    public static void handleSpawnBeddingParticles(ModPackets.SpawnBeddingParticlesS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        BlockPos spawnPos = packet.pos().offset(packet.direction());
        Vec3d particleCenter = Vec3d.ofCenter(spawnPos);

        // Get the particle type. In 1.20.1, use DefaultParticleType instead of SimpleParticleType.
        DefaultParticleType particleType = ModParticles.getForVariant(packet.variant());

        for (int i = 0; i < 30; i++) {
            double offsetX = client.world.random.nextGaussian() * 1.2;
            double offsetY = client.world.random.nextGaussian() * 1.2;
            double offsetZ = client.world.random.nextGaussian() * 1.2;
            // Spawn the dynamic particle type and use the vy flag to trigger floaty physics
            client.world.addParticle(particleType,
                    particleCenter.x + offsetX, particleCenter.y + offsetY, particleCenter.z + offsetZ,
                    0, HamsterBeddingParticle.BEDDING_ITEM_FLAG, 0);
        }
    }

    /**
     * Handles the {@link ModPackets.PlayGuidebookEffectsS2CPacket} packet.
     * Queues effects and an action bar message when the guidebook is retrieved.
     */
    public static void queueGuidebookEffects(ModPackets.PlayGuidebookEffectsS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Close config screen only if requested
        if (packet.closeScreen()) {
            client.setScreen(null);
            playGuidebookEffects(client);
        } else if (client.currentScreen != null) {
            // A GUI is open. Defer effects for up to 5 seconds
            pendingGuidebookEffects = true;
            pendingGuidebookEffectsTimer = 100;
        } else {
            // No GUI open, play immediately
            playGuidebookEffects(client);
        }
    }

    /**
     * Executes feedback for discovering the guidebook.
     * Plays sound effects, particles, and an action bar message
     */
    private static void playGuidebookEffects(MinecraftClient client) {
        PlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return;

        // Feedback
        player.sendMessage(Text.translatable("message.adorablehamsterpets.guidebook_obtained").formatted(Formatting.GOLD), true);
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f, false);
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.7f, 1.5f, false);

        ParticleEffectsUtil.spawnParticlesOnEntity(
                player,
                ParticleTypes.ENCHANT,
                50,
                1.0,
                1.0,
                0.05,
                0.0
        );
        ParticleEffectsUtil.spawnParticlesOnEntity(
                player,
                ParticleTypes.HAPPY_VILLAGER,
                20,
                1.0,
                1.0,
                0.5,
                0.0
        );
    }

    /**
     * Handles the {@link ModPackets.PlayDistantSoundS2CPacket} packet.
     * Plays a sound at a specific location on the client, bypassing vanilla's distance attenuation checks
     * often imposed by ServerPlayerEntity#playSound, allowing "distant" sounds to be heard.
     *
     * @param packet The packet data containing sound ID, volume, and pitch.
     */
    public static void handlePlayDistantSound(ModPackets.PlayDistantSoundS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Resolve the sound identifier to a SoundEvent (1.20.1 syntax)
        SoundEvent sound = SoundEvent.of(packet.soundId());

        // Play the sound at the PLAYER'S location to ensure audibility
        client.world.playSound(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                sound,
                SoundCategory.NEUTRAL,
                packet.volume(),
                packet.pitch(),
                false // distanceDelay
        );
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                       5. Trackers & Accessors
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Called by the renderer to track which entities are currently visible.
     * Used for optimizing network traffic related to rendering state.
     * @param entityId The ID of the rendered entity.
     */
    public static void onHamsterRendered(int entityId) {
        renderedHamsterIdsThisTick.add(entityId);
    }

    /**
     * Public getter for other client classes (like the HUD renderer and Widget)
     * to access the cached list of pending notifications.
     * @return The current list of pending notifications.
     */
    public static List<AnnouncementManager.PendingNotification> getPendingNotifications() {
        return pendingNotifications;
    }
}
