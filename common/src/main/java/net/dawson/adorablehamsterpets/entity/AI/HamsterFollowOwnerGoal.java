package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.FollowOwnerGoalAccessor;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class HamsterFollowOwnerGoal extends FollowOwnerGoal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double BUFFED_FOLLOW_SPEED = 1.5D;
    private static final double MINIMUM_WATER_PROGRESS = 0.5D;
    private static final int WATER_PROGRESS_SAMPLE_TICKS = 10;
    private static final int WATER_STUCK_THRESHOLD_TICKS = 40;
    private static final int WATER_RESCUE_RETRY_TICKS = 20;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private double lastHorizontalDistance = Double.NaN;
    private int waterProgressSampleTicks;
    private int waterRescueRetryTicks;
    private int waterStuckTicks;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterFollowOwnerGoal(HamsterEntity hamster, double speed, float minDistance, float maxDistance) {
        super(hamster, speed, minDistance, maxDistance);
        this.hamster = hamster;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        // --- Base Logic ---
        if (!super.canStart()) {
            return false;
        }

        // --- Parent Override ---
        // Abort if baby is tracking a living parent
        if (this.hamster.isBaby() && this.hamster.getParentUuid() != null) {
            if (this.hamster.getWorld() instanceof ServerWorld serverWorld) {
                Entity parent = serverWorld.getEntity(this.hamster.getParentUuid());
                if (parent instanceof HamsterEntity parentHamster && parentHamster.isAlive()) {
                    return false;
                }
            }
        }

        // --- State Exclusions ---
        if (HamsterMovementUtil.shouldNotFollow(this.hamster)) {
            return false;
        }

        // --- Distance Calculation ---
        // Recalculate minimum follow distance for certain states
        float minDist = ((FollowOwnerGoalAccessor) this).getMinDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();

        if (owner == null || this.hamster.cannotFollowOwner()) {
            return false;
        }

        if (this.hamster.hasGreenBeanBuff() || this.hamster.getAggressionState() == HamsterEntity.AggressionState.MENACE) {
            minDist += 5.0F;
        }

        return !(this.hamster.squaredDistanceTo(owner) < (double) (minDist * minDist));
    }

    @Override
    public boolean shouldContinue() {
        // --- State Exclusions ---
        if (this.isFollowRescueBlocked()) {
            this.resetWaterWatchdog();
            return false;
        }

        // --- Distance Calculation ---
        // Recalculate follow distances for certain states
        float minDist = ((FollowOwnerGoalAccessor) this).getMinDistance();
        float maxDist = ((FollowOwnerGoalAccessor) this).getMaxDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();

        if (owner == null) {
            return false;
        }

        if (this.hamster.hasGreenBeanBuff() || this.hamster.getAggressionState() == HamsterEntity.AggressionState.MENACE) {
            minDist += 5.0F;
            maxDist += 5.0F;
        }

        double ownerDistanceSquared = this.hamster.squaredDistanceTo(owner);
        boolean ordinaryFollowContinues =
                !this.hamster.getNavigation().isIdle()
                        && ownerDistanceSquared > (double) (maxDist * maxDist);
        boolean waterFollowContinues =
                this.hamster.isTouchingWater()
                        && ownerDistanceSquared > (double) (minDist * minDist);
        return ordinaryFollowContinues || waterFollowContinues;
    }

    @Override
    public void start() {
        super.start();
        this.resetWaterWatchdog();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner != null && this.hamster.isTouchingWater()) {
            this.lastHorizontalDistance = this.getHorizontalDistance(owner);
        }
        this.hamster.setActiveCustomGoalName(
                this.getClass().getSimpleName()
                        + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
    }

    @Override
    public void stop() {
        super.stop();
        this.resetWaterWatchdog();
        if (this.hamster.getActiveCustomGoalName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }

    @Override
    public void tick() {
        // --- Target Resolution ---
        FollowOwnerGoalAccessor accessor = (FollowOwnerGoalAccessor) this;
        LivingEntity owner = accessor.getOwner();

        if (owner == null) {
            this.resetWaterWatchdog();
            return;
        }

        boolean shouldTeleport = HamsterMovementUtil.shouldTeleportTo(this.hamster, owner);
        boolean waterRescueReady = this.tickWaterWatchdog(owner);

        // --- Facing Logic ---
        if (!shouldTeleport && !waterRescueReady) {
            HamsterMovementUtil.faceEntity(this.hamster, owner);
        }

        // --- Update Timer ---
        int currentTicks = accessor.getUpdateCountdownTicks() - 1;
        accessor.setUpdateCountdownTicks(currentTicks);

        if (currentTicks <= 0) {
            accessor.setUpdateCountdownTicks(this.getTickCount(10));

            // --- Movement Execution ---
            if (shouldTeleport || waterRescueReady) {
                HamsterMovementUtil.TeleportResult result =
                        HamsterMovementUtil.tryTeleportTo(this.hamster, owner, shouldTeleport);
                this.handleTeleportResult(result);
            } else {
                // Calculate base speed and apply a 50% reduction if they are currently busting a move
                double activeSpeed = this.hamster.hasGreenBeanBuff() ? BUFFED_FOLLOW_SPEED : accessor.getSpeed();
                if (this.hamster.isDancing()) {
                    activeSpeed *= 0.5;
                }

                if (this.hamster.hasGreenBeanBuff()) {
                    // Zoomies erratic pathfinding
                    Vec3d targetPos = FuzzyTargeting.findTo(this.hamster, 8, 5, Vec3d.ofCenter(owner.getBlockPos()));
                    if (targetPos != null) {
                        this.hamster.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, activeSpeed);
                    }
                } else {
                    // Standard pathfinding
                    this.hamster.getNavigation().startMovingTo(owner, activeSpeed);
                }
            }
        }
    }

    private boolean isFollowRescueBlocked() {
        return HamsterMovementUtil.shouldNotFollow(this.hamster)
                || this.hamster.isLeashed()
                || this.hamster.hasVehicle()
                || this.hamster.isShoulderPet()
                || this.hamster.cannotFollowOwner();
    }

    private boolean tickWaterWatchdog(LivingEntity owner) {
        if (!this.hamster.isTouchingWater() || this.isFollowRescueBlocked()) {
            this.resetWaterWatchdog();
            return false;
        }

        if (Double.isNaN(this.lastHorizontalDistance)) {
            this.lastHorizontalDistance = this.getHorizontalDistance(owner);
        }
        if (this.waterRescueRetryTicks > 0) {
            this.waterRescueRetryTicks--;
        }
        if (++this.waterProgressSampleTicks < WATER_PROGRESS_SAMPLE_TICKS) {
            return this.waterStuckTicks >= WATER_STUCK_THRESHOLD_TICKS
                    && this.waterRescueRetryTicks == 0;
        }

        this.waterProgressSampleTicks = 0;
        double horizontalDistance = this.getHorizontalDistance(owner);
        if (this.lastHorizontalDistance - horizontalDistance >= MINIMUM_WATER_PROGRESS) {
            this.waterStuckTicks = 0;
        } else {
            this.waterStuckTicks =
                    Math.min(
                            WATER_STUCK_THRESHOLD_TICKS,
                            this.waterStuckTicks + WATER_PROGRESS_SAMPLE_TICKS);
        }
        this.lastHorizontalDistance = horizontalDistance;

        return this.waterStuckTicks >= WATER_STUCK_THRESHOLD_TICKS
                && this.waterRescueRetryTicks == 0;
    }

    private double getHorizontalDistance(LivingEntity owner) {
        double deltaX = this.hamster.getX() - owner.getX();
        double deltaZ = this.hamster.getZ() - owner.getZ();
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    private void handleTeleportResult(HamsterMovementUtil.TeleportResult result) {
        if (result == HamsterMovementUtil.TeleportResult.TELEPORTED) {
            this.resetWaterWatchdog();
        } else if (result == HamsterMovementUtil.TeleportResult.FAILED) {
            this.waterRescueRetryTicks = WATER_RESCUE_RETRY_TICKS;
        }
    }

    private void resetWaterWatchdog() {
        this.lastHorizontalDistance = Double.NaN;
        this.waterProgressSampleTicks = 0;
        this.waterRescueRetryTicks = 0;
        this.waterStuckTicks = 0;
    }
}
