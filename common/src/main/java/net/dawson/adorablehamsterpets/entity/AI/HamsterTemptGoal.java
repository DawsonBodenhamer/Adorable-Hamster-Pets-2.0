package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.TemptGoalAccessor;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.recipe.Ingredient;

import java.util.EnumSet;

public class HamsterTemptGoal extends TemptGoal {

    // --- 1. Fields ---
    private final HamsterEntity hamster;
    private int recheckTimer = 0; // Frequency of begging state updates

    // A custom predicate that uses the dynamic item tag check.
    private final TargetPredicate TEMPTATION_PREDICATE = TargetPredicate.createNonAttackable()
            .setBaseMaxDistance(10.0)
            .ignoreVisibility()
            .setPredicate(this::isTemptedBy);

    // --- 2. Constructors ---
    public HamsterTemptGoal(HamsterEntity hamster, double speed, boolean canBeScared) {
        // We must call super, but the Ingredient is now irrelevant.
        super(hamster, speed, Ingredient.EMPTY, canBeScared);
        this.hamster = hamster;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    /**
     * This is the predicate method used by the custom TargetPredicate.
     * It checks if the given entity (a player) is holding a valid temptation item.
     */
    private boolean isTemptedBy(LivingEntity entity) {
        return ConfigDataCache.isTamingFood(entity.getMainHandStack()) || ConfigDataCache.isTamingFood(entity.getOffHandStack());
    }

    @Override
    public boolean canStart() {
        // --- 1. Cooldown and State Checks ---
        TemptGoalAccessor accessor = (TemptGoalAccessor) this;
        if (accessor.getCooldown() > 0) {
            accessor.setCooldown(accessor.getCooldown() - 1);
            return false;
        }
        if (this.hamster.isSitting() || this.hamster.isCelebratingDiamond()) {
            return false;
        }

        // --- 2. Custom Player Search ---
        // Instead of super.canStart(), we do the player search ourselves using our custom predicate.
        this.closestPlayer = this.hamster.getWorld().getClosestPlayer(this.TEMPTATION_PREDICATE, this.hamster);
        if (this.closestPlayer == null) {
            return false; // No tempting player found.
        }

        // --- 3. Ownership Check ---
        if (this.hamster.isTamed()) {
            return this.hamster.isOwner(this.closestPlayer);
        }

        return true; // Wild hamsters can be tempted by anyone.
    }



    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    // --- 3. Public Methods (Overrides from TemptGoal/Goal) ---
    @Override
    public boolean shouldContinue() {
        // --- 1. Sitting Check ---
        if (this.hamster.isSitting() || this.hamster.isCelebratingDiamond()) {
            return false;
        }
        // --- End 1. Sitting Check ---

        // --- 2. Superclass Logic ---
        return super.shouldContinue();
    }

    @Override
    public void tick() {
        super.tick(); // Handles pathfinding towards the player and looking at them.

        // --- Begging State Logic ---
        if (this.recheckTimer > 0) {
            this.recheckTimer--;
            return;
        }
        this.recheckTimer = 5; // Re-check begging state roughly every 5 ticks.

        if (this.closestPlayer != null) {
            // The begging state should only be true if the player is actually holding the item.
            this.hamster.setBegging(isTemptedBy(this.closestPlayer));
        } else {
            this.hamster.setBegging(false);
        }
    }

    @Override
    public void stop() {
        super.stop(); // Calls vanilla TemptGoal's stop logic (clears navigation, sets cooldown).
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
        this.hamster.setBegging(false);
        this.recheckTimer = 0;
    }
}