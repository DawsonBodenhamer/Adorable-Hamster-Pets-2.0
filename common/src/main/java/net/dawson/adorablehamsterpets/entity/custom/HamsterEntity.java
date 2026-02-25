package net.dawson.adorablehamsterpets.entity.custom;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.MountPriority;
import net.dawson.adorablehamsterpets.entity.AI.*;
import net.dawson.adorablehamsterpets.entity.AI.navigation.HamsterNavigation;
import net.dawson.adorablehamsterpets.entity.ImplementedInventory;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderAnimationState;
import net.dawson.adorablehamsterpets.entity.control.HamsterBodyControl;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.*;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
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

import static net.dawson.adorablehamsterpets.sound.ModSounds.HAMSTER_CELEBRATE_SOUNDS;
import static net.dawson.adorablehamsterpets.sound.ModSounds.getRandomSoundFrom;


public class HamsterEntity extends TameableEntity implements GeoEntity, ImplementedInventory {


    /* ──────────────────────────────────────────────────────────────────────────────
     *                    1. Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Constants ---
    private static final double WALK_TO_RUN_THRESHOLD_SQUARED = 0.002;
    private static final double RUN_TO_SPRINT_THRESHOLD_SQUARED = 0.008;
    private static final int CUSTOM_LOVE_TICKS = 600;                 // 30 seconds
    private static final double HAMSTER_ATTACK_BOX_EXPANSION = 0.70D;  // Expand by 0.7 blocks horizontally (vanilla is 0.83 blocks, so really this is shrinking it)
    private static final int NORMAL_FALL_PITCH_DURATION = 15;
    private static final int PITCH_RESET_DURATION = 3;

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

    public static final int CELEBRATION_PARTICLE_DURATION_TICKS = 600;
    private static final float DEFAULT_FOOTSTEP_VOLUME = 0.10F;
    private static final float GRAVEL_VOLUME_MODIFIER = 0.60F;

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
        HamsterEntity hamster = preconfiguredHamster != null ? preconfiguredHamster : HamsterNbtUtil.createFromNbt(world, player, nbt);
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

        // --- 3. Find Safe Spawn Position ---
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

        // Determine pos with safe spawning algorithm
        Optional<BlockPos> safePosOpt = HamsterPlacementUtil.findSafeSpawnPosition(initialSearchPos, world, 5, hamster);

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
    public static final int HOLDING_MOUTH_ITEM_FLAG = 1 << 14;
    public static final int TAUNTING_FLAG = 1 << 15;
    public static final int PRESENTING_ITEM_FLAG = 1 << 20;
    public static final int CELEBRATING_RETRIEVAL_FLAG = 1 << 16;
    public static final int IS_SHOULDER_PET_FLAG = 1 << 17;
    public static final int IS_WANDER_MODE_ACTIVE_FLAG = 1 << 18;
    public static final int ON_THE_WAY_TO_BED_FLAG = 1 << 19;
    public static final int STUCK_SEARCHING_FOR_BED_FLAG = 1 << 21;
    public static final int RESCUE_SLEEPING_FLAG = 1 << 22;
    public static final int IS_PLAYING_TAG_FLAG = 1 << 23;

    // --- Data Trackers ---
    public static final TrackedData<Integer> HAMSTER_FLAGS = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> VARIANT = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> ANIMATION_PERSONALITY_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> PINK_PETAL_TYPE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> DOZING_PHASE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<String> CURRENT_DEEP_SLEEP_ANIM_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);
    public static final TrackedData<Integer> GENERIC_INTERACTION_TIMER = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<ItemStack> MOUTH_ITEM_STACK = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    public static final TrackedData<Long> GREEN_BEAN_BUFF_DURATION = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.LONG);
    public static final TrackedData<Integer> CURRENT_LOOK_UP_ANIM_ID = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> SHOULDER_ANIMATION_STATE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> TRACKED_ACCESSORY_STACK = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<ItemStack> TRACKED_ARMOR_STACK = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> FALL_IMMUNITY_ACTIVE = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> ACTIVE_CUSTOM_GOAL_NAME_DEBUG = DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);

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
    private static final RawAnimation TAUNTING_ANIM = RawAnimation.begin().thenPlay("anim_hamster_taunt_with_item");
    private static final RawAnimation PRESENTING_ITEM_ANIM = RawAnimation.begin().thenPlay("anim_hamster_presenting_item");
    private static final RawAnimation CELEBRATE_CHASE_ANIM = RawAnimation.begin().thenPlay("anim_hamster_celebrate_chase");
    private static final RawAnimation CHEEK_UNLOAD_ANIM = RawAnimation.begin().thenPlay("anim_hamster_cheek_unload");
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
    @Unique public long stealingCooldownEndTick = 0L;
    @Unique private int celebrationRetrievalTicks = 0;
    @Unique private Entity celebrationTarget = null;
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
    @Unique private int localSpawnImmunityTicks = 60;
    @Unique public long tagGameCooldownEndTick = 0L;

    // --- Inventory ---
    private final DefaultedList<ItemStack> items = ImplementedInventory.create(HamsterInventoryUtil.INVENTORY_SIZE);

    // --- Armor Tracking ---
    private ItemStack lastArmorStack = ItemStack.EMPTY;

    // --- Animation ---
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final HamsterAnimationScheduler animScheduler = new HamsterAnimationScheduler();

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
    public void enableZoomies(PlayerEntity player) {
        this.zoomiesIsClockwise = this.random.nextBoolean();
        this.zoomiesRadiusModifier = this.random.nextBetween(-2, 4);
        // Calculate and set initial angle based on where the player is.
        double dx = this.getX() - player.getX();
        double dz = this.getZ() - player.getZ();
        this.lastZoomiesAngle = Math.atan2(dz, dx);
    }
    public void setCelebrationTarget(Entity target) { this.celebrationTarget = target; }
    public void setCelebrationRetrievalTicks(int ticks) { this.celebrationRetrievalTicks = ticks; }
    public void setSilentInventoryUpdate(boolean silent) { this.isSilentInventoryUpdate = silent; }
    public boolean hasPlayedIncomingSound() { return this.hasPlayedIncomingSound; }
    public void setHasPlayedIncomingSound(boolean value) { this.hasPlayedIncomingSound = value; }
    public int getQuiescentSitTimer() { return this.quiescentSitDurationTimer; }
    public void setQuiescentSitTimer(int ticks) { this.quiescentSitDurationTimer = ticks; }
    public int getDriftingOffTimer() { return this.driftingOffTimer; }
    public void setDriftingOffTimer(int ticks) { this.driftingOffTimer = ticks; }
    public int getSettleSleepCooldown() { return this.settleSleepAnimationCooldown; }
    public void setSettleSleepCooldown(int ticks) { this.settleSleepAnimationCooldown = ticks; }
    public void setCurrentDeepSleepAnimId(String animId) { this.dataTracker.set(CURRENT_DEEP_SLEEP_ANIM_ID, animId); }
    public ItemStack getLastFoodItem() { return this.lastFoodItem; }
    public void setLastFoodItem(ItemStack stack) { this.lastFoodItem = stack; }
    public long getGreenBeanBuffEndTick() { return this.greenBeanBuffEndTick; }
    public void setGreenBeanBuffEndTick(long tick) { this.greenBeanBuffEndTick = tick; }
    public void setRefuseTimer(int ticks) { this.refuseTimer = ticks; }
    public boolean isCheekPouchUnlocked() { return getHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG); }
    public void setCheekPouchUnlocked(boolean unlocked) { setHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG, unlocked); }
    public int getVariant() { return this.dataTracker.get(VARIANT); }
    public void setVariant(int variantId) { this.dataTracker.set(VARIANT, variantId); }
    public boolean isSleeping() { return getHamsterFlag(SLEEPING_FLAG); }
    public void setSleeping(boolean sleeping) { setHamsterFlag(SLEEPING_FLAG, sleeping); }
    public int getAutoEatCooldownTicks() { return this.autoEatCooldownTicks; }
    public void setAutoEatCooldownTicks(int ticks) { this.autoEatCooldownTicks = ticks; }
    public int getEjectionCheckCooldown() { return this.ejectionCheckCooldown; }
    public void setEjectionCheckCooldown(int ticks) { this.ejectionCheckCooldown = ticks; }
    public void setLoadingNbt(boolean loading) { this.isLoadingNbt = loading; }
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
    public boolean tryTame(PlayerEntity player, ItemStack itemStack) {
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
    // --- Riding State Accessors ---
    public int getRiderJumpCooldown() { return this.riderJumpCooldown; }
    public void setRiderJumpCooldown(int ticks) { this.riderJumpCooldown = ticks; }
    public boolean isRiderJumpHeld() { return this.riderJumpHeld; }
    public void setRiderJumpHeld(boolean held) { this.riderJumpHeld = held; }
    public boolean isRiderJumpQueued() { return this.riderJumpQueued; }
    public void setRiderJumpQueued(boolean queued) { this.riderJumpQueued = queued; }
    public boolean isRiderSprintHeld() { return this.riderSprintHeld; }
    public void setRiderSprintHeld(boolean held) { this.riderSprintHeld = held; }
    // --- Riding Protected Wrappers ---
    public void delegateTravel(Vec3d movementInput) {
        super.travel(movementInput);
    }
    public void delegateSetRotation(float yaw, float pitch) {
        this.setRotation(yaw, pitch);
    }
    public void executeJump() {
        this.jump();
    }
    /**
     * Handles the logic when a player successfully right-clicks a hamster playing tag.
     * Stops the game, plays a celebration animation, and then schedules a gift sequence.
     */
    public void concludeTagGame(PlayerEntity player) {
        // 1. Stop Goal & Clear State
        this.setPlayingTag(false);
        this.setTaunting(false);
        this.getNavigation().stop();
        // Clear debug name
        if (this.getActiveCustomGoalDebugName().equals(HamsterTagGoal.class.getSimpleName())) {
            this.setActiveCustomGoalDebugName("None");
        }

        // 2. Set Cooldowns
        // Hamster cooldown
        this.tagGameCooldownEndTick = this.getWorld().getTime() + Configs.AHP.tagGameCooldown.get();
        // Player daily limit increment
        if (player instanceof PlayerEntityAccessor accessor) {
            accessor.ahp$incrementTagGameCount();
        }

        // 3. Start Celebration Phase
        // Store the player who interacted as the rotation target
        this.celebrationTarget = player;
        HamsterMovementUtil.faceEntity(this, player);

        // Lock rotation to target (Owner or Stranger) for the duration of both animations
        this.setCelebratingRetrieval(true);
        this.celebrationRetrievalTicks = 80;
        this.interactionCooldown = 80;

        // Visuals & Audio
        this.getWorld().playSound(null, this.getBlockPos(), ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random), SoundCategory.NEUTRAL, 1.0f, 1.0f);
        ParticleEffectsUtil.spawnParticles(
                this.getWorld(),
                new Vec3d(this.getX(), this.getBodyY(0.8), this.getZ()),
                ParticleTypes.HEART,
                3,
                new Vec3d(0.3, 0.2, 0.3),
                0.2
        );

        // Trigger Celebration Animation
        this.triggerAnimOnServer("mainController", "anim_hamster_celebrate_chase");

        // 4. Schedule Gifting Sequence
        long baseTime = this.getWorld().getTime();
        long giftSequenceStart = baseTime + 32;

        this.animScheduler.scheduleTask(giftSequenceStart, "start_gift_anim", () -> {
            Item giftItem = getRandomTagGameReward();
            if (giftItem != net.minecraft.item.Items.AIR) {
                ItemStack giftStack = new ItemStack(giftItem);

                // Trigger Unload Animation
                this.triggerAnimOnServer("mainController", "anim_hamster_cheek_unload");

                // T+10 (relative to start of gift sequence): Hamster "moves item" from cheek to mouth
                this.animScheduler.scheduleTask(giftSequenceStart + 10, "gift_appear", () -> {
                    this.setMouthItemStack(giftStack);
                    this.setHoldingMouthItem(true);
                    this.setGenericInteractionTimer(0);
                });

                // T+33 (relative to start of gift sequence): Hamster spits out the item
                this.animScheduler.scheduleTask(giftSequenceStart + 33, "gift_spit", () -> {
                    if (this.isHoldingMouthItem() && !this.getMouthItemStack().isEmpty()) {
                        Vec3d look = this.getRotationVec(1.0f);
                        ItemEntity itemEntity = new ItemEntity(this.getWorld(),
                                this.getX() + look.x * 0.5,
                                this.getY() + 0.3,
                                this.getZ() + look.z * 0.5,
                                this.getMouthItemStack().copy()
                        );
                        // Forward velocity to item
                        itemEntity.setVelocity(look.x * 0.2, 0.2, look.z * 0.2);
                        this.getWorld().spawnEntity(itemEntity);
                    }
                    // Cleanup
                    this.setMouthItemStack(ItemStack.EMPTY);
                    this.setHoldingMouthItem(false);
                });
            }
        });
    }
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
    public boolean isHoldingMouthItem() {return getHamsterFlag(HOLDING_MOUTH_ITEM_FLAG);}
    public void setHoldingMouthItem(boolean holding) {setHamsterFlag(HOLDING_MOUTH_ITEM_FLAG, holding);}
    public int getGenericInteractionTimer() {return this.dataTracker.get(GENERIC_INTERACTION_TIMER);}
    public void setGenericInteractionTimer(int ticks) {this.dataTracker.set(GENERIC_INTERACTION_TIMER, ticks);}
    public boolean isTaunting() {return getHamsterFlag(TAUNTING_FLAG);}
    public void setTaunting(boolean taunting) {setHamsterFlag(TAUNTING_FLAG, taunting);}
    public boolean isPresentingItem() { return getHamsterFlag(PRESENTING_ITEM_FLAG); }
    public void setPresentingItem(boolean presenting) { setHamsterFlag(PRESENTING_ITEM_FLAG, presenting); }
    public ItemStack getMouthItemStack() {return this.dataTracker.get(MOUTH_ITEM_STACK);}
    public void setMouthItemStack(ItemStack stack) {this.dataTracker.set(MOUTH_ITEM_STACK, stack);}
    public boolean isPlayingTag() {return getHamsterFlag(IS_PLAYING_TAG_FLAG);}
    public void setPlayingTag(boolean playing) {setHamsterFlag(IS_PLAYING_TAG_FLAG, playing);}
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
    public boolean isStuckSearchingForBed() { return getHamsterFlag(STUCK_SEARCHING_FOR_BED_FLAG); }
    public void setStuckSearchingForBed(boolean stuck) { setHamsterFlag(STUCK_SEARCHING_FOR_BED_FLAG, stuck); }
    public boolean isRescueSleeping() { return getHamsterFlag(RESCUE_SLEEPING_FLAG); }
    public void setRescueSleeping(boolean rescueSleeping) { setHamsterFlag(RESCUE_SLEEPING_FLAG, rescueSleeping); }
    public void setFallFlyImmunityTicks(int ticks) {
        // Sets the fall fly immunity state.
        // Used when spawning a hamster in mid-air (e.g. Tree Heist exit) to ensure
        // falling animations play immediately instead of waiting for the grace period
        if (ticks <= 0) {
            // Disable immunity logic entirely for this entity
            this.dataTracker.set(FALL_IMMUNITY_ACTIVE, false);
            this.localSpawnImmunityTicks = 0;
        } else {
            // Enable immunity and set local timer
            this.dataTracker.set(FALL_IMMUNITY_ACTIVE, true);
            this.localSpawnImmunityTicks = ticks;
        }
    }
    public void wakeUpFromBed(boolean isManualWakeUp) {
        // Wakes the hamster up from its bed, setting the bed block to unoccupied
        // and applying a cooldown to prevent it from immediately going back to sleep.
        if (!this.isSleeping()) return;

        // Trigger animation and sound
        triggerWakeUpFromSleepAnimation(isManualWakeUp); // Pass in the context

        this.setSleeping(false);
        this.setRescueSleeping(false); // Clear the rescue flag so normal logic resumes
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
                ParticleEffectsUtil.spawnParticles(
                        this.getWorld(),
                        Vec3d.ofBottomCenter(bedPos).add(0, 0.3, 0),
                        ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                        50,
                        new Vec3d(0.2, 0.5, 0.2),
                        0.0
                );

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

                    // Determine pos with safe spawning algorithm
                    if (HamsterPlacementUtil.isSafeSpawnLocation(checkPos, this.getWorld(), this)) {
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
            HamsterState data = HamsterNbtUtil.saveToHamsterState(this);
            playerAccessor.setShoulderHamster(availableSlot, data.toNbt());
            playerAccessor.adorablehamsterpets$getMountOrderQueue().addLast(availableSlot);

            BlockPos hamsterPosForMountSound = this.getBlockPos();
            this.discard(); // Remove hamster from world

            // Trigger Generic Events and Play Mount Sound
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ModCriteria.HAMSTER_ON_SHOULDER.trigger(serverPlayer);

                // Check for Hamster Tower Advancement
                if (!playerAccessor.getShoulderHamster(ShoulderLocation.HEAD).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty() &&
                        !playerAccessor.getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty()) {
                    ModCriteria.MAX_SHOULDER_HAMSTERS.trigger(serverPlayer);
                }
            }
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_mount_success"), true);

            SoundEvent mountSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SHOULDER_MOUNT_SOUNDS, this.random);
            if (mountSound != null) {
                this.getWorld().playSound(null, player.getBlockPos(), mountSound, SoundCategory.PLAYERS, 1.0f, this.getSoundPitch());
            }

            // Item-Specific Effects and Consumption (if stack is valid lure)
            if (ConfigDataCache.isLureItem(stack)) {
                SoundEvent mountLureSound = ModSounds.getDynamicItemSound(stack);
                float volume = ModSounds.getDynamicSoundVolume(mountLureSound);
                this.getWorld().playSound(null, hamsterPosForMountSound, mountLureSound, SoundCategory.PLAYERS, volume, 1.0f);

                ParticleEffectsUtil.spawnParticles(
                        this.getWorld(),
                        Vec3d.ofCenter(hamsterPosForMountSound),
                        new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                        8,
                        new Vec3d(0.25, 0.25, 0.25),
                        0.05
                );

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
        // --- 1. Suffocation Rescue Trigger ---
        // If hamster starts suffocating, trigger self-rescue teleport logic in tick()
        if (source.isOf(DamageTypes.IN_WALL)) {
            this.suffocationGracePeriod = 40; // 2 seconds to find safe spot
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
        // Intercepts damage after the game has decided the entity was hit (so still shows visual feedback)
        // 1.20.1: Use BYPASSES_ARMOR instead of BYPASSES_WOLF_ARMOR
        if (!this.getWorld().isClient && !source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            // Modify the actual item stack that lives in the server's inventory
            ItemStack realArmorStack = this.items.get(HamsterInventoryUtil.ARMOR_SLOT_INDEX);

            // Check if armor exists and if it should absorb this specific damage source
            if (!realArmorStack.isEmpty()
                    && realArmorStack.getItem() instanceof HamsterArmorItem
                    && shouldArmorAbsorb(source, realArmorStack)) {

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
                    // Feedback
                    // 1.20.1: Use Shield Break sound
                    this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.5f, 1.2f);
                    ParticleEffectsUtil.spawnParticles(
                            this.getWorld(),
                            new Vec3d(this.getX(), this.getBodyY(0.5), this.getZ()),
                            new ItemStackParticleEffect(ParticleTypes.ITEM, particleStack),
                            15,
                            new Vec3d(0.2, 0.2, 0.2),
                            0.1
                    );

                    // Flag slot to be cleared in the next tick.
                    this.performDeferredArmorUpdate = true;

                } else {
                    // Play armor repair/damage sound if not broken
                    // 1.20.1 Fix: Use Shield Block sound
                    this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 0.5f, 1.2f);

                    ParticleEffectsUtil.spawnParticles(
                            this.getWorld(),
                            new Vec3d(this.getX(), this.getBodyY(0.5), this.getZ()),
                            new ItemStackParticleEffect(ParticleTypes.ITEM, particleStack),
                            5,
                            new Vec3d(0.2, 0.2, 0.2),
                            0.05
                    );
                }

                // Completely negate the health damage by not calling super.applyDamage
                return;
            }
        }

        // If checks fail, apply health damage normally
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
        ItemStack oldStack = this.items.get(slot).copy();
        this.getItems().set(slot, stack);

        if (!this.getWorld().isClient) {
            if (slot == HamsterInventoryUtil.ACCESSORY_SLOT_INDEX || slot == HamsterInventoryUtil.ARMOR_SLOT_INDEX) {
                HamsterInventoryUtil.syncEquipmentTrackers(this);
            }
        }

        if (!this.getWorld().isClient && !this.isLoadingNbt && !this.isSilentInventoryUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, stack);
        }

        this.markDirty();
    }
    @Override
    public ItemStack removeStack(int slot) {
        ItemStack oldStack = this.getStack(slot).copy();
        ItemStack result = ImplementedInventory.super.removeStack(slot);
        ItemStack newStack = this.getStack(slot);

        if (!this.getWorld().isClient && !this.isLoadingNbt && !this.isSilentInventoryUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, newStack);
        }
        return result;
    }
    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack oldStack = this.getStack(slot).copy();
        ItemStack result = ImplementedInventory.super.removeStack(slot, amount);
        ItemStack newStack = this.getStack(slot);

        if (!this.getWorld().isClient && !this.isLoadingNbt && !this.isSilentInventoryUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, newStack);
        }
        return result;
    }
    /**
     * True any time the hamster is falling, unless sitting or in the startup grace period.
     */
    public boolean shouldRenderFlying() {
        if (this.isSitting()) return false;

        // Thrown state overrides immunity
        if (this.isThrown()) return true;

        // Prevent visual glitch where entities loading in apparently have enough downward velocity to trigger flying
        if (this.dataTracker.get(FALL_IMMUNITY_ACTIVE) && this.localSpawnImmunityTicks > 0) return false;

        return !this.isOnGround() && this.getVelocity().y < -0.01; // Extremely high sensitivity
    }
    public void putPlayerOnBack(PlayerEntity player) {
        HamsterRidingUtil.putPlayerOnBack(this, player);
    }
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return HamsterRidingUtil.getControllingPassenger(this);
    }
    @Override
    public void travel(Vec3d movementInput) {
        if (!HamsterRidingUtil.handleTravel(this, movementInput)) {
            super.travel(movementInput);
        }
    }
    public void setRiderInput(boolean jump, boolean sprint) {
        HamsterRidingUtil.setRiderInput(this, jump, sprint);
    }

    // --- Vanilla Equipment Mapping ---
    // Map the custom ARMOR_SLOT_INDEX to the vanilla FEET slot
    // Allows vanilla systems (Frost Walker, Thorns) to see the armor and execute their logic automatically
    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        if (slot == EquipmentSlot.FEET) {
            return this.items.get(HamsterInventoryUtil.ARMOR_SLOT_INDEX);
        }
        return super.getEquippedStack(slot);
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.FEET) {
            this.setStack(HamsterInventoryUtil.ARMOR_SLOT_INDEX, stack);
            return;
        }
        super.equipStack(slot, stack);
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        // Only one armor piece, mapped to FEET
        return List.of(this.items.get(HamsterInventoryUtil.ARMOR_SLOT_INDEX));
    }

    // --- Inventory Implementation ---
    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void markDirty() {
        if (!this.getWorld().isClient()) {
            HamsterInventoryUtil.updateCheekStates(this);
            this.updateAccessoryState();
        }
    }
    public ItemStack getArmorStack() { return this.dataTracker.get(TRACKED_ARMOR_STACK); }
    public ItemStack getAccessoryStack() { return this.dataTracker.get(TRACKED_ACCESSORY_STACK); }
    public void setArmorStack(ItemStack stack) { this.setStack(HamsterInventoryUtil.ARMOR_SLOT_INDEX, stack); }
    public void setTrackedAccessoryStack(ItemStack stack) { this.dataTracker.set(TRACKED_ACCESSORY_STACK, stack); }
    public void setTrackedArmorStack(ItemStack stack) { this.dataTracker.set(TRACKED_ARMOR_STACK, stack); }

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


    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return HamsterInventoryUtil.isValidForSlot(slot, stack);
    }

    // --- NBT Saving/Loading ---
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        HamsterNbtUtil.writeCustomDataToNbt(this, nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        HamsterNbtUtil.readCustomDataFromNbt(this, nbt);
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
            HamsterSleepUtil.resetSleepState(this);
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

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        // --- 0. Pre-checks ---
        if (this.hasPassenger(player)) return ActionResult.PASS;
        if (this.interactionCooldown > 0) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        World world = this.getWorld();

        // --- 1. Global Interactions ---
        ActionResult result = HamsterInteractionUtil.handleDebugToggle(this, player, stack);
        if (result != ActionResult.PASS) return result;

        result = HamsterInteractionUtil.handleTagGame(this, player);
        if (result != ActionResult.PASS) return result;

        result = HamsterInteractionUtil.handleTaming(this, player, stack);
        if (result != ActionResult.PASS) return result;

        // --- 2. Fallback for Untamed ---
        if (!this.isTamed()) return super.interactMob(player, hand);

        // --- 3. Owner-Only Interactions ---
        if (this.isOwner(player)) {

            result = HamsterInteractionUtil.handleBedLinking(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleArmorEquip(this, player, stack);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleStateRestoration(this, player);
            if (result != ActionResult.PASS) return result;

            // State reset that falls through
            if (this.getDozingPhase() != DozingPhase.NONE) {
                HamsterSleepUtil.resetSleepState(this);
            }

            result = HamsterInteractionUtil.handleMouthItemReturn(this, player);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleAccessoryInteraction(this, player, stack);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleShearing(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleShoulderMount(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleInventoryOpen(this, player);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleFeeding(this, player, stack);
            if (result != ActionResult.PASS) return result;

            // Vanilla Fallback
            boolean isPotentialFood = ConfigDataCache.isStandardFood(stack) || ConfigDataCache.isBuffFood(stack) || ConfigDataCache.isPouchUnlockFood(stack);
            if (!player.isSneaking() && !isPotentialFood && !ConfigDataCache.isLureItem(stack)) {
                ActionResult vanillaResult = super.interactMob(player, hand);
                if (vanillaResult.isAccepted()) return vanillaResult;
            }

            // Sitting Toggle (Final Fallback for Owners)
            if (!world.isClient() && !player.isSneaking()) {
                this.setSitting(!this.isSitting());
                this.setJumping(false);
                this.getNavigation().stop();
                this.setTarget(null);
                return ActionResult.CONSUME_PARTIAL;
            }

            return ActionResult.success(world.isClient());
        }

        // --- 4. Non-Owner Tamed Fallback ---
        return super.interactMob(player, hand);
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

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) {
        HamsterEntity baby = ModEntities.HAMSTER.get().create(world);
        if (baby == null) return null;

        // Calculate variant using utility
        int babyVariantId = HamsterGeneticsUtil.calculateBabyVariant(this, mate, this.random);
        baby.setVariant(babyVariantId);

        // Retain owner copying logic since it relies on entity state
        UUID ownerUUID = this.getOwnerUuid();
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
            this.animScheduler.tick(this.getWorld().getTime());
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
        if (this.localSpawnImmunityTicks > 0) this.localSpawnImmunityTicks--;

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
                    ParticleEffectsUtil.spawnParticles(
                            this.getWorld(),
                            Vec3d.ofBottomCenter(bedPos).add(0, 0.3, 0),
                            ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                            particleCount,
                            new Vec3d(0.2, 0.3, 0.2),
                            1.0
                    );
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
                // Prioritize custom target, fallback to Owner
                Entity target = this.celebrationTarget;
                if (target == null) target = this.getOwner();

                if (target != null) {
                    HamsterMovementUtil.faceEntity(this, target);
                }
                this.celebrationRetrievalTicks--;
            } else {
                this.setCelebratingRetrieval(false);
                this.celebrationTarget = null; // Cleanup
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
                HamsterPhysicsUtil.broadcastImpactSound(this, SoundEvents.ENTITY_GENERIC_SMALL_FALL, 1.2f);

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
                        // 1. Calculate Damage
                        float damageAmount = HamsterPhysicsUtil.calculateThrowDamage(this, this.getArmorStack());

                        // 2. Create a DamageSource where the thrown hamster is the attacker.
                        DamageSource damageSource = this.getDamageSources().mobAttack(this);

                        // 3. Deal the damage to the target using the correct source.
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
                        // Feedback
                        HamsterPhysicsUtil.broadcastImpactSound(this, ModSounds.HAMSTER_IMPACT.get(), 1.0f);
                        ParticleEffectsUtil.spawnParticles(
                                world,
                                new Vec3d(this.getX(), this.getY() + this.getHeight() / 2.0, this.getZ()),
                                ParticleTypes.POOF,
                                50,
                                new Vec3d(0.4, 0.4, 0.4),
                                0.1
                        );
                    }

                    // Determine pos with safe spawning algorithm
                    Optional<BlockPos> safePosOpt = HamsterPlacementUtil.findSafeSpawnPosition(impactPos, world, 2, this);

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
                    HamsterPhysicsUtil.simulateTrajectoryAndCheckSound(this);
                }

                if (!this.hasNoGravity()) {
                    this.setVelocity(this.getVelocity().add(0.0, HamsterPhysicsUtil.THROWN_GRAVITY, 0.0));
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

                        // Effects
                        ParticleEffectsUtil.spawnParticles(
                                world,
                                new Vec3d(spawnX, spawnY, spawnZ),
                                ParticleTypes.CLOUD, // CLOUD instead of GUST on 1.20.1
                                1,
                                new Vec3d(0.1, 0.1, 0.1),
                                0.0
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
        // This logic only applies to tamed hamsters and runs on the server
        if (!this.getWorld().isClient() && this.isTamed() && !this.isKnockedOut()) {
            HamsterSleepUtil.tickTamedSleepLogic(this);
        }

        // Call super.tick() *after* processing thrown state and timers
        super.tick();

        // --- Check for Armor Changes & Update Attributes ---
        if (!this.getWorld().isClient) {
            ItemStack currentArmor = this.getArmorStack();
            if (!ItemStack.areEqual(currentArmor, this.lastArmorStack)) {
                HamsterPhysicsUtil.updateArmorModifiers(this, currentArmor);
                this.lastArmorStack = currentArmor.copy();
            }
        }

        // --- Dynamic Navigation Swapping & Periodic Config Sync ---
        if (!this.getWorld().isClient() && this.age % 20 == 0) { // Check once per second
            this.updateNavigation();

            // Periodically validate armor attributes to catch Config changes
            HamsterPhysicsUtil.updateArmorModifiers(this, this.getArmorStack());
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
                // Don't wake up if this is a rescue sleep waiting for player interaction
                if (!this.isRescueSleeping()) {
                    wakeUpFromBed(false); // Natural wake-up
                }
            }

            // --- Day/Night Cycle Wake-Up Logic ---
            if (!Configs.AHP.circadianChaos.get() && this.isSleeping() && this.getLinkedBedPos().isPresent()) {
                // If rescued, bypass time check entirely. Hamster stays asleep
                if (!this.isRescueSleeping()) {
                    boolean isSleepTime = Configs.AHP.sleepDuringDay.get() ? world.isDay() : world.isNight();
                    if (!isSleepTime) {
                        // If it's wake-up time, and delay timer has not yet been started
                        if (this.wakeUpFromBedDelay == 0 && this.goToBedCooldown == 0) {
                            this.wakeUpFromBedDelay = this.random.nextBetween(5, 60); // Set random 0.25s to 3s delay
                        }
                    } else {
                        // If time flips back to sleep time while the timer is counting down, cancel the wake-up.
                        this.wakeUpFromBedDelay = 0;
                    }
                }
            }
            // Check if the wake-up timer has just expired
            if (this.wakeUpFromBedDelay == 1) {
                this.wakeUpFromBed(false); // Natural wake-up
            }

            // --- 4a. Suffocation Self-Rescue Logic ---
            HamsterPlacementUtil.trySuffocationRescue(this);

            // --- 4b. Ejection Logic ---
            if (this.ejectionCheckCooldown <= 0) {
                this.ejectionCheckCooldown = 100; // Reset cooldown (check every 5 seconds)
                if (HamsterInventoryUtil.enforceInventoryRules(this)) {
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

                    // Feedback
                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.7F, 1.3F);
                    ParticleEffectsUtil.spawnParticles(
                            world,
                            new Vec3d(this.getX(), this.getY() + this.getHeight() / 2.0, this.getZ()),
                            new ItemStackParticleEffect(ParticleTypes.ITEM, foodToEat.split(1)),
                            5,
                            new Vec3d(0.1, 0.1, 0.1),
                            0.02
                    );
                    if (foodToEat.isEmpty()) { // If split made it empty
                        this.items.set(foodSlot, ItemStack.EMPTY);
                    }
                    HamsterInventoryUtil.updateCheekStates(this);
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
                        // 1. Ominous Particles on Hamster
                        // Use COMPOSTER and absolute offset method on 1.20.1
                        ParticleEffectsUtil.spawnParticlesWithOffset(
                                this,
                                ParticleTypes.COMPOSTER,
                                2,
                                0.12,
                                0.25,
                                0.12,
                                0.15,
                                1.8
                        );

                        // 2. Firework Particles above Ore
                        if (this.currentOreTarget != null && this.random.nextInt(4) == 0) {
                            BlockPos particlePos = this.currentOreTarget.up();
                            ParticleEffectsUtil.spawnParticles(
                                    this.getWorld(),
                                    Vec3d.ofCenter(particlePos), // Center of block above
                                    ParticleTypes.FIREWORK,
                                    1,
                                    new Vec3d(0.2, 0.35, 0.2),
                                    0.003
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
                        ParticleEffectsUtil.spawnParticles(
                                this.getWorld(),
                                Vec3d.ofCenter(particlePos),
                                ParticleTypes.SMOKE,
                                2,
                                new Vec3d(0.3, 0.3, 0.3),
                                0.005
                        );
                    }
                }

                // Black Entity Effect Particles on Hamster
                if (this.sulkEntityEffectTicks > 0) {
                    if (this.random.nextInt(5) == 0) {
                        ParticleEffect darkGrayEffect = ParticleEffectsUtil.createColoredEffect(0.3f, 0.3f, 0.3f);

                        ParticleEffectsUtil.spawnParticlesOnEntity(
                                this,
                                darkGrayEffect,
                                1,
                                0.6,
                                0.5,
                                0.005,
                                0.1
                        );
                    }
                }
            }
        }

        // --- 5. Client-Side Logic ---
        // --- 5.1 Buff Particle Logic (Zoomies) ---
        if (world.isClient && this.hasGreenBeanBuff()) {
            if (this.random.nextInt(2) == 0) {
                // Use CLOUD instead of WHITE_SMOKE on 1.20.1
                ParticleEffectsUtil.spawnMotionTrail(
                        this,
                        ParticleTypes.CLOUD,
                        3,
                        1.4,
                        0.025,
                        1.7,
                        0.17
                );
            }
        }

        // --- 5.2 Taunting Particle Logic ---
        if (this.isTaunting()) {
            if (this.random.nextInt(7) == 0) {
                ParticleEffectsUtil.spawnParticlesOnEntity(
                        this,
                        ParticleTypes.INSTANT_EFFECT,
                        2,
                        1.2,
                        0.5,
                        0.5,
                        0.2
                );
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
            HamsterInventoryUtil.updateCheekStates(this);
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
            if (this.isTaunting()) {return event.setAndContinue(TAUNTING_ANIM);}
            // --- Item Retrieval State ---
            if (this.isPresentingItem()) {return event.setAndContinue(PRESENTING_ITEM_ANIM);}
            // --- Seeking/Wanting to Seek Diamond/Ore State ---
            boolean isSeekingGoalActive = false;
            String activeGoalName = this.getActiveCustomGoalDebugName();
            if (activeGoalName.startsWith(HamsterSniffForOreGoal.class.getSimpleName())) {
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
            .triggerableAnim("anim_hamster_cheek_unload", CHEEK_UNLOAD_ANIM)

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

            // --- 2. Schedule cancellation task via Utility ---
            this.animScheduler.scheduleAnimationStop(this.getWorld().getTime(), controllerName, animName, this);
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
        this.dataTracker.startTracking(GENERIC_INTERACTION_TIMER, 0);
        this.dataTracker.startTracking(MOUTH_ITEM_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(GREEN_BEAN_BUFF_DURATION, 0L);
        this.dataTracker.startTracking(CURRENT_LOOK_UP_ANIM_ID, 1);
        this.dataTracker.startTracking(SHOULDER_ANIMATION_STATE, ShoulderAnimationState.STANDING.ordinal());
        this.dataTracker.startTracking(TRACKED_ACCESSORY_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(TRACKED_ARMOR_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(FALL_IMMUNITY_ACTIVE, true);
    }

    // --- AI Goals ---
    @Override
    protected void initGoals() {
        AdorableHamsterPets.LOGGER.trace("[AI Init {} Tick {}] Initializing goals. Current State: isSleeping={}, isSittingPose={}",
                this.getId(), this.getWorld().isClient ? "ClientTick?" : this.getWorld().getTime(), this.isSleeping(), this.isInSittingPose());
        // --- 1. Initialize Goals ---
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new HamsterSniffForOreGoal(this));
        this.goalSelector.add(1, new HamsterPlayWithItemGoal(this));
        this.goalSelector.add(2, new HamsterGoToBedAndSleepGoal(this));
        this.goalSelector.add(2, new HamsterMeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.add(3, new HamsterMateGoal(this, 0.75D));
        this.goalSelector.add(4, new HamsterTagGoal(this));
        this.goalSelector.add(5, new HamsterFollowOwnerGoal(this, 1.0D, 4.0F, 16.0F));
        this.goalSelector.add(6, new HamsterFleeGoal<>(this, LivingEntity.class, 8.0F, 0.75D, 1.5D));
        this.goalSelector.add(7, new HamsterTemptGoal(this, 1.0D, false));
        this.goalSelector.add(8, new HamsterSitGoal(this));
        this.goalSelector.add(9, new HamsterSleepGoal(this));
        this.goalSelector.add(0, new HamsterWanderAroundFarGoal(this, 0.75D));
        this.goalSelector.add(11, new HamsterLookAtEntityGoal(this, PlayerEntity.class, 2.0F, 0.15F));
        this.goalSelector.add(12, new HamsterLookAroundGoal(this));

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
     * Uses {@link HamsterRidingUtil.HamsterSeatOffsets} to ensure the rider remains visually anchored
     * to the hamster's back, dynamically compensating for the entity's scale factor.
     */
    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if (this.hasPassenger(passenger)) {
            // Use the dynamic polyfill helper created specifically for 1.20.1
            float currentScale = this.getScale();

            // Vehicle (hamster) height is already scaled at runtime
            double baseY = this.getHeight() * 0.85;

        // Passenger-size compensation (applying scaleFactor again causes scale^2 offsets).
        double riderAdjustY = passenger instanceof LivingEntity living
                ? HamsterRidingUtil.HamsterSeatOffsets.physicsSeatAdjustY(living, this.getScale())
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
                ParticleEffectsUtil.spawnParticles(
                        this.getWorld(),
                        Vec3d.ofBottomCenter(bedPos).add(0, 0.3, 0),
                        ModParticles.getForVariant(bedState.get(HamsterBedBlock.WOOD_VARIANT)),
                        70,
                        new Vec3d(0.2, 0.5, 0.2),
                        1.0
                );
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
    public boolean canHitEntity(Entity entity) {
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

            HamsterVariant chosenVariant = HamsterGeneticsUtil.determineVariantForBiome(biomeEntry, this.random);
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
        HamsterInventoryUtil.generateWildLoot(this, this.random);

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
     * Selects a random item from the Default or Extra cheek pouch loot lists.
     * Prioritizes lists that actually contain items.
     */
    private Item getRandomTagGameReward() {
        List<Integer> validPools = new ArrayList<>();
        validPools.add(0); // Default is always valid

        // Check if Extra Loot list has entries
        if (!Configs.AHP_WORLDGEN.extraCheekLootList.isEmpty()) {
            validPools.add(1);
        }

        int selectedPool = validPools.get(this.random.nextInt(validPools.size()));

        return (selectedPool == 1)
                ? ConfigDataCache.getRandomCustomLootItem(this.random)
                : ConfigDataCache.getRandomDefaultLootItem(this.random);
    }

    /**
     * Determines if the hamster armor should completely absorb the incoming damage.
     * <p>
     * <ul>
     *     <li><b>Fire Damage:</b> Only absorbed if the armor has the <b>Fire Protection</b> enchantment.
     *     Otherwise, the damage passes through to the hamster (though standard armor reduction still applies).</li>
     *     <li><b>Other Damage:</b> Always absorbed.</li>
     * </ul>
     */
    private boolean shouldArmorAbsorb(DamageSource source, ItemStack armorStack) {
        if (source.isIn(DamageTypeTags.IS_FIRE)) {
            return getFireProtectionLevel(armorStack) > 0;
        }
        return true;
    }

    /**
     * Gets the level of Fire Protection on the stack.
     */
    private int getFireProtectionLevel(ItemStack stack) {
        return EnchantmentHelper.getLevel(Enchantments.FIRE_PROTECTION, stack);
    }

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
            // Bed is occupied, determine pos with safe spawning algorithm
            Optional<BlockPos> safePosOpt = HamsterPlacementUtil.findSafeSpawnPosition(bedPos, bedWorld, 2, this);
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
        ParticleEffectsUtil.spawnParticles(
                bedWorld,
                Vec3d.ofCenter(bedPos),
                ParticleTypes.REVERSE_PORTAL,
                20,
                new Vec3d(0.3, 0.3, 0.3),
                0.1
        );
        if (this.getOwner() instanceof PlayerEntity owner) {
            owner.sendMessage(Text.translatable("message.adorablehamsterpets.respawn.success").formatted(Formatting.GOLD), true);
        }

        return true;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        Entity controller = this.getControllingPassenger();
        super.removePassenger(passenger);
        HamsterRidingUtil.onPassengerRemoved(this, passenger, controller);
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
            ParticleEffectsUtil.spawnParticles(
                    this.getWorld(),
                    Vec3d.ofCenter(pos),
                    ModParticles.getForVariant(WoodVariant.BAMBOO),
                    50,
                    new Vec3d(0.4, 0.4, 0.4),
                    0.0
            );

            ParticleEffectsUtil.spawnParticles(
                    this.getWorld(),
                    Vec3d.ofCenter(pos),
                    net.minecraft.particle.ParticleTypes.POOF,
                    50,
                    new Vec3d(0.5, 0.75, 0.5),
                    0.0
            );
        }
    }

    /**
     * Triggers a delayed celebratory sound after a successful tree heist.
     */
    public void scheduleTreeHeistCelebration() {
        if (!this.getWorld().isClient()) {
            // Schedule sound 20 ticks (1 second) later
            this.animScheduler.scheduleTask(this.getWorld().getTime() + 20, "heist_celebration", () -> {
                SoundEvent sparkleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random);
                if (sparkleSound != null) {
                    this.playSound(sparkleSound, 1.0F, 1.0F);
                }
            });
        }
    }

    /**
     * Synchronizes the visual state (DataTrackers) with the Accessory Slot inventory.
     */
    public void updateAccessoryState() {
        ItemStack accessory = this.items.get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX);

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
     * Gets the value of a specific boolean flag from the packed integer.
     * @param flag The bitmask of the flag to check (e.g., SLEEPING_FLAG).
     * @return True if the bit for the flag is set, false otherwise.
     */
    public boolean getHamsterFlag(int flag) {
        return (this.dataTracker.get(HAMSTER_FLAGS) & flag) != 0;
    }

    /**
     * Sets or clears a specific boolean flag in the packed integer.
     * @param flag The bitmask of the flag to modify (e.g., SLEEPING_FLAG).
     * @param value True to set the bit, false to clear it.
     */
    public void setHamsterFlag(int flag, boolean value) {
        int currentFlags = this.dataTracker.get(HAMSTER_FLAGS);
        if (value) {
            this.dataTracker.set(HAMSTER_FLAGS, currentFlags | flag);
        } else {
            this.dataTracker.set(HAMSTER_FLAGS, currentFlags & ~flag);
        }
    }

    private RegistryWrapper.WrapperLookup getRegistryLookup() {
        return this.getWorld().getRegistryManager();
    }

    /**
     * Triggers the appropriate headshake animation based on the hamster's current physical state.
     * Intelligent selection between sitting, standing, or moving headshakes.
     */
    public void playRefusalAnimation() {
        if (!this.getWorld().isClient()) {
            if (this.isSitting()) {
                // If sitting, play the sitting headshake
                this.triggerAnimOnServer("mainController", "sitting_headshake");
            } else {
                // If standing/moving, check velocity
                boolean isMoving = this.getVelocity().horizontalLengthSquared() > 1.0E-6;
                if (isMoving) {
                    this.triggerAnimOnServer("mainController", "moving_headshake");
                } else {
                    this.triggerAnimOnServer("mainController", "standing_headshake");
                }
            }
        }
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