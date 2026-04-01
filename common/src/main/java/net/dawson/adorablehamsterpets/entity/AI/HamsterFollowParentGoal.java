package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.UUID;

public class HamsterFollowParentGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private HamsterEntity parent;
    private final double speed;
    private int delay;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterFollowParentGoal(HamsterEntity hamster, double speed) {
        this.hamster = hamster;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        if (!this.hamster.isBaby()) return false;
        if (HamsterMovementUtil.shouldNotFollow(this.hamster)) return false;

        UUID parentUuid = this.hamster.getParentUuid();
        if (parentUuid == null) return false;

        if (this.hamster.getWorld() instanceof ServerWorld serverWorld) {
            Entity entity = serverWorld.getEntity(parentUuid);
            if (entity instanceof HamsterEntity hamsterEntity && entity.isAlive()) {
                this.parent = hamsterEntity;
                // Follow if more than 2 blocks away
                return this.hamster.squaredDistanceTo(this.parent) > 4.0;
            }
        }
        return false;
    }


    @Override
    public boolean shouldContinue() {
        if (!this.hamster.isBaby() || this.parent == null || !this.parent.isAlive()) return false;
        if (HamsterMovementUtil.shouldNotFollow(this.hamster)) return false;

        double distanceSq = this.hamster.squaredDistanceTo(this.parent);
        // Continue following as long as distance is between 2 and 16 blocks
        return distanceSq > 4.0 && distanceSq <= 256.0;
    }

    @Override
    public void start() {
        this.delay = 0;
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        this.parent = null;
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        if (--this.delay <= 0) {
            this.delay = this.getTickCount(10);
            this.hamster.getNavigation().startMovingTo(this.parent, this.speed);
        }
    }
}