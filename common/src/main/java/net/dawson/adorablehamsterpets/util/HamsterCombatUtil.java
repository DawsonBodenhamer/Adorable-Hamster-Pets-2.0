package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles hamster-specific melee range and owner-combat exclusions.
 */
public final class HamsterCombatUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double ATTACK_BOX_EXPANSION = 0.70D;
    private static final int OWNER_EVENT_FRESHNESS_TICKS = 100;
    private static final long STANDARD_COMBAT_DURATION_TICKS = 600L;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isInAttackRange(HamsterEntity hamster, LivingEntity target) {
        Box attackBox =
                hamster.getBoundingBox().expand(ATTACK_BOX_EXPANSION, 0.0D, ATTACK_BOX_EXPANSION);
        return attackBox.intersects(target.getBoundingBox());
    }

    public static StandardCombatState createStandardCombatState() {
        return new StandardCombatState();
    }

    public static boolean canAttackWithOwner(
            HamsterEntity hamster, LivingEntity target, LivingEntity owner) {
        if (!isTargetLegal(hamster, target) || !isFreshOwnerConflict(owner, target)) {
            return false;
        }
        return !isSuppressedConflict(hamster, target);
    }

    public static boolean canAcquireTarget(HamsterEntity hamster, LivingEntity target) {
        return isTargetLegal(hamster, target) && !isSuppressedConflict(hamster, target);
    }

    public static boolean canContinueTarget(HamsterEntity hamster, @Nullable LivingEntity target) {
        if (target == null || !isTargetLegal(hamster, target)) {
            return false;
        }
        if (!usesStandardCombatWindow(hamster)) {
            return hamster.getAggressionState() != HamsterEntity.AggressionState.PACIFIST;
        }

        StandardCombatState state = hamster.getStandardCombatState();
        return state.targetUuid != null
                && state.targetUuid.equals(target.getUuid())
                && hamster.getWorld().getTime() < state.deadline;
    }

    public static void acceptTarget(
            HamsterEntity hamster,
            @Nullable LivingEntity previousTarget,
            @Nullable LivingEntity acceptedTarget) {
        if (hamster.getWorld().isClient()) {
            return;
        }
        if (acceptedTarget == null) {
            if (usesStandardCombatWindow(hamster)) {
                clearActiveCombatWindow(hamster.getStandardCombatState());
            } else {
                clearStandardCombatState(hamster);
            }
            return;
        }
        if (!usesStandardCombatWindow(hamster)) {
            clearStandardCombatState(hamster);
            return;
        }

        StandardCombatState state = hamster.getStandardCombatState();
        if (previousTarget == acceptedTarget && acceptedTarget.getUuid().equals(state.targetUuid)) {
            refreshFromOwnerCombat(hamster, acceptedTarget);
            return;
        }

        LivingEntity owner = hamster.getOwner();
        state.targetUuid = acceptedTarget.getUuid();
        state.deadline = hamster.getWorld().getTime() + STANDARD_COMBAT_DURATION_TICKS;
        state.observedOwner = owner;
        state.consumedOwnerAttackTime = owner == null ? Integer.MIN_VALUE : owner.getLastAttackTime();
        state.consumedOwnerAttackedTime =
                owner == null ? Integer.MIN_VALUE : owner.getLastAttackedTime();
        state.consumedHamsterAttackedTime = hamster.getLastAttackedTime();
        state.expiredTargetUuid = null;
        state.expiredOwner = null;
    }

    public static void tickStandardCombat(HamsterEntity hamster) {
        if (hamster.getWorld().isClient()) {
            return;
        }

        LivingEntity target = hamster.getTarget();
        if (!usesStandardCombatWindow(hamster)) {
            if (hamster.getAggressionState() != HamsterEntity.AggressionState.STANDARD) {
                clearStandardCombatState(hamster);
            }
            return;
        }
        if (target == null) {
            clearActiveCombatWindow(hamster.getStandardCombatState());
            return;
        }
        if (!isTargetLegal(hamster, target)) {
            terminateStandardCombat(hamster, false);
            return;
        }

        refreshFromOwnerCombat(hamster, target);
        if (hamster.getWorld().getTime() >= hamster.getStandardCombatState().deadline) {
            terminateStandardCombat(hamster, true);
        }
    }

    public static void handleAggressionStateChange(
            HamsterEntity hamster,
            HamsterEntity.AggressionState previousState,
            HamsterEntity.AggressionState newState) {
        if (hamster.getWorld().isClient() || previousState == newState) {
            return;
        }
        if (newState == HamsterEntity.AggressionState.MENACE) {
            clearStandardCombatState(hamster);
            return;
        }
        if (newState == HamsterEntity.AggressionState.PACIFIST
                || previousState == HamsterEntity.AggressionState.MENACE) {
            terminateStandardCombat(hamster, false);
        }
    }

    public static void terminateStandardCombat(HamsterEntity hamster) {
        terminateStandardCombat(hamster, false);
    }

    public static void clearStandardCombatState(HamsterEntity hamster) {
        StandardCombatState state = hamster.getStandardCombatState();
        clearActiveCombatWindow(state);
        state.expiredTargetUuid = null;
        state.expiredOwner = null;
        state.expiredOwnerAttackTime = Integer.MIN_VALUE;
        state.expiredOwnerAttackedTime = Integer.MIN_VALUE;
        state.expiredHamsterAttackedTime = Integer.MIN_VALUE;
    }

    private static boolean isTargetLegal(HamsterEntity hamster, LivingEntity target) {
        if (!target.isAlive()
                || target.isRemoved()
                || hamster.getAggressionState() == HamsterEntity.AggressionState.PACIFIST) {
            return false;
        }

        LivingEntity owner = hamster.getOwner();
        if (target == hamster || target == owner || target instanceof CreeperEntity
                || target instanceof ArmorStandEntity) {
            return false;
        }
        if (owner == null) {
            return true;
        }

        UUID ownerUuid = owner.getUuid();
        AdorableHamsterPets.LOGGER.trace(
                "[canAttackWithOwner] Hamster: {}, Target: {}, Owner: {}",
                hamster.getName().getString(),
                target.getName().getString(),
                owner.getName().getString());

        if (target instanceof PlayerEntity && target.getUuid().equals(ownerUuid)) {
            return false;
        }
        if (target instanceof TameableEntity tameablePet) {
            UUID petOwnerUuid = tameablePet.getOwnerUuid();
            if (petOwnerUuid != null && petOwnerUuid.equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[canAttackWithOwner] Target is a TameableEntity owned by the same player."
                                + " Preventing attack.");
                return false;
            }
        } else if (target instanceof AbstractHorseEntity horsePet) {
            Entity horseOwnerEntity = horsePet.getOwner();
            if (horseOwnerEntity != null && horseOwnerEntity.getUuid().equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[canAttackWithOwner] Target is an AbstractHorseEntity owned by the same"
                                + " player. Preventing attack.");
                return false;
            }
        } else if (target instanceof Ownable ownableFallback) {
            Entity fallbackOwnerEntity = ownableFallback.getOwner();
            if (fallbackOwnerEntity != null && fallbackOwnerEntity.getUuid().equals(ownerUuid)) {
                AdorableHamsterPets.LOGGER.trace(
                        "[canAttackWithOwner] Target is an Ownable (fallback) owned by the same"
                                + " player. Preventing attack.");
                return false;
            }
        }
        return true;
    }

    private static boolean isFreshOwnerConflict(LivingEntity owner, LivingEntity target) {
        boolean freshOwnerAttack =
                target == owner.getAttacking()
                        && owner.age - owner.getLastAttackTime() <= OWNER_EVENT_FRESHNESS_TICKS;
        boolean freshOwnerAttacker =
                target == owner.getAttacker()
                        && owner.age - owner.getLastAttackedTime() <= OWNER_EVENT_FRESHNESS_TICKS;
        return freshOwnerAttack || freshOwnerAttacker;
    }

    private static boolean isSuppressedConflict(HamsterEntity hamster, LivingEntity target) {
        if (!usesStandardCombatWindow(hamster)) {
            return false;
        }

        StandardCombatState state = hamster.getStandardCombatState();
        if (!target.getUuid().equals(state.expiredTargetUuid)) {
            return false;
        }

        LivingEntity owner = hamster.getOwner();
        if (owner != null && owner != state.expiredOwner) {
            return !isFreshOwnerConflict(owner, target);
        }
        if (owner != null) {
            if (target == owner.getAttacking()
                    && owner.getLastAttackTime() > state.expiredOwnerAttackTime) {
                return false;
            }
            if (target == owner.getAttacker()
                    && owner.getLastAttackedTime() > state.expiredOwnerAttackedTime) {
                return false;
            }
        }
        return target != hamster.getAttacker()
                || hamster.getLastAttackedTime() <= state.expiredHamsterAttackedTime;
    }

    private static void refreshFromOwnerCombat(HamsterEntity hamster, LivingEntity target) {
        StandardCombatState state = hamster.getStandardCombatState();
        LivingEntity owner = hamster.getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        if (owner != state.observedOwner) {
            state.observedOwner = owner;
            state.consumedOwnerAttackTime = owner.getLastAttackTime();
            state.consumedOwnerAttackedTime = owner.getLastAttackedTime();
            return;
        }

        boolean refreshed = false;
        if (target == owner.getAttacking()
                && owner.getLastAttackTime() > state.consumedOwnerAttackTime) {
            state.consumedOwnerAttackTime = owner.getLastAttackTime();
            refreshed = true;
        }
        if (target == owner.getAttacker()
                && owner.getLastAttackedTime() > state.consumedOwnerAttackedTime) {
            state.consumedOwnerAttackedTime = owner.getLastAttackedTime();
            refreshed = true;
        }
        if (refreshed) {
            state.deadline = hamster.getWorld().getTime() + STANDARD_COMBAT_DURATION_TICKS;
        }
    }

    private static boolean usesStandardCombatWindow(HamsterEntity hamster) {
        return hamster.isTamed()
                && hamster.getAggressionState() == HamsterEntity.AggressionState.STANDARD;
    }

    private static void terminateStandardCombat(HamsterEntity hamster, boolean suppressConflict) {
        LivingEntity target = hamster.getTarget();
        StandardCombatState state = hamster.getStandardCombatState();
        UUID expiredTargetUuid = suppressConflict && target != null ? target.getUuid() : null;
        LivingEntity expiredOwner = suppressConflict ? hamster.getOwner() : null;
        int expiredOwnerAttackTime =
                expiredOwner == null ? Integer.MIN_VALUE : expiredOwner.getLastAttackTime();
        int expiredOwnerAttackedTime =
                expiredOwner == null ? Integer.MIN_VALUE : expiredOwner.getLastAttackedTime();
        int expiredHamsterAttackedTime = hamster.getLastAttackedTime();

        hamster.setTarget(null);
        hamster.getNavigation().stop();
        clearActiveCombatWindow(state);
        if (suppressConflict && target != null) {
            state.expiredTargetUuid = expiredTargetUuid;
            state.expiredOwner = expiredOwner;
            state.expiredOwnerAttackTime = expiredOwnerAttackTime;
            state.expiredOwnerAttackedTime = expiredOwnerAttackedTime;
            state.expiredHamsterAttackedTime = expiredHamsterAttackedTime;
        } else {
            clearStandardCombatState(hamster);
        }
    }

    private static void clearActiveCombatWindow(StandardCombatState state) {
        state.targetUuid = null;
        state.deadline = Long.MIN_VALUE;
        state.observedOwner = null;
        state.consumedOwnerAttackTime = Integer.MIN_VALUE;
        state.consumedOwnerAttackedTime = Integer.MIN_VALUE;
        state.consumedHamsterAttackedTime = Integer.MIN_VALUE;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterCombatUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final class StandardCombatState {
        @Nullable private UUID targetUuid;
        private long deadline = Long.MIN_VALUE;
        @Nullable private LivingEntity observedOwner;
        private int consumedOwnerAttackTime = Integer.MIN_VALUE;
        private int consumedOwnerAttackedTime = Integer.MIN_VALUE;
        private int consumedHamsterAttackedTime = Integer.MIN_VALUE;
        @Nullable private UUID expiredTargetUuid;
        @Nullable private LivingEntity expiredOwner;
        private int expiredOwnerAttackTime = Integer.MIN_VALUE;
        private int expiredOwnerAttackedTime = Integer.MIN_VALUE;
        private int expiredHamsterAttackedTime = Integer.MIN_VALUE;

        private StandardCombatState() {}
    }
}
