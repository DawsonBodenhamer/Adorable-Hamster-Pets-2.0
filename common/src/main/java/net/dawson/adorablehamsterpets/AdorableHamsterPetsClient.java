package net.dawson.adorablehamsterpets;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.networking.NetworkManager;
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
import net.dawson.adorablehamsterpets.client.event.AHPClientScreenEvents;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconAnimator;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdorableHamsterPetsClient {

    // --- Rendering State ---
    private static final Set<Integer> renderedHamsterIdsThisTick = new HashSet<>();
    private static final Set<Integer> renderedHamsterIdsLastTick = new HashSet<>();

    // --- Input & Dismount Logic ---
    private static long lastSneakPressTime = 0;
    private static boolean isWaitingForSecondSneakPress = false;
    private static boolean hadShoulderHamsterLastTick = false;
    private static int dismountDebounceTicks = 0;
    private static final int DISMOUNT_DEBOUNCE_DEFAULT = 5;

    // --- Announcement System ---
    private static final AnnouncementHudRenderer announcementHudRenderer = new AnnouncementHudRenderer();
    private static List<AnnouncementManager.PendingNotification> pendingNotifications = Collections.emptyList();
    private static int nextRefreshTicks = 6000; // 5 minutes

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

        // --- Config Reload Listener ---
        ConfigApiJava.event().onUpdateClient((id, config) -> {
            if (id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "main"))) {
                // Re-parse cached tags if the main config changes
                ConfigDataCache.parseConfig();
                AdorableHamsterPets.LOGGER.info("Reloaded Adorable Hamster Pets item tag config on client.");
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

        // --- Force-Mount Keybind Interaction ---
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            // Ensure we are on client and main hand to avoid double firing
            if (player.getWorld().isClient && hand == net.minecraft.util.Hand.MAIN_HAND && entity instanceof HamsterEntity hamster) {
                // Check if key is pressed AND config enabled
                if (Configs.AHP.enableShoulderMountKeybind && ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY.isPressed()) {
                    // Only if it's tamed hamster and owned by player
                    if (hamster.isTamed() && hamster.isOwner(player)) {
                        // Send packet
                        NetworkManager.sendToServer(new RequestHamsterMountPayload(hamster.getId()));
                        return EventResult.interruptTrue(); // Cancel default interaction to prevent sitting
                    }
                }
            }
            return EventResult.pass();
        });
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
        // --- 1. Announcement System Logic ---
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

        // --- 2. Input & Game Logic ---
        if (client.player == null || client.world == null) {
            renderedHamsterIdsThisTick.clear();
            renderedHamsterIdsLastTick.clear();
            return;
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
                    dev.architectury.networking.NetworkManager.sendToServer(new ThrowHamsterPayload());
                }
            }
        }

        // --- 3. Render State Tracking ---
        // Determines which hamsters stopped rendering this tick (went off-screen)
        Set<Integer> stoppedRendering = new HashSet<>(renderedHamsterIdsLastTick);
        stoppedRendering.removeAll(renderedHamsterIdsThisTick);

        for (Integer entityId : stoppedRendering) {
            dev.architectury.networking.NetworkManager.sendToServer(new UpdateHamsterRenderStatePayload(entityId, false));
        }

        renderedHamsterIdsLastTick.clear();
        renderedHamsterIdsLastTick.addAll(renderedHamsterIdsThisTick);
        renderedHamsterIdsThisTick.clear();

        // --- 4. Dismount Logic ---
        handleDismountKeyPress(client);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            3. Logic Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Handles the complex client-side logic for dismounting a shoulder hamster.
     * Checks the user's configuration to determine if dismount requires:
     * <ul>
     *     <li>Sneak Key vs. Custom Keybind</li>
     *     <li>Single Press vs. Double Tap</li>
     * </ul>
     * Includes debounce logic to prevent accidental immediate dismounts.
     *
     * @param client The MinecraftClient instance.
     */
    private static void handleDismountKeyPress(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // --- 1. Shoulder state ---
        boolean hasShoulderHamster;
        try {
            hasShoulderHamster = ((PlayerEntityAccessor) client.player).hasAnyShoulderHamster();
        } catch (RuntimeException e) {
            // If the player entity's data tracker is corrupted (missing entries due to mod conflicts),
            // assume no hamster is present to prevent a crash.
            hasShoulderHamster = false;
        }

        // Detect transition: Just mounted this tick
        if (hasShoulderHamster && !hadShoulderHamsterLastTick) {
            // Drain any queued presses on both possible bindings
            KeyBinding vanillaSneak = client.options.sneakKey;
            KeyBinding customDismount = ModKeyBindings.DISMOUNT_HAMSTER_KEY;

            if (vanillaSneak != null) {
                vanillaSneak.setPressed(false);
                while (vanillaSneak.wasPressed()) {}
            }
            if (customDismount != null) {
                customDismount.setPressed(false);
                while (customDismount.wasPressed()) {}
            }

            // Start a short debounce to ignore any immediate post-mount noise.
            dismountDebounceTicks = DISMOUNT_DEBOUNCE_DEFAULT;
            isWaitingForSecondSneakPress = false;
        }

        // Remember shoulder state
        hadShoulderHamsterLastTick = hasShoulderHamster;

        // If no hamster on shoulder, clear double-tap state and bail
        if (!hasShoulderHamster) {
            isWaitingForSecondSneakPress = false;
            return;
        }

        // While the debounce window is active, ignore dismount input
        if (dismountDebounceTicks > 0) {
            dismountDebounceTicks--;
            return;
        }

        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 2. Choose Key ---
        KeyBinding keyToListenFor;
        if (Configs.AHP.dismountTriggerType == DismountTriggerType.CUSTOM_KEYBIND) {
            keyToListenFor = ModKeyBindings.DISMOUNT_HAMSTER_KEY;
        } else {
            keyToListenFor = client.options.sneakKey;
        }

        // --- 3. Detect Press ---
        boolean wasKeyPressed = keyToListenFor != null && keyToListenFor.wasPressed();

        if (wasKeyPressed) {
            // --- 4. Apply Press Type Logic ---
            if (config.dismountPressType.get() == DismountPressType.SINGLE_PRESS) {
                dev.architectury.networking.NetworkManager.sendToServer(new DismountHamsterPayload());
            } else { // DOUBLE_TAP
                long currentTime = System.currentTimeMillis();
                long delayMillis = config.doubleTapDelayTicks.get() * 50L;

                if (isWaitingForSecondSneakPress && (currentTime - lastSneakPressTime) <= delayMillis) {
                    // Double tap confirmed
                    dev.architectury.networking.NetworkManager.sendToServer(new DismountHamsterPayload());
                    isWaitingForSecondSneakPress = false;
                } else {
                    // First press
                    isWaitingForSecondSneakPress = true;
                    lastSneakPressTime = currentTime;
                }
            }
        }

        // --- 5. Timeout for Double Tap ---
        if (isWaitingForSecondSneakPress) {
            long currentTime = System.currentTimeMillis();
            long delayMillis = config.doubleTapDelayTicks.get() * 50L;
            if ((currentTime - lastSneakPressTime) > delayMillis) {
                isWaitingForSecondSneakPress = false;
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
     * Plays sound effects, particles, and an action bar message when the guidebook
     * is retrieved via the config menu.
     */
    public static void handlePlayGuidebookEffects() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Close config screen to un-pause game
        client.setScreen(null);

        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        // Feedback
        player.sendMessage(Text.translatable("message.adorablehamsterpets.guidebook_rediscovered").formatted(Formatting.GOLD), true);
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f, false);
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.7f, 1.5f, false);

        // Particles
        for (int i = 0; i < 50; i++) {
            client.world.addParticle(ParticleTypes.ENCHANT,
                    player.getParticleX(0.6), player.getRandomBodyY(), player.getParticleZ(0.6),
                    (client.world.random.nextDouble() - 0.5) * 0.5,
                    (client.world.random.nextDouble() - 0.5) * 0.5,
                    (client.world.random.nextDouble() - 0.5) * 0.5);
        }
        for (int i = 0; i < 20; i++) {
            client.world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    player.getParticleX(1.0), player.getRandomBodyY(), player.getParticleZ(1.0),
                    (client.world.random.nextDouble() - 0.5) * 0.5,
                    (client.world.random.nextDouble() - 0.5) * 0.5,
                    (client.world.random.nextDouble() - 0.5) * 0.5);
        }
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