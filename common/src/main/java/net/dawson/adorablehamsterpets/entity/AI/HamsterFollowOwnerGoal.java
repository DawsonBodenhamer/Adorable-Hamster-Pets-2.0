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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;

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
        // Recalculate minimum follow distance for zoomies
        float minDist = ((FollowOwnerGoalAccessor) this).getMinDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();

        if (owner == null || this.hamster.cannotFollowOwner()) {
            return false;
        }

        if (this.hamster.hasGreenBeanBuff()) {
            minDist += 5.0F;
        }

        return !(this.hamster.squaredDistanceTo(owner) < (double) (minDist * minDist));
    }

    @Override
    public boolean shouldContinue() {
        // --- State Exclusions ---
        if (HamsterMovementUtil.shouldNotFollow(this.hamster)) {
            return false;
        }

        // --- Distance Calculation ---
        // Recalculate maximum follow distance for zoomies
        float maxDist = ((FollowOwnerGoalAccessor) this).getMaxDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();

        if (owner == null) {
            return false;
        }

        if (this.hamster.hasGreenBeanBuff()) {
            maxDist += 5.0F;
        }

        return !this.hamster.getNavigation().isIdle() && this.hamster.squaredDistanceTo(owner) > (double) (maxDist * maxDist);
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        // --- Target Resolution ---
        FollowOwnerGoalAccessor accessor = (FollowOwnerGoalAccessor) this;
        LivingEntity owner = accessor.getOwner();

        if (owner == null) {
            return;
        }

        boolean shouldTeleport = HamsterMovementUtil.shouldTeleportTo(this.hamster, owner);

        // --- Facing Logic ---
        if (!shouldTeleport) {
            HamsterMovementUtil.faceEntity(this.hamster, owner);
        }

        // --- Update Timer ---
        int currentTicks = accessor.getUpdateCountdownTicks() - 1;
        accessor.setUpdateCountdownTicks(currentTicks);

        if (currentTicks <= 0) {
            accessor.setUpdateCountdownTicks(this.getTickCount(10));

            // --- Movement Execution ---
            if (shouldTeleport) {
                HamsterMovementUtil.tryTeleportTo(this.hamster, owner);
            } else {
                if (this.hamster.hasGreenBeanBuff()) {
                    // Zoomies erratic pathfinding
                    Vec3d targetPos = FuzzyTargeting.findTo(this.hamster, 8, 5, Vec3d.ofCenter(owner.getBlockPos()));
                    if (targetPos != null) {
                        this.hamster.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, BUFFED_FOLLOW_SPEED);
                    }
                } else {
                    // Standard pathfinding
                    this.hamster.getNavigation().startMovingTo(owner, accessor.getSpeed());
                }
            }
        }
    }
}