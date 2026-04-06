package net.dawson.adorablehamsterpets;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
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
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.dawson.adorablehamsterpets.client.perk.PlayerPerkManager;
import net.dawson.adorablehamsterpets.client.render.LeafJiggleManager;
import net.dawson.adorablehamsterpets.client.sound.HamsterTreeLoopSoundInstance;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.renderer.HamsterTreeSearcherRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.mixin.accessor.ValidatedFieldAccessor;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.ClientParticleManager;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
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

                // Sync supporter crown theme preference to server
                if (MinecraftClient.getInstance().player != null) {
                    int payloadTheme = Configs.AHP.showMyCrown ? Configs.AHP.crownTheme.get().ordinal() : -1;
                    NetworkManager.sendToServer(new UpdateCrownThemePayload(payloadTheme));
                }

                AdorableHamsterPets.LOGGER.info("Reloaded Adorable Hamster Pets config caches on client.");
            }
        });

        // --- Item Colors ---
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> -1, ModItems.HAMSTER_SPAWN_EGG.get());

        // --- Networking ---
        ModPackets.registerS2CPackets();

        // --- Announcement System ---
        AHPClientScreenEvents.register();

        // --- Events ---
        ClientTickEvent.CLIENT_POST.register(AdorableHamsterPetsClient::onEndClientTick);
        ClientGuiEvent.RENDER_HUD.register((context, tickCounter) -> announcementHudRenderer.render(context, tickCounter.getTickDelta(true)));

        // --- Register Client Commands ---
        ClientCommandRegistrationEvent.EVENT.register(ModClientCommands::register);

        // --- Timers Reset & Sync ---
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            clientSessionTimer = 0;
            ClientParticleManager.INSTANCE.clear();
            pendingGuidebookEffects = false;

            // Sync initial supporter crown theme preference to server
            int payloadTheme = Configs.AHP.showMyCrown ? Configs.AHP.crownTheme.get().ordinal() : -1;
            NetworkManager.sendToServer(new UpdateCrownThemePayload(payloadTheme));
        });

        // --- Register Tree Heist Sound & Jiggle Logic ---
        EntityEvent.ADD.register((entity, world) -> {
            if (world.isClient() && entity instanceof HamsterTreeSearcherEntity searcher) {
                MinecraftClient client = MinecraftClient.getInstance();

                // 1. Sound Logic
                HamsterTreeLoopSoundInstance existingSound = activeTreeSounds.get(searcher.getId());

                if (existingSound == null || existingSound.isDone()) {
                    HamsterTreeLoopSoundInstance newSound = new HamsterTreeLoopSoundInstance(searcher);
                    client.getSoundManager().play(newSound);
                    activeTreeSounds.put(searcher.getId(), newSound);
                }

                // 2. Leaf Jiggle Tracking
                LeafJiggleManager.INSTANCE.onSearcherAdded(searcher);
            }
            return EventResult.pass();
        });

        // --- Custom Keybind Interaction ---
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            // Ensure we are on client and main hand to avoid double firing
            if (player.getWorld().isClient && hand == net.minecraft.util.Hand.MAIN_HAND && entity instanceof HamsterEntity hamster) {

                // 1. Force Shoulder Mount
                if (Configs.AHP.enableShoulderMountKeybind && ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY.isPressed()) {
                    if (hamster.isTamed() && hamster.isOwner(player)) {
                        NetworkManager.sendToServer(new RequestHamsterMountPayload(hamster.getId()));
                        return EventResult.interruptTrue(); // Cancel default interaction
                    }
                }

                // 2. Hamster Riding
                if (Configs.AHP.enableMountableHamsters.get() && ModKeyBindings.RIDE_HAMSTER_KEY.isPressed()) {
                    // Prevent mounting if already riding
                    if (!hamster.hasPassenger(player)) {
                        NetworkManager.sendToServer(new RequestHamsterRidePayload(hamster.getId()));
                        return EventResult.interruptTrue(); // Cancel default interaction
                    }
                }
            }
            return EventResult.pass();
        });

        // --- Perk System ---
        PlayerPerkManager.INSTANCE.refreshManifestOnce();
    }

    /**
     * Registers the Block Entity Renderers.
     * Separated for cross-loader compatibility (NeoForge requires a specific event).
     */
    public static void initBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), HamsterBedRenderer::new);
    }

    /**
     * Registers the Screen Handlers (Menus).
     * Separated for cross-loader compatibility.
     */
    public static void initScreenHandlers() {
        MenuRegistry.registerScreenFactory(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), HamsterInventoryScreen::new);
    }

    /**
     * Registers the Entity Renderers.
     * Separated for cross-loader compatibility.
     */
    public static void initEntityRenderers() {
        EntityRendererRegistry.register(ModEntities.HAMSTER, HamsterRenderer::new);
        EntityRendererRegistry.register(ModEntities.HAMSTER_TREE_SEARCHER, HamsterTreeSearcherRenderer::new);
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
        // --- 1. Leaf Jiggle Manager ---
        LeafJiggleManager.INSTANCE.clientTick(client);

        // --- 2. Announcement System Logic ---
        boolean isGuiOpen = client.currentScreen != null;
        AnnouncementIconAnimator.INSTANCE.tick(isGuiOpen);

        // Sync Patchouli State (once per session after world load)
        if (client.world != null && !AnnouncementManager.INSTANCE.isPatchouliStateSynced()) {
            AnnouncementManager.INSTANCE.syncPatchouliReadState();
            // Once the sync is successful, also process any deferred read marks from the session
            if (AnnouncementManager.INSTANCE.isPatchouliStateSynced()) {
                AnnouncementManager.INSTANCE.processDeferredReadMarks();
            }
        }

        if (client.world != null) {
            // Update the cached list of pending notifications once per tick
            pendingNotifications = AnnouncementManager.INSTANCE.getPendingNotifications();
        }

        // Periodic Manifest Refresh
        if (--nextRefreshTicks <= 0) {
            nextRefreshTicks = 6000; // Reset timer (5 min)
            AnnouncementManager.INSTANCE.refreshManifest();
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
        if (ridingHamster && Configs.AHP.enableMountableHamsters.get()) {
            boolean jumpDown = client.options.jumpKey.isPressed();
            boolean sprintDown = client.options.sprintKey.isPressed();

            // If either input changed, send update
            if (jumpDown != lastJumpDown || sprintDown != lastSprintDown) {
                lastJumpDown = jumpDown;
                lastSprintDown = sprintDown;

                // 1. Send Packet
                NetworkManager.sendToServer(new HamsterInputPayload(jumpDown, sprintDown));

                // 2. Client-Side Prediction
                HamsterEntity hamster = (HamsterEntity) client.player.getVehicle();
                hamster.setRiderInput(jumpDown, sprintDown);
            }
        } else if (lastJumpDown || lastSprintDown) {
            // Reset state if dismounted while holding buttons
            lastJumpDown = false;
            lastSprintDown = false;
            NetworkManager.sendToServer(new HamsterInputPayload(false, false));
        }

        // Handle Throw Hamster Keybind
        if (ModKeyBindings.THROW_HAMSTER_KEY.wasPressed()) {
            final AhpConfig currentConfig = AdorableHamsterPets.CONFIG;
            if (!currentConfig.enableHamsterThrowing) {
                client.player.sendMessage(Text.translatable("message.adorablehamsterpets.throwing_disabled"), true);
            } else {
                boolean lookingAtReachableBlock = client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK;
                boolean hasShoulderHamsterClient = ((PlayerEntityAccessor) client.player).hasAnyShoulderHamster();

                if (!lookingAtReachableBlock && hasShoulderHamsterClient) {
                    NetworkManager.sendToServer(new ThrowHamsterPayload());
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
                    int nextOrdinal = (Configs.AHP.crownTheme.get().ordinal() + 1) % themes.length;
                    PixieDustParticleTheme nextTheme = themes[nextOrdinal];

                    // Update config using the accessor
                    @SuppressWarnings("unchecked")
                    ValidatedFieldAccessor<PixieDustParticleTheme> accessor = (ValidatedFieldAccessor<PixieDustParticleTheme>) (Object) Configs.AHP.crownTheme;
                    accessor.adorablehamsterpets$set(nextTheme);
                    Configs.AHP.save();

                    // Broadcast to server if currently visible or in trial
                    if (Configs.AHP.showMyCrown || trialTicks > 0) {
                        NetworkManager.sendToServer(new UpdateCrownThemePayload(nextOrdinal));
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
                    Configs.AHP.showMyCrown = !Configs.AHP.showMyCrown;
                    Configs.AHP.save();

                    int payloadTheme = Configs.AHP.showMyCrown ? Configs.AHP.crownTheme.get().ordinal() : -1;
                    NetworkManager.sendToServer(new UpdateCrownThemePayload(payloadTheme));

                    client.player.sendMessage(Text.translatable(Configs.AHP.showMyCrown ? "message.adorablehamsterpets.supporter_crown_enabled" : "message.adorablehamsterpets.supporter_crown_disabled").formatted(Formatting.GOLD), true);
                } else {
                    if (trialTicks > 0) {
                        // Allow user to hide supporter crown manually during trial period
                        NetworkManager.sendToServer(new UpdateCrownThemePayload(-1));
                        client.player.sendMessage(Text.translatable("message.adorablehamsterpets.supporter_crown_disabled").formatted(Formatting.GOLD), true);
                    } else if (hasUsedTrial) {
                        client.player.sendMessage(Text.translatable("message.adorablehamsterpets.crown_trial_used").formatted(Formatting.RED), true);
                    } else {
                        // Start trial
                        Configs.AHP.showMyCrown = true;
                        Configs.AHP.save();
                        NetworkManager.sendToServer(new StartCrownTrialPayload(Configs.AHP.crownTheme.get().ordinal()));
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

        if (!startedRendering.isEmpty()) {
            NetworkManager.sendToServer(new UpdateHamsterRenderStatePayload(new ArrayList<>(startedRendering), true));
        }

        if (!stoppedRendering.isEmpty()) {
            NetworkManager.sendToServer(new UpdateHamsterRenderStatePayload(new ArrayList<>(stoppedRendering), false));
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
        if (client.world != null && !client.isPaused() && Configs.AHP.enableSupporterCrown) {
            boolean isFirstPerson = client.options.getPerspective().isFirstPerson();

            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (!player.isAlive() || player.isSpectator()) continue;

                // Hide from local player if in first person and config is off
                if (player == client.player && !Configs.AHP.showCrownInFirstPerson && isFirstPerson) continue;

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
                        if (Configs.AHP.enableCrownAudio) {
                            float volume = Configs.AHP.crownAudioVolume.get();
                            SoundEvent sound = ModSounds.getRandomSoundFrom(ModSounds.CROWN_SPARKLE_SOUNDS, client.world.random);
                            if (sound != null) {
                                client.world.playSound(player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS, volume, 1.0f + client.world.random.nextFloat() * 0.2f, false);
                            }
                        }
                        // Reset timer to ~3 seconds +/- 20 ticks for randomness
                        accessor.ahp$setSupporterCrownAudioTimer(60 + client.world.random.nextBetween(-20, 20));
                    }

                    // Use player's lerped neck position as pivot point for rotation
                    double pivotOffset = (player.isSneaking() ? 1.2375 : 1.5) * player.getScale();
                    Vec3d pivotPos = player.getLerpedPos(1.0f).add(0, pivotOffset, 0);

                    // Create a 3D rotation based on the player's head yaw and pitch
                    Quaternionf headRotation = new Quaternionf()
                            .rotateY(-player.headYaw * MathHelper.RADIANS_PER_DEGREE)
                            .rotateX(player.getPitch() * MathHelper.RADIANS_PER_DEGREE);

                    PixieDustParticleTheme theme = PixieDustParticleTheme.values()[MathHelper.clamp(themeOrdinal, 0, PixieDustParticleTheme.values().length - 1)];
                    SimpleParticleType particleType = ModParticles.PIXIE_DUST.get(theme).get();

                    // Add distance between the eyes and the neck to config offset
                    double adjustedYOffset = Configs.AHP.crownYOffset.get() + (player.getStandingEyeHeight() - pivotOffset);

                    // Add 0.1 to radius if 3D Skin Layers is installed
                    double adjustedRadius = Configs.AHP.crownRadius.get() + (IS_SKIN_LAYERS_3D_LOADED ? 0.1 : 0.0);

                    ParticleEffectsUtil.spawnOrientedSpinningRing(
                            client.world,
                            pivotPos,
                            headRotation,
                            particleType,
                            Configs.AHP.crownParticleCount.get(),
                            adjustedRadius,
                            Configs.AHP.crownHorizontalThickness.get(),
                            Configs.AHP.crownVerticalThickness.get(),
                            0.3,
                            0.03,
                            0.007,
                            adjustedYOffset
                    );
                }
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

        final AhpConfig config = AdorableHamsterPets.CONFIG;
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
                config.playersWhoHaveSeenGuidebookWarning.add(username);
                config.save();
            }
        }

        // Check 2: Configured time - Warning Part 1
        if (clientSessionTimer == warningTime) {
            if (!hasGuideBook(client.player)) {
                sendWarningPart1(client.player);
            } else {
                // If they have the book now, mark as seen and don't proceed to Part 2
                config.playersWhoHaveSeenGuidebookWarning.add(username);
                config.save();
            }
        }

        // Check 3: 5 seconds later - Warning Part 2
        if (clientSessionTimer == warningTime + 140) {
            if (!hasGuideBook(client.player)) {
                sendWarningPart2(client.player);
            }
            // Mark as seen regardless to prevent spamming next session
            if (!config.playersWhoHaveSeenGuidebookWarning.contains(username)) {
                config.playersWhoHaveSeenGuidebookWarning.add(username);
                config.save();
            }
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
        int ticks = AdorableHamsterPets.CONFIG.guidebookWarningTimer.get();
        int minutes = Math.max(1, ticks / 1200);

        String key = (minutes == 1)
                ? "message.adorablehamsterpets.warning.context.singular"
                : "message.adorablehamsterpets.warning.context.plural";

        message.append(Text.translatable(key, minutes).formatted(Formatting.GRAY));

        player.sendMessage(message, false);
        player.playSound(ModSounds.HAMSTER_DING.value(), 1.0f, 0.8f);
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
        player.playSound(ModSounds.HAMSTER_DING.value(), 1.0f, 1.0f);
    }

    /**
     * Handles the complex client-side logic for a hamster dismounting from the player's
     * shoulder. Checks the user's configuration to determine if dismount requires:
     * <ul>
     *     <li>Sneak Key vs. Custom Keybind</li>
     *     <li>Single Press vs. Double Tap</li>
     * </ul>
     * Includes debounce logic and an OS Key Repeat filter to prevent accidental dismounts
     * when the button is held down.
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

        // --- 2. Handle Mount Transition ---
        if (hasShoulderHamster && !hadShoulderHamsterLastTick) {
            // Player just mounted a hamster this tick
            dismountDebounceTicks = DISMOUNT_DEBOUNCE_DEFAULT;
            isWaitingForSecondTap = false;
            doubleTapTimer = 0;

            // Flush buffers to prevent accumulated vanilla presses from triggering instant/accidental dismounts
            KeyBinding vanillaSneak = client.options.sneakKey;
            KeyBinding customDismount = ModKeyBindings.DISMOUNT_HAMSTER_KEY;
            if (vanillaSneak != null) {
                while (vanillaSneak.wasPressed()) {}
            }
            if (customDismount != null) {
                while (customDismount.wasPressed()) {}
            }
        }
        hadShoulderHamsterLastTick = hasShoulderHamster;

        // --- 3. Decrement Timers ---
        if (dismountDebounceTicks > 0) {
            dismountDebounceTicks--;
        }
        if (doubleTapTimer > 0) {
            doubleTapTimer--;
            if (doubleTapTimer == 0) {
                isWaitingForSecondTap = false; // Double tap window expired
            }
        }

        // --- 4. Early Exit if No Hamster ---
        if (!hasShoulderHamster) {
            return;
        }

        // --- 5. Determine Active Keybind ---
        final AhpConfig config = AdorableHamsterPets.CONFIG;
        KeyBinding keyToListenFor = (config.dismountTriggerType == DismountTriggerType.CUSTOM_KEYBIND)
                ? ModKeyBindings.DISMOUNT_HAMSTER_KEY
                : client.options.sneakKey;

        if (keyToListenFor == null) return;

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
        if (config.dismountPressType.get() == DismountPressType.SINGLE_PRESS) {
            NetworkManager.sendToServer(new DismountHamsterPayload());
        } else { // DOUBLE_TAP
            // Handle edge case where player double-tapped so fast it occurred within a single tick
            if (validTaps >= 2) {
                NetworkManager.sendToServer(new DismountHamsterPayload());
                isWaitingForSecondTap = false;
                doubleTapTimer = 0;
            } else {
                // Standard single press detected
                if (isWaitingForSecondTap) {
                    // Second tap
                    NetworkManager.sendToServer(new DismountHamsterPayload());
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
     * Handles the {@link SpawnBeddingParticlesPayload} packet.
     * Spawns a burst of "floaty" leaf particles at the specified location.
     * Used by dispensers and the Hamster Bedding item.
     *
     * @param payload The packet data containing position, direction, and wood variant.
     */
    public static void handleSpawnBeddingParticles(SpawnBeddingParticlesPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        BlockPos spawnPos = payload.pos().offset(payload.direction());
        Vec3d particleCenter = Vec3d.ofCenter(spawnPos);

        // Get the particle type for the correct wood variant
        SimpleParticleType particleType = ModParticles.getForVariant(payload.variant());

        for (int i = 0; i < 30; i++) {
            double offsetX = client.world.random.nextGaussian() * 1.2;
            double offsetY = client.world.random.nextGaussian() * 1.2;
            double offsetZ = client.world.random.nextGaussian() * 1.2;
            // Spawn with 'vy' magic flag to trigger floaty physics in HamsterBeddingParticle
            client.world.addParticle(particleType,
                    particleCenter.x + offsetX, particleCenter.y + offsetY, particleCenter.z + offsetZ,
                    0, HamsterBeddingParticle.BEDDING_ITEM_FLAG, 0);
        }
    }

    /**
     * Handles the {@link PlayGuidebookEffectsPayload} packet.
     * Queues effects and an action bar message when the guidebook is retrieved.
     */
    public static void queueGuidebookEffects(PlayGuidebookEffectsPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Close config screen only if requested
        if (payload.closeScreen()) {
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
        if (player == null || client.world == null) return;

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
     * Handles the {@link PlayDistantSoundPayload} packet.
     * Plays a sound at a specific location on the client, bypassing vanilla's distance attenuation checks
     * often imposed by ServerPlayerEntity#playSound, allowing "distant" impact sounds to be heard.
     *
     * @param payload The packet data containing sound ID, volume, and pitch.
     */
    public static void handlePlayDistantSound(PlayDistantSoundPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Resolve the sound identifier to a SoundEvent
        SoundEvent sound = SoundEvent.of(payload.soundId());

        // Play the sound at the player's location to ensure audibility
        client.world.playSound(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                sound,
                SoundCategory.NEUTRAL,
                payload.volume(),
                payload.pitch(),
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