package net.dawson.adorablehamsterpets.mixin.server;

import com.mojang.authlib.GameProfile;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.SunflowerBlock;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.config.*;
import net.dawson.adorablehamsterpets.entity.AI.HamsterSniffForOreGoal;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterProjectileEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.*;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityAccessor {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    @Unique private static final int CHECK_INTERVAL_TICKS = 20;
    @Unique private static final int AHP_GUIDEBOOK_CHECK_INTERVAL_TICKS = 20;
    @Unique private static final long HEIST_MEMORY_DURATION = 24000L; // 1 Minecraft Day
    @Unique private static final String AHP_NBT_GUIDEBOOK_HAS_KEY = "AHPHasGuideBook";
    @Unique private static final String AHP_NBT_GUIDEBOOK_INIT_KEY = "AHPGuideBookTrackingInit";

    @Unique
    private static final List<String> DISMOUNT_MESSAGE_KEYS = Arrays.asList(
            "message.adorablehamsterpets.dismount.1", "message.adorablehamsterpets.dismount.2",
            "message.adorablehamsterpets.dismount.3", "message.adorablehamsterpets.dismount.4",
            "message.adorablehamsterpets.dismount.5", "message.adorablehamsterpets.dismount.6"
    );

    @Unique
    private record ScheduledTask(long executionTick, Runnable action) {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Shoulder Data ---
    @Unique private NbtCompound ahp$hamsterState = new NbtCompound();
    @Unique private transient ClientShoulderHamsterData adorablehamsterpets$clientHamsterState;
    @Unique private final transient ArrayDeque<ShoulderLocation> adorablehamsterpets$mountOrderQueue = new ArrayDeque<>();

    // --- Timers & Cooldowns ---
    @Unique private int adorablehamsterpets$diamondCheckTimer = 0;
    @Unique private int adorablehamsterpets$creeperCheckTimer = 0;
    @Unique private int adorablehamsterpets$diamondSoundCooldownTicks = 0;
    @Unique private int adorablehamsterpets$creeperSoundCooldownTicks = 0;
    @Unique private int ahp$guideBookCheckTimer = 0;
    @Unique private int ahp$guideBookCheckGracePeriodTimer = 0;
    @Unique private int ahp$sunflowerCheckTimer = 0;
    @Unique private int ahp$tagGamesPlayedToday = 0;
    @Unique private long ahp$lastTagGameDayTime = 0;
    @Unique private int ahp$hamstersFedForBreeding = 0;
    @Unique private long ahp$lastBreedingTime = 0;

    // --- Genetics Tracking ---
    @Unique private final Set<Integer> ahp$tamedGenomes = new HashSet<>();
    @Unique private final Set<Integer> ahp$bredGenomes = new HashSet<>();
    @Unique private UUID ahp$geneticParent1Uuid = null;
    @Unique private UUID ahp$geneticParent2Uuid = null;

    // --- Teleport Tracking ---
    @Unique private Vec3d ahp$lastTickPos = null;
    @Unique private RegistryKey<World> ahp$lastTickDimension = null;
    @Unique private final List<NbtCompound> ahp$inTransitHamsters = new ArrayList<>();
    @Unique private int ahp$transitTimer = 0;

    // --- State Flags & Trackers ---
    @Unique private final Map<String, Integer> ahp$randomMessageIndices = new HashMap<>();
    @Unique private String adorablehamsterpets$lastDismountMessageKey = "";
    @Unique private boolean adorablehamsterpets$isDiamondAlertConditionMet = false;
    @Unique private int adorablehamsterpets$lastGoldMessageIndex = -1;
    @Unique private boolean ahp$cachedHasGuideBook = false;
    @Unique private boolean ahp$guideBookTrackingInitialized = false;
    @Unique private static final TrackedData<Integer> AHP_CROWN_THEME = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Boolean> AHP_HAS_USED_CROWN_TRIAL = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private static final TrackedData<Integer> AHP_CROWN_TRIAL_TICKS = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private int ahp$crownAudioTimer = 0;

    // --- Collections ---
    @Unique private final List<ScheduledTask> adorablehamsterpets$scheduledTasks = new ArrayList<>();
    @Unique private final List<TreeHeistUtil.HeistRecord> ahp$heistHistory = new ArrayList<>();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors & Initialization
     * ────────────────────────────────────────────────────────────────────────────*/

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void adorablehamsterpets$onInit(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        // Client-side visual setup
        if (world.isClient) {
            this.adorablehamsterpets$clientHamsterState = new ClientShoulderHamsterData();
        }
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void adorablehamsterpets$initCrownData(CallbackInfo ci) { // 1.20.1: "zero arguments" = still requires CallbackInfo
        this.dataTracker.startTracking(AHP_CROWN_THEME, -1); // Default to disabled
        this.dataTracker.startTracking(AHP_HAS_USED_CROWN_TRIAL, false);
        this.dataTracker.startTracking(AHP_CROWN_TRIAL_TICKS, 0);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks (Mixin Injections)
     * ────────────────────────────────────────────────────────────────────────────*/

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void adorablehamsterpets$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        // --- Action Bar Randomized Message History ---
        if (!this.ahp$randomMessageIndices.isEmpty()) {
            NbtCompound msgNbt = new NbtCompound();
            this.ahp$randomMessageIndices.forEach(msgNbt::putInt);
            nbt.put("AHPRandomMessageIndices", msgNbt);
        }

        // --- Shoulder Data ---
        if (!this.ahp$hamsterState.isEmpty()) {
            nbt.put("ShoulderHamsters", this.ahp$hamsterState);
        }

        // --- Mount Queue ---
        if (!this.adorablehamsterpets$mountOrderQueue.isEmpty()) {
            NbtList mountOrderList = new NbtList();
            for (ShoulderLocation location : this.adorablehamsterpets$mountOrderQueue) {
                mountOrderList.add(NbtString.of(location.name()));
            }
            nbt.put("MountOrderQueue", mountOrderList);
        }

        // --- Tree Heist ---
        if (!this.ahp$heistHistory.isEmpty()) {
            NbtList historyList = new NbtList();
            long currentTime = this.getWorld().getTime();

            for (TreeHeistUtil.HeistRecord record : this.ahp$heistHistory) {
                if (currentTime - record.timestamp() < HEIST_MEMORY_DURATION) {
                    NbtCompound tag = new NbtCompound();
                    tag.putLong("x", record.pos().getX());
                    tag.putLong("y", record.pos().getY());
                    tag.putLong("z", record.pos().getZ());
                    tag.putLong("t", record.timestamp());
                    historyList.add(tag);
                }
            }
            if (!historyList.isEmpty()) {
                nbt.put("AHPHeistHistory", historyList);
            }
        }

        // --- Genetics ---
        if (!this.ahp$tamedGenomes.isEmpty()) {
            int[] tamedArray = this.ahp$tamedGenomes.stream().mapToInt(Integer::intValue).toArray();
            nbt.putIntArray("AHPTamedGenomes", tamedArray);
        }
        if (!this.ahp$bredGenomes.isEmpty()) {
            int[] bredArray = this.ahp$bredGenomes.stream().mapToInt(Integer::intValue).toArray();
            nbt.putIntArray("AHPBredGenomes", bredArray);
        }
        if (this.ahp$geneticParent1Uuid != null) nbt.putUuid("AHPGeneticParent1", this.ahp$geneticParent1Uuid);
        if (this.ahp$geneticParent2Uuid != null) nbt.putUuid("AHPGeneticParent2", this.ahp$geneticParent2Uuid);

        // --- Tag Game ---
        nbt.putInt("AHPTagGamesPlayed", this.ahp$tagGamesPlayedToday);
        nbt.putLong("AHPLastTagTime", this.ahp$lastTagGameDayTime);

        // --- Player Breeding Limit ---
        nbt.putInt("AHPHamstersFedForBreeding", this.ahp$hamstersFedForBreeding);
        nbt.putLong("AHPLastBreedingTime", this.ahp$lastBreedingTime);

        // --- Guidebook ---
        nbt.putBoolean(AHP_NBT_GUIDEBOOK_HAS_KEY, this.ahp$cachedHasGuideBook);
        nbt.putBoolean(AHP_NBT_GUIDEBOOK_INIT_KEY, this.ahp$guideBookTrackingInitialized);

        // --- Supporter Crown Trial ---
        nbt.putBoolean("AHPHasUsedCrownTrial", this.dataTracker.get(AHP_HAS_USED_CROWN_TRIAL));

        // --- Teleport Rescue  ---
        if (!this.ahp$inTransitHamsters.isEmpty()) {
            NbtList transitList = new NbtList();
            for (NbtCompound transitNbt : this.ahp$inTransitHamsters) {
                transitList.add(transitNbt);
            }
            nbt.put("AHPInTransitHamsters", transitList);
            nbt.putInt("AHPTransitTimer", this.ahp$transitTimer);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void adorablehamsterpets$readNbt(NbtCompound nbt, CallbackInfo ci) {
        // --- Generic Message History ---
        this.ahp$randomMessageIndices.clear();
        if (nbt.contains("AHPRandomMessageIndices", NbtElement.COMPOUND_TYPE)) {
            NbtCompound msgNbt = nbt.getCompound("AHPRandomMessageIndices");
            for (String key : msgNbt.getKeys()) {
                this.ahp$randomMessageIndices.put(key, msgNbt.getInt(key));
            }
        }

        // --- Migrate Legacy Data ---
        if (nbt.contains("ShoulderHamster", NbtElement.COMPOUND_TYPE)) {
            NbtCompound oldHamsterNbt = nbt.getCompound("ShoulderHamster");
            if (!oldHamsterNbt.isEmpty()) {
                NbtCompound newShoulderPetsNbt = new NbtCompound();
                newShoulderPetsNbt.put(ShoulderLocation.RIGHT_SHOULDER.name(), oldHamsterNbt);
                this.ahp$hamsterState = newShoulderPetsNbt;
                this.adorablehamsterpets$mountOrderQueue.clear();
                this.adorablehamsterpets$mountOrderQueue.add(ShoulderLocation.RIGHT_SHOULDER);
                nbt.remove("ShoulderHamster"); // remove old tag to complete migration
                AdorableHamsterPets.LOGGER.info("Migrated legacy shoulder hamster data for player {}.", this.getDisplayName().getString());
            }
        } else if (nbt.contains("ShoulderHamsters", NbtElement.COMPOUND_TYPE)) {
            // standard Read
            this.ahp$hamsterState = nbt.getCompound("ShoulderHamsters");
        }

        // --- Queue Sanitization ---
        this.adorablehamsterpets$mountOrderQueue.clear();
        if (nbt.contains("MountOrderQueue", NbtElement.LIST_TYPE)) {
            NbtList mountOrderList = nbt.getList("MountOrderQueue", NbtElement.STRING_TYPE);
            Set<ShoulderLocation> seenLocations = new HashSet<>();

            for (NbtElement element : mountOrderList) {
                try {
                    ShoulderLocation location = ShoulderLocation.valueOf(element.asString());
                    // Deduplicate and ensure data actually exists for this slot
                    if (!seenLocations.contains(location) && !this.getShoulderHamster(location).isEmpty()) {
                        this.adorablehamsterpets$mountOrderQueue.add(location);
                        seenLocations.add(location);
                    }
                } catch (IllegalArgumentException e) {
                    AdorableHamsterPets.LOGGER.warn("Found invalid ShoulderLocation name in NBT: {}", element.asString());
                }
            }
        }

        // --- Self-Healing ---
        // If data exists but queue is empty (corruption), rebuild it
        if (this.adorablehamsterpets$mountOrderQueue.isEmpty() && this.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.info("Player {} has shoulder hamsters but empty mount queue. Rebuilding...", this.getDisplayName().getString());
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!this.getShoulderHamster(location).isEmpty()) {
                    this.adorablehamsterpets$mountOrderQueue.addLast(location);
                }
            }
        }

        // --- Genetics ---
        this.ahp$tamedGenomes.clear();
        if (nbt.contains("AHPTamedGenomes", NbtElement.INT_ARRAY_TYPE)) {
            for (int hash : nbt.getIntArray("AHPTamedGenomes")) {
                this.ahp$tamedGenomes.add(hash);
            }
        }
        this.ahp$bredGenomes.clear();
        if (nbt.contains("AHPBredGenomes", NbtElement.INT_ARRAY_TYPE)) {
            for (int hash : nbt.getIntArray("AHPBredGenomes")) {
                this.ahp$bredGenomes.add(hash);
            }
        }
        if (nbt.containsUuid("AHPGeneticParent1")) this.ahp$geneticParent1Uuid = nbt.getUuid("AHPGeneticParent1");
        else this.ahp$geneticParent1Uuid = null;
        if (nbt.containsUuid("AHPGeneticParent2")) this.ahp$geneticParent2Uuid = nbt.getUuid("AHPGeneticParent2");
        else this.ahp$geneticParent2Uuid = null;

        // --- Tag Game ---
        this.ahp$tagGamesPlayedToday = nbt.getInt("AHPTagGamesPlayed");
        this.ahp$lastTagGameDayTime = nbt.getLong("AHPLastTagTime");

        // --- Player Breeding Limit ---
        this.ahp$hamstersFedForBreeding = nbt.getInt("AHPHamstersFedForBreeding");
        this.ahp$lastBreedingTime = nbt.getLong("AHPLastBreedingTime");

        // --- Guidebook ---
        if (nbt.contains(AHP_NBT_GUIDEBOOK_HAS_KEY, NbtElement.BYTE_TYPE)) {
            this.ahp$cachedHasGuideBook = nbt.getBoolean(AHP_NBT_GUIDEBOOK_HAS_KEY);
        }
        if (nbt.contains(AHP_NBT_GUIDEBOOK_INIT_KEY, NbtElement.BYTE_TYPE)) {
            this.ahp$guideBookTrackingInitialized = nbt.getBoolean(AHP_NBT_GUIDEBOOK_INIT_KEY);
        }

        // --- Supporter Crown Trial ---
        if (nbt.contains("AHPHasUsedCrownTrial", NbtElement.BYTE_TYPE)) {
            boolean hasUsed = nbt.getBoolean("AHPHasUsedCrownTrial");
            // Wipe slate clean if in dev environment
            if (Platform.isDevelopmentEnvironment()) {
                hasUsed = false;
            }
            this.dataTracker.set(AHP_HAS_USED_CROWN_TRIAL, hasUsed);
        }

        // --- Teleport Rescue ---
        this.ahp$inTransitHamsters.clear();
        if (nbt.contains("AHPInTransitHamsters", NbtElement.LIST_TYPE)) {
            NbtList transitList = nbt.getList("AHPInTransitHamsters", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < transitList.size(); i++) {
                this.ahp$inTransitHamsters.add(transitList.getCompound(i));
            }
            this.ahp$transitTimer = nbt.getInt("AHPTransitTimer");
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ahp$checkTeleport(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        // Only run on the server for living players
        if (self.getWorld().isClient() || !self.isAlive()) return;

        Vec3d currentPos = self.getPos();
        if (this.ahp$lastTickPos != null && this.ahp$lastTickDimension != null) {
            boolean dimensionChanged = this.ahp$lastTickDimension != self.getWorld().getRegistryKey();
            double distSq = this.ahp$lastTickPos.squaredDistanceTo(self.getPos());

            // If dimension changed OR moved > 20 blocks in a single tick (400 sq dist)
            if (dimensionChanged || distSq > 400.0) {
                if (Configs.AHP.enableTeleportRescue) {
                    this.ahp$pocketFollowingHamsters(this.ahp$lastTickPos, this.ahp$lastTickDimension);
                }
            }
        }

        // Update tracking variables for the next tick
        this.ahp$lastTickPos = self.getPos();
        this.ahp$lastTickDimension = self.getWorld().getRegistryKey();
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void adorablehamsterpets$onDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!self.getWorld().isClient() && Configs.AHP.enableTeleportRescue) {
            this.ahp$pocketFollowingHamsters(self.getPos(), self.getWorld().getRegistryKey());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) return;

        // --- 1. Process In-Transit Hamsters ---
        if (self.isAlive() && this.ahp$transitTimer > 0) {
            boolean isSafeToSpawn = self.isOnGround() || self.isTouchingWater() || self.isClimbing() || self.hasVehicle();

            if (isSafeToSpawn) {
                // Schedule incoming shimmer sound to play 15 ticks after arrival window
                if (this.ahp$transitTimer == 15 && !this.ahp$inTransitHamsters.isEmpty()) {
                    this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(world.getTime() + 10, () -> {
                        world.playSound(null, this.getBlockPos(), ModSounds.MAGIC_SHIMMER.get(), SoundCategory.NEUTRAL, 1.5f, 1.2f);
                    }));
                }

                this.ahp$transitTimer--;

                if (this.ahp$transitTimer <= 0 && !this.ahp$inTransitHamsters.isEmpty()) {
                    ServerWorld newWorld = (ServerWorld) world;

                    // Track occupied blocks
                    Set<BlockPos> occupiedPositions = new HashSet<>();
                    int soundsScheduled = 0;
                    long currentWorldTime = newWorld.getTime();

                    for (NbtCompound nbt : this.ahp$inTransitHamsters) {
                        // Stagger spawns over 1-5 ticks
                        int delay = newWorld.getRandom().nextBetween(1, 5);

                        // Hard limit sound effect to 7 times per rescue event
                        boolean playSound = soundsScheduled < 7;
                        if (playSound) {
                            soundsScheduled++;
                        }

                        this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(currentWorldTime + delay, () -> {
                            HamsterEntity newHamster = ModEntities.HAMSTER.get().create(newWorld);
                            if (newHamster != null) {
                                newHamster.readNbt(nbt);

                                // --- Determine Target Position ---
                                // Default to player position
                                Vec3d baseTargetPos = self.getPos();
                                BlockPos baseTargetBlockPos = self.getBlockPos();

                                // If specific target saved, override default
                                if (nbt.containsUuid("AHPTransitTargetUuid")) {
                                    Entity transitTarget = newWorld.getEntity(nbt.getUuid("AHPTransitTargetUuid"));
                                    if (transitTarget != null) {
                                        baseTargetPos = transitTarget.getPos();
                                        baseTargetBlockPos = transitTarget.getBlockPos();
                                    }
                                }

                                // Find unique spawn point for every hamster near target
                                Optional<BlockPos> safePos = HamsterPlacementUtil.findSafeSpawnPosition(baseTargetBlockPos, newWorld, 6, occupiedPositions, newHamster);

                                // Reserve spot
                                safePos.ifPresent(occupiedPositions::add);

                                Vec3d finalBaseTargetPos = baseTargetPos;
                                Vec3d targetPos = safePos.map(Vec3d::ofBottomCenter).orElseGet(() -> {
                                    double offsetX = (newWorld.getRandom().nextDouble() - 0.5) * 3.0;
                                    double offsetZ = (newWorld.getRandom().nextDouble() - 0.5) * 3.0;
                                    return finalBaseTargetPos.add(offsetX, 0, offsetZ);
                                });

                                // --- Sledgehammer Server/Client Sync 1 ---
                                // Drop them with downward velocity
                                newHamster.refreshPositionAndAngles(targetPos.x, targetPos.y + 0.1, targetPos.z, newHamster.getYaw(), newHamster.getPitch());
                                newHamster.setVelocity(0, -0.05, 0);
                                newHamster.velocityDirty = true;
                                newHamster.getNavigation().stop();
                                newHamster.setSitting(false);

                                // Prevent flight animation upon spawning
                                newHamster.setFallFlyImmunityTicks(20);

                                newWorld.spawnEntity(newHamster);

                                // --- Sledgehammer Server/Client Sync 2 ---
                                // Force explicit delayed positional update
                                newHamster.scheduleTask(currentWorldTime + delay + 5, "sledgehammer_teleport_sync", () -> {
                                    if (newHamster.isAlive() && !newHamster.isRemoved()) {
                                        newHamster.requestTeleport(newHamster.getX(), newHamster.getY(), newHamster.getZ());
                                    }
                                });

                                // Feedback
                                if (playSound) {
                                    newWorld.playSound(
                                            null,
                                            BlockPos.ofFloored(targetPos),
                                            SoundEvents.ENTITY_FOX_TELEPORT,
                                            SoundCategory.NEUTRAL,
                                            0.20f,
                                            1.5f + (newWorld.getRandom().nextFloat() - 0.5f) * 0.5f
                                    );
                                }
                                ParticleEffectsUtil.spawnDecayingParticleCloud(
                                        newHamster,
                                        ParticleTypes.PORTAL,
                                        60,
                                        1,
                                        0.1,
                                        0.1,
                                        -0.2
                                );
                                ParticleEffectsUtil.spawnDecayingParticleCloud(
                                        newHamster,
                                        ModParticles.PIXIE_DUST.get(PixieDustParticleTheme.LAVENDER).get(),
                                        80,
                                        5,
                                        0.2,
                                        0.2,
                                        0.2
                                );
                            }
                        }));
                    }
                    this.ahp$inTransitHamsters.clear();
                }
            }
        }

        Random random = world.getRandom();
        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 2. Process Tasks & Cooldowns ---
        long currentTime = world.getTime();
        adorablehamsterpets$scheduledTasks.removeIf(task -> {
            if (currentTime >= task.executionTick()) {
                task.action().run();
                return true;
            }
            return false;
        });

        if (adorablehamsterpets$diamondSoundCooldownTicks > 0) adorablehamsterpets$diamondSoundCooldownTicks--;
        if (adorablehamsterpets$creeperSoundCooldownTicks > 0) adorablehamsterpets$creeperSoundCooldownTicks--;


        // --- 3. Feature Ticks ---
        tickGuideBookTracking();

        // Supporter Crown Trial Period Tick
        if (!world.isClient()) {
            int trialTicks = this.dataTracker.get(AHP_CROWN_TRIAL_TICKS);
            if (trialTicks > 0) {
                this.dataTracker.set(AHP_CROWN_TRIAL_TICKS, trialTicks - 1);

                // Feedback
                if (trialTicks - 1 == 0) {
                    this.ahp$setSupporterCrownTheme(-1);

                    MutableText message = Text.literal("\n")
                            .append(Text.translatable("message.adorablehamsterpets.crown_trial_ended").formatted(Formatting.GOLD))
                            .append("\n")
                            .append(Text.translatable("message.adorablehamsterpets.crown_trial_discord")
                                    .setStyle(Style.EMPTY
                                            .withColor(Formatting.AQUA)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/w54mk5bqdf"))
                                    ))
                            .append("\n");
                    self.sendMessage(message, false);
                }
            }
        }

        // Glowing Sunflower Easter Egg (Server)
        if (++this.ahp$sunflowerCheckTimer >= 20) {
            this.ahp$sunflowerCheckTimer = 0;
            if (Configs.AHP_WORLDGEN.enableGlowingSunflowers && !world.isDay()) {
                BlockPos playerPos = self.getBlockPos();
                for (BlockPos pos : BlockPos.iterate(playerPos.add(-5, -3, -5), playerPos.add(5, 3, 5))) {
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(ModBlocks.SUNFLOWER_BLOCK.get())
                            && state.get(SunflowerBlock.LIT)) {
                        ModCriteria.WITNESS_GLOWING_SUNFLOWER.trigger((ServerPlayerEntity) self);
                        break;
                    }
                }
            }
        }

        // --- Genetic Visualization (Server) ---
        if (this.ahp$geneticParent1Uuid != null && this.ahp$geneticParent2Uuid != null) {
            Entity parent1 = ((ServerWorld) world).getEntity(this.ahp$geneticParent1Uuid);
            Entity parent2 = ((ServerWorld) world).getEntity(this.ahp$geneticParent2Uuid);

            if (parent1 != null && parent2 != null && parent1.isAlive() && parent2.isAlive()) {
                int countPerTick = Configs.AHP.simulatedOffspringPerTick.get();
                ParticleEffectsUtil.spawnGeneticProbabilityCloud(world, parent1.getPos(), parent2.getPos(), countPerTick);
            }
        }

        // --- 4. Shoulder Hamster Sensing ---
        if (this.hasAnyShoulderHamster()) {
            // Diamond Detection
            if (config.enableShoulderDiamondDetection) {
                adorablehamsterpets$diamondCheckTimer++;
                if (adorablehamsterpets$diamondCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$diamondCheckTimer = 0;
                    if (isCelebrationOreNearby(self, config.shoulderDiamondDetectionRadius.get())) {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = true;
                        if (adorablehamsterpets$diamondSoundCooldownTicks == 0) {
                            world.playSound(null, self.getBlockPos(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, random),
                                    SoundCategory.NEUTRAL, 2.5f, 1.0f);
                            self.sendMessage(Text.translatable("message.adorablehamsterpets.diamond_nearby").formatted(Formatting.AQUA), true);
                            adorablehamsterpets$diamondSoundCooldownTicks = random.nextBetween(140, 200);
                            ModCriteria.HAMSTER_DIAMOND_ALERT_TRIGGERED.trigger((ServerPlayerEntity) self);
                        }
                    } else {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = false;
                    }
                }
            }

            // Creeper Detection
            if (config.enableShoulderCreeperDetection) {
                adorablehamsterpets$creeperCheckTimer++;
                if (adorablehamsterpets$creeperCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$creeperCheckTimer = 0;
                    if (creeperSeesPlayer(self, config.shoulderCreeperDetectionRadius.get())) {
                        if (adorablehamsterpets$creeperSoundCooldownTicks == 0) {
                            world.playSound(null, self.getBlockPos(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CREEPER_DETECT_SOUNDS, random),
                                    SoundCategory.NEUTRAL, 1.0f, 1.0f);
                            self.sendMessage(Text.translatable("message.adorablehamsterpets.creeper_detected").formatted(Formatting.RED), true);
                            adorablehamsterpets$creeperSoundCooldownTicks = random.nextBetween(100, 160);
                            ModCriteria.HAMSTER_CREEPER_ALERT_TRIGGERED.trigger((ServerPlayerEntity) self);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void adorablehamsterpets$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!this.getWorld().isClient()) {
            net.dawson.adorablehamsterpets.util.HamsterRenderTracker.onPlayerDisconnect(this.getUuid());
        }
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("RETURN"))
    private void adorablehamsterpets$onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        // Server side only. skipSleepTimer is false for natural wakeup.
        if (!self.getWorld().isClient && !skipSleepTimer) {
            ServerWorld serverWorld = (ServerWorld) self.getWorld();
            UUID ownerUuid = self.getUuid();

            // --- 1. Scan for Stuck Hamsters ---
            List<HamsterEntity> stuckHamsters = new ArrayList<>();
            for (Entity entity : serverWorld.getEntitiesByType(ModEntities.HAMSTER.get(), Entity::isAlive)) {
                if (entity instanceof HamsterEntity hamster) {
                    if (hamster.isTamed() && ownerUuid.equals(hamster.getOwnerUuid()) && hamster.isStuckSearchingForBed()) {
                        stuckHamsters.add(hamster);
                    }
                }
            }

            // --- 2. Rescue Protocol ---
            for (HamsterEntity hamster : stuckHamsters) {
                hamster.getLinkedBedPos().ifPresent(globalPos -> {
                    if (serverWorld.getRegistryKey() == globalPos.getDimension()) {
                        BlockPos bedPos = globalPos.getPos();
                        BlockState bedState = serverWorld.getBlockState(bedPos);

                        // Validate Bed availability
                        if (bedState.getBlock() instanceof HamsterBedBlock && !bedState.get(HamsterBedBlock.OCCUPIED)) {
                            // Teleport and force sleep
                            Vec3d targetCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);

                            // Explicitly request teleport to sync with client
                            hamster.requestTeleport(targetCenter.x, targetCenter.y, targetCenter.z);

                            hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
                            hamster.setSleeping(true);
                            hamster.setRescueSleeping(true);
                            hamster.setInSittingPose(true);

                            serverWorld.setBlockState(bedPos, bedState.with(HamsterBedBlock.OCCUPIED, true), Block.NOTIFY_ALL);

                            // Match personality pose
                            int personality = hamster.getDataTracker().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
                            int poseIndex = (personality >= 1 && personality <= 3) ? personality : 1;
                            hamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, "anim_hamster_sleep_pose" + poseIndex);
                            HamsterBedUtil.startNapTimer(hamster);

                            // Cleanup flags
                            hamster.setStuckSearchingForBed(false);
                            hamster.setWanderModeActive(true);

                            AdorableHamsterPets.LOGGER.info("Rescued stuck hamster {} to bed at {}", hamster.getId(), bedPos);
                        } else {
                            // Bed useless, stop checking
                            hamster.setStuckSearchingForBed(false);
                        }
                    }
                });
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API (PlayerEntityAccessor)
     * ────────────────────────────────────────────────────────────────────────────*/

    @Unique
    @Override
    public List<NbtCompound> ahp$getInTransitHamsters() {
        return this.ahp$inTransitHamsters;
    }

    @Unique
    @Override
    public int ahp$getTransitTimer() {
        return this.ahp$transitTimer;
    }

    @Unique
    @Override
    public void ahp$setTransitTimer(int timer) {
        this.ahp$transitTimer = timer;
    }

    @Unique
    @Override
    public int ahp$getSupporterCrownTheme() {
        return this.dataTracker.get(AHP_CROWN_THEME);
    }

    @Unique
    @Override
    public void ahp$setSupporterCrownTheme(int theme) {
        this.dataTracker.set(AHP_CROWN_THEME, theme);
    }

    @Unique
    @Override
    public int ahp$getSupporterCrownAudioTimer() {
        return this.ahp$crownAudioTimer;
    }

    @Unique
    @Override
    public void ahp$setSupporterCrownAudioTimer(int timer) {
        this.ahp$crownAudioTimer = timer;
    }

    @Unique
    @Override
    public boolean ahp$hasUsedSupporterCrownTrial() {
        return this.dataTracker.get(AHP_HAS_USED_CROWN_TRIAL);
    }

    @Unique
    @Override
    public void ahp$setHasUsedSupporterCrownTrial(boolean used) {
        this.dataTracker.set(AHP_HAS_USED_CROWN_TRIAL, used);
    }

    @Unique
    @Override
    public int ahp$getSupporterCrownTrialTicks() {
        return this.dataTracker.get(AHP_CROWN_TRIAL_TICKS);
    }

    @Unique
    @Override
    public void ahp$setSupporterCrownTrialTicks(int ticks) {
        this.dataTracker.set(AHP_CROWN_TRIAL_TICKS, ticks);
    }

    @Unique
    @Override
    public UUID ahp$getGeneticParent1Uuid() {
        return this.ahp$geneticParent1Uuid;
    }

    @Unique
    @Override
    public void ahp$setGeneticParent1Uuid(UUID uuid) {
        this.ahp$geneticParent1Uuid = uuid;
    }

    @Unique
    @Override
    public UUID ahp$getGeneticParent2Uuid() {
        return this.ahp$geneticParent2Uuid;
    }

    @Unique
    @Override
    public void ahp$setGeneticParent2Uuid(UUID uuid) {
        this.ahp$geneticParent2Uuid = uuid;
    }

    @Unique
    @Override
    public boolean ahp$addTamedGenome(int hash) {
        return this.ahp$tamedGenomes.add(hash);
    }

    @Unique
    @Override
    public boolean ahp$addBredGenome(int hash) {
        return this.ahp$bredGenomes.add(hash);
    }

    @Unique
    @Override
    public int ahp$getTamedGenomeCount() {
        return this.ahp$tamedGenomes.size();
    }

    @Unique
    @Override
    public int ahp$getBredGenomeCount() {
        return this.ahp$bredGenomes.size();
    }

    @Unique
    @Override
    public boolean ahp$canBreedHamsters() {
        if (!Configs.AHP.playerBreedingLimit.get()) {
            return true;
        }

        PlayerEntity self = (PlayerEntity) (Object) this;

        // Whitelist check
        if (Configs.AHP.allowedBreeders.contains(self.getGameProfile().getName())) {
            return true;
        }

        LitterLimitType type = Configs.AHP.playerBreedingLimitType.get();
        if (type == LitterLimitType.DAILY) {
            long currentTime = this.getWorld().getTime();
            long dayDuration = Configs.AHP.useIrlTimeForBreedingLimit.get() ? 1728000L : 24000L;
            long currentDay = currentTime / dayDuration;
            long lastDay = this.ahp$lastBreedingTime / dayDuration;

            if (currentDay > lastDay) {
                this.ahp$hamstersFedForBreeding = 0;
                this.ahp$lastBreedingTime = currentTime;
            }
        }

        // Limit is in litters, so we multiply by 2 to get the number of individual hamsters they can feed
        int maxHamsters = Configs.AHP.maxLittersPerPlayer.get() * 2;
        return this.ahp$hamstersFedForBreeding < maxHamsters;
    }

    @Unique
    @Override
    public void ahp$incrementHamstersFedForBreeding() {
        this.ahp$hamstersFedForBreeding++;
        this.ahp$lastBreedingTime = this.getWorld().getTime();
    }

    @Unique
    @Override
    public void ahp$resetBreedingHistory() {
        this.ahp$hamstersFedForBreeding = 0;
        ((PlayerEntity)(Object)this).sendMessage(Text.translatable("message.adorablehamsterpets.breeding.history_reset").formatted(Formatting.GREEN), true);
    }

    @Unique
    @Override
    public void adorablehamsterpets$startPrecisionTreeHeist(BlockPos leafPos) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) return;

        // --- 1. Queue Validation & Rebuild ---
        if (this.adorablehamsterpets$mountOrderQueue.isEmpty() && this.hasAnyShoulderHamster()) {
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!this.getShoulderHamster(location).isEmpty()) {
                    this.adorablehamsterpets$mountOrderQueue.addLast(location);
                }
            }
        }

        if (this.adorablehamsterpets$mountOrderQueue.isEmpty()) return;

        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // Peek next hamster
        ShoulderLocation locationToProcess = config.dismountOrder.get() == DismountOrder.LIFO
                ? this.adorablehamsterpets$mountOrderQueue.peekLast()
                : this.adorablehamsterpets$mountOrderQueue.peekFirst();

        if (locationToProcess == null) return;

        NbtCompound shoulderNbt = this.getShoulderHamster(locationToProcess);
        if (shoulderNbt.isEmpty()) {
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // Validate entity creation before altering state
        HamsterEntity hamster = HamsterNbtUtil.createFromNbt((ServerWorld) world, self, shoulderNbt);
        if (hamster == null) {
            this.setShoulderHamster(locationToProcess, new NbtCompound());
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // --- 2. Tree Heist Trigger ---
        TreeHeistUtil.TreeScanResult scanResult = TreeHeistUtil.scanForTree(world, leafPos);

        if (HamsterTreeSearcherEntity.isTreeBlocked(world, scanResult.treeId())) {
            self.sendMessage(Text.translatable("message.adorablehamsterpets.tree_heist_occupied").formatted(Formatting.RED), true);
        } else {
            // Start Heist
            HamsterTreeSearcherEntity searcher = ModEntities.HAMSTER_TREE_SEARCHER.get().create(world);
            if (searcher != null) {
                hamster.triggerLeafPopEffects(leafPos, false);
                NbtCompound fullNbt = new NbtCompound();
                hamster.writeNbt(fullNbt);

                searcher.initializeSearch(leafPos, scanResult, fullNbt);
                searcher.setForcedExitPos(leafPos); // Apply Precision Exit

                world.spawnEntity(searcher);

                // Clear Data
                if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
                else this.adorablehamsterpets$mountOrderQueue.pollFirst();
                this.setShoulderHamster(locationToProcess, new NbtCompound());

                // Feedback
                world.playSound(null, self.getBlockPos(), ModSounds.HAMSTER_DISMOUNT.get(), SoundCategory.PLAYERS, 0.7f, 1.0f + world.getRandom().nextFloat() * 0.2f);
                self.sendMessage(Text.translatable("message.adorablehamsterpets.precision_tree_heist_started").formatted(Formatting.GREEN), true);            }
        }
    }

    @Unique
    @Override
    public boolean ahp$canPlayTagGame() {
        // --- 1. Check Config Toggle ---
        if (!Configs.AHP.enableTagGamePlayerLimit.get()) {
            return true;
        }

        // --- 2. Check Daily Limit ---
        World world = ((PlayerEntity) (Object) this).getWorld();
        long currentTime = world.getTime();

        // Calculate days passed (24000 ticks per day)
        long currentDay = currentTime / 24000L;
        long lastPlayedDay = this.ahp$lastTagGameDayTime / 24000L;

        // If new day, reset counter
        if (currentDay > lastPlayedDay) {
            this.ahp$tagGamesPlayedToday = 0;
            this.ahp$lastTagGameDayTime = currentTime;
        }

        return this.ahp$tagGamesPlayedToday < Configs.AHP.maxDailyTagGamesPerPlayer.get();
    }

    @Unique
    @Override
    public void ahp$incrementTagGameCount() {
        this.ahp$tagGamesPlayedToday++;
        this.ahp$lastTagGameDayTime = ((PlayerEntity) (Object) this).getWorld().getTime();
    }

    @Unique
    @Override
    public boolean ahp$computeHasGuideBook(PlayerEntity player) {
        // --- 1. Check Cursor Stack ---
        if (player.currentScreenHandler != null) {
            ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
            if (!cursorStack.isEmpty() && cursorStack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
                return true;
            }
        }

        // --- 2. Check Standard Inventory ---
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {

                // --- Eccentric Tomes Compat ---
                // Don't trigger effects when morphing the Eccentric Tomes book into my guidebook
                if (stack.hasCustomName()) {
                    Text customName = stack.getName();
                    if (customName != null && customName.getString().contains("Eccentric Tome")) {
                        continue;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Unique
    @Override
    public void ahp$initGuideBookTracking(boolean currentlyHasGuideBook) {
        this.ahp$cachedHasGuideBook = currentlyHasGuideBook;
        this.ahp$guideBookTrackingInitialized = true;
        this.ahp$guideBookCheckTimer = 0;
    }

    @Unique
    @Override
    public NbtCompound getShoulderHamster(ShoulderLocation location) {
        return this.ahp$hamsterState.getCompound(location.name());
    }

    @Unique
    @Override
    public void setShoulderHamster(ShoulderLocation location, NbtCompound nbt) {
        // Update local
        if (nbt == null || nbt.isEmpty()) {
            this.ahp$hamsterState.remove(location.name());
        } else {
            this.ahp$hamsterState.put(location.name(), nbt);
        }

        // Sync logic
        if (!this.getWorld().isClient()) {
            ModPackets.SyncHamsterStateS2CPacket packet = new ModPackets.SyncHamsterStateS2CPacket(this.getId(), this.ahp$hamsterState);
            PlayerEntity self = (PlayerEntity) (Object) this;

            // Send to self
            if (self instanceof ServerPlayerEntity serverSelf) {
                ModPackets.CHANNEL.sendToPlayer(serverSelf, packet);
            }

            // Send to watchers
            // Manual loop on 1.20.1
            if (self.getWorld() instanceof ServerWorld serverWorld) {
                for (ServerPlayerEntity otherPlayer : serverWorld.getPlayers()) {
                    if (otherPlayer != self && otherPlayer.squaredDistanceTo(self) < 6400) {
                        ModPackets.CHANNEL.sendToPlayer(otherPlayer, packet);
                    }
                }
            }
        }
    }

    @Unique
    @Override
    public void adorablehamsterpets$setRawHamsterState(NbtCompound nbt) {
        this.ahp$hamsterState = nbt;
    }

    @Unique
    @Override
    public void adorablehamsterpets$syncHamsterState() {
        if (!this.getWorld().isClient() && !this.ahp$hamsterState.isEmpty()) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            if (self instanceof ServerPlayerEntity serverPlayer) {
                // On 1.20.1, use ModPackets.CHANNEL and the inner record class
                var packet = new ModPackets.SyncHamsterStateS2CPacket(this.getId(), this.ahp$hamsterState);
                ModPackets.CHANNEL.sendToPlayer(serverPlayer, packet);
            }
        }
    }

    @Unique
    @Override
    public void adorablehamsterpets$dismountShoulderHamster(boolean isThrow) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) return;

        // --- 1. Queue Validation & Rebuild ---
        if (this.adorablehamsterpets$mountOrderQueue.isEmpty() && this.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.warn("[HamsterDismount] Player {} has shoulder hamsters but empty queue. Rebuilding...", self.getName().getString());
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!this.getShoulderHamster(location).isEmpty()) {
                    this.adorablehamsterpets$mountOrderQueue.addLast(location);
                }
            }
        }

        if (this.adorablehamsterpets$mountOrderQueue.isEmpty()) return;

        final AhpConfig config = AdorableHamsterPets.CONFIG;
        Random random = world.getRandom();

        // Peek next hamster
        ShoulderLocation locationToProcess = config.dismountOrder.get() == DismountOrder.LIFO
                ? this.adorablehamsterpets$mountOrderQueue.peekLast()
                : this.adorablehamsterpets$mountOrderQueue.peekFirst();

        if (locationToProcess == null) return;

        NbtCompound shoulderNbt = this.getShoulderHamster(locationToProcess);
        if (shoulderNbt.isEmpty()) {
            AdorableHamsterPets.LOGGER.warn("Dismount queue pointed to an empty slot ({}). Desync probable.", locationToProcess);
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // Validate entity creation before altering state
        HamsterEntity hamster = HamsterNbtUtil.createFromNbt((ServerWorld) world, self, shoulderNbt);
        if (hamster == null) {
            this.setShoulderHamster(locationToProcess, new NbtCompound());
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // --- 2. Tree Heist Trigger ---
        // If looking at oak leaves, check for heist start
        HitResult hitResult = self.raycast(5.0, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult) hitResult).getBlockPos();
            if (world.getBlockState(hitPos).isOf(Blocks.OAK_LEAVES)) {

                TreeHeistUtil.TreeScanResult scanResult = TreeHeistUtil.scanForTree(world, hitPos);

                if (HamsterTreeSearcherEntity.isTreeBlocked(world, scanResult.treeId())) {
                    self.sendMessage(Text.translatable("message.adorablehamsterpets.tree_heist_occupied").formatted(Formatting.RED), true);
                    return; // Abort
                } else {
                    // Start Heist
                    HamsterTreeSearcherEntity searcher = ModEntities.HAMSTER_TREE_SEARCHER.get().create(world);
                    if (searcher != null) {
                        hamster.triggerLeafPopEffects(hitPos, false);
                        NbtCompound fullNbt = new NbtCompound();
                        hamster.writeNbt(fullNbt);

                        searcher.initializeSearch(hitPos, scanResult, fullNbt);
                        world.spawnEntity(searcher);

                        // Clear Data
                        if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
                        else this.adorablehamsterpets$mountOrderQueue.pollFirst();
                        this.setShoulderHamster(locationToProcess, new NbtCompound());

                        return; // Bypass standard spawn
                    }
                }
            }
        }

        // --- 3. Throw Logic ---
        if (isThrow) {
            if (hamster.isBaby()) {
                self.sendMessage(Text.translatable("message.adorablehamsterpets.baby_throw_refusal").formatted(Formatting.RED), true);
                return;
            }

            long currentTime = world.getTime();
            if (hamster.throwCooldownEndTick > currentTime) {
                long remainingTicks = hamster.throwCooldownEndTick - currentTime;
                long totalSecondsRemaining = Math.max(1, remainingTicks / 20);
                self.sendMessage(Text.translatable("message.adorablehamsterpets.throw_cooldown", totalSecondsRemaining).formatted(Formatting.RED), true);
                return;
            }

            // Calculate Yeet Speed
            boolean isBuffed = hamster.hasGreenBeanBuff();
            float throwSpeed = isBuffed ? config.hamsterThrowVelocityBuffed.get().floatValue() : config.hamsterThrowVelocity.get().floatValue();

            ItemStack armorStack = hamster.getArmorStack();
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
                if (config.enableArmorPerks.get() && armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.IRON) {
                    throwSpeed += config.ironArmorThrowSpeedBoost.get().floatValue();
                }
            }

            // Update Hamster timers before packaging into projectile NBT
            hamster.throwCooldownEndTick = currentTime + config.hamsterThrowCooldown.get();
            NbtCompound updatedShoulderNbt = HamsterNbtUtil.saveToHamsterState(hamster).toNbt();

            // Create Projectile
            HamsterProjectileEntity projectile = new HamsterProjectileEntity(world, self);
            projectile.refreshPositionAndAngles(self.getX(), self.getEyeY() - 0.1, self.getZ(), self.getYaw(), self.getPitch());
            projectile.setHamsterData(updatedShoulderNbt); // Pass NBT into projectile

            Vec3d lookVec = self.getRotationVec(1.0f);
            Vec3d throwVec = new Vec3d(lookVec.x, lookVec.y + 0.1f, lookVec.z).normalize();
            projectile.setVelocity(throwVec.multiply(throwSpeed));

            // Clean up original NBT slot
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            this.setShoulderHamster(locationToProcess, new NbtCompound());

            world.spawnEntity(projectile);

            world.playSound(null, self.getX(), self.getY(), self.getZ(), ModSounds.HAMSTER_THROW.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
            // Delayed celebration squeak
            this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(world.getTime() + 3, () -> {
                SoundEvent celebrationSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_FLYING_SOUNDS, random);
                if (celebrationSound != null) {
                    world.playSound(null, self.getX(), self.getY(), self.getZ(), celebrationSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }));
            ModCriteria.HAMSTER_THROWN.trigger((ServerPlayerEntity) self);
            this.adorablehamsterpets$isDiamondAlertConditionMet = false;
            return;
        }

        // --- 4. Finalize Dismount ---
        if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
        else this.adorablehamsterpets$mountOrderQueue.pollFirst();

        this.setShoulderHamster(locationToProcess, new NbtCompound());

        HamsterEntity.spawnFromNbt((ServerWorld) world, self, shoulderNbt, this.adorablehamsterpets$isDiamondAlertConditionMet);
        this.adorablehamsterpets$isDiamondAlertConditionMet = false;

        world.playSound(null, self.getBlockPos(), ModSounds.HAMSTER_DISMOUNT.get(), SoundCategory.PLAYERS, 0.7f, 1.0f + random.nextFloat() * 0.2f);
        if (config.enableShoulderDismountMessages && !DISMOUNT_MESSAGE_KEYS.isEmpty()) {
            String chosenKey;
            if (DISMOUNT_MESSAGE_KEYS.size() == 1) {
                chosenKey = DISMOUNT_MESSAGE_KEYS.get(0);
            } else {
                List<String> availableKeys = new ArrayList<>(DISMOUNT_MESSAGE_KEYS);
                availableKeys.remove(this.adorablehamsterpets$lastDismountMessageKey);
                chosenKey = availableKeys.isEmpty() ? this.adorablehamsterpets$lastDismountMessageKey : availableKeys.get(random.nextInt(availableKeys.size()));
            }
            self.sendMessage(Text.translatable(chosenKey), true);
            this.adorablehamsterpets$lastDismountMessageKey = chosenKey;
        }
    }

    @Unique
    @Override
    public int ahp$getLastRandomMessageIndex(String context) {
        return this.ahp$randomMessageIndices.getOrDefault(context, -1);
    }

    @Unique
    @Override
    public void ahp$setLastRandomMessageIndex(String context, int index) {
        this.ahp$randomMessageIndices.put(context, index);
    }

    @Unique
    @Override
    public boolean hasAnyShoulderHamster() {
        return !getShoulderHamster(ShoulderLocation.RIGHT_SHOULDER).isEmpty() ||
                !getShoulderHamster(ShoulderLocation.LEFT_SHOULDER).isEmpty() ||
                !getShoulderHamster(ShoulderLocation.HEAD).isEmpty();
    }

    @Unique
    @Override
    public ArrayDeque<ShoulderLocation> adorablehamsterpets$getMountOrderQueue() {
        return this.adorablehamsterpets$mountOrderQueue;
    }

    @Unique
    @Override
    public ClientShoulderHamsterData adorablehamsterpets$getClientHamsterState() {
        // Lazy init for safety
        if (this.adorablehamsterpets$clientHamsterState == null && this.getWorld().isClient) {
            this.adorablehamsterpets$clientHamsterState = new ClientShoulderHamsterData();
        }
        return this.adorablehamsterpets$clientHamsterState;
    }

    @Unique
    @Override
    public void ahp$registerTreeHeist(BlockPos treeId) {
        long time = this.getWorld().getTime();
        this.ahp$heistHistory.add(new TreeHeistUtil.HeistRecord(treeId, time));
        this.ahp$heistHistory.removeIf(r -> time - r.timestamp() > HEIST_MEMORY_DURATION);
    }

    @Unique
    @Override
    public float ahp$getHeistProfitability(BlockPos treeId) {
        long time = this.getWorld().getTime();
        int initialSize = this.ahp$heistHistory.size();

        // Prune expired
        this.ahp$heistHistory.removeIf(r -> time - r.timestamp() > HEIST_MEMORY_DURATION);
        int prunedSize = this.ahp$heistHistory.size();

        // Calculate saturation
        int matchCount = 0;
        List<Long> matchAges = new ArrayList<>();

        for (TreeHeistUtil.HeistRecord record : this.ahp$heistHistory) {
            if (record.pos().equals(treeId)) {
                matchCount++;
                matchAges.add(time - record.timestamp());
            }
        }

        // Sliding scale
        float multiplier;
        if (matchCount == 0) multiplier = 1.0f;
        else if (matchCount == 1) multiplier = 0.6f;
        else if (matchCount == 2) multiplier = 0.3f;
        else multiplier = 0.0f;

        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("""
                [TreeHeist-Profitability] Calculating for Tree Anchor: {}
                  - Current World Time: {}
                  - Player History Size: {} (Pruned from {})
                  - Matches Found for this Tree: {}
                  - Match Ages (ticks ago): {} (Memory Limit: {})
                  - Calculated Multiplier: {}""",
                    treeId.toShortString(),
                    time,
                    prunedSize, initialSize,
                    matchCount,
                    matchAges, HEIST_MEMORY_DURATION,
                    String.format("%.2f", multiplier)
            );
        }

        return multiplier;
    }

    @Unique
    @Override
    public void ahp$clearHeistHistory() {
        this.ahp$heistHistory.clear();
        AdorableHamsterPets.LOGGER.info("[TreeHeist] Cleared heist history for player {}.", this.getName().getString());
        ((PlayerEntity)(Object)this).sendMessage(Text.translatable("message.adorablehamsterpets.tree_heist_history_reset").formatted(Formatting.WHITE), true);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);

        // Sync shoulder data to the watcher immediately
        if (!this.ahp$hamsterState.isEmpty()) {
            ModPackets.SyncHamsterStateS2CPacket packet = new ModPackets.SyncHamsterStateS2CPacket(this.getId(), this.ahp$hamsterState);
            ModPackets.CHANNEL.sendToPlayer(player, packet);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Scoops up all following hamsters when a player teleports. Grabs them from both
     * the old chunk and the new chunk, serializes them into a transit pocket, and
     * discards their bodies to prevent duplicate saving or rendering glitches.
     */
    @Unique
    private void ahp$pocketFollowingHamsters(Vec3d oldPos, RegistryKey<World> oldDimension) {
        MinecraftServer server = this.getServer();
        if (server == null) return;

        ServerWorld oldWorld = server.getWorld(oldDimension);
        ServerWorld newWorld = (ServerWorld) this.getWorld();
        if (oldWorld == null || newWorld == null) return;

        List<HamsterEntity> toRescue = new ArrayList<>();

        // Grab hamsters left behind at old location
        Box oldSearchBox = new Box(oldPos.x - 64, oldPos.y - 64, oldPos.z - 64, oldPos.x + 64, oldPos.y + 64, oldPos.z + 64);
        toRescue.addAll(oldWorld.getEntitiesByClass(HamsterEntity.class, oldSearchBox, this::ahp$isValidRescueTarget));

        // Grab hamsters who might have already teleported to new location
        Box newSearchBox = new Box(this.getX() - 64, this.getY() - 64, this.getZ() - 64, this.getX() + 64, this.getY() + 64, this.getZ() + 64);
        List<HamsterEntity> newWorldHamsters = newWorld.getEntitiesByClass(HamsterEntity.class, newSearchBox, this::ahp$isValidRescueTarget);

        for (HamsterEntity hamster : newWorldHamsters) {
            if (!toRescue.contains(hamster)) {
                toRescue.add(hamster);
            }
        }

        if (toRescue.isEmpty()) return;

        // Pocket them
        for (HamsterEntity hamster : toRescue) {
            NbtCompound nbt = new NbtCompound();
            hamster.writeNbt(nbt); // Save complete state
            this.ahp$inTransitHamsters.add(nbt);
            hamster.discard(); // Remove from world
        }

        // Start transit timer
        this.ahp$transitTimer = 15; // 15 ticks to ensure client has loaded
    }

    @Unique
    private boolean ahp$isValidRescueTarget(HamsterEntity hamster) {
        if (hamster.isWanderModeActive() || hamster.isShoulderPet()) {
            return false;
        }

        // Treat babies identically if their parent is a valid rescue target
        if (hamster.isBaby() && hamster.getParentUuid() != null) {
            MinecraftServer server = hamster.getServer();
            if (server != null) {
                Entity parentEntity = null;
                for (ServerWorld w : server.getWorlds()) {
                    parentEntity = w.getEntity(hamster.getParentUuid());
                    if (parentEntity != null) break;
                }

                boolean parentRescued = false;
                if (parentEntity instanceof HamsterEntity parentHamster && parentHamster.isAlive()) {
                    if (parentHamster.isTamed() && this.getUuid().equals(parentHamster.getOwnerUuid())
                            && !parentHamster.isSitting()
                            && !parentHamster.isWanderModeActive()
                            && !parentHamster.isShoulderPet()) {
                        parentRescued = true;
                    }
                } else {
                    // Parent not active in world. Check if currently mounted to shoulder
                    for (ShoulderLocation loc : ShoulderLocation.values()) {
                        NbtCompound nbt = this.getShoulderHamster(loc);
                        if (!nbt.isEmpty()) {
                            Optional<HamsterState> state = HamsterState.fromNbt(nbt);
                            if (state.isPresent() && state.get().entityUuid().equals(hamster.getParentUuid())) {
                                parentRescued = true;
                                break;
                            }
                        }
                    }
                    // Check if parent is currently in transit
                    if (!parentRescued) {
                        for (NbtCompound nbt : this.ahp$inTransitHamsters) {
                            if (nbt.containsUuid("UUID") && nbt.getUuid("UUID").equals(hamster.getParentUuid())) {
                                parentRescued = true;
                                break;
                            }
                        }
                    }
                }

                if (parentRescued) {
                    return true;
                }
            }
        }

        if (hamster.isSitting()) {
            return false;
        }

        // Tamed and owned by player
        if (hamster.isTamed() && this.getUuid().equals(hamster.getOwnerUuid())) {
            return true;
        }

        return false;
    }

    /**
     * Detects when player gets the Hamster Tips guidebook. Plays FX once
     */
    @Unique
    private void tickGuideBookTracking() {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.getWorld().isClient || !(self instanceof ServerPlayerEntity player)) return;

        // Once per second
        if (++this.ahp$guideBookCheckTimer < AHP_GUIDEBOOK_CHECK_INTERVAL_TICKS) return;
        this.ahp$guideBookCheckTimer = 0;

        boolean hasNow = this.ahp$computeHasGuideBook(player);

        // Init guard
        if (!this.ahp$guideBookTrackingInitialized) {
            this.ahp$initGuideBookTracking(hasNow);
            this.ahp$guideBookCheckGracePeriodTimer = 0;
            return;
        }

        if (hasNow) {
            // Book is present. Reset grace timer
            this.ahp$guideBookCheckGracePeriodTimer = 0;

            // Edge: No -> Yes
            if (!this.ahp$cachedHasGuideBook) {
                ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayGuidebookEffectsS2CPacket(false));
                this.ahp$cachedHasGuideBook = true;
            }
        } else {
            // Book is missing
            if (this.ahp$cachedHasGuideBook) {
                // Start/continue grace period
                this.ahp$guideBookCheckGracePeriodTimer++;

                // 30 seconds = 30 checks (runs once per second)
                if (this.ahp$guideBookCheckGracePeriodTimer >= 30) {
                    this.ahp$cachedHasGuideBook = false;
                }
            } else {
                // Consider the guidebook officially lost
                if (Configs.AHP.enableAutoGuidebookDeliveryFallback) {
                    if (ahp$tryFallbackDelivery(player)) {
                        this.ahp$cachedHasGuideBook = true;
                        this.ahp$guideBookCheckGracePeriodTimer = 0;
                    }
                }
            }
        }
    }

    /**
     * Executes a visual scan for a hamster to trigger fallback guidebook delivery.
     */
    @Unique
    private boolean ahp$tryFallbackDelivery(ServerPlayerEntity player) {
        PlayerAdvancementTracker advancementTracker = player.getAdvancementTracker();
        Identifier flagAdvId = Identifier.of(AdorableHamsterPets.MOD_ID, "technical/has_received_initial_guidebook");
        Advancement flagAdvancement = player.server.getAdvancementLoader().get(flagAdvId);

        // Abort if they've already received the initial delivery at some point
        if (flagAdvancement == null || advancementTracker.getProgress(flagAdvancement).isDone()) {
            return false;
        }

        double searchRadius = 10.0;
        Box searchBox = player.getBoundingBox().expand(searchRadius);
        List<HamsterEntity> nearbyHamsters = player.getWorld().getEntitiesByClass(
                HamsterEntity.class,
                searchBox,
                EntityPredicates.VALID_ENTITY
        );

        for (HamsterEntity hamster : nearbyHamsters) {
            // Check if looking at the hamster with a 1-block padding to make it forgiving
            if (EntityTargetingUtil.isLookingAt(player, hamster, searchRadius, 1.0)) {

                // Deliver guidebook: (grant advancement, send fallback message, play effects, don't close screen)
                AdorableHamsterPets.deliverGuidebook(player, true, true, true, false);

                AdorableHamsterPets.LOGGER.info("Delivered 'Hamster Tips' guidebook to player {} via visual fallback.", player.getName().getString());
                return true;
            }
        }

        return false;
    }

    // Check for yummy rocks. Prioritize exposed ones.
    @Unique
    private boolean isCelebrationOreNearby(PlayerEntity player, double radius) {
        World world = player.getWorld();
        BlockPos center = player.getBlockPos();
        int intRadius = (int) Math.ceil(radius);

        List<BlockPos> exposedOres = new ArrayList<>();
        List<BlockPos> buriedOres = new ArrayList<>();

        for (BlockPos checkPos : BlockPos.iterate(center.add(-intRadius, -intRadius, -intRadius), center.add(intRadius, intRadius, intRadius))) {
            if (checkPos.getSquaredDistance(center) <= radius * radius) {
                BlockState state = world.getBlockState(checkPos);

                if (ConfigDataCache.isCelebrationOre(state)) {
                    if (HamsterSniffForOreGoal.isOreExposed(checkPos, world)) {
                        exposedOres.add(checkPos.toImmutable());
                    } else {
                        buriedOres.add(checkPos.toImmutable());
                    }
                }
            }
        }
        return !exposedOres.isEmpty() || !buriedOres.isEmpty();
    }

    // Is green explody thing looking at player?
    @Unique
    private boolean creeperSeesPlayer(PlayerEntity player, double radius) {
        World world = player.getWorld();
        Box searchBox = new Box(player.getPos().subtract(radius, radius, radius), player.getPos().add(radius, radius, radius));
        List<CreeperEntity> nearbyCreepers = world.getEntitiesByClass(
                CreeperEntity.class,
                searchBox,
                creeper -> creeper.isAlive() && creeper.getTarget() == player && EntityPredicates.VALID_ENTITY.test(creeper)
        );
        return !nearbyCreepers.isEmpty();
    }
}