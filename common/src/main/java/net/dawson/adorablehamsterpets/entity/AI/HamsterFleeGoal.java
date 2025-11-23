package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class HamsterFleeGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

    // --- 1. Fields ---
    private final HamsterEntity hamster;
    // No other unique fields in this class

    // --- 2. Constructors ---
    public HamsterFleeGoal(
            HamsterEntity hamster,
            Class<T> fleeFromType,
            float distance,
            double slowSpeed,
            double fastSpeed
    ) {
        super(
                hamster,
                fleeFromType,
                distance,
                slowSpeed,
                fastSpeed,
                livingEntity -> shouldFlee(hamster, livingEntity) // Pass the static helper method directly
        );
        this.hamster = hamster; // Store the hamster instance
    }

    // --- 3. Public Static Helper Methods ---

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override public void stop(){
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) { this.hamster.setActiveCustomGoalDebugName("None"); }
    }

    /**
     * Determines if a player is considered "safe" to a wild hamster,
     * meaning the hamster should not flee from them.
     * A player is safe if they are sneaking AND holding Sliced Cucumber.
     *
     * @param player The player to check.
     * @return True if the player is safe, false otherwise.
     */
    public static boolean isPlayerSafe(PlayerEntity player) {
        // --- 1. Sneaking Check ---
        // If the player is not sneaking, they are not considered safe.
        if (!player.isSneaking()) {
            return false;
        }
        // --- End 1. Sneaking Check ---

        // --- 2. Item Check ---
        // Check if the player is holding Sliced Cucumber in either hand.
        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        boolean holdingCucumber = mainHandStack.isOf(ModItems.SLICED_CUCUMBER.get()) || offHandStack.isOf(ModItems.SLICED_CUCUMBER.get());
        // --- End 2. Item Check ---

        return holdingCucumber; // Player is safe only if sneaking AND holding cucumber
    }

    // --- 4. Private Static Helper Methods ---

    /**
     * Determines if the given hamster should flee from the specified living entity.
     * This method is used as the predicate for the FleeEntityGoal.
     *
     * @param hamster The hamster that might flee.
     * @param livingToFleeFrom The entity to potentially flee from.
     * @return True if the hamster should flee, false otherwise.
     */
    private static boolean shouldFlee(HamsterEntity hamster, LivingEntity livingToFleeFrom) {
        // --- 1. Tamed Check ---
        // Tamed hamsters do not use this flee logic.
        if (hamster.isTamed()) {
            return false;
        }
        // --- End 1. Tamed Check ---

        // --- 2. Hostile Entity Check ---
        // Always flee from hostile entities.
        if (livingToFleeFrom instanceof HostileEntity) {
            return true;
        }
        // --- End 2. Hostile Entity Check ---

        // --- 3. Player Entity Check ---
        // If the entity is a player, check if the player is considered "safe".
        // Flee if the player is NOT safe.
        if (livingToFleeFrom instanceof PlayerEntity player) {
            return !isPlayerSafe(player);
        }
        // --- End 3. Player Entity Check ---

        // --- 4. Default ---
        // Do not flee from other non-hostile, non-player entities.
        return false;
        // --- End 4. Default ---
    }
    // No other methods (canStart, shouldContinue, start, stop, tick) are overridden in this class, so the superclass FleeEntityGoal implementations are used.
}