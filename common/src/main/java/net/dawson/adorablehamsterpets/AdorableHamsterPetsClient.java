package net.dawson.adorablehamsterpets;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
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
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.client.render.LeafJiggleManager;
import net.dawson.adorablehamsterpets.client.sound.HamsterTreeLoopSoundInstance;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.renderer.HamsterTreeSearcherRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AdorableHamsterPetsClient {

    // --- Rendering State ---
    private static final Set<Integer> renderedHamsterIdsThisTick = new HashSet<>();
    private static final Set<Integer> renderedHamsterIdsLastTick = new HashSet<>();

    // --- Guidebook Warning State ---
    private static int clientSessionTimer = 0;

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

    // --- Tree Heist Feature ---
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

        // --- Config Reload Listener ---
        ConfigApiJava.event().onUpdateClient((id, config) -> {
            if (id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "main"))) {
                ConfigDataCache.parseConfig();
                AdorableHamsterPets.LOGGER.info("Reloaded Adorable Hamster Pets item tag config on client following GUI update. *wink wink*");
            }
        });

        // --- Item Colors ---
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> -1, ModItems.HAMSTER_SPAWN_EGG.get());

        // --- Networking Registration ---
        // On 1.20.1, register all  packets on both sides using the safe common method.
        ModPackets.registerCommonPackets();

        // --- Announcement System ---
        AHPClientScreenEvents.register();

        // --- Event Registrations ---
        ClientTickEvent.CLIENT_POST.register(AdorableHamsterPetsClient::onEndClientTick);
        ClientGuiEvent.RENDER_HUD.register((context, tickDelta) -> announcementHudRenderer.render(context, tickDelta));

        // --- Register Client Commands ---
        ClientCommandRegistrationEvent.EVENT.register(ModClientCommands::register);

        // --- Guidebook Warning Timer Reset ---
        // Reset guidebook warning timer whenever the player joins a world
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> clientSessionTimer = 0);

        // --- Register Tree Heist Sound & Jiggle Logic ---
        EntityEvent.ADD.register((entity, world) -> {
            if (world.isClient() && entity instanceof HamsterTreeSearcherEntity searcher) {
                MinecraftClient client = MinecraftClient.getInstance();

                // 1. Sound Logic
                if (!activeTreeSounds.containsKey(searcher.getId())) {
                    HamsterTreeLoopSoundInstance sound = new HamsterTreeLoopSoundInstance(searcher);
                    client.getSoundManager().play(sound);
                    activeTreeSounds.put(searcher.getId(), sound);
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
                    // Only if it's tamed hamster and owned by player
                    if (hamster.isTamed() && hamster.isOwner(player)) {
                        // Send a typed packet for 1.20.1
                        ModPackets.CHANNEL.sendToServer(new ModPackets.RequestHamsterMountC2SPacket(hamster.getId()));
                        return EventResult.interruptTrue(); // Cancel default interaction to prevent sitting
                    }
                }

                // 2. Hamster Riding
                if (Configs.AHP.enableMountableHamsters.get() && ModKeyBindings.RIDE_HAMSTER_KEY.isPressed()) {
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

    /**
     * Registers entity renderers. Called from a dedicated event handler.
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
        if (ridingHamster && Configs.AHP.enableMountableHamsters.get()) {
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

        // Handle Throw Hamster Keybind
        if (ModKeyBindings.THROW_HAMSTER_KEY.wasPressed()) {
            final AhpConfig currentConfig = AdorableHamsterPets.CONFIG;
            if (!currentConfig.enableHamsterThrowing) {
                client.player.sendMessage(Text.translatable("message.adorablehamsterpets.throwing_disabled"), true);
            } else {
                boolean lookingAtReachableBlock = client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK;
                boolean hasShoulderHamsterClient = ((PlayerEntityAccessor) client.player).hasAnyShoulderHamster();

                if (!lookingAtReachableBlock && hasShoulderHamsterClient) {
                    // Send a typed packet for 1.20.1
                    ModPackets.CHANNEL.sendToServer(new ModPackets.ThrowHamsterC2SPacket());
                }
            }
        }

        // --- 4. Render State Tracking ---
        // Determines which hamsters stopped rendering this tick (went off-screen)
        Set<Integer> stoppedRendering = new HashSet<>(renderedHamsterIdsLastTick);
        stoppedRendering.removeAll(renderedHamsterIdsThisTick);

        for (Integer entityId : stoppedRendering) {
            // Send the render state update packet
            // Using ByteBuf because Architectury 1.20.1 helper logic for update packet
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(entityId);
            buf.writeBoolean(false); // isRendering = false
            // Send a typed packet for 1.20.1
            ModPackets.CHANNEL.sendToServer(new ModPackets.UpdateRenderStateC2SPacket(entityId, false));
        }

        renderedHamsterIdsLastTick.clear();
        renderedHamsterIdsLastTick.addAll(renderedHamsterIdsThisTick);
        renderedHamsterIdsThisTick.clear();

        // --- 5. Hamster Dismount From Shoulder Logic ---
        handleDismountKeyPress(client);

        // --- 6. Guidebook Warning Logic ---
        handleGuidebookWarning(client);
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

        // Fast exit if already seen by this player
        if (config.playersWhoHaveSeenGuidebookWarning.contains(username)) return;

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
        return player.getInventory().contains(new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get()));
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
        message.append(Text.translatable("message.adorablehamsterpets.warning.context").formatted(Formatting.GRAY));

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
     * shoulder. Checks the user's configuration to determine if dismount requires:
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

        // Detect the exact tick we JUST mounted (transition: false -> true)
        if (hasShoulderHamster && !hadShoulderHamsterLastTick) {
            // Drain any queued presses on BOTH possible bindings
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
        }

        // Remember shoulder state for next tick
        hadShoulderHamsterLastTick = hasShoulderHamster;

        // If no hamster, bail
        if (!hasShoulderHamster) {
            isWaitingForSecondSneakPress = false;
            return;
        }

        // Handle debounce
        if (dismountDebounceTicks > 0) {
            dismountDebounceTicks--;
            return;
        }

        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 2. Choose Key ---
        KeyBinding keyToListenFor;
        if (Configs.AHP.dismountTriggerType == DismountTriggerType.CUSTOM_KEYBIND) {
            keyToListenFor = ModKeyBindings.DISMOUNT_HAMSTER_KEY;
        } else { // SNEAK_KEY
            keyToListenFor = client.options.sneakKey;
        }

        // --- 3. Detect Press ---
        boolean wasKeyPressed = keyToListenFor != null && keyToListenFor.wasPressed();

        if (wasKeyPressed) {
            // --- 4. Apply Press Type Logic ---
            if (config.dismountPressType.get() == DismountPressType.SINGLE_PRESS) {
                // Single press always triggers the dismount
                // Send a typed packet for 1.20.1
                ModPackets.CHANNEL.sendToServer(new ModPackets.DismountHamsterC2SPacket());
            } else { // DOUBLE_TAP
                long currentTime = System.currentTimeMillis();
                long delayMillis = config.doubleTapDelayTicks.get() * 50L;

                if (isWaitingForSecondSneakPress && (currentTime - lastSneakPressTime) <= delayMillis) {
                    // Second press was within the delay window, trigger dismount
                    // Send a typed packet for 1.20.1
                    ModPackets.CHANNEL.sendToServer(new ModPackets.DismountHamsterC2SPacket());
                    isWaitingForSecondSneakPress = false; // Reset the double-tap state
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
     * Handles the SyncShoulderData packet on the client.
     */
    public static void handleSyncShoulderData(int entityId, net.minecraft.nbt.NbtCompound data) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            net.minecraft.entity.Entity entity = client.world.getEntityById(entityId);
            if (entity instanceof net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor accessor) {
                accessor.adorablehamsterpets$setRawShoulderData(data);
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
     * Plays sound effects, particles, and an action bar message when the guidebook is retrieved.
     */
    public static void handlePlayGuidebookEffects(ModPackets.PlayGuidebookEffectsS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Close config screen only if requested
        if (packet.closeScreen()) {
            client.setScreen(null);
        }

        PlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return;

        // Feedback
        player.sendMessage(Text.translatable("message.adorablehamsterpets.guidebook_rediscovered").formatted(Formatting.GOLD), true);
        world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f, false);
        world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.7f, 1.5f, false);

        // Particles
        for (int i = 0; i < 50; i++) {
            world.addParticle(ParticleTypes.ENCHANT,
                    player.getParticleX(0.6), player.getRandomBodyY(), player.getParticleZ(0.6),
                    (world.random.nextDouble() - 0.5) * 0.5,
                    (world.random.nextDouble() - 0.5) * 0.5,
                    (world.random.nextDouble() - 0.5) * 0.5);
        }
        for (int i = 0; i < 20; i++) {
            world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    player.getParticleX(1.0), player.getRandomBodyY(), player.getParticleZ(1.0),
                    (world.random.nextDouble() - 0.5) * 0.5,
                    (world.random.nextDouble() - 0.5) * 0.5,
                    (world.random.nextDouble() - 0.5) * 0.5);
        }
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