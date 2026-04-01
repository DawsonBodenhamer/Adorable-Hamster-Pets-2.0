package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class HamsterFleeGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Determines if a player is considered safe to a wild hamster.
     */
    public static boolean isPlayerSafe(PlayerEntity player) {
        // Require sneaking
        if (!player.isSneaking()) {
            return false;
        }

        // Check dynamic config list for taming foods
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        return ConfigDataCache.isTamingFood(mainHand) || ConfigDataCache.isTamingFood(offHand);
    }

    private static boolean shouldFlee(HamsterEntity hamster, LivingEntity livingToFleeFrom) {
        // Skip tamed hamsters and babies
        if (hamster.isTamed() || hamster.isBaby()) {
            return false;
        }

        // Always flee from hostile monsters
        if (livingToFleeFrom instanceof HostileEntity) {
            return true;
        }

        // Check if player is approaching safely with bait
        if (livingToFleeFrom instanceof PlayerEntity player) {
            return !isPlayerSafe(player);
        }

        // Do not flee from other neutral entities
        return false;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterFleeGoal(HamsterEntity hamster, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
        super(hamster, fleeFromType, distance, slowSpeed, fastSpeed, target -> shouldFlee(hamster, target));
        this.hamster = hamster;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }
}