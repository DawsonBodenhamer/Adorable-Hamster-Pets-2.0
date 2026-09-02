package net.dawson.adorablehamsterpets.util;

import net.minecraft.core.UUIDUtil;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
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
        AABB attackBox =
                hamster.getBoundingBox().inflate(ATTACK_BOX_EXPANSION, 0.0D, ATTACK_BOX_EXPANSION);
        return attackBox.intersects(target.getBoundingBox());
    }

    public static StandardCombatState createStandardCombatState() {
        return new StandardCombatState();
    }

    public static boolean isStandardCombatEngaged(HamsterEntity hamster) {
        if (!usesStandardCombatWindow(hamster)) {
            return false;
        }

        LivingEntity target = hamster.getTarget();
        StandardCombatState state = hamster.getStandardCombatState();
        return isConflictEngaged(
                target == null ? null : target.getUUID(),
                state.targetUuid,
                hamster.level().getGameTime(),
                state.deadline);
    }

    public static boolean deescalateStandardCombat(HamsterEntity hamster) {
        if (hamster.level().isClientSide() || !isStandardCombatEngaged(hamster)) {
            return false;
        }

        LivingEntity target = hamster.getTarget();
        StandardCombatState state = hamster.getStandardCombatState();
        if (target != null) {
            refreshFromOwnerCombat(hamster, target);
        }

        UUID conflictUuid = target == null ? state.targetUuid : target.getUUID();
        LivingEntity owner = hamster.getOwner();
        int ownerAttackTime = owner == state.observedOwner
                ? state.consumedOwnerAttackTime
                : owner == null ? Integer.MIN_VALUE : owner.getLastHurtMobTimestamp();
        int ownerAttackedTime = owner == state.observedOwner
                ? state.consumedOwnerAttackedTime
                : owner == null ? Integer.MIN_VALUE : owner.getLastHurtByMobTimestamp();
        LivingEntity attacker = hamster.getLastHurtByMob();
        boolean conflictMatchesRetaliation =
                attacker != null && attacker.getUUID().equals(conflictUuid);
        int hamsterAttackedTime = conflictMatchesRetaliation
                ? hamster.getLastHurtByMobTimestamp()
                : state.consumedHamsterAttackedTime;

        hamster.setTarget(null);
        hamster.getNavigation().stop();
        if (conflictMatchesRetaliation) {
            hamster.setLastHurtByMob(null);
        }

        clearActiveCombatWindow(state);
        state.expiredTargetUuid = conflictUuid;
        state.expiredOwner = owner;
        state.expiredOwnerAttackTime = ownerAttackTime;
        state.expiredOwnerAttackedTime = ownerAttackedTime;
        state.expiredHamsterAttackedTime = hamsterAttackedTime;
        return true;
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
                && state.targetUuid.equals(target.getUUID())
                && hamster.level().getGameTime() < state.deadline;
    }

    public static boolean clearInvalidTarget(HamsterEntity hamster) {
        LivingEntity target = hamster.getTarget();
        if (target == null || canContinueTarget(hamster, target)) {
            return false;
        }

        terminateStandardCombat(hamster, false);
        return true;
    }

    public static void acceptTarget(
            HamsterEntity hamster,
            @Nullable LivingEntity previousTarget,
            @Nullable LivingEntity acceptedTarget) {
        if (hamster.level().isClientSide()) {
            return;
        }
        if (acceptedTarget == null) {
            if (!usesStandardCombatWindow(hamster)) {
                clearStandardCombatState(hamster);
            }
            return;
        }
        if (!usesStandardCombatWindow(hamster)) {
            clearStandardCombatState(hamster);
            return;
        }

        StandardCombatState state = hamster.getStandardCombatState();
        if (previousTarget == acceptedTarget && acceptedTarget.getUUID().equals(state.targetUuid)) {
            refreshFromOwnerCombat(hamster, acceptedTarget);
            return;
        }

        LivingEntity owner = hamster.getOwner();
        state.targetUuid = acceptedTarget.getUUID();
        state.deadline = hamster.level().getGameTime() + STANDARD_COMBAT_DURATION_TICKS;
        state.observedOwner = owner;
        state.consumedOwnerAttackTime = owner == null ? Integer.MIN_VALUE : owner.getLastHurtMobTimestamp();
        state.consumedOwnerAttackedTime =
                owner == null ? Integer.MIN_VALUE : owner.getLastHurtByMobTimestamp();
        state.consumedHamsterAttackedTime = hamster.getLastHurtByMobTimestamp();
        state.expiredTargetUuid = null;
        state.expiredOwner = null;
    }

    public static void tickStandardCombat(HamsterEntity hamster) {
        if (hamster.level().isClientSide()) {
            return;
        }
        if (hamster.hasRedstoneFever()) {
            // Dedicated fever goals replace ordinary aggression state
            clearStandardCombatState(hamster);
            return;
        }

        LivingEntity target = hamster.getTarget();
        if (target != null && !isTargetLegal(hamster, target)) {
            terminateStandardCombat(hamster, false);
            return;
        }

        if (!usesStandardCombatWindow(hamster)) {
            if (hamster.getAggressionState() != HamsterEntity.AggressionState.STANDARD) {
                clearStandardCombatState(hamster);
            }
            return;
        }

        if (target == null) {
            StandardCombatState state = hamster.getStandardCombatState();
            if (state.targetUuid != null && hamster.level().getGameTime() >= state.deadline) {
                terminateStandardCombat(hamster, true);
            }
            return;
        }

        refreshFromOwnerCombat(hamster, target);
        if (hamster.level().getGameTime() >= hamster.getStandardCombatState().deadline) {
            terminateStandardCombat(hamster, true);
        }
    }

    public static void handleAggressionStateChange(
            HamsterEntity hamster,
            HamsterEntity.AggressionState previousState,
            HamsterEntity.AggressionState newState) {
        if (hamster.level().isClientSide() || previousState == newState) {
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
        if (target == hamster || target == owner || target instanceof Creeper
                || target instanceof ArmorStand) {
            return false;
        }
        UUID ownerUuid = (hamster.getOwnerReference() == null ? null : hamster.getOwnerReference().getUUID());
        if (ownerUuid == null) {
            return true;
        }

        AdorableHamsterPets.LOGGER.trace(
                "[canAttackWithOwner] Hamster: {}, Target: {}, Owner: {}",
                hamster.getName().getString(),
                target.getName().getString(),
                owner == null ? ownerUuid : owner.getName().getString());

        UUID targetOwnerUuid = PetOwnershipUtil.resolveTargetOwnerUuid(target);
        if (ownerUuid.equals(targetOwnerUuid)) {
            AdorableHamsterPets.LOGGER.trace(
                    "[canAttackWithOwner] Target belongs to the hamster owner. Preventing attack.");
            return false;
        }
        return !isAcornRingContractProtected(hamster, ownerUuid, targetOwnerUuid);
    }

    private static boolean isAcornRingContractProtected(
            HamsterEntity hamster, UUID hamsterOwnerUuid, @Nullable UUID targetOwnerUuid) {
        if (targetOwnerUuid == null
                || targetOwnerUuid.equals(hamsterOwnerUuid)
                || !(hamster.level() instanceof ServerLevel serverWorld)) {
            return false;
        }

        ServerPlayer hamsterOwner =
                serverWorld.getServer().getPlayerList().getPlayer(hamsterOwnerUuid);
        ServerPlayer targetOwner =
                serverWorld.getServer().getPlayerList().getPlayer(targetOwnerUuid);
        return hamsterOwner != null
                && targetOwner != null
                && isContractProtected(
                        hamsterOwnerUuid,
                        targetOwnerUuid,
                        AcornRingUtil.isEquipped(hamsterOwner),
                        AcornRingUtil.isEquipped(targetOwner));
    }

    static boolean isContractProtected(
            UUID hamsterOwnerUuid,
            UUID targetOwnerUuid,
            boolean hamsterOwnerEquipped,
            boolean targetOwnerEquipped) {
        return AcornRingUtil.isContractProtected(
                hamsterOwnerUuid,
                targetOwnerUuid,
                hamsterOwnerEquipped,
                targetOwnerEquipped);
    }

    private static boolean isFreshOwnerConflict(LivingEntity owner, LivingEntity target) {
        boolean freshOwnerAttack =
                target == owner.getLastHurtMob()
                        && owner.tickCount - owner.getLastHurtMobTimestamp() <= OWNER_EVENT_FRESHNESS_TICKS;
        boolean freshOwnerAttacker =
                target == owner.getLastHurtByMob()
                        && owner.tickCount - owner.getLastHurtByMobTimestamp() <= OWNER_EVENT_FRESHNESS_TICKS;
        return freshOwnerAttack || freshOwnerAttacker;
    }

    private static boolean isSuppressedConflict(HamsterEntity hamster, LivingEntity target) {
        if (!usesStandardCombatWindow(hamster)) {
            return false;
        }

        StandardCombatState state = hamster.getStandardCombatState();
        if (!target.getUUID().equals(state.expiredTargetUuid)) {
            return false;
        }

        LivingEntity owner = hamster.getOwner();
        if (owner != null && owner != state.expiredOwner) {
            return !isFreshOwnerConflict(owner, target);
        }
        if (owner != null) {
            if (target == owner.getLastHurtMob()
                    && owner.getLastHurtMobTimestamp() > state.expiredOwnerAttackTime) {
                return false;
            }
            if (target == owner.getLastHurtByMob()
                    && owner.getLastHurtByMobTimestamp() > state.expiredOwnerAttackedTime) {
                return false;
            }
        }
        return target != hamster.getLastHurtByMob()
                || hamster.getLastHurtByMobTimestamp() <= state.expiredHamsterAttackedTime;
    }

    private static void refreshFromOwnerCombat(HamsterEntity hamster, LivingEntity target) {
        StandardCombatState state = hamster.getStandardCombatState();
        LivingEntity owner = hamster.getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        if (owner != state.observedOwner) {
            state.observedOwner = owner;
            state.consumedOwnerAttackTime = owner.getLastHurtMobTimestamp();
            state.consumedOwnerAttackedTime = owner.getLastHurtByMobTimestamp();
            return;
        }

        boolean refreshed = false;
        if (target == owner.getLastHurtMob()
                && owner.getLastHurtMobTimestamp() > state.consumedOwnerAttackTime) {
            state.consumedOwnerAttackTime = owner.getLastHurtMobTimestamp();
            refreshed = true;
        }
        if (target == owner.getLastHurtByMob()
                && owner.getLastHurtByMobTimestamp() > state.consumedOwnerAttackedTime) {
            state.consumedOwnerAttackedTime = owner.getLastHurtByMobTimestamp();
            refreshed = true;
        }
        if (refreshed) {
            state.deadline = hamster.level().getGameTime() + STANDARD_COMBAT_DURATION_TICKS;
        }
    }

    private static boolean usesStandardCombatWindow(HamsterEntity hamster) {
        return hamster.isTame()
                && hamster.getAggressionState() == HamsterEntity.AggressionState.STANDARD;
    }

    static boolean isConflictEngaged(
            @Nullable UUID currentTargetUuid,
            @Nullable UUID windowTargetUuid,
            long currentTick,
            long deadline) {
        return currentTargetUuid != null
                || windowTargetUuid != null && currentTick < deadline;
    }

    private static void terminateStandardCombat(HamsterEntity hamster, boolean suppressConflict) {
        LivingEntity target = hamster.getTarget();
        StandardCombatState state = hamster.getStandardCombatState();
        UUID expiredTargetUuid = suppressConflict
                ? target == null ? state.targetUuid : target.getUUID()
                : null;
        LivingEntity expiredOwner = suppressConflict
                ? state.observedOwner == null ? hamster.getOwner() : state.observedOwner
                : null;
        int expiredOwnerAttackTime = expiredOwner == null
                ? Integer.MIN_VALUE
                : expiredOwner == state.observedOwner
                        ? state.consumedOwnerAttackTime
                        : expiredOwner.getLastHurtMobTimestamp();
        int expiredOwnerAttackedTime = expiredOwner == null
                ? Integer.MIN_VALUE
                : expiredOwner == state.observedOwner
                        ? state.consumedOwnerAttackedTime
                        : expiredOwner.getLastHurtByMobTimestamp();
        int expiredHamsterAttackedTime = hamster.getLastHurtByMobTimestamp();

        hamster.setTarget(null);
        hamster.getNavigation().stop();
        if (target != null && target == hamster.getLastHurtByMob()) {
            hamster.setLastHurtByMob(null);
        }
        clearActiveCombatWindow(state);
        if (suppressConflict && expiredTargetUuid != null) {
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
