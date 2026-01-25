package net.dawson.adorablehamsterpets.entity.custom;

import com.mojang.serialization.DataResult;
import dev.architectury.registry.menu.MenuRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.component.HamsterShoulderData;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.AI.*;
import net.dawson.adorablehamsterpets.entity.AI.navigation.HamsterNavigation;
import net.dawson.adorablehamsterpets.entity.ImplementedInventory;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderAnimationState;
import net.dawson.adorablehamsterpets.entity.control.HamsterBodyControl;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.dawson.adorablehamsterpets.mixin.accessor.LandPathNodeMakerInvoker;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.HamsterScreenHandlerFactory;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.dawson.adorablehamsterpets.util.HamsterSeatOffsets;
import net.dawson.adorablehamsterpets.util.ModNbtKeys;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.BiConsumer;

import static net.dawson.adorablehamsterpets.sound.ModSounds.HAMSTER_CELEBRATE_SOUNDS;
import static net.dawson.adorablehamsterpets.sound.ModSounds.getRandomSoundFrom;


public class HamsterEntity extends TameableEntity implements GeoEntity, ImplementedInventory {


    /* ──────────────────────────────────────────────────────────────────────────────
     *                    1. Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Constants ---
    private static final double WALK_TO_RUN_THRESHOLD_SQUARED = 0.002;
    private static final double RUN_TO_SPRINT_THRESHOLD_SQUARED = 0.008;
    public static final float FAST_YAW_CHANGE = 25.0f;
    public static final float FAST_PITCH_CHANGE = 25.0f;
    private static final int INVENTORY_SIZE = 8;
    private static final int CHEEK_POUCH_SIZE = 6;
    public static final int ACCESSORY_SLOT_INDEX = 6;
    public static final int ARMOR_SLOT_INDEX = 7;
    private static final int REFUSE_FOOD_TIMER_TICKS = 40;            // 2 seconds
    private static final int CUSTOM_LOVE_TICKS = 600;                 // 30 seconds
    private static final double THROWN_GRAVITY = -0.05;
    private static final double HAMSTER_ATTACK_BOX_EXPANSION = 0.70D;  // Expand by 0.7 blocks horizontally (vanilla is 0.83 blocks, so really this is shrinking it)
    // 1.20.1: Use UUIDs for Attribute Modifiers
    private static final UUID ARMOR_SPEED_BOOST_UUID = UUID.fromString("74ba7508-3010-449e-97c7-573531b7987e");
    private static final UUID ARMOR_KNOCKBACK_RESISTANCE_UUID = UUID.fromString("a8470a74-d2ca-4c8d-806d-6215d290680d");
    private static final int NORMAL_FALL_PITCH_DURATION = 15;
    private static final int PITCH_RESET_DURATION = 3;
    private static final int RIDER_JUMP_COOLDOWN_TICKS = 8;
    private static final double RIDER_JUMP_VELOCITY = 0.6D; // ~2 blocks

    /**
     * Required by the Tameable interface in 1.20.1.
     * It provides a view of the world the entity is in.
     *
     * @return The world this entity belongs to.
     */
    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    public enum DozingPhase {
        NONE,                  // Not in any part of the sleep sequence
        QUIESCENT_SITTING,     // Tamed, sitting by command, waiting for drowsiness timer
        DRIFTING_OFF,          // Playing the 90sec anim_hamster_drifting_off animation
        SETTLING_INTO_SLUMBER, // Playing a short anim_hamster_sit_settle_sleepX transition
        DEEP_SLEEP             // Looping one of the anim_hamster_sleep_poseX animations
    }

    public static final int CELEBRATION_PARTICLE_DURATION_TICKS = 600;    // 3 seconds
    private static final float DEFAULT_FOOTSTEP_VOLUME = 0.10F;
    private static final float GRAVEL_VOLUME_MODIFIER = 0.60F;
    private static final Set<PathNodeType> HAZARDOUS_FLOOR_TYPES = EnumSet.of(
            PathNodeType.LAVA,
            PathNodeType.DAMAGE_FIRE,
            PathNodeType.DANGER_FIRE,
            PathNodeType.POWDER_SNOW,
            PathNodeType.DAMAGE_OTHER,
            PathNodeType.DANGER_OTHER,
            PathNodeType.DAMAGE_CAUTIOUS,
            PathNodeType.WATER
    );

    private static final List<HamsterVariant> ORANGE_VARIANTS = List.of(
            HamsterVariant.ORANGE, HamsterVariant.ORANGE_OVERLAY1, HamsterVariant.ORANGE_OVERLAY2,
            HamsterVariant.ORANGE_OVERLAY3, HamsterVariant.ORANGE_OVERLAY4, HamsterVariant.ORANGE_OVERLAY5,
            HamsterVariant.ORANGE_OVERLAY6, HamsterVariant.ORANGE_OVERLAY7, HamsterVariant.ORANGE_OVERLAY8
    );
    private static final List<HamsterVariant> BLUE_VARIANTS = List.of(
            HamsterVariant.BLUE, HamsterVariant.BLUE_OVERLAY1, HamsterVariant.BLUE_OVERLAY2,
            HamsterVariant.BLUE_OVERLAY3, HamsterVariant.BLUE_OVERLAY4, HamsterVariant.BLUE_OVERLAY5,
            HamsterVariant.BLUE_OVERLAY6, HamsterVariant.BLUE_OVERLAY7, HamsterVariant.BLUE_OVERLAY8
    );
    private static final List<HamsterVariant> CHOCOLATE_VARIANTS = List.of(
            HamsterVariant.CHOCOLATE, HamsterVariant.CHOCOLATE_OVERLAY1, HamsterVariant.CHOCOLATE_OVERLAY2,
            HamsterVariant.CHOCOLATE_OVERLAY3, HamsterVariant.CHOCOLATE_OVERLAY4, HamsterVariant.CHOCOLATE_OVERLAY5,
            HamsterVariant.CHOCOLATE_OVERLAY6, HamsterVariant.CHOCOLATE_OVERLAY7, HamsterVariant.CHOCOLATE_OVERLAY8
    );
    private static final List<HamsterVariant> CREAM_VARIANTS = List.of(
            HamsterVariant.CREAM, HamsterVariant.CREAM_OVERLAY1, HamsterVariant.CREAM_OVERLAY2,
            HamsterVariant.CREAM_OVERLAY3, HamsterVariant.CREAM_OVERLAY4, HamsterVariant.CREAM_OVERLAY5,
            HamsterVariant.CREAM_OVERLAY6, HamsterVariant.CREAM_OVERLAY7, HamsterVariant.CREAM_OVERLAY8
    );
    private static final List<HamsterVariant> DARK_GRAY_VARIANTS = List.of(
            HamsterVariant.DARK_GRAY, HamsterVariant.DARK_GRAY_OVERLAY1, HamsterVariant.DARK_GRAY_OVERLAY2,
            HamsterVariant.DARK_GRAY_OVERLAY3, HamsterVariant.DARK_GRAY_OVERLAY4, HamsterVariant.DARK_GRAY_OVERLAY5,
            HamsterVariant.DARK_GRAY_OVERLAY6, HamsterVariant.DARK_GRAY_OVERLAY7, HamsterVariant.DARK_GRAY_OVERLAY8
    );
    private static final List<HamsterVariant> LAVENDER_VARIANTS = List.of(
            HamsterVariant.LAVENDER, HamsterVariant.LAVENDER_OVERLAY1, HamsterVariant.LAVENDER_OVERLAY2,
            HamsterVariant.LAVENDER_OVERLAY3, HamsterVariant.LAVENDER_OVERLAY4, HamsterVariant.LAVENDER_OVERLAY5,
            HamsterVariant.LAVENDER_OVERLAY6, HamsterVariant.LAVENDER_OVERLAY7, HamsterVariant.LAVENDER_OVERLAY8
    );
    private static final List<HamsterVariant> LIGHT_GRAY_VARIANTS = List.of(
            HamsterVariant.LIGHT_GRAY, HamsterVariant.LIGHT_GRAY_OVERLAY1, HamsterVariant.LIGHT_GRAY_OVERLAY2,
            HamsterVariant.LIGHT_GRAY_OVERLAY3, HamsterVariant.LIGHT_GRAY_OVERLAY4, HamsterVariant.LIGHT_GRAY_OVERLAY5,
            HamsterVariant.LIGHT_GRAY_OVERLAY6, HamsterVariant.LIGHT_GRAY_OVERLAY7, HamsterVariant.LIGHT_GRAY_OVERLAY8
    );

    /**
     * Determines the appropriate HamsterVariant for a given biome, using a prioritized, "hamster-centric" approach.
     * This method checks for variants from most specific/rare to most common, ensuring exclusive variants
     * like BLUE and LAVENDER are assigned correctly before falling back to more general, tag-based assignments.
     *
     * @param biomeEntry The RegistryEntry of the biome to check.
     * @param random     A Random instance for variant selection.
     * @return The chosen HamsterVariant.
     */
    private static HamsterVariant determineVariantForBiome(RegistryEntry<Biome> biomeEntry, net.minecraft.util.math.random.Random random) {
        String biomeName = biomeEntry.getKey().map(k -> k.getValue().toString()).orElse("unknown");
        AdorableHamsterPets.LOGGER.debug("[AHP Spawn Debug] determineVariantForBiome called for biome: {}", biomeName);

        HamsterVariant result;

        // --- Check from most specific/rare to most common ---
        if (canSpawnBlue(biomeEntry)) {
            // Ice Spikes has a 70% chance for Blue, 30% for White.
            result = random.nextInt(10) < 7 ? getRandomVariant(BLUE_VARIANTS, random) : HamsterVariant.WHITE;
        } else if (canSpawnLavender(biomeEntry)) {
            result = getRandomVariant(LAVENDER_VARIANTS, random);
        } else if (canSpawnWhite(biomeEntry)) {
            result = HamsterVariant.WHITE; // White has no overlays.
        } else if (canSpawnGray(biomeEntry)) {
            result = random.nextBoolean() ? getRandomVariant(LIGHT_GRAY_VARIANTS, random) : getRandomVariant(DARK_GRAY_VARIANTS, random);
        } else if (canSpawnBlack(biomeEntry)) {
            // Black hamsters should not spawn with overlays in the wild (breaks the camouflage effect)
            result = HamsterVariant.BLACK;
        } else if (canSpawnCream(biomeEntry)) {
            result = getRandomVariant(CREAM_VARIANTS, random);
        } else if (canSpawnChocolate(biomeEntry)) {
            result = getRandomVariant(CHOCOLATE_VARIANTS, random);
        } else {
            // Default Fallback: Orange is the most common, covering Plains, Savanna, etc.
            result = getRandomVariant(ORANGE_VARIANTS, random);
        }

        AdorableHamsterPets.LOGGER.debug("[AHP Spawn Debug] Determined variant for {} is {}", biomeName, result.name());
        return result;
    }

    // --- "Hamster-Centric" Helper Methods for Variant Spawning ---
    private static boolean canSpawnBlue(RegistryEntry<Biome> biomeEntry) {return ConfigDataCache.isBlueBiome(biomeEntry);}
    private static boolean canSpawnLavender(RegistryEntry<Biome> biomeEntry) {return ConfigDataCache.isLavenderBiome(biomeEntry);}
    private static boolean canSpawnWhite(RegistryEntry<Biome> biomeEntry) {return ConfigDataCache.isWhiteBiome(biomeEntry);}
    private static boolean canSpawnGray(RegistryEntry<Biome> biomeEntry) {return ConfigDataCache.isGrayBiome(biomeEntry);}
    private static boolean canSpawnBlack(RegistryEntry<Biome> biomeEntry) {return ConfigDataCache.isBlackBiome(biomeEntry);}
    private static boolean canSpawnCream(RegistryEntry<Biome> biomeEntry) {return ConfigDataCache.isCreamBiome(biomeEntry);}
    private static boolean canSpawnChocolate(RegistryEntry<Biome> biomeEntry) {return ConfigDataCache.isChocolateBiome(biomeEntry);}

    private static HamsterVariant getRandomVariant(List<HamsterVariant> variantPool, net.minecraft.util.math.random.Random random) {
        if (variantPool == null || variantPool.isEmpty()) {
            // Fallback
            return HamsterVariant.ORANGE;
        }
        return variantPool.get(random.nextInt(variantPool.size()));
    }

    /**
     * Creates the attribute container for the Hamster entity.
     * @return The attribute container builder.
     */
    public static DefaultAttributeContainer.Builder createHamsterAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, Configs.AHP.wildMaxHealth.get())
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, Configs.AHP.meleeDamage.get())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.0D);
    }

    /**
     * Creates a HamsterEntity instance from NBT data, typically from a player's shoulder.
     * This method loads the hamster's variant, health, age, inventory, effects, and custom name.
     * It does NOT set the entity's position or spawn it in the world.
     *
     * @param world The server world to create the entity in.
     * @param player The player who owns the hamster.
     * @param nbt The NbtCompound containing the hamster's data, usually from the player's DataTracker.
     * @return A fully configured, but not yet spawned, HamsterEntity instance, or null if creation fails.
     */
    @Nullable
    public static HamsterEntity createFromNbt(ServerWorld world, PlayerEntity player, NbtCompound nbt) {
        Optional<HamsterShoulderData> dataOpt = HamsterShoulderData.fromNbt(nbt);
        if (dataOpt.isEmpty()) {
            AdorableHamsterPets.LOGGER.error("Failed to deserialize HamsterShoulderData from NBT: {}", nbt);
            return null;
        }
        HamsterShoulderData data = dataOpt.get();

        AdorableHamsterPets.LOGGER.debug("[HamsterEntity] createFromNbt called for player {} with data: {}", player.getName().getString(), data);
        HamsterEntity hamster = ModEntities.HAMSTER.get().create(world);

        if (hamster != null) {
            // --- 1. Load Core Data ---
            hamster.setUuid(data.entityUuid());
            hamster.setVariant(data.variantId());
            hamster.setHealth(data.health());
            hamster.setOwnerUuid(player.getUuid());
            hamster.setTamed(true, true);
            hamster.setBreedingAge(data.breedingAge());
            hamster.throwCooldownEndTick = data.throwCooldownEndTick();
            hamster.autoEatCooldownTicks = data.autoEatCooldownTicks();
            hamster.getDataTracker().set(PINK_PETAL_TYPE, data.pinkPetalType());
            hamster.getDataTracker().set(ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
            hamster.getDataTracker().set(HAMSTER_FLAGS, data.hamsterFlags());

            // Explicitly clear the sitting flag to ensure the hamster always dismounts standing.
            hamster.setHamsterFlag(SITTING_FLAG, false);

            // --- 2. Load Custom Name ---
            data.customName().ifPresent(name -> {
                if (!name.isEmpty()) {
                    hamster.setCustomName(Text.literal(name));
                }
            });

            // --- 3. Load Inventory ---
            if (!data.inventoryNbt().isEmpty()) {
                Inventories.readNbt(data.inventoryNbt(), hamster.items);
                hamster.updateCheekTrackers();
                hamster.updateEquipmentTrackers();
            }

            // --- 4. Load Green Bean Buff Data/Status Effects ---
            HamsterShoulderData.GreenBeanBuffData buffData = data.greenBeanBuffData();
            hamster.greenBeanBuffEndTick = buffData.greenBeanBuffEndTick();
            hamster.getDataTracker().set(GREEN_BEAN_BUFF_DURATION, buffData.greenBeanBuffDuration());
            // In 1.20.1, handle NbtList directly
            NbtList effectsList = buffData.activeEffectsNbt();
            for (int i = 0; i < effectsList.size(); i++) {
                NbtCompound effectNbt = effectsList.getCompound(i);
                StatusEffectInstance effectInstance = StatusEffectInstance.fromNbt(effectNbt);
                if (effectInstance != null) {
                    hamster.addStatusEffect(effectInstance);
                }
            }

            // --- 5. Load Diamond Seeking Data ---
            HamsterShoulderData.SeekingBehaviorData seekingData = data.seekingBehaviorData();
            hamster.isPrimedToSeekDiamonds = seekingData.isPrimedToSeekDiamonds();
            hamster.foundOreCooldownEndTick = seekingData.foundOreCooldownEndTick();
            hamster.currentOreTarget = seekingData.currentOreTarget().orElse(null);

            // --- 6. Load Wander Mode/Bed Data ---
            HamsterShoulderData.WanderModeData wanderData = data.wanderModeData();
            hamster.linkedBedPos = wanderData.linkedBedPos();
            hamster.bypassNextSleepDelay = wanderData.bypassNextSleepDelay();

            // --- 7. Reset Transient States ---
            hamster.isAutoEating = false;
            hamster.autoEatProgressTicks = 0;

            // Explicitly reset transient action flags to prevent stuck states.
            hamster.setHamsterFlag(CLEANING_FLAG, false);
            hamster.setDozingPhase(DozingPhase.NONE);
        }
        return hamster;
    }

    /**
     * Spawns a HamsterEntity from NBT data near the player, handling position and spawning.
     * This is typically called when a player dismounts a hamster or respawns. It can accept a
     * pre-configured hamster instance (for throws) or create one from NBT (for dismounts/respawns).
     *
     * @param world The server world to spawn the entity in.
     * @param player The player who is dismounting the hamster.
     * @param nbt The NbtCompound containing the hamster's data.
     * @param wasDiamondAlertActive True if the hamster should be primed for diamond seeking.
     * @param preconfiguredHamster An optional, pre-configured HamsterEntity instance. If provided, this instance is used directly.
     */
    public static void spawnFromNbt(ServerWorld world, PlayerEntity player, NbtCompound nbt, boolean wasDiamondAlertActive, @Nullable HamsterEntity preconfiguredHamster) {
        // --- 1. Use Pre-configured Hamster or Create from NBT ---
        HamsterEntity hamster = preconfiguredHamster != null ? preconfiguredHamster : createFromNbt(world, player, nbt);
        if (hamster == null) {
            return;
        }

        // --- Set the suffocation grace period ---
        hamster.suffocationGracePeriod = 200; // 10 seconds

        // --- 2. Prime for Diamond Seeking (if applicable) ---
        if (wasDiamondAlertActive && Configs.AHP.enableIndependentDiamondSeeking) {
            hamster.isPrimedToSeekDiamonds = true;
            AdorableHamsterPets.LOGGER.debug("[HamsterEntity {}] Primed for diamond seeking upon dismount.", hamster.getId());
        }

        // --- 3. Set Position and Spawn ---
        if (hamster.isThrown()) {
            // If thrown, its position and velocity were already set. Just spawn it.
            world.spawnEntity(hamster);
            AdorableHamsterPets.LOGGER.debug("[HamsterEntity] Spawned THROWN Hamster ID {} from NBT data near Player {}.", hamster.getId(), player.getName().getString());
        } else {
            // If not thrown (standard dismount), find a safe landing spot.
            BlockPos initialSearchPos;
            BlockPos ultimateFallbackPos = player.getBlockPos(); // Player's feet as the last resort

        // Raycast to find where the player is looking
        HitResult hitResult = player.raycast(4.5, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            initialSearchPos = ((net.minecraft.util.hit.BlockHitResult) hitResult).getBlockPos();
        } else {
            initialSearchPos = ultimateFallbackPos; // Default to player's position if not looking at a block
        }

        // Use the safe spawning algorithm
        Optional<BlockPos> safePosOpt = hamster.findSafeSpawnPosition(initialSearchPos, world, 5);

        // --- 4. Set Position and Spawn ---
        safePosOpt.ifPresentOrElse(
                safePos -> {
                    // Spawn at the center of the safe block
                    hamster.refreshPositionAndAngles(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, player.getYaw(), player.getPitch());
                    AdorableHamsterPets.LOGGER.debug("[HamsterDismount] Found safe spawn at {} for player {}.", safePos, player.getName().getString());
                },
                () -> {
                    // Fallback if no safe spot is found
                    AdorableHamsterPets.LOGGER.warn("[HamsterDismount] Could not find a safe spawn position for player {}. Spawning at player's feet as a fallback.", player.getName().getString());
                    hamster.refreshPositionAndAngles(ultimateFallbackPos.getX() + 0.5, ultimateFallbackPos.getY(), ultimateFallbackPos.getZ() + 0.5, player.getYaw(), player.getPitch());
                }
        );

        world.spawnEntityAndPassengers(hamster);
        AdorableHamsterPets.LOGGER.debug("[HamsterEntity] Spawned Hamster ID {} from NBT data near Player {}.", hamster.getId(), player.getName().getString());
        }
    }

    /**
     * Attempts to throw the hamster from the player's shoulder.
     * This server-side logic is triggered when the throw packet is received. It now delegates
     * the core logic to the PlayerEntityMixin, which determines which hamster to dismount/throw
     * based on the configured LIFO/FIFO order.
     *
     * @param player The player attempting the throw.
     */
    public static void tryThrowFromShoulder(ServerPlayerEntity player) {
        // --- 1. Initial Setup & Config Check ---
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;
        final AhpConfig config = AdorableHamsterPets.CONFIG;

        if (!config.enableHamsterThrowing) {
            player.sendMessage(Text.translatable("message.adorablehamsterpets.throwing_disabled"), true);
            return;
        }

        if (!playerAccessor.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.warn("[HamsterThrow] Player {} tried to throw, but has no shoulder hamster.", player.getName().getString());
            return;
        }

        // --- 2. Delegate Dismount/Throw Logic ---
        playerAccessor.adorablehamsterpets$dismountShoulderHamster(true);
    }

    // --- Bitmask Flags for DataTracker ---
    public static final int SLEEPING_FLAG = 1 << 0;
    public static final int SITTING_FLAG = 1 << 1;
    public static final int BEGGING_FLAG = 1 << 2;
    public static final int IN_LOVE_FLAG = 1 << 3;
    public static final int REFUSING_FOOD_FLAG = 1 << 4;
    public static final int THROWN_FLAG = 1 << 5;
    public static final int LEFT_CHEEK_FULL_FLAG = 1 << 6;
    public static final int RIGHT_CHEEK_FULL_FLAG = 1 << 7;
    public static final int KNOCKED_OUT_FLAG = 1 << 8;
    public static final int CHEEK_POUCH_UNLOCKED_FLAG = 1 << 9;
    public static final int CONSIDERING_AUTO_EAT_FLAG = 1 << 10;
    public static final int SULKING_FLAG = 1 << 11;
    public static final int CELEBRATING_DIAMOND_FLAG = 1 << 12;
    public static final int CLEANING_FLAG = 1 << 13;
    public static final int HOLDING_INTEREST_ITEM_FLAG = 1 << 14;
    public static final int TAUNTING_WITH_ITEM_FLAG = 1 << 15;
    public static final int PRESENTING_ITEM_FLAG = 1 << 20;
    public static final int CELEBRATING_RETRIEVAL_FLAG = 1 << 16;
    public static final int IS_SHOULDER_PET_FLAG = 1 << 17;
    public static final int IS_WANDER_MODE_ACTIVE_FLAG = 1 << 18;
    public static final int ON_THE_WAY_TO_BED_FLAG = 1 << 19;

    // --- Data Trackers ---
    private static final TrackedData<Integer> HAMSTER_FLAGS = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> ANIMATION_PERSONALITY_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> PINK_PETAL_TYPE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> DOZING_PHASE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<String> CURRENT_DEEP_SLEEP_ANIM_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> ACTIVE_CUSTOM_GOAL_NAME_DEBUG = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);
    public static final TrackedData<Integer> ITEM_INTEREST_TIMER = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<ItemStack> INTEREST_ITEM_STACK = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    public static final TrackedData<Long> GREEN_BEAN_BUFF_DURATION = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.LONG);
    public static final TrackedData<Integer> CURRENT_LOOK_UP_ANIM_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> SHOULDER_ANIMATION_STATE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> TRACKED_ACCESSORY_STACK = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<ItemStack> TRACKED_ARMOR_STACK = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

    // --- Animation Constants ---
    private static final RawAnimation CRASH_ANIM = RawAnimation.begin().thenPlay("anim_hamster_crash");
    private static final RawAnimation KNOCKED_OUT_ANIM = RawAnimation.begin().thenPlay("anim_hamster_ko");
    private static final RawAnimation WAKE_UP_FROM_KO_ANIM = RawAnimation.begin().thenPlay("anim_hamster_wakeup_from_ko");
    private static final RawAnimation FLYING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_flying");
    private static final RawAnimation STANDING_HEADSHAKE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_standing_headshake");
    private static final RawAnimation SITTING_HEADSHAKE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sitting_headshake");
    private static final RawAnimation MOVING_HEADSHAKE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_moving_headshake");
    private static final RawAnimation SLEEP_POSE1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sleep_pose1");
    private static final RawAnimation SLEEP_POSE2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sleep_pose2");
    private static final RawAnimation SLEEP_POSE3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sleep_pose3");
    private static final RawAnimation SIT_SETTLE_SLEEP1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sit_settle_sleep1");
    private static final RawAnimation SIT_SETTLE_SLEEP2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sit_settle_sleep2");
    private static final RawAnimation SIT_SETTLE_SLEEP3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sit_settle_sleep3");
    private static final RawAnimation STAND_SETTLE_SLEEP1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_stand_settle_sleep1");
    private static final RawAnimation STAND_SETTLE_SLEEP2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_stand_settle_sleep2");
    private static final RawAnimation STAND_SETTLE_SLEEP3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_stand_settle_sleep3");
    private static final RawAnimation SIT1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sit1");
    private static final RawAnimation SIT2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sit2");
    private static final RawAnimation SIT3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sit3");
    private static final RawAnimation STANDUP1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_standup1");
    private static final RawAnimation STANDUP2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_standup2");
    private static final RawAnimation STANDUP3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_standup3");
    private static final RawAnimation WAKE_UP_1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_wakeup1");
    private static final RawAnimation WAKE_UP_2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_wakeup2");
    private static final RawAnimation WAKE_UP_3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_wakeup3");
    private static final RawAnimation SITTING_POSE1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sitting_pose1");
    private static final RawAnimation SITTING_POSE2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sitting_pose2");
    private static final RawAnimation SITTING_POSE3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sitting_pose3");
    private static final RawAnimation DRIFTING_OFF_POSE1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_drifting_off_pose1");
    private static final RawAnimation DRIFTING_OFF_POSE2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_drifting_off_pose2");
    private static final RawAnimation DRIFTING_OFF_POSE3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_drifting_off_pose3");
    private static final RawAnimation CLEANING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_cleaning");
    private static final RawAnimation RUNNING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_running");
    private static final RawAnimation WALKING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_walking");
    private static final RawAnimation SPRINTING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sprinting");
    private static final RawAnimation BEGGING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_begging");
    private static final RawAnimation IDLE1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_idle1");
    private static final RawAnimation IDLE2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_idle2");
    private static final RawAnimation IDLE_LOOKING_UP1_ANIM = RawAnimation.begin().thenPlay("anim_hamster_idle_looking_up1");
    private static final RawAnimation IDLE_LOOKING_UP2_ANIM = RawAnimation.begin().thenPlay("anim_hamster_idle_looking_up2");
    private static final RawAnimation IDLE_LOOKING_UP3_ANIM = RawAnimation.begin().thenPlay("anim_hamster_idle_looking_up3");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("anim_hamster_attack");
    private static final RawAnimation SULK_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sulk");
    private static final RawAnimation SULKING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_sulking");
    private static final RawAnimation SEEKING_ORE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_seeking_ore");
    private static final RawAnimation WANTS_TO_SEEK_ORE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_wants_to_seek_ore");
    private static final RawAnimation POUNCE_ON_ITEM_ANIM = RawAnimation.begin().thenPlay("anim_hamster_pounce_on_item");
    private static final RawAnimation TAUNT_WITH_ITEM_ANIM = RawAnimation.begin().thenPlay("anim_hamster_taunt_with_item");
    private static final RawAnimation PRESENTING_ITEM_ANIM = RawAnimation.begin().thenPlay("anim_hamster_presenting_item");
    private static final RawAnimation CELEBRATE_CHASE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_celebrate_chase");
    private static final RawAnimation LAYING_DOWN_HEAD_ANIM = RawAnimation.begin().thenPlay("anim_hamster_shoulder_laying_down_head");
    private static final RawAnimation LAYING_DOWN_RIGHT_SHOULDER_ANIM = RawAnimation.begin().thenPlay("anim_hamster_shoulder_laying_down_right_shoulder");
    private static final RawAnimation LAYING_DOWN_LEFT_SHOULDER_ANIM = RawAnimation.begin().thenPlay("anim_hamster_shoulder_laying_down_left_shoulder");


    /* ──────────────────────────────────────────────────────────────────────────────
     *                                  2. Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Unique Instance Fields ---
    @Unique public int interactionCooldown = 0;
    @Unique public int throwTicks = 0;
    @Unique public int wakingUpTicks = 0;
    @Unique private int ejectionCheckCooldown = 20;
    @Unique private int preAutoEatDelayTicks = 0;
    @Unique private int quiescentSitDurationTimer = 0;
    @Unique private int driftingOffTimer = 0;
    @Unique private int settleSleepAnimationCooldown = 0;
    @Unique private String activeCustomGoalDebugName = "None";
    @Unique public boolean isPrimedToSeekDiamonds = false;
    @Unique public long foundOreCooldownEndTick = 0L;
    @Unique public BlockPos currentOreTarget = null;
    @Unique private int celebrationParticleTicks = 0;
    @Unique private int diamondCelebrationSoundTicks = 0;
    @Unique private int sulkOrchestraHitDelayTicks = 0;
    @Unique private int sulkFailParticleTicks = 0;
    @Unique private int sulkEntityEffectTicks = 0;
    @Unique private int sulkShockedSoundDelayTicks = 0;
    @Unique private int diamondSparkleSoundDelayTicks = 0;
    @Unique public transient String particleEffectId = null;
    @Unique public transient String soundEffectId = null;
    @Unique public long interestCooldownEndTick = 0L;
    @Unique private int celebrationRetrievalTicks = 0;
    @Unique private boolean zoomiesIsClockwise = false;
    @Unique private double lastZoomiesAngle = 0.0;
    @Unique private int zoomiesRadiusModifier = 0;
    @Unique public transient float renderedSnowYOffset = 0.0f;
    @Unique public transient ShoulderLocation shoulderLocation = ShoulderLocation.RIGHT_SHOULDER;
    @Unique public int suffocationGracePeriod = 0;
    @Unique public transient float dynamicScaleY = 1.0f;
    @Unique private Optional<GlobalPos> linkedBedPos = Optional.empty();
    @Unique private int goToBedCooldown = 0;
    @Unique private int lureToBedTimer = 0;
    @Unique public int goToBedDelayTicks = 0;
    @Unique private int wakeUpFromBedDelay = 0;
    @Unique public int bedLeafParticleTicks = 0;
    @Unique private boolean bypassNextSleepDelay = false;
    @Unique private int napInBedDurationTimer = 0;
    @Unique private int thumpSoundDelayTicks = 0;
    @Unique private float thumpSoundVolume = 0.2f;
    @Unique public int pathingFailures = 0;
    @Nullable @Unique public BlockPos lastFailedTarget = null;
    @Unique private boolean hasPlayedIncomingSound = false;
    @Unique private boolean isLoadingNbt = false; // Guard to prevent sounds during load
    @Unique private boolean isSilentInventoryUpdate = false;
    private boolean armorAbsorbedDamage = false;
    private boolean performDeferredArmorUpdate = false;
    @Unique public float clientFallPitchProgress = 0.0f;
    @Unique public float prevClientFallPitchProgress = 0.0f;
    @Unique private int riderJumpCooldown = 0;
    @Unique private boolean riderJumpHeld = false;
    @Unique private boolean riderJumpQueued = false;
    @Unique private boolean riderSprintHeld = false;

    // --- Inventory ---
    private final DefaultedList<ItemStack> items = ImplementedInventory.create(INVENTORY_SIZE);

    // --- Armor Tracking ---
    private ItemStack lastArmorStack = ItemStack.EMPTY;

    // --- Animation ---
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // --- State Variables ---
    private int refuseTimer = 0;
    private ItemStack lastFoodItem = ItemStack.EMPTY;
    public int customLoveTimer;
    private int tamingCooldown = 0;
    public long throwCooldownEndTick = 0L;
    private long greenBeanBuffEndTick = 0L;

    // --- Auto-Eating State/Cooldown Fields ---
    private boolean isAutoEating = false; // Flag for potential animation hook
    private int autoEatProgressTicks = 0; // Ticks remaining for the current eating action
    private int autoEatCooldownTicks = 0; // Ticks remaining before it can start eating again

    public int cleaningTimer = 0;
    private int cleaningCooldownTimer = 0;



    /* ──────────────────────────────────────────────────────────────────────────────
     *                             3. Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 3;

        // --- Set pathfinding penalties for all relevant goals ---
        this.setPathfindingPenalty(PathNodeType.WATER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 16.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 16.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 16.0F);
    }


    /* ──────────────────────────────────────────────────────────────────────────────
     *                             4. Public Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Data Tracker Getters/Setters ---
    public int getVariant() { return this.dataTracker.get(VARIANT); }
    public void setVariant(int variantId) { this.dataTracker.set(VARIANT, variantId); }
    public boolean isSleeping() { return getHamsterFlag(SLEEPING_FLAG); }
    public void setSleeping(boolean sleeping) { setHamsterFlag(SLEEPING_FLAG, sleeping); }
    @Override
    public boolean isSitting() {
        return getHamsterFlag(SITTING_FLAG)
                || getHamsterFlag(SLEEPING_FLAG)
                || getHamsterFlag(KNOCKED_OUT_FLAG)
                || getHamsterFlag(SULKING_FLAG);
    }
    public boolean isCleaning() {return getHamsterFlag(CLEANING_FLAG);}
    public boolean isBegging() { return getHamsterFlag(BEGGING_FLAG); }
    public void setBegging(boolean value) { setHamsterFlag(BEGGING_FLAG, value); }
    public boolean isInLove() { return getHamsterFlag(IN_LOVE_FLAG); }
    public void setInLove(boolean value) { setHamsterFlag(IN_LOVE_FLAG, value); }
    public boolean isRefusingFood() { return getHamsterFlag(REFUSING_FOOD_FLAG); }
    public void setRefusingFood(boolean value) { setHamsterFlag(REFUSING_FOOD_FLAG, value); }
    public boolean isThrown() { return getHamsterFlag(THROWN_FLAG); }
    public void setThrown(boolean thrown) {
        setHamsterFlag(THROWN_FLAG, thrown);
        if (thrown) {
            this.hasPlayedIncomingSound = false; // Reset sound flag on new throw
        }
    }
    public boolean isLeftCheekFull() { return getHamsterFlag(LEFT_CHEEK_FULL_FLAG); }
    public void setLeftCheekFull(boolean full) { setHamsterFlag(LEFT_CHEEK_FULL_FLAG, full); }
    public boolean isRightCheekFull() { return getHamsterFlag(RIGHT_CHEEK_FULL_FLAG); }
    public void setRightCheekFull(boolean full) { setHamsterFlag(RIGHT_CHEEK_FULL_FLAG, full); }
    public boolean isKnockedOut() { return getHamsterFlag(KNOCKED_OUT_FLAG); }
    public void setKnockedOut(boolean knocked_out) { setHamsterFlag(KNOCKED_OUT_FLAG, knocked_out); }
    public String getCurrentDeepSleepAnimationIdFromTracker() {return this.dataTracker.get(CURRENT_DEEP_SLEEP_ANIM_ID);}
    public boolean isAutoEating() {return this.isAutoEating;}
    public boolean isConsideringAutoEat() {return getHamsterFlag(CONSIDERING_AUTO_EAT_FLAG);}
    public DozingPhase getDozingPhase() {return DozingPhase.values()[this.dataTracker.get(DOZING_PHASE)];}
    public void setDozingPhase(DozingPhase phase) {this.dataTracker.set(DOZING_PHASE, phase.ordinal());}
    public void setActiveCustomGoalDebugName(String name) {this.dataTracker.set(ACTIVE_CUSTOM_GOAL_NAME_DEBUG, name);}
    public String getActiveCustomGoalDebugName() {String goalName = this.dataTracker.get(ACTIVE_CUSTOM_GOAL_NAME_DEBUG);return goalName;}
    public boolean isSulking() {return getHamsterFlag(SULKING_FLAG);}
    public boolean isCelebratingDiamond() {return getHamsterFlag(CELEBRATING_DIAMOND_FLAG);}
    public void setCelebratingDiamond(boolean celebrating) {
        setHamsterFlag(CELEBRATING_DIAMOND_FLAG, celebrating);
        if (celebrating) {
            this.setBegging(false); // Ensure not also in normal begging state
            if (!this.getWorld().isClient()) { // Only initialize timer on server
                this.celebrationParticleTicks = HamsterEntity.CELEBRATION_PARTICLE_DURATION_TICKS;
                this.diamondSparkleSoundDelayTicks = 10; // 10-tick delay for sparkle sound
            }
        } else {
            // If stopping celebration, ensure all associated timers are also stopped/reset
            this.celebrationParticleTicks = 0;
            this.diamondSparkleSoundDelayTicks = 0;
            this.diamondCelebrationSoundTicks = 0;
        }
    }
    public void setSulking(boolean sulking) {
        setHamsterFlag(SULKING_FLAG, sulking);
        if (sulking) {
            if (!this.getWorld().isClient()) {
                this.sulkOrchestraHitDelayTicks = 10; // 10-tick delay for orchestra hit
                this.sulkShockedSoundDelayTicks = 44; // 2.2 seconds * 20 ticks/second = 44 ticks
                this.sulkFailParticleTicks = 600;     // Duration for fail particles
                this.sulkEntityEffectTicks = 600;     // Duration for entity effect particles
            }
        } else {
            // If stopping sulking, ensure all associated timers are also stopped/reset
            this.sulkOrchestraHitDelayTicks = 0;
            this.sulkFailParticleTicks = 0;
            this.sulkEntityEffectTicks = 0;
        }
    }
    public boolean isHoldingInterestItem() {return getHamsterFlag(HOLDING_INTEREST_ITEM_FLAG);}
    public void setHoldingInterestItem(boolean holding) {setHamsterFlag(HOLDING_INTEREST_ITEM_FLAG, holding);}
    public int getItemInterestTimer() {return this.dataTracker.get(ITEM_INTEREST_TIMER);}
    public void setItemInterestTimer(int ticks) {this.dataTracker.set(ITEM_INTEREST_TIMER, ticks);}
    public boolean isTauntingWithItem() {return getHamsterFlag(TAUNTING_WITH_ITEM_FLAG);}
    public void setTauntingWithItem(boolean taunting) {setHamsterFlag(TAUNTING_WITH_ITEM_FLAG, taunting);}
    public boolean isPresentingItem() { return getHamsterFlag(PRESENTING_ITEM_FLAG); }
    public void setPresentingItem(boolean presenting) { setHamsterFlag(PRESENTING_ITEM_FLAG, presenting); }
    public ItemStack getInterestItemStack() { return this.dataTracker.get(INTEREST_ITEM_STACK); }
    public void setInterestItemStack(ItemStack stack) { this.dataTracker.set(INTEREST_ITEM_STACK, stack); }
    public boolean isCelebratingRetrieval() { return getHamsterFlag(CELEBRATING_RETRIEVAL_FLAG); }
    public void setCelebratingRetrieval(boolean celebrating) { setHamsterFlag(CELEBRATING_RETRIEVAL_FLAG, celebrating); }
    public boolean hasGreenBeanBuff() {return this.getDataTracker().get(GREEN_BEAN_BUFF_DURATION) > this.getWorld().getTime();}
    public boolean getZoomiesIsClockwise() { return this.zoomiesIsClockwise; }
    public double getLastZoomiesAngle() { return this.lastZoomiesAngle; }
    public void setLastZoomiesAngle(double angle) { this.lastZoomiesAngle = angle; }
    public int getZoomiesRadiusModifier() { return this.zoomiesRadiusModifier; }
    public boolean isShoulderPet() { return getHamsterFlag(IS_SHOULDER_PET_FLAG); }
    public void setShoulderPet(boolean isShoulderPet) { setHamsterFlag(IS_SHOULDER_PET_FLAG, isShoulderPet); }
    public boolean isWanderModeActive() { return getHamsterFlag(IS_WANDER_MODE_ACTIVE_FLAG); }
    public void setWanderModeActive(boolean active) { setHamsterFlag(IS_WANDER_MODE_ACTIVE_FLAG, active); }
    public Optional<GlobalPos> getLinkedBedPos() { return this.linkedBedPos; }
    public void setLinkedBedPos(Optional<GlobalPos> pos) { this.linkedBedPos = pos; }
    public int getGoToBedCooldown() { return this.goToBedCooldown; }
    public void wakeUpFromBed(boolean isManualWakeUp) {
        // Wakes the hamster up from its bed, setting the bed block to unoccupied
        // and applying a cooldown to prevent it from immediately going back to sleep.
        if (!this.isSleeping()) return;

        // Trigger animation and sound
        triggerWakeUpFromSleepAnimation(isManualWakeUp); // Pass in the context

        this.setSleeping(false);
        this.setInSittingPose(false); // Explicitly re-enable AI movement
        // Apply a configurable cooldown if woken up by player interaction,
        // preventing the hamster from immediately getting back in bed.
        if (isManualWakeUp) {
            this.goToBedCooldown = Configs.AHP.bedWakeUpCooldown.get();
            this.setBypassNextSleepDelay(true);
        }

        // Set bed block to unoccupied and find a safe spot to move to
        this.getLinkedBedPos().ifPresent(globalPos -> {
            if (this.getWorld().getRegistryKey() == globalPos.getDimension()) {
                BlockPos bedPos = globalPos.getPos();
                BlockState bedState = this.getWorld().getBlockState(bedPos);

                // Spawn Wake-Up Particles with wood type
                if (!this.getWorld().isClient()) {
                    ((ServerWorld)this.getWorld()).spawnParticles(ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                            bedPos.getX() + 0.5, bedPos.getY() + 0.3, bedPos.getZ() + 0.5,
                            50, 0.2, 0.5, 0.2, 0.0);
                }

                // Play Leaf Rustling Sound
                SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, this.random);
                if (rustleSound != null) {
                    this.getWorld().playSound(null, this.getBlockPos(), rustleSound, SoundCategory.NEUTRAL, 0.2f, 1.8f);
                }

                if (bedState.isOf(ModBlocks.HAMSTER_BED.get()) && bedState.get(HamsterBedBlock.OCCUPIED)) {
                    this.getWorld().setBlockState(bedPos, bedState.with(HamsterBedBlock.OCCUPIED, false), Block.NOTIFY_ALL);
                }

                // Trigger bed animation
                BlockEntity be = this.getWorld().getBlockEntity(bedPos);
                if (be instanceof GeoBlockEntity geoBlockEntity) {
                    geoBlockEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_unoccupied");
                }

                // Find Safe Egress Position and Pathfind
                for (BlockPos checkPos : BlockPos.iterate(bedPos.add(-1, 0, -1), bedPos.add(1, 0, 1))) {
                    // Don't move to the bed block itself
                    if (checkPos.equals(bedPos)) continue;

                    if (isSafeSpawnLocation(checkPos, this.getWorld())) {
                        this.getNavigation().startMovingTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5, 1.2D);
                        return; // Exit after finding the first safe spot
                    }
                }
            }
        });
    }
    public int getGoToBedDelayTicks() { return this.goToBedDelayTicks; }
    public void setGoToBedDelayTicks(int ticks) { this.goToBedDelayTicks = ticks; }
    public int getLureToBedTimer() { return this.lureToBedTimer; }
    public void setLureToBedTimer(int ticks) { this.lureToBedTimer = ticks; }
    public void lureToBed() { this.lureToBedTimer = 20; }
    public boolean isOnTheWayToBed() { return getHamsterFlag(ON_THE_WAY_TO_BED_FLAG); }
    public void setOnTheWayToBed(boolean onTheWay) { setHamsterFlag(ON_THE_WAY_TO_BED_FLAG, onTheWay); }
    public boolean shouldBypassNextSleepDelay() { return this.bypassNextSleepDelay; }
    public void setBypassNextSleepDelay(boolean bypass) { this.bypassNextSleepDelay = bypass; }
    public void startNapTimer() {
        // Starts the nap timer for the Circadian Chaos feature.
        // This is called by the AI goal when the hamster successfully enters its bed.
        if (Configs.AHP.circadianChaos.get()) {
            int min = Configs.AHP.minNapInBedIntervalSeconds.get() * 20;
            int max = Configs.AHP.maxNapInBedIntervalSeconds.get() * 20;
            this.napInBedDurationTimer = this.random.nextBetween(min, max);
        }
    }
    public void triggerSettleEffects(float swishVolume, int thumpDelay, float thumpVolume) {
        // Triggers a two-part settle sound effect ("swish" then "thump") with dynamic volumes.
        if (!this.getWorld().isClient()) {
            this.getWorld().playSound(null, this.getBlockPos(), ModSounds.HAMSTER_SWISH.get(), SoundCategory.NEUTRAL, swishVolume, 1.0f + this.random.nextFloat() * 0.5f);
        }
        this.thumpSoundDelayTicks = thumpDelay;
        this.thumpSoundVolume = thumpVolume;
    }
    public void triggerWakeUpFromSleepAnimation(boolean isManualWakeUp) {
        // Triggers the appropriate wake-up animation and sound based on the last used sleep pose.
        // This is the centralized method for all "wake from sleep" scenarios.
        if (this.getWorld().isClient()) return;

        String currentSleepAnim = this.getDataTracker().get(CURRENT_DEEP_SLEEP_ANIM_ID);
        String animToTrigger;

        switch (currentSleepAnim) {
            case "anim_hamster_sleep_pose2" -> animToTrigger = "wakeup2";
            case "anim_hamster_sleep_pose3" -> animToTrigger = "wakeup3";
            default -> animToTrigger = "wakeup1";
        }

        this.triggerAnimOnServer("mainController", animToTrigger);

        // --- Conditional Sound Logic ---
        // Swish sound plays for both manual and natural wake-ups.
        this.getWorld().playSound(null, this.getBlockPos(), ModSounds.HAMSTER_SWISH.get(), SoundCategory.NEUTRAL, 0.1f, 1.0f + this.random.nextFloat() * 0.5f);

        if (isManualWakeUp) {
            // Affection sound only for player-initiated manual wake-ups.
            SoundEvent affectionSound = getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, this.random);
            if (affectionSound != null) {
                this.getWorld().playSound(null, this.getBlockPos(), affectionSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        }
    }
    public boolean isPathThroughUnlinkedBed(@Nullable Path path) {
        // Checks if a given path traverses an unlinked Hamster Bed.
        // This allows AI goals to validate a path before committing the hamster to follow it.
        if (path == null) return false;

        // Get the position of this hamster's linked bed, if it has one.
        BlockPos linkedBed = this.getLinkedBedPos()
                .map(GlobalPos::getPos)
                .orElse(null);

        for (int i = 0; i < path.getLength(); ++i) {
            PathNode node = path.getNode(i);
            // Use direct method to get the BlockPos from the node.
            BlockPos pos = node.getBlockPos();
            if (isUnlinkedBed(pos, linkedBed) || isUnlinkedBed(pos.down(), linkedBed)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[AHP Nav Debug] Path intersects unlinked bed at {}, linkedBed={} for hamster {}",
                        pos, linkedBed, this.getUuid()
                );
                return true;
            }
        }
        return false;
    }
    public boolean isUnlinkedBed(BlockPos pos, BlockPos linkedBed) {
        if (this.getWorld().getBlockState(pos).getBlock() instanceof HamsterBedBlock) {
            // If the node is a bed, check if it's NOT our linked bed.
            // This is true if we have no linked bed, or if the position doesn't match.
            return linkedBed == null || !pos.equals(linkedBed);
        }
        return false; // Path is valid.
    }
    public void updateNavigation() {
        // Dynamically swaps the navigation component based on the current config setting.
        // This ensures that changes to the 'avoidUnlinkedBeds' config are applied to
        // existing hamsters without requiring a world reload.
        if (this.getWorld().isClient()) return;

        boolean useCustomNav = Configs.AHP.avoidUnlinkedBeds;
        boolean isCurrentlyCustom = this.navigation instanceof HamsterNavigation;

        // Only swap if the current navigation type is incorrect
        if (useCustomNav && !isCurrentlyCustom) {
            this.navigation = createNavigation(this.getWorld());
        } else if (!useCustomNav && isCurrentlyCustom) {
            this.navigation = createNavigation(this.getWorld());
        }
    }
    @SuppressWarnings("UnusedReturnValue")
    public boolean tryShoulderMount(PlayerEntity player, ItemStack stack) {
        // Attempts to mount the hamster to the player's shoulder. True if successful.
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

        // --- Mount Priority Logic ---
        ShoulderLocation availableSlot = null;
        MountPriority priority = Configs.AHP.mountPriority.get();

        if (priority == MountPriority.HEAD_FIRST) {
            // Check Head -> Right -> Left
            if (playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty()) {
                availableSlot = ShoulderLocation.HEAD;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.RIGHT_SHOULDER;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.LEFT_SHOULDER;
            }
        } else {
            // Default: Shoulders -> Head
            if (playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.RIGHT_SHOULDER;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                availableSlot = ShoulderLocation.LEFT_SHOULDER;
            } else if (playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty()) {
                availableSlot = ShoulderLocation.HEAD;
            }
        }

        if (availableSlot != null) {
            // Disable Wander Mode Before Saving
            this.setWanderModeActive(false);

            // Save, Set, and Update Queue
            HamsterShoulderData data = this.saveToShoulderData();
            playerAccessor.setShoulderHamster(availableSlot, data.toNbt());
            playerAccessor.adorablehamsterpets$getMountOrderQueue().addLast(availableSlot);

            BlockPos hamsterPosForMountSound = this.getBlockPos();
            this.discard(); // Remove hamster from world

            // Trigger Generic Events and Play Mount Sound
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ModCriteria.HAMSTER_ON_SHOULDER.trigger(serverPlayer);
            }
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_mount_success"), true);

            SoundEvent mountSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SHOULDER_MOUNT_SOUNDS, this.random);
            if (mountSound != null) {
                this.getWorld().playSound(null, player.getBlockPos(), mountSound, SoundCategory.PLAYERS, 1.0f, this.getSoundPitch());
            }

            // Item-Specific Effects and Consumption (if stack is valid lure)
            if (ConfigDataCache.isLureItem(stack)) {
                SoundEvent mountLureSound = ModSounds.getDynamicItemSound(stack);
                this.getWorld().playSound(null, hamsterPosForMountSound, mountLureSound, SoundCategory.PLAYERS, 1.0f, 1.0f);

                // Use stack.copy() to prevent "Failed to encode packet" crashes if decrement empties the stack
                ((ServerWorld)this.getWorld()).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                        hamsterPosForMountSound.getX() + 0.5, hamsterPosForMountSound.getY() + 0.5, hamsterPosForMountSound.getZ() + 0.5,
                        8, 0.25D, 0.25D, 0.25D, 0.05);

                if (!player.getAbilities().creativeMode && Configs.AHP.consumeLureItem) {
                    stack.decrement(1);
                }
            }
            return true;
        } else {
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_occupied"), true);
            return false;
        }
    }
    @Override
    public boolean damage(DamageSource source, float amount) {
        // --- 1. Suffocation Rescue Check ---
        // If the damage is suffocation AND the grace period is active, cancel the damage.
        if (source.isOf(DamageTypes.IN_WALL) && this.suffocationGracePeriod > 0) {
            return false;
        }

        // --- 2. Reset Armor Flag ---
        this.armorAbsorbedDamage = false;

        // --- 3. Delegate to Vanilla Logic ---
        boolean result = super.damage(source, amount);

        // --- 4. Armor Absorption Override ---
        // If armor absorbed the damage, tell the engine "yes, the entity
        // was hit", which is required for the attacker to apply knockback/SFX.
        if (this.armorAbsorbedDamage) {
            return true;
        }
        return result;
    }
    @Override
    protected void applyDamage(DamageSource source, float amount) {
        // --- Armor Protection Logic ---
        // Overrides the actual application of damage to the entity's health, thus
        // intercepting the damage after the game has decided the entity was hit.
        // 1.20.1: Use BYPASSES_ARMOR instead of BYPASSES_WOLF_ARMOR
        if (!this.getWorld().isClient && !source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            // We need to modify the actual item stack that lives in the server's inventory.
            ItemStack realArmorStack = this.items.get(ARMOR_SLOT_INDEX);

            if (!realArmorStack.isEmpty() && realArmorStack.getItem() instanceof HamsterArmorItem) {
                // Flag handling this damage
                this.armorAbsorbedDamage = true;

                // 1. Snapshot the stack before it's removed for particles.
                ItemStack particleStack = realArmorStack.copy();

                // 2. Determine damage to armor
                int armorDamage = (int) Math.ceil(amount);

                // 3. Damage the item in the inventory
                // 1.20.1: Use Consumer callback instead of EquipmentSlot
                realArmorStack.damage(armorDamage, this, e -> e.sendEquipmentBreakStatus(EquipmentSlot.CHEST));

                // 4. Check for Breakage
                if (realArmorStack.isEmpty()) {
                    // Play break sound immediately
                    // 1.20.1: Use Shield Break sound
                    this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.5f, 1.2f);

                    // Spawn particles using the snapshot
                    ((ServerWorld) this.getWorld()).spawnParticles(
                            new ItemStackParticleEffect(ParticleTypes.ITEM, particleStack),
                            this.getX(), this.getBodyY(0.5), this.getZ(),
                            15, 0.2, 0.2, 0.2, 0.1
                    );

                    // Flag slot to be cleared in the next tick.
                    this.performDeferredArmorUpdate = true;

                } else {
                    // Play armor repair/damage sound if not broken
                    // 1.20.1 Fix: Use Shield Block sound
                    this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 0.5f, 1.2f);

                    // Spawn absorption particles
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                                new ItemStackParticleEffect(ParticleTypes.ITEM, particleStack),
                                this.getX(), this.getBodyY(0.5), this.getZ(),
                                5, 0.2, 0.2, 0.2, 0.05
                        );
                    }
                }

                // Completely negate the health damage by not calling super.applyDamage
                return;
            }
        }

        // If no armor or damage bypasses armor, apply health damage normally
        super.applyDamage(source, amount);
    }
    @Override
    public boolean canMoveVoluntarily() {
        return super.canMoveVoluntarily() && !this.isThrown();
    }
    @Override
    public boolean isPushable() {
        // A hamster is not pushable if it's being thrown OR if it's sleeping in a bed.
        if (this.isThrown() || (this.isSleeping() && this.getLinkedBedPos().isPresent())) {
            return false;
        }
        return super.isPushable();
    }
    @Override
    public void setStack(int slot, ItemStack stack) {
        // 1. Capture previous state
        ItemStack oldStack = this.items.get(slot).copy(); // Use direct list access for old state

        // 2. Call super implementation directly to update the inventory list
        this.getItems().set(slot, stack);

        // 3. Sync Trackers if equipment slots changed
        if (!this.getWorld().isClient) {
            if (slot == ACCESSORY_SLOT_INDEX || slot == ARMOR_SLOT_INDEX) {
                updateEquipmentTrackers();
            }
        }

        // 4. Trigger sounds, check suppression flag
        if (!this.getWorld().isClient && !this.isLoadingNbt && !this.isSilentInventoryUpdate) {
            handleSlotUpdateSounds(slot, oldStack, stack);
        }

        // 5. Mark Dirty
        this.markDirty();
    }
    @Override
    public ItemStack removeStack(int slot) {
        // Intercepts item removal (e.g. taking item from GUI) to trigger sound effects.
        // 1. Capture state BEFORE removal
        ItemStack oldStack = this.getStack(slot).copy();

        // 2. Perform removal using the interface's default logic
        ItemStack result = ImplementedInventory.super.removeStack(slot);

        // 3. Capture state AFTER removal (should be empty)
        ItemStack newStack = this.getStack(slot);

        // 4. Trigger sounds, check suppression flag
        if (!this.getWorld().isClient && !this.isLoadingNbt && !this.isSilentInventoryUpdate) {
            handleSlotUpdateSounds(slot, oldStack, newStack);
        }
        return result;
    }
    @Override
    public ItemStack removeStack(int slot, int amount) {
        // Intercepts split stack removal to trigger sound effects.
        // 1. Capture state BEFORE removal
        ItemStack oldStack = this.getStack(slot).copy();

        // 2. Perform removal
        ItemStack result = ImplementedInventory.super.removeStack(slot, amount);

        // 3. Capture state AFTER removal
        ItemStack newStack = this.getStack(slot);

        // 4. Trigger sounds, check suppression flag
        if (!this.getWorld().isClient && !this.isLoadingNbt && !this.isSilentInventoryUpdate) {
            handleSlotUpdateSounds(slot, oldStack, newStack);
        }

        return result;
    }
    /**
     * True any time the hamster is falling.
     */
    public boolean shouldRenderFlying() {
        if (this.isSitting()) return false;

        return this.isThrown() || (!this.isOnGround() && this.getVelocity().y < -0.01); // Extremely high sensitivity
    }
    /**
     * Mounts the player onto the hamster and configures the state for riding.
     * Called by the server-side packet handler.
     * @param player The player to mount.
     */
    public void putPlayerOnBack(PlayerEntity player) {
        if (!this.hasPassenger(player)) {
            player.startRiding(this);
            // Force stand up to allow movement
            this.setSitting(false, false);

            // If the owner mounts, disable wander mode to give them full control.
            if (this.isOwner(player)) {
                this.setWanderModeActive(false);
            }
        }
    }
    /**
     * Determines the entity controlling this mob.
     * <p>
     * Only allows the passenger to steer if the hamster is tamed and the passenger
     * is the verified owner.
     */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        // Only allow steering if tamed and the passenger is the owner
        if (this.isTamed()) {
            Entity firstPassenger = this.getFirstPassenger();
            if (firstPassenger instanceof LivingEntity passenger && this.isOwner(passenger)) {
                return passenger;
            }
        }
        return null;
    }
    /**
     * Manages movement physics and rider inputs.
     * <p>
     * If ridden by the owner, this method synchronizes rotation, calculates speed based on config settings,
     * and executes jump logic on both the Client (for prediction) and Server (for sound/authority).
     */
    @Override
    public void travel(Vec3d movementInput) {
        if (this.isAlive()) {
            LivingEntity passenger = this.getControllingPassenger();
            if (this.isTamed() && passenger instanceof PlayerEntity player) {

                // --- 1. Sync Mount Rotation to Rider ---
                this.setYaw(player.getYaw());
                this.prevYaw = this.getYaw();
                this.setPitch(player.getPitch() * 0.5F);
                this.setRotation(this.getYaw(), this.getPitch());
                this.bodyYaw = this.getYaw();
                this.headYaw = this.bodyYaw;

                // --- 2. Read Rider Movement Input ---
                float forwardSpeed = player.forwardSpeed;
                float sidewaysSpeed = player.sidewaysSpeed;

                // Backward movement penalty
                if (forwardSpeed <= 0.0F) {
                    forwardSpeed *= 0.25F;
                }

                // --- 3. Configuration & Speed Calculation ---
                // Perform this on both Client and Server to ensure attributes are synced.
                final AhpConfig config = AdorableHamsterPets.CONFIG;

                // A. Calculate Sprint State
                // Check 'riderSprintHeld' (Input) AND actual movement (Physics)
                // Prevents "Toggle Sprint" from keeping hamster in a sprint state while standing still
                boolean hasMovement = Math.abs(forwardSpeed) > 1.0e-5 || Math.abs(sidewaysSpeed) > 1.0e-5;
                boolean isSprinting = this.riderSprintHeld && hasMovement;

                // Sync the visual sprinting state (particles/FOV)
                this.setSprinting(isSprinting);

                // B. Select Config Multiplier
                double speedMultiplier = isSprinting
                        ? config.ridingSprintSpeedMultiplier.get()
                        : config.ridingBaseSpeedMultiplier.get();

                // C. Get Attribute Base (Includes Gold Armor buff automatically)
                float attributeSpeed = (float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);

                // D. Apply Multiplier
                float finalSpeed = (float) (attributeSpeed * speedMultiplier);

                // E. Apply Potion Effects (Additive on top of multiplier)
                if (this.hasStatusEffect(StatusEffects.SPEED)) {
                    finalSpeed += 0.1f;
                }

                this.setMovementSpeed(finalSpeed);

                // --- 4. Jump Logic ---
                // Apply BEFORE travel so it participates in the same tick's movement integration
                if (this.riderJumpCooldown > 0) {
                    this.riderJumpCooldown--;
                } else if (this.riderJumpQueued) {
                    this.riderJumpQueued = false; // consume
                    this.tryRiderJump();
                }

                // --- 5. Movement Execution ---
                if (this.isLogicalSideForUpdatingMovement()) {
                    // Logic: Server controlling mob (e.g. no rider, or rider not a player)
                    super.travel(new Vec3d(sidewaysSpeed, 0.0, forwardSpeed));
                } else if (player instanceof ClientPlayerEntity) {
                    // Logic: Physical Client controlling mob
                    super.travel(new Vec3d(sidewaysSpeed, 0.0, forwardSpeed));
                } else {
                    // Logic: Server when mob is controlled by client player.
                    // We DO NOT call super.travel() here.
                    // The client sends position packets. Calling travel() here causes rubberbanding.
                    // However, we successfully ran Step 4 (Jump Logic) above, so the Sound plays and cooldown resets!
                }
                return;
            }
        }
        // Default movement
        super.travel(movementInput);
    }
    /**
     * Updates the input state from the rider.
     * Called by both the Server (via packet) and Client (via prediction).
     */
    public void setRiderInput(boolean jump, boolean sprint) {
        // Rising edge logic for jump
        if (jump && !this.riderJumpHeld) {
            this.riderJumpQueued = true;
            // Only log on server to avoid spam
            if (!this.getWorld().isClient()) {
                AdorableHamsterPets.LOGGER.info("[AHP JUMP][SERVER] hamsterId={} queuedJump=true", this.getId());
            }
        }
        this.riderJumpHeld = jump;
        this.riderSprintHeld = sprint;
    }

    // --- Inventory Implementation ---
    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void markDirty() {
        if (!this.getWorld().isClient()) {
            this.updateCheekTrackers();
            this.updateAccessoryState();
        }
    }
    public ItemStack getArmorStack() {
        return this.dataTracker.get(TRACKED_ARMOR_STACK);
    }

    public ItemStack getAccessoryStack() {
        return this.dataTracker.get(TRACKED_ACCESSORY_STACK);
    }

    public void setArmorStack(ItemStack stack) {
        this.setStack(ARMOR_SLOT_INDEX, stack); // Use setStack to trigger sync
    }

    /**
     * Gets the display name for the hamster.
     * This will be the hamster's custom name if it has one, otherwise it defaults
     * to a translatable title.
     *
     * @return The {@link Text} component to be used as the screen's title.
     */
    @Override
    public Text getDisplayName() {
        // If the entity has a custom name from a name tag, always use that.
        if (this.hasCustomName()) {
            return super.getDisplayName();
        }

        // If no custom name, check the config for the default name.
        if (Configs.AHP.useHampterName) {
            return Text.translatable("entity.adorablehamsterpets.hampter");
        }

        // Otherwise, use the default vanilla behavior, which will resolve to "entity.adorablehamsterpets.hamster".
        return super.getDisplayName();
    }


    // --- Override isValid for Hopper Interaction ---
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // --- 1. Cheek Pouches (Slots 0-5) ---
        if (slot < CHEEK_POUCH_SIZE) {
            return !this.isItemDisallowed(stack);
        }
        // --- 2. Accessory Slot (Slot 6) ---
        if (slot == ACCESSORY_SLOT_INDEX) {
            return stack.isOf(ModItems.ACORN_HAT.get()) || stack.isOf(Items.PINK_PETALS);
        }
        // --- 3. Armor Slot (Slot 7) ---
        if (slot == ARMOR_SLOT_INDEX) {
            return stack.getItem() instanceof HamsterArmorItem;
        }
        return false;
    }

    /**
     * Updates the DataTrackers for cheek fullness based on the inventory content.
     * Only checks the cheek pouch slots (0-5).
     */
    public void updateCheekTrackers() {
        // --- Update Left Cheek (Slots 0, 1, 2) ---
        boolean leftFull = false;
        for (int i = 0; i < 3; i++) {
            if (!this.items.get(i).isEmpty()) {
                leftFull = true;
                break;
            }
        }

        // --- Update Right Cheek (Slots 3, 4, 5) ---
        boolean rightFull = false;
        for (int i = 3; i < CHEEK_POUCH_SIZE; i++) { // Stop at index 5
            if (!this.items.get(i).isEmpty()) {
                rightFull = true;
                break;
            }
        }

        // --- Set Data Trackers ---
        if (this.isLeftCheekFull() != leftFull) this.setLeftCheekFull(leftFull);
        if (this.isRightCheekFull() != rightFull) this.setRightCheekFull(rightFull);

        // --- Trigger "Chipmunk Aspirations" Advancement ---
        if (!this.getWorld().isClient() && this.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
            boolean allSlotsFilled = true;
            for (int i = 0; i < CHEEK_POUCH_SIZE; i++) {
                if (this.items.get(i).isEmpty()) {
                    allSlotsFilled = false;
                    break;
                }
            }
            if (allSlotsFilled) {
                ModCriteria.HAMSTER_POUCH_FILLED.trigger(serverPlayerOwner, this);
            }
        }
    }

    /**
     * Synchronizes the internal inventory equipment slots with the DataTracker.
     * Allowed on client ONLY if this is a shoulder pet (dummy entity).
     */
    public void updateEquipmentTrackers() {
        // Allow if server OR if it's a client-side shoulder dummy
        if (this.getWorld().isClient() && !this.isShoulderPet()) return;

        ItemStack accessory = this.items.get(ACCESSORY_SLOT_INDEX);
        ItemStack armor = this.items.get(ARMOR_SLOT_INDEX);

        this.dataTracker.set(TRACKED_ACCESSORY_STACK, accessory);
        this.dataTracker.set(TRACKED_ARMOR_STACK, armor);
    }

    // --- NBT Saving/Loading ---
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        // --- 1. Write Core Data & Flags ---
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("HamsterVariant", this.getVariant());

        // For backward compatibility, write the flags out as individual booleans.
        if (this.isTamed()) {
            nbt.putBoolean("Sitting", getHamsterFlag(SITTING_FLAG));
        }
        nbt.putBoolean("KnockedOut", getHamsterFlag(KNOCKED_OUT_FLAG));
        nbt.putBoolean("CheekPouchUnlocked", getHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG));

        nbt.putLong("ThrowCooldownEnd", this.throwCooldownEndTick);
        nbt.putLong("GreenBeanBuffDuration", this.getDataTracker().get(GREEN_BEAN_BUFF_DURATION));
        nbt.putInt("AutoEatCooldown", this.autoEatCooldownTicks);
        nbt.putInt("EjectionCheckCooldown", this.ejectionCheckCooldown);
        nbt.putInt("PinkPetalType", this.dataTracker.get(PINK_PETAL_TYPE));
        nbt.putInt("AnimationPersonalityId", this.dataTracker.get(ANIMATION_PERSONALITY_ID));

        // --- 2. Write Sleep State Data ---
        nbt.putInt("DozingPhase", this.getDozingPhase().ordinal());
        nbt.putString("CurrentDeepSleepAnimId", this.dataTracker.get(CURRENT_DEEP_SLEEP_ANIM_ID));
        nbt.putInt("QuiescentSitTimer", this.quiescentSitDurationTimer);
        nbt.putInt("DriftingOffTimer", this.driftingOffTimer);
        nbt.putInt("SettleSleepCooldown", this.settleSleepAnimationCooldown);

        // --- 3. Write Inventory ---
        NbtCompound inventoryWrapperNbt = new NbtCompound();
        Inventories.writeNbt(inventoryWrapperNbt, this.items);
        nbt.put("Inventory", inventoryWrapperNbt);

        // --- 4. Write Seeking and Sulking Data ---
        nbt.putBoolean("IsPrimedToSeekDiamonds", this.isPrimedToSeekDiamonds);
        nbt.putLong("FoundOreCooldownEndTick", this.foundOreCooldownEndTick);
        if (this.currentOreTarget != null) {
            nbt.putInt("OreTargetX", this.currentOreTarget.getX());
            nbt.putInt("OreTargetY", this.currentOreTarget.getY());
            nbt.putInt("OreTargetZ", this.currentOreTarget.getZ());
        }
        nbt.putBoolean("IsSulking", getHamsterFlag(SULKING_FLAG));
        nbt.putBoolean("IsCelebratingDiamond", getHamsterFlag(CELEBRATING_DIAMOND_FLAG));

        // --- 5. Write Item Interest Data ---
        if (this.isHoldingInterestItem()) {
            nbt.putBoolean("isHoldingInterestItem", true);
            nbt.putInt("ItemInterestTimer", this.getItemInterestTimer());
            // Save the stolen item stack using the 1.20.1 method
            if (!this.getInterestItemStack().isEmpty()) {
                nbt.put("InterestItemStack", this.getInterestItemStack().writeNbt(new NbtCompound()));
            }
        }

        // --- 6. Write Wander Mode Data if Relevant ---
        nbt.putBoolean("IsWanderModeActive", this.isWanderModeActive());
        this.linkedBedPos.ifPresent(globalPos -> {
            // In 1.20.1, use RegistryOps.of() and handle getOrThrow arguments
            DataResult<NbtElement> result = GlobalPos.CODEC.encodeStart(RegistryOps.of(NbtOps.INSTANCE, this.getWorld().getRegistryManager()), globalPos);
            result.result().ifPresent(tag -> nbt.put("LinkedBedPos", tag));
        });
        nbt.putBoolean("BypassNextSleepDelay", this.bypassNextSleepDelay);

        // --- 7. Write Flight Data ---
        nbt.putBoolean("HasPlayedIncomingSound", this.hasPlayedIncomingSound);

    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        this.isLoadingNbt = true; // Suppress sounds
        // --- 1. Read Core Data ---
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("HamsterVariant"));

        // --- Read individual booleans and set flags for backward compatibility ---
        boolean wasSittingNbt = this.isTamed() && nbt.getBoolean("Sitting");
        this.setSitting(wasSittingNbt, true); // This will correctly set the SITTING_FLAG
        setHamsterFlag(KNOCKED_OUT_FLAG, nbt.getBoolean("KnockedOut"));
        setHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG, nbt.getBoolean("CheekPouchUnlocked"));
        setHamsterFlag(SULKING_FLAG, nbt.getBoolean("IsSulking"));
        setHamsterFlag(CELEBRATING_DIAMOND_FLAG, nbt.getBoolean("IsCelebratingDiamond"));

        this.throwCooldownEndTick = nbt.getLong("ThrowCooldownEnd");
        this.getDataTracker().set(GREEN_BEAN_BUFF_DURATION, nbt.getLong("GreenBeanBuffDuration"));
        this.autoEatCooldownTicks = nbt.getInt("AutoEatCooldown");
        this.ejectionCheckCooldown = nbt.contains("EjectionCheckCooldown", NbtElement.INT_TYPE) ? nbt.getInt("EjectionCheckCooldown") : 20;
        this.dataTracker.set(PINK_PETAL_TYPE, nbt.getInt("PinkPetalType"));

        // If the NBT from a command or save file doesn't specify an ID, assign one.
        // This covers /summon and ensures the ID persists through saves.
        if (!nbt.contains("AnimationPersonalityId", NbtElement.INT_TYPE)) {
            int personalityId = this.random.nextBetween(1, 3);
            this.dataTracker.set(ANIMATION_PERSONALITY_ID, personalityId);
            AdorableHamsterPets.LOGGER.debug("[NBT READ] Hamster ID {}: NBT had no personality, assigned new ID {}", this.getId(), personalityId);
        } else {
            // If it does contain one (e.g., from a saved world), read it normally.
            this.dataTracker.set(ANIMATION_PERSONALITY_ID, nbt.getInt("AnimationPersonalityId"));
        }

        // --- 2. Read Sleep State Data ---
        if (nbt.contains("DozingPhase", NbtElement.INT_TYPE)) {
            int phaseOrdinal = nbt.getInt("DozingPhase");
            if (phaseOrdinal >= 0 && phaseOrdinal < DozingPhase.values().length) {
                this.setDozingPhase(DozingPhase.values()[phaseOrdinal]);
            } else {
                this.setDozingPhase(DozingPhase.NONE);
            }
        } else {
            this.setDozingPhase(DozingPhase.NONE);
        }
        this.dataTracker.set(CURRENT_DEEP_SLEEP_ANIM_ID, nbt.getString("CurrentDeepSleepAnimId"));
        this.quiescentSitDurationTimer = nbt.getInt("QuiescentSitTimer");
        this.driftingOffTimer = nbt.getInt("DriftingOffTimer");
        this.settleSleepAnimationCooldown = nbt.getInt("SettleSleepCooldown");

        // --- 3. Read Inventory ---
        this.items.clear();
        if (nbt.contains("Inventory", NbtElement.COMPOUND_TYPE)) {
            Inventories.readNbt(nbt.getCompound("Inventory"), this.items);
        }
        // If the NBT from a command or save file doesn't specify wild loot, generate it.
        if (!hasInventoryData(nbt) && !this.isTamed()) {
            generateWildLoot();
        }
        this.updateCheekTrackers();
        this.updateEquipmentTrackers();

        // --- 4. Read Seeking Data ---
        this.isPrimedToSeekDiamonds = nbt.getBoolean("IsPrimedToSeekDiamonds");
        this.foundOreCooldownEndTick = nbt.getLong("FoundOreCooldownEndTick");
        if (nbt.contains("OreTargetX") && nbt.contains("OreTargetY") && nbt.contains("OreTargetZ")) {
            this.currentOreTarget = new BlockPos(nbt.getInt("OreTargetX"), nbt.getInt("OreTargetY"), nbt.getInt("OreTargetZ"));
        } else {
            this.currentOreTarget = null;
        }

        // --- 5. Read Item Interest Data ---
        this.setHoldingInterestItem(nbt.getBoolean("isHoldingInterestItem"));
        if (this.isHoldingInterestItem()) {
            this.setItemInterestTimer(nbt.getInt("ItemInterestTimer"));
            if (nbt.contains("InterestItemStack", NbtElement.COMPOUND_TYPE)) {
                // Use the 1.20.1 method to read the ItemStack from NBT
                this.setInterestItemStack(ItemStack.fromNbt(nbt.getCompound("InterestItemStack")));
            }
        } else {
            this.setItemInterestTimer(0);
            this.setInterestItemStack(ItemStack.EMPTY);
        }

        // --- 6. Read Wander Mode Data if Relevant ---
        setWanderModeActive(nbt.getBoolean("IsWanderModeActive"));
        if (nbt.contains("LinkedBedPos")) {
            // In 1.20.1, use RegistryOps.of() and handle getOrThrow arguments
            this.linkedBedPos = GlobalPos.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, this.getWorld().getRegistryManager()), nbt.get("LinkedBedPos")).result();
        } else {
            this.linkedBedPos = Optional.empty();
        }
        this.bypassNextSleepDelay = nbt.getBoolean("BypassNextSleepDelay");

        // --- 7. Read Flight Data ---
        this.hasPlayedIncomingSound = nbt.getBoolean("HasPlayedIncomingSound");

        this.isLoadingNbt = false;
    }


    // --- Shoulder Riding Data Handling ---
    /**
     * Captures the current state of this hamster into a {@link HamsterShoulderData} record.
     * This record can then be serialized to NBT and stored on the player's DataTracker.
     *
     * @return A {@link HamsterShoulderData} record containing the hamster's current data.
     */
    public HamsterShoulderData saveToShoulderData() {
        // --- 1. Update Trackers and Prepare NBT ---
        this.updateCheekTrackers();
        NbtCompound inventoryNbt = new NbtCompound();
        // In 1.20.1, writeNbt does not take a registry manager.
        Inventories.writeNbt(inventoryNbt, this.items);

        // --- 2. Save Active Status Effects ---
        // In 1.20.1, the active_effects are stored directly in the NbtList, not a wrapper compound.
        NbtList effectsList = new NbtList();
        for (StatusEffectInstance effectInstance : this.getStatusEffects()) {
            effectsList.add(effectInstance.writeNbt(new NbtCompound()));
        }

        // --- 3. Get Custom Name ---
        Optional<String> nameOptional = Optional.ofNullable(this.getCustomName()).map(Text::getString);

        // --- 4. Create Inner Data Record Instances ---
        HamsterShoulderData.SeekingBehaviorData seekingData = new HamsterShoulderData.SeekingBehaviorData(
                this.isPrimedToSeekDiamonds,
                this.foundOreCooldownEndTick,
                Optional.ofNullable(this.currentOreTarget)
        );
        HamsterShoulderData.GreenBeanBuffData buffData = new HamsterShoulderData.GreenBeanBuffData(
                this.greenBeanBuffEndTick,
                this.getDataTracker().get(GREEN_BEAN_BUFF_DURATION),
                effectsList
        );
        HamsterShoulderData.WanderModeData wanderData = new HamsterShoulderData.WanderModeData(
                this.linkedBedPos,
                this.bypassNextSleepDelay
        );

        // --- 5. Create and Return the Main Data Record ---
        return new HamsterShoulderData(
                this.getUuid(),
                this.getVariant(),
                this.getHealth(),
                inventoryNbt,
                this.getBreedingAge(),
                this.throwCooldownEndTick,
                buffData,
                this.autoEatCooldownTicks,
                nameOptional,
                this.dataTracker.get(PINK_PETAL_TYPE),
                this.dataTracker.get(ANIMATION_PERSONALITY_ID),
                seekingData,
                wanderData,
                this.dataTracker.get(HAMSTER_FLAGS) // Pass the entire packed integer
        );
    }

    // --- Entity Behavior ---
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) { return false; }

    @Override
    public void changeLookDirection(double cursorX, double cursorY) {
        if (this.isSleeping()) return;
        super.changeLookDirection(cursorX, cursorY);
    }

    /**
     * Finds a safe spawn position for the hamster near an initial target position.
     * The search is performed in stages for efficiency and logical placement:
     * 1. Checks the initial target position itself.
     * 2. Checks a few blocks directly above the target.
     * 3. Performs a horizontal spiral search outwards on the same Y-level.
     *
     * @param initialTarget The desired starting point for the search.
     * @param world         The world where the search is performed.
     * @param searchRadius  The maximum horizontal radius for the spiral search.
     * @return An Optional containing the first safe BlockPos found, or an empty Optional if no safe spot is found within the search radius.
     */
    public Optional<BlockPos> findSafeSpawnPosition(BlockPos initialTarget, World world, int searchRadius) {
        return findSafeSpawnPosition(initialTarget, world, searchRadius, Collections.emptySet());
    }

    /**
     * Finds a safe, unoccupied spawn position for the hamster near an initial target position,
     * avoiding any positions present in the provided occupied set.
     *
     * @param initialTarget The desired starting point for the search.
     * @param world         The world where the search is performed.
     * @param searchRadius  The maximum horizontal radius for the spiral search.
     * @param occupiedPositions A set of positions that are already taken and should be avoided.
     * @return An Optional containing the first safe and unoccupied BlockPos found, or an empty Optional.
     */
    public Optional<BlockPos> findSafeSpawnPosition(BlockPos initialTarget, World world, int searchRadius, Set<BlockPos> occupiedPositions) {
        // --- Stage 1: Initial Target Check ---
        if (isSafeSpawnLocation(initialTarget, world) && !occupiedPositions.contains(initialTarget)) {
            return Optional.of(initialTarget);
        }

        // --- Stage 2: Vertical Vicinity Check (Upwards) ---
        for (int i = 1; i <= 3; i++) {
            BlockPos abovePos = initialTarget.up(i);
            if (isSafeSpawnLocation(abovePos, world) && !occupiedPositions.contains(abovePos)) {
                return Optional.of(abovePos);
            }
        }

        // --- Stage 3: Horizontal Spiral Search ---
        for (int r = 1; r <= searchRadius; r++) {
            for (int i = -r; i <= r; i++) {
                for (int j = -r; j <= r; j++) {
                    // Only check the "ring" of the spiral, not the inside which was already checked
                    if (Math.abs(i) != r && Math.abs(j) != r) {
                        continue;
                    }
                    BlockPos checkPos = initialTarget.add(i, 0, j);
                    if (isSafeSpawnLocation(checkPos, world) && !occupiedPositions.contains(checkPos)) {
                        return Optional.of(checkPos);
                    }
                }
            }
        }

        // --- Stage 4: Failure ---
        return Optional.empty();
    }

    /**
     * Overrides the vanilla {@link TameableEntity#setSitting(boolean)} method.
     * <p>
     * This method acts as an interceptor for any vanilla or external mod logic that
     * attempts to change the sitting state (e.g., the vanilla {@code SitGoal}). It redirects
     * the call to the custom overloaded {@link #setSitting(boolean, boolean)} method,
     * ensuring that all mod-specific logic (like sleep sequence resets and animation state)
     * is correctly handled.
     *
     * @param sitting {@code true} to make the hamster sit, {@code false} to make it stand.
     */
    @Override
    public void setSitting(boolean sitting) {
        // Calls the overload below. We want player-initiated sits to NOT play the sleep sound.
        // So, suppressSound should always be true when called from here.
        this.setSitting(sitting, true); // Always suppress sound for this basic toggle
    }

    // --- Overload for setSitting (ONLY controls IS_SITTING) ---
    /**
     * Sets the player-commanded sitting state of the hamster.
     * This method updates the {@code IS_SITTING} DataTracker and the vanilla sitting pose.
     * If the hamster is being told to stand up while it was in a dozing/sleep sequence,
     * the sleep sequence will be reset.
     *
     * @param sitting True to make the hamster sit, false to make it stand.
     * @param suppressSound True to suppress any sound normally associated with this action (parameter exists for API compatibility, not actively used for sound suppression within this method currently).
     */
    public void setSitting(boolean sitting, boolean suppressSound) {
        // --- 1. Play sound and trigger animation based on state change ---
        boolean wasSitting = this.isSitting();
        if (sitting && !wasSitting) { // Transitioning to sitting
            int personalityId = this.dataTracker.get(ANIMATION_PERSONALITY_ID);
            String animToTrigger = switch (personalityId) {
                case 2 -> "sit2";
                case 3 -> "sit3";
                default -> "sit1";
            };
            this.triggerAnimOnServer("mainController", animToTrigger);
            triggerSettleEffects(0.12f, 7, 0.2f); // Swish now, thump in 7 ticks when hamster lands
        } else if (!sitting && wasSitting) { // Transitioning from sitting
            if (!this.getWorld().isClient()) {
                this.getWorld().playSound(null, this.getBlockPos(), ModSounds.HAMSTER_SWISH.get(), SoundCategory.NEUTRAL, 0.1f, 1.0f + this.random.nextFloat() * 0.5f);
            }
            int personalityId = this.dataTracker.get(ANIMATION_PERSONALITY_ID);
            String animToTrigger = switch (personalityId) {
                case 2 -> "standup2";
                case 3 -> "standup3";
                default -> "standup1";
            };
            this.triggerAnimOnServer("mainController", animToTrigger);
        }

        // --- 2. Reset Sleep Sequence if Standing Up from a Doze/Sleep ---
        if (!sitting && this.isTamed() && this.getDozingPhase() != DozingPhase.NONE) {
            resetSleepSequence("Player commanded hamster to stand up.");
        }

        // --- 3. Update Core Sitting State ---
        setHamsterFlag(SITTING_FLAG, sitting);

        // --- 4. Update Vanilla State ---
        this.setInSittingPose(sitting);

        // --- 5. Manage Cleaning Timers and Quiescent Sit Timer on State Change ---
        if (sitting) {
            // When commanded to sit, ensure the cleaning timer is reset.
            this.cleaningTimer = 0;
            // quiescentSitDurationTimer will be set by the tick method when DozingPhase becomes QUIESCENT_SITTING.
        } else {
            // If standing up, reset the quiescent sit timer to prevent immediate re-entry into sleep sequence.
            this.quiescentSitDurationTimer = 0;
            // Also ensure cleaning stops if it was active.
            this.cleaningTimer = 0;
            // Explicitly set the cleaning state to false.
            if (getHamsterFlag(CLEANING_FLAG)) {
                setHamsterFlag(CLEANING_FLAG, false);
            }
        }
    }

    // --- Override isInAttackRange ---
    /**
     * Checks if the target entity is within the hamster's shorter melee attack range.
     * Overrides the default MobEntity check which uses a larger expansion.
     * @param entity The entity to check range against.
     * @return True if the entity is within the custom attack range, false otherwise.
     */
    @Override
    public boolean isInAttackRange(LivingEntity entity) {
        // --- Calculate and check intersection with a smaller attack box ---
        // Get the hamster's current bounding box
        Box hamsterBox = this.getBoundingBox();
        // Expand it horizontally by the custom smaller amount
        Box attackBox = hamsterBox.expand(HAMSTER_ATTACK_BOX_EXPANSION, 0.0D, HAMSTER_ATTACK_BOX_EXPANSION);
        // Check if this smaller attack box intersects the target's hitbox
        boolean intersects = attackBox.intersects(entity.getBoundingBox());
        return intersects;
    }

    // --- Target Exclusion Override ---
    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        UUID ownerUuid = owner.getUuid();
        AdorableHamsterPets.LOGGER.trace("[canAttackWithOwner] Hamster: {}, Target: {}, Owner: {}", this.getName().getString(), target.getName().getString(), owner.getName().getString());

        // --- 1. Basic Exclusions (Self, Owner) ---
        if (target == this || target == owner) {
            return false;
        }
        if (target instanceof PlayerEntity && target.getUuid().equals(ownerUuid)) {
            return false;
        }

        // --- 2. Exclude Creepers and Armor Stands ---
        if (target instanceof CreeperEntity || target instanceof ArmorStandEntity) {
            return false;
        }

        // --- 3. Explicitly Check for TameableEntity ---
        if (target instanceof TameableEntity tameablePet) {
            UUID petOwnerUuid = tameablePet.getOwnerUuid();
            if (petOwnerUuid != null && petOwnerUuid.equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace("[canAttackWithOwner] Target is a TameableEntity owned by the same player. Preventing attack.");
                return false;
            }
        }

        // --- 4. Explicitly Check for AbstractHorseEntity ---
        else if (target instanceof net.minecraft.entity.passive.AbstractHorseEntity horsePet) {
            Entity horseOwnerEntity = horsePet.getOwner();
            if (horseOwnerEntity != null && horseOwnerEntity.getUuid().equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace("[canAttackWithOwner] Target is an AbstractHorseEntity owned by the same player. Preventing attack.");
                return false;
            }
        }

        // --- 5. General Ownable Check (Fallback) ---
        else if (target instanceof Ownable ownableFallback) {
            Entity fallbackOwnerEntity = ownableFallback.getOwner();
            if (fallbackOwnerEntity != null && fallbackOwnerEntity.getUuid().equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace("[canAttackWithOwner] Target is an Ownable (fallback) owned by the same player. Preventing attack.");
                return false;
            }
        }

        // --- 6. Default: Allow Attack ---
        return true;
    }

    // --- Interaction Logic ---
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        // --- Hamster Riding Check ---
        // Prevent interaction if the player is currently riding this hamster.
        if (this.hasPassenger(player)) {
            return ActionResult.PASS;
        }

        // --- Initial Setup ---
        ItemStack stack = player.getStackInHand(hand);
        World world = this.getWorld();
        AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Interaction start. Player: {}, Hand: {}, Item: {}", this.getId(), world.getTime(), player.getName().getString(), hand, stack.getItem());

        // --- Interaction Cooldown Check ---
        if (this.interactionCooldown > 0) {
            AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Interaction cooldown active ({} ticks left). Passing.", this.getId(), world.getTime(), this.interactionCooldown);
            return ActionResult.PASS;
        }

        // --- Toggle Jade Debug with Guide Book ---
        if (player.isSneaking() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (!world.isClient) { // Server-side logic
                AhpConfig currentConfig = AdorableHamsterPets.CONFIG;
                boolean currentSetting = currentConfig.enableJadeHamsterDebugInfo;
                boolean newSetting = !currentSetting;

                currentConfig.enableJadeHamsterDebugInfo = newSetting;
                currentConfig.save(); // Save the config to file

                Text message = Text.translatable(
                        newSetting ? "message.adorablehamsterpets.debug_overlay_enabled" : "message.adorablehamsterpets.debug_overlay_disabled"
                ).formatted(newSetting ? Formatting.WHITE : Formatting.RED);
                player.sendMessage(message, true); // Send to action bar

                AdorableHamsterPets.LOGGER.info("Player {} toggled Jade Hamster Debug Info via Guide Book to: {} for hamster {}", player.getName().getString(), newSetting, this.getId());
            }
            return ActionResult.success(world.isClient()); // Consume the action
        }

        // --- Hamster Bed Linking/Configuration ---
        if (this.isTamed() && this.isOwner(player) && stack.getItem() instanceof HamsterBedItem) {
            if (!world.isClient) {
                // 1.20.1 NBT read Logic
                UUID linkedUuid = null;
                if (stack.hasNbt() && stack.getNbt().contains(ModNbtKeys.LINKED_HAMSTER_UUID)) {
                    linkedUuid = stack.getNbt().getUuid(ModNbtKeys.LINKED_HAMSTER_UUID);
                }
                Text nameToSet;
                if (this.hasCustomName()) {
                    nameToSet = this.getName();
                } else {
                    nameToSet = this.getDisplayName().copy().append(" " + this.getId());
                }
                String nameJson = Text.Serializer.toJson(nameToSet);

                if (linkedUuid == null) {
                    // Case 1: Initial Linking (Unlinked Bed)
                    ItemStack newStack = stack.copy();
                    NbtCompound nbt = newStack.getOrCreateNbt();

                    nbt.putUuid(ModNbtKeys.LINKED_HAMSTER_UUID, this.getUuid());
                    nbt.putString(ModNbtKeys.LINKED_HAMSTER_NAME, nameJson);
                    nbt.putString(ModNbtKeys.WANDER_DISTANCE, Configs.AHP.defaultWanderDistance.get().asString());

                    player.setStackInHand(hand, newStack);

                    world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BAMBOO_WOOD_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    ((ServerWorld) world).spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getBodyY(0.5), this.getZ(), 10, 0.5, 0.5, 0.5, 0.0);
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_linked", this.getName()), true);

                    // Trigger advancement
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        ModCriteria.HAMSTER_BED_LINKED.trigger(serverPlayer);
                    }

                } else if (linkedUuid.equals(this.getUuid())) {
                    // Case 2: Re-configuring Wander Distance of already linked bed
                    // 1.20.1 NBT logic
                    WanderDistance currentDistance = Configs.AHP.defaultWanderDistance.get();
                    NbtCompound stackNbt = stack.getOrCreateNbt();

                    if (stackNbt.contains(ModNbtKeys.WANDER_DISTANCE)) {
                        try {
                            currentDistance = WanderDistance.valueOf(stackNbt.getString(ModNbtKeys.WANDER_DISTANCE));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    WanderDistance[] values = WanderDistance.values();
                    WanderDistance nextDistance = values[(currentDistance.ordinal() + 1) % values.length];

                    stackNbt.putString(ModNbtKeys.WANDER_DISTANCE, nextDistance.asString());

                    player.sendMessage(Text.translatable("message.adorablehamsterpets.wander_distance_set", this.getName(), nextDistance.asString()), true);
                    world.playSound(null, this.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.5f, 1.0f);

                } else {
                    // Case 3: Re-linking a bed that was linked to a DIFFERENT hamster
                    ItemStack newStack = stack.copy();
                    NbtCompound nbt = newStack.getOrCreateNbt();

                    nbt.putUuid(ModNbtKeys.LINKED_HAMSTER_UUID, this.getUuid());
                    nbt.putString(ModNbtKeys.LINKED_HAMSTER_NAME, nameJson);
                    nbt.putString(ModNbtKeys.WANDER_DISTANCE, Configs.AHP.defaultWanderDistance.get().asString());  // Reset to default

                    player.setStackInHand(hand, newStack);

                    world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BAMBOO_WOOD_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    ((ServerWorld) world).spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getBodyY(0.5), this.getZ(), 10, 0.5, 0.5, 0.5, 0.0);
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_linked", this.getName()), true);
                }
                // If UUID is present but doesn't match, do nothing.
            }
            return ActionResult.success(world.isClient);
        }

        // --- Taming Logic ---
        if (!this.isTamed()) {
            AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Hamster not tamed. Checking for taming attempt.", this.getId(), world.getTime());
            if (player.isSneaking() && ConfigDataCache.isTamingFood(stack)) {
                AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Taming attempt detected.", this.getId(), world.getTime());
                if (!world.isClient) { tryTame(player, stack); }
                return ActionResult.success(world.isClient());
            }
            AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Not a taming attempt. Calling super.interactMob for untamed.", this.getId(), world.getTime());
            return super.interactMob(player, hand);
        }

        // --- Owner Interaction Logic ---
        if (this.isOwner(player)) {
            AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Player is owner. Processing owner interactions.", this.getId(), world.getTime());
            boolean isSneaking = player.isSneaking();
            PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

            // --- Armor Equipping Logic ---
            if (!player.isSneaking() && stack.getItem() instanceof HamsterArmorItem) {
                if (!world.isClient) {
                    ItemStack currentArmor = this.getArmorStack();
                    ItemStack newArmor = stack.split(1); // Take one from player

                    // Equip new armor
                    this.setArmorStack(newArmor);

                    // Play equip sound
                    world.playSound(null, this.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.NEUTRAL, 0.6f, 1.2f);

                    // Return old armor if it existed
                    if (!currentArmor.isEmpty()) {
                        if (!player.getInventory().insertStack(currentArmor)) {
                            player.dropItem(currentArmor, false);
                        }
                    }
                }
                return ActionResult.success(world.isClient);
            }

            // --- Wake Up From Bed if Sleeping In One ---
            if (this.isSleeping()) {
                if (!world.isClient()) {
                    this.wakeUpFromBed(true); // Manual wake-up
                }
                return ActionResult.success(world.isClient());
            }

            // --- Reset Sleep Sequence if Dozing (not in a bed) ---
            if (this.getDozingPhase() != DozingPhase.NONE) {
                resetSleepSequence("Player interacted with hamster.");
            }

            // --- Handle Item Interest Interaction ---
            if (this.isHoldingInterestItem() && this.isOwner(player)) {
                AdorableHamsterPets.LOGGER.trace("[InteractMob-{}] Passed 'isHoldingInterestItem' check.", this.getId());
                if (!world.isClient) {
                    ItemStack retrievedStack = this.getInterestItemStack().copy();
                    player.getInventory().offerOrDrop(this.getInterestItemStack().copy());
                    this.setInterestItemStack(ItemStack.EMPTY);
                    this.setItemInterestTimer(0);
                    this.setHoldingInterestItem(false);
                    // Set the state flag and initialize the timer so the tick() method can handle the rotation.
                    this.setCelebratingRetrieval(true);
                    this.celebrationRetrievalTicks = 30; // 1.5 second duration
                    this.triggerAnimOnServer("mainController", "anim_hamster_celebrate_chase");
                    // Play a happy/affectionate sound + dynamiic physical touch sound
                    world.playSound(null, this.getBlockPos(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, this.random), SoundCategory.NEUTRAL, 1.0f, this.getSoundPitch());

                    if (!retrievedStack.isEmpty()) {
                        SoundEvent pounceSound = ModSounds.getDynamicItemSound(retrievedStack);
                        float volume = (pounceSound == SoundEvents.ENTITY_GENERIC_EAT) ? 0.35f : 1.0f;
                        world.playSound(null, this.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);

                        // Spawn Particles
                        ((ServerWorld) world).spawnParticles(
                                new ItemStackParticleEffect(ParticleTypes.ITEM, retrievedStack),
                                this.getX(), this.getBodyY(0.5), this.getZ(),
                                10, 0.2, 0.2, 0.2, 0.05
                        );
                    }
                    AdorableHamsterPets.LOGGER.trace("[InteractMob-{}] Item returned to player and goal stopped.", this.getId());
                }
                return ActionResult.success(world.isClient());
            }

            // --- Check for Knocked Out ---
            if (this.isKnockedOut()) {
                AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Hamster is knocked out. Waking up.", this.getId(), world.getTime());
                if (!world.isClient()) {
                    SoundEvent wakeUpSound = getRandomSoundFrom(ModSounds.HAMSTER_WAKE_UP_SOUNDS, this.random);
                    if (wakeUpSound != null) {
                        world.playSound(null, this.getBlockPos(), wakeUpSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    }
                    this.setKnockedOut(false); // Turn off knocked out
                    this.setSitting(false, true); // Make sure sitting doesn't get turned on
                    this.triggerAnimOnServer("mainController", "wakeup_from_ko");
                }
                return ActionResult.success(world.isClient());
            }

            // --- Check for Diamond Celebration ---
            if (this.isCelebratingDiamond()) {
                if (!world.isClient()) {
                    this.setCelebratingDiamond(false); // Turn off celebration
                    this.setSitting(false, true); // Make sure sitting doesn't get turned on
                    SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, this.random);
                    if (affectionSound != null) {
                        world.playSound(null, this.getBlockPos(), affectionSound, SoundCategory.NEUTRAL, 1.0f, this.getSoundPitch());
                    } else { // Fallback
                        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.NEUTRAL, 0.5f, 1.5f);
                    }
                }
                return ActionResult.success(world.isClient()); // Consume the interaction
            }

            // --- Check for Sulking ---
            if (this.isSulking()) {
                if (!world.isClient()) {
                    this.setSulking(false); // Turn off sulking
                    this.setSitting(false, true); // Ensure sitting is also cleared
                    SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, this.random);
                    if (affectionSound != null) {
                        world.playSound(null, this.getBlockPos(), affectionSound, SoundCategory.NEUTRAL, 1.0f, this.getSoundPitch());
                    } else { // Fallback
                        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_CHICKEN_STEP, SoundCategory.NEUTRAL, 0.5f, 1.5f);
                    }
                }
                return ActionResult.success(world.isClient()); // Consume interaction
            }

            // --- Accessory Application/Cycling ---
            if (this.isValid(ACCESSORY_SLOT_INDEX, stack) && !player.isSneaking()) {
                if (!world.isClient) {
                    ItemStack currentAccessory = this.items.get(ACCESSORY_SLOT_INDEX);

                    // Case 1: Cycling Pink Petals
                    if (stack.isOf(Items.PINK_PETALS) && currentAccessory.isOf(Items.PINK_PETALS)) {
                        int currentPetalType = this.dataTracker.get(PINK_PETAL_TYPE);
                        int nextPetalType = (currentPetalType % 3) + 1; // Cycles 1->2->3->1
                        this.dataTracker.set(PINK_PETAL_TYPE, nextPetalType);

                        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_PINK_PETALS_PLACE, SoundCategory.PLAYERS, 0.7f, 1.0f + random.nextFloat() * 0.2f);
                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                                    this.getX(), this.getY() + this.getHeight() * 0.75, this.getZ(),
                                    7, (this.getWidth() / 2.0F), (this.getHeight() / 2.0F), (this.getWidth() / 2.0F), 0.0);
                        }
                        AdorableHamsterPets.LOGGER.trace("[InteractMob {}] Cycled pink petal to type {}.", this.getId(), nextPetalType);
                        // Do not consume item for cycling
                    }
                    // Case 2: General Equip or Swap
                    else {
                        ItemStack toEquip = stack.split(1); // Take one from hand
                        ItemStack toReturn = currentAccessory.copy(); // Capture old item

                        // Equip new item
                        this.setStack(ACCESSORY_SLOT_INDEX, toEquip);

                        // Drop old item if it existed
                        if (!toReturn.isEmpty()) {
                            this.dropStack(toReturn);
                        }

                        // Play Equip Sound
                        world.playSound(null, this.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.0f);

                        // Spawn Dynamic Equip Particles
                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, toEquip),
                                    this.getX(), this.getY() + this.getHeight() * 0.75, this.getZ(),
                                    7, (this.getWidth() / 2.0F), (this.getHeight() / 2.0F), (this.getWidth() / 2.0F), 0.0);
                        }

                        // Specific Trigger for Petals Advancement
                        if (toEquip.isOf(Items.PINK_PETALS) && player instanceof ServerPlayerEntity serverPlayer) {
                            ModCriteria.APPLIED_PINK_PETAL.trigger(serverPlayer, this);
                        }
                    }
                }
                return ActionResult.success(world.isClient());
            }

            // --- Pink Petal & Armor Removal with Shears ---
            if (stack.isOf(Items.SHEARS) && !player.isSneaking()) {
                boolean actionTaken = false;

                // 1. Priority: Remove Armor
                ItemStack armorStack = this.getArmorStack();
                if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem) {
                    if (!world.isClient) {
                        this.dropStack(armorStack);
                        // Suppress generic inventory sound to play specific shearing sound
                        this.isSilentInventoryUpdate = true;
                        this.setArmorStack(ItemStack.EMPTY);
                        this.isSilentInventoryUpdate = false;
                        this.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8f, 1.5f);
                        if (!player.getAbilities().creativeMode) {
                            // For 1.20.1 - Determine EquipmentSlot based on the hand used.
                            stack.damage(1, player, (p) -> p.sendEquipmentBreakStatus(hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                        }
                    }
                    actionTaken = true;
                }

                // 2. Secondary: Remove Accessory (Bling Slot)
                ItemStack accessoryStack = this.items.get(ACCESSORY_SLOT_INDEX);
                if (!actionTaken && !accessoryStack.isEmpty()) {
                    if (!world.isClient) {
                        // Capture a copy for the particle effect
                        ItemStack particleStack = accessoryStack.copy();

                        this.dropStack(accessoryStack);

                        // Suppress generic inventory sound
                        this.isSilentInventoryUpdate = true;
                        this.setStack(ACCESSORY_SLOT_INDEX, ItemStack.EMPTY);
                        this.isSilentInventoryUpdate = false;

                        // Force update trackers immediately to ensure visuals clear
                        this.updateAccessoryState();

                        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 0.9f, 1.0f + random.nextFloat() * 0.1f);

                        // Dynamic particles: use the stack that was just removed
                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, particleStack),
                                    this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
                                    5, (this.getWidth() / 2.0F), (this.getHeight() / 2.0F), (this.getWidth() / 2.0F), 0.05);
                        }

                        if (!player.getAbilities().creativeMode) {
                            // For 1.20.1 - Determine EquipmentSlot based on the hand used.
                            stack.damage(1, player, (p) -> p.sendEquipmentBreakStatus(hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                        }
                    }
                    actionTaken = true;
                }

                if (actionTaken) {
                    return ActionResult.success(world.isClient);
                }
            }

            // --- Shoulder Mounting Logic ---
            boolean isUsingItem = ConfigDataCache.isLureItem(stack);

            // Only check item here. Force-Mount Keybind handled by client event + packet.
            if (ConfigDataCache.isLureItem(stack)) {
                if (!world.isClient) {
                    tryShoulderMount(player, stack);
                } else {
                    player.swingHand(hand);
                }
                return ActionResult.CONSUME; // Consume item interaction
            }

            // --- Inventory Access ---
            if (!world.isClient() && isSneaking) {
                // Check if pouch is unlocked OR if config disables the lock
                if (getHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG) || !AdorableHamsterPets.CONFIG.requireFoodMixToUnlockCheeks) {
                    // --- Use Architectury's openExtendedMenu with the factory ---
                    MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, new HamsterScreenHandlerFactory(this));
                } else {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.cheek_pouch_locked").formatted(Formatting.WHITE), true);
                }
                return ActionResult.CONSUME; // Consume sneak action regardless of opening
            }

            // --- Feeding Logic ---
            boolean isPotentialFood = ConfigDataCache.isStandardFood(stack) || ConfigDataCache.isBuffFood(stack) || ConfigDataCache.isPouchUnlockFood(stack);
            if (!world.isClient() && !isSneaking && isPotentialFood) {
                AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Owner not sneaking, holding potential food. Checking refusal.", this.getId(), world.getTime());
                if (checkRepeatFoodRefusal(stack, player)) {
                    AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Food refused. Consuming interaction.", this.getId(), world.getTime());
                    return ActionResult.CONSUME; // Consume refusal action
                }
                AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Attempting feeding via tryFeedingAsTamed.", this.getId(), world.getTime());
                boolean feedingOccurred = tryFeedingAsTamed(player, stack); // Calls the method with detailed logging
                if (feedingOccurred) {
                    AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] tryFeedingAsTamed returned true. Setting last food, decrementing stack.", this.getId(), world.getTime());
                    this.lastFoodItem = stack.copy(); // Track last food *only* if feeding was successful
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    return ActionResult.CONSUME; // Consume successful feeding action
                } else {
                    // If tryFeedingAsTamed returned false (e.g., cooldown, full health+no breed),
                    // We might still want to allow vanilla interaction or sitting.
                    // Let's PASS for now to allow super.interactMob to run.
                    AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] tryFeedingAsTamed returned false. Passing to vanilla/sitting.", this.getId(), world.getTime());
                }
            }

            // --- Vanilla Interaction Handling ---
            if (!isSneaking && !isPotentialFood && !ConfigDataCache.isLureItem(stack) && !stack.isOf(Items.PINK_PETALS)) {
                AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Not sneaking or holding handled food/petals. Calling super.interactMob.", this.getId(), world.getTime());
                ActionResult vanillaResult = super.interactMob(player, hand);
                AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] super.interactMob returned: {}", this.getId(), world.getTime(), vanillaResult);
                if (vanillaResult.isAccepted()) {
                    return vanillaResult;
                }
            }

            // --- Sitting Logic ---
            if (!world.isClient() && !isSneaking) {
                AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Fallback: Toggling sitting state.", this.getId(), world.getTime());

                // The setSitting method handles all animations and sounds.
                this.setSitting(!this.isSitting());

                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                return ActionResult.CONSUME_PARTIAL; // Indicate partial consumption for state toggle
            }
            // Client-side success or fallback pass for owner
            AdorableHamsterPets.LOGGER.debug("[InteractMob {} Tick {}] Reached end of owner logic. Returning client-side success/pass.", this.getId(), world.getTime());
            return ActionResult.success(world.isClient());

        } else {
            // Interaction by a non-owner on a tamed hamster. Let vanilla handle it.
            AdorableHamsterPets.LOGGER.debug("[InteractMob {} Tick {}] Player is not owner. Calling super.interactMob.", this.getId(), world.getTime());
            return super.interactMob(player, hand);
        }
    }

    // --- Taming Override ---
    /**
     * Overrides the vanilla setTamed method. This is the method called by vanilla logic.
     * It delegates to our custom implementation, ensuring attributes are always updated.
     * @param tamed True if the entity is being tamed.
     */
    @Override
    public void setTamed(boolean tamed) {
        // Always update attributes when this vanilla method is called.
        this.setTamed(tamed, true);
    }

    /**
     * Custom implementation of setTamed that allows controlling the attribute update.
     * In 1.20.1, this is now a helper method for the mod's internal use.
     * @param tamed True if the entity is being tamed.
     * @param updateAttributes True to update the entity's attributes (e.g., max health).
     */
    public void setTamed(boolean tamed, boolean updateAttributes) {
        super.setTamed(tamed); // Call the parent method
        if (updateAttributes) {
            if (tamed) {
                this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(Configs.AHP.tamedMaxHealth.get());
                this.setHealth(this.getMaxHealth());
                this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(Configs.AHP.meleeDamage.get());
            } else {
                this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(Configs.AHP.wildMaxHealth.get());
                this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(Configs.AHP.meleeDamage.get());
            }
        }
    }

    // --- Breeding ---
    public boolean isInCustomLove() { return this.customLoveTimer > 0; }
    public void setCustomInLove(PlayerEntity player) {
        this.customLoveTimer = CUSTOM_LOVE_TICKS;
        if (!this.getWorld().isClient) { this.getWorld().sendEntityStatus(this, (byte) 18); }
    }

    @Override
    public void setBaby(boolean baby) {
        this.setBreedingAge(baby ? -24000 : 0); // Vanilla logic for setting age based on baby status
    }


    // --- Method to Synchronize Custom Sitting DataTracker with Vanilla Pose ---
    /** This method is called by vanilla logic (like SitGoal) when the sitting pose changes.
     * We override it to ensure our custom IS_SITTING DataTracker, which drives animations,
     * stays synchronized with the entity's actual sitting pose state.
     */
    @Override
    public void setInSittingPose(boolean inSittingPose) {
        // --- 1. Call Superclass Method ---
        super.setInSittingPose(inSittingPose);


        // --- 2. Synchronize Custom Flag ---
        if (this.getHamsterFlag(SITTING_FLAG) != inSittingPose) {
            setHamsterFlag(SITTING_FLAG, inSittingPose);
        }

        // --- 3. Additional State Reset if Standing Up ---
        if (!inSittingPose) {
            if (this.isSleeping()) {
                this.setSleeping(false);
            }
            if (this.isKnockedOut()) {
                this.setKnockedOut(false);
            }
        }
    }

    // --- Hamster Breeding and Baby Variant Logic ---
    /**
     * Gets the HamsterVariant enum constant corresponding to this entity's current variant ID.
     * @return The HamsterVariant enum.
     */
    public HamsterVariant getVariantEnum() {
        return HamsterVariant.byId(this.getVariant());
    }

    /**
     * Creates a baby hamster, inheriting traits from its parents.
     * <p>
     * The baby's base color is randomly chosen from one of its parents. The overlay (white markings)
     * follows specific inheritance rules to promote diversity:
     * <ul>
     *     <li>If both parents have an overlay, the baby is guaranteed to have one. The system first
     *         tries to assign an overlay pattern that is different from both parents. If no different
     *         overlay is available for the baby's inherited base color, it will pick any available
     *         overlay for that color, potentially matching a parent's pattern.</li>
     *     <li>If only one or neither parent has an overlay, the baby has a chance to inherit any
     *         eligible overlay for its base color or to have no overlay at all (just the base color).</li>
     *     <li>The {@code WHITE} base color is a special case and never receives an overlay.</li>
     * </ul>
     * The baby inherits the owner of the parent instance that initiated the breeding.
     *
     * @param world The server world where the child will be created.
     * @param mate The other parent entity.
     * @return A new {@code HamsterEntity} instance representing the baby, or {@code null} if creation fails.
     */
    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) {
        HamsterEntity baby = ModEntities.HAMSTER.get().create(world);
        if (baby == null) return null;

        if (!(mate instanceof HamsterEntity mother)) {
            int randomVariantId = this.random.nextInt(HamsterVariant.values().length);
            baby.setVariant(randomVariantId);
            baby.setBaby(true);
            AdorableHamsterPets.LOGGER.warn("Hamster breeding attempted with non-hamster mate. Assigning random variant to baby.");
            return baby;
        }

        HamsterEntity father = this;
        HamsterVariant parentProvidingBaseColor = this.random.nextBoolean() ? father.getVariantEnum() : mother.getVariantEnum();
        HamsterVariant babyBaseColorEnum = parentProvidingBaseColor.getBaseVariant();

        @Nullable String fatherOverlayName = father.getVariantEnum().getOverlayTextureName();
        @Nullable String motherOverlayName = mother.getVariantEnum().getOverlayTextureName();

        List<HamsterVariant> allVariantsForBabyBase = HamsterVariant.getVariantsForBase(babyBaseColorEnum);

        // Build a list of overlay names that are NOT used by either parent.
        List<@Nullable String> eligibleOverlayNames = new ArrayList<>();
        for (HamsterVariant variant : allVariantsForBabyBase) {
            @Nullable String candidateOverlay = variant.getOverlayTextureName();
            boolean matchesFather = fatherOverlayName != null && fatherOverlayName.equals(candidateOverlay);
            boolean matchesMother = motherOverlayName != null && motherOverlayName.equals(candidateOverlay);
            if (!matchesFather && !matchesMother) {
                eligibleOverlayNames.add(candidateOverlay);
            }
        }

        List<@Nullable String> finalSelectableOverlayNames = new ArrayList<>();
        boolean fatherHasOverlay = fatherOverlayName != null;
        boolean motherHasOverlay = motherOverlayName != null;

        if (fatherHasOverlay && motherHasOverlay) {
            // Baby MUST have an overlay. Prioritize overlays different from parents.
            for (@Nullable String overlayName : eligibleOverlayNames) {
                if (overlayName != null) {
                    finalSelectableOverlayNames.add(overlayName);
                }
            }
            // If no different overlay is available, relax the rule and allow any overlay for that base color.
            if (finalSelectableOverlayNames.isEmpty() && babyBaseColorEnum != HamsterVariant.WHITE) {
                for (HamsterVariant variant : allVariantsForBabyBase) {
                    if (variant.getOverlayTextureName() != null) {
                        finalSelectableOverlayNames.add(variant.getOverlayTextureName());
                    }
                }
            }
        } else {
            // If one or neither parent has an overlay, the baby can have no overlay.
            finalSelectableOverlayNames.addAll(eligibleOverlayNames);
        }

        HamsterVariant babyFinalVariant;
        if (!finalSelectableOverlayNames.isEmpty()) {
            @Nullable String chosenOverlayName = finalSelectableOverlayNames.get(this.random.nextInt(finalSelectableOverlayNames.size()));
            babyFinalVariant = HamsterVariant.getVariantByBaseAndOverlay(babyBaseColorEnum, chosenOverlayName);
        } else {
            // Fallback case
            babyFinalVariant = babyBaseColorEnum;
        }

        baby.setVariant(babyFinalVariant.getId());

        UUID ownerUUID = father.getOwnerUuid();
        if (ownerUUID != null) {
            baby.setOwnerUuid(ownerUUID);
            baby.setTamed(true, true);
        }
        baby.setBaby(true);

        return baby;
    }

    /**
     * Checks if the given ItemStack can be used to initiate breeding.
     * This check is now driven by the user-configurable {@code standardFoods} list
     * via the {@link ConfigDataCache#isStandardFood(ItemStack)} helper method.
     *
     * @param stack The ItemStack to check.
     * @return {@code true} if the item is a valid breeding food.
     */
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return ConfigDataCache.isStandardFood(stack);
    }

    // --- Tick Logic ---
    @Override
    public void tick() {
        // --- Triggerable Animation Cancellation Scheduler ---
        if (!this.getWorld().isClient()) {
            long currentTime = this.getWorld().getTime();
            // Use removeIf for safe concurrent modification while iterating
            scheduledTasks.removeIf(task -> {
                if (currentTime >= task.executionTick()) {
                    task.action().run();
                    return true; // Remove the task
                }
                return false;
            });
        }

        // --- 1. Decrement Simple Timers ---
        if (this.interactionCooldown > 0) this.interactionCooldown--;
        if (this.suffocationGracePeriod > 0) this.suffocationGracePeriod--;
        if (this.wakingUpTicks > 0) this.wakingUpTicks--;
        if (this.autoEatCooldownTicks > 0) this.autoEatCooldownTicks--;
        if (this.autoEatProgressTicks > 0) this.autoEatProgressTicks--;
        if (this.ejectionCheckCooldown > 0) this.ejectionCheckCooldown--;
        if (this.preAutoEatDelayTicks > 0) this.preAutoEatDelayTicks--;
        if (this.celebrationParticleTicks > 0) this.celebrationParticleTicks--;
        if (this.celebrationParticleTicks > 0) this.celebrationParticleTicks--;
        if (this.diamondCelebrationSoundTicks > 0) this.diamondCelebrationSoundTicks--;
        if (this.sulkOrchestraHitDelayTicks > 0) this.sulkOrchestraHitDelayTicks--;
        if (this.sulkFailParticleTicks > 0) this.sulkFailParticleTicks--;
        if (this.sulkEntityEffectTicks > 0) this.sulkEntityEffectTicks--;
        if (this.sulkShockedSoundDelayTicks > 0) this.sulkShockedSoundDelayTicks--;
        if (this.diamondSparkleSoundDelayTicks > 0) this.diamondSparkleSoundDelayTicks--;
        if (this.goToBedCooldown > 0) this.goToBedCooldown--;
        if (this.lureToBedTimer > 0) this.lureToBedTimer--;
        if (this.wakeUpFromBedDelay > 0) this.wakeUpFromBedDelay--;
        if (this.napInBedDurationTimer > 0) this.napInBedDurationTimer--;

        // --- Settle "Thump" Sound Effect ---
        if (this.thumpSoundDelayTicks > 0) {
            this.thumpSoundDelayTicks--;
            if (this.thumpSoundDelayTicks == 0 && !this.getWorld().isClient()) {
                this.getWorld().playSound(null, this.getBlockPos(), ModSounds.HAMSTER_THUMP.get(), SoundCategory.NEUTRAL, this.thumpSoundVolume, 1.5f);
            }
        }

        // --- Bed Leaf Particle Effect ---
        if (this.bedLeafParticleTicks > 0) {
            if (!this.getWorld().isClient()) {
                int particleCount = 0;
                // Check for specific moments in the 4-tick duration
                if (this.bedLeafParticleTicks == 3) { // Second burst
                    particleCount = 15;
                } else if (this.bedLeafParticleTicks == 2) { // Third burst
                    particleCount = 10;
                }else if (this.bedLeafParticleTicks == 1) { // Fourth burst
                    particleCount = 5;
                }

                if (particleCount > 0 && this.getLinkedBedPos().isPresent()) {
                    BlockPos bedPos = this.getLinkedBedPos().get().getPos();
                    BlockState bedState = this.getWorld().getBlockState(bedPos);
                    ((ServerWorld)this.getWorld()).spawnParticles(ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                            bedPos.getX() + 0.5, bedPos.getY() + 0.3, bedPos.getZ() + 0.5,
                            particleCount, 0.2, 0.3, 0.2, 1);
                }
            }
            // Decrement the timer after processing the current tick's effect.
            this.bedLeafParticleTicks--;
        }

        // --- Cleaning Logic ---
        if (this.cleaningCooldownTimer > 0) this.cleaningCooldownTimer--;
        if (this.cleaningTimer > 0) {
            this.cleaningTimer--;
            if (this.cleaningTimer == 0) {
                if (!this.getWorld().isClient) {
                    setHamsterFlag(CLEANING_FLAG, false);
                }
                this.cleaningCooldownTimer = 200;
            }
        }
        if (this.isKnockedOut() && getHamsterFlag(CLEANING_FLAG)) {
            setHamsterFlag(CLEANING_FLAG, false);
            this.cleaningTimer = 0;
        }
        DozingPhase currentPhase = this.getDozingPhase();
        if (!this.getWorld().isClient() && this.isTamed() && this.isSitting() && !getHamsterFlag(CLEANING_FLAG) && this.cleaningCooldownTimer <= 0) {
            // Allow cleaning if the hamster is just sitting, but not if it's actively sleeping.
            if (currentPhase == DozingPhase.NONE || currentPhase == DozingPhase.QUIESCENT_SITTING) {
                int chanceDenominator = Configs.AHP.cleaningChanceDenominator.get();
                if (chanceDenominator > 0 && this.random.nextInt(chanceDenominator) == 0) {
                    this.cleaningTimer = this.random.nextBetween(30, 60);
                    setHamsterFlag(CLEANING_FLAG, true);
                }
            }
        }

        // --- Post-Chase Celebration Logic ---
        if (this.isCelebratingRetrieval()) {
            if (this.celebrationRetrievalTicks > 0) {
                if (this.getOwner() != null) {
                    this.getLookControl().lookAt(this.getOwner(), FAST_YAW_CHANGE, FAST_PITCH_CHANGE);
                }
                this.celebrationRetrievalTicks--;
            } else {
                this.setCelebratingRetrieval(false);
            }
        }

        // --- 2. Thrown State Logic ---
        if (this.isThrown()) {
            this.throwTicks++; // Increment throw timer

            Vec3d currentPos = this.getPos();
            Vec3d currentVel = this.getVelocity();
            Vec3d nextPos = currentPos.add(currentVel);
            World world = this.getWorld();

            HitResult blockHit = world.raycast(new RaycastContext(currentPos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));

            boolean stopped = false;

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) blockHit;
                BlockPos hitPos = blockHitResult.getBlockPos();

                // --- Tree Heist Trigger (Projectile) ---
                if (world.getBlockState(hitPos).isOf(Blocks.OAK_LEAVES)) {
                    if (!world.isClient()) {
                        // 1. Scan first to identify the tree anchor
                        TreeHeistUtil.TreeScanResult scanResult = TreeHeistUtil.scanForTree(world, hitPos);

                        // 2. Check occupancy
                        if (HamsterTreeSearcherEntity.isTreeBlocked(world, scanResult.treeId())) {
                            // Tree is busy
                            if (this.getOwner() instanceof PlayerEntity owner) {
                                owner.sendMessage(Text.translatable("message.adorablehamsterpets.tree_heist.occupied").formatted(Formatting.RED), true);
                            }
                            // Proceed to "Standard Block Collision Handling" below
                        } else {
                            // Tree is free. Start Heist.
                            triggerLeafPopEffects(hitPos, true);
                            HamsterTreeSearcherEntity searcher = ModEntities.HAMSTER_TREE_SEARCHER.get().create(world);
                            if (searcher != null) {
                                NbtCompound nbt = new NbtCompound();
                                this.writeNbt(nbt); // Use writeNbt to capture full entity state (Owner, Attributes, etc.)
                                // Pass the already-calculated scan result
                                searcher.initializeSearch(hitPos, scanResult, nbt);
                                world.spawnEntity(searcher);
                                this.discard();
                                return;
                            }
                        }
                    }
                }

                // --- Standard Block Collision Handling ---
                BlockPos adjacentPos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());

                // Place the hamster in the air next to the impacted block face.
                this.setPosition(adjacentPos.getX() + 0.5, adjacentPos.getY(), adjacentPos.getZ() + 0.5);

                // Apply the "tumble" state immediately. Vanilla gravity will handle the fall.
                this.setVelocity(currentVel.multiply(0.6, 0.0, 0.6));
                this.setThrown(false);

                // Play impact sound (Main + Armor if applicable) via custom packet logic
                broadcastImpactSound(SoundEvents.ENTITY_GENERIC_SMALL_FALL, 1.2f);

                this.setKnockedOut(true);
                this.setInSittingPose(true);
                if (!world.isClient()) {
                    this.triggerAnimOnServer("mainController", "crash");
                }
                stopped = true;

            } else {
                EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, this, currentPos, nextPos, this.getBoundingBox().stretch(currentVel).expand(1.0), this::canHitEntity);

                if (entityHit != null && entityHit.getEntity() != null) {
                    // --- 2b. Entity Collision Handling ---
                    Entity hitEntity = entityHit.getEntity();
                    BlockPos impactPos = hitEntity.getBlockPos();
                    boolean playEffects = false;

                    if (hitEntity instanceof ArmorStandEntity) {
                        playEffects = true;
                    } else if (hitEntity instanceof LivingEntity livingHit) {

                        // --- Throw Damage Logic ---
                        // 1. Create a DamageSource where the thrown hamster is the attacker.
                        DamageSource damageSource = this.getDamageSources().mobAttack(this);
                        // 2. Get the damage amount from the config.
                        float damageAmount = Configs.AHP.hamsterThrowDamage.get().floatValue();

                        // 3. Apply Netherite Armor Bonus
                        ItemStack armorStack = this.getArmorStack();
                        if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
                            // Check config boolean before applying bonus
                            if (Configs.AHP.enableArmorPerks.get() && armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.NETHERITE) {
                                damageAmount += Configs.AHP.netheriteArmorThrowDamageBonus.get().floatValue();
                            }
                        }

                        // 4. Deal the damage to the target using the correct source.
                        boolean damaged = livingHit.damage(damageSource, damageAmount);

                        if (damaged) {
                            // Apply damage
                            livingHit.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20, 0, false, false, false));
                            // Calculate knockback direction based on velocity
                            double knockbackStrength = 0.5;
                            double dx = currentVel.x;
                            double dz = currentVel.z;
                            // Apply knockback
                            livingHit.takeKnockback(knockbackStrength, -dx, -dz);
                            playEffects = true;
                        }
                    } else {
                        playEffects = true;
                    }

                    if (playEffects) {
                        // Play impact sound (Main + Armor if applicable) via custom packet logic
                        broadcastImpactSound(ModSounds.HAMSTER_IMPACT.get(), 1.0f);

                        // Spawn particles
                        if (!world.isClient()) {
                            ((ServerWorld)world).spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + this.getHeight() / 2.0, this.getZ(), 50, 0.4, 0.4, 0.4, 0.1);
                        }
                    }

                    // Find safe spot near the hit entity
                    Optional<BlockPos> safePosOpt = findSafeSpawnPosition(impactPos, world, 2);
                    safePosOpt.ifPresentOrElse(
                            safePos -> this.setPosition(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5),
                            () -> {
                                AdorableHamsterPets.LOGGER.warn("[HamsterThrow] Could not find safe landing spot after hitting entity. Using entity's position {} as fallback.", impactPos);
                                this.setPosition(impactPos.getX() + 0.5, impactPos.getY(), impactPos.getZ() + 0.5);
                            }
                    );

                    this.setVelocity(currentVel.multiply(0.1, 0.1, 0.1));
                    this.setThrown(false);
                    this.setKnockedOut(true);
                    this.setInSittingPose(true);
                    if (!world.isClient()) {
                        this.triggerAnimOnServer("mainController", "crash");
                    }
                    stopped = true;
                }
            }

            // Apply gravity, update position, simulate trajectory audio, and spawn trail particles if still thrown
            if (this.isThrown() && !stopped) {
                if (!this.getWorld().isClient() && !this.hasPlayedIncomingSound) {
                    simulateTrajectoryAndCheckSound();
                }

                if (!this.hasNoGravity()) {
                    this.setVelocity(this.getVelocity().add(0.0, THROWN_GRAVITY, 0.0));
                }

                Vec3d currentVelocity = this.getVelocity();
                if (Double.isNaN(currentVelocity.x) || Double.isNaN(currentVelocity.y) || Double.isNaN(currentVelocity.z)) {
                    this.setVelocity(Vec3d.ZERO);
                    this.setThrown(false);
                    AdorableHamsterPets.LOGGER.warn("Hamster velocity became NaN, resetting and stopping throw.");
                } else {
                    this.setPosition(this.getX() + currentVelocity.x, this.getY() + currentVelocity.y, this.getZ() + currentVelocity.z);
                    this.velocityDirty = true;

                    // Determine the delay before particles start spawning.
                    int particleDelay = this.hasGreenBeanBuff() ? 3 : 5;

                    if (!world.isClient() && this.throwTicks > particleDelay) {
                        // Define an offset to push the particle spawn point backwards along the velocity vector. Larger value pushes it back more.
                        double offsetMultiplier = 1.5;

                        // Calculate the spawn position based on the PREVIOUS position, offset backwards.
                        double spawnX = this.prevX - (currentVelocity.x * offsetMultiplier);
                        double spawnY = this.prevY + (this.getHeight() / 2.0) - (currentVelocity.y * offsetMultiplier);
                        double spawnZ = this.prevZ - (currentVelocity.z * offsetMultiplier);

                        ((ServerWorld)world).spawnParticles(
                                ParticleTypes.CLOUD,
                                spawnX, spawnY, spawnZ,
                                1, 0.1, 0.1, 0.1, 0.0
                        );
                    }
                }
            } else {
                if (this.throwTicks != 0) {
                    this.throwTicks = 0;
                }
            }
        }

        // --- 3. Tamed Hamster "Path to Slumber" State Machine ---
        // This logic only applies to tamed hamsters and runs on the server.
        if (!this.getWorld().isClient() && this.isTamed() && !this.isKnockedOut()) {
            boolean canInitiateDrowsiness = checkConditionsForInitiatingDrowsiness(); // Helper method call
            boolean canSustainSlumber = checkConditionsForSustainingSlumber();       // Helper method call

            switch (currentPhase) {
                case NONE:
                    // If commanded to sit and conditions are right, start Phase 1
                    if (this.isSitting() && canInitiateDrowsiness) {
                        // Check if quiescentSitDurationTimer is 0, meaning we can start a new cycle
                        if (this.quiescentSitDurationTimer == 0) {
                            this.setDozingPhase(DozingPhase.QUIESCENT_SITTING);

                            // Calculate random duration based on config
                            int minSeconds = Configs.AHP.tamedQuiescentSitMinSeconds.get();
                            int maxSeconds = Configs.AHP.tamedQuiescentSitMaxSeconds.get();

                            // Safety rail: ensure min is not greater than max
                            if (minSeconds > maxSeconds) {
                                AdorableHamsterPets.LOGGER.info("Config issue: tamedQuiescentSitMinSeconds ({}) > tamedQuiescentSitMaxSeconds ({}). Swapping.", minSeconds, maxSeconds);
                                int temp = minSeconds;
                                minSeconds = maxSeconds;
                                maxSeconds = temp;
                            }
                            // Safety rail: ensure max is not less than min after potential swap
                            if (maxSeconds < minSeconds) maxSeconds = minSeconds;

                            int durationTicks = this.random.nextBetween(minSeconds * 20, maxSeconds * 20 + 1);
                            this.quiescentSitDurationTimer = durationTicks;
                            AdorableHamsterPets.LOGGER.debug("Hamster {} entering QUIESCENT_SITTING for {} ticks.", this.getId(), durationTicks);
                        }
                    }
                    break;

                case QUIESCENT_SITTING:
                    if (!this.isSitting() || !canInitiateDrowsiness) {
                        // Interrupted (stood up, conditions changed, etc.)
                        resetSleepSequence("Quiescent sitting interrupted: no longer sitting or conditions unfavorable.");
                        break;
                    }
                    if (this.quiescentSitDurationTimer > 0) {
                        this.quiescentSitDurationTimer--;
                    } else {
                        // Timer expired, attempt to move to Drifting Off
                        this.setDozingPhase(DozingPhase.DRIFTING_OFF);
                        this.driftingOffTimer = 90 * 20; // 90 seconds for the animation
                        // Animation controller will pick up anim_hamster_drifting_off
                        AdorableHamsterPets.LOGGER.debug("Hamster {} entering DRIFTING_OFF for {} ticks.", this.getId(), this.driftingOffTimer);
                    }
                    break;

                case DRIFTING_OFF:
                    if (!canSustainSlumber) { // Check sustain conditions
                        resetSleepSequence("Drifting off interrupted: conditions for slumber no longer met.");
                        break;
                    }
                    if (this.driftingOffTimer > 0) {
                        this.driftingOffTimer--;
                    } else {
                        // Drifting off animation completed
                        this.setDozingPhase(DozingPhase.SETTLING_INTO_SLUMBER);
                        // Randomly select a settle animation and corresponding deep sleep pose
                        int choice = this.random.nextInt(3);
                        String settleAnimId;
                        String deepSleepAnimIdForTracker = switch (choice) {
                            case 0 -> {
                                settleAnimId = "anim_hamster_settle_sleep1";
                                yield "anim_hamster_sleep_pose1";
                            }
                            case 1 -> {
                                settleAnimId = "anim_hamster_settle_sleep2";
                                yield "anim_hamster_sleep_pose2";
                            }
                            default -> {
                                settleAnimId = "anim_hamster_settle_sleep3";
                                yield "anim_hamster_sleep_pose3";
                            }
                        }; // Temporary variable for clarity
                        this.dataTracker.set(CURRENT_DEEP_SLEEP_ANIM_ID, deepSleepAnimIdForTracker); // Set DataTracker
                        this.triggerAnimOnServer("mainController", settleAnimId);
                        this.settleSleepAnimationCooldown = 20;

                        // Trigger "swish" and set "thump" sound effect timer
                        triggerSettleEffects(0.22f, 5, 0.24f);

                        AdorableHamsterPets.LOGGER.debug("Hamster {} entering SETTLING_INTO_SLUMBER, triggering {}, target deep sleep anim ID: {}.", this.getId(), settleAnimId, deepSleepAnimIdForTracker);
                    }
                    break;

                case SETTLING_INTO_SLUMBER:
                    if (!canSustainSlumber) {
                        resetSleepSequence("Settling into slumber interrupted: conditions for slumber no longer met.");
                        break;
                    }
                    if (this.settleSleepAnimationCooldown > 0) {
                        this.settleSleepAnimationCooldown--;
                    } else {
                        // Settle animation finished, transition to deep sleep
                        this.setDozingPhase(DozingPhase.DEEP_SLEEP);
                        // Animation controller will now loop currentDeepSleepAnimationId
                        AdorableHamsterPets.LOGGER.debug("Hamster {} entering DEEP_SLEEP, playing {}.", this.getId(), this.dataTracker.get(CURRENT_DEEP_SLEEP_ANIM_ID));
                    }
                    break;

                case DEEP_SLEEP:
                    if (!canSustainSlumber) {
                        triggerWakeUpFromSleepAnimation(false); // Trigger natural wakeup animation and sound
                        resetSleepSequence("Deep sleep interrupted: conditions for slumber no longer met.");
                    }
                    // Hamster remains in deep sleep, looping animation, until interrupted
                    break;
            }
        }

        // Call super.tick() *after* processing thrown state and timers
        super.tick();

        // --- Check for Armor Changes & Update Attributes ---
        if (!this.getWorld().isClient) {
            ItemStack currentArmor = this.getArmorStack();
            if (!ItemStack.areEqual(currentArmor, this.lastArmorStack)) {
                this.updateArmorModifiers(currentArmor);
                this.lastArmorStack = currentArmor.copy();
            }
        }

        // --- Dynamic Navigation Swapping & Periodic Config Sync ---
        if (!this.getWorld().isClient() && this.age % 20 == 0) { // Check once per second
            this.updateNavigation();

            // Periodically validate armor attributes to catch Config changes
            this.updateArmorModifiers(this.getArmorStack());
        }

        // --- Apply extra gravity during sulking jump ---
        // This runs on the server to ensure physics are authoritative.
        if (!this.getWorld().isClient()) {
            // If the hamster is sulking, not on the ground, and is currently falling (negative Y velocity)
            if (this.isSulking() && !this.isOnGround() && this.getVelocity().y < 0) {
                // Apply an extra downward force to make it fall faster.
                // -0.08 is the standard gravity value, so adding it again effectively doubles it.
                this.setVelocity(this.getVelocity().add(0.0, -1.0, 0.0));
                this.velocityDirty = true; // Ensure client sees the change
            }
        }

        // --- 4. Server-Side Logic ---
        World world = this.getWorld();
        if (!world.isClient()) {

            // --- Process Deferred Armor Breakage ---
            // Prevents "Equipment Update" packet from colliding with the "Hurt" packet
            if (this.performDeferredArmorUpdate) {
                this.setArmorStack(ItemStack.EMPTY);
                this.performDeferredArmorUpdate = false;
            }

            // --- Circadian Chaos Wake-Up Logic ---
            if (Configs.AHP.circadianChaos.get() &&
                    this.isSleeping() &&
                    this.getLinkedBedPos().isPresent() &&
                    this.napInBedDurationTimer == 0)
            {
                wakeUpFromBed(false); // Natural wake-up
            }

            // --- Day/Night Cycle Wake-Up Logic ---
            if (!Configs.AHP.circadianChaos.get() && this.isSleeping() && this.getLinkedBedPos().isPresent()) {
                boolean isSleepTime = Configs.AHP.sleepDuringDay.get() ? world.isDay() : world.isNight();
                if (!isSleepTime) {
                    // If it's wake-up time, and delay timer has not yet been started
                    if (this.wakeUpFromBedDelay == 0 && this.goToBedCooldown == 0) {
                        this.wakeUpFromBedDelay = this.random.nextBetween(5, 60); // Set the random 0.25s to 3s delay
                    }
                } else {
                    // If time flips back to sleep time while the timer is counting down, cancel the wake-up.
                    this.wakeUpFromBedDelay = 0;
                }
            }
            // Check if the wake-up timer has just expired
            if (this.wakeUpFromBedDelay == 1) {
                this.wakeUpFromBed(false); // Natural wake-up
            }

            // --- 4a. Suffocation Self-Rescue Logic ---
            if (this.suffocationGracePeriod > 0 && this.isInsideWall()) {
                // Search for a safe spot directly above the hamster
                for (int i = 1; i <= 5; i++) {
                    BlockPos checkPos = this.getBlockPos().up(i);
                    if (isSafeSpawnLocation(checkPos, world)) {
                        // Found a safe spot, teleport the hamster
                        this.teleport(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5, false);
                        AdorableHamsterPets.LOGGER.debug("[HamsterSelfRescue] Hamster {} teleported from {} to safe location {}.",
                                this.getId(), this.getBlockPos().down(i), checkPos);
                        this.suffocationGracePeriod = 0; // End the grace period
                        break; // Stop searching
                    }
                }
            }

            // --- 4b. Ejection Logic ---
            if (this.ejectionCheckCooldown <= 0) {
                this.ejectionCheckCooldown = 100; // Reset cooldown (check every 5 seconds)
                boolean inventoryChanged = false; // Track if needing to sync changes

                for (int i = 0; i < this.items.size(); ++i) {
                    ItemStack stack = this.items.get(i);
                    // Check !isValid instead of isItemDisallowed.
                    // isValid handles slot-specific rules (like allowing BlockItems in the Bling slot).
                    if (!stack.isEmpty() && !this.isValid(i, stack)) {
                        AdorableHamsterPets.LOGGER.warn("[HamsterTick {}] Ejecting invalid item {} from slot {}.", this.getId(), stack.getItem(), i);

                        // Drop the item at the hamster's feet
                        ItemScatterer.spawn(world, this.getX(), this.getY(), this.getZ(), stack.copy());

                        // Remove it from the inventory
                        this.items.set(i, ItemStack.EMPTY);

                        inventoryChanged = true;
                    }
                }

                // Update trackers and sync once if any items were ejected
                if (inventoryChanged) {
                    this.markDirty();
                }
            }

            // --- 4c. Auto Eating Logic ---
            // This section now handles the multi-stage auto-eating: considering, eating, healing.
            // --- Stage 1: Check Eligibility and Start "Considering" ---
            if (this.isTamed() && this.getHealth() < this.getMaxHealth() &&
                    !this.isAutoEating() && !this.isConsideringAutoEat() && // Not already eating or considering
                    this.autoEatCooldownTicks == 0 &&
                    !this.isThrown() && !this.isKnockedOut())
            {
                // Check inventory for eligible food
                for (int i = 0; i < this.items.size(); ++i) {
                    ItemStack stack = this.items.get(i);
                    if (!stack.isEmpty() && ConfigDataCache.isAutoHealFood(stack)) {
                        // Found food, start "considering" phase
                        setHamsterFlag(CONSIDERING_AUTO_EAT_FLAG, true);
                        this.preAutoEatDelayTicks = 40; // 2-second delay
                        AdorableHamsterPets.LOGGER.trace("[HamsterTick {}] Eligible to auto-eat. Starting 2s pre-eat delay.", this.getId());
                        break; // Stop searching for food once consideration starts
                    }
                }
            }

            // --- Stage 2: Process "Considering" Delay & Start Actual Eating ---
            if (this.isConsideringAutoEat() && this.preAutoEatDelayTicks == 0) {
                setHamsterFlag(CONSIDERING_AUTO_EAT_FLAG, false); // No longer just considering

                // Re-check for food in case it was removed during the delay
                boolean foodStillAvailable = false;
                ItemStack foodToEat = ItemStack.EMPTY;
                int foodSlot = -1;

                for (int i = 0; i < this.items.size(); ++i) {
                    ItemStack stack = this.items.get(i);
                    if (!stack.isEmpty() && ConfigDataCache.isAutoHealFood(stack)) {
                        foodStillAvailable = true;
                        foodToEat = stack;
                        foodSlot = i;
                        break;
                    }
                }

                if (foodStillAvailable) {
                    AdorableHamsterPets.LOGGER.trace("[HamsterTick {}] Pre-eat delay finished. Starting auto-eat on {} from slot {}", this.getId(), foodToEat.getItem(), foodSlot);
                    this.isAutoEating = true; // Use the boolean flag for the eating animation state
                    this.autoEatProgressTicks = 60; // 3 seconds eating time

                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.7F, 1.3F);
                    if (world instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                                new ItemStackParticleEffect(ParticleTypes.ITEM, foodToEat.split(1)), // Consume one for particles
                                this.getX() + this.random.nextGaussian() * 0.1,
                                this.getY() + this.getHeight() / 2.0 + this.random.nextGaussian() * 0.1,
                                this.getZ() + this.random.nextGaussian() * 0.1,
                                5, 0.1, 0.1, 0.1, 0.02
                        );
                    }
                    if (foodToEat.isEmpty()) { // If split made it empty
                        this.items.set(foodSlot, ItemStack.EMPTY);
                    }
                    this.updateCheekTrackers();
                } else {
                    AdorableHamsterPets.LOGGER.trace("[HamsterTick {}] Pre-eat delay finished, but food no longer available.", this.getId());
                    // No food, so don't proceed to eating state. Cooldowns remain 0.
                }
            }

            // --- Stage 3: Apply Healing After Eating Progress Finishes ---
            if (this.isAutoEating() && this.autoEatProgressTicks == 0) {
                this.heal(Configs.AHP.hamsterFoodMixHealing.get());
                this.autoEatCooldownTicks = 60; // Set main cooldown (3 seconds)
                this.isAutoEating = false; // Reset eating animation flag
                AdorableHamsterPets.LOGGER.trace("[HamsterTick {}] Auto-eat finished. Healed. Cooldown set to 60.", this.getId());

                if (this.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
                    ModCriteria.HAMSTER_AUTO_FED.trigger(serverPlayerOwner, this);
                }
            }
            // --- End 4b. Auto Eating Logic ---

            // --- 4c. Handle Continuous Diamond Celebration Effects ---
            if (!this.getWorld().isClient()) {
                if (this.isCelebratingDiamond()) {
                    // Delayed Diamond Sparkle Sound
                    if (this.diamondSparkleSoundDelayTicks == 1) { // Play when delay reaches 1
                        SoundEvent sparkleSound = ModSounds.getRandomSoundFrom(ModSounds.DIAMOND_SPARKLE_SOUNDS, this.random);
                        if (sparkleSound != null) {
                            // Play sound at the ORE'S location
                            if (this.currentOreTarget != null) {
                                this.getWorld().playSound(null, this.currentOreTarget, sparkleSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                            } else { // Fallback to hamster pos if ore target is somehow null
                                this.getWorld().playSound(null, this.getBlockPos(), sparkleSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                            }
                        }
                    }

                    // Particle Spawning
                    if (this.celebrationParticleTicks > 0) {
                            ((ServerWorld)this.getWorld()).spawnParticles(
                                    ParticleTypes.COMPOSTER,        // 1. Particle Type
                                    this.getX(),                    // 2. Center X-coordinate
                                    this.getY() + 1.8,              // 3. Center Y-coordinate
                                    this.getZ(),                    // 4. Center Z-coordinate
                                    2,                              // 5. Count
                                    0.12,                           // 6. Delta X (Spread X)
                                    0.25,                           // 7. Delta Y (Spread Y)
                                    0.12,                           // 8. Delta Z (Spread Z)
                                    0.15                            // 9. Speed
                            );

                        if (this.currentOreTarget != null && this.random.nextInt(4) == 0) {
                            BlockPos particlePos = this.currentOreTarget.up(); // Spawn above the diamond ore
                            ((ServerWorld)this.getWorld()).spawnParticles(
                                    ParticleTypes.FIREWORK,         // 1. Particle Type
                                    particlePos.getX() + 0.5,       // 2. Center X-coordinate
                                    particlePos.getY() + 0.5,       // 3. Center Y-coordinate
                                    particlePos.getZ() + 0.5,       // 4. Center Z-coordinate
                                    1,                              // 5. Count
                                    0.2,                            // 6. Delta X (Spread X)
                                    0.35,                           // 7. Delta Y (Spread Y)
                                    0.2,                            // 8. Delta Z (Spread Z)
                                    0.003                           // 9. Speed
                            );
                        }
                    }

                    //  Begging Sounds
                    if (this.diamondCelebrationSoundTicks <= 0) {
                        SoundEvent celebrationSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BEG_SOUNDS, this.random);
                        if (celebrationSound != null) {
                            this.getWorld().playSound(null, this.getBlockPos(), celebrationSound, SoundCategory.NEUTRAL, 0.8F, this.getSoundPitch());
                        }
                        this.diamondCelebrationSoundTicks = 30;
                    }
                }
            }

            // --- 4d. Handle Continuous Sulking Effects ---
            if (this.isSulking()) {
                // Delayed Orchestra Hit
                if (this.sulkOrchestraHitDelayTicks == 1) { // Play when delay reaches 1 (was 10, now 1 after 9 ticks)
                    this.getWorld().playSound(null, this.getBlockPos(), ModSounds.ALARM_ORCHESTRA_HIT.get(), SoundCategory.NEUTRAL, 1.0F, 1.0F);
                }

                // Delayed Single Shocked Sound
                if (this.sulkShockedSoundDelayTicks == 1) { // Play when this timer reaches 1
                    this.getWorld().playSound(null, this.getBlockPos(), ModSounds.HAMSTER_SHOCKED.get(), SoundCategory.NEUTRAL, 1.0F, 1.0F);
                }


                // Angry Smoke Particles above Gold Ore
                if (this.sulkFailParticleTicks > 0 && this.currentOreTarget != null) {
                    if (this.random.nextInt(3) == 0) {
                        BlockPos particlePos = this.currentOreTarget.up();
                        ((ServerWorld)this.getWorld()).spawnParticles(
                                ParticleTypes.SMOKE,          // 1. Particle Type
                                particlePos.getX() + 0.5,     // 2. Center X-coordinate
                                particlePos.getY() + 0.5,     // 3. Center Y-coordinate
                                particlePos.getZ() + 0.5,     // 4. Center Z-coordinate
                                2,                            // 5. Count
                                0.3,                          // 6. Delta X (Spread X)
                                0.3,                          // 7. Delta Y (Spread Y)
                                0.3,                          // 8. Delta Z (Spread Z)
                                0.005                         // 9. Speed
                        );
                    }
                }

                // Black Entity Effect Particles on Hamster
                if (this.sulkEntityEffectTicks > 0) {
                    if (this.random.nextInt(5) == 0) { // Spawn periodically
                        // In 1.20.1, colored ENTITY_EFFECT particles are spawned by setting count to 0
                        // and using the delta parameters for RGB color.
                        ((ServerWorld)this.getWorld()).spawnParticles(
                                ParticleTypes.ENTITY_EFFECT,             // The particle type
                                this.getParticleX(0.3),        // Center X (preserves main spread)
                                this.getRandomBodyY(),                   // Center Y (preserves main spread)
                                this.getParticleZ(0.3),        // Center Z (preserves main spread)
                                0,                                       // Count = 0 enables color mode
                                0.3,                                     // Red component (0.0 to 1.0)
                                0.3,                                     // Green component
                                0.3,                                     // Blue component
                                1.0                                      // Speed parameter is used for brightness/intensity
                        );
                    }
                }
            }
        }

        // --- 5. Client-Side Logic ---
        // --- 5.1 Buff Particle Logic ---
        if (world.isClient && this.hasGreenBeanBuff()) {
            // Only spawn particles if the hamster is actually moving.
            if (this.getVelocity().horizontalLengthSquared() > 1.0E-6) {
                // --- Constants for Particle Physics ---
                final double backwardsSpeed = 1.7;
                final double scatterStrength = 0.025;
                final double downwardVelocity = 0.17;
                final double positionOffsetMultiplier = 1.4;

                // Spawn particles frequently, but not every single tick, to avoid being overwhelming.
                if (this.random.nextInt(2) == 0) {
                    for (int i = 0; i < 3; ++i) {
                        // 1. Calculate the base spawn position using the hamster's PREVIOUS tick's location.
                        Vec3d currentVelocity = this.getVelocity();
                        double baseX = this.prevX - (currentVelocity.x * positionOffsetMultiplier);
                        double baseY = this.prevY + (this.getHeight() / 2.0) - (currentVelocity.y * positionOffsetMultiplier);
                        double baseZ = this.prevZ - (currentVelocity.z * positionOffsetMultiplier);

                        // 2. Apply the random spread to the base position.
                        // This maintains spread relative to the calculated "previous" point.
                        double spawnX = baseX + (this.random.nextDouble() - 0.5) * (this.getWidth() * 0.8);
                        double spawnY = baseY + (this.random.nextDouble() - 0.5) * (this.getHeight() * 0.05);
                        double spawnZ = baseZ + (this.random.nextDouble() - 0.5) * (this.getWidth() * 0.8);

                        // 3. Calculate the particle's velocity for the "zoomies" effect.
                        Vec3d hamsterMovementVec = this.getVelocity();
                        Vec3d backwardsBaseVel = hamsterMovementVec.multiply(-1.0 * backwardsSpeed);
                        double finalVelX = backwardsBaseVel.x + (this.random.nextGaussian() * scatterStrength);
                        double finalVelY = backwardsBaseVel.y + (this.random.nextGaussian() * scatterStrength) - downwardVelocity;
                        double finalVelZ = backwardsBaseVel.z + (this.random.nextGaussian() * scatterStrength);

                        // 4. Add the particle to the world with the calculated position and velocity.
                        world.addParticle(ParticleTypes.CLOUD, spawnX, spawnY, spawnZ, finalVelX, finalVelY, finalVelZ);
                    }
                }
            }
        }

        // --- 5.2 Taunting Particle Logic ---
        if (this.isTauntingWithItem()) {
            // Only spawn particles occasionally
            if (this.random.nextInt(7) == 0) { // Spawn roughly 2.86 times per second
                // Spawn energetic "instant effect" particles randomly around the hamster
                for (int i = 0; i < 2; ++i) { // Spawn three particles each time for a noticeable effect
                    world.addParticle(ParticleTypes.INSTANT_EFFECT,
                            this.getParticleX(0.6), // Spawn on the body
                            this.getRandomBodyY(),
                            this.getParticleZ(0.6),
                            (this.random.nextDouble() - 0.5) * 0.5, // dx (energetic outward motion)
                            (this.random.nextDouble() - 0.5) * 0.5, // dy
                            (this.random.nextDouble() - 0.5) * 0.5  // dz
                    );
                }
            }
        }

        // --- 5.3 Fall Pitch Interpolation Logic ---
        if (world.isClient) {
            // Capture state for interpolation before modification
            this.prevClientFallPitchProgress = this.clientFallPitchProgress;

            // Determine whether to pitch down
            // Thrown hamsters handle their own pitch in the Model based on velocity
            if (this.shouldRenderFlying() && !this.isThrown()) {
                // Ease in pitch for natural falls
                this.clientFallPitchProgress += 1.0f / NORMAL_FALL_PITCH_DURATION;
            } else {
                // Reset faster
                this.clientFallPitchProgress -= 1.0f / PITCH_RESET_DURATION;
            }

            // Clamp between 0.0 and 1.0
            this.clientFallPitchProgress = MathHelper.clamp(this.clientFallPitchProgress, 0.0f, 1.0f);
        }

        // --- 6. Other Non-Movement Tick Logic ---
        if (this.isRefusingFood() && refuseTimer > 0) { if (--refuseTimer <= 0) this.setRefusingFood(false); }
        if (tamingCooldown > 0) tamingCooldown--;
        if (customLoveTimer > 0) customLoveTimer--;
        if (customLoveTimer <= 0 && this.isInLove()) this.setInLove(false);
    }

    @Override
    public void onDeath(DamageSource source) {
        // --- Respawn in Bed Logic ---
        if (!this.getWorld().isClient() && Configs.AHP.enableRespawnInBed.get()) {
            boolean respawnSuccessful = tryRespawnInBed();

            // If respawn worked, return immediately
            if (respawnSuccessful) {
                this.discard();
                return;
            }
        }

        // --- Standard Death Logic (Drops & XP) ---
        World world = this.getWorld();
        if (!world.isClient()) {
            // Check if wild loot drops are disabled
            if (!this.isTamed() && Configs.AHP.disableWildLootDrops) {
                // If disabled and untamed, clear the inventory so nothing drops
                this.items.clear();
            }

            // Iterate through the items list and drop each non-empty stack
            for (ItemStack stack : this.items) {
                if (!stack.isEmpty()) {
                    // Use ItemScatterer to drop the stack at the hamster's position
                    ItemScatterer.spawn(world, this.getX(), this.getY(), this.getZ(), stack);
                }
            }
            this.items.clear();
            this.updateCheekTrackers();
        }

        // Call the superclass method after dropping items
        super.onDeath(source);
    }

    // --- Animation ---
    /**
     * Registers the animation controllers for the HamsterEntity.
     * This method defines the main animation state machine, prioritizing states like
     * knocked out, thrown, and the detailed "Path to Slumber" sequence for tamed hamsters.
     * It also handles animations for wild hamster sleep, player-commanded sitting (including cleaning),
     * movement, begging, and defaults to an idle animation.
     *
     * <p>The "Path to Slumber" for tamed hamsters involves several phases:
     * <ul>
     *     <li>{@link DozingPhase#DRIFTING_OFF}: Plays {@code anim_hamster_drifting_off}. Its completion is
     *         managed by {@code driftingOffTimer} in the {@link #tick()} method.</li>
     *     <li>{@link DozingPhase#SETTLING_INTO_SLUMBER}: A short, 1-second {@code anim_hamster_sit_settle_sleepX}
     *         animation is triggered from {@link #tick()}. During this brief transition, this controller
     *         defaults to {@code SITTING_ANIM}. The {@code settleSleepAnimationCooldown} in {@link #tick()}
     *         manages the progression to {@code DEEP_SLEEP}.</li>
     *     <li>{@link DozingPhase#DEEP_SLEEP}: Loops the chosen {@code anim_hamster_sleep_poseX} (e.g.,
     *         {@code SLEEP_POSE1_ANIM}), determined by {@code currentDeepSleepAnimationId}.</li>
     * </ul>
     * Wild hamsters use a simpler sleep mechanism: {@code anim_hamster_wild_settle_sleep} is triggered,
     * followed by looping {@code SLEEP_POSE1_ANIM} if {@link #isSleeping()} is true.
     * </p>
     *
     * <p>Several animations like attack, crash, wakeup_from_ko, and the settle animations are registered
     * as triggerable and will interrupt the main looping state when fired via {@link #triggerAnimOnServer}.</p>
     *
     * @param controllers The registrar for adding animation controllers.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mainController", 3, event -> {

            // --- Initial Setup ---
            DozingPhase currentDozingPhase = this.getDozingPhase();
            int personality = this.dataTracker.get(ANIMATION_PERSONALITY_ID);

            // --- Animation Logic for Shoulder-Mounted Hamsters ---
            if (this.isShoulderPet()) {
                ShoulderAnimationState shoulderState = ShoulderAnimationState.values()[this.dataTracker.get(SHOULDER_ANIMATION_STATE)];
                return switch (shoulderState) {
                    case SITTING -> {
                        if (getHamsterFlag(CLEANING_FLAG)) {
                            yield event.setAndContinue(CLEANING_ANIM);
                        }
                        yield event.setAndContinue(switch (personality) {
                            case 2 -> SITTING_POSE2_ANIM;
                            case 3 -> SITTING_POSE3_ANIM;
                            default -> SITTING_POSE1_ANIM;
                        });
                    }
                    case LAYING_DOWN -> // Location-Specific Logic
                            switch (this.shoulderLocation) {
                                case LEFT_SHOULDER -> event.setAndContinue(LAYING_DOWN_LEFT_SHOULDER_ANIM);
                                case HEAD -> event.setAndContinue(LAYING_DOWN_HEAD_ANIM);
                                default -> event.setAndContinue(LAYING_DOWN_RIGHT_SHOULDER_ANIM);
                            };

                    // Use deterministic selection based on personality instead of random to prevent a
                    // "splitting" glitch on Oculus/Embedium with rapid flickering between IDLE1 and IDLE2.
                    default -> // STANDING
                            event.setAndContinue((personality % 2 == 0) ? IDLE2_ANIM : IDLE1_ANIM);
                };
            }

            // --- Animation Logic for In-World Hamsters ---
            // --- Knocked Out State ---
            if (this.isKnockedOut()) {return event.setAndContinue(KNOCKED_OUT_ANIM);}
            // --- Sulking State ---
            if (this.isSulking()) {return event.setAndContinue(SULKING_ANIM);}
            // --- Flying/Falling/Thrown State ---
            if (this.shouldRenderFlying()) {
                return event.setAndContinue(FLYING_ANIM);
            }
            // --- Taunting State ---
            if (this.isTauntingWithItem()) {return event.setAndContinue(TAUNT_WITH_ITEM_ANIM);}
            // --- Item Retrieval State ---
            if (this.isPresentingItem()) {return event.setAndContinue(PRESENTING_ITEM_ANIM);}
            // --- Seeking/Wanting to Seek Diamond/Ore State ---
            boolean isSeekingGoalActive = false;
            String activeGoalName = this.getActiveCustomGoalDebugName();
            if (activeGoalName.startsWith(HamsterSeekDiamondGoal.class.getSimpleName())) {
                isSeekingGoalActive = true;
            }
            if (isSeekingGoalActive) {
                double horizontalSpeedSquared = this.getVelocity().horizontalLengthSquared();
                if (horizontalSpeedSquared > 1.0E-6) { // Use a very small threshold to detect any movement
                    return event.setAndContinue(SEEKING_ORE_ANIM); // Hamster is moving
                } else {
                    return event.setAndContinue(WANTS_TO_SEEK_ORE_ANIM); // Hamster is not moving
                }
            }

            // --- Found Diamond Celebration ---
            if (this.isCelebratingDiamond()) {
                return event.setAndContinue(BEGGING_ANIM); // Reuse begging animation for celebration
            }

            // --- Sleeping States ---
            // 1. Tamed Sleep Sequence
            if (this.isTamed()) {
                switch (currentDozingPhase) {
                    case DRIFTING_OFF:
                        return event.setAndContinue(switch (personality) {
                            case 2 -> DRIFTING_OFF_POSE2_ANIM;
                            case 3 -> DRIFTING_OFF_POSE3_ANIM;
                            default -> DRIFTING_OFF_POSE1_ANIM;
                        });

                    case SETTLING_INTO_SLUMBER:
                        String targetDeepSleepId = this.dataTracker.get(CURRENT_DEEP_SLEEP_ANIM_ID);
                        if (!targetDeepSleepId.isEmpty()) {
                            RawAnimation targetDeepSleepAnim = switch (targetDeepSleepId) {
                                case "anim_hamster_sleep_pose1" -> SLEEP_POSE1_ANIM;
                                case "anim_hamster_sleep_pose2" -> SLEEP_POSE2_ANIM;
                                case "anim_hamster_sleep_pose3" -> SLEEP_POSE3_ANIM;
                                default -> SITTING_POSE1_ANIM; // Fallback
                            };
                            return event.setAndContinue(targetDeepSleepAnim);
                        } else if (this.isSitting()) {
                            // If interrupted, return to the correct personality-based sitting pose
                            return event.setAndContinue(switch (personality) {
                                case 2 -> SITTING_POSE2_ANIM;
                                case 3 -> SITTING_POSE3_ANIM;
                                default -> SITTING_POSE1_ANIM;
                            });
                        }
                        break;

                    case DEEP_SLEEP:
                        String deepSleepId = this.dataTracker.get(CURRENT_DEEP_SLEEP_ANIM_ID);
                        RawAnimation deepSleepAnimToPlay = switch (deepSleepId) {
                            case "anim_hamster_sleep_pose1" -> SLEEP_POSE1_ANIM;
                            case "anim_hamster_sleep_pose2" -> SLEEP_POSE2_ANIM;
                            case "anim_hamster_sleep_pose3" -> SLEEP_POSE3_ANIM;
                            // If interrupted, return to the correct personality-based sitting pose
                            default -> switch (personality) {
                                case 2 -> SITTING_POSE2_ANIM;
                                case 3 -> SITTING_POSE3_ANIM;
                                default -> SITTING_POSE1_ANIM;
                            };
                        };
                        return event.setAndContinue(deepSleepAnimToPlay);
                }
            }

            // 2. Wild Hamster Sleeping
            if (!this.isTamed() && this.isSleeping()) {
                // Read the target deep sleep animation from the DataTracker
                String deepSleepId = this.dataTracker.get(CURRENT_DEEP_SLEEP_ANIM_ID);
                RawAnimation deepSleepAnimToPlay = switch (deepSleepId) {
                    case "anim_hamster_sleep_pose2" -> SLEEP_POSE2_ANIM;
                    case "anim_hamster_sleep_pose3" -> SLEEP_POSE3_ANIM;
                    default -> SLEEP_POSE1_ANIM; // Fallback to pose 1
                };
                return event.setAndContinue(deepSleepAnimToPlay);
            }

            // --- Player-Commanded Sitting / Tamed Quiescent Sitting ---
            if (this.isSitting() && !this.isKnockedOut()) {
                if (getHamsterFlag(CLEANING_FLAG)) {
                    return event.setAndContinue(CLEANING_ANIM);
                } else {
                    // The logic to start cleaning lives in the tick() method.
                    // The animation controller only reacts to the state.
                    return event.setAndContinue(switch (personality) {
                        case 2 -> SITTING_POSE2_ANIM;
                        case 3 -> SITTING_POSE3_ANIM;
                        default -> SITTING_POSE1_ANIM;
                    });
                }
            }

            // --- Movement State ---
            double horizontalSpeedSquared = this.getVelocity().horizontalLengthSquared();
            if (horizontalSpeedSquared > 1.0E-6) { // Check if moving at all
                if (horizontalSpeedSquared > RUN_TO_SPRINT_THRESHOLD_SQUARED) {
                    return event.setAndContinue(SPRINTING_ANIM);
                } else if (horizontalSpeedSquared > WALK_TO_RUN_THRESHOLD_SQUARED) {
                    return event.setAndContinue(RUNNING_ANIM);
                } else {
                    return event.setAndContinue(WALKING_ANIM);
                }
            }

             // --- Begging State ---
            if (this.isBegging()) {
                 return event.setAndContinue(BEGGING_ANIM);
            }

            // --- Idle Looking Up State ---
             if (activeGoalName.equals(HamsterLookAtEntityGoal.class.getSimpleName())) {
                 return switch (this.dataTracker.get(CURRENT_LOOK_UP_ANIM_ID)) {
            case 2 -> event.setAndContinue(IDLE_LOOKING_UP2_ANIM);
            case 3 -> event.setAndContinue(IDLE_LOOKING_UP3_ANIM);
            default -> event.setAndContinue(IDLE_LOOKING_UP1_ANIM);
                 };
             }

            // --- Default Idle State ---
            // "Sticky" logic: If already playing an idle anim, keep it. Otherwise, pick a new one.
            RawAnimation current = event.getController().getCurrentRawAnimation();
            if (current != null && (current.equals(IDLE1_ANIM) || current.equals(IDLE2_ANIM))) {
                return event.setAndContinue(current);
            }
            return event.setAndContinue(this.random.nextBoolean() ? IDLE1_ANIM : IDLE2_ANIM);
            })
            .triggerableAnim("crash", CRASH_ANIM)
            .triggerableAnim("wakeup_from_ko", WAKE_UP_FROM_KO_ANIM)
            .triggerableAnim("standing_headshake", STANDING_HEADSHAKE_ANIM)
            .triggerableAnim("sitting_headshake", SITTING_HEADSHAKE_ANIM)
            .triggerableAnim("moving_headshake", MOVING_HEADSHAKE_ANIM)
            .triggerableAnim("attack", ATTACK_ANIM)
            .triggerableAnim("sit1", SIT1_ANIM)
            .triggerableAnim("sit2", SIT2_ANIM)
            .triggerableAnim("sit3", SIT3_ANIM)
            .triggerableAnim("standup1", STANDUP1_ANIM)
            .triggerableAnim("standup2", STANDUP2_ANIM)
            .triggerableAnim("standup3", STANDUP3_ANIM)
            .triggerableAnim("wakeup1", WAKE_UP_1_ANIM)
            .triggerableAnim("wakeup2", WAKE_UP_2_ANIM)
            .triggerableAnim("wakeup3", WAKE_UP_3_ANIM)
            .triggerableAnim("anim_hamster_sit_settle_sleep1", SIT_SETTLE_SLEEP1_ANIM)
            .triggerableAnim("anim_hamster_sit_settle_sleep2", SIT_SETTLE_SLEEP2_ANIM)
            .triggerableAnim("anim_hamster_sit_settle_sleep3", SIT_SETTLE_SLEEP3_ANIM)
            .triggerableAnim("anim_hamster_stand_settle_sleep1", STAND_SETTLE_SLEEP1_ANIM)
            .triggerableAnim("anim_hamster_stand_settle_sleep2", STAND_SETTLE_SLEEP2_ANIM)
            .triggerableAnim("anim_hamster_stand_settle_sleep3", STAND_SETTLE_SLEEP3_ANIM)
            .triggerableAnim("anim_hamster_sulk", SULK_ANIM)
            .triggerableAnim("anim_hamster_pounce_on_item", POUNCE_ON_ITEM_ANIM)
            .triggerableAnim("anim_hamster_celebrate_chase", CELEBRATE_CHASE_ANIM)

            // --- Handle Keyframe Particles ---
            .setParticleKeyframeHandler(event -> {
                // Sets a transient flag on the entity with the particle effect's ID.
                // The renderer polls this flag each frame to spawn particles on the client.
                this.particleEffectId = event.getKeyframeData().getEffect();
            })

            // --- Handle Keyframe Sounds ---
            .setSoundKeyframeHandler(event -> {
                // This just sets a flag. The renderer will handle it on the client.
                this.soundEffectId = event.getKeyframeData().getSound();
            })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * Triggers a one-shot animation on the server, which is then synced to clients.
     * This method also schedules a follow-up task to stop the triggered animation after its
     * expected duration. This serves as a failsafe to prevent animations that were triggered
     * while the entity was off-screen from playing belatedly when the entity is rendered again.
     *
     * @param controllerName The name of the animation controller.
     * @param animName The internal name of the triggerable animation (e.g., "crash").
     */
    public void triggerAnimOnServer(String controllerName, String animName) {
        if (!this.getWorld().isClient()) { // Ensure we're on the server
            // --- 1. Immediately trigger the animation ---
            // Use the GeoAnimatable's built-in method for triggering server-side
            this.triggerAnim(controllerName, animName);
            AdorableHamsterPets.LOGGER.trace("[HamsterEntity {}] Triggered server-side animation: Controller='{}', Anim='{}'", this.getId(), controllerName, animName);

            // --- 2. Schedule the cancellation task ---
            Integer duration = TRIGGERABLE_ANIM_DURATIONS.get(animName);
            if (duration != null) {
                long executionTick = this.getWorld().getTime() + duration;
                // Lambda that calls stopTriggeredAnim for the specific animation.
                Runnable cancellationAction = () -> {
                    // On 1.20.1 stopTriggeredAnim is stopTriggeredAnimation
                    this.stopTriggeredAnimation(controllerName, animName);
                    AdorableHamsterPets.LOGGER.trace("[HamsterEntity {}] Executed scheduled stop for animation: '{}'", this.getId(), animName);
                };
                scheduledTasks.add(new ScheduledTask(executionTick, animName, cancellationAction));
                AdorableHamsterPets.LOGGER.trace("[HamsterEntity {}] Scheduled stop for animation '{}' in {} ticks (at tick {}).", this.getId(), animName, duration, executionTick);
            } else {
                AdorableHamsterPets.LOGGER.warn("[HamsterEntity {}] No duration found for triggerable animation '{}'. Cancellation not scheduled.", this.getId(), animName);
            }
        }
    }



    /* ──────────────────────────────────────────────────────────────────────────────
     *                           5. Protected Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Data Tracker Initialization ---
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(HAMSTER_FLAGS, 0);
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(PINK_PETAL_TYPE, 0);
        this.dataTracker.startTracking(DOZING_PHASE, DozingPhase.NONE.ordinal());
        this.dataTracker.startTracking(CURRENT_DEEP_SLEEP_ANIM_ID, "");
        this.dataTracker.startTracking(ACTIVE_CUSTOM_GOAL_NAME_DEBUG, "None");
        this.dataTracker.startTracking(ANIMATION_PERSONALITY_ID, 1);
        this.dataTracker.startTracking(ITEM_INTEREST_TIMER, 0);
        this.dataTracker.startTracking(INTEREST_ITEM_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(GREEN_BEAN_BUFF_DURATION, 0L);
        this.dataTracker.startTracking(CURRENT_LOOK_UP_ANIM_ID, 1);
        this.dataTracker.startTracking(SHOULDER_ANIMATION_STATE, ShoulderAnimationState.STANDING.ordinal());
        this.dataTracker.startTracking(TRACKED_ACCESSORY_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(TRACKED_ARMOR_STACK, ItemStack.EMPTY);
    }

    // --- AI Goals ---
    @Override
    protected void initGoals() {
        AdorableHamsterPets.LOGGER.trace("[AI Init {} Tick {}] Initializing goals. Current State: isSleeping={}, isSittingPose={}",
                this.getId(), this.getWorld().isClient ? "ClientTick?" : this.getWorld().getTime(), this.isSleeping(), this.isInSittingPose());
        // --- 1. Initialize Goals ---
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new HamsterSeekDiamondGoal(this));
        this.goalSelector.add(1, new HamsterPlayWithItemGoal(this));
        this.goalSelector.add(2, new HamsterGoToBedAndSleepGoal(this));
        this.goalSelector.add(2, new HamsterMeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.add(3, new HamsterMateGoal(this, 0.75D));
        this.goalSelector.add(4, new HamsterFollowOwnerGoal(this, 1.0D, 4.0F, 16.0F));
        this.goalSelector.add(5, new HamsterFleeGoal<>(this, LivingEntity.class, 8.0F, 0.75D, 1.5D));
        this.goalSelector.add(6, new HamsterTemptGoal(this, 1.0D, false));
        this.goalSelector.add(7, new HamsterSitGoal(this));
        this.goalSelector.add(8, new HamsterSleepGoal(this));
        this.goalSelector.add(9, new HamsterWanderAroundFarGoal(this, 0.75D));
        this.goalSelector.add(10, new HamsterLookAtEntityGoal(this, PlayerEntity.class, 2.0F, 0.15F));
        this.goalSelector.add(11, new HamsterLookAroundGoal(this));

        // --- Target Selector Goals ---
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
        // --- End 1. Initialize Goals ---
        AdorableHamsterPets.LOGGER.trace("[AI Init {} Tick {}] Finished initializing goals.",
                this.getId(), this.getWorld().isClient ? "ClientTick?" : this.getWorld().getTime());
    }

    // --- Prevent walking over un-linked Hamster Beds ---
    @Override
    protected EntityNavigation createNavigation(World world) {
        if (Configs.AHP.avoidUnlinkedBeds) {
            return new HamsterNavigation(this, world);
        } else {
            return new MobNavigation(this, world);
        }
    }

    // --- Retaliation Against Other Pets Prevention ---
    /**
    * This method is overridden to prevent the hamster from targeting (e.g., retaliating against)
    * other pets owned by its own owner.
    */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null) {
            super.setTarget(null);
            return;
        }

        // --- 1. Check if Tamed and Has Owner ---
        if (this.isTamed() && this.getOwner() != null) {
            LivingEntity owner = this.getOwner();
            UUID ownerUuid = owner.getUuid();

            boolean preventTargeting = false;

            // Check TameableEntity
            if (target instanceof TameableEntity tameablePet) {
                UUID petOwnerUuid = tameablePet.getOwnerUuid();
                if (petOwnerUuid != null && petOwnerUuid.equals(ownerUuid) && tameablePet != this) {
                    // AdorableHamsterPets.LOGGER.debug("[setTarget] Proposed target is a TameableEntity owned by the same player. Preventing targeting.");
                    preventTargeting = true;
                }
            }
            // Check AbstractHorseEntity
            else if (target instanceof net.minecraft.entity.passive.AbstractHorseEntity horsePet) {
                Entity horseOwnerEntity = horsePet.getOwner();
                if (horseOwnerEntity != null && horseOwnerEntity.getUuid().equals(ownerUuid)) {
                    // AdorableHamsterPets.LOGGER.debug("[setTarget] Proposed target is an AbstractHorseEntity owned by the same player. Preventing targeting.");
                    preventTargeting = true;
                }
            }
            // General Ownable Check (fallback)
            else if (target instanceof Ownable ownableFallback) {
                Entity fallbackOwnerEntity = ownableFallback.getOwner();
                if (fallbackOwnerEntity != null && fallbackOwnerEntity.getUuid().equals(ownerUuid) && ownableFallback != this) {
                    // AdorableHamsterPets.LOGGER.debug("[setTarget] Proposed target is an Ownable (fallback) owned by the same player. Preventing targeting.");
                    preventTargeting = true;
                }
            }

            if (preventTargeting) {
                super.setTarget(null);
                return;
            }
        }

        // --- 3. Default Behavior ---
        super.setTarget(target);
    }

    /**
     * Calculates the position where the passenger sits.
     * <p>
     * Uses {@link HamsterSeatOffsets} to ensure the rider remains visually anchored
     * to the hamster's back, dynamically compensating for the entity's scale factor.
     */
    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if (this.hasPassenger(passenger)) {
            // Use the dynamic polyfill helper created specifically for 1.20.1
            float currentScale = this.getScale();

            // Vehicle (hamster) height is already scaled at runtime
            double baseY = this.getHeight() * 0.85;

            // Passenger-size compensation
            double riderAdjustY = passenger instanceof LivingEntity living
                    ? HamsterSeatOffsets.physicsSeatAdjustY(living, currentScale)
                    : 0.0;

            // Apply position via the updater on 1.20.1
            positionUpdater.accept(passenger, this.getX(), this.getY() + baseY + riderAdjustY, this.getZ());
        }
    }

    /**
     * Polyfill for 1.21.1 getScale(), which doesn't exist on 1.20.1.
     * Calculates scale dynamically based on current height vs base height (0.5).
     * Supports Baby state (0.5 scale) and arbitrary commands/mods.
     */
    public float getScale() {
        return this.getHeight() / 0.5F;
    }

    // --- Sounds / Effects ---
    /**
     * Initiates the sound and particle effects for when a hamster settles into its bed.
     * This is called by the AI goal when the hamster's state officially changes to sleeping in the bed.
     */
    public void startBedSleepEffects() {
        if (this.getWorld().isClient()) return;

        // --- 1. Spawn the first burst of particles immediately ---
        this.getLinkedBedPos().ifPresent(globalPos -> {
            if (this.getWorld().getRegistryKey() == globalPos.getDimension()) {
                BlockPos bedPos = globalPos.getPos();
                BlockState bedState = this.getWorld().getBlockState(bedPos);
                ((ServerWorld)this.getWorld()).spawnParticles(ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                        bedPos.getX() + 0.5, bedPos.getY() + 0.3, bedPos.getZ() + 0.5,
                        70, 0.2, 0.5, 0.2, 1);
            }
        });

        // --- 2. Set the timer for the remaining bursts ---
        this.bedLeafParticleTicks = 4;

        // --- 3. Play Sounds ---
        SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, this.random);
        if (rustleSound != null) {
            this.getWorld().playSound(null, this.getBlockPos(), rustleSound, SoundCategory.NEUTRAL, 0.5f, 1.0f);
        }
        this.getWorld().playSound(null, this.getBlockPos(), ModSounds.HAMSTER_THUMP.get(), SoundCategory.NEUTRAL, 1.0f, 1.0f);

        // --- Trigger Advancement ---
        if (this.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
            ModCriteria.HAMSTER_SLEPT_IN_BED.trigger(serverPlayerOwner);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // --- 0. Knocked Out Check (Silence) ---
        if (this.isKnockedOut()) {
            return null; // Knocked out hamsters make no ambient sounds
        }
        // --- 1. Begging/Taunting Sounds ---
        if (this.isBegging() || this.isTauntingWithItem()) {
            return getRandomSoundFrom(ModSounds.HAMSTER_BEG_SOUNDS, this.random);
        }
        // --- 2. Sleep Sounds ---
        boolean playSleepSounds = false;
        if (this.isTamed()) {
            DozingPhase phase = this.getDozingPhase();
            // Play sleep sounds if drifting, settling, or in deep sleep
            if (phase == DozingPhase.DRIFTING_OFF || phase == DozingPhase.SETTLING_INTO_SLUMBER || phase == DozingPhase.DEEP_SLEEP) {
                playSleepSounds = true;
            }
        } else { // Wild hamster
            if (this.isSleeping()) { // Checks the IS_SLEEPING DataTracker for wild hamsters
                playSleepSounds = true;
            }
        }
        if (playSleepSounds) {
            return getRandomSoundFrom(ModSounds.HAMSTER_SLEEP_SOUNDS, this.random);
        }
        // --- 3. Idle Sounds (Default) ---
        return getRandomSoundFrom(ModSounds.HAMSTER_IDLE_SOUNDS, this.random);
    }

    @Override
    public void playAmbientSound() {
        SoundEvent soundEvent = this.getAmbientSound();
        // Check if the selected sound is a begging sound
        if (soundEvent != null && Arrays.asList(ModSounds.HAMSTER_BEG_SOUNDS).contains(soundEvent)) {
            // If it's a begging sound, play it with lower volume
            this.playSound(soundEvent, 0.8F, this.getSoundPitch());
        } else {
            // For all other sounds, use the default behavior
            super.playAmbientSound();
        }
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return getRandomSoundFrom(ModSounds.HAMSTER_HURT_SOUNDS, this.random); }

    @Override protected SoundEvent getDeathSound() { return getRandomSoundFrom(ModSounds.HAMSTER_DEATH_SOUNDS, this.random); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.getWorld().isClient()) {
            return; // Server-side only
        }
        // Query the tracker to see if any player is rendering this hamster
        if (!HamsterRenderTracker.isBeingRendered(this.getId())) {
            try {
                BlockSoundGroup group = state.getSoundGroup();
                float volume = state.isOf(Blocks.GRAVEL)
                        ? (DEFAULT_FOOTSTEP_VOLUME * GRAVEL_VOLUME_MODIFIER)
                        : DEFAULT_FOOTSTEP_VOLUME;
                this.playSound(group.getStepSound(), volume, group.getPitch() * 1.5F);
            } catch (Exception ex) {
                AdorableHamsterPets.LOGGER.warn("Error playing fallback step sound", ex);
            }
        }
    }

    protected boolean canHitEntity(Entity entity) {
        // --- 1. Check if Entity Can Be Hit ---
        // Allow hitting armor stands specifically
        if (entity instanceof net.minecraft.entity.decoration.ArmorStandEntity) {
            return !entity.isSpectator(); // Can hit non-spectator armor stands
        }

        // Original logic for other entities
        if (!entity.isSpectator() && entity.isAlive() && entity.canHit()) {
            Entity owner = this.getOwner();
            // Prevent hitting self or owner or entities owner is riding
            return entity != this && (owner == null || !owner.isConnectedThroughVehicle(entity));
        }
        return false;
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound nbt) {
        AdorableHamsterPets.LOGGER.debug("[AHP Spawn Debug] HamsterEntity.initialize called. SpawnReason: {}", spawnReason);

        // --- Assign Animation Personality ID ---
        if (!world.isClient()) {
            int personalityId = this.random.nextBetween(1, 3);
            this.dataTracker.set(ANIMATION_PERSONALITY_ID, personalityId);
            AdorableHamsterPets.LOGGER.trace("[INITIALIZE] Hamster ID {}: Assigned Personality ID {}", this.getId(), personalityId);
        }

        // Apply biome variants for natural spawns, spawn eggs, AND chunk generation
        if (spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.SPAWN_EGG || spawnReason == SpawnReason.CHUNK_GENERATION) {
            RegistryEntry<Biome> biomeEntry = world.getBiome(this.getBlockPos());
            String biomeKeyStr = biomeEntry.getKey().map(key -> key.getValue().toString()).orElse("UNKNOWN");
            AdorableHamsterPets.LOGGER.trace("[HamsterInit] SpawnReason: {}, BiomeKey: {}", spawnReason, biomeKeyStr);

            HamsterVariant chosenVariant = determineVariantForBiome(biomeEntry, this.random);
            this.setVariant(chosenVariant.getId());
            AdorableHamsterPets.LOGGER.trace("[HamsterInit] Assigned variant: {}", chosenVariant.name());

        } else {
            // Fallback for other spawns (command, breeding, structure, etc.)
            int randomVariantId = this.random.nextInt(HamsterVariant.values().length);
            this.setVariant(randomVariantId);
            AdorableHamsterPets.LOGGER.trace("[HamsterInit] SpawnReason: {}, Assigned random variant: {}",
                    spawnReason, HamsterVariant.byId(randomVariantId).name());
        }

        // --- Apply Configured Health on Spawn ---
        // This ensures the entity instance uses the live config value, overriding any
        // stale value that might have been baked in during static attribute registration.
        if (!this.isTamed()) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(Configs.AHP.wildMaxHealth.get());
            this.setHealth(this.getMaxHealth()); // Set current health to the new max
        }

        // --- Wild Hamster Loot Generation ---
        generateWildLoot();

        // Call and return the super method's result with the added nbt parameter for 1.20.1
        return super.initialize(world, difficulty, spawnReason, entityData, nbt);
    }

    @Override
    protected BodyControl createBodyControl() {
        return new HamsterBodyControl(this);
    }



    /* ──────────────────────────────────────────────────────────────────────────────
     *                       6. Private Helper Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Attempts to respawn the hamster at its linked bed.
     * @return True if respawn was successful, false otherwise.
     */
    private boolean tryRespawnInBed() {
        if (this.getLinkedBedPos().isEmpty()) return false;

        GlobalPos globalBedPos = this.getLinkedBedPos().get();
        MinecraftServer server = this.getServer();
        if (server == null) return false;

        ServerWorld bedWorld = server.getWorld(globalBedPos.getDimension());
        if (bedWorld == null) return false;

        BlockPos bedPos = globalBedPos.getPos();
        BlockState bedState = bedWorld.getBlockState(bedPos);

        // Verify bed exists
        if (!(bedState.getBlock() instanceof HamsterBedBlock)) {
            return false;
        }

        // Check bed-specific enablement
        BlockEntity beCheck = bedWorld.getBlockEntity(bedPos);
        if (!(beCheck instanceof HamsterBedBlockEntity bedEntity) || !bedEntity.isRespawnEnabled()) {
            // Bed exists, but respawn is not paid for/enabled.
            // Silent fail.
            return false;
        }

        // Check occupancy to determine spawn mode
        boolean isBedFree = !bedState.get(HamsterBedBlock.OCCUPIED);
        BlockPos finalSpawnPos = null;

        if (!isBedFree) {
            // Bed is occupied, fallback to finding a safe spot nearby
            Optional<BlockPos> safePosOpt = this.findSafeSpawnPosition(bedPos, bedWorld, 2);
            if (safePosOpt.isEmpty()) {
                // Silent fail
                return false;
            }
            finalSpawnPos = safePosOpt.get();
        }

        // --- Create Clone ---
        HamsterEntity newHamster = ModEntities.HAMSTER.get().create(bedWorld);
        if (newHamster == null) return false;

        // Copy NBT Data
        NbtCompound data = new NbtCompound();
        this.writeCustomDataToNbt(data);
        newHamster.readCustomDataFromNbt(data);

        // Restore attributes that writeCustomDataToNbt might miss (Owner, Tame status)
        newHamster.setOwnerUuid(this.getOwnerUuid());
        newHamster.setTamed(this.isTamed(), false);
        newHamster.setCustomName(this.getCustomName());

        // Reset Common States
        newHamster.setKnockedOut(false);
        newHamster.interactionCooldown = 0;

        // --- Spawn Logic Branch ---
        if (isBedFree) {
            // Scenario A: Bed is free -> Sleep in it
            Vec3d bedCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);
            newHamster.refreshPositionAndAngles(bedCenter.x, bedCenter.y, bedCenter.z, 0f, 0f);

            // Set to 5% Health
            newHamster.setHealth(Math.max(1.0f, newHamster.getMaxHealth() * 0.05f));

            // Force Sleep State
            newHamster.setDozingPhase(DozingPhase.DEEP_SLEEP);
            newHamster.setSleeping(true);
            newHamster.setInSittingPose(true); // Lock AI

            // Select sleep pose based on personality ID to match original hamster
            int personality = newHamster.getDataTracker().get(ANIMATION_PERSONALITY_ID);
            int poseIndex = (personality >= 1 && personality <= 3) ? personality : 1;
            String sleepAnim = "anim_hamster_sleep_pose" + poseIndex;
            newHamster.getDataTracker().set(CURRENT_DEEP_SLEEP_ANIM_ID, sleepAnim);

            // Update Block State
            bedWorld.setBlockState(bedPos, bedState.with(HamsterBedBlock.OCCUPIED, true), Block.NOTIFY_ALL);

            // Trigger Bed Animation
            // Call the method directly on 1.20.1
            bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
        } else {
            // Scenario B: Bed occupied -> Spawn nearby standing up
            newHamster.refreshPositionAndAngles(finalSpawnPos.getX() + 0.5, finalSpawnPos.getY(), finalSpawnPos.getZ() + 0.5, this.getYaw(), 0f);
            newHamster.setHealth(newHamster.getMaxHealth());
            newHamster.setSitting(false);
        }

        // --- Linkage Update & Charge Consumption ---
        // Created a new entity, so it has a new UUID. Update the Bed Block Entity
        if (bedWorld.getBlockEntity(bedPos) instanceof HamsterBedBlockEntity finalBedEntity) {
            Text name = newHamster.hasCustomName() ? newHamster.getCustomName() : newHamster.getDisplayName();
            finalBedEntity.setLinkedHamster(newHamster.getUuid(), name, finalBedEntity.getWanderDistance());

            // Consume charge
            finalBedEntity.setRespawnEnabled(false);
        }

        // Spawn
        bedWorld.spawnEntity(newHamster);

        // Effects
        bedWorld.playSound(null, bedPos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        bedWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);

        if (this.getOwner() instanceof PlayerEntity owner) {
            owner.sendMessage(Text.translatable("message.adorablehamsterpets.respawn.success").formatted(Formatting.GOLD), true);
        }

        return true;
    }

    /**
     * Called when a passenger is removed.
     * <p>
     * Overridden to explicitly reset all rider input state to prevent
     * "sticky" inputs or visual glitches after dismounting.
     */
    @Override
    protected void removePassenger(Entity passenger) {
        Entity controller = this.getControllingPassenger();
        super.removePassenger(passenger);

        // If the driver dismounted, reset all driving state
        if (passenger == controller) {
            this.riderJumpCooldown = 0;
            this.riderJumpHeld = false;
            this.riderSprintHeld = false;
            this.riderJumpQueued = false;
            this.setSprinting(false);
        }
    }

    /**
     * Executes the jump logic for a ridden hamster.
     * <p>
     * Validates ground state, applies vertical velocity, triggers the
     * jump cooldown, and plays a bounce sound.
     */
    private void tryRiderJump() {
        if (!this.isOnGround()) {
            return;
        }

        if (this.isTouchingWater() || this.isInLava()) return;

        this.jump();

        // Enforce exact jump height
        Vec3d v = this.getVelocity();
        this.setVelocity(v.x, RIDER_JUMP_VELOCITY, v.z);
        this.velocityDirty = true;
        this.fallDistance = 0.0F;

        // --- Sound Logic ---
        PlayerEntity rider = (this.getControllingPassenger() instanceof PlayerEntity p) ? p : null;

        // Randomize pitch: Base 1.2 with a variance of +/- 0.2 (Result: 1.0 to 1.4)
        float randomPitch = 1.2f + (this.random.nextFloat() * 0.4f - 0.2f);

        this.getWorld().playSound(rider, this.getX(), this.getY(), this.getZ(),
                ModSounds.HAMSTER_BOUNCE.get(),
                net.minecraft.sound.SoundCategory.PLAYERS,
                0.6f,
                randomPitch
        );

        this.riderJumpCooldown = RIDER_JUMP_COOLDOWN_TICKS;
    }

    /**
     * Checks if the provided NBT compound contains valid inventory data.
     */
    private boolean hasInventoryData(NbtCompound nbt) {
        return nbt.contains("Inventory", NbtElement.COMPOUND_TYPE);
    }

    /**
     * Helper method to generate random loot in the cheek pouches of wild hamsters.
     * Includes a check to ensure we don't overwrite existing items or fill tamed hamsters.
     * Supports configurable loot lists and chances.
     */
    private void generateWildLoot() {
        // Only generate if untamed and inventory is empty (safety check)
        if (this.isTamed() || !this.items.get(0).isEmpty()) return;

        // --- 1. Determine which cheeks to fill ---
        // 60% chance for 1 cheek (lopsided), 40% for both
        boolean fillBothCheeks = this.random.nextFloat() < 0.4f;

        // Helper to fill a cheek (3 slots)
        BiConsumer<Integer, Boolean> fillCheek = (startSlot, isCustom) -> {
            // Random count 1-3
            int count = 1 + this.random.nextInt(3);
            // Put it in a random slot within the cheek (0-2 or 3-5)
            int specificSlot = startSlot + this.random.nextInt(3);

            // Pick item based on source
            Item item = isCustom
                    ? ConfigDataCache.getRandomCustomLootItem(this.random)
                    : ConfigDataCache.getRandomDefaultLootItem(this.random);

            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item, count);
                // Respect pouch restrictions
                if (!isItemDisallowed(stack)) {
                    // Only set if slot is empty
                    if (this.items.get(specificSlot).isEmpty()) {
                        this.setStack(specificSlot, stack);
                    }
                }
            }
        };

        // --- 2. Process Default Loot Pool ---
        float defaultChance = Configs.AHP_WORLDGEN.defaultCheekLootChance.get();
        if (this.random.nextFloat() < defaultChance) {
            // Fill Left Cheek (Slots 0-2) or Right Cheek (Slots 3-5)
            if (fillBothCheeks) {
                fillCheek.accept(0, false); // Left
                fillCheek.accept(3, false); // Right
            } else {
                fillCheek.accept(this.random.nextBoolean() ? 0 : 3, false);
            }
        }

        // --- 3. Process Custom Loot Pool ---
        float customChance = Configs.AHP_WORLDGEN.extraCheekLootChance.get();
        // Only run custom logic if the list isn't empty
        if (!Configs.AHP_WORLDGEN.extraCheekLootList.isEmpty() && this.random.nextFloat() < customChance) {
            // Fill Left Cheek (Slots 0-2) or Right Cheek (Slots 3-5)
            if (fillBothCheeks) {
                fillCheek.accept(0, true); // Left
                fillCheek.accept(3, true); // Right
            } else {
                fillCheek.accept(this.random.nextBoolean() ? 0 : 3, true);
            }
        }
    }

    /**
     * Triggers the visual and auditory effects of a hamster entering a tree canopy.
     *
     * @param pos The position where the effects should play.
     * @param playBreakSound True if the "crunchy" sound should play (e.g. branch/leaf hit).
     */
    public void triggerLeafPopEffects(BlockPos pos, boolean playBreakSound) {
        if (!this.getWorld().isClient()) {

            // --- Audio: Crunch + Rustle ---
            // Simulate entering the dense leaves
            this.getWorld().playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.NEUTRAL, 0.7f, 1.2f);
            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, this.random);
            if (rustleSound != null) {
                this.getWorld().playSound(null, pos, rustleSound, SoundCategory.NEUTRAL, 1.7f, 1.0f);
            }

            // --- Visuals ---
            ((ServerWorld)this.getWorld()).spawnParticles(
                    ModParticles.getForVariant(WoodVariant.BAMBOO),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    50, 0.4, 0.4, 0.4, 0
            );

            ((ServerWorld)this.getWorld()).spawnParticles(
                    net.minecraft.particle.ParticleTypes.POOF,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    50, 0.5, 0.75, 0.5, 0
            );
        }
    }

    /**
     * Triggers a delayed celebratory sound after a successful tree heist.
     */
    public void scheduleTreeHeistCelebration() {
        if (!this.getWorld().isClient()) {
            // Schedule sound 20 ticks (1 second) later
            this.scheduledTasks.add(new ScheduledTask(this.getWorld().getTime() + 20, "heist_celebration", () -> {
                SoundEvent sparkleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random);
                if (sparkleSound != null) {
                    this.playSound(sparkleSound, 1.0F, 1.0F);
                }
            }));
        }
    }

    /**
     * Synchronizes the visual state (DataTrackers) with the Accessory Slot inventory.
     */
    public void updateAccessoryState() {
        ItemStack accessory = this.items.get(ACCESSORY_SLOT_INDEX);

        // Handle Pink Petal Tracker
        if (accessory.isOf(Items.PINK_PETALS)) {
            // If we have petals but tracker is 0 (just equipped), set to default 1
            if (this.dataTracker.get(PINK_PETAL_TYPE) == 0) {
                this.dataTracker.set(PINK_PETAL_TYPE, 1);
            }
        } else {
            // If slot is empty or has a different item (e.g. Hat), reset petal tracker
            if (this.dataTracker.get(PINK_PETAL_TYPE) != 0) {
                this.dataTracker.set(PINK_PETAL_TYPE, 0);
            }
        }
    }

    /**
     * plays appropriate equip/unequip sounds when items change in the Armor or Bling slots.
     *
     * @param slot The slot index changing.
     * @param oldStack The item stack previously in the slot.
     * @param newStack The new item stack in the slot.
     */
    private void handleSlotUpdateSounds(int slot, ItemStack oldStack, ItemStack newStack) {
        boolean isEmpty = newStack.isEmpty();
        boolean wasEmpty = oldStack.isEmpty();

        // If nothing changed effectively (e.g. swapping same item), return
        if (ItemStack.areEqual(oldStack, newStack)) return;

        // Armor Slot (7)
        if (slot == ARMOR_SLOT_INDEX) {
            if (wasEmpty && !isEmpty) {
                // Equip
                this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            } else if (!wasEmpty && isEmpty) {
                // Unequip
                this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.4f, 0.8f);
            } else if (!wasEmpty && !isEmpty) {
                // Swap
                this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            }
        }

        // Bling Slot (6)
        if (slot == ACCESSORY_SLOT_INDEX) {
            if (wasEmpty && !isEmpty) {
                // Equip
                this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            } else if (!wasEmpty && isEmpty) {
                // Unequip (Lower pitch)
                this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.4f, 0.8f);
            } else if (!wasEmpty && !isEmpty) {
                // Swap
                this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            }
        }
    }

    /**
     * Updates the entity's attribute modifiers based on the currently equipped armor item.
     * <p>
     * This method is <b>state-aware (idempotent)</b>. It compares the <i>current</i> attribute modifiers
     * against the <i>expected</i> state (defined by the item + config). Modifiers are added or removed
     * only when a discrepancy is found. This allows safe, periodic execution to sync with config changes.
     *
     * @param armorStack The ItemStack currently residing in the armor slot.
     */
    private void updateArmorModifiers(ItemStack armorStack) {
        EntityAttributeInstance speedAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        EntityAttributeInstance knockbackAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);

        // --- 1. Calculate Expected State ---
        // Determine which buffs should be active based on the global config and the specific armor material.
        boolean perksEnabled = Configs.AHP.enableArmorPerks.get();
        boolean shouldHaveSpeed = false;
        boolean shouldHaveKnockback = false;

        if (perksEnabled && !armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
            HamsterArmorItem.HamsterArmorMaterial material = armorItem.getMaterial();
            if (material == HamsterArmorItem.HamsterArmorMaterial.GOLD) {
                shouldHaveSpeed = true;
            } else if (material == HamsterArmorItem.HamsterArmorMaterial.NETHERITE) {
                shouldHaveKnockback = true;
            }
        }

        // --- 2. Reconcile Speed Attribute ---
        if (speedAttribute != null) {
            // 1.20.1: Check by UUID
            boolean hasSpeed = speedAttribute.getModifier(ARMOR_SPEED_BOOST_UUID) != null;

            if (shouldHaveSpeed && !hasSpeed) {
                // Case: Buff is required but missing -> ADD IT
                // 1.20.1: Use UUID constructor and MULTIPLY_BASE
                speedAttribute.addTemporaryModifier(new EntityAttributeModifier(
                        ARMOR_SPEED_BOOST_UUID, "Hamster Armor Speed", 0.20D, EntityAttributeModifier.Operation.MULTIPLY_BASE
                ));
            } else if (!shouldHaveSpeed && hasSpeed) {
                // Case: Buff is present but forbidden -> REMOVE IT
                speedAttribute.removeModifier(ARMOR_SPEED_BOOST_UUID);
            }
        }

        // --- 3. Reconcile Knockback Attribute ---
        if (knockbackAttribute != null) {
            // 1.20.1: Check by UUID
            boolean hasKnockback = knockbackAttribute.getModifier(ARMOR_KNOCKBACK_RESISTANCE_UUID) != null;

            if (shouldHaveKnockback && !hasKnockback) {
                // Case: Buff is required but missing -> ADD IT
                // 1.20.1: Use UUID constructor and ADDITION
                knockbackAttribute.addTemporaryModifier(new EntityAttributeModifier(
                        ARMOR_KNOCKBACK_RESISTANCE_UUID, "Hamster Armor KB Resist", 0.5D, EntityAttributeModifier.Operation.ADDITION
                ));
            } else if (!shouldHaveKnockback && hasKnockback) {
                // Case: Buff is present but forbidden -> REMOVE IT
                knockbackAttribute.removeModifier(ARMOR_KNOCKBACK_RESISTANCE_UUID);
            }
        }
    }

    /**
     * Plays an impact sound for all players within range, bypassing vanilla attenuation to ensure
     * consistent audibility across distances. Uses a custom volume gradient to mimic natural falloff
     * while maintaining clarity at long ranges. Checks for non-organic armor and plays a shield block sound if present.
     * <p>
     * <b>Volume Curve:</b>
     * <ul>
     *     <li><b>0 - 16 Blocks:</b> Linear decrease from 1.0 to 0.18.</li>
     *     <li><b>16 - 50 Blocks:</b> Linear decrease from 0.18 to 0.10.</li>
     * </ul>
     *
     * @param sound The main sound event to play (e.g., small fall or hamster impact).
     * @param pitch The pitch at which to play the main sound.
     */
    private void broadcastImpactSound(SoundEvent sound, float pitch) {
        if (this.getWorld().isClient()) return;

        double impactX = this.getX();
        double impactY = this.getY();
        double impactZ = this.getZ();

        // Check for armor
        SoundEvent armorSound = null;
        float armorPitch = 1.0f;

        if (this.items.size() > ARMOR_SLOT_INDEX) {
            ItemStack armorStack = this.items.get(ARMOR_SLOT_INDEX);
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
                // Play metallic clang for anything that isn't the base Acorn armor
                if (armorItem.getMaterial() != HamsterArmorItem.HamsterArmorMaterial.ACORN) {
                    armorSound = SoundEvents.BLOCK_BELL_USE;
                    armorPitch = 2.0f + this.random.nextFloat() * 0.5f;
                }
            }
        }

        for (ServerPlayerEntity player : ((ServerWorld) this.getWorld()).getPlayers()) {
            double distSq = player.squaredDistanceTo(impactX, impactY, impactZ);

            if (distSq <= 2500) { // 50 blocks squared
                double distance = Math.sqrt(distSq);
                float volume;

                if (distance <= 16.0) {
                    // Stage 1: Close range (0 to 16 blocks) - Linear 1.0 -> 0.18
                    volume = 1.0F - (0.82F * (float) (distance / 16.0));
                } else {
                    // Stage 2: Distant range (16 to 50 blocks) - Linear 0.18 -> 0.10
                    float remainingProgress = (float) (distance - 16.0) / 34.0F;
                    volume = 0.18F - (0.08F * remainingProgress);
                }

                // Clamp to safe bounds
                volume = MathHelper.clamp(volume, 0.10F, 1.0F);

                // Send packet for Main Sound
                // 1.20.1: Use ModPackets.CHANNEL and the S2CPacket record
                ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayDistantSoundS2CPacket(sound.getId(), volume, pitch));

                // Send packet for Armor Sound if applicable
                if (armorSound != null) {
                    // Reduce armor volume by 50% relative to main sound
                    float armorVolume = Math.min(1.0f, volume * 0.5f);
                    ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayDistantSoundS2CPacket(armorSound.getId(), armorVolume, armorPitch));
                }
            }
        }
    }

    /**
     * Simulates the hamster's trajectory 1 second (20 ticks) into the future.
     * If an impact (block or entity) is predicted within that window, and the total
     * throw time will have been at least 1 second, it plays the "Incoming" sound
     * at the target location.
     */
    private void simulateTrajectoryAndCheckSound() {
        Vec3d simPos = this.getPos();
        Vec3d simVel = this.getVelocity();

        // Simulate up to 20 ticks ahead
        for (int i = 1; i <= 20; i++) {
            // Apply physics matching the actual tick logic
            if (!this.hasNoGravity()) {
                simVel = simVel.add(0.0, THROWN_GRAVITY, 0.0);
            }

            Vec3d nextPos = simPos.add(simVel);

            // 1. Block Collision Check
            HitResult blockHit = this.getWorld().raycast(new RaycastContext(
                    simPos,
                    nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    this
            ));

            // 2. Entity Collision Check
            // We use the same logic as ProjectileUtil to check for entities along the path segment
            EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                    this.getWorld(),
                    this,
                    simPos,
                    nextPos,
                    this.getBoundingBox().stretch(simVel).expand(1.0),
                    this::canHitEntity
            );

            Vec3d impactPos = null;

            if (entityHit != null) {
                impactPos = entityHit.getPos();
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                impactPos = blockHit.getPos();
            }

            if (impactPos != null) {
                // Collision predicted in 'i' ticks.
                // Only play sound if the TOTAL time (elapsed + future) is >= 20 ticks.
                if (this.throwTicks + i >= 20) {
                    this.getWorld().playSound(null, impactPos.x, impactPos.y, impactPos.z, ModSounds.HAMSTER_INCOMING.get(), SoundCategory.NEUTRAL, 1.0f, 1.0f);
                    AdorableHamsterPets.LOGGER.debug("Played Incoming sound at target: {}", impactPos);
                }
                // Mark as handled regardless of whether we played it (to prevent spamming checks for short throws)
                this.hasPlayedIncomingSound = true;
                return;
            }

            // Update simulation position for next tick
            simPos = nextPos;
        }
    }

    /**
     * A simple server-side task scheduler to handle delayed actions, primarily for animation cleanup.
     * When a triggerable animation is fired, a corresponding "stop" task is scheduled to run
     * after the animation's expected duration. This prevents animations that were triggered while
     * the entity was off-screen from playing belatedly when the entity is rendered again.
     */
    // --- Animation Cancellation Scheduler ---
    private record ScheduledTask(long executionTick, String animName, Runnable action) {}
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

    private static final Map<String, Integer> TRIGGERABLE_ANIM_DURATIONS = new HashMap<>();
    static {
        // Durations are in ticks (Animation Length + small 3 tick buffer)
        TRIGGERABLE_ANIM_DURATIONS.put("crash", 32);
        TRIGGERABLE_ANIM_DURATIONS.put("wakeup_from_ko", 18);
        TRIGGERABLE_ANIM_DURATIONS.put("standing_headshake", 25);
        TRIGGERABLE_ANIM_DURATIONS.put("sitting_headshake", 25);
        TRIGGERABLE_ANIM_DURATIONS.put("moving_headshake", 25);
        TRIGGERABLE_ANIM_DURATIONS.put("attack", 23);
        TRIGGERABLE_ANIM_DURATIONS.put("sit1", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("sit2", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("sit3", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("standup1", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("standup2", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("standup3", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("wakeup1", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("wakeup2", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("wakeup3", 13);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_sit_settle_sleep1", 23);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_sit_settle_sleep2", 23);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_sit_settle_sleep3", 23);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_stand_settle_sleep1", 35);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_stand_settle_sleep2", 35);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_stand_settle_sleep3", 35);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_sulk", 63);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_diamond_pounce", 23);
        TRIGGERABLE_ANIM_DURATIONS.put("anim_hamster_celebrate_chase", 33);
    }


    /**
     * Gets the value of a specific boolean flag from the packed integer.
     * @param flag The bitmask of the flag to check (e.g., SLEEPING_FLAG).
     * @return True if the bit for the flag is set, false otherwise.
     */
    private boolean getHamsterFlag(int flag) {
        return (this.dataTracker.get(HAMSTER_FLAGS) & flag) != 0;
    }

    /**
     * Sets or clears a specific boolean flag in the packed integer.
     * @param flag The bitmask of the flag to modify (e.g., SLEEPING_FLAG).
     * @param value True to set the bit, false to clear it.
     */
    private void setHamsterFlag(int flag, boolean value) {
        int currentFlags = this.dataTracker.get(HAMSTER_FLAGS);
        if (value) {
            this.dataTracker.set(HAMSTER_FLAGS, currentFlags | flag);
        } else {
            this.dataTracker.set(HAMSTER_FLAGS, currentFlags & ~flag);
        }
    }

    /**
     * Checks if a given block position is a safe location for a hamster to spawn.
     * A location is safe if:
     * 1. The block below is not a hazard (checked via PathNodeType).
     * 2. The block below has a collision shape to stand on.
     * 3. The two blocks at the spawn position (for feet and head) have no collision shape *for this specific hamster*.
     *
     * @param pos   The block position to check.
     * @param world The world to check in.
     * @return True if the location is safe, false otherwise.
     */
    private boolean isSafeSpawnLocation(BlockPos pos, World world) {
        // --- 1. Check for a valid, non-hazardous floor ---
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);

        // Use invoker to get the pathfinding node type of the floor.
        PathNodeType floorType = LandPathNodeMakerInvoker.callGetCommonNodeType(world, floorPos);
        if (HAZARDOUS_FLOOR_TYPES.contains(floorType)) {
            return false; // Floor is a known hazard.
        }

        // Ensure there is a physical surface to stand on (not just air or grass).
        if (floorState.getCollisionShape(world, floorPos).isEmpty()) {
            return false;
        }

        // --- 2. Check for empty body/head space using entity-specific context ---
        // The block is considered safe if it has no collision for the HamsterEntity.
        ShapeContext entityContext = ShapeContext.of(this);
        return world.getBlockState(pos).getCollisionShape(world, pos, entityContext).isEmpty() &&
                world.getBlockState(pos.up()).getCollisionShape(world, pos.up(), entityContext).isEmpty();
    }

    /**
     * Checks if the given item stack is disallowed in the hamster's inventory.
     *
     * @param stack The ItemStack to check.
     * @return True if the item is disallowed, false otherwise.
     */
    public boolean isItemDisallowed(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 1. Explicit allow list has highest priority.
        if (ConfigDataCache.isPouchAllowed(stack)) {
            return false; // It's allowed, so override everything else.
        }

        // 2. Check the disallow lists from config.
        if (ConfigDataCache.isPouchDisallowed(stack)) {
            return true;
        }

        // 3. Mod Food Logic
        // If the mod considers it food or bait, the hamster can hold it, even if it's a block item.
        if (ConfigDataCache.isStandardFood(stack) ||
                ConfigDataCache.isTamingFood(stack) ||
                ConfigDataCache.isBuffFood(stack) ||
                ConfigDataCache.isPouchUnlockFood(stack) ||
                ConfigDataCache.isAutoHealFood(stack)) {
            return false;
        }

        // 4. Allow any item that is considered food by vanilla.
        // 1.20.1: Food status is a direct method
        if (stack.isFood()) {
            return false;
        }

        Item item = stack.getItem();

        // 5. Global block-item rule
        if (item instanceof BlockItem) {
            // Any block is disallowed by default unless it was on the allowlist or was food.
            return true;
        }

        // 6. Spawn eggs always disallowed.
        return item instanceof SpawnEggItem;
    }

    private RegistryWrapper.WrapperLookup getRegistryLookup() {
        return this.getWorld().getRegistryManager();
    }

    private boolean tryTame(PlayerEntity player, ItemStack itemStack) {
        // --- 1. Taming Attempt ---
        if (!player.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }

        // --- Use Config Value for Taming Chance ---
        final AhpConfig config = AdorableHamsterPets.CONFIG;
        int denominator = Math.max(1, config.tamingChanceDenominator.get()); // Ensure denominator is at least 1
        if (this.random.nextInt(denominator) == 0) {
            this.setOwnerUuid(player.getUuid());
            this.setTamed(true, true);
            this.navigation.stop();
            this.setSitting(false);
            this.setSleeping(false);
            this.setTarget(null);
            this.getWorld().sendEntityStatus(this, (byte) 7);

            // Play celebrate sound only on success
            SoundEvent celebrateSound = getRandomSoundFrom(HAMSTER_CELEBRATE_SOUNDS, this.random);
            this.getWorld().playSound(null, this.getBlockPos(), celebrateSound, SoundCategory.NEUTRAL, 0.7F, 1.0F);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                Criteria.TAME_ANIMAL.trigger(serverPlayer, this);
            }

            return true;
        } else {
            this.getWorld().sendEntityStatus(this, (byte) 6);
            return false;
        }
    }

    // --- Check for Repeatable Foods ---
    /**
     * Checks if the hamster should refuse being fed the same item twice consecutively.
     * <p>
     * If a refusal occurs, this method also triggers a context-aware headshake animation:
     * {@code anim_hamster_moving_headshake} if the hamster is walking, or
     * {@code anim_hamster_stationary_headshake} if it is still.
     * <p>
     * An item is exempt from this check if it is included in the user-configurable
     * {@code repeatableFoods} list, managed by {@link ConfigDataCache#isRepeatableFood(ItemStack)}.
     *
     * @param currentStack The ItemStack the player is attempting to feed.
     * @param player The player performing the action.
     * @return {@code true} if the food was refused, {@code false} otherwise.
     */
    private boolean checkRepeatFoodRefusal(ItemStack currentStack, PlayerEntity player) {
        // --- 1. Check Repeat Food Refusal ---
        if (ConfigDataCache.isRepeatableFood(currentStack)) return false;

        if (!this.lastFoodItem.isEmpty() && ItemStack.areItemsEqual(this.lastFoodItem, currentStack)) {
            this.setRefusingFood(true);
            this.refuseTimer = REFUSE_FOOD_TIMER_TICKS;
            player.sendMessage(Text.translatable("message.adorablehamsterpets.food_refusal"), true);

            // --- Conditional Animation Trigger ---
            if (!this.getWorld().isClient()) {
                // Check if the hamster has significant horizontal velocity.
                boolean isMoving = this.getVelocity().horizontalLengthSquared() > 1.0E-6;
                if (isMoving) {
                    this.triggerAnimOnServer("mainController", "moving_headshake");
                } else {
                    this.triggerAnimOnServer("mainController", "stationary_headshake");
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Attempts to feed the hamster, handling healing, breeding, buffs, and pouch unlocking.
     * This logic is driven by user-configurable item lists from {@link ConfigDataCache},
     * such as {@code standardFoods}, {@code buffFoods}, and {@code pouchUnlockFoods}.
     *
     * @param player The player feeding the hamster.
     * @param stack  The ItemStack being used.
     * @return {@code true} if a feeding action was successfully processed.
     */
    private boolean tryFeedingAsTamed(PlayerEntity player, ItemStack stack) {
        // --- 1. Initial Setup & Logging ---
        boolean isFood = ConfigDataCache.isStandardFood(stack);
        boolean isBuffItem = ConfigDataCache.isBuffFood(stack);
        boolean isPouchUnlockFood = ConfigDataCache.isPouchUnlockFood(stack);
        boolean canHeal = this.getHealth() < this.getMaxHealth();
        boolean readyToBreed = this.getBreedingAge() == 0 && !this.isInCustomLove(); // Check custom love timer
        World world = this.getWorld();
        final AhpConfig config = AdorableHamsterPets.CONFIG;
        boolean actionTaken = false; // Initialize return value

        AdorableHamsterPets.LOGGER.debug("[FeedAttempt {} Tick {}] Entering tryFeedingAsTamed. Item: {}, isFood={}, isBuff={}, canHeal={}, breedingAge={}, isInCustomLove={}, readyToBreed={}",
                this.getId(), world.getTime(), stack.getItem(), isFood, isBuffItem, canHeal, this.getBreedingAge(), this.isInCustomLove(), readyToBreed);

        // --- 2. Check for Pouch Unlock First (Highest Priority Feeding Action) ---
        if (isPouchUnlockFood && !getHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG)) {
            setHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG, true);
            AdorableHamsterPets.LOGGER.debug("Hamster {} cheek pouch unlocked by {}.", this.getId(), stack.getItem());
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ModCriteria.CHEEK_POUCH_UNLOCKED.trigger(serverPlayer, this);
            }
            world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 0.5f, 1.5f);
            if (!world.isClient) {
                ((ServerWorld) world).spawnParticles(
                        new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                        this.getX(), this.getBodyY(0.2D), this.getZ(),
                        25, 0.25D, 0.15D, 0.25D, 0.0D
                );
            }
            return true;
        }

        // --- 3. Check for Buff Food (Steamed Green Beans Logic) ---
        if (isBuffItem) {
            long currentTime = world.getTime();
            if (this.greenBeanBuffEndTick > currentTime) {
                // Still on cooldown
                long remainingTicks = this.greenBeanBuffEndTick - currentTime;
                long totalSecondsRemaining = remainingTicks / 20;
                long minutes = totalSecondsRemaining / 60;
                long seconds = totalSecondsRemaining % 60;
                player.sendMessage(Text.translatable("message.adorablehamsterpets.beans_cooldown", minutes, seconds).formatted(Formatting.RED), true);
                AdorableHamsterPets.LOGGER.debug("[FeedAttempt {} Tick {}] Buff item used, but on cooldown ({} ticks remaining). Returning false.", this.getId(), world.getTime(), remainingTicks);
                return false; // Action failed due to cooldown
            } else {
                // Apply Buffs
                int duration = config.greenBeanBuffDuration.get();
                int speedAmplifier = config.greenBeanBuffAmplifierSpeed.get();
                int strengthAmplifier = config.greenBeanBuffAmplifierStrength.get();
                int absorptionAmplifier = config.greenBeanBuffAmplifierAbsorption.get();
                int regenAmplifier = config.greenBeanBuffAmplifierRegen.get();

                // --- Set "zoomies" state ---
                this.zoomiesIsClockwise = this.random.nextBoolean();
                this.lastZoomiesAngle = 0.0; // Reset angle on new buff application

                // Set Status Effects
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, speedAmplifier));
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, strengthAmplifier));
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, absorptionAmplifier));
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, regenAmplifier));

                // --- Set up zoomies state ---
                this.zoomiesIsClockwise = this.random.nextBoolean();
                this.zoomiesRadiusModifier = this.random.nextBetween(-2, 4);
                // Calculate and set the initial angle.
                double dx = this.getX() - player.getX();
                double dz = this.getZ() - player.getZ();
                this.lastZoomiesAngle = Math.atan2(dz, dx);

                // Play sound
                SoundEvent buffSound = getRandomSoundFrom(HAMSTER_CELEBRATE_SOUNDS, this.random);
                world.playSound(null, this.getBlockPos(), buffSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);

                // Set cooldown and duration
                long buffDurationEnd = currentTime + config.greenBeanBuffDuration.get();
                this.getDataTracker().set(GREEN_BEAN_BUFF_DURATION, buffDurationEnd);
                this.greenBeanBuffEndTick = currentTime + config.steamedGreenBeansBuffCooldown.get();

                actionTaken = true; // Action was successful
                AdorableHamsterPets.LOGGER.trace("[FeedAttempt {} Tick {}] Applied buffs. Duration ends at tick {}. Cooldown ends at tick {}.", this.getId(), world.getTime(), buffDurationEnd, this.greenBeanBuffEndTick);

                // Trigger Fed Steamed Beans Criterion
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ModCriteria.FED_HAMSTER_STEAMED_BEANS.trigger(serverPlayer, this);
                }
            }
        }

        // --- 4. Handle Standard Food (Healing/Breeding) ---
        else if (ConfigDataCache.isStandardFood(stack)) {
            if (canHeal) {
                this.heal(config.standardFoodHealing.get());
                actionTaken = true;
                AdorableHamsterPets.LOGGER.debug("[FeedAttempt {}] Healed with standard food.", this.getId());
            } else if (readyToBreed) {
                this.setSitting(false, true);
                this.setCustomInLove(player);
                this.setInLove(true);
                actionTaken = true;
                AdorableHamsterPets.LOGGER.debug("[FeedAttempt {}] Entered love mode with standard food.", this.getId());
            }
        }
        // If no other action was taken, it wasn't a valid feeding interaction in this context.
        if (!actionTaken) {
            AdorableHamsterPets.LOGGER.debug("[FeedAttempt {} Tick {}] Item {} was not a valid food for any action.",
                    this.getId(), world.getTime(), stack.getItem());
        }
        return actionTaken;
    }

    // --- Tamed Sleep Sequence Helper Methods ---
    /**
     * Checks if the conditions are met for a tamed, sitting hamster to potentially start becoming drowsy.
     * Conditions: Daytime (if configured), no nearby hostile entities, on solid ground, not in love mode.
     * @return True if conditions are met, false otherwise.
     */
    @Unique
    private boolean checkConditionsForInitiatingDrowsiness() {
        if (!this.isSitting()) return false; // Must be player-commanded to sit

        World world = this.getWorld();
        if (Configs.AHP.requireDaytimeForTamedSleep && !world.isDay()) {
            return false; // Must be daytime if config requires it
        }
        if (this.isInLove()) return false; // Cannot sleep if in love mode

        // Check for nearby hostile entities
        double threatRadius = Configs.AHP.tamedSleepThreatDetectionRadiusBlocks.get();
        List<LivingEntity> nearbyHostiles = world.getEntitiesByClass(
                LivingEntity.class,
                this.getBoundingBox().expand(threatRadius),
                entity -> entity instanceof HostileEntity && entity.isAlive() && !entity.isSpectator()
        );
        return nearbyHostiles.isEmpty(); // No hostiles nearby
    }

    /**
     * Checks if the conditions are met to sustain any phase of the slumber sequence (Drifting, Settling, Deep Sleep).
     * These are generally the same as initiating, but crucially, the hamster must *remain* sitting.
     * @return True if conditions are met, false otherwise.
     */
    @Unique
    private boolean checkConditionsForSustainingSlumber() {
        // Includes all checks from initiating, plus ensures it's still in a sitting pose.
        // The IS_SITTING datatracker is the primary driver for player-commanded sitting.
        return this.isSitting() && checkConditionsForInitiatingDrowsiness();
    }

    /**
     * Resets the hamster's sleep sequence state to NONE and clears associated timers.
     * Called when the sleep sequence is interrupted.
     * @param reason A debug message explaining why the sequence was reset.
     */
    @Unique
    private void resetSleepSequence(String reason) {
        AdorableHamsterPets.LOGGER.debug("Hamster {} resetting sleep sequence: {}. Current phase was: {}", this.getId(), reason, this.getDozingPhase());
        this.setDozingPhase(DozingPhase.NONE);
        this.quiescentSitDurationTimer = 0;
        this.driftingOffTimer = 0;
        this.settleSleepAnimationCooldown = 0;
    }

    /**
     * Called when this entity is removed from the world.
     * This override ensures that any server-side tracking or client-side sounds
     * associated with this specific hamster instance are properly cleaned up to prevent memory leaks.
     */
    @Override
    public void onRemoved() {
        // --- 1. Call Superclass Method ---
        super.onRemoved();

        // --- 2. Clean Up Trackers ---
        if (!this.getWorld().isClient()) {
            net.dawson.adorablehamsterpets.util.HamsterRenderTracker.onEntityUnload(this.getId());
        }
    }
}