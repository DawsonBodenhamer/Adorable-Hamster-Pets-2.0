package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HamsterFleeGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Determines if a player is considered safe to a wild hamster.
     */
    public static boolean isPlayerSafe(Player player) {
        // Require sneaking
        if (!player.isShiftKeyDown()) {
            return false;
        }

        // Check dynamic config list for taming foods
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return ConfigDataCache.isTamingFood(mainHand) || ConfigDataCache.isTamingFood(offHand);
    }

    private static boolean shouldFlee(HamsterEntity hamster, LivingEntity livingToFleeFrom) {
        // Skip tamed hamsters and babies
        if (hamster.isTame() || hamster.isBaby()) {
            return false;
        }

        // Always flee from hostile monsters
        if (livingToFleeFrom instanceof Monster) {
            return true;
        }

        // Check if player is approaching safely with bait
        if (livingToFleeFrom instanceof Player player) {
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
    public boolean canUse() {
        // Bypass for performance
        if (this.hamster.isTame() || this.hamster.isBaby()) {
            return false;
        }

        return super.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }
}