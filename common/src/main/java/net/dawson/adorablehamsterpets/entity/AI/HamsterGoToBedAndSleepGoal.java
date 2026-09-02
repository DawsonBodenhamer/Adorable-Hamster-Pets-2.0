package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterBedUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterPoseUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoBlockEntity;

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
    private final Level world;

    // State tracking
    private State currentState = State.MOVING_TO_BED;
    private int pounceTicks;
    @Nullable
    private Vec3 pounceStartPos;
    private int startDelay = 0;
    private boolean wasLured = false;
    private int awakeTimer = 0;
    private int pathfindingFailureCount = 0;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterGoToBedAndSleepGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Goal Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        // --- 1. Pre-Checks ---
        // Basic mode & config checks
        if (!this.hamster.isWanderModeActive()
                || HamsterMovementUtil.shouldNotMove(this.hamster)
                || !Configs.AHP_MAIN.allowSleepInBed.get()) {
            return false;
        }

        // Other checks
        if (this.hamster.isCelebratingBaby()) {
            return false;
        }

        Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
        if (bedPosOptional.isEmpty() || this.world.dimension() != bedPosOptional.get().dimension()) {
            return false;
        }

        BlockPos bedPos = bedPosOptional.get().pos();
        BlockState bedState = this.world.getBlockState(bedPos);
        BlockEntity be = this.world.getBlockEntity(bedPos);

        // Validate bed block state and entity type
        if (!(bedState.getBlock() instanceof HamsterBedBlock) || bedState.getValue(HamsterBedBlock.OCCUPIED) || !(be instanceof HamsterBedBlockEntity bedEntity)) {
            return false;
        }

        // Check specific bed permission
        if (!bedEntity.isSleepingAllowed()) {
            return false;
        }

        // --- 2. Lure Path ---
        // Ignore time of day and cooldowns if lured
        if (this.hamster.getLureToBedTimer() > 0) {
            return true;
        }

        // --- 3. Cooldown Check ---
        if (this.hamster.getGoToBedCooldown() > 0) {
            return false;
        }

        // --- 4. Timing Logic ---
        if (Configs.AHP_MAIN.circadianChaos.get()) {
            // Chaotic random sleep logic
            if (this.awakeTimer > 0) {
                this.awakeTimer--;
                return false;
            }
            return true;
        } else {
            // Standard day/night cycle check
            boolean isSleepTime = Configs.AHP_MAIN.sleepDuringDay.get() ? this.world.isBrightOutside() : this.world.isDarkOutside();
            return isSleepTime;
        }
    }

    @Override
    public boolean canContinueToUse() {
        // --- 1. Basic State Checks ---
        if (HamsterMovementUtil.shouldNotMove(this.hamster) || !this.hamster.isWanderModeActive()) {
            return false;
        }

        // --- 2. Other Checks ---
        if (this.hamster.isCelebratingBaby()) {
            return false;
        }

        // --- 3. Bed Validity Checks ---
        Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
        if (bedPosOptional.isEmpty() || this.world.dimension() != bedPosOptional.get().dimension()) {
            return false;
        }
        BlockPos bedPos = bedPosOptional.get().pos();
        BlockState bedState = this.world.getBlockState(bedPos);

        // Ensure bed still exists and isn't stolen
        if (!(bedState.getBlock() instanceof HamsterBedBlock) || bedState.getValue(HamsterBedBlock.OCCUPIED)) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
        this.hamster.setOnTheWayToBed(true);

        // Reset chaotic timer if enabled
        if (Configs.AHP_MAIN.circadianChaos.get()) {
            int min = Configs.AHP_MAIN.minNapInBedIntervalSeconds.get() * 20;
            int max = Configs.AHP_MAIN.maxNapInBedIntervalSeconds.get() * 20;
            this.awakeTimer = this.hamster.getRandom().nextIntBetweenInclusive(min, max);
        }

        boolean isLured = this.hamster.getLureToBedTimer() > 0;
        this.wasLured = isLured;
        boolean isNewBed = false;
        boolean shouldBypass = this.hamster.shouldBypassNextSleepDelay();

        // Check if bed is new
        Optional<GlobalPos> bedPosOpt = this.hamster.getLinkedBedPos();
        if (bedPosOpt.isPresent()) {
            BlockPos bedPos = bedPosOpt.get().pos();
            if (this.world.dimension() == bedPosOpt.get().dimension()) {
                BlockEntity be = this.world.getBlockEntity(bedPos);
                if (be instanceof HamsterBedBlockEntity bedEntity) {
                    isNewBed = bedEntity.isNewlyPlaced();
                }
            }
        }

        // --- Determine Delay ---
        // If lured, new bed, bypassed, or chaotic, skip the realistic hesitation
        if (isLured || isNewBed || shouldBypass || Configs.AHP_MAIN.circadianChaos.get()) {
            this.startDelay = 0;
            if (isLured) {
                this.hamster.setLureToBedTimer(0);
            }
            if (shouldBypass) {
                this.hamster.setBypassNextSleepDelay(false);
            }
        } else {
            this.startDelay = this.hamster.getRandom().nextIntBetweenInclusive(MIN_START_DELAY_TICKS, MAX_START_DELAY_TICKS);
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

        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
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

        // Start moving if idle
        if (this.currentState == State.MOVING_TO_BED && this.hamster.getNavigation().isDone()) {
            this.hamster.getNavigation().moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.75D);
        }

        // --- 2. Pathfinding & Stuck Check ---
        if (this.currentState == State.MOVING_TO_BED) {
            // Refresh path occasionally or if idle
            if (this.hamster.getNavigation().isDone() || this.hamster.tickCount % 20 == 0) {
                boolean success = this.hamster.getNavigation().moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.75D);

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
                this.hamster.getLookControl().setLookAt(Vec3.atCenterOf(bedPos));

                // Particle breadcrumbs
                if (this.wasLured && !this.world.isClientSide() && !this.hamster.getNavigation().isDone()) {
                    ParticleEffectsUtil.spawnBreadcrumbs(
                            (ServerLevel) this.world,
                            this.hamster.getNavigation().getPath(),
                            ParticleTypes.MYCELIUM,
                            1,
                            0.2,
                            0.0,
                            0.2,
                            3.0
                    );
                }

                if (this.hamster.getNavigation().isDone()) {
                    // Abort if stuck
                    stop();
                    return;
                }

                // Transition to pounce when close
                if (this.hamster.blockPosition().closerThan(bedPos, 1.2)) {
                    this.hamster.getNavigation().stop();
                    this.currentState = State.POUNCING_INTO_BED;
                    this.pounceTicks = 5;
                    this.pounceStartPos = this.hamster.position();

                    // Apply initial hop velocity
                    this.hamster.setDeltaMovement(this.hamster.getDeltaMovement().x, 0.4, this.hamster.getDeltaMovement().z);
                    this.hamster.needsSync = true;

                    this.hamster.suffocationGracePeriod = 40;

                    this.world.playSound(null, this.hamster.blockPosition(), ModSounds.HAMSTER_SWISH.get(), SoundSource.NEUTRAL, 0.35f, 1.0f + this.hamster.getRandom().nextFloat() * 0.5f);

                    // --- Select sleep pose based on personality ---
                    int personalityId = this.hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
                    String settleAnimId = HamsterPoseUtil.getSettleSleepAnimId(personalityId, false);
                    String deepSleepAnimIdForTracker = HamsterPoseUtil.getDeepSleepAnimId(personalityId);

                    this.hamster.getEntityData().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, deepSleepAnimIdForTracker);
                    this.hamster.triggerAnimOnServer("mainController", settleAnimId);
                }
                break;

            case POUNCING_INTO_BED:
                this.pounceTicks--;
                this.hamster.getLookControl().setLookAt(Vec3.atCenterOf(bedPos));

                if (this.pounceStartPos != null && this.pounceTicks >= 0) {
                    // Calc ease-in progress
                    double progress = 1.0 - ((double) this.pounceTicks / 5.0);
                    double easedProgress = progress * progress;
                    Vec3 targetCenter = Vec3.atCenterOf(bedPos).add(0, 0.1, 0);

                    // Interpolate position
                    double newX = pounceStartPos.x + easedProgress * (targetCenter.x - pounceStartPos.x);
                    double newZ = pounceStartPos.z + easedProgress * (targetCenter.z - pounceStartPos.z);
                    this.hamster.setPos(newX, this.hamster.getY(), newZ);
                }

                if (this.pounceTicks < 0) {
                    // --- Finalize Landing ---
                    // Set position slightly elevated inside the bed to prevent clipping through the floor
                    Vec3 targetCenter = Vec3.atCenterOf(bedPos).add(0, 0.1, 0);
                    this.hamster.setPos(targetCenter.x, targetCenter.y, targetCenter.z);
                    this.hamster.setDeltaMovement(Vec3.ZERO);
                    this.hamster.needsSync = true;

                    // Update hamster state
                    this.hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
                    this.hamster.setSleeping(true);
                    this.hamster.setInSittingPose(true);
                    this.world.setBlock(bedPos, this.world.getBlockState(bedPos).setValue(HamsterBedBlock.OCCUPIED, true), Block.UPDATE_ALL);

                    HamsterBedUtil.startNapTimer(this.hamster);

                    // Trigger bed anim
                    BlockEntity be = this.world.getBlockEntity(bedPos);
                    if (be instanceof GeoBlockEntity geoBlockEntity) {
                        geoBlockEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
                    }

                    HamsterBedUtil.startBedSleepEffects(hamster);

                    if (be instanceof HamsterBedBlockEntity bedEntity) {
                        bedEntity.markAsUsed();
                    }
                }
                break;
        }
    }
}
