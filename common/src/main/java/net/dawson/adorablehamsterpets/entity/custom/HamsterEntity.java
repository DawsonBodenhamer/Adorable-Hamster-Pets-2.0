package net.dawson.adorablehamsterpets.entity.custom;

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
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class HamsterEntity extends TameableEntity implements GeoEntity, ImplementedInventory {

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
    public static final TrackedData<Integer> HAMSTER_FLAGS =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> EXACT_AGE =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<NbtCompound> GENOME =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    public static final TrackedData<Integer> ANIMATION_PERSONALITY_ID =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> FLOWER_POS =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> DOZING_PHASE =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<String> CURRENT_DEEP_SLEEP_ANIM_ID =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);
    public static final TrackedData<Integer> GENERIC_INTERACTION_TIMER =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<ItemStack> MOUTH_ITEM_STACK =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    public static final TrackedData<Long> GREEN_BEAN_BUFF_DURATION =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.LONG);
    public static final TrackedData<Integer> CURRENT_LOOK_UP_ANIM_ID =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> SHOULDER_ANIMATION_STATE =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> TRACKED_ACCESSORY_STACK =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<ItemStack> TRACKED_ARMOR_STACK =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> FALL_IMMUNITY_ACTIVE =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> ACTIVE_CUSTOM_GOAL_NAME_DEBUG =
            DataTracker.registerData(HamsterEntity.class, TrackedDataHandlerRegistry.STRING);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration and Setup
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Creates the attribute container for the Hamster entity.
     *
     * @return The attribute container builder.
     */
    public static DefaultAttributeContainer.Builder createHamsterAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, Configs.AHP_MAIN.wildMaxHealth.get())
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, Configs.AHP_MAIN.meleeDamage.get())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.0D);
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
            ServerWorld world,
            PlayerEntity player,
            NbtCompound nbt,
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
    public static void tryThrowFromShoulder(ServerPlayerEntity player) {
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
    @Unique private Vec3d smoothedWaterThrust = Vec3d.ZERO;

    // --- Inventory and Runtime State ---
    private final DefaultedList<ItemStack> items =
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
    private final SleepRuntimeState sleepRuntimeState = new SleepRuntimeState();
    private final ThreeDimensionalLayoutState threeDimensionalLayoutState =
            new ThreeDimensionalLayoutState();
    private final ThumpSoundState thumpSoundState = new ThumpSoundState();
    private final ZoomiesState zoomiesState = new ZoomiesState();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 3;

        // --- Pathfinding Penalties ---
        this.setPathfindingPenalty(PathNodeType.WATER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Initialization ---
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HAMSTER_FLAGS, 0);
        builder.add(EXACT_AGE, 0);
        builder.add(GENOME, HamsterGenome.createDefault().saveToNbt());
        builder.add(FLOWER_POS, 0);
        builder.add(DOZING_PHASE, DozingPhase.NONE.ordinal());
        builder.add(CURRENT_DEEP_SLEEP_ANIM_ID, "");
        builder.add(ACTIVE_CUSTOM_GOAL_NAME_DEBUG, "None");
        builder.add(ANIMATION_PERSONALITY_ID, 1);
        builder.add(GENERIC_INTERACTION_TIMER, 0);
        builder.add(MOUTH_ITEM_STACK, ItemStack.EMPTY);
        builder.add(GREEN_BEAN_BUFF_DURATION, 0L);
        builder.add(CURRENT_LOOK_UP_ANIM_ID, 1);
        builder.add(SHOULDER_ANIMATION_STATE, ShoulderAnimationState.STANDING.ordinal());
        builder.add(TRACKED_ACCESSORY_STACK, ItemStack.EMPTY);
        builder.add(TRACKED_ARMOR_STACK, ItemStack.EMPTY);
        builder.add(FALL_IMMUNITY_ACTIVE, true);
    }

    @Override
    protected void initGoals() {
        // --- Standard Goals ---
        this.goalSelector.add(0, new HamsterPlayWithItemGoal(this));
        this.goalSelector.add(1, new HamsterMeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.add(2, new HamsterSnackOnCropGoal(this));
        this.goalSelector.add(3, new HamsterSniffForOreGoal(this));
        this.goalSelector.add(4, new HamsterSnackOnItemGoal(this));
        this.goalSelector.add(5, new HamsterGoToBedAndSleepGoal(this));
        this.goalSelector.add(6, new HamsterMateGoal(this, 0.75D));
        this.goalSelector.add(7, new HamsterTagGoal(this));
        this.goalSelector.add(8, new HamsterHideAndSeekGoal(this));
        this.goalSelector.add(8, new HamsterInterHamsterTagGoal(this));
        this.goalSelector.add(9, new HamsterFollowParentGoal(this, 1.0D));
        this.goalSelector.add(10, new HamsterFollowOwnerGoal(this, 1.0D, 4.0F, 16.0F));
        this.goalSelector.add(
                11, new HamsterFleeGoal<>(this, LivingEntity.class, 8.0F, 0.75D, 1.5D));
        this.goalSelector.add(12, new HamsterTemptGoal(this, 1.0D, false));
        this.goalSelector.add(13, new HamsterSitGoal(this));
        this.goalSelector.add(14, new HamsterSleepGoal(this));
        this.goalSelector.add(15, new HamsterWanderAroundFarGoal(this, 0.75D));
        this.goalSelector.add(
                16, new HamsterLookAtEntityGoal(this, PlayerEntity.class, 2.0F, 0.15F));
        this.goalSelector.add(17, new HamsterLookAroundGoal(this));

        // --- Target Selector Goals ---
        this.targetSelector.add(1, new HamsterTrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new HamsterAttackWithOwnerGoal(this));
        this.targetSelector.add(3, new HamsterRevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(4, new HamsterMenaceTargetGoal(this));
    }

    @Nullable
    @Override
    public EntityData initialize(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            @Nullable EntityData entityData) {
        HamsterLifecycleUtil.initializeSpawn(this, world, spawnReason);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    // --- Persistence ---
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

    // --- Age Transitions ---
    @Override
    protected void onGrowUp() {
        super.onGrowUp();
        HamsterLifecycleUtil.onGrowUp(this);
    }

    @Override
    public void setBaby(boolean baby) {
        this.setBreedingAge(
                baby ? -24000 : 0); // Vanilla logic for setting age based on baby status
    }

    // --- Tick and Cleanup ---
    @Override
    public void tick() {
        if (this.interactionCooldown > 0) this.interactionCooldown--;

        // --- 1. AI-Disabled Presentation ---
        // Fast-path for AI-disabled statues
        if (this.isAiDisabled()) {
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
    public void onDeath(DamageSource source) {
        if (HamsterLifecycleUtil.handleDeath(this)) return;
        super.onDeath(source);
    }

    /**
     * Called when this entity is removed from the world. This override ensures that any server-side
     * tracking or client-side sounds/fields associated with this specific hamster instance are
     * properly cleaned up to prevent memory leaks.
     */
    @Override
    public void onRemoved() {
        super.onRemoved();

        // Clean up trackers
        if (!this.getWorld().isClient()) {
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
        int flags = this.dataTracker.get(HAMSTER_FLAGS);
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

    public void enableZoomies(PlayerEntity player) {
        this.zoomiesState.clockwise = this.random.nextBoolean();
        this.zoomiesState.radiusModifier = this.random.nextBetween(-2, 4);
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
        this.dataTracker.set(CURRENT_DEEP_SLEEP_ANIM_ID, animId);
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

    public HamsterGenome getGenome() {
        return HamsterGenome.readFromNbt(this.dataTracker.get(GENOME));
    }

    public void setGenome(HamsterGenome genome) {
        this.dataTracker.set(GENOME, genome.saveToNbt());
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
        return this.dataTracker.get(CURRENT_DEEP_SLEEP_ANIM_ID);
    }

    public boolean isAutoEating() {
        return this.autoEatState.eating;
    }

    public boolean isConsideringAutoEat() {
        return getHamsterFlag(CONSIDERING_AUTO_EAT_FLAG);
    }

    public DozingPhase getDozingPhase() {
        return DozingPhase.values()[this.dataTracker.get(DOZING_PHASE)];
    }

    public void setDozingPhase(DozingPhase phase) {
        this.dataTracker.set(DOZING_PHASE, phase.ordinal());
    }

    public void setActiveCustomGoalName(String name) {
        this.dataTracker.set(ACTIVE_CUSTOM_GOAL_NAME_DEBUG, name);
    }

    public String getActiveCustomGoalName() {
        String goalName = this.dataTracker.get(ACTIVE_CUSTOM_GOAL_NAME_DEBUG);
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

    public void delegateTravel(Vec3d movementInput) {
        super.travel(movementInput);
    }

    public void delegateSetRotation(float yaw, float pitch) {
        this.setRotation(yaw, pitch);
    }

    public void executeJump() {
        this.jump();
    }

    // --- Interaction and Animation State ---
    public void setCelebratingDiamond(boolean celebrating) {
        setHamsterFlag(CELEBRATING_DIAMOND_FLAG, celebrating);
        if (celebrating) {
            this.setBegging(false); // Ensure not also in normal begging state
            if (!this.getWorld().isClient()) { // Only initialize timer on server
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
            if (!this.getWorld().isClient()) {
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
        return this.dataTracker.get(GENERIC_INTERACTION_TIMER);
    }

    public void setGenericInteractionTimer(int ticks) {
        this.dataTracker.set(GENERIC_INTERACTION_TIMER, ticks);
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
        return this.dataTracker.get(MOUTH_ITEM_STACK);
    }

    public void setMouthItemStack(ItemStack stack) {
        this.dataTracker.set(MOUTH_ITEM_STACK, stack);
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
        return this.getDataTracker().get(GREEN_BEAN_BUFF_DURATION) > this.getWorld().getTime();
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
            this.dataTracker.set(FALL_IMMUNITY_ACTIVE, false);
            this.localSpawnImmunityTicks = 0;
        } else {
            // Enable immunity and set local timer
            this.dataTracker.set(FALL_IMMUNITY_ACTIVE, true);
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
        if (!this.getWorld().isClient()) {
            this.getWorld()
                    .playSound(
                            null,
                            this.getBlockPos(),
                            ModSounds.HAMSTER_SWISH.get(),
                            SoundCategory.NEUTRAL,
                            swishVolume,
                            1.0f + this.random.nextFloat() * 0.5f);
        }
        this.thumpSoundState.delayTicks = thumpDelay;
        this.thumpSoundState.volume = thumpVolume;
    }

    public void triggerWakeUpFromSleepAnimation(boolean isManualWakeUp) {
        // Triggers the appropriate wake-up animation and sound based on the last used sleep pose.
        // This is the centralized method for all "wake from sleep" scenarios.
        if (this.getWorld().isClient()) return;

        int personalityId = this.getDataTracker().get(ANIMATION_PERSONALITY_ID);
        this.triggerAnimOnServer("mainController", HamsterPoseUtil.getWakeUpAnimId(personalityId));

        // --- Conditional Sounds ---
        // Swish sound plays for both manual and natural wake-ups.
        this.getWorld()
                .playSound(
                        null,
                        this.getBlockPos(),
                        ModSounds.HAMSTER_SWISH.get(),
                        SoundCategory.NEUTRAL,
                        0.1f,
                        1.0f + this.random.nextFloat() * 0.5f);

        if (isManualWakeUp) {
            // Affection sound only for player-initiated manual wake-ups.
            SoundEvent affectionSound =
                    getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, this.random);
            if (affectionSound != null) {
                this.getWorld()
                        .playSound(
                                null,
                                this.getBlockPos(),
                                affectionSound,
                                SoundCategory.NEUTRAL,
                                1.0F,
                                1.0F);
            }
        }
    }

    public void updateNavigation() {
        // Dynamically swaps the navigation component based on the current config setting.
        // This ensures that changes to the 'avoidUnlinkedBeds' config are applied to
        // existing hamsters without requiring a world reload.
        if (this.getWorld().isClient()) return;

        boolean useCustomNav = Configs.AHP_MAIN.avoidUnlinkedBeds;
        boolean isCurrentlyCustom = this.navigation instanceof HamsterNavigation;

        // Only swap if the current navigation type is incorrect
        if (useCustomNav && !isCurrentlyCustom) {
            this.navigation = createNavigation(this.getWorld());
        } else if (!useCustomNav && isCurrentlyCustom) {
            this.navigation = createNavigation(this.getWorld());
        }
    }

    // --- Presentation Queries ---
    /**
     * True any time the hamster is falling, unless swimming, sitting or in the startup grace
     * period.
     */
    public boolean shouldRenderFlying() {
        if (this.isSitting() || this.isTouchingWater() || this.isInLava()) return false;

        // Prevent flying when bobbing on the water surface
        if (!this.getWorld().getFluidState(this.getBlockPos().down()).isEmpty()) {
            return false;
        }

        // Ignore transient downward velocity while newly loaded entities settle
        if (this.dataTracker.get(FALL_IMMUNITY_ACTIVE) && this.localSpawnImmunityTicks > 0)
            return false;

        return !this.isOnGround() && this.getVelocity().y < -0.01; // Detect even slight falls
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
    public void putPlayerOnBack(PlayerEntity player) {
        HamsterRidingUtil.putPlayerOnBack(this, player);
    }

    public void setRiderInput(boolean jump, boolean sprint) {
        HamsterRidingUtil.setRiderInput(this, jump, sprint);
    }

    // --- Equipment and Inventory API ---
    public ItemStack getArmorStack() {
        return this.dataTracker.get(TRACKED_ARMOR_STACK);
    }

    public ItemStack getAccessoryStack() {
        return this.dataTracker.get(TRACKED_ACCESSORY_STACK);
    }

    public void setArmorStack(ItemStack stack) {
        this.setStack(HamsterInventoryUtil.ARMOR_SLOT_INDEX, stack);
    }

    public void setTrackedAccessoryStack(ItemStack stack) {
        this.dataTracker.set(TRACKED_ACCESSORY_STACK, stack);
    }

    public void setTrackedArmorStack(ItemStack stack) {
        this.dataTracker.set(TRACKED_ARMOR_STACK, stack);
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
        boolean wasSitting = this.isSitting();
        if (sitting && !wasSitting) { // Transitioning to sitting
            int personalityId = this.dataTracker.get(ANIMATION_PERSONALITY_ID);
            this.triggerAnimOnServer("mainController", HamsterPoseUtil.getSitAnimId(personalityId));
            triggerSettleEffects(0.12f, 7, 0.2f); // Swish now, thump in 7 ticks when hamster lands
        } else if (!sitting && wasSitting) { // Transitioning from sitting
            if (!this.getWorld().isClient()) {
                this.getWorld()
                        .playSound(
                                null,
                                this.getBlockPos(),
                                ModSounds.HAMSTER_SWISH.get(),
                                SoundCategory.NEUTRAL,
                                0.1f,
                                1.0f + this.random.nextFloat() * 0.5f);
            }
            int personalityId = this.dataTracker.get(ANIMATION_PERSONALITY_ID);
            this.triggerAnimOnServer(
                    "mainController", HamsterPoseUtil.getStandUpAnimId(personalityId));
        }

        // --- 2. Reset Sleep Sequence if Standing Up from a Doze/Sleep ---
        if (!sitting && this.isTamed() && this.getDozingPhase() != DozingPhase.NONE) {
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

    public void setCustomInLove(PlayerEntity player) {
        this.customLoveTimer = CUSTOM_LOVE_TICKS;
        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte) 18);
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
        if (!this.getWorld().isClient()) { // Ensure we're on the server
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
                    this.getWorld().getTime(), controllerName, animName, this);
        }
    }

    /**
     * Triggers the appropriate headshake animation based on the hamster's current physical state.
     * Intelligent selection between sitting, standing, or moving headshakes.
     */
    public void playRefusalAnimation() {
        if (!this.getWorld().isClient() && !this.isAiDisabled()) {
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
     * Calculates the backward Z-offset for the Vanilla circle shadow during the sitting_roll
     * animation. Uses Sine Curve Easing for smoothness.
     */
    public double getRollShadowOffset(float tickDelta) {
        float timer = MathHelper.lerp(tickDelta, this.prevClientRollTimer, this.clientRollTimer);
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
        if (!this.getWorld().isClient()) {

            // --- Audio: Crunch + Rustle ---
            // Simulate entering the dense leaves
            this.getWorld()
                    .playSound(
                            null,
                            pos,
                            SoundEvents.BLOCK_AZALEA_LEAVES_BREAK,
                            SoundCategory.NEUTRAL,
                            0.7f,
                            1.2f);
            SoundEvent rustleSound =
                    ModSounds.getRandomSoundFrom(
                            ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, this.random);
            if (rustleSound != null) {
                this.getWorld()
                        .playSound(null, pos, rustleSound, SoundCategory.NEUTRAL, 1.7f, 1.0f);
            }

            // --- Visuals ---
            ParticleEffectsUtil.spawnParticles(
                    this.getWorld(),
                    Vec3d.ofCenter(pos),
                    ModParticles.getForVariant(WoodVariant.BAMBOO),
                    50,
                    new Vec3d(0.4, 0.4, 0.4),
                    0.0);

            ParticleEffectsUtil.spawnParticles(
                    this.getWorld(),
                    Vec3d.ofCenter(pos),
                    net.minecraft.particle.ParticleTypes.POOF,
                    50,
                    new Vec3d(0.5, 0.75, 0.5),
                    0.0);
        }
    }

    /** Triggers a delayed celebratory sound after a successful tree heist. */
    public void scheduleTreeHeistCelebration() {
        if (!this.getWorld().isClient()) {
            // Schedule sound 20 ticks (1 second) later
            this.animScheduler.scheduleTask(
                    this.getWorld().getTime() + 20,
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
        return (this.dataTracker.get(HAMSTER_FLAGS) & flag) != 0;
    }

    /**
     * Sets or clears a specific boolean flag in the packed integer.
     *
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Event Handlers and Callbacks
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Combat Callbacks ---
    @Override
    public boolean damage(DamageSource source, float amount) {
        // --- 1. Drowning Rescue ---
        if (source.isOf(DamageTypes.DROWN)
                && !this.getWorld().isClient()
                && HamsterPlacementUtil.tryDrowningRescue(this)) {
            return false;
        }

        // --- 2. Suffocation Rescue Trigger ---
        // If hamster starts suffocating, trigger self-rescue teleport logic in tick()
        if (source.isOf(DamageTypes.IN_WALL)) {
            this.suffocationGracePeriod = 40; // 2 seconds to find safe spot
            return false;
        }

        // --- 3. Friendly Fire Prevention ---
        if (Configs.AHP_MAIN.preventOwnerFriendlyFire && this.isTamed()) {
            Entity attacker = source.getAttacker();
            if (attacker instanceof LivingEntity livingAttacker && this.isOwner(livingAttacker)) {
                return false;
            }
        }

        // --- 4. Reset Armor Flag ---
        this.armorRuntimeState.absorbedDamage = false;

        // --- 5. Vanilla Damage ---
        boolean result = super.damage(source, amount);

        // --- 6. Armor Absorption ---
        // If armor absorbed damage, tell engine entity was hit so it applies knockback/SFX
        if (this.armorRuntimeState.absorbedDamage) {
            return true;
        }
        return result;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        ItemStack armorStack = this.items.get(HamsterInventoryUtil.ARMOR_SLOT_INDEX);
        if (HamsterArmorUtil.shouldAbsorbDamage(this, source, armorStack)) {
            this.armorRuntimeState.absorbedDamage = true;
            if (HamsterArmorUtil.absorbDamage(this, armorStack, amount)) {
                this.armorRuntimeState.deferredUpdate = true;
            }
            return;
        }
        super.applyDamage(source, amount);
    }

    // --- Interaction Callback ---
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        // --- 1. Pre-Checks ---
        if (this.hasPassenger(player)) return ActionResult.PASS;
        if (this.interactionCooldown > 0) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        World world = this.getWorld();

        // --- 2. Global Interactions ---
        ActionResult result = HamsterInteractionUtil.handleDebugToggle(this, player, stack, hand);
        if (result != ActionResult.PASS) return result;

        result = HamsterInteractionUtil.handleGeneticsVisualizer(this, player, stack, hand);
        if (result != ActionResult.PASS) return result;

        result = HamsterInteractionUtil.handleTagGame(this, player, hand);
        if (result != ActionResult.PASS) return result;

        result = HamsterInteractionUtil.handleTaming(this, player, stack, hand);
        if (result != ActionResult.PASS) return result;

        // --- 3. Untamed Fallback ---
        if (!this.isTamed()) return super.interactMob(player, hand);

        // --- 4. Owner Interactions ---
        if (this.isOwner(player)) {

            result = HamsterInteractionUtil.handleBedLinking(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleArmorEquip(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleStateRestoration(this, player, hand);
            if (result != ActionResult.PASS) return result;

            // State reset that falls through
            if (this.getDozingPhase() != DozingPhase.NONE) {
                HamsterSleepUtil.resetSleepState(this);
            }

            result = HamsterInteractionUtil.handleMouthItemReturn(this, player, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleAggressionToggle(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleAccessoryInteraction(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleShearing(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleBabyUnlink(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleShoulderMount(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleInventoryOpen(this, player, hand);
            if (result != ActionResult.PASS) return result;

            result = HamsterInteractionUtil.handleFeeding(this, player, stack, hand);
            if (result != ActionResult.PASS) return result;

            // Vanilla Fallback
            if (!player.isSneaking()) {
                ActionResult vanillaResult = super.interactMob(player, hand);
                if (vanillaResult.isAccepted()) return vanillaResult;

                // Sitting Toggle (Final Fallback for Owners)
                if (!world.isClient()) {
                    this.setSitting(!this.isSitting());
                    this.setJumping(false);
                    this.getNavigation().stop();
                    this.setTarget(null);
                }
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        // --- 5. Non-Owner Fallback ---
        return super.interactMob(player, hand);
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
    public boolean isSitting() {
        return getHamsterFlag(SITTING_FLAG)
                || getHamsterFlag(SLEEPING_FLAG)
                || getHamsterFlag(KNOCKED_OUT_FLAG)
                || getHamsterFlag(SULKING_FLAG);
    }

    @Override
    public boolean isPushable() {
        // Not pushable if AI disabled or sleeping in bed
        if (this.isAiDisabled() || (this.isSleeping() && this.getLinkedBedPos().isPresent())) {
            return false;
        }
        return super.isPushable();
    }

    // Skip physics checks entirely if AI disabled
    @Override
    public void pushAwayFrom(Entity entity) {
        if (this.isAiDisabled()) return;
        super.pushAwayFrom(entity);
    }

    @Override
    protected void pushAway(Entity entity) {
        if (this.isAiDisabled()) return;
        super.pushAway(entity);
    }

    @Override
    public boolean isCollidable() {
        return !this.isAiDisabled() && super.isCollidable();
    }

    @Override
    public boolean collidesWith(Entity other) {
        return !this.isAiDisabled() && super.collidesWith(other);
    }

    @Override
    public boolean handleFallDamage(
            float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void changeLookDirection(double cursorX, double cursorY) {
        if (this.isSleeping()) return;
        super.changeLookDirection(cursorX, cursorY);
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
    public void setSitting(boolean sitting) {
        // Keep indirect state changes silent
        this.setSitting(sitting, true);
    }

    // --- Equipment and Inventory ---
    @Override
    public void setStack(int slot, ItemStack stack) {
        ItemStack oldStack = this.items.get(slot).copy();
        this.getItems().set(slot, stack);

        if (!this.getWorld().isClient) {
            if (slot == HamsterInventoryUtil.ACCESSORY_SLOT_INDEX
                    || slot == HamsterInventoryUtil.ARMOR_SLOT_INDEX) {
                HamsterInventoryUtil.syncEquipmentTrackers(this);
            }
        }

        if (!this.getWorld().isClient
                && !this.inventoryRuntimeState.loadingNbt
                && !this.inventoryRuntimeState.silentUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, stack);
        }

        this.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack oldStack = this.getStack(slot).copy();
        ItemStack result = ImplementedInventory.super.removeStack(slot);
        ItemStack newStack = this.getStack(slot);

        if (!this.getWorld().isClient
                && !this.inventoryRuntimeState.loadingNbt
                && !this.inventoryRuntimeState.silentUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, newStack);
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack oldStack = this.getStack(slot).copy();
        ItemStack result = ImplementedInventory.super.removeStack(slot, amount);
        ItemStack newStack = this.getStack(slot);

        if (!this.getWorld().isClient
                && !this.inventoryRuntimeState.loadingNbt
                && !this.inventoryRuntimeState.silentUpdate) {
            HamsterInventoryUtil.handleSlotUpdateSounds(this, slot, oldStack, newStack);
        }
        return result;
    }

    // Expose custom armor as feet equipment for vanilla systems such as Frost Walker and Thorns
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

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void markDirty() {
        if (!this.getWorld().isClient()) {
            HamsterInventoryUtil.synchronizeVisualState(this);
        }
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return HamsterInventoryUtil.isValidForSlot(slot, stack);
    }

    // --- Riding and Movement ---
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
     * @return The {@link Text} component to be used as the screen's title.
     */
    @Override
    public Text getDisplayName() {
        // If the entity has a custom name from a name tag, always use that.
        if (this.hasCustomName()) {
            return super.getDisplayName();
        }

        // If no custom name, check the config for the default name.
        if (Configs.AHP_MAIN.useHampterName) {
            return Text.translatable("entity.adorablehamsterpets.hampter");
        }

        // Otherwise, use the default vanilla behavior, which will resolve to
        // "entity.adorablehamsterpets.hamster".
        return super.getDisplayName();
    }

    /**
     * Gets the base name for the hamster. This will be the hamster's custom name if it has one,
     * otherwise it defaults to the configured fallback name ("Hampter" or "Hamster").
     *
     * @return The {@link Text} component to be used as the entity's name.
     */
    @Override
    public Text getName() {
        // Name tag gets priority
        if (this.hasCustomName()) {
            return super.getName();
        }

        // If no custom name, check config for default
        if (Configs.AHP_MAIN.useHampterName) {
            return Text.translatable("entity.adorablehamsterpets.hampter");
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
    public boolean isInAttackRange(LivingEntity entity) {
        return HamsterCombatUtil.isInAttackRange(this, entity);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return HamsterCombatUtil.canAcquireTarget(this, target) && super.canTarget(target);
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return HamsterCombatUtil.canAttackWithOwner(this, target, owner);
    }

    /**
     * Applies aggression-mode, protected-target, and Standard combat-window rules whenever a goal
     * changes the hamster's target.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
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
    public void setTamed(boolean tamed, boolean updateAttributes) {
        // --- Tamed State and Attributes ---
        super.setTamed(tamed, updateAttributes);
        if (tamed) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                    .setBaseValue(Configs.AHP_MAIN.tamedMaxHealth.get());
            this.setHealth(this.getMaxHealth()); // Set health to the updated maximum
            // Set the base attack damage attribute to the defined melee damage when tamed.
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                    .setBaseValue(Configs.AHP_MAIN.meleeDamage.get());
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                    .setBaseValue(Configs.AHP_MAIN.wildMaxHealth.get());
            // Reset attack damage if untamed
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
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
    public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) {
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
    public boolean isBreedingItem(ItemStack stack) {
        return ConfigDataCache.isStandardFood(stack);
    }

    // --- Animation ---
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // --- Navigation and Positioning ---
    @Override
    protected EntityNavigation createNavigation(World world) {
        if (Configs.AHP_MAIN.avoidUnlinkedBeds) {
            return new HamsterNavigation(this, world);
        } else {
            return new MobNavigation(this, world);
        }
    }

    /**
     * Calculates the position where the passenger sits.
     *
     * <p>Uses {@link HamsterRidingUtil.HamsterSeatOffsets} to ensure the rider remains visually
     * anchored to the hamster's back, dynamically compensating for the entity's scale factor.
     */
    @Override
    protected Vec3d getPassengerAttachmentPos(
            Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return HamsterRidingUtil.getPassengerAttachmentPos(this, passenger);
    }

    @Override
    protected BodyControl createBodyControl() {
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
        // Check if the selected sound is a begging sound
        if (soundEvent != null && HamsterSoundUtil.isBeggingSound(soundEvent)) {
            // If it's a begging sound, play it with lower volume
            this.playSound(soundEvent, 0.8F, this.getSoundPitch());
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
                    this.getCommandTags().contains("3d_layout_center");
            if (this.threeDimensionalLayoutState.isCenter) {
                this.threeDimensionalLayoutState.parsedY = this.getBlockPos().getY();
                for (String tag : this.getCommandTags()) {
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
        if (this.threeDimensionalLayoutState.isCenter && !this.getWorld().isClient()) {
            if (Configs.AHP_MAIN.continuousGeneticsCylinder || this.age <= 20) {
                BlockPos cylinderBase =
                        new BlockPos(
                                this.getBlockPos().getX(),
                                this.threeDimensionalLayoutState.parsedY,
                                this.getBlockPos().getZ());
                double bobbingAmplitude = this.threeDimensionalLayoutState.parsedScale / 2.0;
                double yOffset = bobbingAmplitude - 0.5;

                ParticleEffectsUtil.spawnSpinningRing(
                        this.getWorld(),
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
        if (!this.getWorld().isClient()) {
            this.animScheduler.tick(this.getWorld().getTime());
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
            if (this.sulkTimer == 0 && this.isSulking() && !this.getWorld().isClient()) {
                this.setSulking(false);
                this.setSitting(false, true);
            }
        }

        // --- 4. Settle Thump ---
        if (this.thumpSoundState.delayTicks > 0) {
            this.thumpSoundState.delayTicks--;
            if (this.thumpSoundState.delayTicks == 0 && !this.getWorld().isClient()) {
                this.getWorld()
                        .playSound(
                                null,
                                this.getBlockPos(),
                                ModSounds.HAMSTER_THUMP.get(),
                                SoundCategory.NEUTRAL,
                                this.thumpSoundState.volume,
                                1.5f);
            }
        }
    }

    private void tickPreSuperBehaviors() {
        // --- 1. Bed Leaf Particles ---
        HamsterBedUtil.tickBedLeafParticles(this);

        // --- 2. Ambient Sitting ---
        if (!this.getWorld().isClient()) {
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
        if (!this.getWorld().isClient() && this.isTamed() && !this.isKnockedOut()) {
            HamsterSleepUtil.tickTamedSleepLogic(this);
        }

        // --- 5. Auto-Petting ---
        if (!this.getWorld().isClient()
                && Configs.AHP_MAIN.enablePetting
                && Platform.isModLoaded("punchy")) {
            // Check twice per second
            if (this.age % 10 == 0) {
                if (this.isTamed() && this.getOwner() instanceof ServerPlayerEntity serverPlayer) {
                    // Ensure player is not looking inside a GUI
                    if (serverPlayer.currentScreenHandler == serverPlayer.playerScreenHandler
                            && serverPlayer.isSneaking()) {
                        // Ensure hamster is in a pet-able state & within 5 blocks
                        if (!this.isShoulderPet()
                                && !this.isAiDisabled()
                                && !this.isSleeping()
                                && !this.isKnockedOut()
                                && !this.isSulking()
                                && !this.isCelebratingDiamond()
                                && !this.isCelebratingBaby()
                                && !this.isFrozenMovement()
                                && this.squaredDistanceTo(serverPlayer) < 25.0) {
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
        if (this.isTouchingWater() || this.isInLava()) {

            // Bypass node-by-node navigation that causes orbital loops in water
            if (this.getNavigation().isFollowingPath()
                    && this.getNavigation().getTargetPos() != null) {
                BlockPos finalTarget = this.getNavigation().getTargetPos();
                this.getLookControl()
                        .lookAt(
                                finalTarget.getX() + 0.5,
                                finalTarget.getY() + 0.5,
                                finalTarget.getZ() + 0.5,
                                25.0f,
                                25.0f);
                this.setYaw(this.headYaw);
                this.bodyYaw = this.headYaw;
                this.getMoveControl()
                        .moveTo(
                                finalTarget.getX() + 0.5,
                                finalTarget.getY() + 0.5,
                                finalTarget.getZ() + 0.5,
                                1.2D);
            }

            Vec3d velocity = this.getVelocity();

            double newVelX = velocity.x;
            double newVelY = velocity.y;
            double newVelZ = velocity.z;

            // Vertical motion
            Vec3d lookVec = this.getRotationVec(1.0F);

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
                    Math.abs(this.forwardSpeed) > 0.01F || Math.abs(this.sidewaysSpeed) > 0.01F;

            if (isTryingToMove) {
                Vec3d targetDir = new Vec3d(lookVec.x, 0.0, lookVec.z).normalize();

                // Smooth thrust direction
                if (this.smoothedWaterThrust.lengthSquared() == 0.0) {
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
                this.smoothedWaterThrust = this.smoothedWaterThrust.multiply(0.8D);
            }

            this.setVelocity(newVelX, newVelY, newVelZ);

        } else if (this.smoothedWaterThrust.lengthSquared() > 0.0) {
            // Reset smoothed thrust when out of water
            this.smoothedWaterThrust = Vec3d.ZERO;
        }

        // --- 2. Armor and Attribute Updates ---
        if (!this.getWorld().isClient) {
            ItemStack currentArmor = this.getArmorStack();
            if (!ItemStack.areEqual(currentArmor, this.armorRuntimeState.lastStack)) {
                HamsterPhysicsUtil.updateArmorModifiers(this, currentArmor);
                this.armorRuntimeState.lastStack = currentArmor.copy();
            }
        }

        // --- 3. Navigation and Config Sync ---
        if (!this.getWorld().isClient() && this.age % 20 == 0) { // Check once per second
            this.updateNavigation();

            // Periodically validate armor attributes to catch Config changes
            HamsterPhysicsUtil.updateArmorModifiers(this, this.getArmorStack());
        }

        // --- 4. Sulking Gravity ---
        // This runs on the server to ensure physics are authoritative.
        if (!this.getWorld().isClient()) {
            // If the hamster is sulking, not on the ground, and is currently falling (negative Y
            // velocity)
            if (this.isSulking() && !this.isOnGround() && this.getVelocity().y < 0) {
                // Apply an extra downward force to make it fall faster.
                // -0.08 is the standard gravity value, so adding it again effectively doubles it.
                this.setVelocity(this.getVelocity().add(0.0, -1.0, 0.0));
                this.velocityDirty = true; // Ensure client sees the change
            }
        }
    }

    private void tickServerLifecycle() {
        World world = this.getWorld();
        if (!world.isClient()) {

            // --- 1. Age ---
            //   1 real day = 86,400s * 20 MC ticks/s = 1,728,000 MC ticks
            //   1,728,000 / 24,000 = 72 MC ticks per age tick
            int ageProgressInterval = Configs.AHP_UI.displayAgeInIrlTime ? 72 : 1;
            if (this.age % ageProgressInterval == 0) {
                this.totalAgeTicks++;
            }

            // --- 2. Water Pathfinding and Escape ---
            if (this.age % 10 == 0) {
                String activeGoal = this.getActiveCustomGoalName();
                boolean isLooting =
                        activeGoal.startsWith(HamsterPlayWithItemGoal.class.getSimpleName())
                                || activeGoal.startsWith(
                                        HamsterSnackOnCropGoal.class.getSimpleName())
                                || activeGoal.startsWith(
                                        HamsterSnackOnItemGoal.class.getSimpleName());

                if (this.isTouchingWater() || isLooting) {
                    if (this.getPathfindingPenalty(PathNodeType.WATER) != 0.0F) {
                        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
                    }

                    // If in water, not looting, and not currently moving somewhere, actively seek
                    // land
                    if (this.isTouchingWater() && !isLooting && this.getNavigation().isIdle()) {
                        HamsterMovementUtil.findNearbyLand(world, this.getBlockPos(), 6, this)
                                .ifPresent(
                                        landPos -> {
                                            this.getNavigation()
                                                    .startMovingTo(
                                                            landPos.getX() + 0.5,
                                                            landPos.getY(),
                                                            landPos.getZ() + 0.5,
                                                            1.0D);
                                            this.setActiveCustomGoalName("Escaping Water");
                                        });
                    }
                } else {
                    if (this.getPathfindingPenalty(PathNodeType.WATER) != 16.0F) {
                        this.setPathfindingPenalty(PathNodeType.WATER, 16.0F);
                    }
                }
            }

            // --- 3. Throw Cooldown Sync ---
            boolean hasThrowCooldown = this.throwCooldownEndTick > world.getTime();
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
            this.dataTracker.set(EXACT_AGE, this.getBreedingAge());

            // --- 7. Day/Night Wake-Up ---
            if (!Configs.AHP_MAIN.circadianChaos.get() && HamsterBedUtil.isSleepingInBed(this)) {
                // If rescued, bypass time check entirely. Hamster stays asleep
                if (!this.isRescueSleeping()) {
                    boolean isSleepTime =
                            Configs.AHP_MAIN.sleepDuringDay.get() ? world.isDay() : world.isNight();
                    if (!isSleepTime) {
                        // If it's wake-up time, and delay timer has not yet been started
                        if (this.sleepRuntimeState.wakeUpFromBedDelay == 0
                                && this.sleepRuntimeState.goToBedCooldown == 0) {
                            this.sleepRuntimeState.wakeUpFromBedDelay =
                                    this.random.nextBetween(5, 60); // Set random 0.25s to 3s delay
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
                    this.markDirty();
                }
            }

            // --- 11. Auto-Eating ---
            // Stage 1: Check eligibility and start considering
            if (this.isTamed()
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
                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.7F, 1.3F);
                    ParticleEffectsUtil.spawnParticles(
                            world,
                            new Vec3d(
                                    this.getX(), this.getY() + this.getHeight() / 2.0, this.getZ()),
                            new ItemStackParticleEffect(ParticleTypes.ITEM, foodToEat.split(1)),
                            5,
                            new Vec3d(0.1, 0.1, 0.1),
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

                if (this.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
                    ModCriteria.HAMSTER_AUTO_FED.get().trigger(serverPlayerOwner, this);
                }
            }

            // --- 12. Diamond Celebration Effects ---
            if (!this.getWorld().isClient()) {
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
                                this.getWorld()
                                        .playSound(
                                                null,
                                                this.currentOreTarget,
                                                sparkleSound,
                                                SoundCategory.NEUTRAL,
                                                1.0F,
                                                1.0F);
                            } else { // Fallback to hamster pos if ore target is somehow null
                                this.getWorld()
                                        .playSound(
                                                null,
                                                this.getBlockPos(),
                                                sparkleSound,
                                                SoundCategory.NEUTRAL,
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
                                ParticleTypes.TRIAL_SPAWNER_DETECTION_OMINOUS,
                                4,
                                0.3,
                                0.3,
                                0.01,
                                0.7);

                        // 2. Firework Particles above Ore
                        if (this.currentOreTarget != null && this.random.nextInt(4) == 0) {
                            BlockPos particlePos = this.currentOreTarget.up();
                            ParticleEffectsUtil.spawnParticles(
                                    this.getWorld(),
                                    Vec3d.ofCenter(particlePos), // Center of block above
                                    ParticleTypes.FIREWORK,
                                    1,
                                    new Vec3d(0.2, 0.35, 0.2),
                                    0.003);
                        }
                    }

                    //  Begging Sounds
                    if (this.celebrationRuntimeState.diamondSoundTicks <= 0) {
                        SoundEvent celebrationSound =
                                ModSounds.getRandomSoundFrom(
                                        ModSounds.HAMSTER_BEG_SOUNDS, this.random);
                        if (celebrationSound != null) {
                            this.getWorld()
                                    .playSound(
                                            null,
                                            this.getBlockPos(),
                                            celebrationSound,
                                            SoundCategory.NEUTRAL,
                                            0.8F,
                                            this.getSoundPitch());
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
                    this.getWorld()
                            .playSound(
                                    null,
                                    this.getBlockPos(),
                                    ModSounds.ALARM_ORCHESTRA_HIT.get(),
                                    SoundCategory.NEUTRAL,
                                    1.0F,
                                    1.0F);
                }

                // Delayed Single Shocked Sound
                if (this.celebrationRuntimeState.sulkShockedSoundDelayTicks
                        == 1) { // Play when this timer reaches 1
                    this.getWorld()
                            .playSound(
                                    null,
                                    this.getBlockPos(),
                                    ModSounds.HAMSTER_SHOCKED.get(),
                                    SoundCategory.NEUTRAL,
                                    1.0F,
                                    1.0F);
                }

                // Angry Smoke Particles above Gold Ore
                if (this.celebrationRuntimeState.sulkFailParticleTicks > 0
                        && this.currentOreTarget != null) {
                    if (this.random.nextInt(3) == 0) {
                        BlockPos particlePos = this.currentOreTarget.up();
                        ParticleEffectsUtil.spawnParticles(
                                this.getWorld(),
                                Vec3d.ofCenter(particlePos),
                                ParticleTypes.SMOKE,
                                2,
                                new Vec3d(0.3, 0.3, 0.3),
                                0.005);
                    }
                }

                // Black Entity Effect Particles on Hamster
                if (this.celebrationRuntimeState.sulkEntityEffectTicks > 0) {
                    if (this.random.nextInt(5) == 0) {
                        EntityEffectParticleEffect darkGrayEffect =
                                EntityEffectParticleEffect.create(
                                        ParticleTypes.ENTITY_EFFECT, 0.3f, 0.3f, 0.3f);
                        ParticleEffectsUtil.spawnParticlesOnEntity(
                                this, darkGrayEffect, 1, 0.6, 0.5, 0.005, 0.1);
                    }
                }
            }

            // --- 14. Pacifist Break ---
            if (Configs.AHP_MAIN.pacifistBreakOnOwnerAttack
                    && this.getAggressionState() == AggressionState.PACIFIST
                    && this.isTamed()) {
                if (this.getOwner() instanceof PlayerEntity owner && owner.getAttacking() != null) {
                    // Check if the attack was recent to prevent stale targets
                    if (owner.age - owner.getLastAttackTime() < 100) {
                        this.setAggressionState(AggressionState.STANDARD);

                        // Audio Feedback
                        SoundEvent sound =
                                ModSounds.getRandomSoundFrom(
                                        ModSounds.HAMSTER_HURT_SOUNDS, this.getRandom());
                        if (sound != null) {
                            this.getWorld()
                                    .playSound(
                                            null,
                                            this.getBlockPos(),
                                            sound,
                                            SoundCategory.NEUTRAL,
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
        World world = this.getWorld();

        // --- 1. Rolling Animation ---
        if (world.isClient()) {
            this.prevClientRollTimer = this.clientRollTimer;
            boolean isRolling = false;

            var manager = this.getAnimatableInstanceCache().getManagerForId(this.getId());
            if (manager != null) {
                var controller = manager.getAnimationControllers().get("mainController");
                if (controller != null) {
                    var currentAnim = controller.getCurrentAnimation();
                    if (currentAnim != null
                            && "anim_hamster_sitting_roll".equals(currentAnim.animation().name())) {
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
        if (world.isClient && this.hasGreenBeanBuff()) {
            if (this.random.nextInt(2) == 0) {
                ParticleEffectsUtil.spawnMotionTrail(
                        this, ParticleTypes.WHITE_SMOKE, 3, 1.4, 0.025, 1.7, 0.17);
            }
        }

        // --- 3. Taunting Particles ---
        if (this.isTaunting()) {
            if (this.random.nextInt(7) == 0) {
                ParticleEffectsUtil.spawnParticlesOnEntity(
                        this, ParticleTypes.INSTANT_EFFECT, 2, 1.2, 0.5, 0.5, 0.2);
            }
        }

        // --- 4. Fall Pitch Interpolation ---
        if (world.isClient) {
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
                    MathHelper.clamp(this.clientFallPitchProgress, 0.0f, 1.0f);

            // --- 5. Swim Pitch Interpolation ---
            this.prevClientSwimPitch = this.clientSwimPitch;

            if (this.isTouchingWater() || this.isInLava()) {
                Vec3d velocity = this.getVelocity();
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
        World world = this.getWorld();

        // --- 1. Jukebox Dancing ---
        if (!world.isClient() && this.age % 20 == 0) {
            boolean dancing = false;
            boolean isSniffingForOre =
                    this.getActiveCustomGoalName()
                            .startsWith(HamsterSniffForOreGoal.class.getSimpleName());

            if (!this.isSitting()
                    && !this.isSleeping()
                    && !this.isKnockedOut()
                    && !this.isSulking()
                    && !this.isPlayingTag()
                    && !this.isFrozenMovement()
                    && !this.isCelebratingBaby()
                    && !this.isCelebratingDiamond()
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
    private RegistryWrapper.WrapperLookup getRegistryLookup() {
        return this.getWorld().getRegistryManager();
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
