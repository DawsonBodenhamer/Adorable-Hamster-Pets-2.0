package net.dawson.adorablehamsterpets;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.command.ModCommands;
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
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

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
	 * This is called from FMLCommonSetupEvent on Forge and onInitialize on Fabric.
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
			// On 1.20.1, register all packets on both sides using safe common method
			ModPackets.registerCommonPackets();

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
		// Use SpawnRestriction.Location on 1.20.1
		ModSpawnPlacements.register(ModEntities.HAMSTER, SpawnRestriction.Location.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
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
	private static void onPlayerJoin(ServerPlayerEntity player) {
		AcornRingUtil.defer(player, null);
		RedstoneFeverCureCreditState.consume(player);
		if (Configs.AHP_UI.enableAutoGuidebookDelivery) {
			PlayerAdvancementTracker advancementTracker = player.getAdvancementTracker();
			Identifier flagAdvId = Identifier.of(MOD_ID, "technical/has_received_initial_guidebook");
			Advancement flagAdvancement = player.server.getAdvancementLoader().get(flagAdvId);

			if (flagAdvancement != null) {
				AdvancementProgress flagProgress = advancementTracker.getProgress(flagAdvancement);
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
	private static void onPlayerChangeDimension(ServerPlayerEntity player, RegistryKey<World> oldWorld, RegistryKey<World> newWorld) {
		((PlayerEntityAccessor) player).adorablehamsterpets$syncHamsterState();
	}

	/**
	 * Event handler called when a player respawns.
	 * Used to ensure client-side shoulder data correctly synchronizes.
	 */
	private static void onPlayerRespawn(ServerPlayerEntity player, boolean conqueredEnd) {
		AcornRingUtil.defer(player, null);
		((PlayerEntityAccessor) player).adorablehamsterpets$syncHamsterState();
	}

	/**
	 * Event handler called when a player entity is cloned upon respawn after death.
	 * <p>
	 * NOTE: This event does not fire for dimension travel.
	 * <p>
	 *
	 * @param oldPlayer The player entity instance before death.
	 * @param newPlayer The new player entity instance created upon respawn.
	 * @param wasDeath_UNRELIABLE A boolean flag that is not reliable on all platforms and is ignored.
	 */
	private static void onPlayerClone(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean wasDeath_UNRELIABLE) {
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
				NbtCompound shoulderNbt = oldPlayerAccessor.getShoulderHamster(location);
				if (!shoulderNbt.isEmpty()) {
					newPlayerAccessor.setShoulderHamster(location, shoulderNbt);
					AdorableHamsterPets.LOGGER.debug("Player {} respawned with 'Keep on Shoulder' enabled. Transferring {} hamster to new entity.", newPlayer.getName().getString(), location);
				}
			}
			return;
		}

		// --- 3. Handle Spawning at Death Location (Default) ---
		ServerWorld world = oldPlayer.getServerWorld();
		BlockPos deathPos = oldPlayer.getBlockPos();
		boolean isVoidDeath = deathPos.getY() < world.getBottomY();
		Set<BlockPos> occupiedSpawnPositions = new HashSet<>();

		for (ShoulderLocation location : ShoulderLocation.values()) {
			NbtCompound shoulderNbt = oldPlayerAccessor.getShoulderHamster(location);
			if (shoulderNbt.isEmpty()) continue;

			// Modify NBT to set the knocked-out state before spawning
			NbtCompound modifiedNbt = HamsterNbtUtil.setKnockedOutInNbt(shoulderNbt);
			HamsterEntity hamster = HamsterNbtUtil.createFromNbt(world, oldPlayer, modifiedNbt);
			if (hamster == null) continue;

			BlockPos finalSpawnPos = null;
			ServerWorld targetWorld = world;
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
					ServerWorld bedWorld = oldPlayer.getServer().getWorld(linkedBed.getDimension());
					if (bedWorld != null) {
						BlockPos bedPos = linkedBed.getPos();
						BlockState bedState = bedWorld.getBlockState(bedPos);

						// Verify bed is valid and unoccupied before sending them
						if (bedState.getBlock() instanceof HamsterBedBlock && !bedState.get(HamsterBedBlock.OCCUPIED)) {
							targetWorld = bedWorld;
							finalSpawnPos = bedPos; // Satisfy fallback check
							HamsterBedUtil.forceTeleportAndSleepInBed(hamster, bedWorld, bedPos, bedState);
							positionAlreadySet = true;
						}
					}
				}

				// 2. Try Player's Respawn Point
				if (finalSpawnPos == null) {
					ServerWorld spawnWorld = oldPlayer.getServer().getWorld(newPlayer.getSpawnPointDimension());
					if (spawnWorld != null) {
						targetWorld = spawnWorld;
						BlockPos spawnPoint = newPlayer.getSpawnPointPosition();
						if (spawnPoint != null) {
							finalSpawnPos = spawnPoint;
						} else {
							finalSpawnPos = targetWorld.getSpawnPos();
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
				hamster.refreshPositionAndAngles(finalSpawnPos.getX() + 0.5, finalSpawnPos.getY(), finalSpawnPos.getZ() + 0.5, 0, 0);
			}

			// Spawn the entity in the correct world
			targetWorld.spawnEntityAndPassengers(hamster);

			// Randomize the Yaw so they don't all face the exact same direction
			float randomYaw = targetWorld.random.nextFloat() * 360.0F;
			hamster.setBodyYaw(randomYaw);
			hamster.setHeadYaw(randomYaw);

			AdorableHamsterPets.LOGGER.debug("Player {} died. Spawning {} hamster at {} in target world {}.", oldPlayer.getName().getString(), location, finalSpawnPos, targetWorld.getRegistryKey().getValue());
		}
		// By not transferring any data to newPlayer, they will respawn with empty shoulders.
	}

	/**
	 * Centralized utility for delivering the Hamster Tips guidebook to a player.
	 * Handles item creation, Patchouli NBT assignment, inventory insertion,
	 * advancement granting, and visual/audio effects.
	 *
	 * @param player The player receiving the book.
	 * @param grantInitialAdvancement If true, grants the 'has_received_initial_guidebook' flag.
	 * @param sendFallbackMessage If true, sends the introductory chat message.
	 * @param playEffects If true, triggers the client-side 'rediscovered' effects.
	 * @param closeScreen If true, tells the client to close their current GUI screen.
	 */
    public static void deliverGuidebook(ServerPlayerEntity player, boolean grantInitialAdvancement, boolean sendFallbackMessage, boolean playEffects, boolean closeScreen) {
		// --- 1. Create the Book ItemStack Directly on 1.20.1 ---
		ItemStack bookStack = new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get());
		NbtCompound nbt = bookStack.getOrCreateNbt();
		nbt.putString("patchouli:book", "adorablehamsterpets:hamster_tips_guide_book");

		// --- 2. Give the Item to the Player ---
		player.getInventory().offerOrDrop(bookStack);

		// --- 3. Grant the Flag Advancement ---
		if (grantInitialAdvancement) {
			PlayerAdvancementTracker advancementTracker = player.getAdvancementTracker();
			Identifier flagAdvId = Identifier.of(MOD_ID, "technical/has_received_initial_guidebook");
			Advancement flagAdvancement = player.server.getAdvancementLoader().get(flagAdvId);

			if (flagAdvancement != null) {
				for (String criterion : flagAdvancement.getCriteria().keySet()) {
					advancementTracker.grantCriterion(flagAdvancement, criterion);
				}
			}
		}

		// --- 4. Send Fallback Message ---
		if (sendFallbackMessage) {
			player.sendMessage(Text.translatable("message.adorablehamsterpets.guidebook_obtained_fallback").formatted(Formatting.GOLD), false);
		}

		// --- 5. Trigger Client Effects ---
		ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayGuidebookEffectsS2CPacket(closeScreen));
	}

	/**
	 * Iterates through an inventory and replaces any outdated Hamster Guide Books
	 * with the new Patchouli-compatible version added in version 3.3.0.
	 *
	 * @param inventory The inventory to scan and upgrade.
	 */
    public static void replaceOldBooksInInventory(Inventory inventory) {
        if (inventory == null) return;

        // --- 1. Define the Patchouli NBT key and target book ID for 1.20.1---
        // Patchouli identifies a book via a root-level String NBT: "patchouli:book" -> "<namespace>:<book_id>"
        final String PATCHOULI_BOOK_TAG = "patchouli:book";
        final String TARGET_BOOK_ID = new Identifier(MOD_ID, "hamster_tips_guide_book").toString();

        // --- 2. Iterate through all slots in the provided inventory ---
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            // --- 3. Check if the item is an OLD guide book ---
            // It's an old book if it's the guide book item but lacks the Patchouli NBT tag.
            if (!stack.isEmpty() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
                NbtCompound nbt = stack.getNbt();
                boolean hasPatchouliBookTag = nbt != null && nbt.contains(PATCHOULI_BOOK_TAG, NbtElement.STRING_TYPE);

                if (!hasPatchouliBookTag) {
                    // --- 4. Create the new, upgraded book stack ---
                    ItemStack newBookStack = new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get(), stack.getCount());
                    newBookStack.getOrCreateNbt().putString(PATCHOULI_BOOK_TAG, TARGET_BOOK_ID);

                    // --- 5. Replace the old stack with the new one ---
                    inventory.setStack(i, newBookStack);
                    LOGGER.info("Upgraded an old Hamster Tips Guide Book to the new Patchouli version.");
                }
            }
        }
    }
}
