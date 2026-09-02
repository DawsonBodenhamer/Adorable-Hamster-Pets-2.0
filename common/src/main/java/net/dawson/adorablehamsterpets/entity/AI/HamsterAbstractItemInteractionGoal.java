package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterAIUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterPhysicsUtil;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * An abstract base goal that provides a robust, unified state machine for locating,
 * pathfinding, repositioning, and pouncing on dropped items.
 */
public abstract class HamsterAbstractItemInteractionGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    protected static final int LUNGE_DURATION_TICKS = 5;
    protected static final int SEARCH_RADIUS = 10;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    protected final HamsterEntity hamster;
    protected final Level world;

    @Nullable protected ItemEntity targetItem;
    @Nullable protected Vec3 pounceStartPos;
    @Nullable protected Vec3 repositionTarget;

    protected int lungeTicks;
    protected int repositionAttempts;
    protected int checkTimer = 0;
    protected int moveTimeout = 0;

    protected State currentState = State.SCANNING;

    protected enum State {
        SCANNING,
        MOVING_TO_ITEM,
        REPOSITIONING,
        POUNCING,
        POST_POUNCE_ACTION
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterAbstractItemInteractionGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    // Subclasses must implement these to define their specific behaviors
    protected abstract boolean isValidTarget(ItemStack stack);
    protected abstract boolean canStartBaseChecks();
    protected abstract boolean shouldContinueBaseChecks();
    protected abstract void onPounceComplete(ItemStack stackSnapshot);
    protected abstract void tickPostPounce();
    protected abstract void onGoalStopped();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        if (this.world.isClientSide()) return false;
        if (!canStartBaseChecks()) return false;

        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = this.adjustedTickDelay(10);

        Optional<ItemEntity> closestItem = HamsterAIUtil.findReachableItem(this.hamster, SEARCH_RADIUS, item -> isValidTarget(item.getItem()));

        if (closestItem.isPresent()) {
            this.targetItem = closestItem.get();
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!shouldContinueBaseChecks()) return false;

        if (this.currentState == State.MOVING_TO_ITEM || this.currentState == State.REPOSITIONING || this.currentState == State.POUNCING) {
            return this.targetItem != null && this.targetItem.isAlive();
        }
        return true;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
        this.currentState = State.MOVING_TO_ITEM;
        this.moveTimeout = 0;
        this.repositionAttempts = 0;
        this.repositionTarget = null;
        this.hamster.getNavigation().moveTo(this.targetItem, 1.5D);
    }

    @Override
    public void stop() {
        this.hamster.getNavigation().stop();
        this.targetItem = null;
        this.currentState = State.SCANNING;

        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
        onGoalStopped();
    }

    @Override
    public void tick() {
        switch (this.currentState) {
            case MOVING_TO_ITEM -> {
                if (this.targetItem == null || this.targetItem.isRemoved()) {
                    this.targetItem = null;
                    return;
                }

                this.moveTimeout++;
                if (this.moveTimeout > 150) {
                    this.targetItem = null; // Give up after ~7.5 seconds
                    return;
                }

                HamsterMovementUtil.faceEntity(this.hamster, this.targetItem);

                if (this.hamster.getNavigation().isDone()) {
                    this.currentState = State.REPOSITIONING;
                    return;
                }

                if (this.hamster.getBoundingBox().inflate(1.5, 1.5, 1.5).intersects(this.targetItem.getBoundingBox())) {
                    this.currentState = State.POUNCING;
                    this.pounceStartPos = this.hamster.position();
                    this.hamster.getNavigation().stop();

                    if (this.hamster.isInWater() || this.hamster.isInLava()) {
                        this.lungeTicks = 0; // Skip lunge delay and animation in fluid
                    } else {
                        this.lungeTicks = LUNGE_DURATION_TICKS;
                        this.hamster.triggerAnimOnServer("mainController", "anim_hamster_pounce");
                    }
                }
            }
            case REPOSITIONING -> {
                if (this.targetItem == null || this.targetItem.isRemoved()) {
                    this.targetItem = null;
                    return;
                }

                if (this.repositionAttempts >= 3) {
                    this.targetItem = null; // Force stop if it can't find a way after 3 tries
                    return;
                }

                if (this.repositionTarget == null) {
                    this.repositionAttempts++;
                    this.repositionTarget = LandRandomPos.getPosTowards(this.hamster, 2, 3, Vec3.atCenterOf(this.targetItem.blockPosition()));
                    if (this.repositionTarget != null) {
                        this.hamster.getNavigation().moveTo(this.repositionTarget.x, this.repositionTarget.y, this.repositionTarget.z, 1.55D);
                    } else {
                        this.targetItem = null;
                        return;
                    }
                }

                if (this.hamster.getNavigation().isDone()) {
                    this.repositionTarget = null;
                    this.currentState = State.MOVING_TO_ITEM;
                    this.hamster.getNavigation().moveTo(this.targetItem, 1.5D);
                }
            }
            case POUNCING -> {
                if (this.targetItem == null || this.targetItem.isRemoved()) {
                    this.targetItem = null;
                    return;
                }

                this.lungeTicks--;

                if (this.lungeTicks >= 0) {
                    net.dawson.adorablehamsterpets.util.HamsterMovementUtil.faceEntity(this.hamster, this.targetItem);
                }

                if (this.pounceStartPos != null && this.lungeTicks >= 0) {
                    Vec3 interpolatedPos = HamsterPhysicsUtil.calculatePouncePosition(
                            this.pounceStartPos,
                            this.targetItem.position(),
                            this.lungeTicks,
                            LUNGE_DURATION_TICKS
                    );
                    this.hamster.setPos(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
                }

                if (this.lungeTicks < 0) {
                    ItemStack stackToConsume = this.targetItem.getItem().copy();
                    if (!stackToConsume.isEmpty()) {
                        this.currentState = State.POST_POUNCE_ACTION;
                        onPounceComplete(stackToConsume);
                    } else {
                        this.targetItem = null;
                    }
                }
            }
            case POST_POUNCE_ACTION -> {
                tickPostPounce();
            }
        }
    }
}