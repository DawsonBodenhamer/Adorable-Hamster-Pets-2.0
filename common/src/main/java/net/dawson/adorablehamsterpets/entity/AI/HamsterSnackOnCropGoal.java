package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterHarvestUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterPhysicsUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import java.util.Optional;

/**
 * AI Goal allowing tamed, wandering hamsters to discover and harvest mature crops.
 */
public class HamsterSnackOnCropGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants & Enums
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int SEARCH_RADIUS = 6;
    private static final int LUNGE_DURATION_TICKS = 5;
    private static final int MAX_MOVE_TIMEOUT = 200; // 10 seconds

    private enum State {
        SCANNING_CROP,
        MOVING_TO_CROP,
        POUNCING_CROP
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private final Level world;
    private State currentState = State.SCANNING_CROP;
    private boolean isFinished = false;

    private int checkTimer = 0;
    private int lungeTicks = 0;
    private int moveTimeout = 0;
    private Vec3 pounceStartPos;
    private BlockPos targetCrop;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterSnackOnCropGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        if (this.world.isClientSide() || !Configs.AHP_MAIN.enableCropSnacking) return false;

        // Ensure hamster is tamed, wandering, and has completed its cooldown
        if (!this.hamster.isTame() || !this.hamster.isWanderModeActive()) return false;
        if (this.hamster.cropSnackCooldownEndTick > this.world.getGameTime()) return false;

        // Exclusions
        if (this.hamster.isOnTheWayToBed()
                || HamsterMovementUtil.shouldNotMove(this.hamster)
                || this.hamster.isHoldingMouthItem()
        ) {
            return false;
        }

        // Rate limit heavy block scanning
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = this.adjustedTickDelay(20);

        // Add RNG element to prevent immediate harvesting (scale denominator to match check frequency)
        int denominator = Math.max(1, Configs.AHP_MAIN.cropSnackingChanceDenominator.get() / 20);
        if (this.hamster.getRandom().nextInt(denominator) != 0) {
            return false;
        }

        // Scan for nearest mature crop
        BlockPos nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.withinManhattan(this.hamster.blockPosition(), SEARCH_RADIUS, 2, SEARCH_RADIUS)) {
            BlockState state = this.world.getBlockState(pos);

            // evaluate if crop is config-valid and biologically mature
            if (ConfigDataCache.isSnackableCrop(state) && HamsterHarvestUtil.isMature(state)) {
                double distSq = this.hamster.distanceToSqr(Vec3.atCenterOf(pos));
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    nearest = pos.immutable();
                }
            }
        }

        if (nearest != null) {
            this.targetCrop = nearest;
            this.isFinished = false; // Reset termination flag for new execution
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Terminate here if finished
        if (this.isFinished) return false;

        if (HamsterMovementUtil.shouldNotMove(this.hamster)) return false;

        // If scanning crops or moving to one, ensure it still exists and is mature
        if (this.currentState == State.MOVING_TO_CROP || this.currentState == State.POUNCING_CROP) {
            if (this.targetCrop == null) return false;

            // If crop has already been harvested, it's no longer mature
            // Allow animation to finish without terminating goal prematurely
            if (this.currentState == State.POUNCING_CROP && this.lungeTicks <= -1) {
                return true;
            }

            BlockState state = this.world.getBlockState(this.targetCrop);
            return ConfigDataCache.isSnackableCrop(state) && HamsterHarvestUtil.isMature(state);
        }

        return false; // Terminate if in any unexpected state
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
        this.hamster.setPathfindingMalus(PathType.WATER, 0.0F);
        this.currentState = State.MOVING_TO_CROP;
        this.moveTimeout = 0;
        this.hamster.getNavigation().moveTo(this.targetCrop.getX() + 0.5, this.targetCrop.getY(), this.targetCrop.getZ() + 0.5, 1.2D);
    }

    @Override
    public void stop() {
        this.hamster.getNavigation().stop();
        this.targetCrop = null;
        this.currentState = State.SCANNING_CROP;

        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }

        // Guarantee a cooldown whether harvest succeeded or failed/interrupted
        this.hamster.cropSnackCooldownEndTick = this.world.getGameTime() + Configs.AHP_MAIN.cropSnackCooldownTicks.get();
    }

    @Override
    public void tick() {
        switch (this.currentState) {
            case MOVING_TO_CROP -> {
                this.moveTimeout++;
                if (this.moveTimeout > MAX_MOVE_TIMEOUT) {
                    this.isFinished = true; // Give up if timed out
                    return;
                }

                HamsterMovementUtil.facePosition(this.hamster, this.targetCrop.getX() + 0.5, this.targetCrop.getY() + 0.5, this.targetCrop.getZ() + 0.5);

                if (this.hamster.blockPosition().closerThan(this.targetCrop, 1.5)) {
                    this.currentState = State.POUNCING_CROP;
                    this.lungeTicks = LUNGE_DURATION_TICKS;
                    this.pounceStartPos = this.hamster.position();
                    this.hamster.getNavigation().stop();
                    this.hamster.triggerAnimOnServer("mainController", "anim_hamster_pounce");
                } else if (this.hamster.getNavigation().isDone()) {
                    // Try repathing if stuck, abort immediately if unreachable
                    boolean canPath = this.hamster.getNavigation().moveTo(this.targetCrop.getX() + 0.5, this.targetCrop.getY(), this.targetCrop.getZ() + 0.5, 1.0D);
                    if (!canPath) {
                        this.isFinished = true;
                        return;
                    }
                }
            }
            case POUNCING_CROP -> {
                this.lungeTicks--;

                if (this.targetCrop != null && this.lungeTicks >= 0) {
                    HamsterMovementUtil.facePosition(this.hamster, this.targetCrop.getX() + 0.5, this.targetCrop.getY() + 0.5, this.targetCrop.getZ() + 0.5);
                }

                if (this.pounceStartPos != null && this.lungeTicks >= 0) {
                    Vec3 interpolatedPos = HamsterPhysicsUtil.calculatePouncePosition(this.pounceStartPos, Vec3.atBottomCenterOf(this.targetCrop), this.lungeTicks, LUNGE_DURATION_TICKS);
                    this.hamster.setPos(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
                }

                if (this.lungeTicks == -1) {
                    // Break and replant
                    BlockState state = this.world.getBlockState(this.targetCrop);
                    HamsterHarvestUtil.harvestAndReplant((ServerLevel) this.world, this.targetCrop, state);

                    // Sound feedback
                    this.world.playSound(null, this.targetCrop, ModSounds.getDynamicBlockSound(state), SoundSource.BLOCKS, 1.0f, 1.0f);
                    this.world.playSound(null, this.targetCrop, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.2f, 1.2f);

                    if (!this.world.isClientSide()) {
                        ParticleEffectsUtil.spawnParticles(
                                this.world,
                                Vec3.atCenterOf(this.targetCrop),
                                new BlockParticleOption(ParticleTypes.BLOCK, state),
                                15,
                                new Vec3(0.2, 0.2, 0.2),
                                0.05
                        );
                        ParticleEffectsUtil.spawnParticles(
                                this.world,
                                Vec3.atCenterOf(this.targetCrop),
                                ParticleTypes.POOF,
                                10,
                                new Vec3(0.2, 0.2, 0.2),
                                0.02
                        );
                    }
                    // Wait for 23-tick pounce animation to finish before moving
                } else if (this.lungeTicks < -18) {
                    this.isFinished = true; // Harvest complete, cleanly exit
                }
            }
        }
    }
}
