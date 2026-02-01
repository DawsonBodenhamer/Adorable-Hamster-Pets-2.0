package net.dawson.adorablehamsterpets.entity.custom;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HamsterTreeSearcherEntity extends Entity {

    // --- Static Registry for Active Heists ---
    // Maps a specific Tree Anchor (Dimension + BlockPos) to the ID of the entity searching it.
    private static final Map<GlobalPos, Integer> ACTIVE_HEISTS = new ConcurrentHashMap<>();

    // --- Persistence Fields ---
    private NbtCompound hamsterNbt = new NbtCompound();
    private final List<Long> validLeafPositions = new ArrayList<>();
    private int searchTimer;
    private int maxSearchDuration;
    private int validationTimer;
    private int rummageTimer;
    private float dropChanceMultiplier = 1.0f;
    private boolean isExhausted = false;
    private BlockPos treeAnchor = null;
    private boolean hasAcornHat = false;
    private int dropCooldown = 0;

    // --- Transient State ---
    private boolean isRegistered = false; // Tracks if this entity has locked its tree in the map

    // --- Constants ---
    private static final int VALIDATION_INTERVAL = 20; // Check tree integrity every second
    private static final int BASE_DURATION_MIN = 180;  // 9 sec
    private static final int BASE_DURATION_MAX = 280;  // 14 sec
    private static final float HAT_DROP_CHANCE_MULTIPLIER = 2.0f;
    private static final float BASE_DROP_CHANCE = 0.03f; // 3%
    private static final float DURATION_MULTIPLIER = 2.0f; // (currently unused)

    public HamsterTreeSearcherEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // No client-synced data needed for this logic entity
    }

    /**
     * Checks if a tree anchor is currently being searched by any hamster.
     */
    public static boolean isTreeBlocked(World world, BlockPos anchor) {
        if (anchor == null || world.isClient()) return false;
        GlobalPos key = GlobalPos.create(world.getRegistryKey(), anchor);
        return ACTIVE_HEISTS.containsKey(key);
    }

    private void registerHeist() {
        if (this.getWorld().isClient() || this.treeAnchor == null || this.isRegistered) return;

        GlobalPos key = GlobalPos.create(this.getWorld().getRegistryKey(), this.treeAnchor);
        // Put ID in the map. If it overwrites (collision), last one wins
        ACTIVE_HEISTS.put(key, this.getId());
        this.isRegistered = true;

        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist-Registry] Registered heist for Tree {} (Entity {}). Active Heists: {}",
                    this.treeAnchor.toShortString(), this.getId(), ACTIVE_HEISTS.size());
        }
    }

    private void unregisterHeist() {
        if (this.getWorld().isClient() || this.treeAnchor == null || !this.isRegistered) return;

        GlobalPos key = GlobalPos.create(this.getWorld().getRegistryKey(), this.treeAnchor);
        // Only remove if WE are the one holding the lock
        if (ACTIVE_HEISTS.remove(key, this.getId())) {
            this.isRegistered = false;
            if (Configs.AHP.debugTreeDetection) {
                AdorableHamsterPets.LOGGER.info("[TreeHeist-Registry] Unregistered heist for Tree {} (Entity {}). Active Heists: {}",
                        this.treeAnchor.toShortString(), this.getId(), ACTIVE_HEISTS.size());
            }
        }
    }

    public BlockPos getTreeAnchor() {
        return this.treeAnchor;
    }

    public void initializeSearch(BlockPos startPos, TreeHeistUtil.TreeScanResult scanResult, NbtCompound originalHamsterNbt) {
        this.hamsterNbt = originalHamsterNbt;
        this.setPosition(startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5);

        // --- 1. Apply Pre-Calculated Scan Result ---
        this.validLeafPositions.clear();
        for (BlockPos pos : scanResult.validCanopyPositions()) {
            this.validLeafPositions.add(pos.asLong());
        }
        this.treeAnchor = scanResult.treeId();

        // Lock the tree immediately
        registerHeist();

        if (Configs.AHP.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist-Init] Searcher Entity Initialized. Anchor: {}. Canopy: {}.",
                    this.treeAnchor.toShortString(), this.validLeafPositions.size());
        }

        // If no canopy found (rare fallback), abort.
        if (this.validLeafPositions.isEmpty()) {
            if (Configs.AHP.debugTreeDetection) {
                AdorableHamsterPets.LOGGER.warn("[TreeHeist-Init] No valid leaves found. Aborting.");
            }
            popOut(false);
            return;
        }

        // --- 2. Debug Visualization ---
        if (Configs.AHP.debugTreeDetection) {
            TreeHeistUtil.spawnDebugParticles(this.getWorld(), scanResult);
            AdorableHamsterPets.LOGGER.info("[TreeHeist] Identified Tree ID: {} | Canopy Size: {}", this.treeAnchor, this.validLeafPositions.size());
        }

        // --- 3. Calculate Profitability & Depletion ---
        if (this.hamsterNbt.containsUuid("Owner")) {
            UUID ownerUuid = this.hamsterNbt.getUuid("Owner");
            PlayerEntity player = this.getWorld().getPlayerByUuid(ownerUuid);

            if (player instanceof PlayerEntityAccessor accessor) {
                // Get profitability based on the unique Tree ID
                this.dropChanceMultiplier = accessor.ahp$getHeistProfitability(this.treeAnchor);

                // Register this heist history immediately
                accessor.ahp$registerTreeHeist(this.treeAnchor);

                if (this.dropChanceMultiplier <= 0.01f) {
                    this.isExhausted = true;
                }

                // Send start message if not yet exhausted.
                if (!this.isExhausted) {
                    TreeHeistUtil.sendHeistStartMessage(player, this.dropChanceMultiplier);
                }

                // Trigger advancement
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ModCriteria.TREE_HEIST_STARTED.get().trigger(serverPlayer);
                }
            }
        }

        // --- 4. Calculate Duration & Perks ---
        int baseDuration = this.random.nextBetween(BASE_DURATION_MIN, BASE_DURATION_MAX);

        // Check for Acorn Hat in Slot 6
        this.hasAcornHat = false;
        if (this.hamsterNbt.contains("Inventory", NbtElement.COMPOUND_TYPE)) {
            NbtCompound invNbt = this.hamsterNbt.getCompound("Inventory");
            if (invNbt.contains("Items", NbtElement.LIST_TYPE)) {
                NbtList itemsList = invNbt.getList("Items", NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < itemsList.size(); ++i) {
                    NbtCompound itemTag = itemsList.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot == HamsterEntity.ACCESSORY_SLOT_INDEX) {
                        ItemStack stack = ItemStack.fromNbt(this.getRegistryManager(), itemTag).orElse(ItemStack.EMPTY);
                        if (stack.isOf(ModItems.ACORN_HAT.get())) {
                            this.hasAcornHat = true;
                            break;
                        }
                    }
                }
            }
        }

        // Duration multiplier logic (currently unused)
        // if (hasAcornHat) {
        //     baseDuration = (int) (baseDuration * DURATION_MULTIPLIER);
        // }

        // If exhausted, drastically reduce duration (20% of normal, min 60 ticks)
        if (this.isExhausted) {
            baseDuration = Math.max(60, (int)(baseDuration * 0.1f));
        }

        this.searchTimer = baseDuration;
        this.maxSearchDuration = baseDuration;
        this.validationTimer = VALIDATION_INTERVAL;

        // --- 5. Enhanced Debug Logging ---
        if (Configs.AHP.debugTreeDetection) {
            float baseChance = Configs.AHP.acornDropChance.get();
            float estimatedFinalChance = baseChance * this.dropChanceMultiplier;
            if (this.hasAcornHat) {
                estimatedFinalChance *= HAT_DROP_CHANCE_MULTIPLIER;
            }

            AdorableHamsterPets.LOGGER.info("""
                [TreeHeist-Stats] Heist Initialized:
                  - Tree Anchor: {}
                  - Base Profitability (History): {}%
                  - Acorn Hat Equipped: {}
                  - Hat Multiplier: {}x
                  - FINAL Drop Chance per Rummage: {}% (Base: {}%)
                """,
                    this.treeAnchor.toShortString(),
                    String.format("%.1f", this.dropChanceMultiplier * 100),
                    this.hasAcornHat,
                    this.hasAcornHat ? HAT_DROP_CHANCE_MULTIPLIER : 1.0f,
                    String.format("%.2f", estimatedFinalChance * 100),
                    String.format("%.2f", baseChance * 100)
            );
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        // 1. Ensure registration in the global map (in the case of a server restart)
        if (!this.isRegistered && this.treeAnchor != null) {
            registerHeist();
        }

        // 2. Safety check: if initialized with no leaves or data lost
        if (this.validLeafPositions.isEmpty()) {
            if (this.age > 5) discard();
            return;
        }

        // 3. Decrement drop cooldown
        if (this.dropCooldown > 0) {
            this.dropCooldown--;
        }

        // 4. Continuous Debug Visualization
        if (Configs.AHP.debugTreeDetection) {
            // Call the overload that accepts the raw data fields directly
            TreeHeistUtil.spawnDebugParticles(this.getWorld(), this.treeAnchor, this.validLeafPositions);
        }

        // 5. Validation (Tree Integrity)
        if (--this.validationTimer <= 0) {
            this.validationTimer = VALIDATION_INTERVAL;
            if (!validateTreeIntegrity()) {
                popOut(false); // Tree broken -> Abort
                return;
            }
        }

        // 6. Rummaging Behavior
        if (--this.rummageTimer <= 0) {
            this.rummageTimer = this.random.nextBetween(3, 5);
            rummage();
        }

        // 7. Timer
        if (--this.searchTimer <= 0) {
            popOut(true);
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        // Always attempt to release the tree lock when the entity is removed
        if (!this.getWorld().isClient()) {
            unregisterHeist();
        }
    }

    private boolean validateTreeIntegrity() {
        // Sample up to 3 random positions
        int samples = Math.min(3, this.validLeafPositions.size());
        int failures = 0;

        for (int i = 0; i < samples; i++) {
            long posLong = this.validLeafPositions.get(this.random.nextInt(this.validLeafPositions.size()));
            BlockPos pos = BlockPos.fromLong(posLong);
            if (!this.getWorld().getBlockState(pos).isOf(Blocks.OAK_LEAVES)) {
                failures++;
            }
        }

        // Fail if > 50% of samples are invalid (e.g. 2 out of 3)
        return failures <= (samples / 2);
    }

    private void rummage() {
        BlockPos currentPos = this.getBlockPos();
        BlockPos targetPos = null;

        // --- 1. Simulate Realistic Movement with Surface Bias ---
        List<BlockPos> nearbyExposed = new ArrayList<>();
        List<BlockPos> nearbyBuried = new ArrayList<>();

        for (Long posLong : this.validLeafPositions) {
            BlockPos p = BlockPos.fromLong(posLong);
            // Check distance (not self, and close enough to scurry to)
            if (!p.equals(currentPos) && p.getManhattanDistance(currentPos) <= 2) {
                if (isLeafExposed(this.getWorld(), p)) {
                    nearbyExposed.add(p);
                } else {
                    nearbyBuried.add(p);
                }
            }
        }

        // Priority 1: Nearby exposed leaves
        if (!nearbyExposed.isEmpty()) {
            targetPos = nearbyExposed.get(this.random.nextInt(nearbyExposed.size()));
        }
        // Priority 2: Nearby buried leaves
        else if (!nearbyBuried.isEmpty()) {
            targetPos = nearbyBuried.get(this.random.nextInt(nearbyBuried.size()));
        }
        // Priority 3: Any exposed leaves
        else {
            List<BlockPos> allExposed = new ArrayList<>();
            for (Long posLong : this.validLeafPositions) {
                BlockPos p = BlockPos.fromLong(posLong);
                if (isLeafExposed(this.getWorld(), p)) {
                    allExposed.add(p);
                }
            }

            if (!allExposed.isEmpty()) {
                targetPos = allExposed.get(this.random.nextInt(allExposed.size()));
            } else {
                // Fallback: If isolated or stuck, jump to random position to keep feature working
                long posLong = this.validLeafPositions.get(this.random.nextInt(this.validLeafPositions.size()));
                targetPos = BlockPos.fromLong(posLong);
            }
        }

        // Teleport entity (moves sound source)
        this.setPosition(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);

        // Effects
        ParticleEffectsUtil.spawnParticles(
                this.getWorld(),
                new Vec3d(this.getX(), this.getY(), this.getZ()),
                ModParticles.getForVariant(WoodVariant.BAMBOO), // BAMBOO variant for green leaves
                50,
                new Vec3d(0.4, 0.4, 0.4),
                0.0
        );

        ParticleEffectsUtil.spawnParticles(
                this.getWorld(),
                new Vec3d(this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5),
                ParticleTypes.WHITE_ASH,
                7,
                new Vec3d(0.4, 0.4, 0.4),
                0.1
        );

        // --- Acorn Tossing ---
        float dropChance = Configs.AHP.acornDropChance.get() * this.dropChanceMultiplier;

        // Apply Acorn Hat Buff Multiplier if equipped
        if (this.hasAcornHat) {
            dropChance *= HAT_DROP_CHANCE_MULTIPLIER;
        }

        if (this.random.nextFloat() < dropChance && this.dropCooldown <= 0) {
            // Calculate spawn position using static utility
            BlockPos spawnPos = TreeHeistUtil.findExitPosition(this.getWorld(), targetPos);
            ItemStack acornStack = new ItemStack(ModItems.ACORN.get());
            // Spawn near the upper center of the found block
            ItemEntity acornEntity = new ItemEntity(this.getWorld(), spawnPos.getX() + 0.5, spawnPos.getY() + 0.7, spawnPos.getZ() + 0.5, acornStack);

            // Random outward velocity
            double velX = (this.random.nextDouble() - 0.5) * 0.7;  // Slight sideways toss
            double velY = 0.0;
            double velZ = (this.random.nextDouble() - 0.5) * 0.7;  // Slight sideways toss
            acornEntity.setVelocity(velX, velY, velZ);

            // Play Sounds
            SoundEvent acornPopSound = ModSounds.getDynamicItemSound(acornStack);
            this.getWorld().playSound(null, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, acornPopSound, SoundCategory.NEUTRAL, 0.5f, 1.8f);
            this.getWorld().playSound(null, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, ModSounds.HAMSTER_DING.get(), SoundCategory.NEUTRAL, 0.7f, 1.0f + (this.random.nextFloat() - 0.5f) * 0.2f);

            this.getWorld().spawnEntity(acornEntity);

            // Set cooldown
            this.dropCooldown = 20;
        }
    }

    private boolean isLeafExposed(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (world.isAir(pos.offset(dir))) {
                return true;
            }
        }
        return false;
    }

    private void popOut(boolean success) {
        if (this.getWorld().isClient) return;
        ServerWorld serverWorld = (ServerWorld) this.getWorld();

        // 0. --- Release Tree Lock ---
        unregisterHeist();

        // 1. --- Calculate Exit Point ---
        BlockPos startPoint = this.getBlockPos();
        BlockPos exitPos = TreeHeistUtil.findExitPosition(this.getWorld(), startPoint);

        // --- 2. Respawn Hamster ---
        HamsterEntity newHamster = ModEntities.HAMSTER.get().create(serverWorld);
        if (newHamster != null) {
            // Restore state
            newHamster.readNbt(this.hamsterNbt); // Read full NBT to restore Owner/Variant/Attributes

            // Disable fall immunity so dynamic pitch/flying kick in immediately upon exit
            newHamster.setFallFlyImmunityTicks(0);

            // Set Position/Pitch
            newHamster.refreshPositionAndAngles(
                    exitPos.getX() + 0.5,
                    exitPos.getY() + 0.1,
                    exitPos.getZ() + 0.5,
                    this.random.nextFloat() * 360,
                    0
            );

            // Set Velocity & Flags
            newHamster.setVelocity(Vec3d.ZERO);
            newHamster.setThrown(false);
            newHamster.setKnockedOut(false);
            newHamster.setSitting(false);
            newHamster.velocityDirty = true;

            // Success logic
            if (success && !this.isExhausted) {
                // --- SUCCESS ---
                // Give an acorn if the area wasn't exhausted
                ItemStack prize = new ItemStack(ModItems.ACORN.get());
                newHamster.setInterestItemStack(prize);
                newHamster.setHoldingInterestItem(true);
                // Set the interest timer so the AI doesn't immediately drop the item.
                newHamster.setItemInterestTimer(1200); // 60 seconds
                // HamsterPlayWithItemGoal will automatically detect this and switch to RETURNING

                // Only play sparkle if successful
                SoundEvent sparkleSound = ModSounds.getRandomSoundFrom(ModSounds.DIAMOND_SPARKLE_SOUNDS, this.random);
                if (sparkleSound != null) {
                    serverWorld.playSound(null, newHamster.getBlockPos(), sparkleSound, SoundCategory.NEUTRAL, 0.5F, 1.0F);
                }

                // Schedule Celebration sound
                newHamster.scheduleTreeHeistCelebration();
            } else {
                // Failure logic
                newHamster.setSulking(true);
                newHamster.triggerAnimOnServer("mainController", "anim_hamster_sulk");

                // Send exhausted message & trigger deforestation advancement
                if (this.isExhausted && this.hamsterNbt.containsUuid("Owner")) {
                    UUID ownerUuid = this.hamsterNbt.getUuid("Owner");
                    PlayerEntity owner = serverWorld.getPlayerByUuid(ownerUuid);

                    if (owner != null) {
                        owner.sendMessage(Text.translatable("message.adorablehamsterpets.tree_heist_exhausted").formatted(Formatting.RED), true);
                        if (owner instanceof ServerPlayerEntity serverPlayer) {
                            ModCriteria.TREE_HEIST_DEPLETION.get().trigger(serverPlayer);
                        }
                    }
                }
            }

            serverWorld.spawnEntityAndPassengers(newHamster);

            // --- Shared Audio & Visuals ---
            // Trigger centralized pop effects at the source position (startPoint), not the spawn position.
            newHamster.triggerLeafPopEffects(startPoint, true);

            if (success && !this.isExhausted) {
                SoundEvent celebrateSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random);
                if (celebrateSound != null) {
                    serverWorld.playSound(null, newHamster.getBlockPos(), celebrateSound, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                }
            }
        }

        // Cleanup
        this.discard();
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("HamsterNBT", this.hamsterNbt);
        nbt.putInt("SearchTimer", this.searchTimer);
        nbt.putInt("MaxSearchDuration", this.maxSearchDuration);
        nbt.putInt("RummageTimer", this.rummageTimer);
        if (this.treeAnchor != null) {
            nbt.putLong("TreeAnchor", this.treeAnchor.asLong());
        }
        nbt.putBoolean("IsExhausted", this.isExhausted);
        nbt.putFloat("DropMultiplier", this.dropChanceMultiplier);
        nbt.putBoolean("HasAcornHat", this.hasAcornHat);
        nbt.putInt("DropCooldown", this.dropCooldown);

        NbtList posList = new NbtList();
        for (Long pos : this.validLeafPositions) {
            posList.add(NbtLong.of(pos));
        }
        nbt.put("ValidLeafPositions", posList);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.hamsterNbt = nbt.getCompound("HamsterNBT");
        this.searchTimer = nbt.getInt("SearchTimer");
        this.maxSearchDuration = nbt.getInt("MaxSearchDuration");
        this.rummageTimer = nbt.getInt("RummageTimer");
        if (nbt.contains("TreeAnchor")) {
            this.treeAnchor = BlockPos.fromLong(nbt.getLong("TreeAnchor"));
        }
        this.isExhausted = nbt.getBoolean("IsExhausted");
        this.dropChanceMultiplier = nbt.getFloat("DropMultiplier");
        this.hasAcornHat = nbt.getBoolean("HasAcornHat");
        this.dropCooldown = nbt.getInt("DropCooldown");

        this.validLeafPositions.clear();
        if (nbt.contains("ValidLeafPositions", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("ValidLeafPositions", NbtElement.LONG_TYPE);
            for (NbtElement element : list) {
                if (element instanceof NbtLong nbtLong) {
                    this.validLeafPositions.add(nbtLong.longValue());
                }
            }
        }

        // Reset validation timer on load
        this.validationTimer = VALIDATION_INTERVAL;
    }
}