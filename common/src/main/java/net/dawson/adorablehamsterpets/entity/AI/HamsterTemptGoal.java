package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class HamsterTemptGoal extends TemptGoal {

    // --- 1. Fields ---
    private final HamsterEntity hamster;
    private int recheckTimer = 0; // Frequency of begging state updates

    // --- 2. Constructors ---
    public HamsterTemptGoal(HamsterEntity hamster, double speed, boolean canBeScared) {
        super(hamster, speed, ConfigDataCache::isTamingFood, canBeScared); // Call to superclass constructor
        this.hamster = hamster;
        // setControls(EnumSet.of(Control.MOVE, Control.LOOK)) is handled by superclass.
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    // --- 3. Public Methods (Overrides from TemptGoal/Goal) ---
    @Override
    public boolean canStart() {
        // --- 1. Initial State Checks ---
        if (this.hamster.isSitting() || this.hamster.isCelebratingDiamond()) {
            return false;
        }

        // --- 2. Superclass Logic ---
        if (!super.canStart()) {
            return false;
        }

        // --- 3. Ownership Check ---
        // If the hamster is tamed, only its owner can tempt it.
        if (this.hamster.isTamed()) {
            // The `closestPlayer` field is set by the `super.canStart()` call above.
            return this.hamster.isOwner(this.closestPlayer);
        }

        // If the hamster is not tamed, any player can tempt it.
        return true;
    }

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

        World world = this.hamster.getWorld();
        // Begging state is visual and primarily client-driven by animation,
        PlayerEntity temptingPlayer = this.closestPlayer;

        if (temptingPlayer != null && temptingPlayer.isAlive() && this.hamster.squaredDistanceTo(temptingPlayer) < 64.0) {
            // If a valid tempting player is nearby, set begging state based on whether they are holding a tempting item.
            this.hamster.setBegging(isHoldingTemptItem(temptingPlayer));
        } else {
            // If no valid tempting player, ensure begging state is off.
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

    // --- 4. Private Helper Methods ---

    /**
     * Checks if the given player is holding an item that matches the temptation predicate.
     *
     * @param player The player to check.
     * @return True if the player is holding a tempting item in either hand, false otherwise.
     */
    private boolean isHoldingTemptItem(PlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        return ConfigDataCache.isTamingFood(mainHandStack) || ConfigDataCache.isTamingFood(offHandStack);
    }
}