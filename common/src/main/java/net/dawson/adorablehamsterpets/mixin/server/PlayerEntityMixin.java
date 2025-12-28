package net.dawson.adorablehamsterpets.mixin.server;

import com.mojang.authlib.GameProfile;
import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.DismountOrder;
import net.dawson.adorablehamsterpets.entity.AI.HamsterSeekDiamondGoal;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.payload.SyncShoulderDataPayload;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
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

    // --- 1. Data Storage ---
    @Unique
    private NbtCompound ahp$shoulderData = new NbtCompound();

    @Unique
    private transient ClientShoulderHamsterData adorablehamsterpets$clientShoulderData;

    // --- Constants and Static Utilities ---
    @Unique private static final int CHECK_INTERVAL_TICKS = 20;
    @Unique private static final List<String> DISMOUNT_MESSAGE_KEYS = Arrays.asList(
            "message.adorablehamsterpets.dismount.1", "message.adorablehamsterpets.dismount.2",
            "message.adorablehamsterpets.dismount.3", "message.adorablehamsterpets.dismount.4",
            "message.adorablehamsterpets.dismount.5", "message.adorablehamsterpets.dismount.6"
    );

    // --- Fields ---
    @Unique private int adorablehamsterpets$diamondCheckTimer = 0;
    @Unique private int adorablehamsterpets$creeperCheckTimer = 0;
    @Unique private int adorablehamsterpets$diamondSoundCooldownTicks = 0;
    @Unique private int adorablehamsterpets$creeperSoundCooldownTicks = 0;
    @Unique private String adorablehamsterpets$lastDismountMessageKey = "";
    @Unique private boolean adorablehamsterpets$isDiamondAlertConditionMet = false;
    @Unique private int adorablehamsterpets$lastGoldMessageIndex = -1;
    @Unique private final transient ArrayDeque<ShoulderLocation> adorablehamsterpets$mountOrderQueue = new ArrayDeque<>();
    @Unique private final List<ScheduledTask> adorablehamsterpets$scheduledTasks = new ArrayList<>();
    @Unique private record ScheduledTask(long executionTick, Runnable action) {}

    // --- Constructor Injection ---
    @Inject(method = "<init>", at = @At("TAIL"))
    private void adorablehamsterpets$onInit(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        if (world.isClient) {
            this.adorablehamsterpets$clientShoulderData = new ClientShoulderHamsterData();
        }
    }

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // --- 2. NBT Read/Write ---
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void adorablehamsterpets$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        // Save from local field
        if (!this.ahp$shoulderData.isEmpty()) {
            nbt.put("ShoulderHamsters", this.ahp$shoulderData);
        }

        // Save Mount Order Queue
        if (!this.adorablehamsterpets$mountOrderQueue.isEmpty()) {
            NbtList mountOrderList = new NbtList();
            for (ShoulderLocation location : this.adorablehamsterpets$mountOrderQueue) {
                mountOrderList.add(NbtString.of(location.name()));
            }
            nbt.put("MountOrderQueue", mountOrderList);
        }

        if (this.adorablehamsterpets$lastGoldMessageIndex != -1) {
            nbt.putInt("LastGoldMessageIndex", this.adorablehamsterpets$lastGoldMessageIndex);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void adorablehamsterpets$readNbt(NbtCompound nbt, CallbackInfo ci) {
        // --- Backward Compatibility: Check for old single hamster data ---
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

        // Read Mount Order Queue with Auto-Sanitization
        this.adorablehamsterpets$mountOrderQueue.clear();
        if (nbt.contains("MountOrderQueue", NbtElement.LIST_TYPE)) {
            NbtList mountOrderList = nbt.getList("MountOrderQueue", NbtElement.STRING_TYPE);

            // Track seen locations to prevent duplicates
            Set<ShoulderLocation> seenLocations = new HashSet<>();

            for (NbtElement element : mountOrderList) {
                try {
                    ShoulderLocation location = ShoulderLocation.valueOf(element.asString());

                    // Sanitize: Only add if:
                    // 1. Haven't added this location already (Deduplication)
                    // 2. The shoulder slot actually contains data (Ghost cleanup)
                    if (!seenLocations.contains(location) && !this.getShoulderHamster(location).isEmpty()) {
                        this.adorablehamsterpets$mountOrderQueue.add(location);
                        seenLocations.add(location);
                    }
                } catch (IllegalArgumentException e) {
                    AdorableHamsterPets.LOGGER.warn("Found invalid ShoulderLocation name in NBT: {}", element.asString());
                }
            }
        }

        // --- Self-Healing Logic for Potential Corrupted State ---
        // If sanitization cleared everything but there's still data, rebuild cleanly.
        if (this.adorablehamsterpets$mountOrderQueue.isEmpty() && this.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.info("Player {} has shoulder hamsters but an empty mount queue. Rebuilding queue...", this.getDisplayName().getString());
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!this.getShoulderHamster(location).isEmpty()) {
                    this.adorablehamsterpets$mountOrderQueue.addLast(location);
                }
            }
            AdorableHamsterPets.LOGGER.info("Successfully rebuilt mount queue for player {}. New queue: {}", this.getDisplayName().getString(), this.adorablehamsterpets$mountOrderQueue);
        }

        if (nbt.contains("LastGoldMessageIndex", NbtElement.INT_TYPE)) {
            this.adorablehamsterpets$lastGoldMessageIndex = nbt.getInt("LastGoldMessageIndex");
        } else {
            this.adorablehamsterpets$lastGoldMessageIndex = -1;
        }
    }

    /**
     * Injects the start tracking logic using the vanilla/Yarn mapped method.
     * This fires when another player starts tracking this player entity.
     * We use this moment to send the shoulder data to the watcher so they can render the hamsters.
     */
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        // Call super first (vanilla logic)
        super.onStartedTrackingBy(player);

        // If this player has shoulder data, send it to the player who just started watching
        if (!this.ahp$shoulderData.isEmpty()) {
            SyncShoulderDataPayload packet = new SyncShoulderDataPayload(this.getId(), this.ahp$shoulderData);
            NetworkManager.sendToPlayer(player, packet);
        }
    }

    @Inject(method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void adorablehamsterpets$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!this.getWorld().isClient()) {
            net.dawson.adorablehamsterpets.util.HamsterRenderTracker.onPlayerDisconnect(this.getUuid());
        }
    }

    // --- 3. Public Accessors ---
    @Unique
    @Override
    public NbtCompound getShoulderHamster(ShoulderLocation location) {
        return this.ahp$shoulderData.getCompound(location.name());
    }

    @Unique
    @Override
    public void setShoulderHamster(ShoulderLocation location, NbtCompound nbt) {
        // Update local NBT
        if (nbt == null || nbt.isEmpty()) {
            this.ahp$shoulderData.remove(location.name());
        } else {
            this.ahp$shoulderData.put(location.name(), nbt);
        }

        // Sync with clients manually
        if (!this.getWorld().isClient()) {
            SyncShoulderDataPayload packet = new SyncShoulderDataPayload(this.getId(), this.ahp$shoulderData);
            PlayerEntity self = (PlayerEntity) (Object) this;

            // Send to self
            if (self instanceof ServerPlayerEntity serverSelf) {
                NetworkManager.sendToPlayer(serverSelf, packet);
            }

            // Send to tracking players
            if (self.getWorld() instanceof ServerWorld serverWorld) {
                for (ServerPlayerEntity otherPlayer : serverWorld.getPlayers()) {
                    // Standard tracking range is usually 64 blocks squared
                    if (otherPlayer != self && otherPlayer.squaredDistanceTo(self) < 4096) {
                        NetworkManager.sendToPlayer(otherPlayer, packet);
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
                SyncShoulderDataPayload packet = new SyncShoulderDataPayload(this.getId(), this.ahp$shoulderData);
                NetworkManager.sendToPlayer(serverPlayer, packet);
            }
        }
    }

    // --- 4. Tick Logic ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        // Initial Setup and Server-Side Check
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) {
            return;
        }
        Random random = world.getRandom();
        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // Process Scheduled Tasks
        long currentTime = world.getTime();
        adorablehamsterpets$scheduledTasks.removeIf(task -> {
            if (currentTime >= task.executionTick()) {
                task.action().run();
                return true;
            }
            return false;
        });

        // Cooldown Decrement
        if (adorablehamsterpets$diamondSoundCooldownTicks > 0) adorablehamsterpets$diamondSoundCooldownTicks--;
        if (adorablehamsterpets$creeperSoundCooldownTicks > 0) adorablehamsterpets$creeperSoundCooldownTicks--;

        // Shoulder Pet Logic
        if (this.hasAnyShoulderHamster()) {

            // Shoulder Diamond Detection
            if (config.enableShoulderDiamondDetection) {
                adorablehamsterpets$diamondCheckTimer++;
                if (adorablehamsterpets$diamondCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$diamondCheckTimer = 0;
                    // The isCelebrationOreNearby method internally prioritizes exposed ore.
                    if (isCelebrationOreNearby(self, config.shoulderDiamondDetectionRadius.get())) {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = true;
                        if (adorablehamsterpets$diamondSoundCooldownTicks == 0) {
                            world.playSound(null, self.getBlockPos(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, random),
                                    SoundCategory.NEUTRAL, 2.5f, 1.0f);
                            self.sendMessage(Text.translatable("message.adorablehamsterpets.diamond_nearby").formatted(Formatting.AQUA), true);
                            adorablehamsterpets$diamondSoundCooldownTicks = random.nextBetween(140, 200);
                            ModCriteria.HAMSTER_DIAMOND_ALERT_TRIGGERED.get().trigger((ServerPlayerEntity) self);
                        }
                    } else {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = false;
                    }
                }
            }

            // Shoulder Creeper Detection
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
                            ModCriteria.HAMSTER_CREEPER_ALERT_TRIGGERED.get().trigger((ServerPlayerEntity) self);
                        }
                    }
                }
            }
        }
    }

    // --- Dismount Logic ---
    /**
     * Executes the server-side logic to dismount a hamster from the player's shoulder.
     * This method is triggered upon receiving a {@code DismountHamsterPayload} from the client.
     * It handles choosing which hamster to dismount if there are more than one,
     * spawning the hamster entity from its stored data, clearing the player's
     * shoulder data, and playing the necessary sounds and messages.
     */
    @Unique
    @Override
    public void adorablehamsterpets$dismountShoulderHamster(boolean isThrow) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) {
            return;
        }

        // Self-Healing: Rebuild Queue if Desynced
        if (this.adorablehamsterpets$mountOrderQueue.isEmpty() && this.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.warn("[HamsterDismount] Player {} has shoulder hamsters but empty queue. Rebuilding...", self.getName().getString());
            for (ShoulderLocation location : ShoulderLocation.values()) {
                if (!this.getShoulderHamster(location).isEmpty()) {
                    this.adorablehamsterpets$mountOrderQueue.addLast(location);
                }
            }
        }

        if (this.adorablehamsterpets$mountOrderQueue.isEmpty()) {
            return;
        }

        final AhpConfig config = AdorableHamsterPets.CONFIG;
        Random random = world.getRandom();

        // Determine which hamster to dismount/throw
        ShoulderLocation locationToProcess = config.dismountOrder.get() == DismountOrder.LIFO
                ? this.adorablehamsterpets$mountOrderQueue.peekLast()
                : this.adorablehamsterpets$mountOrderQueue.peekFirst();

        if (locationToProcess == null) return;

        NbtCompound shoulderNbt = this.getShoulderHamster(locationToProcess);
        if (shoulderNbt.isEmpty()) {
            AdorableHamsterPets.LOGGER.warn("Dismount queue pointed to an empty slot ({}). This may indicate a desync.", locationToProcess);
            // Remove the bad entry from the queue
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // Create Hamster Instance for Validation
        HamsterEntity hamster = HamsterEntity.createFromNbt((ServerWorld) world, self, shoulderNbt);
        if (hamster == null) {
            this.setShoulderHamster(locationToProcess, new NbtCompound());
            // Remove from queue
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // Handle Throw-Specific Logic
        if (isThrow) {
            if (hamster.isBaby()) {
                self.sendMessage(Text.translatable("message.adorablehamsterpets.baby_throw_refusal").formatted(Formatting.RED), true);
                return;
            }

            long currentTime = world.getTime();
            if (hamster.throwCooldownEndTick > currentTime) {
                long remainingTicks = hamster.throwCooldownEndTick - currentTime;
                long totalSecondsRemaining = remainingTicks / 20;
                long minutes = totalSecondsRemaining / 60;
                long seconds = totalSecondsRemaining % 60;
                self.sendMessage(Text.translatable("message.adorablehamsterpets.throw_cooldown", minutes, seconds).formatted(Formatting.RED), true);
                return;
            }

            // Set the initial position to the player's eye level
            hamster.refreshPositionAndAngles(self.getX(), self.getEyeY() - 0.1, self.getZ(), self.getYaw(), self.getPitch());

            // Set Throw States
            hamster.setThrown(true);
            hamster.interactionCooldown = 10;
            hamster.throwCooldownEndTick = currentTime + config.hamsterThrowCooldown.get();

            // Dynamic Velocity Logic
            boolean isBuffed = hamster.hasGreenBeanBuff();
            float throwSpeed = isBuffed ? config.hamsterThrowVelocityBuffed.get().floatValue() : config.hamsterThrowVelocity.get().floatValue();
            Vec3d lookVec = self.getRotationVec(1.0f);
            Vec3d throwVec = new Vec3d(lookVec.x, lookVec.y + 0.1f, lookVec.z).normalize();
            hamster.setVelocity(throwVec.multiply(throwSpeed));
            hamster.velocityDirty = true;
        }

        // Finalize Dismount/Throw
        if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
        else this.adorablehamsterpets$mountOrderQueue.pollFirst();

        // Clear the data and trigger the manual sync packet
        this.setShoulderHamster(locationToProcess, new NbtCompound());

        // Spawn and Play Effects
        HamsterEntity.spawnFromNbt((ServerWorld) world, self, shoulderNbt, this.adorablehamsterpets$isDiamondAlertConditionMet, hamster);
        this.adorablehamsterpets$isDiamondAlertConditionMet = false;

        if (isThrow) {
            // --- Throw-Specific Effects ---
            // Play throw sound at the player's location
            world.playSound(null, self.getX(), self.getY(), self.getZ(), ModSounds.HAMSTER_THROW.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Schedule airborne celebration sound with a 3-tick delay
            this.adorablehamsterpets$scheduledTasks.add(new ScheduledTask(world.getTime() + 3, () -> {
                SoundEvent celebrationSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_FLYING_SOUNDS, random);
                if (celebrationSound != null) {
                    world.playSound(null, self.getX(), self.getY(), self.getZ(), celebrationSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }));
            ModCriteria.HAMSTER_THROWN.get().trigger((ServerPlayerEntity) self);
        } else {
            // --- Standard Dismount Effects ---
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

    // --- Helper Methods ---
    /**
     * Scans a spherical area around the player for "Desirable" ore blocks (configured via Config), prioritizing exposed ores.
     *
     * @param player The player to check around.
     * @param radius The radius of the sphere to scan, in blocks.
     * @return {@code true} if any desirable ore is found (with exposed ones taking precedence), otherwise {@code false}.
     */
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

                // Use the ConfigDataCache to check against the user-configured list of "Desirable Ores" that cause celebration upon being found
                if (ConfigDataCache.isCelebrationOre(state)) {
                    // Use public static helper from HamsterSeekDiamondGoal
                    if (HamsterSeekDiamondGoal.isOreExposed(checkPos, world)) {
                        exposedOres.add(checkPos.toImmutable());
                    } else {
                        buriedOres.add(checkPos.toImmutable());
                    }
                }
            }
        }
        // Prioritize exposed ores. If any are found, the condition is met.
        // If not, check if any buried ores were found as a fallback.
        return !exposedOres.isEmpty() || !buriedOres.isEmpty();
    }

    /**
     * Checks for nearby creepers that are actively targeting the player.
     * This is used for the shoulder hamster's creeper alert feature.
     *
     * @param player The player being targeted.
     * @param radius The search radius for creepers.
     * @return {@code true} if at least one creeper is found with the player as its current attack target, otherwise {@code false}.
     */
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
        // Lazy initialization to support shader/fake players
        if (this.adorablehamsterpets$clientShoulderData == null && this.getWorld().isClient) {
            this.adorablehamsterpets$clientShoulderData = new ClientShoulderHamsterData();
        }
        return this.adorablehamsterpets$clientShoulderData;
    }
}