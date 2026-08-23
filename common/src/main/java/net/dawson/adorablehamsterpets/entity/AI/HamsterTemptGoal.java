package net.dawson.adorablehamsterpets.entity.AI;

import java.util.EnumSet;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.TemptGoalAccessor;
import net.dawson.adorablehamsterpets.util.HamsterCombatUtil;
import net.dawson.adorablehamsterpets.util.HamsterLureUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.Ingredient;

public class HamsterTemptGoal extends TemptGoal {

    // --- 1. Fields ---
    private final HamsterEntity hamster;
    private int recheckTimer = 0;

    private final TargetPredicate TEMPTATION_PREDICATE = TargetPredicate.createNonAttackable()
            .setBaseMaxDistance(10.0)
            .ignoreVisibility()
            .setPredicate(this::isTemptedBy);

    // --- 2. Constructors ---
    public HamsterTemptGoal(HamsterEntity hamster, double speed, boolean canBeScared) {
        // 1.20.1 TemptGoal requires an Ingredient, so player selection remains custom.
        super(hamster, speed, Ingredient.EMPTY, canBeScared);
        this.hamster = hamster;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private boolean isTemptedBy(LivingEntity entity) {
        return HamsterLureUtil.isTemptingItem(entity.getMainHandStack())
                || HamsterLureUtil.isTemptingItem(entity.getOffHandStack());
    }

    @Override
    public boolean canStart() {
        // --- 1. Cooldown and Protected States ---
        TemptGoalAccessor accessor = (TemptGoalAccessor) this;
        if (accessor.getCooldown() > 0) {
            accessor.setCooldown(accessor.getCooldown() - 1);
            return false;
        }
        if (!HamsterLureUtil.canFollowLure(this.hamster)) {
            return false;
        }

        // --- 2. Ownership-Aware Player Search ---
        PlayerEntity nearestPlayer = this.hamster.getWorld().getClosestPlayer(this.TEMPTATION_PREDICATE, this.hamster);
        this.closestPlayer = HamsterLureUtil.resolveTemptingPlayer(this.hamster, nearestPlayer);
        return this.closestPlayer != null;
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    @Override
    public boolean shouldContinue() {
        if (!HamsterLureUtil.canFollowLure(this.hamster)) {
            return false;
        }
        return super.shouldContinue();
    }

    @Override
    public void tick() {
        if (this.hamster.hasRedstoneFever()) {
            this.hamster.setBegging(false);
            return;
        }

        // --- Combat Target Maintenance ---
        HamsterCombatUtil.clearInvalidTarget(this.hamster);
        super.tick();

        if (this.recheckTimer > 0) {
            this.recheckTimer--;
            return;
        }
        this.recheckTimer = 5;

        PlayerEntity temptingPlayer = this.closestPlayer;
        if (temptingPlayer != null
                && temptingPlayer.isAlive()
                && this.hamster.squaredDistanceTo(temptingPlayer) < 64.0) {
            this.hamster.setBegging(HamsterLureUtil.isHoldingBeggingItem(temptingPlayer));
        } else {
            this.hamster.setBegging(false);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
        this.hamster.setBegging(false);
        this.recheckTimer = 0;
    }
}
