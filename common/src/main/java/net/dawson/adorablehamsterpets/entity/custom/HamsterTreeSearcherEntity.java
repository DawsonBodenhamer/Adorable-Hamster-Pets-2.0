package net.dawson.adorablehamsterpets.entity.custom;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.UUIDUtil;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HamsterTreeSearcherEntity extends HamsterAbstractHiddenEntity {

    // --- Persistence Fields ---
    private final List<Long> validLeafPositions = new ArrayList<>();
    private int searchTimer;
    private int maxSearchDuration;
    private int validationTimer;
    private int rummageTimer;
    private float dropChanceMultiplier = 1.0f;
    private boolean isExhausted = false;
    private boolean hasAcornHat = false;
    private int dropCooldown = 0;

    // --- Constants ---
    private static final int VALIDATION_INTERVAL = 20; // Check tree integrity every second
    private static final int BASE_DURATION_MIN = 180;  // 9 sec
    private static final int BASE_DURATION_MAX = 280;  // 14 sec
    private static final float HAT_DROP_CHANCE_MULTIPLIER = 2.0f;

    public HamsterTreeSearcherEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public void initializeSearch(BlockPos startPos, TreeHeistUtil.TreeScanResult scanResult, CompoundTag originalHamsterNbt) {
        this.hamsterNbt = originalHamsterNbt;
        this.setPos(startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5);

        // --- 1. Apply Pre-Calculated Scan Result ---
        this.validLeafPositions.clear();
        for (BlockPos pos : scanResult.validCanopyPositions()) {
            this.validLeafPositions.add(pos.asLong());
        }
        this.anchorPos = scanResult.treeId();

        // Lock the tree immediately
        registerOccupancy();

        if (Configs.AHP_MAIN.debugTreeDetection) {
            AdorableHamsterPets.LOGGER.info("[TreeHeist-Init] Searcher Entity Initialized. Anchor: {}. Canopy: {}.",
                    this.anchorPos.toShortString(), this.validLeafPositions.size());
        }

        // If no canopy found (rare fallback), abort.
        if (this.validLeafPositions.isEmpty()) {
            if (Configs.AHP_MAIN.debugTreeDetection) {
                AdorableHamsterPets.LOGGER.warn("[TreeHeist-Init] No valid leaves found. Aborting.");
            }
            finishHeist(false);
            return;
        }

        // --- 2. Debug Visualization ---
        if (Configs.AHP_MAIN.debugTreeDetection) {
            TreeHeistUtil.spawnDebugParticles(this.level(), scanResult);
            AdorableHamsterPets.LOGGER.info("[TreeHeist] Identified Tree ID: {} | Canopy Size: {}", this.anchorPos, this.validLeafPositions.size());
        }

        // --- 3. Calculate Profitability & Depletion ---
        if (this.hamsterNbt.read("Owner", UUIDUtil.CODEC).isPresent()) {
            UUID ownerUuid = this.hamsterNbt.read("Owner", UUIDUtil.CODEC).orElse(null);
            Player player = this.level().getPlayerByUUID(ownerUuid);

            if (player instanceof PlayerEntityAccessor accessor) {
                // Get profitability based on the unique Tree ID
                this.dropChanceMultiplier = accessor.ahp$getHeistProfitability(this.anchorPos);

                // Register this heist history immediately
                accessor.ahp$registerTreeHeist(this.anchorPos);

                if (this.dropChanceMultiplier <= 0.01f) {
                    this.isExhausted = true;
                }

                // Send start message if not yet exhausted
                if (!this.isExhausted) {
                    TreeHeistUtil.sendHeistStartMessage(player, this.dropChanceMultiplier);
                }

                // Trigger advancement
                if (player instanceof ServerPlayer serverPlayer) {
                    ModCriteria.TREE_HEIST_STARTED.get().trigger(serverPlayer);
                }
            }
        }

        // --- 4. Calculate Duration & Perks ---
        int baseDuration = this.random.nextIntBetweenInclusive(BASE_DURATION_MIN, BASE_DURATION_MAX);

        // Check for Acorn Hat in Slot 6
        this.hasAcornHat = false;
        if (this.hamsterNbt.contains("Inventory")) {
            CompoundTag invNbt = this.hamsterNbt.getCompoundOrEmpty("Inventory");
            // 26.2 port: the inventory is a codec list now (see HamsterInventoryNbt), slots by index
            invNbt.read("Items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(items -> {
                if (items.size() > HamsterInventoryUtil.ACCESSORY_SLOT_INDEX
                        && items.get(HamsterInventoryUtil.ACCESSORY_SLOT_INDEX).is(ModItems.ACORN_HAT.get())) {
                    this.hasAcornHat = true;
                }
            });
        }

        // If exhausted, drastically reduce duration (20% of normal, min 60 ticks)
        if (this.isExhausted) {
            baseDuration = Math.max(60, (int)(baseDuration * 0.1f));
        }

        this.searchTimer = baseDuration;
        this.maxSearchDuration = baseDuration;
        this.validationTimer = VALIDATION_INTERVAL;

        // --- 5. Enhanced Debug Logging ---
        if (Configs.AHP_MAIN.debugTreeDetection) {
            float baseChance = Configs.AHP_MAIN.acornDropChance.get();
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
                    this.anchorPos.toShortString(),
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
        if (this.level().isClientSide()) return;

        // 1. Ensure registration in case of server restart
        if (!this.isRegistered && this.anchorPos != null) {
            registerOccupancy();
        }

        // 2. Safety check: if initialized with no leaves or data lost
        if (this.validLeafPositions.isEmpty()) {
            if (this.tickCount > 5) discard();
            return;
        }

        // 3. Decrement drop cooldown
        if (this.dropCooldown > 0) {
            this.dropCooldown--;
        }

        // 4. Continuous Debug Visualization
        if (Configs.AHP_MAIN.debugTreeDetection) {
            TreeHeistUtil.spawnDebugParticles(this.level(), this.anchorPos, this.validLeafPositions);
        }

        // 5. Validation (Tree Integrity)
        if (--this.validationTimer <= 0) {
            this.validationTimer = VALIDATION_INTERVAL;
            if (!validateTreeIntegrity()) {
                finishHeist(false); // Tree broken -> Abort
                return;
            }
        }

        // 6. Rummaging Behavior
        if (--this.rummageTimer <= 0) {
            this.rummageTimer = this.random.nextIntBetweenInclusive(3, 5);
            rummage();
        }

        // 7. Timer
        if (--this.searchTimer <= 0) {
            finishHeist(true);
        }
    }

    private boolean validateTreeIntegrity() {
        // Sample up to 3 random positions
        int samples = Math.min(3, this.validLeafPositions.size());
        int failures = 0;

        for (int i = 0; i < samples; i++) {
            long posLong = this.validLeafPositions.get(this.random.nextInt(this.validLeafPositions.size()));
            BlockPos pos = BlockPos.of(posLong);
            if (!ConfigDataCache.isHeistableLeaf(this.level().getBlockState(pos))) {
                failures++;
            }
        }

        // Fail if > 50% of samples are invalid (e.g. 2 out of 3)
        return failures <= (samples / 2);
    }

    private void rummage() {
        BlockPos currentPos = this.blockPosition();
        BlockPos targetPos = null;

        // --- 1. Simulate Realistic Movement with Surface Bias ---
        List<BlockPos> nearbyExposed = new ArrayList<>();
        List<BlockPos> nearbyBuried = new ArrayList<>();

        for (Long posLong : this.validLeafPositions) {
            BlockPos p = BlockPos.of(posLong);
            // Check distance (not self, and close enough to scurry to)
            if (!p.equals(currentPos) && p.distManhattan(currentPos) <= 2) {
                if (isLeafExposed(this.level(), p)) {
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
                BlockPos p = BlockPos.of(posLong);
                if (isLeafExposed(this.level(), p)) {
                    allExposed.add(p);
                }
            }

            if (!allExposed.isEmpty()) {
                targetPos = allExposed.get(this.random.nextInt(allExposed.size()));
            } else {
                // Fallback: If isolated or stuck, jump to random position to keep feature working
                long posLong = this.validLeafPositions.get(this.random.nextInt(this.validLeafPositions.size()));
                targetPos = BlockPos.of(posLong);
            }
        }

        // Teleport entity (moves sound source)
        this.setPos(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);

        // Effects
        ParticleEffectsUtil.spawnParticles(
                this.level(),
                new Vec3(this.getX(), this.getY(), this.getZ()),
                ModParticles.getForVariant(WoodVariant.BAMBOO), // BAMBOO variant for green leaves
                50,
                new Vec3(0.4, 0.4, 0.4),
                0.0
        );

        ParticleEffectsUtil.spawnParticles(
                this.level(),
                new Vec3(this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5),
                ParticleTypes.WHITE_ASH,
                7,
                new Vec3(0.4, 0.4, 0.4),
                0.1
        );

        // --- Acorn Tossing ---
        float dropChance = Configs.AHP_MAIN.acornDropChance.get() * this.dropChanceMultiplier;

        // Apply Acorn Hat Buff Multiplier if equipped
        if (this.hasAcornHat) {
            dropChance *= HAT_DROP_CHANCE_MULTIPLIER;
        }

        if (this.random.nextFloat() < dropChance && this.dropCooldown <= 0) {
            // Calculate spawn position using static utility
            BlockPos spawnPos = TreeHeistUtil.findExitPosition(this.level(), targetPos);
            ItemStack acornStack = new ItemStack(ModItems.ACORN.get());
            // Spawn near the upper center of the found block
            ItemEntity acornEntity = new ItemEntity(this.level(), spawnPos.getX() + 0.5, spawnPos.getY() + 0.7, spawnPos.getZ() + 0.5, acornStack);

            // Random outward velocity
            double velX = (this.random.nextDouble() - 0.5) * 0.7;  // Slight sideways toss
            double velY = 0.0;
            double velZ = (this.random.nextDouble() - 0.5) * 0.7;  // Slight sideways toss
            acornEntity.setDeltaMovement(velX, velY, velZ);

            // Feedback
            SoundEvent acornPopSound = ModSounds.getDynamicItemSound(acornStack);
            float baseVol = ModSounds.getDynamicSoundVolume(acornPopSound);
            this.level().playSound(null, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, acornPopSound, SoundSource.NEUTRAL, baseVol * 0.5f, 1.8f);
            this.level().playSound(null, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, ModSounds.HAMSTER_DING.get(), SoundSource.NEUTRAL, 0.7f, 1.0f + (this.random.nextFloat() - 0.5f) * 0.2f);

            this.level().addFreshEntity(acornEntity);

            // Set cooldown
            this.dropCooldown = 20;
        }
    }

    private boolean isLeafExposed(Level world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (world.isEmptyBlock(pos.relative(dir))) {
                return true;
            }
        }
        return false;
    }

    private void finishHeist(boolean success) {
        if (this.level().isClientSide()) return;
        ServerLevel serverWorld = (ServerLevel) this.level();

        BlockPos effectPos = (this.forcedExitPos != null && this.validLeafPositions.contains(this.forcedExitPos.asLong())) ? this.forcedExitPos : this.blockPosition();

        HamsterEntity newHamster = this.popOut(success && !this.isExhausted);

        if (newHamster != null) {
            if (success && !this.isExhausted) {
                // --- Success Logic ---
                // Give an acorn if the area wasn't exhausted
                ItemStack prize = new ItemStack(ModItems.ACORN.get());
                newHamster.setMouthItemStack(prize);
                newHamster.setHoldingMouthItem(true);
                // Set interest timer so AI doesn't immediately drop item
                // HamsterPlayWithItemGoal will automatically detect this and switch to RETURNING
                newHamster.setGenericInteractionTimer(1200); // 60 sec

                // Only play sparkle if successful
                SoundEvent sparkleSound = ModSounds.getRandomSoundFrom(ModSounds.DIAMOND_SPARKLE_SOUNDS, this.random);
                if (sparkleSound != null) {
                    serverWorld.playSound(null, newHamster.blockPosition(), sparkleSound, SoundSource.NEUTRAL, 0.5F, 1.0F);
                }

                // Schedule Celebration sound
                newHamster.scheduleTreeHeistCelebration();
            } else {
                // --- Failure Logic ---
                newHamster.setSulking(true);
                newHamster.triggerAnimOnServer("mainController", "anim_hamster_sulk");

                // Send exhausted message & trigger deforestation advancement
                if (this.isExhausted && this.hamsterNbt.read("Owner", UUIDUtil.CODEC).isPresent()) {
                    UUID ownerUuid = this.hamsterNbt.read("Owner", UUIDUtil.CODEC).orElse(null);
                    Player owner = serverWorld.getPlayerByUUID(ownerUuid);

                    if (owner != null) {
                        owner.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.tree_heist_exhausted").withStyle(ChatFormatting.RED));
                        if (owner instanceof ServerPlayer serverPlayer) {
                            ModCriteria.TREE_HEIST_DEPLETION.get().trigger(serverPlayer);
                        }
                    }
                }
            }

            serverWorld.addFreshEntityWithPassengers(newHamster);

            // --- Shared Audio & Visuals ---
            // Trigger effects at the block the hamster exits from, not the one it spawns in
            newHamster.triggerLeafPopEffects(effectPos, true);

            if (success && !this.isExhausted) {
                SoundEvent celebrateSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random);
                if (celebrateSound != null) {
                    serverWorld.playSound(null, newHamster.blockPosition(), celebrateSound, SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
            }
        }

        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("SearchTimer", this.searchTimer);
        nbt.putInt("MaxSearchDuration", this.maxSearchDuration);
        nbt.putInt("RummageTimer", this.rummageTimer);
        nbt.putBoolean("IsExhausted", this.isExhausted);
        nbt.putFloat("DropMultiplier", this.dropChanceMultiplier);
        nbt.putBoolean("HasAcornHat", this.hasAcornHat);
        nbt.putInt("DropCooldown", this.dropCooldown);

        ListTag posList = new ListTag();
        for (Long pos : this.validLeafPositions) {
            posList.add(LongTag.valueOf(pos));
        }
        nbt.put("ValidLeafPositions", posList);
        out.store("AdorableHamsterPets.TreeSearcher", CompoundTag.CODEC, nbt);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        CompoundTag nbt = in.read("AdorableHamsterPets.TreeSearcher", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        this.searchTimer = nbt.getIntOr("SearchTimer", 0);
        this.maxSearchDuration = nbt.getIntOr("MaxSearchDuration", 0);
        this.rummageTimer = nbt.getIntOr("RummageTimer", 0);
        this.isExhausted = nbt.getBooleanOr("IsExhausted", false);
        this.dropChanceMultiplier = nbt.getFloatOr("DropMultiplier", 0.0F);
        this.hasAcornHat = nbt.getBooleanOr("HasAcornHat", false);
        this.dropCooldown = nbt.getIntOr("DropCooldown", 0);

        this.validLeafPositions.clear();
        if (nbt.contains("ValidLeafPositions")) {
            ListTag list = nbt.getListOrEmpty("ValidLeafPositions");
            for (Tag element : list) {
                if (element instanceof LongTag nbtLong) {
                    this.validLeafPositions.add(nbtLong.asLong().orElse(0L));
                }
            }
        }
        this.validationTimer = VALIDATION_INTERVAL;
    }
}