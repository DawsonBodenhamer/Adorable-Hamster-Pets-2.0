package net.dawson.adorablehamsterpets.entity.custom;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.menu.MenuRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.component.HamsterShoulderData;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.AI.*;
import net.dawson.adorablehamsterpets.entity.ImplementedInventory;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderAnimationState;
import net.dawson.adorablehamsterpets.entity.control.HamsterBodyControl;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.mixin.accessor.LandPathNodeMakerInvoker;
import net.dawson.adorablehamsterpets.screen.HamsterScreenHandlerFactory;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

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
    private static final int INVENTORY_SIZE = 6;
    private static final int REFUSE_FOOD_TIMER_TICKS = 40;            // 2 seconds
    private static final int CUSTOM_LOVE_TICKS = 600;                 // 30 seconds
    private static final float THROW_DAMAGE = 20.0f;
    private static final double THROWN_GRAVITY = -0.05;
    private static final double HAMSTER_ATTACK_BOX_EXPANSION = 0.70D;  // Expand by 0.7 blocks horizontally (vanilla is 0.83 blocks, so really this is shrinking it)

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
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, THROW_DAMAGE)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, Configs.AHP.meleeDamage.get())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D);
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
            HamsterShoulderData.GreenBeanBuffData buffData = data.greenBeanBuffData();
            hamster.greenBeanBuffEndTick = buffData.greenBeanBuffEndTick();
            hamster.getDataTracker().set(GREEN_BEAN_BUFF_DURATION, buffData.greenBeanBuffDuration());
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
            }

            // --- 4. Load Status Effects ---
            // In 1.20.1, the data record holds the NbtList directly.
            NbtList effectsList = buffData.activeEffectsNbt();
            for (NbtElement effectElement : effectsList) {
                if (effectElement instanceof NbtCompound effectInstanceNbt) {
                    // StatusEffectInstance.fromNbt takes the compound directly.
                    StatusEffectInstance effectInstance = StatusEffectInstance.fromNbt(effectInstanceNbt);
                    if (effectInstance != null) {
                        hamster.addStatusEffect(effectInstance);
                    }
                }
            }

            // --- 5. Load Seeking Data ---
            HamsterShoulderData.SeekingBehaviorData seekingData = data.seekingBehaviorData();
            hamster.isPrimedToSeekDiamonds = seekingData.isPrimedToSeekDiamonds();
            hamster.foundOreCooldownEndTick = seekingData.foundOreCooldownEndTick();
            hamster.currentOreTarget = seekingData.currentOreTarget().orElse(null);

            // --- 6. Load Wander Mode/Bed Data ---
            HamsterShoulderData.WanderModeData wanderData = data.wanderModeData();

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
    public static final int STEALING_DIAMOND_FLAG = 1 << 14;
    public static final int TAUNTING_FLAG = 1 << 15;
    public static final int CELEBRATING_CHASE_FLAG = 1 << 16;
    public static final int IS_SHOULDER_PET_FLAG = 1 << 17;

    // --- Data Trackers ---
    private static final TrackedData<Integer> HAMSTER_FLAGS = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> ANIMATION_PERSONALITY_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> PINK_PETAL_TYPE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> DOZING_PHASE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<String> CURRENT_DEEP_SLEEP_ANIM_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> ACTIVE_CUSTOM_GOAL_NAME_DEBUG = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);
    public static final TrackedData<Integer> STEAL_DURATION_TIMER = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<ItemStack> STOLEN_ITEM_STACK = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    public static final TrackedData<Long> GREEN_BEAN_BUFF_DURATION = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.LONG);
    public static final TrackedData<Integer> CURRENT_LOOK_UP_ANIM_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> SHOULDER_ANIMATION_STATE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // --- Animation Constants ---
    private static final RawAnimation CRASH_ANIM = RawAnimation.begin().thenPlay("anim_hamster_crash");
    private static final RawAnimation KNOCKED_OUT_ANIM = RawAnimation.begin().thenPlay("anim_hamster_ko");
    private static final RawAnimation WAKE_UP_ANIM = RawAnimation.begin().thenPlay("anim_hamster_wakeup");
    private static final RawAnimation FLYING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_flying");
    private static final RawAnimation STATIONARY_HEADSHAKE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_stationary_headshake");
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
    private static final RawAnimation SEEKING_DIAMOND_ANIM = RawAnimation.begin().thenPlay("anim_hamster_seeking_diamond");
    private static final RawAnimation WANTS_TO_SEEK_DIAMOND_ANIM = RawAnimation.begin().thenPlay("anim_hamster_wants_to_seek_diamond");
    private static final RawAnimation DIAMOND_POUNCE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_diamond_pounce");
    private static final RawAnimation DIAMOND_TAUNT_ANIM = RawAnimation.begin().thenPlay("anim_hamster_diamond_taunt");
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
    @Unique public long stealCooldownEndTick = 0L;
    @Unique private int celebrationChaseTicks = 0;
    @Unique private boolean zoomiesIsClockwise = false;
    @Unique private double lastZoomiesAngle = 0.0;
    @Unique private int zoomiesRadiusModifier = 0;
    @Unique public transient float renderedSnowYOffset = 0.0f;
    @Unique public transient ShoulderLocation shoulderLocation = ShoulderLocation.RIGHT_SHOULDER;
    @Unique public int suffocationGracePeriod = 0;
    @Unique public transient float dynamicScaleY = 1.0f;


    // --- Inventory ---
    private final DefaultedList<ItemStack> items = ImplementedInventory.create(INVENTORY_SIZE);

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
    // --- End Auto-Eating Fields ---

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
    public void setThrown(boolean thrown) { setHamsterFlag(THROWN_FLAG, thrown); }
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
    public boolean isStealingDiamond() {return getHamsterFlag(STEALING_DIAMOND_FLAG);}
    public void setStealingDiamond(boolean stealing) {setHamsterFlag(STEALING_DIAMOND_FLAG, stealing);}
    public int getStealDurationTimer() {return this.dataTracker.get(STEAL_DURATION_TIMER);}
    public void setStealDurationTimer(int ticks) {this.dataTracker.set(STEAL_DURATION_TIMER, ticks);}
    public boolean isTaunting() {return getHamsterFlag(TAUNTING_FLAG);}
    public void setTaunting(boolean taunting) {setHamsterFlag(TAUNTING_FLAG, taunting);}
    public ItemStack getStolenItemStack() { return this.dataTracker.get(STOLEN_ITEM_STACK); }
    public void setStolenItemStack(ItemStack stack) { this.dataTracker.set(STOLEN_ITEM_STACK, stack); }
    public boolean isCelebratingChase() { return getHamsterFlag(CELEBRATING_CHASE_FLAG); }
    public void setCelebratingChase(boolean celebrating) { setHamsterFlag(CELEBRATING_CHASE_FLAG, celebrating); }
    public boolean hasGreenBeanBuff() {return this.getDataTracker().get(GREEN_BEAN_BUFF_DURATION) > this.getWorld().getTime();}
    public boolean getZoomiesIsClockwise() { return this.zoomiesIsClockwise; }
    public double getLastZoomiesAngle() { return this.lastZoomiesAngle; }
    public void setLastZoomiesAngle(double angle) { this.lastZoomiesAngle = angle; }
    public int getZoomiesRadiusModifier() { return this.zoomiesRadiusModifier; }
    public boolean isShoulderPet() { return getHamsterFlag(IS_SHOULDER_PET_FLAG); }
    public void setShoulderPet(boolean isShoulderPet) { setHamsterFlag(IS_SHOULDER_PET_FLAG, isShoulderPet); }

    // --- Inventory Implementation ---
    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void markDirty() {
        if (!this.getWorld().isClient()) {
            this.updateCheekTrackers();
        }
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
        // --- 1. Check if the item is allowed based on the disallowed logic ---
        // Ensure the slot index is valid for the hamster inventory (0-5)
        if (slot < 0 || slot >= INVENTORY_SIZE) {
            return false;
        }
        // Use the helper method to determine if the item is allowed
        return !this.isItemDisallowed(stack);
        // --- End 1. Check if the item is allowed based on the disallowed logic ---
    }
    // --- End isValid Override ---

    /**
     * Updates the DataTrackers for cheek fullness based on the inventory content.
     * Also triggers the "Chipmunk Aspirations" advancement if all pouch slots become full.
     */
    public void updateCheekTrackers() {
        // --- Update Left Cheek ---
        boolean leftFull = false;
        for (int i = 0; i < 3; i++) {
            if (!this.items.get(i).isEmpty()) {
                leftFull = true;
                break;
            }
        }

        // --- Update Right Cheek ---
        boolean rightFull = false;
        for (int i = 3; i < INVENTORY_SIZE; i++) {
            if (!this.items.get(i).isEmpty()) {
                rightFull = true;
                break;
            }
        }

        // --- Set Data Trackers ---
        if (this.isLeftCheekFull() != leftFull) this.setLeftCheekFull(leftFull);
        if (this.isRightCheekFull() != rightFull) this.setRightCheekFull(rightFull);
        // --- End Set Data Trackers ---

        // --- Trigger "Chipmunk Aspirations" Advancement ---
        if (!this.getWorld().isClient() && this.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
            boolean allSlotsFilled = true;
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                if (this.items.get(i).isEmpty()) {
                    allSlotsFilled = false;
                    break;
                }
            }
            if (allSlotsFilled) {
                ModCriteria.HAMSTER_POUCH_FILLED.trigger(serverPlayerOwner, this);
            }
        }
        // --- End Trigger ---
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

        // --- 5. Write Diamond Stealing Data ---
        if (this.isStealingDiamond()) {
            nbt.putBoolean("IsStealingDiamond", true);
            nbt.putInt("StealDurationTimer", this.getStealDurationTimer());
            // Save the stolen item stack using the 1.20.1 method
            if (!this.getStolenItemStack().isEmpty()) {
                nbt.put("StolenItemStack", this.getStolenItemStack().writeNbt(new NbtCompound()));
            }
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
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
        this.updateCheekTrackers();

        // --- 4. Read Seeking Data ---
        this.isPrimedToSeekDiamonds = nbt.getBoolean("IsPrimedToSeekDiamonds");
        this.foundOreCooldownEndTick = nbt.getLong("FoundOreCooldownEndTick");
        if (nbt.contains("OreTargetX") && nbt.contains("OreTargetY") && nbt.contains("OreTargetZ")) {
            this.currentOreTarget = new BlockPos(nbt.getInt("OreTargetX"), nbt.getInt("OreTargetY"), nbt.getInt("OreTargetZ"));
        } else {
            this.currentOreTarget = null;
        }

        // --- 5. Read Diamond Stealing Data ---
        this.setStealingDiamond(nbt.getBoolean("IsStealingDiamond"));
        if (this.isStealingDiamond()) {
            this.setStealDurationTimer(nbt.getInt("StealDurationTimer"));
            if (nbt.contains("StolenItemStack", NbtElement.COMPOUND_TYPE)) {
                // Use the 1.20.1 method to read the ItemStack from NBT
                this.setStolenItemStack(ItemStack.fromNbt(nbt.getCompound("StolenItemStack")));
            }
        } else {
            this.setStealDurationTimer(0);
            this.setStolenItemStack(ItemStack.EMPTY);
        }
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

        // --- Create GreenBeanBuffData ---
        HamsterShoulderData.GreenBeanBuffData buffData = new HamsterShoulderData.GreenBeanBuffData(
                this.greenBeanBuffEndTick,
                this.getDataTracker().get(GREEN_BEAN_BUFF_DURATION),
                effectsList
        );

        // --- 3. Get Custom Name ---
        Optional<String> nameOptional = Optional.ofNullable(this.getCustomName()).map(Text::getString);

        // --- 4. Create SeekingBehaviorData instance ---
        HamsterShoulderData.SeekingBehaviorData seekingData = new HamsterShoulderData.SeekingBehaviorData(
                this.isPrimedToSeekDiamonds,
                this.foundOreCooldownEndTick,
                Optional.ofNullable(this.currentOreTarget)
        );

        // --- 5. Create WanderModeData ---
        HamsterShoulderData.WanderModeData wanderData = HamsterShoulderData.WanderModeData.empty();

        // --- 6. Create and Return the Main Data Record ---
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
                this.dataTracker.get(HAMSTER_FLAGS)
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
        // --- Stage 1: Initial Target Check ---
        if (isSafeSpawnLocation(initialTarget, world)) {
            return Optional.of(initialTarget);
        }

        // --- Stage 2: Vertical Vicinity Check (Upwards) ---
        for (int i = 1; i <= 3; i++) {
            BlockPos abovePos = initialTarget.up(i);
            if (isSafeSpawnLocation(abovePos, world)) {
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
                    if (isSafeSpawnLocation(checkPos, world)) {
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
        // --- 1. Reset Sleep Sequence if Standing Up from a Doze/Sleep ---
        if (!sitting && this.isTamed() && this.getDozingPhase() != DozingPhase.NONE) {
            resetSleepSequence("Player commanded hamster to stand up.");
        }

        // --- 2. Update Core Sitting State ---
        setHamsterFlag(SITTING_FLAG, sitting);

        // --- 3. Update Vanilla State ---
        this.setInSittingPose(sitting);

        // --- 4. Manage Cleaning Timers and Quiescent Sit Timer on State Change ---
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
        // --- Description: Calculate and check intersection with a smaller attack box ---
        // Get the hamster's current bounding box
        Box hamsterBox = this.getBoundingBox();
        // Expand it horizontally by the custom smaller amount
        Box attackBox = hamsterBox.expand(HAMSTER_ATTACK_BOX_EXPANSION, 0.0D, HAMSTER_ATTACK_BOX_EXPANSION);
        // Check if this smaller attack box intersects the target's hitbox
        boolean intersects = attackBox.intersects(entity.getBoundingBox());
        return intersects;
        // --- End Description ---
    }
    // --- End Override ---

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
        // --- End 6. Default: Allow Attack ---
    }

    // --- Interaction Logic ---
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        // --- 1. Initial Setup ---
        ItemStack stack = player.getStackInHand(hand);
        World world = this.getWorld();
        AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Interaction start. Player: {}, Hand: {}, Item: {}", this.getId(), world.getTime(), player.getName().getString(), hand, stack.getItem());

        // --- 2. Interaction Cooldown Check ---
        if (this.interactionCooldown > 0) {
            AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Interaction cooldown active ({} ticks left). Passing.", this.getId(), world.getTime(), this.interactionCooldown);
            return ActionResult.PASS;
        }

        // --- 3. Toggle Jade Debug with Guide Book ---
        if (player.isSneaking() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
            if (!world.isClient) { // Server-side logic
                AhpConfig currentConfig = AdorableHamsterPets.CONFIG;
                boolean currentSetting = currentConfig.enableJadeHamsterDebugInfo;
                boolean newSetting = !currentSetting;

                currentConfig.enableJadeHamsterDebugInfo = newSetting;
                currentConfig.save(); // Save the config to file

                Text message = Text.translatable(
                        newSetting ? "message.adorablehamsterpets.debug_overlay_enabled" : "message.adorablehamsterpets.debug_overlay_disabled"
                ).formatted(newSetting ? Formatting.GREEN : Formatting.RED);
                player.sendMessage(message, true); // Send to action bar

                AdorableHamsterPets.LOGGER.info("Player {} toggled Jade Hamster Debug Info via Guide Book to: {} for hamster {}", player.getName().getString(), newSetting, this.getId());
            }
            return ActionResult.success(world.isClient()); // Consume the action
        }

        // --- 5. Taming Logic ---
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

        // --- 5. Owner Interaction Logic ---
        if (this.isOwner(player)) {
            AdorableHamsterPets.LOGGER.trace("[InteractMob {} Tick {}] Player is owner. Processing owner interactions.", this.getId(), world.getTime());
            boolean isSneaking = player.isSneaking();
            PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

            // --- Reset Sleep Sequence if Dozing ---
            if (this.getDozingPhase() != DozingPhase.NONE) {
                resetSleepSequence("Player attempted to feed hamster.");
            }

            // --- Handle Diamond Stealing Interaction ---
            if (this.isStealingDiamond() && this.isOwner(player)) {
                AdorableHamsterPets.LOGGER.trace("[InteractMob-{}] Passed 'isStealingDiamond' check.", this.getId());
                if (!world.isClient) {
                    ItemStack retrievedStack = this.getStolenItemStack().copy();
                    player.getInventory().offerOrDrop(this.getStolenItemStack().copy());
                    this.setStolenItemStack(ItemStack.EMPTY);
                    this.setStealDurationTimer(0);
                    this.setStealingDiamond(false);
                    // Set the state flag and initialize the timer so the tick() method can handle the rotation.
                    this.setCelebratingChase(true);
                    this.celebrationChaseTicks = 30; // 1.5 second duration
                    this.triggerAnimOnServer("mainController", "anim_hamster_celebrate_chase");
                    // Play a happy/affectionate and diamond "tink" sound
                    world.playSound(null, this.getBlockPos(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, this.random), SoundCategory.NEUTRAL, 1.0f, this.getSoundPitch());
                    // Get and play the dynamic sound
                    if (!retrievedStack.isEmpty()) {
                        SoundEvent pounceSound = ModSounds.getDynamicItemSound(retrievedStack);
                        float volume = (pounceSound == SoundEvents.ENTITY_GENERIC_EAT) ? 0.35f : 1.0f;
                        world.playSound(null, this.getBlockPos(), pounceSound, SoundCategory.NEUTRAL, volume, 1.7f);
                    }
                    AdorableHamsterPets.LOGGER.trace("[InteractMob-{}] Diamond returned to player and goal stopped.", this.getId());
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
                    this.triggerAnimOnServer("mainController", "wakeup");
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

            // --- Pink Petal Application/Cycling ---
            if (stack.isOf(Items.PINK_PETALS) && !player.isSneaking()) {
                if (!world.isClient) {
                    int currentPetalType = this.dataTracker.get(PINK_PETAL_TYPE);
                    int nextPetalType = (currentPetalType % 3) + 1; // Cycles 0->1, 1->2, 2->3, 3->1
                    this.dataTracker.set(PINK_PETAL_TYPE, nextPetalType);
                    world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_PINK_PETALS_PLACE, SoundCategory.PLAYERS, 0.7f, 1.0f + random.nextFloat() * 0.2f);
                    if (world instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                                this.getX(), this.getY() + this.getHeight() * 0.75, this.getZ(),
                                7, (this.getWidth() / 2.0F), (this.getHeight() / 2.0F), (this.getWidth() / 2.0F), 0.0);
                    }
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    AdorableHamsterPets.LOGGER.trace("[InteractMob {}] Cycled/Applied pink petal to type {}.", this.getId(), nextPetalType);
                    // Trigger advancement criterion
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        ModCriteria.APPLIED_PINK_PETAL.trigger(serverPlayer, this);
                    }
                }
                return ActionResult.success(world.isClient()); // Consume interaction
            }

            // --- Pink Petal Removal with Shears ---
            if (stack.isOf(Items.SHEARS) && !player.isSneaking()) {
                if (this.dataTracker.get(PINK_PETAL_TYPE) > 0) { // Only if petals are currently applied
                    if (!world.isClient) {
                        this.dataTracker.set(PINK_PETAL_TYPE, 0); // Remove petals
                        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 0.9f, 1.0f + random.nextFloat() * 0.1f);
                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PINK_PETALS)),
                                    this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
                                    5, (this.getWidth() / 2.0F), (this.getHeight() / 2.0F), (this.getWidth() / 2.0F), 0.05);
                        }
                        // Drop one pink petal
                        ItemScatterer.spawn(world, this.getX(), this.getY() + 0.5, this.getZ(), new ItemStack(Items.PINK_PETALS, 1));

                        if (!player.getAbilities().creativeMode) {
                            // For 1.20.1 - Determine EquipmentSlot based on the hand used.
                            stack.damage(1, player, (p) -> p.sendEquipmentBreakStatus(hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                        }
                        AdorableHamsterPets.LOGGER.trace("[InteractMob {}] Removed pink petals with shears.", this.getId());
                    }
                    return ActionResult.success(world.isClient()); // Consume interaction
                }
            }

            // --- Shoulder Mounting Logic ---
            boolean isUsingItem = ConfigDataCache.isShoulderMountFood(stack);
            boolean isUsingKeybind = !world.isClient() && Configs.AHP.enableShoulderMountKeybind && ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY.isPressed();

            if (isUsingItem || isUsingKeybind) {
                if (!world.isClient) {
                    // --- Find First Available Slot ---
                    ShoulderLocation availableSlot = null;
                    if (playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty()) {
                        availableSlot = ShoulderLocation.RIGHT_SHOULDER;
                    } else if (playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                        availableSlot = ShoulderLocation.LEFT_SHOULDER;
                    } else if (playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty()) {
                        availableSlot = ShoulderLocation.HEAD;
                    }

                    if (availableSlot != null) {
                        AdorableHamsterPets.LOGGER.debug("[AHP DEBUG] MOUNTING: Found available slot '{}' for Hamster ID {}.", availableSlot, this.getId());

                        // --- Save, Set, and Update Queue ---
                        HamsterShoulderData data = this.saveToShoulderData();
                        playerAccessor.setShoulderHamster(availableSlot, data.toNbt());
                        playerAccessor.adorablehamsterpets$getMountOrderQueue().addLast(availableSlot);

                        BlockPos hamsterPosForMountSound = this.getBlockPos();
                        this.discard(); // Remove hamster from world

                        // --- Trigger Generic Events and Play Mount Sound ---
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            ModCriteria.HAMSTER_ON_SHOULDER.trigger(serverPlayer);
                        }
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_mount_success"), true);

                        SoundEvent mountSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SHOULDER_MOUNT_SOUNDS, this.random);
                        if (mountSound != null) {
                            world.playSound(null, player.getBlockPos(), mountSound, SoundCategory.PLAYERS, 1.0f, this.getSoundPitch());
                        }

                        // --- Item-Specific Effects and Consumption ---
                        if (isUsingItem) {
                            SoundEvent mountLureSound = ModSounds.getDynamicItemSound(stack);
                            world.playSound(null, hamsterPosForMountSound, mountLureSound, SoundCategory.PLAYERS, 1.0f, 1.0f);

                            ((ServerWorld)world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack),
                                    hamsterPosForMountSound.getX() + 0.5, hamsterPosForMountSound.getY() + 0.5, hamsterPosForMountSound.getZ() + 0.5,
                                    8, 0.25D, 0.25D, 0.25D, 0.05);

                            if (!player.getAbilities().creativeMode && Configs.AHP.consumeShoulderMountItem) {
                                stack.decrement(1);
                            }
                        }
                    } else {
                        player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_occupied"), true);
                    }
                } else {
                    player.swingHand(hand);
                }
                return ActionResult.CONSUME;
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
            if (!isSneaking && !isPotentialFood && !ConfigDataCache.isShoulderMountFood(stack) && !stack.isOf(Items.PINK_PETALS)) {
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
                this.setSitting(!this.isSitting()); // Toggle sitting state
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
        // --- 1. Decrement Timers ---
        if (this.interactionCooldown > 0) this.interactionCooldown--;
        if (this.suffocationGracePeriod > 0) this.suffocationGracePeriod--;
        // --- Cleaning Timer Logic ---
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
        // --- End Cleaning Timer Logic ---
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
        // --- Logic for Handling Cleaning State ---
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
        // --- End Logic for Handling Cleaning State ---
        // --- End 1. Decrement Timers ---

        // --- Post-Chase Celebration State Logic ---
        if (this.isCelebratingChase()) {
            if (this.celebrationChaseTicks > 0) {
                if (this.getOwner() != null) {
                    this.getLookControl().lookAt(this.getOwner(), FAST_YAW_CHANGE, FAST_PITCH_CHANGE);
                }
                this.celebrationChaseTicks--;
            } else {
                this.setCelebratingChase(false);
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
                // --- 2a. Block Collision Handling ---
                net.minecraft.util.hit.BlockHitResult blockHitResult = (net.minecraft.util.hit.BlockHitResult) blockHit;
                BlockPos adjacentPos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());

                // Place the hamster in the air next to the impacted block face.
                this.setPosition(adjacentPos.getX() + 0.5, adjacentPos.getY(), adjacentPos.getZ() + 0.5);

                // Apply the "tumble" state immediately. Vanilla gravity will handle the fall.
                this.setVelocity(currentVel.multiply(0.6, 0.0, 0.6));
                this.setThrown(false);
                this.playSound(SoundEvents.ENTITY_GENERIC_SMALL_FALL, 1.0f, 1.2f);
                this.setKnockedOut(true);
                this.setInSittingPose(true);
                if (!world.isClient()) {
                    this.triggerAnimOnServer("mainController", "crash");
                }
                stopped = true;
                // --- End 2a. Block Collision Handling ---

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

                        // --- THROW DAMAGE LOGIC ---
                        // 1. Create a DamageSource where the thrown hamster is the attacker.
                        DamageSource damageSource = this.getDamageSources().mobAttack(this);
                        // 2. Get the damage amount from the config.
                        float damageAmount = Configs.AHP.hamsterThrowDamage.get().floatValue();
                        // 3. Deal the damage to the target using the correct source.
                        boolean damaged = livingHit.damage(damageSource, damageAmount);

                        if (damaged) {
                            livingHit.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20, 0, false, false, false));
                            playEffects = true;
                        }
                    } else {
                        playEffects = true;
                    }

                    if (playEffects) {
                        // For 1.20.1, call .get() on the RegistrySupplier
                        world.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.HAMSTER_IMPACT.get(), SoundCategory.NEUTRAL, 1.0F, 1.0F);
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
                    // --- End 2b. Entity Collision Handling ---
                }
            }

            // Apply gravity, update position, and spawn trail particles if still thrown
            if (this.isThrown() && !stopped) {
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
        // --- End 2. Thrown State Logic ---

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
                        resetSleepSequence("Deep sleep interrupted: conditions for slumber no longer met.");
                    }
                    // Hamster remains in deep sleep, looping animation, until interrupted
                    break;
            }
        }
        // --- End 3. Tamed Hamster "Path to Slumber" State Machine ---

        // Call super.tick() *after* processing thrown state and timers
        super.tick();

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
        // --- END Apply extra gravity during sulking jump ---

        // --- 4. Server-Side Logic ---
        World world = this.getWorld();
        if (!world.isClient()) {

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
                boolean ejectedItem = false; // Flag to only eject one item per cycle

                for (int i = 0; i < this.items.size(); ++i) {
                    ItemStack stack = this.items.get(i);
                    if (!stack.isEmpty() && this.isItemDisallowed(stack)) {
                        AdorableHamsterPets.LOGGER.warn("[HamsterTick {}] Ejecting disallowed item {} from slot {}.", this.getId(), stack.getItem(), i);
                        // Drop the item at the hamster's feet
                        ItemScatterer.spawn(world, this.getX(), this.getY(), this.getZ(), stack.copy());
                        // Remove it from the inventory
                        this.items.set(i, ItemStack.EMPTY);
                        // Mark dirty and update visuals
                        this.markDirty(); // This calls updateCheekTrackers
                        ejectedItem = true;
                        break; // Eject only one item per check cycle
                    }
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
            // --- End Stage 1 ---

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
            // --- End Stage 2 ---

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
            // --- End Stage 3 ---
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
            // --- End 4c. ---

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
            // --- End 4d. ---
        }
        // --- End 4. Server-Side Logic ---

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
        // --- End 5.1 Buff Particle Logic ---

        // --- 5.2 Taunting Particle Logic ---
        if (this.isTaunting()) {
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
        // --- End 5.2 Taunting Particle Logic ---
        // --- End 5. Client-Side Logic ---

        // --- 6. Other Non-Movement Tick Logic ---
        if (this.isRefusingFood() && refuseTimer > 0) { if (--refuseTimer <= 0) this.setRefusingFood(false); }
        if (tamingCooldown > 0) tamingCooldown--;
        if (customLoveTimer > 0) customLoveTimer--;
        if (customLoveTimer <= 0 && this.isInLove()) this.setInLove(false);
        // --- End 6. Other Non-Movement Tick Logic ---
    }

    @Override public boolean canMoveVoluntarily() { return super.canMoveVoluntarily() && !this.isThrown(); }
    @Override public boolean isPushable() { return super.isPushable() && !this.isThrown(); }

    // --- Override onDeath to Drop Inventory ---
    @Override
    public void onDeath(DamageSource source) {
        // --- 1. Drop Cheek Pouch Inventory ---
        World world = this.getWorld(); // Get the world instance
        if (!world.isClient()) {
            // Iterate through the items list and drop each non-empty stack
            for (ItemStack stack : this.items) {
                if (!stack.isEmpty()) {
                    // Use ItemScatterer to drop the stack at the hamster's position
                    ItemScatterer.spawn(world, this.getX(), this.getY(), this.getZ(), stack);
                }
            }
            // Clear the internal list after dropping
            this.items.clear();
            // Update cheek trackers one last time
            this.updateCheekTrackers();
        }
        // --- End 1. Drop Cheek Pouch Inventory ---

        // Call the superclass method AFTER dropping items
        super.onDeath(source);
    }
    // --- End Override ---

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
     * <p>Several animations like attack, crash, wakeup, and the settle animations are registered
     * as triggerable and will interrupt the main looping state when fired via {@link #triggerAnimOnServer}.</p>
     *
     * @param controllers The registrar for adding animation controllers.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mainController", 2, event -> {

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
                    default -> { // STANDING
                        // "Sticky" logic: If already playing an idle anim, keep it. Otherwise, pick a new one.
                        RawAnimation current = event.getController().getCurrentRawAnimation();
                        if (current != null && (current.equals(IDLE1_ANIM) || current.equals(IDLE2_ANIM))) {
                            yield event.setAndContinue(current);
                        }
                        yield event.setAndContinue(this.random.nextBoolean() ? IDLE1_ANIM : IDLE2_ANIM);
                    }
                };
            }

            // --- Animation Logic for In-World Hamsters ---
            // --- Knocked Out State ---
            if (this.isKnockedOut()) {return event.setAndContinue(KNOCKED_OUT_ANIM);}
            // --- Sulking State ---
            if (this.isSulking()) {return event.setAndContinue(SULKING_ANIM);}
            // --- Thrown State ---
            if (this.isThrown()) {return event.setAndContinue(FLYING_ANIM);}
            // --- Diamond Stealing / Taunting State ---
            if (this.isTaunting()) return event.setAndContinue(DIAMOND_TAUNT_ANIM);
            // --- Seeking/Wanting to Seek Diamond/Ore State ---
            boolean isSeekingGoalActive = false;
            String activeGoalName = this.getActiveCustomGoalDebugName();
            if (activeGoalName.startsWith(HamsterSeekDiamondGoal.class.getSimpleName())) {
                isSeekingGoalActive = true;
            }
            if (isSeekingGoalActive) {
                double horizontalSpeedSquared = this.getVelocity().horizontalLengthSquared();
                if (horizontalSpeedSquared > 1.0E-6) { // Use a very small threshold to detect any movement
                    return event.setAndContinue(SEEKING_DIAMOND_ANIM); // Hamster is moving
                } else {
                    return event.setAndContinue(WANTS_TO_SEEK_DIAMOND_ANIM); // Hamster is not moving
                }
            }

            // --- Found Diamond Celebration ---
            if (this.isCelebratingDiamond()) {
                return event.setAndContinue(BEGGING_ANIM); // Reuse begging animation for celebration
            }
            // --- End Seeking/Wanting to Seek Diamond Ore State ---

                    // --- Tamed Hamster Sleep Sequence ---
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


                    // --- Wild Hamster Sleeping State ---
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
                .triggerableAnim("wakeup", WAKE_UP_ANIM)
                .triggerableAnim("stationary_headshake", STATIONARY_HEADSHAKE_ANIM)
                .triggerableAnim("moving_headshake", MOVING_HEADSHAKE_ANIM)
                .triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("anim_hamster_sit_settle_sleep1", SIT_SETTLE_SLEEP1_ANIM)
                .triggerableAnim("anim_hamster_sit_settle_sleep2", SIT_SETTLE_SLEEP2_ANIM)
                .triggerableAnim("anim_hamster_sit_settle_sleep3", SIT_SETTLE_SLEEP3_ANIM)
                .triggerableAnim("anim_hamster_stand_settle_sleep1", STAND_SETTLE_SLEEP1_ANIM)
                .triggerableAnim("anim_hamster_stand_settle_sleep2", STAND_SETTLE_SLEEP2_ANIM)
                .triggerableAnim("anim_hamster_stand_settle_sleep3", STAND_SETTLE_SLEEP3_ANIM)
                .triggerableAnim("anim_hamster_sulk", SULK_ANIM)
                .triggerableAnim("anim_hamster_diamond_pounce", DIAMOND_POUNCE_ANIM)
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

    // --- Helper method to trigger animations server-side ---
    // Needs to be called from server-side logic (tick, interactMob, goals)
    public void triggerAnimOnServer(String controllerName, String animName) {
        if (!this.getWorld().isClient()) { // Ensure we're on the server
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            // Use the GeoAnimatable's built-in method for triggering server-side
            this.triggerAnim(controllerName, animName);
            // The library handles the synchronization to clients automatically.
            AdorableHamsterPets.LOGGER.trace("[HamsterEntity {}] Triggered server-side animation: Controller='{}', Anim='{}'", this.getId(), controllerName, animName);
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
        this.dataTracker.startTracking(STEAL_DURATION_TIMER, 0);
        this.dataTracker.startTracking(STOLEN_ITEM_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(GREEN_BEAN_BUFF_DURATION, 0L);
        this.dataTracker.startTracking(CURRENT_LOOK_UP_ANIM_ID, 1);
        this.dataTracker.startTracking(SHOULDER_ANIMATION_STATE, ShoulderAnimationState.STANDING.ordinal());
    }

    // --- AI Goals ---
    @Override
    protected void initGoals() {
        AdorableHamsterPets.LOGGER.debug("[AI Init {} Tick {}] Initializing goals. Current State: isSleeping={}, isSittingPose={}",
                this.getId(), this.getWorld().isClient ? "ClientTick?" : this.getWorld().getTime(), this.isSleeping(), this.isInSittingPose());
        // --- 1. Initialize Goals ---
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new HamsterSeekDiamondGoal(this));
        this.goalSelector.add(1, new HamsterStealDiamondGoal(this));
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
        // --- End Target Selector Goals ---
        // --- End 1. Initialize Goals ---
        AdorableHamsterPets.LOGGER.debug("[AI Init {} Tick {}] Finished initializing goals.",
                this.getId(), this.getWorld().isClient ? "ClientTick?" : this.getWorld().getTime());
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
        // --- End 1. Check if Tamed ---

        // --- 3. Default Behavior ---
        super.setTarget(target);
    }

    // --- Sounds ---
    @Override
    protected SoundEvent getAmbientSound() {
        // --- 0. Knocked Out Check (Silence) ---
        if (this.isKnockedOut()) {
            return null; // Knocked out hamsters make no ambient sounds
        }
        // --- 1. Begging/Taunting Sounds ---
        if (this.isBegging() || this.isTaunting()) {
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
        // --- End 1. Check if Entity Can Be Hit ---
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

        // Always update cheek trackers on initialization
        this.updateCheekTrackers();

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
     * The logic follows a clear priority:
     * 1. If the item is on the `pouchAllowedItems` config list, it is ALWAYS allowed.
     * 2. If the item is on the `pouchDisallowedItems` or `pouchDisallowedTags` config lists, it is disallowed.
     * 3. If the item is a `BlockItem` (and wasn't on the allow list), it is disallowed.
     * 4. If the item is a `SpawnEggItem`, it is disallowed.
     *
     * @param stack The ItemStack to check.
     * @return True if the item is disallowed, false otherwise.
     */
    public boolean isItemDisallowed(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 1. Explicit ALLOW list has highest priority.
        if (ConfigDataCache.isPouchAllowed(stack)) {
            return false; // It's allowed, so it's NOT disallowed.
        }

        // 2. Check the new DISALLOW lists from config.
        if (ConfigDataCache.isPouchDisallowed(stack)) {
            return true;
        }

        // 3. Allow any item that is considered food.
        if (stack.getItem().isFood()) { // Use isFood() method for 1.20.1
            return false; // It's food, so it's NOT disallowed.
        }

        Item item = stack.getItem();

        // 4. Global block-item rule (a general disallow).
        if (item instanceof BlockItem) {
            // Any block is disallowed by default unless it was on the allowlist.
            return true;
        }

        // 5. Spawn eggs are always disallowed.
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
            // --- End Use Config Value ---
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
        // --- End 1. Taming Attempt ---
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
        if (!this.isOnGround()) return false; // Must be on safe, solid ground

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