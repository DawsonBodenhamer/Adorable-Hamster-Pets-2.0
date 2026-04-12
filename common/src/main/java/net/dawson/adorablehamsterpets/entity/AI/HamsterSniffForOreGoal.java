package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class HamsterSniffForOreGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants & Enums
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int PATHING_RECHECK_INTERVAL = 20;

    private enum SeekingState {
        IDLE,
        SCANNING,
        MOVING_TO_ORE,
        WAITING_FOR_PATH,
        CELEBRATING_DIAMOND,
        SULKING_AT_GOLD
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Checks if an ore block is "exposed" by having at least one adjacent air-like block.
     *
     * @param orePos The position of the ore block.
     * @return True if the ore is exposed, false otherwise.
     */
    public static boolean isOreExposed(BlockPos orePos, World world) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = orePos.offset(direction);
            // Exposed if adjacent block has no collision shape
            if (world.getBlockState(adjacentPos).getCollisionShape(world, adjacentPos, ShapeContext.absent()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private final World world;

    private BlockPos targetOrePos;
    private boolean isSeekingDisappointingOre;
    private SeekingState currentState = SeekingState.IDLE;
    private int pathingTickTimer;

    @Nullable
    private Path path;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterSniffForOreGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        if (this.world.isClient || !Configs.AHP.enableIndependentDiamondSeeking) {
            return false;
        }

        if (!this.hamster.isPrimedToSeekDiamonds) {
            return false;
        }

        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isCelebratingRetrieval() || this.hamster.isSulking()) {
            return false;
        }

        // Block start if in combat
        if (this.hamster.getTarget() != null) {
            return false;
        }

        if (Configs.AHP.enableIndependentDiamondSeekCooldown && this.hamster.foundOreCooldownEndTick > this.world.getTime()) {
            return false;
        }

        return findNewTargetOreAndSetState();
    }

    @Override
    public boolean shouldContinue() {
        // Terminal states
        if (this.currentState == SeekingState.IDLE || this.currentState == SeekingState.CELEBRATING_DIAMOND || this.currentState == SeekingState.SULKING_AT_GOLD) {
            return false;
        }

        // Interruptions
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isSulking()) {
            return false;
        }

        // Combat interruption
        if (this.hamster.getTarget() != null) {
            return false;
        }

        if (this.targetOrePos == null) return false;

        BlockState targetState = this.world.getBlockState(this.targetOrePos);
        boolean isTargetCelebration = ConfigDataCache.isCelebrationOre(targetState);
        boolean isTargetSulking = ConfigDataCache.isSulkingOre(targetState);

        // Ensure the ore block hasn't been broken or changed
        if (this.isSeekingDisappointingOre) {
            return isTargetSulking;
        } else {
            return isTargetCelebration;
        }
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (this.isSeekingDisappointingOre ? "_Gold" : "_Diamond"));
        this.pathingTickTimer = 0;

        // currentState is already SCANNING from canStart/findNewTargetOreAndSetState
        attemptPathToTarget();
    }

    @Override
    public void stop() {
        this.path = null;
        this.hamster.getNavigation().stop();

        boolean targetOreStillExists = false;

        if (this.targetOrePos != null) {
            BlockState targetState = this.world.getBlockState(this.targetOrePos);
            boolean isTargetCelebration = ConfigDataCache.isCelebrationOre(targetState);
            boolean isTargetSulking = ConfigDataCache.isSulkingOre(targetState);

            if (this.isSeekingDisappointingOre && isTargetSulking) targetOreStillExists = true;
            if (!this.isSeekingDisappointingOre && isTargetCelebration) targetOreStillExists = true;
        }

        // Clear the primed flag if it didn't finish successfully or target broke
        if (this.currentState != SeekingState.CELEBRATING_DIAMOND && this.currentState != SeekingState.SULKING_AT_GOLD && !targetOreStillExists) {
            this.hamster.isPrimedToSeekDiamonds = false;
        }

        if (this.hamster.isCelebratingDiamond() && (this.currentState != SeekingState.CELEBRATING_DIAMOND || !targetOreStillExists)) {
            this.hamster.setCelebratingDiamond(false);
        }

        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }

        this.currentState = SeekingState.IDLE;
        this.targetOrePos = null;
    }

    @Override
    public void tick() {
        if (this.targetOrePos == null) {
            stop();
            return;
        }

        // Sync relative elevation for animation
        this.hamster.setOreTargetAbove(this.targetOrePos.getY() >= this.hamster.getY());

        // Fast turn speed towards target
        HamsterMovementUtil.facePosition(
                this.hamster,
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY() + 0.5,
                this.targetOrePos.getZ() + 0.5
        );

        switch (this.currentState) {
            case MOVING_TO_ORE -> {
                // Spawn particle breadcrumbs
                if (!this.world.isClient()) {
                    ParticleEffectsUtil.spawnBreadcrumbs((ServerWorld) this.world, this.path);
                }

                if (this.hamster.getNavigation().isIdle() || this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5)) {
                    // Only celebrate if hamster close and ore exposed
                    if (this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5) && isOreExposed(this.targetOrePos, this.world)) {
                        onOreReached();
                    } else {
                        // Clear old path and wait for re-evaluation
                        this.path = null;
                        this.currentState = SeekingState.WAITING_FOR_PATH;
                        this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
                    }
                }
            }
            case WAITING_FOR_PATH -> {
                if (this.pathingTickTimer > 0) {
                    this.pathingTickTimer--;
                } else {
                    attemptPathToTarget();
                }
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- 1. Target Evaluation ---
    private boolean findNewTargetOreAndSetState() {
        this.targetOrePos = null;
        this.isSeekingDisappointingOre = false;
        this.hamster.currentOreTarget = null;

        List<BlockPos> exposedCelebrationOres = new ArrayList<>();
        List<BlockPos> buriedCelebrationOres = new ArrayList<>();
        List<BlockPos> buriedSulkingOres = new ArrayList<>();
        int radius = Configs.AHP.diamondSeekRadius.get();

        for (BlockPos pos : BlockPos.iterateOutwards(this.hamster.getBlockPos(), radius, radius, radius)) {
            BlockState state = this.world.getBlockState(pos);

            if (ConfigDataCache.isCelebrationOre(state)) {
                if (isOreExposed(pos, this.world)) {
                    exposedCelebrationOres.add(pos.toImmutable());
                } else {
                    buriedCelebrationOres.add(pos.toImmutable());
                }
            } else if (ConfigDataCache.isSulkingOre(state)) {
                if (!isOreExposed(pos, this.world)) {
                    buriedSulkingOres.add(pos.toImmutable());
                }
            }
        }

        // Make mistake if applicable
        boolean targetIsSulkingOre = !buriedSulkingOres.isEmpty() && this.world.random.nextFloat() < Configs.AHP.goldMistakeChance.get();

        if (targetIsSulkingOre) {
            buriedSulkingOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(this.hamster.getPos())));
            this.targetOrePos = buriedSulkingOres.get(0);
            this.isSeekingDisappointingOre = true;
        } else {
            if (!exposedCelebrationOres.isEmpty()) {
                exposedCelebrationOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(this.hamster.getPos())));
                this.targetOrePos = exposedCelebrationOres.get(0);
            } else if (!buriedCelebrationOres.isEmpty()) {
                buriedCelebrationOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(this.hamster.getPos())));
                this.targetOrePos = buriedCelebrationOres.get(0);
            }
        }

        if (this.targetOrePos != null) {
            this.hamster.currentOreTarget = this.targetOrePos;
            this.currentState = SeekingState.SCANNING;
            return true;
        }

        return false;
    }

    private void attemptPathToTarget() {
        if (this.targetOrePos == null) {
            this.currentState = SeekingState.IDLE;
            return;
        }

        this.path = this.hamster.getNavigation().findPathTo(
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY(),
                this.targetOrePos.getZ() + 0.5,
                0
        );

        if (this.path != null) {
            this.hamster.getNavigation().startMovingAlong(this.path, 0.5D);
            this.currentState = SeekingState.MOVING_TO_ORE;
        } else {
            this.currentState = SeekingState.WAITING_FOR_PATH;
            this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
        }
    }

    // --- 2. Ore Reached Logic ---

    private void onOreReached() {
        this.hamster.getNavigation().stop();
        this.hamster.isPrimedToSeekDiamonds = false;

        if (Configs.AHP.enableIndependentDiamondSeekCooldown) {
            this.hamster.foundOreCooldownEndTick = this.world.getTime() + Configs.AHP.independentOreSeekCooldownTicks.get();
        }

        if (this.isSeekingDisappointingOre) {
            processGoldMistake();
        } else {
            processDiamondFind();
        }
    }

    private void processGoldMistake() {
        this.currentState = SeekingState.SULKING_AT_GOLD;

        if (this.hamster.getOwner() instanceof ServerPlayerEntity owner) {
            if (this.hamster.squaredDistanceTo(owner) < 36.0) {
                HamsterMovementUtil.faceEntity(this.hamster, owner);
            }

            // Output randomized snarky message for owner and trigger advancement
            MiscUtil.MessagingUtil.sendRandomizedSequentialMessage(
                    owner,
                    Identifier.of(AdorableHamsterPets.MOD_ID, "technical/hamster_found_gold_first_time"),
                    "message.adorablehamsterpets.found_gold_mistake",
                    7,
                    "gold_mistake_messages"
            );
            ModCriteria.HAMSTER_FOUND_GOLD.get().trigger(owner);
        }

        // Apply small backward and upward startled jump velocity
        Vec3d awayFromOre = this.hamster.getPos().subtract(Vec3d.ofCenter(this.targetOrePos)).normalize();
        this.hamster.setVelocity(awayFromOre.x * 0.1, 0.5, awayFromOre.z * 0.1);
        this.hamster.velocityDirty = true;

        SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, this.hamster.getRandom());
        if (bounceSound != null) {
            this.world.playSound(null, this.hamster.getBlockPos(), bounceSound, SoundCategory.NEUTRAL, 0.6f, this.hamster.getSoundPitch());
        }

        this.hamster.setSulking(true);
        this.hamster.triggerAnimOnServer("mainController", "anim_hamster_sulk");
    }

    private void processDiamondFind() {
        this.currentState = SeekingState.CELEBRATING_DIAMOND;
        this.hamster.setCelebratingDiamond(true);

        if (this.hamster.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
            ModCriteria.HAMSTER_LED_TO_DIAMOND.get().trigger(serverPlayerOwner, this.hamster, this.targetOrePos);
        }
    }
}