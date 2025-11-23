package net.dawson.adorablehamsterpets.mixin.server;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.DismountOrder;
import net.dawson.adorablehamsterpets.entity.AI.HamsterSeekDiamondGoal;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityAccessor {

    // --- 1. DataTracker Definition ---
    @Unique
    private static final TrackedData<NbtCompound> SHOULDER_HAMSTERS = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
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

    // --- Inject into the constructor to initialize the data holder ---
    @Inject(method = "<init>", at = @At("TAIL"))
    private void adorablehamsterpets$onInit(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        // This ensures every PlayerEntity on the client gets its own data manager instance.
        if (world.isClient) {
            this.adorablehamsterpets$clientShoulderData = new ClientShoulderHamsterData();
        }
    }

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // --- 2. DataTracker Registration ---
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void adorablehamsterpets$initDataTracker(CallbackInfo ci) {
        // For 1.20.1, use startTracking instead of builder.add
        this.dataTracker.startTracking(SHOULDER_HAMSTERS, new NbtCompound());
    }

    // --- 3. NBT Read/Write ---
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void adorablehamsterpets$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        AdorableHamsterPets.LOGGER.trace("[AHP Mixin] PlayerEntityMixin writeNbt is RUNNING for entity {}.", this.getId());

        // --- Save the single compound from the DataTracker ---
        NbtCompound shoulderPetsNbt = this.getDataTracker().get(SHOULDER_HAMSTERS);
        if (!shoulderPetsNbt.isEmpty()) {
            nbt.put("ShoulderHamsters", shoulderPetsNbt);
        }

        // --- Save Mount Order Queue ---
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
        AdorableHamsterPets.LOGGER.trace("[AHP Mixin] PlayerEntityMixin readNbt is RUNNING for entity {}.", this.getId());

        // --- Backward Compatibility: Check for old single hamster data ---
        if (nbt.contains("ShoulderHamster", NbtElement.COMPOUND_TYPE)) {
            NbtCompound oldHamsterNbt = nbt.getCompound("ShoulderHamster");
            if (!oldHamsterNbt.isEmpty()) {
                NbtCompound newShoulderPetsNbt = new NbtCompound();
                newShoulderPetsNbt.put(ShoulderLocation.RIGHT_SHOULDER.name(), oldHamsterNbt);
                this.getDataTracker().set(SHOULDER_HAMSTERS, newShoulderPetsNbt);
                this.adorablehamsterpets$mountOrderQueue.clear();
                this.adorablehamsterpets$mountOrderQueue.add(ShoulderLocation.RIGHT_SHOULDER);
                nbt.remove("ShoulderHamster"); // Remove old tag to complete migration
                AdorableHamsterPets.LOGGER.info("Migrated legacy shoulder hamster data for player {}.", this.getDisplayName().getString());
            }
        } else if (nbt.contains("ShoulderHamsters", NbtElement.COMPOUND_TYPE)) {
            // --- Standard Read for New Data Format ---
            this.getDataTracker().set(SHOULDER_HAMSTERS, nbt.getCompound("ShoulderHamsters"));
        }

        // --- Read Mount Order Queue ---
        this.adorablehamsterpets$mountOrderQueue.clear();
        if (nbt.contains("MountOrderQueue", NbtElement.LIST_TYPE)) {
            NbtList mountOrderList = nbt.getList("MountOrderQueue", NbtElement.STRING_TYPE);
            for (NbtElement element : mountOrderList) {
                try {
                    this.adorablehamsterpets$mountOrderQueue.add(ShoulderLocation.valueOf(element.asString()));
                } catch (IllegalArgumentException e) {
                    AdorableHamsterPets.LOGGER.warn("Found invalid ShoulderLocation name in NBT: {}", element.asString());
                }
            }
        }

        // --- Self-Healing Logic for Potential Corrupted State ---
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

    // --- Player Removal Cleanup ---
    /**
     * Injects into the entity's remove method.
     * When a player is removed from the world (e.g., disconnects), this ensures they are
     * cleaned up from the server-side render tracker to prevent memory leaks.
     */
    @Inject(method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void adorablehamsterpets$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!this.getWorld().isClient()) {
            net.dawson.adorablehamsterpets.util.HamsterRenderTracker.onPlayerDisconnect(this.getUuid());
        }
    }

    // --- 4. Public Accessors for the DataTracker ---
    @Unique
    @Override
    public NbtCompound getShoulderHamster(ShoulderLocation location) {
        NbtCompound allShoulderPets = this.getDataTracker().get(SHOULDER_HAMSTERS);
        // Return the specific compound, or an empty one if it doesn't exist.
        return allShoulderPets.getCompound(location.name());
    }

    @Unique
    @Override
    public void setShoulderHamster(ShoulderLocation location, NbtCompound nbt) {
        NbtCompound allShoulderPets = new NbtCompound();
        // Create a mutable copy to avoid modifying the original from the DataTracker directly.
        allShoulderPets.copyFrom(this.getDataTracker().get(SHOULDER_HAMSTERS));

        if (nbt == null || nbt.isEmpty()) {
            allShoulderPets.remove(location.name());
        } else {
            allShoulderPets.put(location.name(), nbt);
        }
        this.getDataTracker().set(SHOULDER_HAMSTERS, allShoulderPets);
    }

    // --- 5. Tick Logic ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        // --- 1. Initial Setup and Server-Side Check ---
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) {
            return;
        }
        Random random = world.getRandom();
        final AhpConfig config = AdorableHamsterPets.CONFIG;

        // --- 2. Process Scheduled Tasks ---
        long currentTime = world.getTime();
        adorablehamsterpets$scheduledTasks.removeIf(task -> {
            if (currentTime >= task.executionTick()) {
                task.action().run();
                return true;
            }
            return false;
        });

        // --- 3. Cooldown Decrement ---
        if (adorablehamsterpets$diamondSoundCooldownTicks > 0) adorablehamsterpets$diamondSoundCooldownTicks--;
        if (adorablehamsterpets$creeperSoundCooldownTicks > 0) adorablehamsterpets$creeperSoundCooldownTicks--;

        // --- 4. Shoulder Pet Logic ---
        if (this.hasAnyShoulderHamster()) {

            // --- 4a. Shoulder Diamond Detection ---
            if (config.enableShoulderDiamondDetection) {
                adorablehamsterpets$diamondCheckTimer++;
                if (adorablehamsterpets$diamondCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$diamondCheckTimer = 0;
                    // The isDiamondNearby method internally prioritizes exposed ore.
                    if (isDiamondNearby(self, config.shoulderDiamondDetectionRadius.get())) {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = true; // Set the priming flag
                        if (adorablehamsterpets$diamondSoundCooldownTicks == 0) {
                            world.playSound(null, self.getBlockPos(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, random),
                                    SoundCategory.NEUTRAL, 2.5f, 1.0f);
                            self.sendMessage(Text.translatable("message.adorablehamsterpets.diamond_nearby").formatted(Formatting.AQUA), true);
                            adorablehamsterpets$diamondSoundCooldownTicks = random.nextBetween(140, 200);
                            ModCriteria.HAMSTER_DIAMOND_ALERT_TRIGGERED.trigger((ServerPlayerEntity) self);
                        }
                    } else {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = false; // Clear the priming flag
                    }
                }
            }

            // --- 4b. Shoulder Creeper Detection ---
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

    // --- Dismount Shoulder Hamster ---
    /**
     * Executes the server-side logic to dismount a hamster from the player's shoulder.
     * This method is triggered upon receiving a {@code DismountHamsterPayload} from the client.
     * It handles choosing which hamster to dismount if there are more than one,
     * spawning the hamster entity from its stored NBT data, clearing the player's
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

        // --- Self-Healing: Rebuild Queue if Desynced ---
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

        // --- 1. Determine which hamster to dismount/throw ---
        ShoulderLocation locationToProcess = config.dismountOrder.get() == DismountOrder.LIFO
                ? this.adorablehamsterpets$mountOrderQueue.peekLast()  // Peek, don't remove yet
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

        // --- 2. Create Hamster Instance for Validation ---
        HamsterEntity hamster = HamsterEntity.createFromNbt((ServerWorld) world, self, shoulderNbt);
        if (hamster == null) {
            AdorableHamsterPets.LOGGER.error("Failed to create hamster from NBT for slot {}. Clearing data.", locationToProcess);
            this.setShoulderHamster(locationToProcess, new NbtCompound());
            // Also remove from queue
            if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
            else this.adorablehamsterpets$mountOrderQueue.pollFirst();
            return;
        }

        // --- 3. Handle Throw-Specific Logic & Validation ---
        if (isThrow) {
            if (hamster.isBaby()) {
                self.sendMessage(Text.translatable("message.adorablehamsterpets.baby_throw_refusal").formatted(Formatting.RED), true);
                return; // Abort, do not dismount
            }

            long currentTime = world.getTime();
            if (hamster.throwCooldownEndTick > currentTime) {
                long remainingTicks = hamster.throwCooldownEndTick - currentTime;
                long totalSecondsRemaining = remainingTicks / 20;
                long minutes = totalSecondsRemaining / 60;
                long seconds = totalSecondsRemaining % 60;
                self.sendMessage(Text.translatable("message.adorablehamsterpets.throw_cooldown", minutes, seconds).formatted(Formatting.RED), true);
                return; // Abort, do not dismount
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

        // --- 4. Finalize Dismount/Throw ---
        // Now that all checks have passed, officially remove from queue
        if (config.dismountOrder.get() == DismountOrder.LIFO) this.adorablehamsterpets$mountOrderQueue.pollLast();
        else this.adorablehamsterpets$mountOrderQueue.pollFirst();

        this.setShoulderHamster(locationToProcess, new NbtCompound()); // Clear the slot

        // --- 5. Spawn and Play Effects ---
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

            ModCriteria.HAMSTER_THROWN.trigger((ServerPlayerEntity) self);
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

    /**
     * Scans a spherical area around the player for diamond ore blocks, prioritizing exposed ores.
     *
     * @param player The player to check around.
     * @param radius The radius of the sphere to scan, in blocks.
     * @return {@code true} if any diamond ore is found (with exposed ones taking precedence), otherwise {@code false}.
     */
    @Unique
    private boolean isDiamondNearby(PlayerEntity player, double radius) {
        World world = player.getWorld();
        BlockPos center = player.getBlockPos();
        int intRadius = (int) Math.ceil(radius);

        List<BlockPos> exposedOres = new ArrayList<>();
        List<BlockPos> buriedOres = new ArrayList<>();

        for (BlockPos checkPos : BlockPos.iterate(center.add(-intRadius, -intRadius, -intRadius), center.add(intRadius, intRadius, intRadius))) {
            if (checkPos.getSquaredDistance(center) <= radius * radius) {
                BlockState state = world.getBlockState(checkPos);
                if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                    // Use the public static helper from the HamsterSeekDiamondGoal
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
        // On the server, this will be null, which is fine as it's only used by client code.
        return this.adorablehamsterpets$clientShoulderData;
    }
}