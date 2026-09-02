package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.minecraft.world.entity.player.Player;

/**
 * Utility for tracking and evaluating complex player input gestures.
 */
public final class PlayerGestureUtil {

    private PlayerGestureUtil() {}

    /**
     * Tracks the player's sneak state to detect rapid toggling (spamming).
     * Should be called every tick from the player entity.
     */
    public static void tickSneakTracking(Player player) {
        PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
        boolean isSneaking = player.isShiftKeyDown();
        boolean wasSneaking = accessor.ahp$getWasSneaking();

        // Detect state change
        if (isSneaking != wasSneaking) {
            accessor.ahp$setWasSneaking(isSneaking);
            accessor.ahp$setSneakToggleCount(accessor.ahp$getSneakToggleCount() + 1);
            accessor.ahp$setSneakToggleTimer(15); // 15 tick window to continue the gesture
        }

        // Tick decay timer
        int timer = accessor.ahp$getSneakToggleTimer();
        if (timer > 0) {
            accessor.ahp$setSneakToggleTimer(timer - 1);
            if (timer - 1 == 0) {
                // Reset sequence if too slow
                accessor.ahp$setSneakToggleCount(0);
            }
        }
    }

    /**
     * Checks if the player has toggled their sneak state the required number of times.
     */
    public static boolean isSpammingSneak(Player player, int requiredToggles) {
        return ((PlayerEntityAccessor) player).ahp$getSneakToggleCount() >= requiredToggles;
    }

    /**
     * Consumes the current sneak spam sequence, resetting the count so it doesn't trigger multiple times.
     */
    public static void consumeSneakSpam(Player player) {
        PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
        accessor.ahp$setSneakToggleCount(0);
        accessor.ahp$setSneakToggleTimer(0);
    }
}