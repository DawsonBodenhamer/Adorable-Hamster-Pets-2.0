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
    private final HamsterEntity hamster;
    private final World world;
    private int pounceTicks;
    @Nullable
    private Vec3d pounceStartPos;
    private int startDelay = 0;
    private boolean wasLured = false;
    private int awakeTimer = 0;

    private static final int MIN_START_DELAY_TICKS = 5;
    private static final int MAX_START_DELAY_TICKS = 100;

    private enum State {
        MOVING_TO_BED,
        POUNCING_INTO_BED
    }

    private State currentState = State.MOVING_TO_BED;

    public HamsterGoToBedAndSleepGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        // --- 1. Pre-checks for any sleep attempt ---
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
        if (!(bedState.getBlock() instanceof HamsterBedBlock) || bedState.get(HamsterBedBlock.OCCUPIED) || !(be instanceof HamsterBedBlockEntity bedEntity)) {
            return false;
        }

        // Check if sleeping is allowed on this specific bed
        if (!bedEntity.isSleepingAllowed()) {
            return false;
        }

        // --- 2. Lure Path (Ignores time of day and cooldowns) ---
        if (this.hamster.getLureToBedTimer() > 0) {
            return true; // Lure is active, all basic checks passed.
        }

        // --- 3. Automatic Path (Checks time of day or random timer) ---
        if (bedEntity.isNewlyPlaced()) {
            return true; // Bypass cooldown and time checks if bed was just placed
        }

        if (this.hamster.getGoToBedCooldown() > 0) {
            return false;
        }

        // --- 4a. Circadian Chaos Logic ---
        if (Configs.AHP.circadianChaos.get()) {
            if (this.awakeTimer > 0) {
                this.awakeTimer--;
                return false;
            }
            return true; // Cooldown is over, time for a random nap
        } else {
            // --- 4b. Time-of-Day Logic ---
            boolean isSleepTime = Configs.AHP.sleepDuringDay.get() ? this.world.isDay() : this.world.isNight();
            return isSleepTime;
        }
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
        this.hamster.setOnTheWayToBed(true); // Set the flag for the debug overlay

        // Reset random sleep cooldown if Circadian Chaos is enabled
        if (Configs.AHP.circadianChaos.get()) {
            int min = Configs.AHP.minNapInBedIntervalSeconds .get() * 20;
            int max = Configs.AHP.maxNapInBedIntervalSeconds.get() * 20;
            this.awakeTimer = this.hamster.getRandom().nextBetween(min, max);
        }

        boolean isLured = this.hamster.getLureToBedTimer() > 0;
        this.wasLured = isLured;
        boolean isNewBed = false;
        boolean shouldBypass = this.hamster.shouldBypassNextSleepDelay();

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

        // If lured to bed, bed lock was just placed, manually woken up, or Circadian Chaos is enabled, bypass the "realism" delay.
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

        this.hamster.setGoToBedDelayTicks(this.startDelay); // Update for the debug overlay
        this.currentState = State.MOVING_TO_BED; // Start in idle, wait for delay
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
        this.hamster.setOnTheWayToBed(false); // Clear the flag on the debug overlay
        this.hamster.setGoToBedDelayTicks(this.startDelay); // Reset delay for the debug overlay
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Basic state checks ---
        if (this.hamster.isSitting() || !this.hamster.isWanderModeActive()) {
            return false;
        }

        // --- 2. Bed validity checks ---
        Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
        if (bedPosOptional.isEmpty() || this.world.getRegistryKey() != bedPosOptional.get().dimension()) {
            return false;
        }
        BlockPos bedPos = bedPosOptional.get().pos();
        BlockState bedState = this.world.getBlockState(bedPos);
        if (!(bedState.getBlock() instanceof HamsterBedBlock) || bedState.get(HamsterBedBlock.OCCUPIED)) {
            return false;
        }

        // --- 3. If all checks pass, continue the goal ---
        return true;
    }

    @Override
    public void tick() {
        if (startDelay > 0) {
            startDelay--;
            this.hamster.setGoToBedDelayTicks(startDelay); // Continuously update for the debug overlay
            return; // Wait for delay to finish
        }

        Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
        if (bedPosOptional.isEmpty()) {
            stop();
            return;
        }
        BlockPos bedPos = bedPosOptional.get().pos();

        // This block now runs only after the delay is over.
        // If we are in the moving state but the navigator is idle, it means we need to start it.
        if (this.currentState == State.MOVING_TO_BED && this.hamster.getNavigation().isIdle()) {
            this.hamster.getNavigation().startMovingTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.75D);
        }

        switch (this.currentState) {
            case MOVING_TO_BED:
                this.hamster.getLookControl().lookAt(Vec3d.ofCenter(bedPos));

                // Particle Breadcrumb Logic for Lure
                if (this.wasLured && !this.world.isClient() && !this.hamster.getNavigation().isIdle()) {
                    ParticleBreadcrumbHelper.spawnBreadcrumbs((ServerWorld) this.world, this.hamster.getNavigation().getCurrentPath());
                }

                if (this.hamster.getNavigation().isIdle()) {
                    // If navigation becomes idle before reaching the target (e.g., stuck), stop the goal to allow re-evaluation.
                    stop();
                    return;
                }

                // When close enough, transition to pouncing.
                if (this.hamster.getBlockPos().isWithinDistance(bedPos, 1.2)) {
                    this.hamster.getNavigation().stop();
                    this.currentState = State.POUNCING_INTO_BED;
                    this.pounceTicks = 5; // 0.25 seconds for the pounce
                    this.pounceStartPos = this.hamster.getPos();

                    // --- Pounce Arc ---
                    // Apply an initial upward velocity to create a "hop" into the bed.
                    this.hamster.setVelocity(this.hamster.getVelocity().x, 0.4, this.hamster.getVelocity().z);
                    this.hamster.velocityDirty = true; // Client sync

                    // --- Set Suffocation Grace Period ---
                    this.hamster.suffocationGracePeriod = 40;

                    // --- Play Sound ---
                    this.world.playSound(null, this.hamster.getBlockPos(), ModSounds.HAMSTER_SWISH.get(), SoundCategory.NEUTRAL, 0.35f, 1.0f + this.hamster.getRandom().nextFloat() * 0.5f);

                    // --- Randomized Animation Logic ---
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
                    // Calculate progress (from 0.0 to 1.0 over the pounce duration)
                    double progress = 1.0 - ((double) this.pounceTicks / 5.0);
                    // Apply a quadratic ease-in curve for acceleration
                    double easedProgress = progress * progress;
                    Vec3d targetCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);

                    // Interpolate X and Z coordinates; let gravity handle Y.
                    double newX = pounceStartPos.x + easedProgress * (targetCenter.x - pounceStartPos.x);
                    double newZ = pounceStartPos.z + easedProgress * (targetCenter.z - pounceStartPos.z);
                    this.hamster.setPosition(newX, this.hamster.getY(), newZ);
                }

                if (this.pounceTicks < 0) {
                    // --- Finalize Landing ---
                    // Set the hamster's final position to be slightly elevated inside the bed, preventing it from clipping through the floor on landing.
                    Vec3d targetCenter = Vec3d.ofCenter(bedPos).add(0, 0.1, 0);
                    this.hamster.setPosition(targetCenter.x, targetCenter.y, targetCenter.z);
                    this.hamster.setVelocity(Vec3d.ZERO); // Stop all movement
                    this.hamster.velocityDirty = true;

                    // Finalize state after pounce
                    this.hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
                    this.hamster.setSleeping(true);
                    this.hamster.setInSittingPose(true); // Vanilla flag to prevent other AI movement
                    this.world.setBlockState(bedPos, this.world.getBlockState(bedPos).with(HamsterBedBlock.OCCUPIED, true), Block.NOTIFY_ALL);

                    // Start the nap timer on the hamster
                    this.hamster.startNapTimer();

                    // Trigger bed animation
                    BlockEntity be = this.world.getBlockEntity(bedPos);
                    if (be instanceof GeoBlockEntity geoBlockEntity) {
                        geoBlockEntity.triggerAnim("hamster_bed_controller", "anim_bed_becoming_occupied");
                    }

                    // Call the effects helper method
                    this.hamster.startBedSleepEffects();

                    // Mark the bed as used to ensure the cooldown comes into effect the next time
                    if (be instanceof HamsterBedBlockEntity bedEntity) {
                        bedEntity.markAsUsed();
                    }
                }
                break;
        }
    }
}