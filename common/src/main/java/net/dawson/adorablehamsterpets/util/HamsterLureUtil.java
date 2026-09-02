package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.AI.HamsterFollowOwnerGoal;
import net.dawson.adorablehamsterpets.entity.AI.HamsterGoToBedAndSleepGoal;
import net.dawson.adorablehamsterpets.entity.AI.HamsterHideAndSeekGoal;
import net.dawson.adorablehamsterpets.entity.AI.HamsterInterHamsterTagGoal;
import net.dawson.adorablehamsterpets.entity.AI.HamsterPlayWithItemGoal;
import net.dawson.adorablehamsterpets.entity.AI.HamsterTagGoal;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Centralizes dietary attraction, begging, ownership, and protected-state rules.
 */
public final class HamsterLureUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double TEMPT_RANGE_SQUARED = 100.0D;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isTemptingItem(ItemStack stack) {
        return shouldTempt(
                ConfigDataCache.isDietaryItem(stack), ConfigDataCache.isLureItem(stack));
    }

    public static boolean isBeggingItem(ItemStack stack) {
        return shouldBeg(
                ConfigDataCache.isTamingFood(stack), ConfigDataCache.isLureItem(stack));
    }

    public static boolean isTamingItem(ItemStack stack) {
        return isBeggingItem(stack) && ConfigDataCache.isTamingFood(stack);
    }

    public static boolean isShoulderMountItem(ItemStack stack) {
        return isBeggingItem(stack) && ConfigDataCache.isLureItem(stack);
    }

    public static boolean isHoldingTemptingItem(Player player) {
        return isTemptingItem(player.getMainHandItem())
                || isTemptingItem(player.getOffhandItem());
    }

    public static boolean isHoldingBeggingItem(Player player) {
        return isBeggingItem(player.getMainHandItem()) || isBeggingItem(player.getOffhandItem());
    }

    @Nullable
    public static Player resolveTemptingPlayer(
            HamsterEntity hamster, @Nullable Player nearestDietaryPlayer) {
        if (!hamster.isTame()) {
            return nearestDietaryPlayer;
        }

        Entity owner = hamster.getOwner();
        if (!(owner instanceof Player ownerPlayer)
                || !ownerPlayer.isAlive()
                || ownerPlayer.isSpectator()
                || hamster.distanceToSqr(ownerPlayer) > TEMPT_RANGE_SQUARED
                || !isHoldingTemptingItem(ownerPlayer)) {
            return null;
        }
        return ownerPlayer;
    }

    public static boolean canFollowLure(HamsterEntity hamster) {
        // --- Condition, Physical, and Sleep Locks ---
        if (hamster.hasRedstoneFever()
                || HamsterMovementUtil.shouldNotMove(hamster)
                || hamster.isRescueSleeping()
                || hamster.getDozingPhase() != HamsterEntity.DozingPhase.NONE
                || hamster.isLeashed()
                || hamster.isPassenger()
                || hamster.isVehicle()
                || hamster.isShoulderPet()) {
            return false;
        }

        // --- Purposeful Activity Locks ---
        if (hamster.isOnTheWayToBed()
                || hamster.isPlayingTag()
                || hamster.isHiding()
                || hamster.isHoldingMouthItem()
                || hamster.getGenericInteractionTimer() > 0
                || hamster.isAutoEating()
                || hamster.isConsideringAutoEat()
                || hamster.isBeingPet()
                || hamster.isRefusingFood()
                || hamster.isCelebratingDiamond()
                || hamster.isCelebratingBaby()) {
            return false;
        }

        // --- Active Goal Locks ---
        String activeGoal = hamster.getActiveCustomGoalName();
        return !activeGoal.startsWith(HamsterPlayWithItemGoal.class.getSimpleName())
                && !activeGoal.startsWith(HamsterGoToBedAndSleepGoal.class.getSimpleName())
                && !activeGoal.startsWith(HamsterTagGoal.class.getSimpleName())
                && !activeGoal.startsWith(HamsterInterHamsterTagGoal.class.getSimpleName())
                && !activeGoal.startsWith(HamsterHideAndSeekGoal.class.getSimpleName())
                && !(hamster.isInWater()
                        && activeGoal.startsWith(HamsterFollowOwnerGoal.class.getSimpleName()));
    }

    static boolean shouldBeg(boolean isTamingFood, boolean isShoulderMountFood) {
        return isTamingFood || isShoulderMountFood;
    }

    static boolean shouldTempt(boolean isDietaryItem, boolean isShoulderMountItem) {
        return isDietaryItem || isShoulderMountItem;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterLureUtil() {}
}
