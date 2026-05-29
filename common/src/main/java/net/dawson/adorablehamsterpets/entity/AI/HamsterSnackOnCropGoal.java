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
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
    private final World world;
    private State currentState = State.SCANNING_CROP;
    private boolean isFinished = false;

    private int checkTimer = 0;
    private int lungeTicks = 0;
    private int moveTimeout = 0;
    private Vec3d pounceStartPos;
    private BlockPos targetCrop;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterSnackOnCropGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        if (this.world.isClient() || !Configs.AHP.enableCropSnacking) return false;

        // Ensure hamster is tamed, wandering, and has completed its cooldown
        if (!this.hamster.isTamed() || !this.hamster.isWanderModeActive()) return false;
        if (this.hamster.cropSnackCooldownEndTick > this.world.getTime()) return false;

        // Exclusions
        if (this.hamster.isOnTheWayToBed()
                || this.hamster.isSitting()
                || this.hamster.isSleeping()
                || this.hamster.isKnockedOut()
                || this.hamster.isSulking()
                || this.hamster.isHoldingMouthItem()
                || this.hamster.isFrozenMovement()
                || this.hamster.isCelebratingBaby()
                || this.hamster.isCelebratingDiamond()
        ) {
            return false;
        }

        // Rate limit heavy block scanning
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = this.getTickCount(20);

        // Add RNG element to prevent immediate harvesting (scale denominator to match check frequency)
        int denominator = Math.max(1, Configs.AHP.cropSnackingChanceDenominator.get() / 20);
        if (this.hamster.getRandom().nextInt(denominator) != 0) {
            return false;
        }

        // Scan for nearest mature crop
        BlockPos nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(this.hamster.getBlockPos(), SEARCH_RADIUS, 2, SEARCH_RADIUS)) {
            BlockState state = this.world.getBlockState(pos);

            // evaluate if crop is config-valid and biologically mature
            if (ConfigDataCache.isSnackableCrop(state) && HamsterHarvestUtil.isMature(state)) {
                double distSq = this.hamster.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    nearest = pos.toImmutable();
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
    public boolean shouldContinue() {
        // Terminate here if finished
        if (this.isFinished) return false;

        if (this.hamster.isSitting() || this.hamster.isKnockedOut()) return false;

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
        this.hamster.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.currentState = State.MOVING_TO_CROP;
        this.moveTimeout = 0;
        this.hamster.getNavigation().startMovingTo(this.targetCrop.getX() + 0.5, this.targetCrop.getY(), this.targetCrop.getZ() + 0.5, 1.2D);
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
        this.hamster.cropSnackCooldownEndTick = this.world.getTime() + Configs.AHP.cropSnackCooldownTicks.get();
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

                if (this.hamster.getBlockPos().isWithinDistance(this.targetCrop, 1.5)) {
                    this.currentState = State.POUNCING_CROP;
                    this.lungeTicks = LUNGE_DURATION_TICKS;
                    this.pounceStartPos = this.hamster.getPos();
                    this.hamster.getNavigation().stop();
                    this.hamster.triggerAnimOnServer("mainController", "anim_hamster_pounce");
                } else if (this.hamster.getNavigation().isIdle()) {
                    // Try repathing if stuck, abort immediately if unreachable
                    boolean canPath = this.hamster.getNavigation().startMovingTo(this.targetCrop.getX() + 0.5, this.targetCrop.getY(), this.targetCrop.getZ() + 0.5, 1.0D);
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
                    Vec3d interpolatedPos = HamsterPhysicsUtil.calculatePouncePosition(this.pounceStartPos, Vec3d.ofBottomCenter(this.targetCrop), this.lungeTicks, LUNGE_DURATION_TICKS);
                    this.hamster.setPosition(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
                }

                if (this.lungeTicks == -1) {
                    // Break and replant
                    BlockState state = this.world.getBlockState(this.targetCrop);
                    HamsterHarvestUtil.harvestAndReplant((ServerWorld) this.world, this.targetCrop, state);

                    // Sound feedback
                    this.world.playSound(null, this.targetCrop, ModSounds.getDynamicBlockSound(state), SoundCategory.BLOCKS, 1.0f, 1.0f);
                    this.world.playSound(null, this.targetCrop, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.BLOCKS, 1.2f, 1.2f);

                    if (!this.world.isClient()) {
                        ParticleEffectsUtil.spawnParticles(
                                this.world,
                                Vec3d.ofCenter(this.targetCrop),
                                new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                                15,
                                new Vec3d(0.2, 0.2, 0.2),
                                0.05
                        );
                        ParticleEffectsUtil.spawnParticles(
                                this.world,
                                Vec3d.ofCenter(this.targetCrop),
                                ParticleTypes.POOF,
                                10,
                                new Vec3d(0.2, 0.2, 0.2),
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