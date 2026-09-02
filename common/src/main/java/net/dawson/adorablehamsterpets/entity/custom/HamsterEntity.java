package net.dawson.adorablehamsterpets.entity.custom;

import net.dawson.adorablehamsterpets.entity.ModDataSerializers;
import net.minecraft.util.ARGB;
import static net.dawson.adorablehamsterpets.sound.ModSounds.getRandomSoundFrom;

import dev.architectury.platform.Platform;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.AI.*;
import net.dawson.adorablehamsterpets.entity.AI.navigation.HamsterNavigation;
import net.dawson.adorablehamsterpets.entity.ImplementedInventory;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderAnimationState;
import net.dawson.adorablehamsterpets.entity.control.HamsterBodyControl;
import net.dawson.adorablehamsterpets.entity.custom.animation.HamsterAnimationController;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;

import java.util.*;

public class HamsterEntity extends TamableAnimal implements GeoEntity, ImplementedInventory {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static State
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Timing Constants ---
    private static final int CUSTOM_LOVE_TICKS = 600; // 30 seconds
    private static final int NORMAL_FALL_PITCH_DURATION = 15;
    private static final int PITCH_RESET_DURATION = 3;
    public static final int CELEBRATION_PARTICLE_DURATION_TICKS = 600;

    // --- Shadow Animation Tuning ---
    private static final float SHADOW_HOLD_START_TICKS = 15.0f;
    private static final float SHADOW_ROLL_BACK_TICKS = 8.0f;
    private static final float SHADOW_HOLD_APEX_TICKS = 10.0f;
    private static final float SHADOW_ROLL_FORWARD_TICKS = 8.0f;
    private static final double SHADOW_MAX_OFFSET = 0.35;

    // --- Hamster State Flags ---
    // TODO: All 31 bits are used up now, so need to split flags into two separate
    //  DataTracker entries, starting back at 1 << 0 for the second integer
    public static final int SLEEPING_FLAG = 1 << 0;
    public static final int SITTING_FLAG = 1 << 1;
    public static final int BEGGING_FLAG = 1 << 2;
    public static final int IN_LOVE_FLAG = 1 << 3;
    public static final int REFUSING_FOOD_FLAG = 1 << 4;
    public static final int THROW_COOLDOWN_FLAG = 1 << 5;
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
    public static final int FREEZING_MOVEMENT_FLAG = 1 << 16;
    public static final int IS_SHOULDER_PET_FLAG = 1 << 17;
    public static final int IS_WANDER_MODE_ACTIVE_FLAG = 1 << 18;
    public static final int ON_THE_WAY_TO_BED_FLAG = 1 << 19;
    public static final int STUCK_SEARCHING_FOR_BED_FLAG = 1 << 21;
    public static final int RESCUE_SLEEPING_FLAG = 1 << 22;
    public static final int IS_PLAYING_TAG_FLAG = 1 << 23;
    public static final int CELEBRATING_BABY_FLAG = 1 << 24;
    public static final int GENETICS_VISUALIZER_MEMBER_FLAG = 1 << 25;
    public static final int IS_ORE_TARGET_ABOVE_FLAG = 1 << 26;
    public static final int IS_BEING_PET_FLAG = 1 << 27;
    public static final int AGGRESSION_STATE_BIT_1 = 1 << 28;
    public static final int AGGRESSION_STATE_BIT_2 = 1 << 29;
    public static final int IS_DANCING_FLAG = 1 << 30;
    public static final int IS_HIDING_FLAG = 1 << 31;

    // --- Tracked Data ---
    public static final EntityDataAccessor<Integer> HAMSTER_FLAGS = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> EXACT_AGE = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<CompoundTag> GENOME = SynchedEntityData.defineId(HamsterEntity.class, ModDataSerializers.COMPOUND_TAG);
    public static final EntityDataAccessor<Integer> ANIMATION_PERSONALITY_ID = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FLOWER_POS = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DOZING_PHASE = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> CURRENT_DEEP_SLEEP_ANIM_ID = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> GENERIC_INTERACTION_TIMER = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<ItemStack> MOUTH_ITEM_STACK = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Long> GREEN_BEAN_BUFF_DURATION = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.LONG);
    public static final EntityDataAccessor<Integer> CURRENT_LOOK_UP_ANIM_ID = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> SHOULDER_ANIMATION_STATE = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> TRACKED_ACCESSORY_STACK = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> TRACKED_ARMOR_STACK = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> ARMOR_VISIBLE = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FALL_IMMUNITY_ACTIVE = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> ACTIVE_CUSTOM_GOAL_NAME_DEBUG = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> REDSTONE_FEVER_VISUAL_STATE = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REDSTONE_FEVER_BURST_ACTIVE = SynchedEntityData.defineId(HamsterEntity.class, EntityDataSerializers.BOOLEAN);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration and Setup
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Creates the attribute container for the Hamster entity.
     *
     * @return The attribute container builder.
     */
    public static AttributeSupplier.Builder createHamsterAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Configs.AHP_MAIN.wildMaxHealth.get())
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, Configs.AHP_MAIN.meleeDamage.get())
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities and Factories
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Spawns a HamsterEntity from NBT data near the player, handling position and spawning. This is
     * typically called when a player dismounts a hamster or respawns.
     *
     * @param world The server world to spawn the entity in.
     * @param player The player who is dismounting the hamster.
     * @param nbt The NbtCompound containing the hamster's data.
     * @param wasDiamondAlertActive True if the hamster should be primed for diamond seeking.
     * @param forceStand True if the hamster should be forced to stand up upon spawning.
     */
    public static void spawnFromNbt(
            ServerLevel world,
            Player player,
            CompoundTag nbt,
            boolean wasDiamondAlertActive,
            boolean forceStand) {
        HamsterShoulderUtil.spawnFromNbt(world, player, nbt, wasDiamondAlertActive, forceStand);
    }

    /**
     * Attempts to throw the hamster from the player's shoulder. This server-side logic is triggered
     * when the throw packet is received. It now delegates the core logic to the PlayerEntityMixin,
     * which determines which hamster to dismount/throw based on the configured LIFO/FIFO order.
     *
     * @param player The player attempting the throw.
     */
    public static void tryThrowFromShoulder(ServerPlayer player) {
        HamsterShoulderUtil.tryThrowFromShoulder(player);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields and State
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Cooldowns and End Ticks ---
    @Unique public int interactionCooldown = 0;
    @Unique public long foundOreCooldownEndTick = 0L;
    @Unique public long stealingCooldownEndTick = 0L;
    @Unique public int suffocationGracePeriod = 0;
    @Unique private int localSpawnImmunityTicks = 60;
    @Unique public long tagGameCooldownEndTick = 0L;
    @Unique public long cropSnackCooldownEndTick = 0L;
    public long throwCooldownEndTick = 0L;
    public long hideAndSeekCooldownEndTick = 0L;
    private long greenBeanBuffEndTick = 0L;
    public int ambientSittingCooldown = 0;

    // --- Timers and Ticks ---
    @Unique public int clientRollTimer = 0;
    @Unique public int sulkTimer = 0;
    @Unique public int wakingUpTicks = 0;
    @Unique public int goToBedDelayTicks = 0;
    @Unique public int bedLeafParticleTicks = 0;
    public int customLoveTimer;
    public int ambientSittingTimer = 0;

    // --- Flags and Toggles ---
    @Unique public transient boolean isProjectileDummy = false;
    @Unique public boolean isPrimedToSeekDiamonds = false;
    @Unique public transient boolean isTagChaser = false;
    @Unique public transient boolean isInterHamsterTagActive = false;
    @Unique public transient boolean tagGameSlapped = false;
    @Unique public transient boolean tagGameWon = false;
    @Unique public transient boolean isLookAtEntityGoalActive = false;
    @Unique public transient boolean hasMutualGaze = false;

    // --- State Values and Metrics ---
    @Unique public long totalAgeTicks = 0L;
    @Unique public transient double lastRenderTime = -1.0;
    @Unique public int prevClientRollTimer = 0;
    @Unique public transient float renderedGroundYOffset = 0.0f;
    @Unique public transient float dynamicScaleY = 1.0f;
    @Unique public int pathingFailures = 0;
    @Unique public float clientFallPitchProgress = 0.0f;
    @Unique public float prevClientFallPitchProgress = 0.0f;
    @Unique public transient float clientSwimPitch = 0.0f;
    @Unique public transient float prevClientSwimPitch = 0.0f;
    public int timesBred = 0;

    // --- Object References and Positions ---
    @Unique private UUID parentUuid = null;
    @Unique public BlockPos currentOreTarget = null;
    @Unique public transient String particleEffectId = null;
    @Unique public transient String soundEffectId = null;
    @Unique public transient ShoulderLocation shoulderLocation = ShoulderLocation.RIGHT_SHOULDER;
    @Nullable @Unique public BlockPos lastFailedTarget = null;
    @Unique public transient HamsterEntity tagGamePartner = null;
    @Unique private Vec3 smoothedWaterThrust = Vec3.ZERO;

    // --- Inventory and Runtime State ---
    private final NonNullList<ItemStack> items =
            ImplementedInventory.create(HamsterInventoryUtil.INVENTORY_SIZE);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final HamsterAnimationScheduler animScheduler = new HamsterAnimationScheduler();
    private final HamsterCombatUtil.StandardCombatState standardCombatState =
            HamsterCombatUtil.createStandardCombatState();
    private final ArmorRuntimeState armorRuntimeState = new ArmorRuntimeState();
    private final AutoEatState autoEatState = new AutoEatState();
    private final CelebrationRuntimeState celebrationRuntimeState = new CelebrationRuntimeState();
    private final FeedingInteractionState feedingInteractionState = new FeedingInteractionState();
    private final InventoryRuntimeState inventoryRuntimeState = new InventoryRuntimeState();
    private final RiderInputState riderInputState = new RiderInputState();
    private final RedstoneFeverState redstoneFeverState = new RedstoneFeverState();
    private final SleepRuntimeState sleepRuntimeState = new SleepRuntimeState();
    private final ThreeDimensionalLayoutState threeDimensionalLayoutState =
            new ThreeDimensionalLayoutState();
    private final ThumpSoundState thumpSoundState = new ThumpSoundState();
    private final ZoomiesState zoomiesState = new ZoomiesState();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 3;

        // --- Pathfinding Penalties ---
        this.setPathfindingMalus(PathType.WATER, 16.0F);
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Initialization ---
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAMSTER_FLAGS, 0);
        builder.define(EXACT_AGE, 0);
        builder.define(GENOME, HamsterGenome.createDefault().saveToNbt());
        builder.define(FLOWER_POS, 0);
        builder.define(DOZING_PHASE, DozingPhase.NONE.ordinal());
        builder.define(CURRENT_DEEP_SLEEP_ANIM_ID, "");
        builder.define(ACTIVE_CUSTOM_GOAL_NAME_DEBUG, "None");
        builder.define(ANIMATION_PERSONALITY_ID, 1);
        builder.define(GENERIC_INTERACTION_TIMER, 0);
        builder.define(MOUTH_ITEM_STACK, ItemStack.EMPTY);
        builder.define(GREEN_BEAN_BUFF_DURATION, 0L);
        builder.define(CURRENT_LOOK_UP_ANIM_ID, 1);
        builder.define(SHOULDER_ANIMATION_STATE, ShoulderAnimationState.STANDING.ordinal());
        builder.define(TRACKED_ACCESSORY_STACK, ItemStack.EMPTY);
        builder.define(TRACKED_ARMOR_STACK, ItemStack.EMPTY);
        builder.define(ARMOR_VISIBLE, true);
        builder.define(FALL_IMMUNITY_ACTIVE, true);
        builder.define(REDSTONE_FEVER_VISUAL_STATE, 0);
        builder.define(REDSTONE_FEVER_BURST_ACTIVE, false);
    }

    @Override
    protected void registerGoals() {
        // --- Redstone Fever Goals ---
        // Negative priorities let fever behavior interrupt every ordinary activity
        this.goalSelector.addGoal(-3, new HamsterRedstoneFeverBurstGoal(this));
        this.goalSelector.addGoal(-2, new HamsterRedstoneFeverCombatGoal(this));
        // --- Standard Goals ---
        this.goalSelector.addGoal(0, new HamsterPlayWithItemGoal(this));
        this.goalSelector.addGoal(1, new HamsterTemptGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new HamsterMeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.addGoal(3, new HamsterSnackOnCropGoal(this));
        this.goalSelector.addGoal(4, new HamsterSniffForOreGoal(this));
        this.goalSelector.addGoal(5, new HamsterSnackOnItemGoal(this));
        this.goalSelector.addGoal(6, new HamsterGoToBedAndSleepGoal(this));
        this.goalSelector.addGoal(7, new HamsterMateGoal(this, 0.75D));
        this.goalSelector.addGoal(8, new HamsterTagGoal(this));
        this.goalSelector.addGoal(9, new HamsterHideAndSeekGoal(this));
        this.goalSelector.addGoal(9, new HamsterInterHamsterTagGoal(this));
        this.goalSelector.addGoal(10, new HamsterFollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(11, new HamsterFollowOwnerGoal(this, 1.0D, 4.0F, 16.0F));
        this.goalSelector.addGoal(12, new HamsterFleeGoal<>(this, LivingEntity.class, 8.0F, 0.75D, 1.5D));
        this.goalSelector.addGoal(13, new HamsterSitGoal(this));
        this.goalSelector.addGoal(14, new HamsterSleepGoal(this));
        this.goalSelector.addGoal(15, new HamsterWanderAroundFarGoal(this, 0.75D));
        this.goalSelector.addGoal(16, new HamsterLookAtEntityGoal(this, Player.class, 2.0F, 0.15F));
        this.goalSelector.addGoal(17, new HamsterLookAroundGoal(this));

        // --- Target Selector Goals ---
        this.targetSelector.addGoal(1, new HamsterTrackOwnerAttackerGoal(this));
        this.targetSelector.addGoal(-2, new HamsterRedstoneFeverTargetGoal(this));
        this.targetSelector.addGoal(2, new HamsterAttackWithOwnerGoal(this));
        this.targetSelector.addGoal(3, new HamsterRevengeGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new HamsterMenaceTargetGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor world,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData entityData) {
        HamsterLifecycleUtil.initializeSpawn(this, world, spawnReason);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    /**
     * Runs the normal natural-spawn lifecycle for a position already selected as a cave.
     */
    public SpawnGroupData initializeCaveSpawn(
            ServerLevelAccessor world, DifficultyInstance difficulty) {
        HamsterLifecycleUtil.initializeSpawn(this, world, EntitySpawnReason.NATURAL, true);
        return super.finalizeSpawn(world, difficulty, EntitySpawnReason.NATURAL, null);
    }

    // --- Persistence ---
    /** Everything HamsterNbtUtil writes lives under this one key in the entity's saved data. */
    private static final String SAVE_KEY = "AdorableHamsterPets";

    // 26.2 port: entities save through ValueOutput/ValueInput now. HamsterNbtUtil
    // still speaks CompoundTag (the item-form HamsterState needs that too), so the
    // tag is bridged under a single codec-stored key.
    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        CompoundTag nbt = new CompoundTag();
        HamsterNbtUtil.writeCustomDataToNbt(this, nbt);
        out.store(SAVE_KEY, CompoundTag.CODEC, nbt);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        in.read(SAVE_KEY, CompoundTag.CODEC).ifPresent(nbt -> HamsterNbtUtil.readCustomDataFromNbt(this, nbt));
    }

    // --- Redstone Fever State ---
    public RedstoneFeverState getRedstoneFeverState() {
        return this.redstoneFeverState;
    }

    public boolean hasRedstoneFever() {
        return this.getEntityData().get(REDSTONE_FEVER_VISUAL_STATE) > 0;
    }

    public int getRedstoneFeverScarVariant() {
        int visualState = this.getEntityData().get(REDSTONE_FEVER_VISUAL_STATE);
        return visualState == 0 ? -1 : (visualState - 1) & 3;
    }

    public int getRedstoneFeverRecoveryStage() {
        int progressPercent = this.getRedstoneFeverRecoveryPercent();
        return progressPercent >= 67 ? 2 : progressPercent >= 33 ? 1 : 0;
    }

    public int getRedstoneFeverRecoveryPercent() {
        return this.getEntityData().get(REDSTONE_FEVER_VISUAL_STATE) >> 2;
    }

    public double getSynchronizedRedstoneFeverSeverity() {
        return 1.0D - this.getRedstoneFeverRecoveryPercent() / 100.0D;
    }

    public void synchronizeRedstoneFeverVisualState() {
        // Pack scar and recovery percentage into dedicated tracked state
        int visualState = 0;
        if (this.redstoneFeverState.isFevered()) {
            long required = RedstoneFeverUtil.SUNLIGHT_TICKS_PER_DAY
                    * Configs.AHP_MAIN.redstoneFeverSunlightCureDays.get();
            double progress = required == 0L
                    ? 0.0D
                    : (double) this.redstoneFeverState.getSunlightTicks() / required;
            int progressPercent = Math.clamp((int) Math.floor(progress * 100.0D), 0, 100);
            visualState = 1 + this.redstoneFeverState.getScarVariant() | progressPercent << 2;
        }
        this.getEntityData().set(REDSTONE_FEVER_VISUAL_STATE, visualState);
        RedstoneFeverUtil.reconcileMovementSpeed(this);
    }

    public boolean isRedstoneFeverBurstActive() {
        return this.getEntityData().get(REDSTONE_FEVER_BURST_ACTIVE);
    }

    public void setRedstoneFeverBurstActive(boolean active) {
        if (this.level().isClientSide()) return;
        this.getEntityData().set(REDSTONE_FEVER_BURST_ACTIVE, active);
    }

    // --- Age Transitions ---
    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        HamsterLifecycleUtil.onGrowUp(this);
    }

    // --- Tick and Cleanup ---
    @Override
    public void tick() {
        if (this.interactionCooldown > 0) this.interactionCooldown--;

        // Apply the global Redstone Fever gate before AI-disabled presentation or ordinary behavior.
        if (!this.level().isClientSide()) {
            RedstoneFeverUtil.enforceFeatureToggle(this);
        }

        // --- 1. AI-Disabled Presentation ---
        // Fast-path for AI-disabled statues
        if (this.isNoAi()) {
            this.baseTick();
            this.tickAiDisabledCommandVisuals();
            return;
        }

        // --- 2. Timers and Pre-Super Behavior ---
        this.tickSchedulersAndTimers();
        this.tickPreSuperBehaviors();
        HamsterCombatUtil.tickStandardCombat(this);

        // --- 3. Vanilla Tick ---
        super.tick();

        // --- 4. Post-Super Physics and Server Lifecycle ---
        this.tickPostSuperPhysics();
        this.tickServerLifecycle();

        // --- 5. Client Presentation and Interaction State ---
        this.tickClientPresentation();
        this.tickJukeboxAndInteractionState();
    }

    @Override
    public void die(DamageSource source) {
        if (HamsterLifecycleUtil.handleDeath(this)) return;
        super.die(source);
    }

    /**
     * Called when this entity is removed from the world. This override ensures that any server-side
     * tracking or client-side sounds/fields associated with this specific hamster instance are
     * properly cleaned up to prevent memory leaks.
     */
    @Override
    public void onClientRemoval() {
        super.onClientRemoval();

        // Clean up trackers
        if (!this.level().isClientSide()) {
            RedstoneFeverUtil.clearMovementSpeedModifier(this);
            HamsterRenderTracker.onEntityUnload(this.getId());
        }

        // Clean up transient Tag Game references
        if (this.isInterHamsterTagActive && this.tagGamePartner != null) {
            this.tagGamePartner.setPlayingTag(false);
            this.tagGamePartner.isInterHamsterTagActive = false;
            this.tagGamePartner.tagGamePartner = null;
            this.tagGamePartner.tagGameSlapped = false;
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Tracked State Accessors ---
    public AggressionState getAggressionState() {
        int flags = this.entityData.get(HAMSTER_FLAGS);
        int val =
                ((flags & AGGRESSION_STATE_BIT_1) != 0 ? 1 : 0)
                        | ((flags & AGGRESSION_STATE_BIT_2) != 0 ? 2 : 0);
        return AggressionState.values()[val];
    }

    public void setAggressionState(AggressionState state) {
        AggressionState previousState = this.getAggressionState();
        int val = state.ordinal();
        setHamsterFlag(AGGRESSION_STATE_BIT_1, (val & 1) != 0);
        setHamsterFlag(AGGRESSION_STATE_BIT_2, (val & 2) != 0);
        HamsterCombatUtil.handleAggressionStateChange(this, previousState, state);
    }

    public HamsterCombatUtil.StandardCombatState getStandardCombatState() {
        return this.standardCombatState;
    }

    public void scheduleTask(long executionTick, String debugName, Runnable action) {
        this.animScheduler.scheduleTask(executionTick, debugName, action);
    }

    public void enableZoomies(Player player) {
        this.zoomiesState.clockwise = this.random.nextBoolean();
        this.zoomiesState.radiusModifier = this.random.nextIntBetweenInclusive(-2, 4);
        // Calculate and set initial angle based on where the player is.
        double dx = this.getX() - player.getX();
        double dz = this.getZ() - player.getZ();
        this.zoomiesState.lastAngle = Math.atan2(dz, dx);
    }

    public UUID getParentUuid() {
        return this.parentUuid;
    }

    public void setParentUuid(UUID uuid) {
        this.parentUuid = uuid;
    }

    public void setCelebrationTarget(Entity target) {
        this.celebrationRuntimeState.target = target;
    }

    public void setCelebrationTicks(int ticks) {
        this.celebrationRuntimeState.ticks = ticks;
    }

    public void setSilentInventoryUpdate(boolean silent) {
        this.inventoryRuntimeState.silentUpdate = silent;
    }

    public int getQuiescentSitTimer() {
        return this.sleepRuntimeState.quiescentSitDurationTimer;
    }

    public void setQuiescentSitTimer(int ticks) {
        this.sleepRuntimeState.quiescentSitDurationTimer = ticks;
    }

    public int getDriftingOffTimer() {
        return this.sleepRuntimeState.driftingOffTimer;
    }

    public void setDriftingOffTimer(int ticks) {
        this.sleepRuntimeState.driftingOffTimer = ticks;
    }

    public int getSettleSleepCooldown() {
        return this.sleepRuntimeState.settleSleepAnimationCooldown;
    }

    public void setSettleSleepCooldown(int ticks) {
        this.sleepRuntimeState.settleSleepAnimationCooldown = ticks;
    }

    public void setCurrentDeepSleepAnimId(String animId) {
        this.entityData.set(CURRENT_DEEP_SLEEP_ANIM_ID, animId);
    }

    public ItemStack getLastFoodItem() {
        return this.autoEatState.lastFoodItem;
    }

    public void setLastFoodItem(ItemStack stack) {
        this.autoEatState.lastFoodItem = stack;
    }

    public long getGreenBeanBuffEndTick() {
        return this.greenBeanBuffEndTick;
    }

    public void setGreenBeanBuffEndTick(long tick) {
        this.greenBeanBuffEndTick = tick;
    }

    public void setRefuseTimer(int ticks) {
        this.feedingInteractionState.refuseTimer = ticks;
    }

    public boolean isCheekPouchUnlocked() {
        return getHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG);
    }

    public void setCheekPouchUnlocked(boolean unlocked) {
        setHamsterFlag(CHEEK_POUCH_UNLOCKED_FLAG, unlocked);
    }

    public boolean isArmorVisible() {
        return this.entityData.get(ARMOR_VISIBLE);
    }

    public void setArmorVisible(boolean visible) {
        this.entityData.set(ARMOR_VISIBLE, visible);
    }

    public HamsterGenome getGenome() {
        return HamsterGenome.readFromNbt(this.entityData.get(GENOME));
    }

    public void setGenome(HamsterGenome genome) {
        this.entityData.set(GENOME, genome.saveToNbt());
    }

    public boolean isSleeping() {
        return getHamsterFlag(SLEEPING_FLAG);
    }

    public void setSleeping(boolean sleeping) {
        setHamsterFlag(SLEEPING_FLAG, sleeping);
    }

    public int getAutoEatCooldownTicks() {
        return this.autoEatState.cooldownTicks;
    }

    public void setAutoEatCooldownTicks(int ticks) {
        this.autoEatState.cooldownTicks = ticks;
    }

    public int getEjectionCheckCooldown() {
        return this.inventoryRuntimeState.ejectionCheckCooldown;
    }

    public void setEjectionCheckCooldown(int ticks) {
        this.inventoryRuntimeState.ejectionCheckCooldown = ticks;
    }

    public void setLoadingNbt(boolean loading) {
        this.inventoryRuntimeState.loadingNbt = loading;
    }

    public boolean isCleaning() {
        return getHamsterFlag(CLEANING_FLAG);
    }

    public boolean isBegging() {
        return getHamsterFlag(BEGGING_FLAG);
    }

    public void setBegging(boolean value) {
        setHamsterFlag(BEGGING_FLAG, value);
    }

    public boolean isInLove() {
        return getHamsterFlag(IN_LOVE_FLAG);
    }

    public void setInLove(boolean value) {
        setHamsterFlag(IN_LOVE_FLAG, value);
    }

    public boolean isRefusingFood() {
        return getHamsterFlag(REFUSING_FOOD_FLAG);
    }

    public void setRefusingFood(boolean value) {
        setHamsterFlag(REFUSING_FOOD_FLAG, value);
    }

    public boolean isLeftCheekFull() {
        return getHamsterFlag(LEFT_CHEEK_FULL_FLAG);
    }

    public void setLeftCheekFull(boolean full) {
        setHamsterFlag(LEFT_CHEEK_FULL_FLAG, full);
    }

    public boolean isRightCheekFull() {
        return getHamsterFlag(RIGHT_CHEEK_FULL_FLAG);
    }

    public void setRightCheekFull(boolean full) {
        setHamsterFlag(RIGHT_CHEEK_FULL_FLAG, full);
    }

    public boolean isKnockedOut() {
        return getHamsterFlag(KNOCKED_OUT_FLAG);
    }

    public void setKnockedOut(boolean knocked_out) {
        setHamsterFlag(KNOCKED_OUT_FLAG, knocked_out);
    }

    public String getCurrentDeepSleepAnimationIdFromTracker() {
        return this.entityData.get(CURRENT_DEEP_SLEEP_ANIM_ID);
    }

    public boolean isAutoEating() {
        return this.autoEatState.eating;
    }

    public boolean isConsideringAutoEat() {
        return getHamsterFlag(CONSIDERING_AUTO_EAT_FLAG);
    }

    public DozingPhase getDozingPhase() {
        return DozingPhase.values()[this.entityData.get(DOZING_PHASE)];
    }

    public void setDozingPhase(DozingPhase phase) {
        this.entityData.set(DOZING_PHASE, phase.ordinal());
    }

    public void setActiveCustomGoalName(String name) {
        this.entityData.set(ACTIVE_CUSTOM_GOAL_NAME_DEBUG, name);
    }

    public String getActiveCustomGoalName() {
        String goalName = this.entityData.get(ACTIVE_CUSTOM_GOAL_NAME_DEBUG);
        return goalName;
    }

    public boolean isSulking() {
        return getHamsterFlag(SULKING_FLAG);
    }

    public boolean isCelebratingDiamond() {
        return getHamsterFlag(CELEBRATING_DIAMOND_FLAG);
    }

    public boolean isCelebratingBaby() {
        return getHamsterFlag(CELEBRATING_BABY_FLAG);
    }

    public void setCelebratingBaby(boolean celebratingBaby) {
        setHamsterFlag(CELEBRATING_BABY_FLAG, celebratingBaby);
    }

    public boolean isOreTargetAbove() {
        return getHamsterFlag(IS_ORE_TARGET_ABOVE_FLAG);
    }

    public void setOreTargetAbove(boolean above) {
        setHamsterFlag(IS_ORE_TARGET_ABOVE_FLAG, above);
    }

    public boolean isGeneticsVisualizerMember() {
        return getHamsterFlag(GENETICS_VISUALIZER_MEMBER_FLAG);
    }

    public void setGeneticsVisualizerMember(boolean isGeneticsVisualizerMember) {
        setHamsterFlag(GENETICS_VISUALIZER_MEMBER_FLAG, isGeneticsVisualizerMember);
    }

    public boolean isBeingPet() {
        return getHamsterFlag(IS_BEING_PET_FLAG);
    }

    public void setBeingPet(boolean beingPet) {
        setHamsterFlag(IS_BEING_PET_FLAG, beingPet);
    }

    public boolean isDancing() {
        return getHamsterFlag(IS_DANCING_FLAG);
    }

    public void setDancing(boolean dancing) {
        setHamsterFlag(IS_DANCING_FLAG, dancing);
    }

    public boolean isHiding() {
        return getHamsterFlag(IS_HIDING_FLAG);
    }

    public void setHiding(boolean hiding) {
        setHamsterFlag(IS_HIDING_FLAG, hiding);
    }

    // --- Riding State and Delegation ---
    public int getRiderJumpCooldown() {
        return this.riderInputState.jumpCooldown;
    }

    public void setRiderJumpCooldown(int ticks) {
        this.riderInputState.jumpCooldown = ticks;
    }

    public boolean isRiderJumpHeld() {
        return this.riderInputState.jumpHeld;
    }

    public void setRiderJumpHeld(boolean held) {
        this.riderInputState.jumpHeld = held;
    }

    public boolean isRiderJumpQueued() {
        return this.riderInputState.jumpQueued;
    }

    public void setRiderJumpQueued(boolean queued) {
        this.riderInputState.jumpQueued = queued;
    }

    public boolean isRiderSprintHeld() {
        return this.riderInputState.sprintHeld;
    }

    public void setRiderSprintHeld(boolean held) {
        this.riderInputState.sprintHeld = held;
    }

    public void delegateTravel(Vec3 movementInput) {
        super.travel(movementInput);
    }

    public void delegateSetRotation(float yaw, float pitch) {
        this.setRot(yaw, pitch);
    }

    public void executeJump() {
        this.jumpFromGround();
    }

    // --- Interaction and Animation State ---
    public void setCelebratingDiamond(boolean celebrating) {
        setHamsterFlag(CELEBRATING_DIAMOND_FLAG, celebrating);
        if (celebrating) {
            this.setBegging(false); // Ensure not also in normal begging state
            if (!this.level().isClientSide()) { // Only initialize timer on server
                this.celebrationRuntimeState.particleTicks =
                        HamsterEntity.CELEBRATION_PARTICLE_DURATION_TICKS;
                this.celebrationRuntimeState.diamondSparkleSoundDelayTicks =
                        10; // 10-tick delay for sparkle sound
            }
        } else {
            // If stopping celebration, ensure all associated timers are also stopped/reset
            this.celebrationRuntimeState.particleTicks = 0;
            this.celebrationRuntimeState.diamondSparkleSoundDelayTicks = 0;
            this.celebrationRuntimeState.diamondSoundTicks = 0;
        }
    }

    public void setSulking(boolean sulking) {
        setHamsterFlag(SULKING_FLAG, sulking);
        if (sulking) {
            if (!this.level().isClientSide()) {
                this.celebrationRuntimeState.sulkOrchestraHitDelayTicks =
                        10; // 10-tick delay for orchestra hit
                this.celebrationRuntimeState.sulkShockedSoundDelayTicks =
                        44; // 2.2 seconds * 20 ticks/second = 44 ticks
                this.celebrationRuntimeState.sulkFailParticleTicks =
                        600; // Duration for fail particles
                this.celebrationRuntimeState.sulkEntityEffectTicks =
                        600; // Duration for entity effect particles
                this.sulkTimer = 160 + this.getRandom().nextInt(80); // 8-12 seconds
            }
        } else {
            // If stopping sulking, ensure all associated timers are also stopped/reset
            this.celebrationRuntimeState.sulkOrchestraHitDelayTicks = 0;
            this.celebrationRuntimeState.sulkFailParticleTicks = 0;
            this.celebrationRuntimeState.sulkEntityEffectTicks = 0;
            this.sulkTimer = 0;
        }
    }

    public boolean isHoldingMouthItem() {
        return getHamsterFlag(HOLDING_MOUTH_ITEM_FLAG);
    }

    public void setHoldingMouthItem(boolean holding) {
        setHamsterFlag(HOLDING_MOUTH_ITEM_FLAG, holding);
    }

    public int getGenericInteractionTimer() {
        return this.entityData.get(GENERIC_INTERACTION_TIMER);
    }

    public void setGenericInteractionTimer(int ticks) {
        this.entityData.set(GENERIC_INTERACTION_TIMER, ticks);
    }

    public boolean isTaunting() {
        return getHamsterFlag(TAUNTING_FLAG);
    }

    public void setTaunting(boolean taunting) {
        setHamsterFlag(TAUNTING_FLAG, taunting);
    }

    public boolean isPresentingItem() {
        return getHamsterFlag(PRESENTING_ITEM_FLAG);
    }

    public void setPresentingItem(boolean presenting) {
        setHamsterFlag(PRESENTING_ITEM_FLAG, presenting);
    }

    public ItemStack getMouthItemStack() {
        return this.entityData.get(MOUTH_ITEM_STACK);
    }

    public void setMouthItemStack(ItemStack stack) {
        this.entityData.set(MOUTH_ITEM_STACK, stack);
    }

    public boolean isPlayingTag() {
        return getHamsterFlag(IS_PLAYING_TAG_FLAG);
    }

    public void setPlayingTag(boolean playing) {
        setHamsterFlag(IS_PLAYING_TAG_FLAG, playing);
    }

    public boolean isFrozenMovement() {
        return getHamsterFlag(FREEZING_MOVEMENT_FLAG);
    }

    public void setFrozenMovement(boolean freezingMovement) {
        setHamsterFlag(FREEZING_MOVEMENT_FLAG, freezingMovement);
    }

    // --- Movement and Sleep State ---
    public boolean hasGreenBeanBuff() {
        return this.getEntityData().get(GREEN_BEAN_BUFF_DURATION) > this.level().getGameTime();
    }

    public boolean getZoomiesIsClockwise() {
        return this.zoomiesState.clockwise;
    }

    public double getLastZoomiesAngle() {
        return this.zoomiesState.lastAngle;
    }

    public void setLastZoomiesAngle(double angle) {
        this.zoomiesState.lastAngle = angle;
    }

    public int getZoomiesRadiusModifier() {
        return this.zoomiesState.radiusModifier;
    }

    public boolean isShoulderPet() {
        return getHamsterFlag(IS_SHOULDER_PET_FLAG);
    }

    public void setShoulderPet(boolean isShoulderPet) {
        setHamsterFlag(IS_SHOULDER_PET_FLAG, isShoulderPet);
    }

    public boolean isWanderModeActive() {
        return getHamsterFlag(IS_WANDER_MODE_ACTIVE_FLAG)
                && Configs.AHP_MAIN.enableWanderMode.get();
    }

    public void setWanderModeActive(boolean active) {
        setHamsterFlag(IS_WANDER_MODE_ACTIVE_FLAG, active);
    }

    public Optional<GlobalPos> getLinkedBedPos() {
        return this.sleepRuntimeState.linkedBedPos;
    }

    public void setLinkedBedPos(Optional<GlobalPos> pos) {
        this.sleepRuntimeState.linkedBedPos = pos;
    }

    public int getGoToBedCooldown() {
        return this.sleepRuntimeState.goToBedCooldown;
    }

    public void setGoToBedCooldown(int ticks) {
        this.sleepRuntimeState.goToBedCooldown = ticks;
    }

    public boolean isStuckSearchingForBed() {
        return getHamsterFlag(STUCK_SEARCHING_FOR_BED_FLAG);
    }

    public void setStuckSearchingForBed(boolean stuck) {
        setHamsterFlag(STUCK_SEARCHING_FOR_BED_FLAG, stuck);
    }

    public boolean isRescueSleeping() {
        return getHamsterFlag(RESCUE_SLEEPING_FLAG);
    }

    public void setRescueSleeping(boolean rescueSleeping) {
        setHamsterFlag(RESCUE_SLEEPING_FLAG, rescueSleeping);
    }

    public void setFallFlyImmunityTicks(int ticks) {
        // Sets the fall fly immunity state.
        // Used when spawning a hamster in mid-air (e.g. Tree Heist exit) to ensure
        // falling animations play immediately instead of waiting for the grace period
        if (ticks <= 0) {
            // Disable immunity logic entirely for this entity
            this.entityData.set(FALL_IMMUNITY_ACTIVE, false);
            this.localSpawnImmunityTicks = 0;
        } else {
            // Enable immunity and set local timer
            this.entityData.set(FALL_IMMUNITY_ACTIVE, true);
            this.localSpawnImmunityTicks = ticks;
        }
    }

    public int getGoToBedDelayTicks() {
        return this.goToBedDelayTicks;
    }

    public void setGoToBedDelayTicks(int ticks) {
        this.goToBedDelayTicks = ticks;
    }

    public int getLureToBedTimer() {
        return this.sleepRuntimeState.lureToBedTimer;
    }

    public void setLureToBedTimer(int ticks) {
        this.sleepRuntimeState.lureToBedTimer = ticks;
    }

    public void lureToBed() {
        this.sleepRuntimeState.lureToBedTimer = 20;
    }

    public boolean isOnTheWayToBed() {
        return getHamsterFlag(ON_THE_WAY_TO_BED_FLAG);
    }

    public void setOnTheWayToBed(boolean onTheWay) {
        setHamsterFlag(ON_THE_WAY_TO_BED_FLAG, onTheWay);
    }

    public boolean shouldBypassNextSleepDelay() {
        return this.sleepRuntimeState.bypassNextSleepDelay;
    }

    public void setBypassNextSleepDelay(boolean bypass) {
        this.sleepRuntimeState.bypassNextSleepDelay = bypass;
    }

    public void setNapInBedDurationTimer(int ticks) {
        this.sleepRuntimeState.napInBedDurationTimer = ticks;
    }

    public void triggerSettleEffects(float swishVolume, int thumpDelay, float thumpVolume) {
        // Triggers a two-part settle sound effect ("swish" then "thump") with dynamic volumes.
        if (!this.level().isClientSide()) {
            this.level()
                    .playSound(
                            null,
                            this.blockPosition(),
                            ModSounds.HAMSTER_SWISH.get(),
                            SoundSource.NEUTRAL,
                            swishVolume,
                            1.0f + this.random.nextFloat() * 0.5f);
        }
        this.thumpSoundState.delayTicks = thumpDelay;
        this.thumpSoundState.volume = thumpVolume;
    }

    public void triggerWakeUpFromSleepAnimation(boolean isManualWakeUp) {
        // Triggers the appropriate wake-up animation and sound based on the last used sleep pose.
        // This is the centralized method for all "wake from sleep" scenarios.
        if (this.level().isClientSide()) return;

        int personalityId = this.getEntityData().get(ANIMATION_PERSONALITY_ID);
        this.triggerAnimOnServer("mainController", HamsterPoseUtil.getWakeUpAnimId(personalityId));

        // --- Conditional Sounds ---
        // Swish sound plays for both manual and natural wake-ups.
        this.level()
                .playSound(
                        null,
                        this.blockPosition(),
                        ModSounds.HAMSTER_SWISH.get(),
                        SoundSource.NEUTRAL,
                        0.1f,
                        1.0f + this.random.nextFloat() * 0.5f);

        if (isManualWakeUp) {
            // Affection sound only for player-initiated manual wake-ups.
            SoundEvent affectionSound =
                    getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, this.random);
            if (affectionSound != null) {
                this.level()
                        .playSound(
                                null,
                                this.blockPosition(),
                                affectionSound,
                                SoundSource.NEUTRAL,
                                1.0F,
                                1.0F);
            }
        }
    }

    public void updateNavigation() {
        // Dynamically swaps the navigation component based on the current config setting.
        // This ensures that changes to the 'avoidUnlinkedBeds' config are applied to
        // existing hamsters without requiring a world reload.
        if (this.level().isClientSide()) return;

        boolean useCustomNav = Configs.AHP_MAIN.avoidUnlinkedBeds;
        boolean isCurrentlyCustom = this.navigation instanceof HamsterNavigation;

        // Only swap if the current navigation type is incorrect
        if (useCustomNav && !isCurrentlyCustom) {
            this.navigation = createNavigation(this.level());
        } else if (!useCustomNav && isCurrentlyCustom) {
            this.navigation = createNavigation(this.level());
        }
    }

    // --- Presentation Queries ---
    /**
     * True any time the hamster is falling, unless swimming, sitting or in the startup grace
     * period.
     */
    public boolean shouldRenderFlying() {
        if (this.isOrderedToSit() || this.isInWater() || this.isInLava()) return false;

        // Prevent flying when bobbing on the water surface
        if (!this.level().getFluidState(this.blockPosition().below()).isEmpty()) {
            return false;
        }

        // Ignore transient downward velocity while newly loaded entities settle
        if (this.entityData.get(FALL_IMMUNITY_ACTIVE) && this.localSpawnImmunityTicks > 0)
            return false;

        return !this.onGround() && this.getDeltaMovement().y < -0.01; // Detect even slight falls
    }

    /** Checks if this hamster has been named "Sweet Potato" for an easter egg. */
    public boolean isSweetPotato() {
        if (this.hasCustomName()) {
            String name = this.getCustomName().getString().toLowerCase(Locale.ROOT).trim();
            return name.equals("sweet potato")
                    || name.equals("sweetpotato")
                    || name.equals("sweet-potato");
        }
        return false;
    }

    /** Checks if this hamster has been named "Hamtaro" for an easter egg. */
    public boolean isHamtaro() {
        if (this.hasCustomName()) {
            String name = this.getCustomName().getString().toLowerCase(Locale.ROOT).trim();
            return name.equals("hamtaro");
        }
        return false;
    }

    /**
     * Checks if this hamster has been named either of the special names for the
     * backwards/moonwalking easter egg.
     */
    public boolean isMoonwalking() {
        if (this.hasCustomName()) {
            String name = this.getCustomName().getString().toLowerCase(Locale.ROOT).trim();
            return name.equals("michael jackson") || name.equals("steve irwin");
        }
        return false;
    }

    // --- Riding API ---
    public void putPlayerOnBack(Player player) {
        HamsterRidingUtil.putPlayerOnBack(this, player);
    }

    public void setRiderInput(boolean jump, boolean sprint) {
        HamsterRidingUtil.setRiderInput(this, jump, sprint);
    }

    // --- Equipment and Inventory API ---
    public ItemStack getArmorStack() {
        return this.entityData.get(TRACKED_ARMOR_STACK);
    }

    public ItemStack getAccessoryStack() {
        return this.entityData.get(TRACKED_ACCESSORY_STACK);
    }

    public void setArmorStack(ItemStack stack) {
        this.setItem(HamsterInventoryUtil.ARMOR_SLOT_INDEX, stack);
    }

    public void setTrackedAccessoryStack(ItemStack stack) {
        this.entityData.set(TRACKED_ACCESSORY_STACK, stack);
    }

    public void setTrackedArmorStack(ItemStack stack) {
        this.entityData.set(TRACKED_ARMOR_STACK, stack);
    }

    // --- Sitting and Breeding API ---
    /**
     * Sets the player-commanded sitting state of the hamster. This method updates the {@code
     * IS_SITTING} DataTracker and the vanilla sitting pose. If the hamster is being told to stand
     * up while it was in a dozing/sleep sequence, the sleep sequence will be reset.
     *
     * @param sitting True to make the hamster sit, false to make it stand.
     * @param suppressSound True to suppress any sound normally associated with this action
     *     (parameter exists for API compatibility, not actively used for sound suppression within
     *     this method currently).
     */
    public void setSitting(boolean sitting, boolean suppressSound) {
        // --- 1. Play sound and trigger animation based on state change ---
        boolean wasSitting = this.isOrderedToSit();
        if (sitting && !wasSitting) { // Transitioning to sitting
            int personalityId = this.entityData.get(ANIMATION_PERSONALITY_ID);
            this.triggerAnimOnServer("mainController", HamsterPoseUtil.getSitAnimId(personalityId));
            triggerSettleEffects(0.12f, 7, 0.2f); // Swish now, thump in 7 ticks when hamster lands
        } else if (!sitting && wasSitting) { // Transitioning from sitting
            if (!this.level().isClientSide()) {
                this.level()
                        .playSound(
                                null,
                                this.blockPosition(),
                                ModSounds.HAMSTER_SWISH.get(),
                                SoundSource.NEUTRAL,
                                0.1f,
                                1.0f + this.random.nextFloat() * 0.5f);
            }
            int personalityId = this.entityData.get(ANIMATION_PERSONALITY_ID);
            this.triggerAnimOnServer(
                    "mainController", HamsterPoseUtil.getStandUpAnimId(personalityId));
        }

        // --- 2. Reset Sleep Sequence if Standing Up from a Doze/Sleep ---
        if (!sitting && this.isTame() && this.getDozingPhase() != DozingPhase.NONE) {
            HamsterSleepUtil.resetSleepState(this);
        }

        // --- 3. Update Core Sitting State ---
        setHamsterFlag(SITTING_FLAG, sitting);

        // --- 4. Update Vanilla State ---
        this.setInSittingPose(sitting);

        // --- 5. Manage Ambient Timers and Quiescent Sit Timer on State Change ---
        if (sitting) {
            // When commanded to sit, ensure the ambient timer is reset.
            this.ambientSittingTimer = 0;
            // quiescentSitDurationTimer will be set by the tick method when DozingPhase becomes
            // QUIESCENT_SITTING.
        } else {
            // If standing up, reset the quiescent sit timer to prevent immediate re-entry into
            // sleep sequence.
            this.sleepRuntimeState.quiescentSitDurationTimer = 0;
            // Also ensure ambient actions stop if they were active.
            this.ambientSittingTimer = 0;
            // Explicitly set continuous ambient flags to false.
            if (getHamsterFlag(CLEANING_FLAG)) {
                setHamsterFlag(CLEANING_FLAG, false);
            }
        }
    }

    public boolean isInCustomLove() {
        return this.customLoveTimer > 0;
    }

    public void setCustomInLove(Player player) {
        this.customLoveTimer = CUSTOM_LOVE_TICKS;
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 18);
        }
    }

    // --- Animation and Effects API ---
    /**
     * Triggers a one-shot animation on the server, which is then synced to clients. This method
     * also schedules a follow-up task to stop the triggered animation after its expected duration.
     * This serves as a failsafe to prevent animations that were triggered while the entity was
     * off-screen from playing belatedly when the entity is rendered again.
     *
     * @param controllerName The name of the animation controller.
     * @param animName The internal name of the triggerable animation (e.g., "crash").
     */
    public void triggerAnimOnServer(String controllerName, String animName) {
        if (!this.level().isClientSide()) { // Ensure we're on the server
            // --- 1. Immediately trigger the animation ---
            // Use the GeoAnimatable's built-in method for triggering server-side
            this.triggerAnim(controllerName, animName);
            AdorableHamsterPets.LOGGER.trace(
                    "[HamsterEntity {}] Triggered server-side animation: Controller='{}',"
                            + " Anim='{}'",
                    this.getId(),
                    controllerName,
                    animName);

            // --- 2. Schedule cancellation task via Utility ---
            this.animScheduler.scheduleAnimationStop(
                    this.level().getGameTime(), controllerName, animName, this);
        }
    }

    /**
     * Triggers the appropriate headshake animation based on the hamster's current physical state.
     * Intelligent selection between sitting, standing, or moving headshakes.
     */
    public void playRefusalAnimation() {
        if (!this.level().isClientSide() && !this.isNoAi()) {
            if (this.isOrderedToSit()) {
                // If sitting, play the sitting headshake
                this.triggerAnimOnServer("mainController", "sitting_headshake");
            } else {
                // If standing/moving, check velocity
                boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
                if (isMoving) {
                    this.triggerAnimOnServer("mainController", "moving_headshake");
                } else {
                    this.triggerAnimOnServer("mainController", "standing_headshake");
                }
            }
        }
    }

    /**
     * Calculates the backward Z-offset for the Vanilla circle shadow during the sitting_roll
     * animation. Uses Sine Curve Easing for smoothness.
     */
    public double getRollShadowOffset(float tickDelta) {
        float timer = Mth.lerpInt(tickDelta, this.prevClientRollTimer, this.clientRollTimer);
        float totalDuration =
                SHADOW_HOLD_START_TICKS
                        + SHADOW_ROLL_BACK_TICKS
                        + SHADOW_HOLD_APEX_TICKS
                        + SHADOW_ROLL_FORWARD_TICKS;

        // Return 0 if not rolling/in hold phase/animation finished
        if (timer <= SHADOW_HOLD_START_TICKS || timer >= totalDuration) {
            return 0.0;
        }

        if (timer <= SHADOW_HOLD_START_TICKS + SHADOW_ROLL_BACK_TICKS) {
            // Roll backwards → easing to max offset
            float progress = (timer - SHADOW_HOLD_START_TICKS) / SHADOW_ROLL_BACK_TICKS;
            return SHADOW_MAX_OFFSET * 0.5 * (1.0 - Math.cos(Math.PI * progress));
        } else if (timer
                <= SHADOW_HOLD_START_TICKS + SHADOW_ROLL_BACK_TICKS + SHADOW_HOLD_APEX_TICKS) {
            // Hold at apex
            return SHADOW_MAX_OFFSET;
        } else {
            // Roll forwards → easing back to start
            float progress =
                    (timer
                                    - SHADOW_HOLD_START_TICKS
                                    - SHADOW_ROLL_BACK_TICKS
                                    - SHADOW_HOLD_APEX_TICKS)
                            / SHADOW_ROLL_FORWARD_TICKS;
            return SHADOW_MAX_OFFSET * 0.5 * (1.0 + Math.cos(Math.PI * progress));
        }
    }

    /**
     * Triggers the visual and auditory effects of a hamster entering a tree canopy.
     *
     * @param pos The position where the effects should play.
     * @param playBreakSound True if the "crunchy" sound should play (e.g. branch/leaf hit).
     */
    public void triggerLeafPopEffects(BlockPos pos, boolean playBreakSound) {
        if (!this.level().isClientSide()) {

            // --- Audio: Crunch + Rustle ---
            // Simulate entering the dense leaves
            this.level()
                    .playSound(
                            null,
                            pos,
                            SoundEvents.AZALEA_LEAVES_BREAK,
                            SoundSource.NEUTRAL,
                            0.7f,
                            1.2f);
            SoundEvent rustleSound =
                    ModSounds.getRandomSoundFrom(
                            ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, this.random);
            if (rustleSound != null) {
                this.level()
                        .playSound(null, pos, rustleSound, SoundSource.NEUTRAL, 1.7f, 1.0f);
            }

            // --- Visuals ---
            ParticleEffectsUtil.spawnParticles(
                    this.level(),
                    Vec3.atCenterOf(pos),
                    ModParticles.getForVariant(WoodVariant.BAMBOO),
                    50,
                    new Vec3(0.4, 0.4, 0.4),
                    0.0);

            ParticleEffectsUtil.spawnParticles(
                    this.level(),
                    Vec3.atCenterOf(pos),
                    net.minecraft.core.particles.ParticleTypes.POOF,
                    50,
                    new Vec3(0.5, 0.75, 0.5),
                    0.0);
        }
    }

    /** Triggers a delayed celebratory sound after a successful tree heist. */
    public void scheduleTreeHeistCelebration() {
        if (!this.level().isClientSide()) {
            // Schedule sound 20 ticks (1 second) later
            this.animScheduler.scheduleTask(
                    this.level().getGameTime() + 20,
                    "heist_celebration",
                    () -> {
                        SoundEvent sparkleSound =
                                ModSounds.getRandomSoundFrom(
                                        ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random);
                        if (sparkleSound != null) {
                            this.playSound(sparkleSound, 1.0F, 1.0F);
                        }
                    });
        }
    }

    // --- Tracked Flag Access ---
    /** Synchronizes the visual state (DataTrackers) with the Accessory Slot inventory. */
    public void updateAccessoryState() {
        HamsterInventoryUtil.updateAccessoryState(this);
    }

    /**
     * Gets the value of a specific boolean flag from the packed integer.
     *
     * @param flag The bitmask of the flag to check (e.g., SLEEPING_FLAG).
     * @return True if the bit for the flag is set, false otherwise.
     */
    public boolean getHamsterFlag(int flag) {
        return (this.entityData.get(HAMSTER_FLAGS) & flag) != 0;
    }

    /**
     * Sets or clears a specific boolean flag in the packed integer.
     *
     * @param flag The bitmask of the flag to modify (e.g., SLEEPING_FLAG).
     * @param value True to set the bit, false to clear it.
     */
    public void setHamsterFlag(int flag, boolean value) {
        int currentFlags = this.entityData.get(HAMSTER_FLAGS);
        if (value) {
            this.entityData.set(HAMSTER_FLAGS, currentFlags | flag);
        } else {
            this.entityData.set(HAMSTER_FLAGS, currentFlags & ~flag);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Event Handlers and Callbacks
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Combat Callbacks ---
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // --- 1. Drowning Rescue ---
        if (source.is(DamageTypes.DROWN)
                && !this.level().isClientSide()
                && HamsterPlacementUtil.tryDrowningRescue(this)) {
            return false;
        }

        // --- 2. Suffocation Rescue Trigger ---
        // If hamster starts suffocating, trigger self-rescue teleport logic in tick()
        if (source.is(DamageTypes.IN_WALL)) {
            this.suffocationGracePeriod = 40; // 2 seconds to find safe spot
            return false;
        }

        // --- 3. Friendly Fire Prevention ---
        if (Configs.AHP_MAIN.preventOwnerFriendlyFire && this.isTame()) {
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity livingAttacker && this.isOwnedBy(livingAttacker)) {
                return false;
            }
        }

        // --- 4. Reset Armor Flag ---
        this.armorRuntimeState.absorbedDamage = false;

        // --- 5. Vanilla Damage ---
        boolean result = super.hurtServer(level, source, amount);

        // --- 6. Armor Absorption ---
        // If armor absorbed damage, tell engine entity was hit so it applies knockback/SFX
        if (this.armorRuntimeState.absorbedDamage) {
            return true;
        }
        return result;
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        ItemStack armorStack = this.items.get(HamsterInventoryUtil.ARMOR_SLOT_INDEX);
        if (HamsterArmorUtil.shouldAbsorbDamage(this, source, armorStack)) {
            this.armorRuntimeState.absorbedDamage = true;
            if (HamsterArmorUtil.absorbDamage(this, armorStack, amount)) {
                this.armorRuntimeState.deferredUpdate = true;
            }
            return;
        }
        super.actuallyHurt(level, source, amount);
    }

    // --- Interaction Callback ---
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // --- 1. Pre-Checks ---
        if (this.hasPassenger(player)) return InteractionResult.PASS;
        if (this.interactionCooldown > 0) return InteractionResult.PASS;
        // Fever blocks food, taming, play, and other ordinary interaction paths
        if (this.hasRedstoneFever()) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        Level world = this.level();

        // --- 2. Global Interactions ---
        InteractionResult result = HamsterInteractionUtil.handleDebugToggle(this, player, stack, hand);
        if (result != InteractionResult.PASS) return result;

        result = HamsterInteractionUtil.handleGeneticsVisualizer(this, player, stack, hand);
        if (result != InteractionResult.PASS) return result;

        result = HamsterInteractionUtil.handleTagGame(this, player, hand);
        if (result != InteractionResult.PASS) return result;

        result = HamsterInteractionUtil.handleTaming(this, player, stack, hand);
        if (result != InteractionResult.PASS) return result;

        // --- 3. Untamed Fallback ---
        if (!this.isTame()) return super.mobInteract(player, hand);

        // --- 4. Owner Interactions ---
        if (this.isOwnedBy(player)) {

            result = HamsterInteractionUtil.handleBedLinking(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleArmorEquip(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleStateRestoration(this, player, hand);
            if (result != InteractionResult.PASS) return result;

            // State reset that falls through
            if (this.getDozingPhase() != DozingPhase.NONE) {
                HamsterSleepUtil.resetSleepState(this);
            }

            result = HamsterInteractionUtil.handleMouthItemReturn(this, player, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleAggressionToggle(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleAccessoryInteraction(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleShearing(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleBabyUnlink(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleShoulderMount(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleInventoryOpen(this, player, hand);
            if (result != InteractionResult.PASS) return result;

            result = HamsterInteractionUtil.handleFeeding(this, player, stack, hand);
            if (result != InteractionResult.PASS) return result;

            // Vanilla Fallback
            if (!player.isShiftKeyDown()) {
                InteractionResult vanillaResult = super.mobInteract(player, hand);
                if (vanillaResult.consumesAction()) return vanillaResult;

                // Sitting Toggle (Final Fallback for Owners)
                if (!world.isClientSide()) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.setJumping(false);
                    this.getNavigation().stop();
                    this.setTarget(null);
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        // --- 5. Non-Owner Fallback ---
        return super.mobInteract(player, hand);
    }

    // --- Animation Callback ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        HamsterAnimationController.register(this, controllers);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- State and Collision ---
    @Override
    public boolean isOrderedToSit() {
        return getHamsterFlag(SITTING_FLAG)
                || getHamsterFlag(SLEEPING_FLAG)
                || getHamsterFlag(KNOCKED_OUT_FLAG)
                || getHamsterFlag(SULKING_FLAG);
    }

    @Override
    public boolean isPushable() {
        // Not pushable if AI disabled or sleeping in bed
        if (this.isNoAi() || (this.isSleeping() && this.getLinkedBedPos().isPresent())) {
            return false;
        }
        return super.isPushable();
    }

    // Skip physics checks entirely if AI disabled
    @Override
    public void push(Entity entity) {
        if (this.isNoAi()) return;
        super.push(entity);
    }

    @Override
    protected void doPush(Entity entity) {
        if (this.isNoAi()) return;
        super.doPush(entity);
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return !this.isNoAi() && super.canBeCollidedWith(other);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return !this.isNoAi() && super.canCollideWith(other);
    }

    @Override
    public boolean causeFallDamage(
            double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void turn(double cursorX, double cursorY) {
        if (this.isSleeping()) return;
        super.turn(cursorX, cursorY);
    }

    /**
     * Routes vanilla and external sitting changes through the custom state handler.
     *
     * <p>Delegation keeps tracked sitting, sleep, and animation state synchronized when goals or
     * other mods call the vanilla method.
     *
     * @param sitting whether the hamster should sit
     */
    @Override
    public void setOrderedToSit(boolean sitting) {
        // Keep indirect state changes silent
        this.setSitting(sitting, true);
    }

    // --- Equipment and Inventory ---
    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack oldStack = this.items.get(slot).copy();
        this.getItems().set(slot, stack);

        if (!this.level().isClientSide()) {
            if (slot == HamsterInventoryUtil.ACCESSORY_SLOT_INDEX
                    || slot == HamsterInventoryUtil.ARMOR_SLOT_INDEX) {
                HamsterInventoryUtil.syncEquipmentTrackers(this);
            }
        }

        if (!this.level().isClientSide()
                && !this.inventoryRuntimeState.loadingNbt
                && !this.inventoryRuntimeState.silentUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, stack);
        }

        this.setChanged();
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack oldStack = this.getItem(slot).copy();
        ItemStack result = ImplementedInventory.super.removeItemNoUpdate(slot);
        ItemStack newStack = this.getItem(slot);

        if (!this.level().isClientSide()
                && !this.inventoryRuntimeState.loadingNbt
                && !this.inventoryRuntimeState.silentUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, newStack);
        }
        return result;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack oldStack = this.getItem(slot).copy();
        ItemStack result = ImplementedInventory.super.removeItem(slot, amount);
        ItemStack newStack = this.getItem(slot);

        if (!this.level().isClientSide()
                && !this.inventoryRuntimeState.loadingNbt
                && !this.inventoryRuntimeState.silentUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, newStack);
        }
        return result;
    }

    // Expose custom armor as feet equipment for vanilla systems such as Frost Walker and Thorns
    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.FEET) {
            return this.items.get(HamsterInventoryUtil.ARMOR_SLOT_INDEX);
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.FEET) {
            this.setItem(HamsterInventoryUtil.ARMOR_SLOT_INDEX, stack);
            return;
        }
        super.setItemSlot(slot, stack);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void setChanged() {
        if (!this.level().isClientSide()) {
            HamsterInventoryUtil.synchronizeVisualState(this);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return HamsterInventoryUtil.isValidForSlot(slot, stack);
    }

    // --- Riding and Movement ---
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return HamsterRidingUtil.getControllingPassenger(this);
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (!HamsterRidingUtil.handleTravel(this, movementInput)) {
            super.travel(movementInput);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        Entity controller = this.getControllingPassenger();
        super.removePassenger(passenger);
        HamsterRidingUtil.onPassengerRemoved(this, passenger, controller);
    }

    // --- Display Names ---
    /**
     * Gets the display name for the hamster. This will be the hamster's custom name if it has one,
     * otherwise it defaults to a translatable title.
     *
     * @return The {@link Component} component to be used as the screen's title.
     */
    @Override
    public Component getDisplayName() {
        // If the entity has a custom name from a name tag, always use that.
        if (this.hasCustomName()) {
            return super.getDisplayName();
        }

        // If no custom name, check the config for the default name.
        if (Configs.AHP_MAIN.useHampterName) {
            return Component.translatable("entity.adorablehamsterpets.hampter");
        }

        // Otherwise, use the default vanilla behavior, which will resolve to
        // "entity.adorablehamsterpets.hamster".
        return super.getDisplayName();
    }

    /**
     * Gets the base name for the hamster. This will be the hamster's custom name if it has one,
     * otherwise it defaults to the configured fallback name ("Hampter" or "Hamster").
     *
     * @return The {@link Component} component to be used as the entity's name.
     */
    @Override
    public Component getName() {
        // Name tag gets priority
        if (this.hasCustomName()) {
            return super.getName();
        }

        // If no custom name, check config for default
        if (Configs.AHP_MAIN.useHampterName) {
            return Component.translatable("entity.adorablehamsterpets.hampter");
        }

        // Vanilla fallback
        return super.getName();
    }

    // --- Targeting and Combat ---
    /**
     * Checks if the target entity is within the hamster's shorter melee attack range. Overrides the
     * default MobEntity check which uses a larger expansion.
     *
     * @param entity The entity to check range against.
     * @return True if the entity is within the custom attack range, false otherwise.
     */
    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity entity) {
        return HamsterCombatUtil.isInAttackRange(this, entity);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.hasRedstoneFever()) {
            // Fever target goals own eligibility instead of ordinary aggression policy
            return RedstoneFeverUtil.isEligibleFeverTarget(this, target) && super.canAttack(target);
        }
        return HamsterCombatUtil.canAcquireTarget(this, target) && super.canAttack(target);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return HamsterCombatUtil.canAttackWithOwner(this, target, owner);
    }

    /**
     * Applies aggression-mode, protected-target, and Standard combat-window rules whenever a goal
     * changes the hamster's target.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.hasRedstoneFever()) {
            if (target != null && !RedstoneFeverUtil.isEligibleFeverTarget(this, target)) {
                super.setTarget(null);
                return;
            }
            // Preserve fever-selected targets without Standard combat-window filtering
            super.setTarget(target);
            return;
        }
        LivingEntity previousTarget = this.getTarget();
        if (target != null && !HamsterCombatUtil.canAcquireTarget(this, target)) {
            super.setTarget(null);
            HamsterCombatUtil.acceptTarget(this, previousTarget, null);
            return;
        }

        super.setTarget(target);
        HamsterCombatUtil.acceptTarget(this, previousTarget, target);
    }

    // --- Taming and Breeding ---
    @Override
    public void setTame(boolean tamed, boolean updateAttributes) {
        // --- Tamed State and Attributes ---
        super.setTame(tamed, updateAttributes);
        if (tamed && this.hasRedstoneFever()) {
            RedstoneFeverUtil.cureAdministratively(this);
        }
        if (tamed) {
            this.getAttribute(Attributes.MAX_HEALTH)
                    .setBaseValue(Configs.AHP_MAIN.tamedMaxHealth.get());
            this.setHealth(this.getMaxHealth()); // Set health to the updated maximum
            // Set the base attack damage attribute to the defined melee damage when tamed.
            this.getAttribute(Attributes.ATTACK_DAMAGE)
                    .setBaseValue(Configs.AHP_MAIN.meleeDamage.get());
        } else {
            this.getAttribute(Attributes.MAX_HEALTH)
                    .setBaseValue(Configs.AHP_MAIN.wildMaxHealth.get());
            // Reset attack damage if untamed
            this.getAttribute(Attributes.ATTACK_DAMAGE)
                    .setBaseValue(Configs.AHP_MAIN.meleeDamage.get());
        }
    }

    /**
     * This method is called by vanilla logic (like SitGoal) when the sitting pose changes. We
     * override it to ensure our custom IS_SITTING DataTracker, which drives animations, stays
     * synchronized with the entity's actual sitting pose state.
     */
    @Override
    public void setInSittingPose(boolean inSittingPose) {
        // --- 1. Vanilla Pose ---
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

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mate) {
        return HamsterLifecycleUtil.createChild(this, world, mate);
    }

    /**
     * Checks if the given ItemStack can be used to initiate breeding. This check is now driven by
     * the user-configurable {@code standardDiet} list via the {@link
     * ConfigDataCache#isStandardFood(ItemStack)} helper method.
     *
     * @param stack The ItemStack to check.
     * @return {@code true} if the item is a valid breeding food.
     */
    @Override
    public boolean isFood(ItemStack stack) {
        return ConfigDataCache.isStandardFood(stack);
    }

    // --- Animation ---
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // --- Navigation and Positioning ---
    @Override
    protected PathNavigation createNavigation(Level world) {
        if (Configs.AHP_MAIN.avoidUnlinkedBeds) {
            return new HamsterNavigation(this, world);
        } else {
            return new GroundPathNavigation(this, world);
        }
    }

    /**
     * Calculates the position where the passenger sits.
     *
     * <p>Uses {@link HamsterRidingUtil.HamsterSeatOffsets} to ensure the rider remains visually
     * anchored to the hamster's back, dynamically compensating for the entity's scale factor.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(
            Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return HamsterRidingUtil.getPassengerAttachmentPos(this, passenger);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new HamsterBodyControl(this);
    }

    // --- Sounds and Effects ---
    @Override
    protected SoundEvent getAmbientSound() {
        return HamsterSoundUtil.selectAmbientSound(this);
    }

    @Override
    public void playAmbientSound() {
        SoundEvent soundEvent = this.getAmbientSound();
        if (soundEvent != null && HamsterSoundUtil.isRedstoneFeverSnort(soundEvent)) {
            this.playSound(soundEvent, 0.4F, this.getVoicePitch() * 1.5F);
        } else if (soundEvent != null && HamsterSoundUtil.isBeggingSound(soundEvent)) {
            // If it's a begging sound, play it with lower volume
            this.playSound(soundEvent, 0.8F, this.getVoicePitch());
        } else {
            // For all other sounds, use the default behavior
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HamsterSoundUtil.selectHurtSound(this);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HamsterSoundUtil.selectDeathSound(this);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        HamsterSoundUtil.playFallbackStepSound(this, state);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Protected and Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Tick Phases ---
    private void tickAiDisabledCommandVisuals() {

        // Cache command tags for performance
        if (this.threeDimensionalLayoutState.isCenter == null) {
            this.threeDimensionalLayoutState.isCenter =
                    this.entityTags().contains("3d_layout_center");
            if (this.threeDimensionalLayoutState.isCenter) {
                this.threeDimensionalLayoutState.parsedY = this.blockPosition().getY();
                for (String tag : this.entityTags()) {
                    if (tag.startsWith("3d_scale_")) {
                        try {
                            this.threeDimensionalLayoutState.parsedScale =
                                    Double.parseDouble(tag.substring(9));
                        } catch (Exception ignored) {
                        }
                    } else if (tag.startsWith("3d_base_y_")) {
                        try {
                            this.threeDimensionalLayoutState.parsedY =
                                    Integer.parseInt(tag.substring(10));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // --- Cylinder Visuals for 3D Spawning Command ---
        if (this.threeDimensionalLayoutState.isCenter && !this.level().isClientSide()) {
            if (Configs.AHP_MAIN.continuousGeneticsCylinder || this.tickCount <= 20) {
                BlockPos cylinderBase =
                        new BlockPos(
                                this.blockPosition().getX(),
                                this.threeDimensionalLayoutState.parsedY,
                                this.blockPosition().getZ());
                double bobbingAmplitude = this.threeDimensionalLayoutState.parsedScale / 2.0;
                double yOffset = bobbingAmplitude - 0.5;

                ParticleEffectsUtil.spawnSpinningRing(
                        this.level(),
                        cylinderBase,
                        ParticleTypes.END_ROD,
                        250,
                        this.threeDimensionalLayoutState.parsedScale,
                        0.0,
                        0.08,
                        bobbingAmplitude,
                        0.0,
                        yOffset);
            }
        }
    }

    private void tickSchedulersAndTimers() {
        // --- 1. Animation Cancellation Scheduler ---
        if (!this.level().isClientSide()) {
            this.animScheduler.tick(this.level().getGameTime());
        }

        // --- 2. Simple Timers ---
        if (this.suffocationGracePeriod > 0) this.suffocationGracePeriod--;
        if (this.wakingUpTicks > 0) this.wakingUpTicks--;
        if (this.autoEatState.cooldownTicks > 0) this.autoEatState.cooldownTicks--;
        if (this.autoEatState.progressTicks > 0) this.autoEatState.progressTicks--;
        if (this.inventoryRuntimeState.ejectionCheckCooldown > 0)
            this.inventoryRuntimeState.ejectionCheckCooldown--;
        if (this.autoEatState.preEatDelayTicks > 0) this.autoEatState.preEatDelayTicks--;
        if (this.celebrationRuntimeState.particleTicks > 0)
            this.celebrationRuntimeState.particleTicks--;
        if (this.celebrationRuntimeState.particleTicks > 0)
            this.celebrationRuntimeState.particleTicks--;
        if (this.celebrationRuntimeState.diamondSoundTicks > 0)
            this.celebrationRuntimeState.diamondSoundTicks--;
        if (this.celebrationRuntimeState.sulkOrchestraHitDelayTicks > 0)
            this.celebrationRuntimeState.sulkOrchestraHitDelayTicks--;
        if (this.celebrationRuntimeState.sulkFailParticleTicks > 0)
            this.celebrationRuntimeState.sulkFailParticleTicks--;
        if (this.celebrationRuntimeState.sulkEntityEffectTicks > 0)
            this.celebrationRuntimeState.sulkEntityEffectTicks--;
        if (this.celebrationRuntimeState.sulkShockedSoundDelayTicks > 0)
            this.celebrationRuntimeState.sulkShockedSoundDelayTicks--;
        if (this.celebrationRuntimeState.diamondSparkleSoundDelayTicks > 0)
            this.celebrationRuntimeState.diamondSparkleSoundDelayTicks--;
        if (this.sleepRuntimeState.goToBedCooldown > 0) this.sleepRuntimeState.goToBedCooldown--;
        if (this.sleepRuntimeState.lureToBedTimer > 0) this.sleepRuntimeState.lureToBedTimer--;
        if (this.sleepRuntimeState.wakeUpFromBedDelay > 0)
            this.sleepRuntimeState.wakeUpFromBedDelay--;
        if (this.sleepRuntimeState.napInBedDurationTimer > 0)
            this.sleepRuntimeState.napInBedDurationTimer--;
        if (this.localSpawnImmunityTicks > 0) this.localSpawnImmunityTicks--;

        // --- 3. Sulking Timer ---
        if (this.sulkTimer > 0) {
            this.sulkTimer--;
            if (this.sulkTimer == 0 && this.isSulking() && !this.level().isClientSide()) {
                this.setSulking(false);
                this.setSitting(false, true);
            }
        }

        // --- 4. Settle Thump ---
        if (this.thumpSoundState.delayTicks > 0) {
            this.thumpSoundState.delayTicks--;
            if (this.thumpSoundState.delayTicks == 0 && !this.level().isClientSide()) {
                this.level()
                        .playSound(
                                null,
                                this.blockPosition(),
                                ModSounds.HAMSTER_THUMP.get(),
                                SoundSource.NEUTRAL,
                                this.thumpSoundState.volume,
                                1.5f);
            }
        }
    }

    private void tickPreSuperBehaviors() {
        // --- 1. Bed Leaf Particles ---
        HamsterBedUtil.tickBedLeafParticles(this);

        // --- 2. Ambient Sitting ---
        if (!this.level().isClientSide()) {
            HamsterAIUtil.tickAmbientSittingBehaviors(this);
        }

        // --- 3. Celebration State ---
        if (this.isFrozenMovement() || this.isCelebratingBaby()) {
            if (this.isFrozenMovement()) {
                if (this.celebrationRuntimeState.ticks > 0) {
                    this.celebrationRuntimeState.ticks--;
                } else {
                    this.setFrozenMovement(false);
                    // Only clear target if baby celebration isn't active
                    if (!this.isCelebratingBaby()) {
                        this.celebrationRuntimeState.target = null;
                    }
                }
            }

            Entity target = this.celebrationRuntimeState.target;
            if (target == null && this.isFrozenMovement()) {
                target = this.getOwner();
            }

            if (target != null && target.isAlive()) {
                HamsterMovementUtil.faceEntity(this, target);
            }
        }

        // --- 4. Tamed Sleep State Machine ---
        // This logic only applies to tamed hamsters and runs on the server
        if (!this.level().isClientSide() && this.isTame() && !this.isKnockedOut()) {
            HamsterSleepUtil.tickTamedSleepLogic(this);
        }

        // --- 5. Auto-Petting ---
        if (!this.level().isClientSide()
                && Configs.AHP_MAIN.enablePetting
                && Platform.isModLoaded("punchy")) {
            // Check twice per second
            if (this.tickCount % 10 == 0) {
                if (this.isTame() && this.getOwner() instanceof ServerPlayer serverPlayer) {
                    // Ensure player is not looking inside a GUI
                    if (serverPlayer.containerMenu == serverPlayer.inventoryMenu
                            && serverPlayer.isShiftKeyDown()) {
                        // Ensure hamster is in a pet-able state & within 5 blocks
                        if (!this.isShoulderPet()
                                && !HamsterMovementUtil.shouldNotMove(this)
                                && this.distanceToSqr(serverPlayer) < 25.0) {
                            // Verify player is looking at hamster
                            if (EntityTargetingUtil.isLookingAt(serverPlayer, this, 5.0, 0)) {
                                // Divide chance denominator by 10 so rarity is still the same
                                int chance =
                                        Math.max(
                                                1,
                                                Configs.AHP_MAIN.pettingChanceDenominator.get()
                                                        / 10);
                                if (this.getRandom().nextInt(chance) == 0) {
                                    ((PlayerEntityAccessor) serverPlayer)
                                            .ahp$startPettingHamster(this.getId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void tickPostSuperPhysics() {
        // --- 1. Swimming Physics ---
        if (this.isInWater() || this.isInLava()) {

            // Bypass node-by-node navigation that causes orbital loops in water
            if (this.getNavigation().isInProgress()
                    && this.getNavigation().getTargetPos() != null) {
                BlockPos finalTarget = this.getNavigation().getTargetPos();
                this.getLookControl()
                        .setLookAt(
                                finalTarget.getX() + 0.5,
                                finalTarget.getY() + 0.5,
                                finalTarget.getZ() + 0.5,
                                25.0f,
                                25.0f);
                this.setYRot(this.yHeadRot);
                this.yBodyRot = this.yHeadRot;
                this.getMoveControl()
                        .setWantedPosition(
                                finalTarget.getX() + 0.5,
                                finalTarget.getY() + 0.5,
                                finalTarget.getZ() + 0.5,
                                1.2D);
            }

            Vec3 velocity = this.getDeltaMovement();

            double newVelX = velocity.x;
            double newVelY = velocity.y;
            double newVelZ = velocity.z;

            // Vertical motion
            Vec3 lookVec = this.getViewVector(1.0F);

            if (this.getRandom().nextFloat() < 0.60F) { // 60% chance per tick
                double fluidHeight = this.getFluidHeight(FluidTags.WATER);

                if (fluidHeight > 0.05D) {
                    if (lookVec.y < -0.25) {
                        // Actively dive if looking down
                        double sinkForce = lookVec.y * 0.05D;
                        newVelY += sinkForce;
                    } else {
                        // Normal buoyancy
                        double buoyancy = 0.08D * fluidHeight;
                        if (newVelY < 0.04D) { // Cap upward velocity
                            newVelY += buoyancy;
                        }
                    }
                } else {
                    // Damper at surface
                    newVelY -= 0.002D;
                }
            }

            // Horizontal motion
            boolean isTryingToMove =
                    Math.abs(this.zza) > 0.01F || Math.abs(this.xxa) > 0.01F;

            if (isTryingToMove) {
                Vec3 targetDir = new Vec3(lookVec.x, 0.0, lookVec.z).normalize();

                // Smooth thrust direction
                if (this.smoothedWaterThrust.lengthSqr() == 0.0) {
                    this.smoothedWaterThrust = targetDir;
                } else {
                    // Lerp towards new direction
                    this.smoothedWaterThrust = this.smoothedWaterThrust.lerp(targetDir, 0.4D);
                }

                // Apply thrust
                double thrust = 0.01D;
                newVelX += this.smoothedWaterThrust.x * thrust;
                newVelZ += this.smoothedWaterThrust.z * thrust;
            } else {
                // Decay smoothed thrust
                this.smoothedWaterThrust = this.smoothedWaterThrust.scale(0.8D);
            }

            this.setDeltaMovement(newVelX, newVelY, newVelZ);

        } else if (this.smoothedWaterThrust.lengthSqr() > 0.0) {
            // Reset smoothed thrust when out of water
            this.smoothedWaterThrust = Vec3.ZERO;
        }

        // --- 2. Armor and Attribute Updates ---
        if (!this.level().isClientSide()) {
            ItemStack currentArmor = this.getArmorStack();
            if (!ItemStack.matches(currentArmor, this.armorRuntimeState.lastStack)) {
                HamsterPhysicsUtil.updateArmorModifiers(this, currentArmor);
                this.armorRuntimeState.lastStack = currentArmor.copy();
            }
        }

        // --- 3. Navigation and Config Sync ---
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) { // Check once per second
            this.updateNavigation();

            // Periodically validate armor attributes to catch Config changes
            HamsterPhysicsUtil.updateArmorModifiers(this, this.getArmorStack());
        }

        // --- 4. Sulking Gravity ---
        // This runs on the server to ensure physics are authoritative.
        if (!this.level().isClientSide()) {
            // If the hamster is sulking, not on the ground, and is currently falling (negative Y
            // velocity)
            if (this.isSulking() && !this.onGround() && this.getDeltaMovement().y < 0) {
                // Apply an extra downward force to make it fall faster.
                // -0.08 is the standard gravity value, so adding it again effectively doubles it.
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -1.0, 0.0));
                this.needsSync = true; // Ensure client sees the change
            }
        }
    }

    private void tickServerLifecycle() {
        Level world = this.level();
        if (!world.isClientSide()) {
            RedstoneFeverUtil.reconcileMovementSpeed(this);
            // Fever transitions, rescue credit, audio, and particles remain server-authoritative
            RedstoneFeverUtil.tick(this);

            // --- 1. Age ---
            //   1 real day = 86,400s * 20 MC ticks/s = 1,728,000 MC ticks
            //   1,728,000 / 24,000 = 72 MC ticks per age tick
            int ageProgressInterval = Configs.AHP_UI.displayAgeInIrlTime ? 72 : 1;
            if (this.tickCount % ageProgressInterval == 0) {
                this.totalAgeTicks++;
            }

            // --- 2. Water Pathfinding and Escape ---
            if (this.tickCount % 10 == 0) {
                String activeGoal = this.getActiveCustomGoalName();
                boolean isLooting =
                        activeGoal.startsWith(HamsterPlayWithItemGoal.class.getSimpleName())
                                || activeGoal.startsWith(
                                        HamsterSnackOnCropGoal.class.getSimpleName())
                                || activeGoal.startsWith(
                                        HamsterSnackOnItemGoal.class.getSimpleName());

                if (this.isInWater() || isLooting) {
                    if (this.getPathfindingMalus(PathType.WATER) != 0.0F) {
                        this.setPathfindingMalus(PathType.WATER, 0.0F);
                    }

                    // If in water, not looting, and not currently moving somewhere, actively seek
                    // land
                    if (this.isInWater() && !isLooting && this.getNavigation().isDone()) {
                        HamsterMovementUtil.findNearbyLand(world, this.blockPosition(), 6, this)
                                .ifPresent(
                                        landPos -> {
                                            this.getNavigation()
                                                    .moveTo(
                                                            landPos.getX() + 0.5,
                                                            landPos.getY(),
                                                            landPos.getZ() + 0.5,
                                                            1.0D);
                                            this.setActiveCustomGoalName("Escaping Water");
                                        });
                    }
                } else {
                    if (this.getPathfindingMalus(PathType.WATER) != 16.0F) {
                        this.setPathfindingMalus(PathType.WATER, 16.0F);
                    }
                }
            }

            // --- 3. Throw Cooldown Sync ---
            boolean hasThrowCooldown = this.throwCooldownEndTick > world.getGameTime();
            if (this.getHamsterFlag(THROW_COOLDOWN_FLAG) != hasThrowCooldown) {
                this.setHamsterFlag(THROW_COOLDOWN_FLAG, hasThrowCooldown);
            }

            // --- 4. Deferred Armor Breakage ---
            // Prevents "Equipment Update" packet from colliding with the "Hurt" packet
            if (this.armorRuntimeState.deferredUpdate) {
                this.setArmorStack(ItemStack.EMPTY);
                this.armorRuntimeState.deferredUpdate = false;
            }

            // --- 5. Circadian Chaos Wake-Up ---
            if (Configs.AHP_MAIN.circadianChaos.get()
                    && HamsterBedUtil.isSleepingInBed(this)
                    && this.sleepRuntimeState.napInBedDurationTimer == 0) {
                // Don't wake up if this is a rescue sleep waiting for player interaction
                if (!this.isRescueSleeping()) {
                    HamsterBedUtil.wakeUpFromBed(this, false); // Natural wake-up
                }
            }

            // --- 6. Exact Age Sync ---
            this.entityData.set(EXACT_AGE, this.getAge());

            // --- 7. Day/Night Wake-Up ---
            if (!Configs.AHP_MAIN.circadianChaos.get() && HamsterBedUtil.isSleepingInBed(this)) {
                // If rescued, bypass time check entirely. Hamster stays asleep
                if (!this.isRescueSleeping()) {
                    boolean isSleepTime =
                            Configs.AHP_MAIN.sleepDuringDay.get() ? world.isBrightOutside() : world.isDarkOutside();
                    if (!isSleepTime) {
                        // If it's wake-up time, and delay timer has not yet been started
                        if (this.sleepRuntimeState.wakeUpFromBedDelay == 0
                                && this.sleepRuntimeState.goToBedCooldown == 0) {
                            this.sleepRuntimeState.wakeUpFromBedDelay =
                                    this.random.nextIntBetweenInclusive(5, 60); // Set random 0.25s to 3s delay
                        }
                    } else {
                        // If time flips back to sleep time while the timer is counting down, cancel
                        // the wake-up.
                        this.sleepRuntimeState.wakeUpFromBedDelay = 0;
                    }
                }
            }
            // Check if the wake-up timer has just expired
            if (this.sleepRuntimeState.wakeUpFromBedDelay == 1) {
                HamsterBedUtil.wakeUpFromBed(this, false); // Natural wake-up
            }

            // --- 8. Bed State Repair ---
            HamsterBedUtil.autoHealBedState(this);

            // --- 9. Suffocation Rescue ---
            HamsterPlacementUtil.trySuffocationRescue(this);

            // --- 10. Inventory Ejection ---
            if (this.inventoryRuntimeState.ejectionCheckCooldown <= 0) {
                this.inventoryRuntimeState.ejectionCheckCooldown =
                        100; // Reset cooldown (check every 5 seconds)
                if (HamsterInventoryUtil.enforceInventoryRules(this)) {
                    this.setChanged();
                }
            }

            // --- 11. Auto-Eating ---
            // Stage 1: Check eligibility and start considering
            if (this.isTame()
                    && this.getHealth() < this.getMaxHealth()
                    && !this.isAutoEating()
                    && !this.isConsideringAutoEat()
                    && this.autoEatState.cooldownTicks == 0
                    && !this.isKnockedOut()) {
                // Check inventory for eligible food
                for (int i = 0; i < this.items.size(); ++i) {
                    ItemStack stack = this.items.get(i);
                    if (!stack.isEmpty() && ConfigDataCache.isAutoHealFood(stack)) {
                        // Found food, start "considering" phase
                        setHamsterFlag(CONSIDERING_AUTO_EAT_FLAG, true);
                        this.autoEatState.preEatDelayTicks = 40; // 2-second delay
                        break; // Stop searching for food once consideration starts
                    }
                }
            }

            // Stage 2: Process consideration delay and start eating
            if (this.isConsideringAutoEat() && this.autoEatState.preEatDelayTicks == 0) {
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
                    this.autoEatState.eating = true; // Use boolean flag for eating animation state
                    this.autoEatState.progressTicks = 60; // 3 seconds eating time

                    // Feedback
                    this.playSound(SoundEvents.GENERIC_EAT.value(), 0.7F, 1.3F);
                    ParticleEffectsUtil.spawnParticles(
                            world,
                            new Vec3(
                                    this.getX(), this.getY() + this.getBbHeight() / 2.0, this.getZ()),
                            new ItemParticleOption(ParticleTypes.ITEM, foodToEat.getItem()),
                            5,
                            new Vec3(0.1, 0.1, 0.1),
                            0.02);
                    if (foodToEat.isEmpty()) { // If split made it empty
                        this.items.set(foodSlot, ItemStack.EMPTY);
                    }
                    HamsterInventoryUtil.updateCheekStates(this);
                }
            }

            // Stage 3: Apply healing after eating finishes
            if (this.isAutoEating() && this.autoEatState.progressTicks == 0) {
                this.heal(Configs.AHP_ITEMS.hamsterFoodMixHealing.get());
                this.autoEatState.cooldownTicks = 60; // Set main cooldown (3 seconds)
                this.autoEatState.eating = false; // Reset eating animation flag

                if (this.getOwner() instanceof ServerPlayer serverPlayerOwner) {
                    ModCriteria.HAMSTER_AUTO_FED.get().trigger(serverPlayerOwner, this);
                }
            }

            // --- 12. Diamond Celebration Effects ---
            if (!this.level().isClientSide()) {
                if (this.isCelebratingDiamond()) {
                    // Delayed Diamond Sparkle Sound
                    if (this.celebrationRuntimeState.diamondSparkleSoundDelayTicks
                            == 1) { // Play when delay reaches 1
                        SoundEvent sparkleSound =
                                ModSounds.getRandomSoundFrom(
                                        ModSounds.DIAMOND_SPARKLE_SOUNDS, this.random);
                        if (sparkleSound != null) {
                            // Play sound at the ORE'S location
                            if (this.currentOreTarget != null) {
                                this.level()
                                        .playSound(
                                                null,
                                                this.currentOreTarget,
                                                sparkleSound,
                                                SoundSource.NEUTRAL,
                                                1.0F,
                                                1.0F);
                            } else { // Fallback to hamster pos if ore target is somehow null
                                this.level()
                                        .playSound(
                                                null,
                                                this.blockPosition(),
                                                sparkleSound,
                                                SoundSource.NEUTRAL,
                                                1.0F,
                                                1.0F);
                            }
                        }
                    }

                    // Particle Spawning
                    if (this.celebrationRuntimeState.particleTicks > 0) {
                        // 1. Ominous Particles on Hamster
                        ParticleEffectsUtil.spawnParticlesOnEntity(
                                this,
                                ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS,
                                4,
                                0.3,
                                0.3,
                                0.01,
                                0.7);

                        // 2. Firework Particles above Ore
                        if (this.currentOreTarget != null && this.random.nextInt(4) == 0) {
                            BlockPos particlePos = this.currentOreTarget.above();
                            ParticleEffectsUtil.spawnParticles(
                                    this.level(),
                                    Vec3.atCenterOf(particlePos), // Center of block above
                                    ParticleTypes.FIREWORK,
                                    1,
                                    new Vec3(0.2, 0.35, 0.2),
                                    0.003);
                        }
                    }

                    //  Begging Sounds
                    if (this.celebrationRuntimeState.diamondSoundTicks <= 0) {
                        SoundEvent celebrationSound =
                                ModSounds.getRandomSoundFrom(
                                        ModSounds.HAMSTER_BEG_SOUNDS, this.random);
                        if (celebrationSound != null) {
                            this.level()
                                    .playSound(
                                            null,
                                            this.blockPosition(),
                                            celebrationSound,
                                            SoundSource.NEUTRAL,
                                            0.8F,
                                            this.getVoicePitch());
                        }
                        this.celebrationRuntimeState.diamondSoundTicks = 30;
                    }
                }
            }

            // --- 13. Sulking Effects ---
            if (this.isSulking()) {
                // Delayed Orchestra Hit
                if (this.celebrationRuntimeState.sulkOrchestraHitDelayTicks
                        == 1) { // Play when delay reaches 1 (was 10, now 1 after 9 ticks)
                    this.level()
                            .playSound(
                                    null,
                                    this.blockPosition(),
                                    ModSounds.ALARM_ORCHESTRA_HIT.get(),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.0F);
                }

                // Delayed Single Shocked Sound
                if (this.celebrationRuntimeState.sulkShockedSoundDelayTicks
                        == 1) { // Play when this timer reaches 1
                    this.level()
                            .playSound(
                                    null,
                                    this.blockPosition(),
                                    ModSounds.HAMSTER_SHOCKED.get(),
                                    SoundSource.NEUTRAL,
                                    1.0F,
                                    1.0F);
                }

                // Angry Smoke Particles above Gold Ore
                if (this.celebrationRuntimeState.sulkFailParticleTicks > 0
                        && this.currentOreTarget != null) {
                    if (this.random.nextInt(3) == 0) {
                        BlockPos particlePos = this.currentOreTarget.above();
                        ParticleEffectsUtil.spawnParticles(
                                this.level(),
                                Vec3.atCenterOf(particlePos),
                                ParticleTypes.SMOKE,
                                2,
                                new Vec3(0.3, 0.3, 0.3),
                                0.005);
                    }
                }

                // Black Entity Effect Particles on Hamster
                if (this.celebrationRuntimeState.sulkEntityEffectTicks > 0) {
                    if (this.random.nextInt(5) == 0) {
                        ColorParticleOption darkGrayEffect =
                                ColorParticleOption.create(
                                        ParticleTypes.ENTITY_EFFECT, 0.3f, 0.3f, 0.3f);
                        ParticleEffectsUtil.spawnParticlesOnEntity(
                                this, darkGrayEffect, 1, 0.6, 0.5, 0.005, 0.1);
                    }
                }
            }

            // --- 14. Pacifist Break ---
            if (Configs.AHP_MAIN.pacifistBreakOnOwnerAttack
                    && this.getAggressionState() == AggressionState.PACIFIST
                    && this.isTame()) {
                if (this.getOwner() instanceof Player owner && owner.getLastHurtMob() != null) {
                    // Check if the attack was recent to prevent stale targets
                    if (owner.tickCount - owner.getLastHurtMobTimestamp() < 100) {
                        this.setAggressionState(AggressionState.STANDARD);

                        // Audio Feedback
                        SoundEvent sound =
                                ModSounds.getRandomSoundFrom(
                                        ModSounds.HAMSTER_HURT_SOUNDS, this.getRandom());
                        if (sound != null) {
                            this.level()
                                    .playSound(
                                            null,
                                            this.blockPosition(),
                                            sound,
                                            SoundSource.NEUTRAL,
                                            1.0f,
                                            1.0f);
                        }

                        // Visual Feedback
                        ParticleEffectsUtil.spawnParticlesOnEntity(
                                this, ParticleTypes.ANGRY_VILLAGER, 5, 0.5, 0.5, 0.0, 0.2);
                    }
                }
            }
        }
    }

    private void tickClientPresentation() {
        Level world = this.level();

        // --- 1. Rolling Animation ---
        if (world.isClientSide()) {
            this.prevClientRollTimer = this.clientRollTimer;
            boolean isRolling = false;

            var manager = this.getAnimatableInstanceCache().getManagerForId(this.getId());
            if (manager != null) {
                var controller = manager.getAnimationControllers().get("mainController");
                if (controller != null) {
                    // 26.2 port (GeckoLib 5): the queued animation is gone; inspect the raw animation's stages
                    var currentAnim = controller.getCurrentRawAnimation();
                    if (currentAnim != null && currentAnim.getAnimationStages().stream()
                            .anyMatch(stage -> "anim_hamster_sitting_roll".equals(stage.animationName()))) {
                        isRolling = true;
                    }
                }
            }

            if (isRolling) {
                this.clientRollTimer++;
            } else {
                this.clientRollTimer = 0;
                this.prevClientRollTimer = 0;
            }
        }

        // --- 2. Zoomies Particles ---
        if (world.isClientSide() && this.hasGreenBeanBuff()) {
            if (this.random.nextInt(2) == 0) {
                ParticleEffectsUtil.spawnMotionTrail(
                        this, ParticleTypes.WHITE_SMOKE,
                        3,
                        0.5D,
                        1.4D,
                        0.025D,
                        1.7D,
                        0.17D
                );
            }
        }

        // --- 3. Redstone Fever Particles ---
        if (world.isClientSide() && this.hasRedstoneFever()
                && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
            ParticleEffectsUtil.spawnMotionTrail(
                    this, new DustParticleOptions(ARGB.color(255, (int) ((0.85F) * 255), (int) ((0.05F) * 255), (int) ((0.02F) * 255)), 1.0F),
                    2,
                    0.25D,
                    2.0D,
                    0.15D,
                    1.7D,
                    0.35D
            );
        }

        // --- 4. Taunting Particles ---
        if (this.isTaunting()) {
            if (this.random.nextInt(7) == 0) {
                ParticleEffectsUtil.spawnParticlesOnEntity(
                        this, net.minecraft.core.particles.SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFFFFFFFF, 1.0F), 2, 1.2, 0.5, 0.5, 0.2);
            }
        }

        // --- 5. Fall Pitch Interpolation ---
        if (world.isClientSide()) {
            // Capture state for interpolation before modification
            this.prevClientFallPitchProgress = this.clientFallPitchProgress;

            // Determine whether to pitch down
            if (this.shouldRenderFlying()) {
                // Ease in pitch for natural falls
                this.clientFallPitchProgress += 1.0f / NORMAL_FALL_PITCH_DURATION;
            } else {
                // Reset faster
                this.clientFallPitchProgress -= 1.0f / PITCH_RESET_DURATION;
            }

            // Clamp between 0.0 and 1.0
            this.clientFallPitchProgress =
                    Mth.clamp(this.clientFallPitchProgress, 0.0f, 1.0f);

            // --- 6. Swim Pitch Interpolation ---
            this.prevClientSwimPitch = this.clientSwimPitch;

            if (this.isInWater() || this.isInLava()) {
                Vec3 velocity = this.getDeltaMovement();
                double horizontalSpeed =
                        Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                float targetPitch = (float) Math.atan2(velocity.y, horizontalSpeed);

                // Move 15% to target pitch every tick to filter out high-frequency RNG jitter
                this.clientSwimPitch += (targetPitch - this.clientSwimPitch) * 0.15f;
            } else if (this.clientSwimPitch != 0.0f) {
                // Return to level quickly if exiting water
                this.clientSwimPitch += (0.0f - this.clientSwimPitch) * 0.25f;
            }
        }
    }

    private void tickJukeboxAndInteractionState() {
        Level world = this.level();

        // --- 1. Jukebox Dancing ---
        if (!world.isClientSide() && this.tickCount % 20 == 0) {
            boolean dancing = false;
            boolean isSniffingForOre =
                    this.getActiveCustomGoalName()
                            .startsWith(HamsterSniffForOreGoal.class.getSimpleName());

            if (!HamsterMovementUtil.shouldNotMove(this)
                    && !this.isPlayingTag()
                    && !isSniffingForOre) {
                dancing = HamsterAIUtil.isDancingSongPlayingNearby(this);
            }

            if (this.isDancing() != dancing) {
                this.setDancing(dancing);
            }
        }

        // --- 2. Interaction Timers ---
        if (this.isRefusingFood() && this.feedingInteractionState.refuseTimer > 0) {
            if (--this.feedingInteractionState.refuseTimer <= 0) this.setRefusingFood(false);
        }
        if (this.feedingInteractionState.tamingCooldown > 0) {
            this.feedingInteractionState.tamingCooldown--;
        }
        if (customLoveTimer > 0) customLoveTimer--;
        if (customLoveTimer <= 0 && this.isInLove()) this.setInLove(false);
    }

    // --- Registry Access ---
    private HolderLookup.Provider getRegistryLookup() {
        return this.level().registryAccess();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Public State Types ---
    public enum DozingPhase {
        NONE, // Not in any part of the sleep sequence
        QUIESCENT_SITTING, // Tamed, sitting by command, waiting for drowsiness timer
        DRIFTING_OFF, // Playing the 90sec anim_hamster_drifting_off animation
        SETTLING_INTO_SLUMBER, // Playing a short anim_hamster_sit_settle_sleepX transition
        DEEP_SLEEP // Looping one of the anim_hamster_sleep_poseX animations
    }

    public enum AggressionState {
        STANDARD, // Ordinal 0 (Default)
        PACIFIST, // Ordinal 1
        MENACE // Ordinal 2
    }

    // --- Runtime State Holders ---
    private static final class ArmorRuntimeState {
        private boolean absorbedDamage;
        private boolean deferredUpdate;
        private ItemStack lastStack = ItemStack.EMPTY;
    }

    private static final class AutoEatState {
        private int cooldownTicks;
        private int preEatDelayTicks;
        private int progressTicks;
        private boolean eating;
        private ItemStack lastFoodItem = ItemStack.EMPTY;
    }

    private static final class CelebrationRuntimeState {
        private int particleTicks;
        private int diamondSoundTicks;
        private int sulkOrchestraHitDelayTicks;
        private int sulkFailParticleTicks;
        private int sulkEntityEffectTicks;
        private int sulkShockedSoundDelayTicks;
        private int diamondSparkleSoundDelayTicks;
        private int ticks;
        private Entity target;
    }

    private static final class FeedingInteractionState {
        private int refuseTimer;
        private int tamingCooldown;
    }

    private static final class InventoryRuntimeState {
        private int ejectionCheckCooldown = 20;
        private boolean loadingNbt;
        private boolean silentUpdate;
    }

    private static final class RiderInputState {
        private int jumpCooldown;
        private boolean jumpHeld;
        private boolean jumpQueued;
        private boolean sprintHeld;
    }

    private static final class SleepRuntimeState {
        private int settleSleepAnimationCooldown;
        private int goToBedCooldown;
        private int quiescentSitDurationTimer;
        private int driftingOffTimer;
        private int lureToBedTimer;
        private int wakeUpFromBedDelay;
        private int napInBedDurationTimer;
        private boolean bypassNextSleepDelay;
        private Optional<GlobalPos> linkedBedPos = Optional.empty();
    }

    private static final class ThreeDimensionalLayoutState {
        private Boolean isCenter;
        private double parsedScale = 1.0;
        private int parsedY;
    }

    private static final class ThumpSoundState {
        private int delayTicks;
        private float volume = 0.2f;
    }

    private static final class ZoomiesState {
        private boolean clockwise;
        private double lastAngle;
        private int radiusModifier;
    }
}
