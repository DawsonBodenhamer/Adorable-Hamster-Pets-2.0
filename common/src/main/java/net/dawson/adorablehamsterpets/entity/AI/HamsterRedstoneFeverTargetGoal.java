package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.RedstoneFeverUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Owns all Redstone Fever target acquisition.
 */
public final class HamsterRedstoneFeverTargetGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    @Nullable private LivingEntity selectedTarget;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterRedstoneFeverTargetGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        LivingEntity currentTarget = this.hamster.getTarget();
        if (!this.canAcquireFeverTarget()) {
            if (!isEligibleTarget(currentTarget)) this.clearInvalidTarget(currentTarget);
            return false;
        }

        if (this.isSelectableTarget(currentTarget)) {
            return false;
        }
        this.clearInvalidTarget(currentTarget);

        this.selectedTarget = this.findNearestTarget();
        return this.selectedTarget != null;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity currentTarget = this.hamster.getTarget();
        if (!this.canAcquireFeverTarget()) {
            if (!isEligibleTarget(currentTarget)) this.clearInvalidTarget(currentTarget);
            return false;
        }
        if (!this.isSelectableTarget(currentTarget)) {
            this.clearInvalidTarget(currentTarget);
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (this.selectedTarget != null) {
            this.hamster.setTarget(this.selectedTarget);
        }
        this.selectedTarget = null;
    }

    @Override
    public void tick() {
        // Re-evaluate often enough for a newly eligible player to take priority immediately
        if (this.hamster.age % 10 != 0) return;

        LivingEntity currentTarget = this.hamster.getTarget();
        LivingEntity preferredTarget = this.findNearestTarget();
        if (preferredTarget != null && preferredTarget != currentTarget) {
            this.hamster.setTarget(preferredTarget);
        } else if (preferredTarget == null && !this.isSelectableTarget(currentTarget)) {
            this.clearInvalidTarget(currentTarget);
        }
    }

    @Override
    public void stop() {
        this.selectedTarget = null;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean canAcquireFeverTarget() {
        return this.hamster.hasRedstoneFever()
                && !HamsterMovementUtil.shouldNotMove(this.hamster)
                && this.hamster.getWorld().getDifficulty() != Difficulty.PEACEFUL;
    }

    @Nullable
    private LivingEntity findNearestTarget() {
        double range = Configs.AHP_MAIN.redstoneFeverTargetingRange.get();
        PlayerEntity nearestReachablePlayer = this.hamster.getWorld().getPlayers().stream()
                .filter(RedstoneFeverUtil::isEligiblePlayer)
                .filter(player -> this.isWithinRange(player))
                .filter(this::isReachableTarget)
                .min(Comparator.comparingDouble(this.hamster::squaredDistanceTo))
                .orElse(null);
        if (nearestReachablePlayer != null) return nearestReachablePlayer;

        if (!Configs.AHP_MAIN.redstoneFeverAttackMostLivingMobs) return null;

        Box searchBox = this.hamster.getBoundingBox().expand(range);
        return this.hamster.getWorld().getEntitiesByClass(
                        LivingEntity.class,
                        searchBox,
                        candidate -> candidate != this.hamster
                                && !(candidate instanceof PlayerEntity)
                                && isEligibleTarget(candidate)
                                && this.isWithinRange(candidate)
                                && this.isReachableTarget(candidate))
                .stream()
                .min(Comparator.comparingDouble(this.hamster::squaredDistanceTo))
                .orElse(null);
    }

    private boolean isEligibleTarget(@Nullable LivingEntity target) {
        return RedstoneFeverUtil.isEligibleFeverTarget(this.hamster, target);
    }

    private boolean isWithinRange(@Nullable LivingEntity target) {
        return RedstoneFeverUtil.isWithinTargetingRange(this.hamster, target);
    }

    private boolean isSelectableTarget(@Nullable LivingEntity target) {
        return this.isEligibleTarget(target)
                && this.isWithinRange(target)
                && this.isReachableTarget(target);
    }

    private boolean isReachableTarget(@Nullable LivingEntity target) {
        if (target == null) return false;
        Path path = this.hamster.getNavigation().findPathTo(target, 0);
        return path != null && path.reachesTarget();
    }

    private void clearInvalidTarget(@Nullable LivingEntity currentTarget) {
        if (currentTarget != null
                && this.hamster.getTarget() == currentTarget
                && !this.isSelectableTarget(currentTarget)) {
            this.hamster.setTarget(null);
        }
    }
}
