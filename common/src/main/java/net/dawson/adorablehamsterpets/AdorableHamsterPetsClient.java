package net.dawson.adorablehamsterpets;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementHudRenderer;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.event.AHPClientScreenEvents;
import net.dawson.adorablehamsterpets.client.gui.widgets.AnnouncementIconAnimator;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.sound.HamsterFlightSoundInstance;
import net.dawson.adorablehamsterpets.client.sound.HamsterThrowSoundInstance;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.DismountPressType;
import net.dawson.adorablehamsterpets.config.DismountTriggerType;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdorableHamsterPetsClient {

    private static final Set<Integer> renderedHamsterIdsThisTick = new HashSet<>();
    private static final Set<Integer> renderedHamsterIdsLastTick = new HashSet<>();
    private static long lastSneakPressTime = 0;
    private static boolean isWaitingForSecondSneakPress = false;

    private static boolean hadShoulderHamsterLastTick = false;
    private static int dismountDebounceTicks = 0;
    private static final int DISMOUNT_DEBOUNCE_DEFAULT = 5;

    // --- Announcement System Fields ---
    private static final AnnouncementHudRenderer announcementHudRenderer = new AnnouncementHudRenderer();
    private static List<AnnouncementManager.PendingNotification> pendingNotifications = Collections.emptyList();
    private static int nextRefreshTicks = 6000; // 5 minutes

    /**
     * Public getter for other client classes to access the cached list of pending notifications.
     * @return The current list of pending notifications.
     */
    public static List<AnnouncementManager.PendingNotification> getPendingNotifications() {
        return pendingNotifications;
    }

    /**
     * Initializes general client-side features like screens, keybinds, and events.
     */
    public static void init() {
        // --- Initializers ---
        RenderTypeRegistry.register(RenderLayer.getCutout(), ModBlocks.GREEN_BEANS_CROP.get(), ModBlocks.CUCUMBER_CROP.get(), ModBlocks.SUNFLOWER_BLOCK.get(), ModBlocks.WILD_CUCUMBER_BUSH.get(), ModBlocks.WILD_GREEN_BEAN_BUSH.get());
        ConfigApiJava.event().onUpdateClient((id, config) -> {
            if (id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "main"))) {
                ConfigDataCache.parseConfig();
                AdorableHamsterPets.LOGGER.info("Reloaded Adorable Hamster Pets item tag config on client following GUI update. *wink wink*");
            }
        });
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> -1, ModItems.HAMSTER_SPAWN_EGG.get());

        // --- Client-Side Packet Handler Registration ---
        ModPackets.registerS2CPackets();

        // Announcement System
        AHPClientScreenEvents.register();

        // --- Event Registrations ---
        ClientTickEvent.CLIENT_POST.register(AdorableHamsterPetsClient::onEndClientTick);
        ClientGuiEvent.RENDER_HUD.register((context, tickCounter) -> announcementHudRenderer.render(context, tickCounter.getTickDelta(true)));
    }

    /**
     * Registers the screen factory. Separate because NeoForge needs to call it natively.
     */
    public static void initScreenHandlers() {
        MenuRegistry.registerScreenFactory(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), HamsterInventoryScreen::new);
    }

    /**
     * Registers entity renderers. Called from a dedicated event handler.
     */
    public static void initEntityRenderers() {
        EntityRendererRegistry.register(ModEntities.HAMSTER, HamsterRenderer::new);
    }

    public static void onHamsterRendered(int entityId) {
        renderedHamsterIdsThisTick.add(entityId);
    }

    private static void onEndClientTick(MinecraftClient client) {
        // --- Announcement System Tick Logic ---
        boolean isGuiOpen = client.currentScreen != null;
        AnnouncementIconAnimator.INSTANCE.tick(isGuiOpen);

        // --- Sync Patchouli State (once per session after world load) ---
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

        // --- Periodic Manifest Refresh ---
        if (--nextRefreshTicks <= 0) {
            nextRefreshTicks = 6000; // Reset timer
            AnnouncementManager.INSTANCE.refreshManifest(); // Fire and forget
            AdorableHamsterPets.LOGGER.debug("[AHP Client Tick] Triggered periodic manifest refresh.");
        }

        // -- Key Presses and Other Tick Logic ---
        if (client.player == null || client.world == null) {
            renderedHamsterIdsThisTick.clear();
            renderedHamsterIdsLastTick.clear();
            return;
        }

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

        Set<Integer> stoppedRendering = new HashSet<>(renderedHamsterIdsLastTick);
        stoppedRendering.removeAll(renderedHamsterIdsThisTick);

        for (Integer entityId : stoppedRendering) {
            dev.architectury.networking.NetworkManager.sendToServer(new UpdateHamsterRenderStatePayload(entityId, false));
        }

        renderedHamsterIdsLastTick.clear();
        renderedHamsterIdsLastTick.addAll(renderedHamsterIdsThisTick);
        renderedHamsterIdsThisTick.clear();

        // --- Dismount Logic ---
        handleDismountKeyPress(client);
    }

    public static void handleStartFlightSound(StartHamsterFlightSoundPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(payload.hamsterEntityId());
        if (entity instanceof HamsterEntity hamster) {
            SoundEvent flightSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_FLYING_SOUNDS, hamster.getRandom());
            if (flightSound != null) {
                client.getSoundManager().play(new HamsterFlightSoundInstance(flightSound, SoundCategory.NEUTRAL, hamster));
            }
        }
    }

    public static void handleStartThrowSound(StartHamsterThrowSoundPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(payload.hamsterEntityId());
        if (entity instanceof HamsterEntity hamster) {
            client.getSoundManager().play(new HamsterThrowSoundInstance(ModSounds.HAMSTER_THROW.get(), SoundCategory.PLAYERS, hamster));
        }
    }

    /**
     * Handles the client-side logic for dismounting a shoulder hamster.
     * <p>
     * This method is called every client tick. It checks the user's configuration to determine
     * which key to listen for (vanilla sneak or a custom keybind) and what press behavior
     * is required (single press or double-tap).
     * <p>
     * When a valid dismount action is detected, it sends a {@link DismountHamsterPayload}
     * to the server to execute the dismount.
     *
     * @param client The MinecraftClient instance.
     */
    private static void handleDismountKeyPress(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // --- 1. Shoulder state ---
        boolean hasShoulderHamster = ((PlayerEntityAccessor) client.player).hasAnyShoulderHamster();

        // Detect the exact tick we JUST mounted (transition: false -> true)
        if (hasShoulderHamster && !hadShoulderHamsterLastTick) {
            // Drain any queued presses on BOTH possible bindings, and clear held states.
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

            // Reset any double-tap state on fresh mount.
            isWaitingForSecondSneakPress = false;

            AdorableHamsterPets.LOGGER.trace("[AHP DEBUG CLIENT] Mount transition detected -> draining input queues and starting debounce ({} ticks).",
                    DISMOUNT_DEBOUNCE_DEFAULT);
        }

        // Remember shoulder state for next tick.
        hadShoulderHamsterLastTick = hasShoulderHamster;

        // If no hamster on shoulder, clear double-tap state and bail.
        if (!hasShoulderHamster) {
            if (isWaitingForSecondSneakPress) {
                isWaitingForSecondSneakPress = false;
            }
            return;
        }

        // While the debounce window is active, ignore dismount input.
        if (dismountDebounceTicks > 0) {
            dismountDebounceTicks--;
            return;
        }

        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 2. Choose which key to listen for based on config ---
        KeyBinding keyToListenFor;
        if (Configs.AHP.dismountTriggerType == DismountTriggerType.CUSTOM_KEYBIND) {
            keyToListenFor = ModKeyBindings.DISMOUNT_HAMSTER_KEY;
        } else { // SNEAK_KEY
            keyToListenFor = client.options.sneakKey;
        }

        // --- 3. Edge detection: call wasPressed() ONCE and store the result ---
        boolean wasKeyPressed = keyToListenFor != null && keyToListenFor.wasPressed();

        AdorableHamsterPets.LOGGER.trace(
                "[AHP DEBUG CLIENT] Tick Handler: Listening for '{}'. wasPressed() = {}",
                keyToListenFor != null ? keyToListenFor.getTranslationKey() : "null-binding",
                wasKeyPressed
        );

        if (wasKeyPressed) {
            AdorableHamsterPets.LOGGER.debug("[AHP DEBUG CLIENT] Tick Handler: SINGLE_PRESS detected. Press type config: {}",
                    config.dismountPressType.get());

            // --- 4. Apply press type logic (SINGLE vs DOUBLE) ---
            if (config.dismountPressType.get() == DismountPressType.SINGLE_PRESS) {
                // Single press always triggers the dismount
                dev.architectury.networking.NetworkManager.sendToServer(new DismountHamsterPayload());
            } else { // DOUBLE_TAP
                long currentTime = System.currentTimeMillis();
                long delayMillis = config.doubleTapDelayTicks.get() * 50L;

                if (isWaitingForSecondSneakPress && (currentTime - lastSneakPressTime) <= delayMillis) {
                    AdorableHamsterPets.LOGGER.trace("[AHP DEBUG CLIENT] Tick Handler: DOUBLE_TAP second press detected. Sending dismount payload.");
                    dev.architectury.networking.NetworkManager.sendToServer(new DismountHamsterPayload());
                    isWaitingForSecondSneakPress = false; // Reset the double-tap state
                } else {
                    AdorableHamsterPets.LOGGER.trace("[AHP DEBUG CLIENT] Tick Handler: DOUBLE_TAP first press detected. Starting timer.");
                    isWaitingForSecondSneakPress = true;
                    lastSneakPressTime = currentTime;
                }
            }
        }

        // --- 5. Handle timeout for the double-tap window ---
        if (isWaitingForSecondSneakPress) {
            long currentTime = System.currentTimeMillis();
            long delayMillis = config.doubleTapDelayTicks.get() * 50L;
            if ((currentTime - lastSneakPressTime) > delayMillis) {
                AdorableHamsterPets.LOGGER.trace("[AHP DEBUG CLIENT] Tick Handler: DOUBLE_TAP timed out.");
                isWaitingForSecondSneakPress = false;
            }
        }
    }
}