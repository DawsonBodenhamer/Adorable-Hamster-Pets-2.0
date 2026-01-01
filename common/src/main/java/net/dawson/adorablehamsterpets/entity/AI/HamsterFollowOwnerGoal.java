package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.FollowOwnerGoalAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.util.math.Vec3d;

public class HamsterFollowOwnerGoal extends FollowOwnerGoal {
    private final HamsterEntity hamster;
    private static final double BUFFED_FOLLOW_SPEED = 1.5D;

    public HamsterFollowOwnerGoal(HamsterEntity hamster, double speed, float minDistance, float maxDistance) {
        super(hamster, speed, minDistance, maxDistance);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        // --- 1. Let the vanilla logic run first ---
        // This is crucial because super.canStart() finds and sets the 'owner' field.
        if (!super.canStart()) {
            return false;
        }

        // --- 2. Apply  custom conditions ---
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond() ||
                this.hamster.isCelebratingRetrieval() ||
                this.hamster.isWanderModeActive()) {
            return false;
        }

        // --- 3. Re-check distance with buff modification ---
        // Use accessor to get the base minimum distance
        float minDist = ((FollowOwnerGoalAccessor) this).getMinDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return false; // Should be handled by super, but good practice
        if (hamster.cannotFollowOwner()) return false;

        // Dynamically adjust min distance for buffed state
        if (this.hamster.hasGreenBeanBuff()) {
            minDist += 5.0F;
        }

        // Re-check the distance condition with the potentially modified value
        return !(hamster.squaredDistanceTo(owner) < (double) (minDist * minDist));
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond() ||
                this.hamster.isCelebratingRetrieval()) {
            return false;
        }

        // Use accessor to get the base maximum distance
        float maxDist = ((FollowOwnerGoalAccessor) this).getMaxDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return false;

        // Dynamically adjust max distance for buffed state
        if (this.hamster.hasGreenBeanBuff()) {
            maxDist += 5.0F;
        }

        // The rest of the vanilla logic
        return !this.hamster.getNavigation().isIdle() && this.hamster.squaredDistanceTo(owner) > (double)(maxDist * maxDist);
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
        AdorableHamsterPets.LOGGER.trace("[FollowGoal-{}] start: Goal has started. IsBuffed: {}", this.hamster.getId(), this.hamster.hasGreenBeanBuff());
    }

    @Override
    public void tick() {
        // --- 1. Get Owner and Check Teleport Condition ---
        // This logic is now shared for both buffed and non-buffed states.
        FollowOwnerGoalAccessor accessor = (FollowOwnerGoalAccessor) this;
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return; // Safety check

        // The shouldTryTeleportToOwner() method respects the follow distance.
        boolean shouldTeleport = this.hamster.shouldTryTeleportToOwner();

        // --- 2. Handle Looking ---
        // Always look at the owner if not about to teleport.
        if (!shouldTeleport) {
            this.hamster.getLookControl().lookAt(owner, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);
        }

        // --- 4. Use Vanilla Update Timer via Accessor ---
        int currentTicks = accessor.getUpdateCountdownTicks() - 1;
        accessor.setUpdateCountdownTicks(currentTicks);

        if (currentTicks <= 0) {
            accessor.setUpdateCountdownTicks(this.getTickCount(10));

            // --- 5. Execute Teleport or Pathfinding ---
            if (shouldTeleport) {
                this.hamster.tryTeleportToOwner();
            } else {
                if (this.hamster.hasGreenBeanBuff()) {
                    // "Zoomies" pathfinding logic.
                    Vec3d targetPos = FuzzyTargeting.findTo(this.hamster, 8, 5, Vec3d.ofCenter(owner.getBlockPos()));
                    if (targetPos != null) {
                        this.hamster.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, BUFFED_FOLLOW_SPEED);
                    }
                } else {
                    // Standard, direct pathfinding for non-buffed hamsters, using the accessor for speed.
                    this.hamster.getNavigation().startMovingTo(owner, accessor.getSpeed());
                }
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }
}