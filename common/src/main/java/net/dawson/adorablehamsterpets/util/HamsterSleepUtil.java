package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * Handles the "Path to Slumber" state machine, sleep condition validation,
 * and sleep cycle management for tamed hamsters.
 */
public final class HamsterSleepUtil {

    private HamsterSleepUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Core State Machine
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Executes the main sleep state machine logic.
     * Should be called from the entity's server-side tick method.
     */
    public static void tickTamedSleepLogic(HamsterEntity hamster) {
        // Bypass if the hamster is actively sleeping in its bed
        // The bed's own logic handles day/night and circadian wakeups
        if (HamsterBedUtil.isSleepingInBed(hamster)) {
            return;
        }

        HamsterEntity.DozingPhase currentPhase = hamster.getDozingPhase();
        boolean canInitiate = evaluateSleepConditions(hamster, false);
        boolean canSustain = evaluateSleepConditions(hamster, true);

        switch (currentPhase) {
            case NONE -> {
                // Command to sit starts Phase 1 if conditions allow
                if (hamster.isOrderedToSit() && canInitiate) {
                    if (hamster.getQuiescentSitTimer() == 0) {
                        hamster.setDozingPhase(HamsterEntity.DozingPhase.QUIESCENT_SITTING);

                        int minSeconds = Configs.AHP_MAIN.tamedQuiescentSitMinSeconds.get();
                        int maxSeconds = Configs.AHP_MAIN.tamedQuiescentSitMaxSeconds.get();

                        // Safety rail flip
                        if (minSeconds > maxSeconds) {
                            int temp = minSeconds;
                            minSeconds = maxSeconds;
                            maxSeconds = temp;
                        }
                        if (maxSeconds < minSeconds) maxSeconds = minSeconds;

                        int durationTicks = hamster.getRandom().nextIntBetweenInclusive(minSeconds * 20, maxSeconds * 20 + 1);
                        hamster.setQuiescentSitTimer(durationTicks);
                    }
                }
            }
            case QUIESCENT_SITTING -> {
                if (!hamster.isOrderedToSit() || !canInitiate) {
                    resetSleepState(hamster);
                    break;
                }
                if (hamster.getQuiescentSitTimer() > 0) {
                    hamster.setQuiescentSitTimer(hamster.getQuiescentSitTimer() - 1);
                } else {
                    // Timer expired move to Drifting Off
                    hamster.setDozingPhase(HamsterEntity.DozingPhase.DRIFTING_OFF);
                    hamster.setDriftingOffTimer(90 * 20); // 90 seconds
                }
            }
            case DRIFTING_OFF -> {
                if (!canSustain) {
                    resetSleepState(hamster);
                    break;
                }
                if (hamster.getDriftingOffTimer() > 0) {
                    hamster.setDriftingOffTimer(hamster.getDriftingOffTimer() - 1);
                } else {
                    // Drifting off animation completed
                    hamster.setDozingPhase(HamsterEntity.DozingPhase.SETTLING_INTO_SLUMBER);

                    // Select sleep pose based on personality
                    int personalityId = hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
                    String settleAnimId = HamsterPoseUtil.getSettleSleepAnimId(personalityId, true);
                    String deepSleepAnimIdForTracker = HamsterPoseUtil.getDeepSleepAnimId(personalityId);

                    hamster.setCurrentDeepSleepAnimId(deepSleepAnimIdForTracker);
                    hamster.triggerAnimOnServer("mainController", settleAnimId);
                    hamster.setSettleSleepCooldown(20);

                    // Swish and thump audio
                    hamster.triggerSettleEffects(0.22f, 5, 0.24f);
                }
            }
            case SETTLING_INTO_SLUMBER -> {
                if (!canSustain) {
                    resetSleepState(hamster);
                    break;
                }
                if (hamster.getSettleSleepCooldown() > 0) {
                    hamster.setSettleSleepCooldown(hamster.getSettleSleepCooldown() - 1);
                } else {
                    // Settle animation finished transition to deep sleep
                    hamster.setDozingPhase(HamsterEntity.DozingPhase.DEEP_SLEEP);
                    hamster.setSleeping(true);
                }
            }
            case DEEP_SLEEP -> {
                if (!canSustain) {
                    hamster.triggerWakeUpFromSleepAnimation(false);
                    resetSleepState(hamster);
                }
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Checks if conditions allow slumber.
     */
    private static boolean evaluateSleepConditions(HamsterEntity hamster, boolean isSustaining) {
        if (!hamster.isOrderedToSit()) return false;

        Level world = hamster.level();
        if (Configs.AHP_MAIN.requireDaytimeForTamedSleep && !world.isBrightOutside()) {
            return false;
        }
        if (hamster.isInLove()) return false;

        double threatRadius = Configs.AHP_MAIN.tamedSleepThreatDetectionRadiusBlocks.get();
        List<LivingEntity> nearbyHostiles = world.getEntitiesOfClass(
                LivingEntity.class,
                hamster.getBoundingBox().inflate(threatRadius),
                entity -> entity instanceof Monster && entity.isAlive() && !entity.isSpectator()
        );
        return nearbyHostiles.isEmpty();
    }

    /**
     * Resets the sleep sequence back to the default state and clears timers.
     */
    public static void resetSleepState(HamsterEntity hamster) {
        hamster.setDozingPhase(HamsterEntity.DozingPhase.NONE);
        hamster.setQuiescentSitTimer(0);
        hamster.setDriftingOffTimer(0);
        hamster.setSettleSleepCooldown(0);
        hamster.setSleeping(false);
    }
}