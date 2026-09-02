package net.dawson.adorablehamsterpets.mixin.server;

import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.UUIDUtil;
import com.mojang.authlib.GameProfile;
import dev.architectury.networking.NetworkManager;
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
import net.dawson.adorablehamsterpets.effect.FeatherYeetingStatusEffect;
import net.dawson.adorablehamsterpets.effect.ModStatusEffects;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.networking.payload.PlayGuidebookEffectsPayload;
import net.dawson.adorablehamsterpets.networking.payload.SyncHamsterStatePayload;
import net.dawson.adorablehamsterpets.networking.payload.SyncPettingStatePayload;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(Player.class)
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
    @Unique private CompoundTag ahp$hamsterState = new CompoundTag();
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
    @Unique private Vec3 ahp$lastTickPos = null;
    @Unique private ResourceKey<Level> ahp$lastTickDimension = null;
    @Unique private final List<CompoundTag> ahp$inTransitHamsters = new ArrayList<>();
    @Unique private int ahp$transitTimer = 0;

    // --- Petting Tracking ---
    @Unique private CompoundTag ahp$pettingHamster = new CompoundTag();
    @Unique private int ahp$pettingTimer = 0;

    // --- Gesture Tracking ---
    @Unique private boolean ahp$wasSneaking = false;
    @Unique private int ahp$sneakToggleCount = 0;
    @Unique private int ahp$sneakToggleTimer = 0;

    // --- State Flags & Trackers ---
    @Unique private int ahp$shoulderSyncTimer = 0;
    @Unique private final Map<String, Integer> ahp$randomMessageIndices = new HashMap<>();
    @Unique private String adorablehamsterpets$lastDismountMessageKey = "";
    @Unique private boolean adorablehamsterpets$isDiamondAlertConditionMet = false;
    @Unique private int adorablehamsterpets$lastGoldMessageIndex = -1;
    @Unique private boolean ahp$cachedHasGuideBook = false;
    @Unique private boolean ahp$guideBookTrackingInitialized = false;
    @Unique private static final EntityDataAccessor<Integer> AHP_CROWN_THEME = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Boolean> AHP_HAS_USED_CROWN_TRIAL = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Integer> AHP_CROWN_TRIAL_TICKS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    @Unique private int ahp$crownAudioTimer = 0;

    // --- Collections ---
    @Unique private final List<ScheduledTask> adorablehamsterpets$scheduledTasks = new ArrayList<>();
    @Unique private final List<TreeHeistUtil.HeistRecord> ahp$heistHistory = new ArrayList<>();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors & Initialization
     * ────────────────────────────────────────────────────────────────────────────*/

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void adorablehamsterpets$onInit(Level world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        // Client-side visual setup
        if (world.isClientSide()) {
            this.adorablehamsterpets$clientHamsterState = new ClientShoulderHamsterData();
        }
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void adorablehamsterpets$initCrownData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(AHP_CROWN_THEME, -1); // Default to disabled
        builder.define(AHP_HAS_USED_CROWN_TRIAL, false);
        builder.define(AHP_CROWN_TRIAL_TICKS, 0);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks (Mixin Injections)
     * ────────────────────────────────────────────────────────────────────────────*/

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void adorablehamsterpets$writeNbt(CompoundTag nbt, CallbackInfo ci) {
        // --- Action Bar Randomized Message History ---
        if (!this.ahp$randomMessageIndices.isEmpty()) {
            CompoundTag msgNbt = new CompoundTag();
            this.ahp$randomMessageIndices.forEach(msgNbt::putInt);
            nbt.put("AHPRandomMessageIndices", msgNbt);
        }

        // --- Shoulder Data ---
        if (!this.ahp$hamsterState.isEmpty()) {
            nbt.put("ShoulderHamsters", this.ahp$hamsterState);
        }

        // --- Mount Queue ---
        if (!this.adorablehamsterpets$mountOrderQueue.isEmpty()) {
            ListTag mountOrderList = new ListTag();
            for (ShoulderLocation location : this.adorablehamsterpets$mountOrderQueue) {
                mountOrderList.add(StringTag.valueOf(location.name()));
            }
            nbt.put("MountOrderQueue", mountOrderList);
        }

        // --- Tree Heist ---
        if (!this.ahp$heistHistory.isEmpty()) {
            ListTag historyList = new ListTag();
            long currentTime = this.level().getGameTime();

            for (TreeHeistUtil.HeistRecord record : this.ahp$heistHistory) {
                if (currentTime - record.timestamp() < HEIST_MEMORY_DURATION) {
                    CompoundTag tag = new CompoundTag();
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
        if (this.ahp$geneticParent1Uuid != null) nbt.store("AHPGeneticParent1", UUIDUtil.CODEC, this.ahp$geneticParent1Uuid);
        if (this.ahp$geneticParent2Uuid != null) nbt.store("AHPGeneticParent2", UUIDUtil.CODEC, this.ahp$geneticParent2Uuid);

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
        nbt.putBoolean("AHPHasUsedCrownTrial", this.entityData.get(AHP_HAS_USED_CROWN_TRIAL));

        // --- Teleport Rescue  ---
        if (!this.ahp$inTransitHamsters.isEmpty()) {
            ListTag transitList = new ListTag();
            for (CompoundTag transitNbt : this.ahp$inTransitHamsters) {
                transitList.add(transitNbt);
            }
            nbt.put("AHPInTransitHamsters", transitList);
            nbt.putInt("AHPTransitTimer", this.ahp$transitTimer);
        }

        // --- Petting Persistence ---
        if (!this.ahp$pettingHamster.isEmpty()) {
            nbt.put("AHPPettingHamster", this.ahp$pettingHamster);
            nbt.putInt("AHPPettingTimer", this.ahp$pettingTimer);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void adorablehamsterpets$readNbt(CompoundTag nbt, CallbackInfo ci) {
        // --- Generic Message History ---
        this.ahp$randomMessageIndices.clear();
        if (nbt.contains("AHPRandomMessageIndices")) {
            CompoundTag msgNbt = nbt.getCompoundOrEmpty("AHPRandomMessageIndices");
            for (String key : msgNbt.keySet()) {
                this.ahp$randomMessageIndices.put(key, msgNbt.getIntOr(key, 0));
            }
        }

        // --- Migrate Legacy Data ---
        if (nbt.contains("ShoulderHamster")) {
            CompoundTag oldHamsterNbt = nbt.getCompoundOrEmpty("ShoulderHamster");
            if (!oldHamsterNbt.isEmpty()) {
                CompoundTag newShoulderPetsNbt = new CompoundTag();
                newShoulderPetsNbt.put(ShoulderLocation.RIGHT_SHOULDER.name(), oldHamsterNbt);
                this.ahp$hamsterState = newShoulderPetsNbt;
                this.adorablehamsterpets$mountOrderQueue.clear();
                this.adorablehamsterpets$mountOrderQueue.add(ShoulderLocation.RIGHT_SHOULDER);
                nbt.remove("ShoulderHamster"); // remove old tag to complete migration
                AdorableHamsterPets.LOGGER.info("Migrated legacy shoulder hamster data for player {}.", this.getDisplayName().getString());
            }
        } else if (nbt.contains("ShoulderHamsters")) {
            // standard Read
            this.ahp$hamsterState = nbt.getCompoundOrEmpty("ShoulderHamsters");
        }

        // --- Queue Sanitization ---
        this.adorablehamsterpets$mountOrderQueue.clear();
        if (nbt.contains("MountOrderQueue")) {
            ListTag mountOrderList = nbt.getListOrEmpty("MountOrderQueue");
            Set<ShoulderLocation> seenLocations = new HashSet<>();

            for (Tag element : mountOrderList) {
                try {
                    ShoulderLocation location = ShoulderLocation.valueOf(element.asString().orElse(""));
                    // Deduplicate and ensure data actually exists for this slot
                    if (!seenLocations.contains(location) && !this.getShoulderHamster(location).isEmpty()) {
                        this.adorablehamsterpets$mountOrderQueue.add(location);
                        seenLocations.add(location);
                    }
                } catch (IllegalArgumentException e) {
                    AdorableHamsterPets.LOGGER.warn("Found invalid ShoulderLocation name in NBT: {}", element.asString().orElse(""));
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
        if (nbt.contains("AHPTamedGenomes")) {
            for (int hash : nbt.getIntArray("AHPTamedGenomes").orElse(new int[0])) {
                this.ahp$tamedGenomes.add(hash);
            }
        }
        this.ahp$bredGenomes.clear();
        if (nbt.contains("AHPBredGenomes")) {
            for (int hash : nbt.getIntArray("AHPBredGenomes").orElse(new int[0])) {
                this.ahp$bredGenomes.add(hash);
            }
        }
        if (nbt.read("AHPGeneticParent1", UUIDUtil.CODEC).isPresent()) this.ahp$geneticParent1Uuid = nbt.read("AHPGeneticParent1", UUIDUtil.CODEC).orElse(null);
        else this.ahp$geneticParent1Uuid = null;
        if (nbt.read("AHPGeneticParent2", UUIDUtil.CODEC).isPresent()) this.ahp$geneticParent2Uuid = nbt.read("AHPGeneticParent2", UUIDUtil.CODEC).orElse(null);
        else this.ahp$geneticParent2Uuid = null;

        // --- Tag Game ---
        this.ahp$tagGamesPlayedToday = nbt.getIntOr("AHPTagGamesPlayed", 0);
        this.ahp$lastTagGameDayTime = nbt.getLongOr("AHPLastTagTime", 0L);

        // --- Player Breeding Limit ---
        this.ahp$hamstersFedForBreeding = nbt.getIntOr("AHPHamstersFedForBreeding", 0);
        this.ahp$lastBreedingTime = nbt.getLongOr("AHPLastBreedingTime", 0L);

        // --- Guidebook ---
        if (nbt.contains(AHP_NBT_GUIDEBOOK_HAS_KEY)) {
            this.ahp$cachedHasGuideBook = nbt.getBooleanOr(AHP_NBT_GUIDEBOOK_HAS_KEY, false);
        }
        if (nbt.contains(AHP_NBT_GUIDEBOOK_INIT_KEY)) {
            this.ahp$guideBookTrackingInitialized = nbt.getBooleanOr(AHP_NBT_GUIDEBOOK_INIT_KEY, false);
        }

        // --- Supporter Crown Trial ---
        if (nbt.contains("AHPHasUsedCrownTrial")) {
            boolean hasUsed = nbt.getBooleanOr("AHPHasUsedCrownTrial", false);
            // Wipe slate clean if in dev environment
            if (Platform.isDevelopmentEnvironment()) {
                hasUsed = false;
            }
            this.entityData.set(AHP_HAS_USED_CROWN_TRIAL, hasUsed);
        }

        // --- Teleport Rescue ---
        this.ahp$inTransitHamsters.clear();
        if (nbt.contains("AHPInTransitHamsters")) {
            ListTag transitList = nbt.getListOrEmpty("AHPInTransitHamsters");
            for (int i = 0; i < transitList.size(); i++) {
                this.ahp$inTransitHamsters.add(transitList.getCompoundOrEmpty(i));
            }
            this.ahp$transitTimer = nbt.getIntOr("AHPTransitTimer", 0);
        }

        // --- Petting Persistence ---
        this.ahp$pettingHamster = new CompoundTag();
        this.ahp$pettingTimer = 0;
        if (nbt.contains("AHPPettingHamster")) {
            this.ahp$pettingHamster = nbt.getCompoundOrEmpty("AHPPettingHamster");
            this.ahp$pettingTimer = nbt.getIntOr("AHPPettingTimer", 0);

            // If logging back in with a hamster in pocket, ensure timer is at least 15 ticks
            // so it spawns safely after the client finishes loading the world
            if (this.ahp$pettingTimer <= 0) {
                this.ahp$pettingTimer = 15;
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ahp$checkTeleport(CallbackInfo ci) {
        Player self = (Player) (Object) this;

        // Only run on the server for living players
        if (self.level().isClientSide() || !self.isAlive()) return;

        Vec3 currentPos = self.position();
        if (this.ahp$lastTickPos != null && this.ahp$lastTickDimension != null) {
            boolean dimensionChanged = this.ahp$lastTickDimension != self.level().dimension();
            double distSq = this.ahp$lastTickPos.distanceToSqr(self.position());

            // If dimension changed OR moved > 20 blocks in a single tick (400 sq dist)
            if (dimensionChanged || distSq > 400.0) {
                if (Configs.AHP_MAIN.enableTeleportRescue) {
                    this.ahp$pocketFollowingHamsters(this.ahp$lastTickPos, this.ahp$lastTickDimension);
                }
            }
        }

        // Update tracking variables for the next tick
        this.ahp$lastTickPos = self.position();
        this.ahp$lastTickDimension = self.level().dimension();
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void adorablehamsterpets$onDeath(DamageSource damageSource, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() && Configs.AHP_MAIN.enableTeleportRescue) {
            this.ahp$pocketFollowingHamsters(self.position(), self.level().dimension());
        }

        // Rescue petted hamster if player dies mid-pet
        if (!this.ahp$pettingHamster.isEmpty()) {
            if (Configs.AHP_MAIN.enableTeleportRescue) {
                // Hand off to transit system to drop near the player's respawn point
                this.ahp$inTransitHamsters.add(this.ahp$pettingHamster);
            } else {
                // Fallback: Drop at the death location
                HamsterEntity.spawnFromNbt((ServerLevel) self.level(), self, this.ahp$pettingHamster, false, false);
            }
            this.ahp$pettingHamster = new CompoundTag();
            this.ahp$pettingTimer = 0;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        Level world = self.level();
        if (world.isClientSide()) return;

        // --- 1. Process In-Transit Hamsters ---
        if (self.isAlive() && this.ahp$transitTimer > 0) {
            boolean isSafeToSpawn = self.onGround() || self.isInWater() || self.onClimbable() || self.isPassenger();

            if (isSafeToSpawn) {
                // Schedule incoming shimmer sound to play 15 ticks after arrival window
                if (this.ahp$transitTimer == 15 && !this.ahp$inTransitHamsters.isEmpty()) {
                    this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(world.getGameTime() + 10, () -> {
                        world.playSound(null, this.blockPosition(), ModSounds.MAGIC_SHIMMER.get(), SoundSource.NEUTRAL, 1.5f, 1.2f);
                    }));
                }

                this.ahp$transitTimer--;

                if (this.ahp$transitTimer <= 0 && !this.ahp$inTransitHamsters.isEmpty()) {
                    ServerLevel newWorld = (ServerLevel) world;

                    // Track occupied blocks
                    Set<BlockPos> occupiedPositions = new HashSet<>();
                    int soundsScheduled = 0;
                    long currentWorldTime = newWorld.getGameTime();

                    for (CompoundTag nbt : this.ahp$inTransitHamsters) {
                        // Stagger spawns over 1-5 ticks
                        int delay = newWorld.getRandom().nextIntBetweenInclusive(1, 5);

                        // Hard limit sound effect to 7 times per rescue event
                        boolean playSound = soundsScheduled < 7;
                        if (playSound) {
                            soundsScheduled++;
                        }

                        this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(currentWorldTime + delay, () -> {
                            HamsterEntity newHamster = ModEntities.HAMSTER.get().create(newWorld, EntitySpawnReason.LOAD);
                            if (newHamster != null) {
                                newHamster.load(TagValueInput.create(ProblemReporter.DISCARDING, newWorld.registryAccess(), nbt));

                                // --- Determine Target Position ---
                                // Default to player position
                                Vec3 baseTargetPos = self.position();
                                BlockPos baseTargetBlockPos = self.blockPosition();

                                // If specific target saved, override default
                                if (nbt.read("AHPTransitTargetUuid", UUIDUtil.CODEC).isPresent()) {
                                    Entity transitTarget = newWorld.getEntity(nbt.read("AHPTransitTargetUuid", UUIDUtil.CODEC).orElse(null));
                                    if (transitTarget != null) {
                                        baseTargetPos = transitTarget.position();
                                        baseTargetBlockPos = transitTarget.blockPosition();
                                    }
                                }

                                // Find unique spawn point for every hamster near target
                                Optional<BlockPos> safePos = HamsterPlacementUtil.findSafeSpawnPosition(baseTargetBlockPos, newWorld, 6, occupiedPositions, newHamster);

                                // Reserve spot
                                safePos.ifPresent(occupiedPositions::add);

                                Vec3 finalBaseTargetPos = baseTargetPos;
                                Vec3 targetPos = safePos.map(Vec3::atBottomCenterOf).orElseGet(() -> {
                                    double offsetX = (newWorld.getRandom().nextDouble() - 0.5) * 3.0;
                                    double offsetZ = (newWorld.getRandom().nextDouble() - 0.5) * 3.0;
                                    return finalBaseTargetPos.add(offsetX, 0, offsetZ);
                                });

                                // --- Sledgehammer Server/Client Sync 1 ---
                                // Drop them with downward velocity
                                newHamster.snapTo(targetPos.x, targetPos.y + 0.1, targetPos.z, newHamster.getYRot(), newHamster.getXRot());
                                newHamster.setDeltaMovement(0, -0.05, 0);
                                newHamster.needsSync = true;
                                newHamster.getNavigation().stop();
                                newHamster.setOrderedToSit(false);

                                // Prevent flight animation upon spawning
                                newHamster.setFallFlyImmunityTicks(20);

                                newWorld.addFreshEntity(newHamster);

                                // --- Sledgehammer Server/Client Sync 2 ---
                                // Force explicit delayed positional update
                                newHamster.scheduleTask(currentWorldTime + delay + 5, "sledgehammer_teleport_sync", () -> {
                                    if (newHamster.isAlive() && !newHamster.isRemoved()) {
                                        newHamster.teleportTo(newHamster.getX(), newHamster.getY(), newHamster.getZ());
                                    }
                                });

                                // Feedback
                                if (playSound) {
                                    newWorld.playSound(
                                            null,
                                            BlockPos.containing(targetPos),
                                            SoundEvents.FOX_TELEPORT,
                                            SoundSource.NEUTRAL,
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

        // --- 2. Process Petting Animation Logic ---
        if (!this.ahp$pettingHamster.isEmpty()) {
            if (this.ahp$pettingTimer > 0) {
                this.ahp$pettingTimer--;
            }

            if (this.ahp$pettingTimer <= 0 && self.isAlive()) {
                // Swish sound when player places hamster back down
                world.playSound(
                        null,
                        self.blockPosition(),
                        ModSounds.HAMSTER_SWISH.get(),
                        SoundSource.NEUTRAL,
                        0.1f,
                        1.0f + world.getRandom().nextFloat() * 0.5f
                );

                HamsterEntity.spawnFromNbt((ServerLevel) world, self, this.ahp$pettingHamster, false, false);
                this.ahp$pettingHamster = new CompoundTag();
            }
        }

        RandomSource random = world.getRandom();
        final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;

        // --- 3. Process Tasks & Cooldowns ---
        long currentTime = world.getGameTime();
        adorablehamsterpets$scheduledTasks.removeIf(task -> {
            if (currentTime >= task.executionTick()) {
                task.action().run();
                return true;
            }
            return false;
        });

        if (adorablehamsterpets$diamondSoundCooldownTicks > 0) adorablehamsterpets$diamondSoundCooldownTicks--;
        if (adorablehamsterpets$creeperSoundCooldownTicks > 0) adorablehamsterpets$creeperSoundCooldownTicks--;


        // --- 4. Feature Ticks ---
        tickGuideBookTracking();
        PlayerGestureUtil.tickSneakTracking(self);

        // --- Supporter Crown Trial Period Tick ---
        if (!world.isClientSide()) {
            int trialTicks = this.entityData.get(AHP_CROWN_TRIAL_TICKS);
            if (trialTicks > 0) {
                this.entityData.set(AHP_CROWN_TRIAL_TICKS, trialTicks - 1);

                // Feedback
                if (trialTicks - 1 == 0) {
                    this.ahp$setSupporterCrownTheme(-1);

                    MutableComponent message = Component.literal("\n")
                            .append(Component.translatable("message.adorablehamsterpets.crown_trial_ended").withStyle(ChatFormatting.GOLD))
                            .append("\n")
                            .append(Component.translatable("message.adorablehamsterpets.crown_trial_discord")
                                    .setStyle(Style.EMPTY
                                            .withColor(ChatFormatting.AQUA)
                                            .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create("https://discord.gg/w54mk5bqdf")))
                                    ))
                            .append("\n");
                    self.sendSystemMessage(message);
                }
            }
        }

        // --- Periodic Shoulder Sync (For Replay/Flashback Mods) ---
        if (!world.isClientSide() && this.hasAnyShoulderHamster()) {
            if (++this.ahp$shoulderSyncTimer >= 20) { // Once per second
                this.ahp$shoulderSyncTimer = 0;
                this.adorablehamsterpets$syncHamsterState();
            }
        } else {
            this.ahp$shoulderSyncTimer = 0;
        }

        // --- Glowing Sunflower Easter Egg (Server) ---
        if (++this.ahp$sunflowerCheckTimer >= 20) {
            this.ahp$sunflowerCheckTimer = 0;
            if (Configs.AHP_WORLDGEN.enableGlowingSunflowers && !world.isBrightOutside()) {
                BlockPos playerPos = self.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-5, -3, -5), playerPos.offset(5, 3, 5))) {
                    BlockState state = world.getBlockState(pos);
                    if (state.is(ModBlocks.SUNFLOWER_BLOCK.get()) && state.getValue(SunflowerBlock.LIT)) {
                        ModCriteria.WITNESS_GLOWING_SUNFLOWER.get().trigger((ServerPlayer) self);
                        break;
                    }
                }
            }
        }

        // --- Genetic Visualization (Server) ---
        if (this.ahp$geneticParent1Uuid != null && this.ahp$geneticParent2Uuid != null) {
            Entity parent1 = ((ServerLevel) world).getEntity(this.ahp$geneticParent1Uuid);
            Entity parent2 = ((ServerLevel) world).getEntity(this.ahp$geneticParent2Uuid);

            if (parent1 != null && parent2 != null && parent1.isAlive() && parent2.isAlive()) {
                int countPerTick = Configs.AHP_MAIN.simulatedOffspringPerTick.get();
                ParticleEffectsUtil.spawnGeneticProbabilityCloud(world, parent1.position(), parent2.position(), countPerTick);
            }
        }

        // --- 5. Shoulder Hamster Sensing ---
        if (this.hasAnyShoulderHamster()) {
            // Diamond Detection
            if (config.enableShoulderDiamondDetection) {
                adorablehamsterpets$diamondCheckTimer++;
                if (adorablehamsterpets$diamondCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$diamondCheckTimer = 0;
                    if (isCelebrationOreNearby(self, config.shoulderDiamondDetectionRadius.get())) {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = true;
                        if (adorablehamsterpets$diamondSoundCooldownTicks == 0) {
                            world.playSound(null, self.blockPosition(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, random),
                                    SoundSource.NEUTRAL, 2.5f, 1.0f);
                            self.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.diamond_nearby").withStyle(ChatFormatting.AQUA));
                            adorablehamsterpets$diamondSoundCooldownTicks = random.nextIntBetweenInclusive(140, 200);
                            ModCriteria.HAMSTER_DIAMOND_ALERT_TRIGGERED.get().trigger((ServerPlayer) self);
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
                            world.playSound(null, self.blockPosition(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CREEPER_DETECT_SOUNDS, random),
                                    SoundSource.NEUTRAL, 1.0f, 1.0f);
                            self.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.creeper_detected").withStyle(ChatFormatting.RED));
                            adorablehamsterpets$creeperSoundCooldownTicks = random.nextIntBetweenInclusive(100, 160);
                            ModCriteria.HAMSTER_CREEPER_ALERT_TRIGGERED.get().trigger((ServerPlayer) self);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void adorablehamsterpets$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            net.dawson.adorablehamsterpets.util.HamsterRenderTracker.onPlayerDisconnect(this.getUUID());
        }
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("RETURN"))
    private void adorablehamsterpets$onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        // Server side only. skipSleepTimer is false for natural wakeup.
        if (!self.level().isClientSide() && !skipSleepTimer) {
            ServerLevel serverWorld = (ServerLevel) self.level();
            UUID ownerUuid = self.getUUID();

            // --- 1. Scan for Stuck Hamsters ---
            List<HamsterEntity> stuckHamsters = new ArrayList<>();
            for (Entity entity : serverWorld.getEntities(ModEntities.HAMSTER.get(), Entity::isAlive)) {
                if (entity instanceof HamsterEntity hamster) {
                    if (hamster.isTame() && ownerUuid.equals((hamster.getOwnerReference() == null ? null : hamster.getOwnerReference().getUUID())) && hamster.isStuckSearchingForBed()) {
                        stuckHamsters.add(hamster);
                    }
                }
            }

            // --- 2. Rescue Protocol ---
            for (HamsterEntity hamster : stuckHamsters) {
                hamster.getLinkedBedPos().ifPresent(globalPos -> {
                    if (serverWorld.dimension() == globalPos.dimension()) {
                        BlockPos bedPos = globalPos.pos();
                        BlockState bedState = serverWorld.getBlockState(bedPos);

                        // Validate Bed availability
                        if (bedState.getBlock() instanceof HamsterBedBlock && !bedState.getValue(HamsterBedBlock.OCCUPIED)) {
                            // Teleport and force sleep
                            Vec3 targetCenter = Vec3.atCenterOf(bedPos).add(0, 0.1, 0);

                            // Explicitly request teleport to sync with client
                            hamster.teleportTo(targetCenter.x, targetCenter.y, targetCenter.z);

                            hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
                            hamster.setSleeping(true);
                            hamster.setRescueSleeping(true);
                            hamster.setInSittingPose(true);

                            serverWorld.setBlock(bedPos, bedState.setValue(HamsterBedBlock.OCCUPIED, true), Block.UPDATE_ALL);

                            // Match personality pose
                            int personality = hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
                            int poseIndex = (personality >= 1 && personality <= 3) ? personality : 1;
                            hamster.getEntityData().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, "anim_hamster_sleep_pose" + poseIndex);
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

    @Unique @Override public boolean ahp$getWasSneaking() { return this.ahp$wasSneaking; }
    @Unique @Override public void ahp$setWasSneaking(boolean sneaking) { this.ahp$wasSneaking = sneaking; }
    @Unique @Override public int ahp$getSneakToggleCount() { return this.ahp$sneakToggleCount; }
    @Unique @Override public void ahp$setSneakToggleCount(int count) { this.ahp$sneakToggleCount = count; }
    @Unique @Override public int ahp$getSneakToggleTimer() { return this.ahp$sneakToggleTimer; }
    @Unique @Override public void ahp$setSneakToggleTimer(int timer) { this.ahp$sneakToggleTimer = timer; }

    @Unique
    @Override
    public List<CompoundTag> ahp$getInTransitHamsters() {
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
        return this.entityData.get(AHP_CROWN_THEME);
    }

    @Unique
    @Override
    public void ahp$setSupporterCrownTheme(int theme) {
        this.entityData.set(AHP_CROWN_THEME, theme);
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
        return this.entityData.get(AHP_HAS_USED_CROWN_TRIAL);
    }

    @Unique
    @Override
    public void ahp$setHasUsedSupporterCrownTrial(boolean used) {
        this.entityData.set(AHP_HAS_USED_CROWN_TRIAL, used);
    }

    @Unique
    @Override
    public int ahp$getSupporterCrownTrialTicks() {
        return this.entityData.get(AHP_CROWN_TRIAL_TICKS);
    }

    @Unique
    @Override
    public void ahp$setSupporterCrownTrialTicks(int ticks) {
        this.entityData.set(AHP_CROWN_TRIAL_TICKS, ticks);
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
        if (!Configs.AHP_MAIN.playerBreedingLimit.get()) {
            return true;
        }

        Player self = (Player) (Object) this;

        // Whitelist check
        if (Configs.AHP_MAIN.allowedBreeders.contains(self.getGameProfile().name())) {
            return true;
        }

        LitterLimitType type = Configs.AHP_MAIN.playerBreedingLimitType.get();
        if (type == LitterLimitType.DAILY) {
            long currentTime = this.level().getGameTime();
            long dayDuration = Configs.AHP_MAIN.useIrlTimeForBreedingLimit.get() ? 1728000L : 24000L;
            long currentDay = currentTime / dayDuration;
            long lastDay = this.ahp$lastBreedingTime / dayDuration;

            if (currentDay > lastDay) {
                this.ahp$hamstersFedForBreeding = 0;
                this.ahp$lastBreedingTime = currentTime;
            }
        }

        // Limit is in litters, so we multiply by 2 to get the number of individual hamsters they can feed
        int maxHamsters = Configs.AHP_MAIN.maxLittersPerPlayer.get() * 2;
        return this.ahp$hamstersFedForBreeding < maxHamsters;
    }

    @Unique
    @Override
    public void ahp$incrementHamstersFedForBreeding() {
        this.ahp$hamstersFedForBreeding++;
        this.ahp$lastBreedingTime = this.level().getGameTime();
    }

    @Unique
    @Override
    public void ahp$resetBreedingHistory() {
        this.ahp$hamstersFedForBreeding = 0;
        ((Player)(Object)this).sendOverlayMessage(Component.translatable("message.adorablehamsterpets.breeding.history_reset").withStyle(ChatFormatting.GREEN));
    }

    @Unique
    @Override
    public void adorablehamsterpets$startPrecisionTreeHeist(BlockPos leafPos) {
        Player self = (Player) (Object) this;
        Level world = self.level();
        if (world.isClientSide()) return;

        // --- 1. Queue Validation & Rebuild ---
        if (this.adorablehamsterpets$mountOrderQueue.isEmpty() && this.hasAnyShoulderHamster()) {
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!this.getShoulderHamster(location).isEmpty()) {
                    this.adorablehamsterpets$mountOrderQueue.addLast(location);
                }
            }
        }

        if (this.adorablehamsterpets$mountOrderQueue.isEmpty()) return;

        final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;

        // Peek next hamster
        ShoulderLocation locationToProcess = config.dismountOrder.get() == DismountOrder.LIFO
                ? this.adorablehamsterpets$mountOrderQueue.peekLast()
                : this.adorablehamsterpets$mountOrderQueue.peekFirst();

        if (locationToProcess == null) return;

        CompoundTag shoulderNbt = this.getShoulderHamster(locationToProcess);
        if (shoulderNbt.isEmpty()) {
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // Validate entity creation before altering state
        HamsterEntity hamster = HamsterNbtUtil.createFromNbt((ServerLevel) world, self, shoulderNbt);
        if (hamster == null) {
            this.setShoulderHamster(locationToProcess, new CompoundTag());
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // --- 2. Tree Heist Trigger ---
        // If looking at valid block, check for heist start
        HitResult hitResult = self.pick(5.0, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult) hitResult).getBlockPos();
            BlockState hitState = world.getBlockState(hitPos);

            if (TreeHeistUtil.isValidHeistStartBlock(hitState)) {

                TreeHeistUtil.TreeScanResult scanResult = TreeHeistUtil.scanForTree(world, hitPos);

                if (HamsterTreeSearcherEntity.isBlockOccupied(world, scanResult.treeId())) {
                    self.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.tree_heist_occupied").withStyle(ChatFormatting.RED));
                } else {
                    // Start Heist
                    HamsterTreeSearcherEntity searcher = ModEntities.HAMSTER_TREE_SEARCHER.get().create(world, EntitySpawnReason.LOAD);
                    if (searcher != null) {
                        hamster.triggerLeafPopEffects(hitPos, false);
                        TagValueOutput fullNbtOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, hamster.registryAccess());
                        hamster.saveWithoutId(fullNbtOut);
                        CompoundTag fullNbt = fullNbtOut.buildResult();

                        searcher.initializeSearch(hitPos, scanResult, fullNbt);
                        searcher.setForcedExitPos(hitPos); // Apply Precision Exit

                        world.addFreshEntity(searcher);

                        // Clear Data
                        if (config.dismountOrder.get() == DismountOrder.LIFO)
                            this.adorablehamsterpets$mountOrderQueue.pollLast();
                        else this.adorablehamsterpets$mountOrderQueue.pollFirst();
                        this.setShoulderHamster(locationToProcess, new CompoundTag());

                        // Feedback
                        world.playSound(null, self.blockPosition(), ModSounds.HAMSTER_DISMOUNT.get(), SoundSource.PLAYERS, 0.7f, 1.0f + world.getRandom().nextFloat() * 0.2f);
                        self.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.precision_tree_heist_started").withStyle(ChatFormatting.GREEN));
                    }
                }
            }
        }
    }

    @Unique
    @Override
    public boolean ahp$canPlayTagGame() {
        // --- 1. Check Config Toggle ---
        if (!Configs.AHP_MAIN.enableTagGamePlayerLimit.get()) {
            return true;
        }

        // --- 2. Check Daily Limit ---
        Level world = ((Player) (Object) this).level();
        long currentTime = world.getGameTime();

        // Calculate days passed (24000 ticks per day)
        long currentDay = currentTime / 24000L;
        long lastPlayedDay = this.ahp$lastTagGameDayTime / 24000L;

        // If new day, reset counter
        if (currentDay > lastPlayedDay) {
            this.ahp$tagGamesPlayedToday = 0;
            this.ahp$lastTagGameDayTime = currentTime;
        }

        return this.ahp$tagGamesPlayedToday < Configs.AHP_MAIN.maxDailyTagGamesPerPlayer.get();
    }

    @Unique
    @Override
    public void ahp$incrementTagGameCount() {
        this.ahp$tagGamesPlayedToday++;
        this.ahp$lastTagGameDayTime = ((Player) (Object) this).level().getGameTime();
    }

    @Unique
    @Override
    public boolean ahp$computeHasGuideBook(Player player) {
        // --- 1. Check Cursor Stack ---
        if (player.containerMenu != null) {
            ItemStack cursorStack = player.containerMenu.getCarried();
            if (!cursorStack.isEmpty() && cursorStack.is(ModItems.HAMSTER_GUIDE_BOOK.get())) {
                return true;
            }
        }

        // --- 2. Check Standard Inventory ---
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.HAMSTER_GUIDE_BOOK.get())) {

                // --- Eccentric Tomes Compat ---
                // Don't trigger effects when morphing the Eccentric Tomes book into my guidebook
                if (stack.has(DataComponents.CUSTOM_NAME)) {
                    Component customName = stack.get(DataComponents.CUSTOM_NAME);
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
    public CompoundTag getShoulderHamster(ShoulderLocation location) {
        return this.ahp$hamsterState.getCompoundOrEmpty(location.name());
    }

    @Unique
    @Override
    public void setShoulderHamster(ShoulderLocation location, CompoundTag nbt) {
        // Update local
        if (nbt == null || nbt.isEmpty()) {
            this.ahp$hamsterState.remove(location.name());
        } else {
            this.ahp$hamsterState.put(location.name(), nbt);
        }

        // Sync logic
        if (!this.level().isClientSide()) {
            this.adorablehamsterpets$syncHamsterState();
        }
    }

    @Unique
    @Override
    public void adorablehamsterpets$setRawHamsterState(CompoundTag nbt) {
        this.ahp$hamsterState = nbt;

        // Rebuild queue on client
        if (nbt.contains("ClientSyncQueue")) {
            this.adorablehamsterpets$mountOrderQueue.clear();
            ListTag list = nbt.getListOrEmpty("ClientSyncQueue");
            for (Tag e : list) {
                try {
                    this.adorablehamsterpets$mountOrderQueue.add(ShoulderLocation.valueOf(e.asString().orElse("")));
                } catch (Exception ignored) {}
            }
        }
    }

    @Unique
    @Override
    public void adorablehamsterpets$syncHamsterState() {
        if (!this.level().isClientSide()) { // Always sync (even if empty) to clear client state
            // Pack queue into compound for client
            ListTag mountOrderList = new ListTag();
            for (ShoulderLocation location : this.adorablehamsterpets$mountOrderQueue) {
                mountOrderList.add(StringTag.valueOf(location.name()));
            }
            this.ahp$hamsterState.put("ClientSyncQueue", mountOrderList);

            Player self = (Player) (Object) this;
            SyncHamsterStatePayload packet = new SyncHamsterStatePayload(this.getId(), this.ahp$hamsterState);

            // Send to self
            if (self instanceof ServerPlayer serverPlayer) {
                NetworkManager.sendToPlayer(serverPlayer, packet);
            }

            // Send to watchers
            if (self.level() instanceof ServerLevel serverWorld) {
                for (ServerPlayer otherPlayer : serverWorld.players()) {
                    if (otherPlayer != self && otherPlayer.distanceToSqr(self) < 4096) {
                        NetworkManager.sendToPlayer(otherPlayer, packet);
                    }
                }
            }
        }
    }

    @Unique
    @Override
    public void adorablehamsterpets$dismountShoulderHamster(boolean isThrow) {
        Player self = (Player) (Object) this;
        Level world = self.level();
        if (world.isClientSide()) return;

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

        final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;
        final AhpUiConfig uiConfig = AdorableHamsterPets.UI_CONFIG;
        RandomSource random = world.getRandom();

        // Skip cooling-down hamsters only when throwing
        ShoulderLocation locationToProcess = HamsterInteractionUtil.getNextSlotToDismount(self, isThrow);

        if (locationToProcess == null) return;

        CompoundTag shoulderNbt = this.getShoulderHamster(locationToProcess);
        if (shoulderNbt.isEmpty()) {
            AdorableHamsterPets.LOGGER.warn("Dismount queue pointed to an empty slot ({}). Desync probable.", locationToProcess);
            HamsterInteractionUtil.removeSlotFromDismountQueue(self, locationToProcess);
            return;
        }

        // Validate entity creation before altering state
        HamsterEntity hamster = HamsterNbtUtil.createFromNbt((ServerLevel) world, self, shoulderNbt);
        if (hamster == null) {
            this.setShoulderHamster(locationToProcess, new CompoundTag());
            HamsterInteractionUtil.removeSlotFromDismountQueue(self, locationToProcess);
            return;
        }

        // --- 2. Tree Heist Trigger ---
        // If looking at oak leaves, check for heist start
        HitResult hitResult = self.pick(5.0, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult) hitResult).getBlockPos();
            BlockState hitState = world.getBlockState(hitPos);

            if (ConfigDataCache.isHeistableLeaf(hitState) || ConfigDataCache.isHeistableLog(hitState)) {

                TreeHeistUtil.TreeScanResult scanResult = TreeHeistUtil.scanForTree(world, hitPos);

                if (HamsterTreeSearcherEntity.isBlockOccupied(world, scanResult.treeId())) {
                    self.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.tree_heist_occupied").withStyle(ChatFormatting.RED));
                    return; // Abort
                } else {
                    // Start Heist
                    HamsterTreeSearcherEntity searcher = ModEntities.HAMSTER_TREE_SEARCHER.get().create(world, EntitySpawnReason.LOAD);
                    if (searcher != null) {
                        hamster.triggerLeafPopEffects(hitPos, false);
                        TagValueOutput fullNbtOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, hamster.registryAccess());
                        hamster.saveWithoutId(fullNbtOut);
                        CompoundTag fullNbt = fullNbtOut.buildResult();

                        searcher.initializeSearch(hitPos, scanResult, fullNbt);
                        world.addFreshEntity(searcher);

                        // Clear Data
                        HamsterInteractionUtil.removeSlotFromDismountQueue(self, locationToProcess);
                        this.setShoulderHamster(locationToProcess, new CompoundTag());

                        return; // Bypass standard spawn
                    }
                }
            }
        }

        // --- 3. Throw Logic ---
        if (isThrow) {
            if (hamster.isBaby()) {
                self.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.baby_throw_refusal").withStyle(ChatFormatting.RED));
                return;
            }

            long currentTime = world.getGameTime();
            if (hamster.throwCooldownEndTick > currentTime) {
                long remainingTicks = hamster.throwCooldownEndTick - currentTime;
                long totalSecondsRemaining = Math.max(1, remainingTicks / 20);
                self.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.throw_cooldown", totalSecondsRemaining).withStyle(ChatFormatting.RED));
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
            boolean hasFeatherYeeting = self.hasEffect(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.FEATHER_YEETING.get())
            );
            long assignedCooldownDuration = FeatherYeetingStatusEffect.calculateThrowCooldownDuration(
                    config.hamsterThrowCooldown.get(),
                    hasFeatherYeeting,
                    config.featherYeetingCooldownReductionPercent.get()
            );
            hamster.throwCooldownEndTick = currentTime + assignedCooldownDuration;
            CompoundTag updatedShoulderNbt = HamsterNbtUtil.saveToHamsterState(hamster).toNbt();

            // Create Projectile
            HamsterProjectileEntity projectile = new HamsterProjectileEntity(world, self);
            projectile.snapTo(self.getX(), self.getEyeY() - 0.1, self.getZ(), self.getYRot(), self.getXRot());
            projectile.setHamsterData(updatedShoulderNbt); // Pass NBT into projectile

            Vec3 lookVec = self.getViewVector(1.0f);
            Vec3 throwVec = new Vec3(lookVec.x, lookVec.y + 0.1f, lookVec.z).normalize();
            projectile.setDeltaMovement(throwVec.scale(throwSpeed));

            // Clean up original NBT slot
            HamsterInteractionUtil.removeSlotFromDismountQueue(self, locationToProcess);
            this.setShoulderHamster(locationToProcess, new CompoundTag());

            world.addFreshEntity(projectile);

            world.playSound(null, self.getX(), self.getY(), self.getZ(), ModSounds.HAMSTER_THROW.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            // Delayed celebration squeak
            this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(world.getGameTime() + 3, () -> {
                SoundEvent celebrationSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_FLYING_SOUNDS, random);
                if (celebrationSound != null) {
                    world.playSound(null, self.getX(), self.getY(), self.getZ(), celebrationSound, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }));
            ModCriteria.HAMSTER_THROWN.get().trigger((ServerPlayer) self);
            this.adorablehamsterpets$isDiamondAlertConditionMet = false;
            return;
        }

        // --- 4. Finalize Dismount ---
        HamsterInteractionUtil.removeSlotFromDismountQueue(self, locationToProcess);

        this.setShoulderHamster(locationToProcess, new CompoundTag());

        HamsterEntity.spawnFromNbt((ServerLevel) world, self, shoulderNbt, this.adorablehamsterpets$isDiamondAlertConditionMet, true);
        this.adorablehamsterpets$isDiamondAlertConditionMet = false;

        world.playSound(null, self.blockPosition(), ModSounds.HAMSTER_DISMOUNT.get(), SoundSource.PLAYERS, 0.7f, 1.0f + random.nextFloat() * 0.2f);
        if (uiConfig.enableShoulderDismountMessages && !DISMOUNT_MESSAGE_KEYS.isEmpty()) {
            String chosenKey;
            if (DISMOUNT_MESSAGE_KEYS.size() == 1) {
                chosenKey = DISMOUNT_MESSAGE_KEYS.get(0);
            } else {
                List<String> availableKeys = new ArrayList<>(DISMOUNT_MESSAGE_KEYS);
                availableKeys.remove(this.adorablehamsterpets$lastDismountMessageKey);
                chosenKey = availableKeys.isEmpty() ? this.adorablehamsterpets$lastDismountMessageKey : availableKeys.get(random.nextInt(availableKeys.size()));
            }
            self.sendOverlayMessage(Component.translatable(chosenKey));
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
        if (this.adorablehamsterpets$clientHamsterState == null && this.level().isClientSide()) {
            this.adorablehamsterpets$clientHamsterState = new ClientShoulderHamsterData();
        }
        return this.adorablehamsterpets$clientHamsterState;
    }

    @Unique
    @Override
    public void ahp$registerTreeHeist(BlockPos treeId) {
        long time = this.level().getGameTime();
        this.ahp$heistHistory.add(new TreeHeistUtil.HeistRecord(treeId, time));
        this.ahp$heistHistory.removeIf(r -> time - r.timestamp() > HEIST_MEMORY_DURATION);
    }

    @Unique
    @Override
    public float ahp$getHeistProfitability(BlockPos treeId) {
        long time = this.level().getGameTime();
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

        if (Configs.AHP_MAIN.debugTreeDetection) {
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
        ((Player)(Object)this).sendOverlayMessage(Component.translatable("message.adorablehamsterpets.tree_heist_history_reset").withStyle(ChatFormatting.WHITE));
    }

    @Unique
    @Override
    public void ahp$startPettingHamster(int entityId) {
        Player self = (Player) (Object) this;
        Level world = self.level();
        if (world.isClientSide()) return;

        // Prevent multiple simultaneous petting animations
        if (!this.ahp$pettingHamster.isEmpty()) return;

        Entity entity = world.getEntity(entityId);
        if (entity instanceof HamsterEntity hamster) {
            // Must meet criteria
            if (!hamster.isAlive()
                    || HamsterMovementUtil.shouldNotMove(hamster)
                    || hamster.isShoulderPet())
                return;

            // Strict distance check to prevent remote manipulation
            if (hamster.distanceToSqr(self) > 64.0) return;

            this.ahp$pettingHamster = HamsterNbtUtil.saveToHamsterState(hamster).toNbt();
            this.ahp$pettingTimer = 200; // 10 seconds for petting

            // Swish SFX on pick up
            world.playSound(
                    null,
                    self.getX(),
                    self.getY(),
                    self.getZ(),
                    ModSounds.HAMSTER_SWISH.get(),
                    SoundSource.PLAYERS,
                    0.1f,
                    1.0f + world.getRandom().nextFloat() * 0.5f);

            hamster.discard();

            if (self instanceof ServerPlayer serverPlayer) {
                NetworkManager.sendToPlayer(serverPlayer, new SyncPettingStatePayload(true));
            }
        }
    }

    @Unique
    @Override
    public void ahp$cancelPettingHamster() {
        if (!this.ahp$pettingHamster.isEmpty()) {
            Player self = (Player) (Object) this;

            this.ahp$pettingTimer = 0; // Instantly trigger hamster respawn in next tick
            if (self instanceof ServerPlayer serverPlayer) {
                NetworkManager.sendToPlayer(serverPlayer, new SyncPettingStatePayload(false));
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);

        // Sync shoulder data to the watcher immediately
        if (!this.ahp$hamsterState.isEmpty()) {
            SyncHamsterStatePayload packet = new SyncHamsterStatePayload(this.getId(), this.ahp$hamsterState);
            NetworkManager.sendToPlayer(player, packet);
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
    private void ahp$pocketFollowingHamsters(Vec3 oldPos, ResourceKey<Level> oldDimension) {
        MinecraftServer server = this.level().getServer();
        if (server == null) return;

        ServerLevel oldWorld = server.getLevel(oldDimension);
        ServerLevel newWorld = (ServerLevel) this.level();
        if (oldWorld == null || newWorld == null) return;

        List<HamsterEntity> toRescue = new ArrayList<>();

        // Grab hamsters left behind at old location
        AABB oldSearchBox = new AABB(oldPos.x - 64, oldPos.y - 64, oldPos.z - 64, oldPos.x + 64, oldPos.y + 64, oldPos.z + 64);
        toRescue.addAll(oldWorld.getEntitiesOfClass(HamsterEntity.class, oldSearchBox, this::ahp$isValidRescueTarget));

        // Grab hamsters who might have already teleported to new location
        AABB newSearchBox = new AABB(this.getX() - 64, this.getY() - 64, this.getZ() - 64, this.getX() + 64, this.getY() + 64, this.getZ() + 64);
        List<HamsterEntity> newWorldHamsters = newWorld.getEntitiesOfClass(HamsterEntity.class, newSearchBox, this::ahp$isValidRescueTarget);

        for (HamsterEntity hamster : newWorldHamsters) {
            if (!toRescue.contains(hamster)) {
                toRescue.add(hamster);
            }
        }

        if (toRescue.isEmpty()) return;

        // Pocket them
        for (HamsterEntity hamster : toRescue) {
            TagValueOutput nbtOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, hamster.registryAccess());
            hamster.saveWithoutId(nbtOut);
            CompoundTag nbt = nbtOut.buildResult(); // Save complete state
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
            MinecraftServer server = hamster.level().getServer();
            if (server != null) {
                Entity parentEntity = null;
                for (ServerLevel w : server.getAllLevels()) {
                    parentEntity = w.getEntity(hamster.getParentUuid());
                    if (parentEntity != null) break;
                }

                boolean parentRescued = false;
                if (parentEntity instanceof HamsterEntity parentHamster && parentHamster.isAlive()) {
                    if (parentHamster.isTame() && this.getUUID().equals((parentHamster.getOwnerReference() == null ? null : parentHamster.getOwnerReference().getUUID()))
                            && !parentHamster.isOrderedToSit()
                            && !parentHamster.isWanderModeActive()
                            && !parentHamster.isShoulderPet()) {
                        parentRescued = true;
                    }
                } else {
                    // Parent not active in world. Check if currently mounted to shoulder
                    for (ShoulderLocation loc : ShoulderLocation.values()) {
                        CompoundTag nbt = this.getShoulderHamster(loc);
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
                        for (CompoundTag nbt : this.ahp$inTransitHamsters) {
                            if (nbt.read("UUID", UUIDUtil.CODEC).isPresent() && nbt.read("UUID", UUIDUtil.CODEC).orElse(null).equals(hamster.getParentUuid())) {
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

        if (hamster.isOrderedToSit()) {
            return false;
        }

        // Tamed and owned by player
        if (hamster.isTame() && this.getUUID().equals((hamster.getOwnerReference() == null ? null : hamster.getOwnerReference().getUUID()))) {
            return true;
        }

        return false;
    }

    /**
     * Detects when player gets the Hamster Tips guidebook. Plays FX once
     */
    @Unique
    private void tickGuideBookTracking() {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || !(self instanceof ServerPlayer player)) return;

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
                NetworkManager.sendToPlayer(player, new PlayGuidebookEffectsPayload(false));
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
                if (Configs.AHP_UI.enableAutoGuidebookDeliveryFallback) {
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
    private boolean ahp$tryFallbackDelivery(ServerPlayer player) {
        PlayerAdvancements advancementTracker = player.getAdvancements();
        Identifier flagAdvId = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "technical/has_received_initial_guidebook");
        AdvancementHolder flagAdvancementEntry = player.level().getServer().getAdvancements().get(flagAdvId);

        // Abort if they've already received the initial delivery at some point
        if (flagAdvancementEntry == null || advancementTracker.getOrStartProgress(flagAdvancementEntry).isDone()) {
            return false;
        }

        double searchRadius = 10.0;
        AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        List<HamsterEntity> nearbyHamsters = player.level().getEntitiesOfClass(
                HamsterEntity.class,
                searchBox,
                EntitySelector.ENTITY_STILL_ALIVE
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
    private boolean isCelebrationOreNearby(Player player, double radius) {
        Level world = player.level();
        BlockPos center = player.blockPosition();
        int intRadius = (int) Math.ceil(radius);

        List<BlockPos> exposedOres = new ArrayList<>();
        List<BlockPos> buriedOres = new ArrayList<>();

        for (BlockPos checkPos : BlockPos.betweenClosed(center.offset(-intRadius, -intRadius, -intRadius), center.offset(intRadius, intRadius, intRadius))) {
            if (checkPos.distSqr(center) <= radius * radius) {
                BlockState state = world.getBlockState(checkPos);

                if (ConfigDataCache.isCelebrationOre(state)) {
                    if (HamsterSniffForOreGoal.isOreExposed(checkPos, world)) {
                        exposedOres.add(checkPos.immutable());
                    } else {
                        buriedOres.add(checkPos.immutable());
                    }
                }
            }
        }
        return !exposedOres.isEmpty() || !buriedOres.isEmpty();
    }

    // Is green explody thing looking at player?
    @Unique
    private boolean creeperSeesPlayer(Player player, double radius) {
        Level world = player.level();
        AABB searchBox = new AABB(player.position().subtract(radius, radius, radius), player.position().add(radius, radius, radius));
        List<Creeper> nearbyCreepers = world.getEntitiesOfClass(
                Creeper.class,
                searchBox,
                creeper -> creeper.isAlive() && creeper.getTarget() == player && EntitySelector.ENTITY_STILL_ALIVE.test(creeper)
        );
        return !nearbyCreepers.isEmpty();
    }
}
