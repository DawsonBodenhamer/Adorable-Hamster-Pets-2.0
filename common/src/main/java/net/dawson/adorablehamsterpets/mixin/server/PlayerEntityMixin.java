package net.dawson.adorablehamsterpets.mixin.server;

import com.mojang.authlib.GameProfile;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.SunflowerBlock;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.DismountOrder;
import net.dawson.adorablehamsterpets.entity.AI.HamsterSniffForOreGoal;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    @Unique private static final long HEIST_MEMORY_DURATION = 24000L; // 1 Minecraft Day (20 mins)
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
    @Unique private NbtCompound ahp$shoulderData = new NbtCompound();
    @Unique private transient ClientShoulderHamsterData adorablehamsterpets$clientShoulderData;
    @Unique private final transient ArrayDeque<ShoulderLocation> adorablehamsterpets$mountOrderQueue = new ArrayDeque<>();

    // --- Timers & Cooldowns ---
    @Unique private int adorablehamsterpets$diamondCheckTimer = 0;
    @Unique private int adorablehamsterpets$creeperCheckTimer = 0;
    @Unique private int adorablehamsterpets$diamondSoundCooldownTicks = 0;
    @Unique private int adorablehamsterpets$creeperSoundCooldownTicks = 0;
    @Unique private int ahp$guideBookCheckTimer = 0;
    @Unique private int ahp$sunflowerCheckTimer = 0;

    // --- State Flags & Trackers ---
    @Unique private String adorablehamsterpets$lastDismountMessageKey = "";
    @Unique private boolean adorablehamsterpets$isDiamondAlertConditionMet = false;
    @Unique private int adorablehamsterpets$lastGoldMessageIndex = -1;
    @Unique private boolean ahp$cachedHasGuideBook = false;
    @Unique private boolean ahp$guideBookTrackingInitialized = false;

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
        if (world.isClient) {
            this.adorablehamsterpets$clientShoulderData = new ClientShoulderHamsterData();
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks (Mixin Injections)
     * ────────────────────────────────────────────────────────────────────────────*/

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void adorablehamsterpets$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        // --- 1. Save Shoulder Data ---
        if (!this.ahp$shoulderData.isEmpty()) {
            nbt.put("ShoulderHamsters", this.ahp$shoulderData);
        }

        // --- 2. Save Mount Queue ---
        if (!this.adorablehamsterpets$mountOrderQueue.isEmpty()) {
            NbtList mountOrderList = new NbtList();
            for (ShoulderLocation location : this.adorablehamsterpets$mountOrderQueue) {
                mountOrderList.add(NbtString.of(location.name()));
            }
            nbt.put("MountOrderQueue", mountOrderList);
        }

        // --- 3. Save History & State ---
        if (this.adorablehamsterpets$lastGoldMessageIndex != -1) {
            nbt.putInt("LastGoldMessageIndex", this.adorablehamsterpets$lastGoldMessageIndex);
        }

        // Tree Heist memory
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

        // Guidebook tracking
        nbt.putBoolean(AHP_NBT_GUIDEBOOK_HAS_KEY, this.ahp$cachedHasGuideBook);
        nbt.putBoolean(AHP_NBT_GUIDEBOOK_INIT_KEY, this.ahp$guideBookTrackingInitialized);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void adorablehamsterpets$readNbt(NbtCompound nbt, CallbackInfo ci) {
        // --- 1. Migration from Legacy Data ---
        if (nbt.contains("ShoulderHamster", NbtElement.COMPOUND_TYPE)) {
            NbtCompound oldHamsterNbt = nbt.getCompound("ShoulderHamster");
            if (!oldHamsterNbt.isEmpty()) {
                NbtCompound newShoulderPetsNbt = new NbtCompound();
                newShoulderPetsNbt.put(ShoulderLocation.RIGHT_SHOULDER.name(), oldHamsterNbt);
                this.ahp$shoulderData = newShoulderPetsNbt; // Update local field
                this.adorablehamsterpets$mountOrderQueue.clear();
                this.adorablehamsterpets$mountOrderQueue.add(ShoulderLocation.RIGHT_SHOULDER);
                nbt.remove("ShoulderHamster"); // Remove old tag to complete migration
                AdorableHamsterPets.LOGGER.info("Migrated legacy shoulder hamster data for player {}.", this.getDisplayName().getString());
            }
        } else if (nbt.contains("ShoulderHamsters", NbtElement.COMPOUND_TYPE)) {
            // Standard Read
            this.ahp$shoulderData = nbt.getCompound("ShoulderHamsters");
        }

        // --- 2. Queue Sanitization ---
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

        // --- 3. Self-Healing ---
        // If data exists but queue is empty (corruption), rebuild it
        if (this.adorablehamsterpets$mountOrderQueue.isEmpty() && this.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.info("Player {} has shoulder hamsters but empty mount queue. Rebuilding...", this.getDisplayName().getString());
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!this.getShoulderHamster(location).isEmpty()) {
                    this.adorablehamsterpets$mountOrderQueue.addLast(location);
                }
            }
        }

        // --- 4. History & State ---
        this.adorablehamsterpets$lastGoldMessageIndex = nbt.contains("LastGoldMessageIndex", NbtElement.INT_TYPE)
                ? nbt.getInt("LastGoldMessageIndex")
                : -1;

        this.ahp$heistHistory.clear();
        if (nbt.contains("AHPHeistHistory", NbtElement.LIST_TYPE)) {
            NbtList historyList = nbt.getList("AHPHeistHistory", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < historyList.size(); i++) {
                NbtCompound tag = historyList.getCompound(i);
                BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                long time = tag.getLong("t");
                this.ahp$heistHistory.add(new TreeHeistUtil.HeistRecord(pos, time));
            }
        }

        if (nbt.contains(AHP_NBT_GUIDEBOOK_HAS_KEY, NbtElement.BYTE_TYPE)) {
            this.ahp$cachedHasGuideBook = nbt.getBoolean(AHP_NBT_GUIDEBOOK_HAS_KEY);
        }
        if (nbt.contains(AHP_NBT_GUIDEBOOK_INIT_KEY, NbtElement.BYTE_TYPE)) {
            this.ahp$guideBookTrackingInitialized = nbt.getBoolean(AHP_NBT_GUIDEBOOK_INIT_KEY);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) return;

        Random random = world.getRandom();
        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 1. Process Tasks & Cooldowns ---
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

        // --- 2. Feature Ticks ---
        tickGuideBookTracking();

        // Glowing Sunflower Easter Egg (Server side only, low frequency)
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

        // --- 3. Shoulder Hamster Sensing ---
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API (PlayerEntityAccessor)
     * ────────────────────────────────────────────────────────────────────────────*/

    @Unique
    @Override
    public boolean ahp$computeHasGuideBook(PlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
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
        return this.ahp$shoulderData.getCompound(location.name());
    }

    @Unique
    @Override
    public void setShoulderHamster(ShoulderLocation location, NbtCompound nbt) {
        // Update local
        if (nbt == null || nbt.isEmpty()) {
            this.ahp$shoulderData.remove(location.name());
        } else {
            this.ahp$shoulderData.put(location.name(), nbt);
        }

        // Sync with clients manually
        if (!this.getWorld().isClient()) {
            ModPackets.SyncShoulderDataS2CPacket packet = new ModPackets.SyncShoulderDataS2CPacket(this.getId(), this.ahp$shoulderData);
            PlayerEntity self = (PlayerEntity) (Object) this;

            // Send to self
            if (self instanceof ServerPlayerEntity serverSelf) {
                ModPackets.CHANNEL.sendToPlayer(serverSelf, packet);
            }

            // Send to tracking players (Manual loop on 1.20.1)
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
    public void adorablehamsterpets$setRawShoulderData(NbtCompound nbt) {
        // Called by client packet handler to update local state
        this.ahp$shoulderData = nbt;
    }

    @Unique
    @Override
    public void adorablehamsterpets$syncShoulderData() {
        // Called via PlayerEvent.PLAYER_JOIN to ensure connection is ready before sending
        if (!this.getWorld().isClient() && !this.ahp$shoulderData.isEmpty()) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            if (self instanceof ServerPlayerEntity serverPlayer) {
                // On 1.20.1, use ModPackets.CHANNEL and the inner record class
                var packet = new ModPackets.SyncShoulderDataS2CPacket(this.getId(), this.ahp$shoulderData);
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
        HamsterEntity hamster = HamsterEntity.createFromNbt((ServerWorld) world, self, shoulderNbt);
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
                    self.sendMessage(Text.translatable("message.adorablehamsterpets.tree_heist.occupied").formatted(Formatting.RED), true);
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

            // Prep for launch
            hamster.refreshPositionAndAngles(self.getX(), self.getEyeY() - 0.1, self.getZ(), self.getYaw(), self.getPitch());
            hamster.setThrown(true);
            hamster.interactionCooldown = 10;
            hamster.throwCooldownEndTick = currentTime + config.hamsterThrowCooldown.get();

            // Calculate Yeet Speed
            boolean isBuffed = hamster.hasGreenBeanBuff();
            float throwSpeed = isBuffed ? config.hamsterThrowVelocityBuffed.get().floatValue() : config.hamsterThrowVelocity.get().floatValue();

            ItemStack armorStack = hamster.getArmorStack();
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
                if (config.enableArmorPerks.get() && armorItem.getMaterial() == HamsterArmorItem.HamsterArmorMaterial.IRON) {
                    throwSpeed += config.ironArmorThrowSpeedBoost.get().floatValue();
                }
            }

            Vec3d lookVec = self.getRotationVec(1.0f);
            Vec3d throwVec = new Vec3d(lookVec.x, lookVec.y + 0.1f, lookVec.z).normalize();
            hamster.setVelocity(throwVec.multiply(throwSpeed));
            hamster.velocityDirty = true;
        }

        // --- 4. Finalize Dismount ---
        if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
        else this.adorablehamsterpets$mountOrderQueue.pollFirst();

        this.setShoulderHamster(locationToProcess, new NbtCompound());

        // Spawn & Alert check
        HamsterEntity.spawnFromNbt((ServerWorld) world, self, shoulderNbt, this.adorablehamsterpets$isDiamondAlertConditionMet, hamster);
        this.adorablehamsterpets$isDiamondAlertConditionMet = false;

        if (isThrow) {
            world.playSound(null, self.getX(), self.getY(), self.getZ(), ModSounds.HAMSTER_THROW.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
            // Delayed celebration squeak
            this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(world.getTime() + 3, () -> {
                SoundEvent celebrationSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_FLYING_SOUNDS, random);
                if (celebrationSound != null) {
                    world.playSound(null, self.getX(), self.getY(), self.getZ(), celebrationSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }));
            ModCriteria.HAMSTER_THROWN.trigger((ServerPlayerEntity) self);
        } else {
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
    public int ahp_getLastGoldMessageIndex() {
        return this.adorablehamsterpets$lastGoldMessageIndex;
    }

    @Unique
    @Override
    public void ahp_setLastGoldMessageIndex(int index) {
        this.adorablehamsterpets$lastGoldMessageIndex = index;
    }

    @Unique
    @Override
    public ArrayDeque<ShoulderLocation> adorablehamsterpets$getMountOrderQueue() {
        return this.adorablehamsterpets$mountOrderQueue;
    }

    @Unique
    @Override
    public ClientShoulderHamsterData adorablehamsterpets$getClientShoulderData() {
        // Lazy init for safety
        if (this.adorablehamsterpets$clientShoulderData == null && this.getWorld().isClient) {
            this.adorablehamsterpets$clientShoulderData = new ClientShoulderHamsterData();
        }
        return this.adorablehamsterpets$clientShoulderData;
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

        // 1. Prune expired first
        this.ahp$heistHistory.removeIf(r -> time - r.timestamp() > HEIST_MEMORY_DURATION);
        int prunedSize = this.ahp$heistHistory.size();

        // 2. Count nearby recent heists
        int matchCount = 0;
        List<Long> matchAges = new ArrayList<>();

        for (TreeHeistUtil.HeistRecord record : this.ahp$heistHistory) {
            if (record.pos().equals(treeId)) {
                matchCount++;
                matchAges.add(time - record.timestamp());
            }
        }

        // 3. Calculate sliding scale
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
        // Cast to PlayerEntity to access the 2-argument sendMessage method
        ((PlayerEntity)(Object)this).sendMessage(Text.translatable("message.adorablehamsterpets.heist_history_reset").formatted(Formatting.WHITE), true);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);

        // Sync shoulder data to the watcher immediately
        if (!this.ahp$shoulderData.isEmpty()) {
            ModPackets.SyncShoulderDataS2CPacket packet = new ModPackets.SyncShoulderDataS2CPacket(this.getId(), this.ahp$shoulderData);
            ModPackets.CHANNEL.sendToPlayer(player, packet);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    // Detects when player gets a book. Plays FX once.
    @Unique
    private void tickGuideBookTracking() {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (!(self instanceof ServerPlayerEntity player)) return;

        if (++this.ahp$guideBookCheckTimer < AHP_GUIDEBOOK_CHECK_INTERVAL_TICKS) {
            return;
        }
        this.ahp$guideBookCheckTimer = 0;

        // Init guard
        if (!this.ahp$guideBookTrackingInitialized) {
            this.ahp$initGuideBookTracking(this.ahp$computeHasGuideBook(player));
            return;
        }

        boolean hasNow = ahp$computeHasGuideBook(player);

        // Edge: No -> Yes
        if (hasNow && !this.ahp$cachedHasGuideBook) {
            ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayGuidebookEffectsS2CPacket(false));
        }

        this.ahp$cachedHasGuideBook = hasNow;
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