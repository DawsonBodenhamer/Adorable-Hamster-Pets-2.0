package net.dawson.adorablehamsterpets.util;

/**
 * Resolves gesture precedence for hamster interactions that share configurable items.
 */
public final class HamsterInteractionGestureUtil {

    public static boolean isAggressionToggleGesture(
            boolean sneaking,
            boolean isPacifistItem,
            boolean isStandardItem,
            boolean isMenaceItem) {
        return !sneaking && (isPacifistItem || isStandardItem || isMenaceItem);
    }

    public static boolean isAccessoryEquipGesture(boolean sneaking, boolean isFlower) {
        return isFlower ? sneaking : !sneaking;
    }

    public static PacifistItemAction resolvePacifistItemAction(
            boolean alreadyPacifist, boolean hasActiveCombat) {
        if (alreadyPacifist) {
            return PacifistItemAction.FALL_THROUGH;
        }
        return hasActiveCombat
                ? PacifistItemAction.END_FIGHT_IN_STANDARD
                : PacifistItemAction.ENABLE_PACIFIST;
    }

    public enum PacifistItemAction {
        FALL_THROUGH,
        END_FIGHT_IN_STANDARD,
        ENABLE_PACIFIST
    }

    private HamsterInteractionGestureUtil() {}
}
