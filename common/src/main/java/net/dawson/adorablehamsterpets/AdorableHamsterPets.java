package net.dawson.adorablehamsterpets;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.utils.Env;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.command.ModCommands;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.effect.ModStatusEffects;
import net.dawson.adorablehamsterpets.event.AHPCommonEvents;
import net.dawson.adorablehamsterpets.item.ModItemGroups;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.networking.payload.PlayGuidebookEffectsPayload;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterBedUtil;
import net.dawson.adorablehamsterpets.util.HamsterNbtUtil;
import net.dawson.adorablehamsterpets.util.HamsterPlacementUtil;
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.dawson.adorablehamsterpets.util.ModLootTableModifiers;
import net.dawson.adorablehamsterpets.util.RedstoneFeverCureCreditState;
import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AdorableHamsterPets {
	public static final String MOD_ID = "adorablehamsterpets";
	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

	public static AhpRootConfig ROOT_CONFIG;
	public static AhpSupporterConfig SUPPORTER_CONFIG;
	public static AhpMainConfig MAIN_CONFIG;
	public static AhpWorldGenConfig WORLD_GEN_CONFIG;
	public static AhpUiConfig UI_CONFIG;
	public static AhpItemConfig ITEM_CONFIG;

	/**
	 * Initializes all DeferredRegister instances.
	 * This must be called during mod construction (e.g., the loader-specific entrypoint's constructor or onInitialize).
	 */
	public static void initRegistries() {
		ROOT_CONFIG = Configs.AHP_ROOT;
		SUPPORTER_CONFIG = Configs.AHP_SUPPORTER;
		MAIN_CONFIG = Configs.AHP_MAIN;
		WORLD_GEN_CONFIG = Configs.AHP_WORLDGEN;
		UI_CONFIG = Configs.AHP_UI;
		ITEM_CONFIG = Configs.AHP_ITEMS;
		ModEntities.register();
		ModDataComponentTypes.registerDataComponentTypes();
		ModSounds.register();
		ModBlocks.register();
		ModItems.register();
		ModItemGroups.register();
		ModScreenHandlers.register();
		ModCriteria.register();
		ModBlockEntities.register();
		ModParticles.register();
		ModStatusEffects.register();
	}

	/**
	 * Initializes common setup logic that needs to run after registries are populated.
	 * This is called from FMLCommonSetupEvent on NeoForge and onInitialize on Fabric.
	 */
	public static void initCommonSetup() {
		// Check if inside data generation environment. If so, skip runtime-only logic to prevent crashes
		if (System.getProperty("fabric-api.datagen") == null) {
			// --- Configuration Parsing ---
			ConfigDataCache.parseConfig();
			ModEntitySpawns.parseConfig();
			ModWorldGeneration.parseConfig();

			// --- Config-Dependent Systems ---
			HamsterPaletteManager.init();
			ModRegistries.registerCompostables();
			ModRegistries.registerDispenserBehaviors();
			ModRegistries.registerFuels();
			ModLootTableModifiers.init();

			// --- Networking Registration ---
			// On the server, explicitly register the S2C payload types so it knows how to send them
			// On the client, the types are registered implicitly when the S2C receivers are registered
			if (Platform.getEnvironment() == Env.SERVER) {
				ModPackets.registerPayloads();
			}
			ModPackets.registerC2SPackets();

			// --- World Gen ---
			ModWorldGeneration.registerBiomeModifications();

			// --- Events ---
			AHPCommonEvents.init();
			AcornRingUtil.init();
			PlayerEvent.PLAYER_JOIN.register(AdorableHamsterPets::onPlayerJoin);
			PlayerEvent.PLAYER_CLONE.register(AdorableHamsterPets::onPlayerClone);
			PlayerEvent.PLAYER_RESPAWN.register(AdorableHamsterPets::onPlayerRespawn);
			PlayerEvent.CHANGE_DIMENSION.register(AdorableHamsterPets::onPlayerChangeDimension);
			CommandRegistrationEvent.EVENT.register(ModCommands::register);
		}
	}

	/**
	 * Initializes entity attributes. This must be called after registries are initialized
	 * but before the main setup event, typically during mod construction.
	 */
	public static void initAttributes() {
		EntityAttributeRegistry.register(ModEntities.HAMSTER, HamsterEntity::createHamsterAttributes);
	}

	/**
	 * Registers entity spawn placements.
	 * This must be called at specific times depending on the loader:
	 * - Fabric: During onInitialize.
	 * - NeoForge: During the RegisterSpawnPlacementsEvent (or queued before it).
	 */
	public static void registerSpawnPlacements() {
		ModSpawnPlacements.register(ModEntities.HAMSTER, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntitySpawns::isValidHamsterNaturalSpawn);
	}

	/**
	 * An event handler that is called whenever a player joins the server.
	 * <p>
	 * This method is responsible for the one-time delivery of the Hamster Guide Book. It checks if the player
	 * has the {@code adorablehamsterpets:technical/has_received_initial_guidebook} advancement. If they do not,
	 * and if the {@code uiTweaks.enableAutoGuidebookDelivery} config option is enabled, it directly gives
	 * the player the book, plays effects, and then grants the advancement flag to prevent future deliveries.
	 *
	 * @param player The ServerPlayerEntity who has just joined the world.
	 */
	private static void onPlayerJoin(ServerPlayer player) {
		AcornRingUtil.defer(player, null);
		RedstoneFeverCureCreditState.consume(player);
		if (Configs.AHP_UI.enableAutoGuidebookDelivery) {
			PlayerAdvancements advancementTracker = player.getAdvancements();
			Identifier flagAdvId = Identifier.fromNamespaceAndPath(MOD_ID, "technical/has_received_initial_guidebook");
			net.minecraft.advancements.AdvancementHolder flagAdvancementEntry = player.level().getServer().getAdvancements().get(flagAdvId);

			if (flagAdvancementEntry != null) {
				AdvancementProgress flagProgress = advancementTracker.getOrStartProgress(flagAdvancementEntry);
				if (!flagProgress.isDone()) {
					// Deliver guidebook (grant advancement, no fallback message, don't play effects, don't close screen)
					deliverGuidebook(player, true, false, false, false);
					LOGGER.info("Gave 'Hamster Tips' guide book to player {}.", player.getName().getString());
				}
			} else {
				LOGGER.warn("Could not find flag advancement: {}", flagAdvId);
			}
		}

		// Sync initial shoulder data
		((PlayerEntityAccessor) player).adorablehamsterpets$syncHamsterState();

		// Upgrade any old hamster tips guide books in the player's inventory
		replaceOldBooksInInventory(player.getInventory());

		// Initialize guidebook tracking state based on current inventory
		// NOTE: Auto-delivered books are intentionally silent
		PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
		accessor.ahp$initGuideBookTracking(accessor.ahp$computeHasGuideBook(player));
	}

	/**
	 * An event handler called when a player changes dimensions.
	 * Forces a resync of shoulder data (only necessary on 1.20.1).
	 */
	private static void onPlayerChangeDimension(ServerPlayer player, ResourceKey<Level> oldWorld, ResourceKey<Level> newWorld) {
		((PlayerEntityAccessor) player).adorablehamsterpets$syncHamsterState();
	}

	/**
	 * An event handler called when a player respawns.
	 * Used to ensure client-side shoulder data correctly synchronizes.
	 */
	private static void onPlayerRespawn(ServerPlayer player, boolean conqueredEnd, Entity.RemovalReason reason) {
		AcornRingUtil.defer(player, null);
		((PlayerEntityAccessor) player).adorablehamsterpets$syncHamsterState();
	}

	/**
	 * An event handler called when a player entity is cloned upon respawn after death.
	 * <p>
	 * NOTE: This event does not fire for dimension travel.
	 * <p>
	 *
	 * @param oldPlayer The player entity instance before death.
	 * @param newPlayer The new player entity instance created upon respawn.
	 * @param wasDeath_UNRELIABLE A boolean flag that is not reliable on all platforms and is ignored.
	 */
	private static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wasDeath_UNRELIABLE) {
		AcornRingUtil.defer(newPlayer, null);
		PlayerEntityAccessor oldPlayerAccessor = (PlayerEntityAccessor) oldPlayer;
		PlayerEntityAccessor newPlayerAccessor = (PlayerEntityAccessor) newPlayer;

		// --- 1. Transfer Transient & Tracking Data ---
		newPlayerAccessor.ahp$getInTransitHamsters().addAll(oldPlayerAccessor.ahp$getInTransitHamsters());
		newPlayerAccessor.ahp$setTransitTimer(oldPlayerAccessor.ahp$getTransitTimer());
		newPlayerAccessor.ahp$setSupporterCrownTheme(oldPlayerAccessor.ahp$getSupporterCrownTheme());

		// --- 2. Handle "Keep on Shoulder" Scenario ---
		if (Configs.AHP_MAIN.keepHamstersOnShoulderOnDeath) {
			newPlayerAccessor.adorablehamsterpets$getMountOrderQueue().addAll(oldPlayerAccessor.adorablehamsterpets$getMountOrderQueue());
			for (ShoulderLocation location : ShoulderLocation.values()) {
				CompoundTag shoulderNbt = oldPlayerAccessor.getShoulderHamster(location);
				if (!shoulderNbt.isEmpty()) {
					newPlayerAccessor.setShoulderHamster(location, shoulderNbt);
					AdorableHamsterPets.LOGGER.debug("Player {} respawned with 'Keep on Shoulder' enabled. Transferring {} hamster to new entity.", newPlayer.getName().getString(), location);
				}
			}
			return;
		}

		// --- 3. Handle Spawning at Death Location (Default) ---
		ServerLevel world = ((ServerLevel) oldPlayer.level());
		BlockPos deathPos = oldPlayer.blockPosition();
		boolean isVoidDeath = deathPos.getY() < world.getMinY();
		Set<BlockPos> occupiedSpawnPositions = new HashSet<>();

		for (ShoulderLocation location : ShoulderLocation.values()) {
			CompoundTag shoulderNbt = oldPlayerAccessor.getShoulderHamster(location);
			if (shoulderNbt.isEmpty()) continue;

			// Modify NBT to set the knocked-out state before spawning
			CompoundTag modifiedNbt = HamsterNbtUtil.setKnockedOutInNbt(shoulderNbt);
			HamsterEntity hamster = HamsterNbtUtil.createFromNbt(world, oldPlayer, modifiedNbt);
			if (hamster == null) continue;

			BlockPos finalSpawnPos = null;
			ServerLevel targetWorld = world;
			boolean positionAlreadySet = false;

			// Attempt to find a local safe spot first
			Optional<BlockPos> safePosOpt = HamsterPlacementUtil.findSafeSpawnPosition(deathPos, targetWorld, 5, occupiedSpawnPositions, hamster);

			if (safePosOpt.isPresent()) {
				finalSpawnPos = safePosOpt.get();
			} else if (isVoidDeath) {
				// --- VOID RESCUE PROTOCOL ---
				AdorableHamsterPets.LOGGER.info("Player died in the void. Initiating Void Rescue Protocol for hamster on {}.", location);

				// 1. Try Linked Bed
				if (hamster.getLinkedBedPos().isPresent()) {
					GlobalPos linkedBed = hamster.getLinkedBedPos().get();
					ServerLevel bedWorld = oldPlayer.level().getServer().getLevel(linkedBed.dimension());
					if (bedWorld != null) {
						BlockPos bedPos = linkedBed.pos();
						BlockState bedState = bedWorld.getBlockState(bedPos);

						// Verify bed is valid and unoccupied before sending them
						if (bedState.getBlock() instanceof HamsterBedBlock && !bedState.getValue(HamsterBedBlock.OCCUPIED)) {
							targetWorld = bedWorld;
							finalSpawnPos = bedPos; // Satisfy fallback check
							HamsterBedUtil.forceTeleportAndSleepInBed(hamster, bedWorld, bedPos, bedState);
							positionAlreadySet = true;
						}
					}
				}

				// 2. Try Player's Respawn Point
				if (finalSpawnPos == null) {
					net.minecraft.server.level.ServerPlayer.RespawnConfig respawnConfig = newPlayer.getRespawnConfig();
					// 26.2 port: respawn dimension/position now live on a RespawnConfig record
					net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> respawnDimension =
							respawnConfig != null ? respawnConfig.respawnData().dimension() : net.minecraft.world.level.Level.OVERWORLD;
					ServerLevel spawnWorld = oldPlayer.level().getServer().getLevel(respawnDimension);
					if (spawnWorld != null) {
						targetWorld = spawnWorld;
						BlockPos spawnPoint = respawnConfig != null ? respawnConfig.respawnData().pos() : null;
						if (spawnPoint != null) {
							finalSpawnPos = spawnPoint;
						} else {
							finalSpawnPos = targetWorld.getRespawnData().pos();
						}
						// Find safe spot around the respawn point so they don't spawn inside a block
						finalSpawnPos = HamsterPlacementUtil.findSafeSpawnPosition(finalSpawnPos, targetWorld, 5, occupiedSpawnPositions, hamster).orElse(finalSpawnPos);
					}
				}
			}

			// Ultimate fallback
			if (finalSpawnPos == null) {
				finalSpawnPos = deathPos;
			}

			occupiedSpawnPositions.add(finalSpawnPos);

			// Set initial position if not already handled by the bed rescue
			if (!positionAlreadySet) {
				hamster.snapTo(finalSpawnPos.getX() + 0.5, finalSpawnPos.getY(), finalSpawnPos.getZ() + 0.5, 0, 0);
			}

			// Spawn the entity in the correct world
			targetWorld.addFreshEntityWithPassengers(hamster);

			// Randomize the Yaw so they don't all face the exact same direction
			float randomYaw = targetWorld.getRandom().nextFloat() * 360.0F;
			hamster.setYBodyRot(randomYaw);
			hamster.setYHeadRot(randomYaw);

			AdorableHamsterPets.LOGGER.debug("Player {} died. Spawning {} hamster at {} in target world {}.", oldPlayer.getName().getString(), location, finalSpawnPos, targetWorld.dimension().identifier());
		}
		// By not transferring any data to newPlayer, they will respawn with empty shoulders.
	}

	/**
	 * Centralized utility for delivering the Hamster Tips guidebook to a player.
	 * Handles item creation, Patchouli component assignment, inventory insertion,
	 * advancement granting, and visual/audio effects.
	 *
	 * @param player The player receiving the book.
	 * @param grantInitialAdvancement If true, grants the 'has_received_initial_guidebook' flag.
	 * @param sendFallbackMessage If true, sends the introductory chat message.
	 * @param playEffects If true, triggers the client-side 'rediscovered' effects.
	 * @param closeScreen If true, tells the client to close their current GUI screen.
	 */
	public static void deliverGuidebook(ServerPlayer player, boolean grantInitialAdvancement, boolean sendFallbackMessage, boolean playEffects, boolean closeScreen) {
		// --- 1. Create the Book ItemStack Directly ---
		ItemStack bookStack = new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get());
		@SuppressWarnings("unchecked")
		DataComponentType<Identifier> bookComponent = (DataComponentType<Identifier>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(Identifier.fromNamespaceAndPath("patchouli", "book")).map(net.minecraft.core.Holder::value).orElse(null);
		if (bookComponent != null) {
			bookStack.set(bookComponent, Identifier.fromNamespaceAndPath(MOD_ID, "hamster_tips_guide_book"));
		} else {
			LOGGER.error("Could not find Patchouli's book component type! Guidebook will not be functional.");
		}

		// --- 2. Give the Item to the Player ---
		player.getInventory().placeItemBackInInventory(bookStack);

		// --- 3. Grant the Flag Advancement ---
		if (grantInitialAdvancement) {
			PlayerAdvancements advancementTracker = player.getAdvancements();
			Identifier flagAdvId = Identifier.fromNamespaceAndPath(MOD_ID, "technical/has_received_initial_guidebook");
			net.minecraft.advancements.AdvancementHolder flagAdvancementEntry = player.level().getServer().getAdvancements().get(flagAdvId);

			if (flagAdvancementEntry != null) {
				for (String criterion : flagAdvancementEntry.value().criteria().keySet()) {
					advancementTracker.award(flagAdvancementEntry, criterion);
				}
			}
		}

		// --- 4. Send Fallback Message ---
		if (sendFallbackMessage) {
			player.sendSystemMessage(Component.translatable("message.adorablehamsterpets.guidebook_obtained_fallback").withStyle(ChatFormatting.GOLD));
		}

		// --- 5. Trigger Client Effects ---
		if (playEffects) {
			NetworkManager.sendToPlayer(player, new PlayGuidebookEffectsPayload(closeScreen));
		}
	}

	/**
	 * Iterates through an inventory and replaces any outdated Hamster Guide Books
	 * with the new Patchouli-compatible version added in version 3.3.0.
	 *
	 * @param inventory The inventory to scan and upgrade.
	 */
	public static void replaceOldBooksInInventory(Container inventory) {
		// --- 1. Get the component type for Patchouli books ---
		// Suppress the "unchecked" warning because the 'patchouli:book' component is of type ComponentType<Identifier>.
		@SuppressWarnings("unchecked")
		DataComponentType<Identifier> bookComponent = (DataComponentType<Identifier>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(Identifier.fromNamespaceAndPath("patchouli", "book")).map(net.minecraft.core.Holder::value).orElse(null);

		if (bookComponent == null) {
			// This can happen if Patchouli is not present, so fail gracefully.
			return;
		}

		// --- 2. Iterate through all slots in the provided inventory ---
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);

			// --- 3. Check if the item is an OLD guide book ---
			// It's an old book if it's the guide book item but lacks the Patchouli component.
			if (stack.is(ModItems.HAMSTER_GUIDE_BOOK.get()) && !stack.has(bookComponent)) {
				// --- 4. Create the new, upgraded book stack ---
				ItemStack newBookStack = new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get(), stack.getCount());
				newBookStack.set(bookComponent, Identifier.fromNamespaceAndPath(MOD_ID, "hamster_tips_guide_book"));

				// --- 5. Replace the old stack with the new one ---
				inventory.setItem(i, newBookStack);
				LOGGER.info("Upgraded an old Hamster Tips Guide Book to the new Patchouli version.");
			}
		}
	}
}
