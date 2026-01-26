package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.ParticleBreadcrumbHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;

import java.util.EnumSet;
import java.util.Optional;

public class HamsterGoToBedAndSleepGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants & Enums
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int MIN_START_DELAY_TICKS = 5;
    private static final int MAX_START_DELAY_TICKS = 100;

    private enum State {
        MOVING_TO_BED,
        POUNCING_INTO_BED
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private final World world;

    // State tracking
    private State currentState = State.MOVING_TO_BED;
    private int pounceTicks;
    @Nullable
    private Vec3d pounceStartPos;
    private int startDelay = 0;
    private boolean wasLured = false;
    private int awakeTimer = 0;
    private int pathfindingFailureCount = 0;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterGoToBedAndSleepGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Goal Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        // --- 1. Pre-Checks ---
        // basic mode & config checks
        if (!this.hamster.isWanderModeActive() || this.hamster.isSitting() || !Configs.AHP.allowSleepInBed.get()) {
            return false;
        }

        Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
        if (bedPosOptional.isEmpty() || this.world.getRegistryKey() != bedPosOptional.get().dimension()) {
            return false;
        }

        BlockPos bedPos = bedPosOptional.get().pos();
        BlockState bedState = this.world.getBlockState(bedPos);
        BlockEntity be = this.world.getBlockEntity(bedPos);

        // validate bed block state and entity type
        if (!(bedState.getBlock() instanceof HamsterBedBlock) || bedState.get(HamsterBedBlock.OCCUPIED) || !(be instanceof HamsterBedBlockEntity bedEntity)) {
            return false;
        }

        // check specific bed permission
        if (!bedEntity.isSleepingAllowed()) {
            return false;
        }

        // --- 2. Lure Path ---
        // ignore time of day and cooldowns if lured
        if (this.hamster.getLureToBedTimer() > 0) {
            return true;
        }

        // --- 3. Automatic Path ---
        // bypass cooldown if bed is fresh
        if (bedEntity.isNewlyPlaced()) {
            return true;
        }

        if (this.hamster.getGoToBedCooldown() > 0) {
            return false;
        }

        // --- 4. Timing Logic ---
        if (Configs.AHP.circadianChaos.get()) {
            // chaotic random sleep logic
            if (this.awakeTimer > 0) {
                this.awakeTimer--;
                return false;
            }
            return true;
        } else {
            // standard day/night cycle check
            boolean isSleepTime = Configs.AHP.sleepDuringDay.get() ? this.world.isDay() : this.world.isNight();
            return isSleepTime;
        }
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Basic State Checks ---
        if (this.hamster.isSitting() || !this.hamster.isWanderModeActive()) {
            return false;
        }

        // --- 2. Bed Validity Checks ---
        Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
        if (bedPosOptional.isEmpty() || this.world.getRegistryKey() != bedPosOptional.get().dimension()) {
            return false;
        }
        BlockPos bedPos = bedPosOptional.get().pos();
        BlockState bedState = this.world.getBlockState(bedPos);

        // ensure bed still exists and isn't stolen
        if (!(bedState.getBlock() instanceof HamsterBedBlock) || bedState.get(HamsterBedBlock.OCCUPIED)) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
        this.hamster.setOnTheWayToBed(true);

        // reset chaotic timer if enabled
        if (Configs.AHP.circadianChaos.get()) {
            int min = Configs.AHP.minNapInBedIntervalSeconds.get() * 20;
            int max = Configs.AHP.maxNapInBedIntervalSeconds.get() * 20;
            this.awakeTimer = this.hamster.getRandom().nextBetween(min, max);
        }

        boolean isLured = this.hamster.getLureToBedTimer() > 0;
        this.wasLured = isLured;
        boolean isNewBed = false;
        boolean shouldBypass = this.hamster.shouldBypassNextSleepDelay();

        // check if bed is new
        Optional<GlobalPos> bedPosOpt = this.hamster.getLinkedBedPos();
        if (bedPosOpt.isPresent()) {
            BlockPos bedPos = bedPosOpt.get().pos();
            if (this.world.getRegistryKey() == bedPosOpt.get().dimension()) {
                BlockEntity be = this.world.getBlockEntity(bedPos);
                if (be instanceof HamsterBedBlockEntity bedEntity) {
                    isNewBed = bedEntity.isNewlyPlaced();
                }
            }
        }

        // --- Determine Delay ---
        // if lured, new bed, bypassed, or chaotic, skip the realistic hesitation
        if (isLured || isNewBed || shouldBypass || Configs.AHP.circadianChaos.get()) {
            this.startDelay = 0;
            if (isLured) {
                this.hamster.setLureToBedTimer(0);
            }
            if (shouldBypass) {
                this.hamster.setBypassNextSleepDelay(false);
            }
        } else {
            this.startDelay = this.hamster.getRandom().nextBetween(MIN_START_DELAY_TICKS, MAX_START_DELAY_TICKS);
        }

        this.hamster.setGoToBedDelayTicks(this.startDelay);
        this.currentState = State.MOVING_TO_BED;
    }

    @Override
    public void stop() {
        this.hamster.getNavigation().stop();
        this.currentState = State.MOVING_TO_BED;
        this.pounceStartPos = null;
        this.wasLured = false;

        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }

        this.hamster.setOnTheWayToBed(false);
        this.hamster.setGoToBedDelayTicks(this.startDelay);
    }

    @Override
    public void tick() {
        // --- 1. Handle Start Delay ---
        if (startDelay > 0) {
            startDelay--;
            this.hamster.setGoToBedDelayTicks(startDelay);
            return;
        }

        Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
        if (bedPosOptional.isEmpty()) {
            stop();
            return;
        }
        BlockPos bedPos = bedPosOptional.get().pos();

        // start moving if idle
        if (this.currentState == State.MOVING_TO_BED && this.hamster.getNavigation().isIdle()) {
            this.hamster.getNavigation().startMovingTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.75D);
        }

        // --- 2. Pathfinding & Stuck Check ---
        if (this.currentState == State.MOVING_TO_BED) {
            // refresh path occasionally or if idle
            if (this.hamster.getNavigation().isIdle() || this.hamster.age % 20 == 0) {
                boolean success = this.hamster.getNavigation().startMovingTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.75D);

                if (!success) {
                    this.pathfindingFailureCount++;
                    // 5 failures is approx 5 seconds
                    if (this.pathfindingFailureCount > 5 && !this.hamster.isStuckSearchingForBed()) {
                        this.hamster.setStuckSearchingForBed(true);
                    }
                } else {
                    this.pathfindingFailureCount = 0;
                    if (this.hamster.isStuckSearchingForBed()) {
                        this.hamster.setStuckSearchingForBed(false);
                    }
                }
            }
        }

        // --- 3. State Machine ---
        switch (this.currentState) {
            case MOVING_TO_BED:
                this.hamster.getLookControl().lookAt(Vec3d.ofCenter(bedPos));

                // particle breadcrumbs for debugging/visuals
                if (this.wasLured && !this.world.isClient() && !this.hamster.getNavigation().isIdle()) {
                    ParticleBreadcrumbHelper.spawnBreadcrumbs((ServerWorld) this.world, this.hamster.getNavigation().getCurrentPath());
                }

                if (this.hamster.getNavigation().isIdle()) {
                    // abort if stuck
                    stop();
                    return;
                }

                // transition to pounce when close
                if (this.hamster.getBlockPos().isWithinDistance(bedPos, 1.2)) {
                    this.hamster.getNavigation().stop();
                    this.currentState = State.POUNCING_INTO_BED;
                    this.pounceTicks = 5;
                    this.pounceStartPos = this.hamster.getPos();

                    // apply initial hop velocity
                    this.hamster.setVelocity(this.hamster.getVelocity().x, 0.4, this.hamster.getVelocity().z);
                    this.hamster.velocityDirty = true;

                    this.hamster.suffocationGracePeriod = 40;

                    this.world.playSound(null, this.hamster.getBlockPos(), ModSounds.HAMSTER_SWISH.get(), SoundCategory.NEUTRAL, 0.35f, 1.0f + this.hamster.getRandom().nextFloat() * 0.5f);

                    // select random sleep pose
                    int choice = this.hamster.getRandom().nextInt(3);
                    String settleAnimId;
                    String deepSleepAnimIdForTracker;
                    switch (choice) {
                        case 0 -> {
                            settleAnimId = "anim_hamster_stand_settle_sleep1";
                            deepSleepAnimIdForTracker = "anim_hamster_sleep_pose1";
                        }
                        case 1 -> {
                            settleAnimId = "anim_hamster_stand_settle_sleep2";
                            deepSleepAnimIdForTracker = "anim_hamster_sleep_pose2";
                        }
                        default -> {
                            settleAnimId = "anim_hamster_stand_settle_sleep3";
                            deepSleepAnimIdForTracker = "anim_hamster_sleep_pose3";
                        }
                    }
                    this.hamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, deepSleepAnimIdForTracker);
                    this.hamster.triggerAnimOnServer("mainController", settleAnimId);
                }
                break;

            case POUNCING_INTO_BED:
                this.pounceTicks--;
                this.hamster.getLookControl().lookAt(Vec3d.ofCenter(bedPos));

                if (this.pounceStartPos != null && this.pounceTicks >= 0) {
                    // calc ease-in progress
                    double progress = 1.0 - ((double) this.pounceTicks / 5.0);
                    double easedProgress = progress * progress;
                    Vec3d targetCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);

                    // interpolate position
                    double newX = pounceStartPos.x + easedProgress * (targetCenter.x - pounceStartPos.x);
                    double newZ = pounceStartPos.z + easedProgress * (targetCenter.z - pounceStartPos.z);
                    this.hamster.setPosition(newX, this.hamster.getY(), newZ);
                }

                if (this.pounceTicks < 0) {
                    // --- Finalize Landing ---
                    // set position slightly elevated inside the bed to prevent clipping through the floor
                    Vec3d targetCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);
                    this.hamster.setPosition(targetCenter.x, targetCenter.y, targetCenter.z);
                    this.hamster.setVelocity(Vec3d.ZERO);
                    this.hamster.velocityDirty = true;

                    // update hamster state
                    this.hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
                    this.hamster.setSleeping(true);
                    this.hamster.setInSittingPose(true);
                    this.world.setBlockState(bedPos, this.world.getBlockState(bedPos).with(HamsterBedBlock.OCCUPIED, true), Block.NOTIFY_ALL);

                    this.hamster.startNapTimer();

                    // trigger bed anim
                    BlockEntity be = this.world.getBlockEntity(bedPos);
                    if (be instanceof GeoBlockEntity geoBlockEntity) {
                        geoBlockEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
                    }

                    this.hamster.startBedSleepEffects();

                    if (be instanceof HamsterBedBlockEntity bedEntity) {
                        bedEntity.markAsUsed();
                    }
                }
                break;
        }
    }
}