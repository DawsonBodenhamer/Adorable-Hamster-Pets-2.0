package net.dawson.adorablehamsterpets.entity.custom.animation;

import net.dawson.adorablehamsterpets.entity.AI.HamsterLookAtEntityGoal;
import net.dawson.adorablehamsterpets.entity.AI.HamsterSniffForOreGoal;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderAnimationState;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;

import com.geckolib.animation.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

/**
 * Owns hamster animation definitions, selection precedence, trigger registration, and the three
 * GeckoLib keyframe callbacks.
 *
 * <p>Gameplay state remains owned by {@link HamsterEntity}. Selection reads the entity's existing
 * public state surface. The keyframe callbacks intentionally mutate only transient renderer effect
 * IDs or trigger another animation.
 */
public final class HamsterAnimationController {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static State
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Movement Thresholds ---
    private static final double WALK_TO_RUN_THRESHOLD_SQUARED = 0.002;
    private static final double RUN_TO_SPRINT_THRESHOLD_SQUARED = 0.008;

    // --- Impact, Flight, and Refusal ---
    private static final RawAnimation CRASH_ANIM = animation("anim_hamster_crash");
    private static final RawAnimation KNOCKED_OUT_ANIM = animation("anim_hamster_ko");
    private static final RawAnimation WAKE_UP_FROM_KO_ANIM = animation("anim_hamster_wakeup_from_ko");
    private static final RawAnimation FLYING_ANIM = animation("anim_hamster_flying");
    private static final RawAnimation STANDING_HEADSHAKE_ANIM = animation("anim_hamster_standing_headshake");
    private static final RawAnimation SITTING_HEADSHAKE_ANIM = animation("anim_hamster_sitting_headshake");
    private static final RawAnimation MOVING_HEADSHAKE_ANIM = animation("anim_hamster_moving_headshake");

    // --- Sleep Poses and Transitions ---
    private static final RawAnimation SLEEP_POSE1_ANIM = animation("anim_hamster_sleep_pose1");
    private static final RawAnimation SLEEP_POSE2_ANIM = animation("anim_hamster_sleep_pose2");
    private static final RawAnimation SLEEP_POSE3_ANIM = animation("anim_hamster_sleep_pose3");
    private static final RawAnimation SIT_SETTLE_SLEEP1_ANIM = animation("anim_hamster_sit_settle_sleep1");
    private static final RawAnimation SIT_SETTLE_SLEEP2_ANIM = animation("anim_hamster_sit_settle_sleep2");
    private static final RawAnimation SIT_SETTLE_SLEEP3_ANIM = animation("anim_hamster_sit_settle_sleep3");
    private static final RawAnimation STAND_SETTLE_SLEEP1_ANIM = animation("anim_hamster_stand_settle_sleep1");
    private static final RawAnimation STAND_SETTLE_SLEEP2_ANIM = animation("anim_hamster_stand_settle_sleep2");
    private static final RawAnimation STAND_SETTLE_SLEEP3_ANIM = animation("anim_hamster_stand_settle_sleep3");

    // --- Sitting and Waking Transitions ---
    private static final RawAnimation SIT1_ANIM = animation("anim_hamster_sit1");
    private static final RawAnimation SIT2_ANIM = animation("anim_hamster_sit2");
    private static final RawAnimation SIT3_ANIM = animation("anim_hamster_sit3");
    private static final RawAnimation STANDUP1_ANIM = animation("anim_hamster_standup1");
    private static final RawAnimation STANDUP2_ANIM = animation("anim_hamster_standup2");
    private static final RawAnimation STANDUP3_ANIM = animation("anim_hamster_standup3");
    private static final RawAnimation WAKE_UP_1_ANIM = animation("anim_hamster_wakeup1");
    private static final RawAnimation WAKE_UP_2_ANIM = animation("anim_hamster_wakeup2");
    private static final RawAnimation WAKE_UP_3_ANIM = animation("anim_hamster_wakeup3");

    // --- Personality Poses ---
    private static final RawAnimation SITTING_POSE1_ANIM = animation("anim_hamster_sitting_pose1");
    private static final RawAnimation SITTING_POSE2_ANIM = animation("anim_hamster_sitting_pose2");
    private static final RawAnimation SITTING_POSE3_ANIM = animation("anim_hamster_sitting_pose3");
    private static final RawAnimation DRIFTING_OFF_POSE1_ANIM = animation("anim_hamster_drifting_off_pose1");
    private static final RawAnimation DRIFTING_OFF_POSE2_ANIM = animation("anim_hamster_drifting_off_pose2");
    private static final RawAnimation DRIFTING_OFF_POSE3_ANIM = animation("anim_hamster_drifting_off_pose3");

    // --- Locomotion and Idle ---
    private static final RawAnimation CLEANING_ANIM = animation("anim_hamster_cleaning");
    private static final RawAnimation RUNNING_ANIM = animation("anim_hamster_running");
    private static final RawAnimation WALKING_ANIM = animation("anim_hamster_walking");
    private static final RawAnimation SPRINTING_ANIM = animation("anim_hamster_sprinting");
    private static final RawAnimation BOUNCING_ANIM = animation("anim_hamster_bouncing");
    private static final RawAnimation IDLE1_ANIM = animation("anim_hamster_idle1");
    private static final RawAnimation IDLE2_ANIM = animation("anim_hamster_idle2");
    private static final RawAnimation FEVER_IDLE_ANIM = animation("anim_hamster_idle_fever");
    private static final RawAnimation IDLE_LOOKING_UP1_ANIM = animation("anim_hamster_idle_looking_up1");
    private static final RawAnimation IDLE_LOOKING_UP2_ANIM = animation("anim_hamster_idle_looking_up2");
    private static final RawAnimation IDLE_LOOKING_UP3_ANIM = animation("anim_hamster_idle_looking_up3");

    // --- Combat, Goals, and Interaction ---
    private static final RawAnimation ATTACK_ANIM = animation("anim_hamster_attack");
    private static final RawAnimation FREAK_OUT_ANIM = animation("anim_hamster_freak_out");
    private static final RawAnimation SULK_ANIM = animation("anim_hamster_sulk");
    private static final RawAnimation SULKING_ANIM = animation("anim_hamster_sulking");
    private static final RawAnimation SEEKING_ORE_ANIM = animation("anim_hamster_seeking_ore");
    private static final RawAnimation WANTS_TO_SEEK_ORE_ABOVE_ANIM = animation("anim_hamster_wants_to_seek_ore_above");
    private static final RawAnimation WANTS_TO_SEEK_ORE_BELOW_ANIM = animation("anim_hamster_wants_to_seek_ore_below");
    private static final RawAnimation POUNCE_ANIM = animation("anim_hamster_pounce");
    private static final RawAnimation TAUNTING_ANIM = animation("anim_hamster_taunt_with_item");
    private static final RawAnimation PRESENTING_ITEM_ANIM = animation("anim_hamster_presenting_item");
    private static final RawAnimation QUICK_BOUNCE_ANIM = animation("anim_hamster_quick_bounce");
    private static final RawAnimation CHEEK_UNLOAD_ANIM = animation("anim_hamster_cheek_unload");
    private static final RawAnimation CROUCH_INVESTIGATE_ANIM = animation("anim_hamster_crouch_and_investigate");

    // --- Shoulder Poses ---
    private static final RawAnimation LAYING_DOWN_HEAD_ANIM = animation("anim_hamster_shoulder_laying_down_head");
    private static final RawAnimation LAYING_DOWN_RIGHT_SHOULDER_ANIM = animation("anim_hamster_shoulder_laying_down_right_shoulder");
    private static final RawAnimation LAYING_DOWN_LEFT_SHOULDER_ANIM = animation("anim_hamster_shoulder_laying_down_left_shoulder");

    // --- Throwing, Petting, and Status ---
    private static final RawAnimation QUICK_BOUNCE_LOOKING_UP = animation("anim_hamster_quick_bounce_looking_up");
    private static final RawAnimation ASSUME_THROW_POSE_ANIM = animation("anim_hamster_assume_throw_pose");
    private static final RawAnimation WAITING_FOR_THROW_ANIM = animation("anim_hamster_waiting_for_throw");
    private static final RawAnimation RECEIVING_PETS_ANIM = animation("anim_hamster_receiving_pets");
    private static final RawAnimation STUN_ANIM = animation("anim_hamster_stun");
    private static final RawAnimation SITTING_ROLL_ANIM = animation("anim_hamster_sitting_roll");
    private static final RawAnimation SWIMMING_ANIM = animation("anim_hamster_swimming");

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterAnimationController() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration and Setup
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void register(
            HamsterEntity hamster, AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(
                                hamster,
                                "mainController",
                                3,
                                event -> {
                                    AnimationState state = AnimationState.capture(hamster);

                                    // --- 1. Controller Timing ---
                                    if (state.aiDisabled() && !state.projectileDummy()) {
                                        event.getController().setAnimationSpeed(0);
                                        event.getController().transitionLength(0);
                                    } else {
                                        event.getController().setAnimationSpeed(1);
                                        event.getController().transitionLength(3);
                                    }

                                    HamsterEntity.DozingPhase currentDozingPhase =
                                            state.dozingPhase();
                                    int personality = state.personality();

                                    // --- 2. Attached and High-Priority States ---
                                    if (state.shoulderPet()) {
                                        ShoulderAnimationState shoulderState =
                                                ShoulderAnimationState.values()[
                                                        state.shoulderStateId()];
                                        return switch (shoulderState) {
                                            case SITTING -> {
                                                if (state.cleaning()) {
                                                    yield event.setAndContinue(CLEANING_ANIM);
                                                }
                                                yield event.setAndContinue(
                                                        sittingAnimation(personality));
                                            }
                                            case LAYING_DOWN ->
                                                    switch (state.shoulderLocation()) {
                                                        case LEFT_SHOULDER ->
                                                                event.setAndContinue(
                                                                        LAYING_DOWN_LEFT_SHOULDER_ANIM);
                                                        case HEAD ->
                                                                event.setAndContinue(
                                                                        LAYING_DOWN_HEAD_ANIM);
                                                        default ->
                                                                event.setAndContinue(
                                                                        LAYING_DOWN_RIGHT_SHOULDER_ANIM);
                                                    };
                                            default ->
                                                    event.setAndContinue(
                                                            (personality % 2 == 0)
                                                                    ? IDLE2_ANIM
                                                                    : IDLE1_ANIM);
                                        };
                                    }

                                    if (state.knockedOut())
                                        return event.setAndContinue(KNOCKED_OUT_ANIM);
                                    if (state.sulking()) return event.setAndContinue(SULKING_ANIM);
                                    if (state.touchingWater() && !state.onGround())
                                        return event.setAndContinue(SWIMMING_ANIM);
                                    if (state.projectileDummy() || state.renderFlying())
                                        return event.setAndContinue(FLYING_ANIM);
                                    if (state.taunting())
                                        return event.setAndContinue(TAUNTING_ANIM);
                                    if (state.presentingItem())
                                        return event.setAndContinue(PRESENTING_ITEM_ANIM);

                                    // --- 3. Goal-Driven States ---
                                    String activeGoalName = state.activeGoalName();
                                    if (activeGoalName.startsWith(
                                            HamsterSniffForOreGoal.class.getSimpleName())) {
                                        double horizontalSpeedSquared =
                                                state.horizontalSpeedSquared();
                                        if (horizontalSpeedSquared > 1.0E-6)
                                            return event.setAndContinue(SEEKING_ORE_ANIM);
                                        return event.setAndContinue(
                                                state.oreTargetAbove()
                                                        ? WANTS_TO_SEEK_ORE_ABOVE_ANIM
                                                        : WANTS_TO_SEEK_ORE_BELOW_ANIM);
                                    }

                                    if (state.celebratingDiamond())
                                        return event.setAndContinue(BOUNCING_ANIM);
                                    if (state.dancing()) return event.setAndContinue(BOUNCING_ANIM);

                                    // --- 4. Sleep Sequence ---
                                    if (state.tamed()) {
                                        switch (currentDozingPhase) {
                                            case DRIFTING_OFF:
                                                return event.setAndContinue(
                                                        driftingOffAnimation(personality));
                                            case SETTLING_INTO_SLUMBER:
                                                String targetDeepSleepId =
                                                        state.deepSleepAnimationId();
                                                if (!targetDeepSleepId.isEmpty()) {
                                                    return event.setAndContinue(
                                                            deepSleepAnimation(
                                                                    targetDeepSleepId,
                                                                    SITTING_POSE1_ANIM));
                                                } else if (state.sitting()) {
                                                    return event.setAndContinue(
                                                            sittingAnimation(personality));
                                                }
                                                break;
                                            case DEEP_SLEEP:
                                                String deepSleepId = state.deepSleepAnimationId();
                                                return event.setAndContinue(
                                                        deepSleepAnimation(
                                                                deepSleepId,
                                                                sittingAnimation(personality)));
                                        }
                                    }

                                    if (!state.tamed() && state.sleeping()) {
                                        String deepSleepId = state.deepSleepAnimationId();
                                        return event.setAndContinue(
                                                deepSleepAnimation(deepSleepId, SLEEP_POSE1_ANIM));
                                    }

                                    if (state.sitting() && !state.knockedOut()) {
                                        return event.setAndContinue(
                                                state.cleaning()
                                                        ? CLEANING_ANIM
                                                        : sittingAnimation(personality));
                                    }

                                    // --- 5. Movement and Idle ---
                                    double horizontalSpeedSquared = state.horizontalSpeedSquared();
                                    if (horizontalSpeedSquared > 1.0E-6) {
                                        if (horizontalSpeedSquared
                                                > RUN_TO_SPRINT_THRESHOLD_SQUARED)
                                            return event.setAndContinue(SPRINTING_ANIM);
                                        if (horizontalSpeedSquared > WALK_TO_RUN_THRESHOLD_SQUARED)
                                            return event.setAndContinue(RUNNING_ANIM);
                                        return event.setAndContinue(WALKING_ANIM);
                                    }

                                    if (state.begging()) return event.setAndContinue(BOUNCING_ANIM);

                                    if (state.redstoneFever()) return event.setAndContinue(FEVER_IDLE_ANIM);

                                    if (activeGoalName.equals(
                                            HamsterLookAtEntityGoal.class.getSimpleName())) {
                                        return switch (state.lookUpAnimationId()) {
                                            case 2 -> event.setAndContinue(IDLE_LOOKING_UP2_ANIM);
                                            case 3 -> event.setAndContinue(IDLE_LOOKING_UP3_ANIM);
                                            default -> event.setAndContinue(IDLE_LOOKING_UP1_ANIM);
                                        };
                                    }

                                    RawAnimation current =
                                            event.getController().getCurrentRawAnimation();

                                    if (current != null
                                            && (current.equals(IDLE1_ANIM)
                                                    || current.equals(IDLE2_ANIM))) {
                                        return event.setAndContinue(current);
                                    }
                                    return event.setAndContinue(
                                            hamster.getRandom().nextBoolean()
                                                    ? IDLE1_ANIM
                                                    : IDLE2_ANIM);
                                })
                        // --- 6. Triggered Animations ---
                        .triggerableAnim("crash", CRASH_ANIM)
                        .triggerableAnim("wakeup_from_ko", WAKE_UP_FROM_KO_ANIM)
                        .triggerableAnim("standing_headshake", STANDING_HEADSHAKE_ANIM)
                        .triggerableAnim("sitting_headshake", SITTING_HEADSHAKE_ANIM)
                        .triggerableAnim("moving_headshake", MOVING_HEADSHAKE_ANIM)
                        .triggerableAnim("attack", ATTACK_ANIM)
                        .triggerableAnim("anim_hamster_freak_out", FREAK_OUT_ANIM)
                        .triggerableAnim("quick_bounce_on_back_legs", QUICK_BOUNCE_LOOKING_UP)
                        .triggerableAnim("sit1", SIT1_ANIM)
                        .triggerableAnim("sit2", SIT2_ANIM)
                        .triggerableAnim("sit3", SIT3_ANIM)
                        .triggerableAnim("standup1", STANDUP1_ANIM)
                        .triggerableAnim("standup2", STANDUP2_ANIM)
                        .triggerableAnim("standup3", STANDUP3_ANIM)
                        .triggerableAnim("wakeup1", WAKE_UP_1_ANIM)
                        .triggerableAnim("wakeup2", WAKE_UP_2_ANIM)
                        .triggerableAnim("wakeup3", WAKE_UP_3_ANIM)
                        .triggerableAnim("anim_hamster_sit_settle_sleep1", SIT_SETTLE_SLEEP1_ANIM)
                        .triggerableAnim("anim_hamster_sit_settle_sleep2", SIT_SETTLE_SLEEP2_ANIM)
                        .triggerableAnim("anim_hamster_sit_settle_sleep3", SIT_SETTLE_SLEEP3_ANIM)
                        .triggerableAnim("anim_hamster_stand_settle_sleep1", STAND_SETTLE_SLEEP1_ANIM)
                        .triggerableAnim("anim_hamster_stand_settle_sleep2", STAND_SETTLE_SLEEP2_ANIM)
                        .triggerableAnim("anim_hamster_stand_settle_sleep3", STAND_SETTLE_SLEEP3_ANIM)
                        .triggerableAnim("anim_hamster_sulk", SULK_ANIM)
                        .triggerableAnim("anim_hamster_pounce", POUNCE_ANIM)
                        .triggerableAnim("anim_hamster_quick_bounce", QUICK_BOUNCE_ANIM)
                        .triggerableAnim("anim_hamster_cheek_unload", CHEEK_UNLOAD_ANIM)
                        .triggerableAnim("anim_hamster_crouch_and_investigate", CROUCH_INVESTIGATE_ANIM)
                        .triggerableAnim("anim_hamster_assume_throw_pose", ASSUME_THROW_POSE_ANIM)
                        .triggerableAnim("anim_hamster_receiving_pets", RECEIVING_PETS_ANIM)
                        .triggerableAnim("stun", STUN_ANIM)
                        .triggerableAnim("sitting_roll", SITTING_ROLL_ANIM)
                        // --- 7. Keyframe Effects ---
                        .setParticleKeyframeHandler(
                                event ->
                                        hamster.particleEffectId =
                                                event.getKeyframeData().getEffect())
                        .setSoundKeyframeHandler(
                                event -> hamster.soundEffectId = event.getKeyframeData().getSound())
                        .setCustomInstructionKeyframeHandler(
                                event -> {
                                    if (event.getKeyframeData()
                                            .getInstructions()
                                            .contains("trigger_jump_anim")) {
                                        hamster.triggerAnim(
                                                "mainController", "quick_bounce_on_back_legs");
                                    }
                                }));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities and Factories
     * ────────────────────────────────────────────────────────────────────────────*/

    private static RawAnimation animation(String name) {
        return RawAnimation.begin().thenPlay(name);
    }

    private static RawAnimation sittingAnimation(int personality) {
        return switch (personality) {
            case 2 -> SITTING_POSE2_ANIM;
            case 3 -> SITTING_POSE3_ANIM;
            default -> SITTING_POSE1_ANIM;
        };
    }

    private static RawAnimation driftingOffAnimation(int personality) {
        return switch (personality) {
            case 2 -> DRIFTING_OFF_POSE2_ANIM;
            case 3 -> DRIFTING_OFF_POSE3_ANIM;
            default -> DRIFTING_OFF_POSE1_ANIM;
        };
    }

    private static RawAnimation deepSleepAnimation(String id, RawAnimation fallback) {
        return switch (id) {
            case "anim_hamster_sleep_pose1" -> SLEEP_POSE1_ANIM;
            case "anim_hamster_sleep_pose2" -> SLEEP_POSE2_ANIM;
            case "anim_hamster_sleep_pose3" -> SLEEP_POSE3_ANIM;
            default -> fallback;
        };
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ────────────────────────────────────────────────────────────────────────────*/

    private record AnimationState(
            boolean aiDisabled,
            boolean projectileDummy,
            HamsterEntity.DozingPhase dozingPhase,
            int personality,
            boolean shoulderPet,
            int shoulderStateId,
            ShoulderLocation shoulderLocation,
            boolean cleaning,
            boolean knockedOut,
            boolean sulking,
            boolean touchingWater,
            boolean onGround,
            boolean renderFlying,
            boolean taunting,
            boolean presentingItem,
            String activeGoalName,
            double horizontalSpeedSquared,
            boolean oreTargetAbove,
            boolean celebratingDiamond,
            boolean dancing,
            boolean tamed,
            String deepSleepAnimationId,
            boolean sitting,
            boolean sleeping,
            boolean begging,
            int lookUpAnimationId,
            boolean redstoneFever) {
        private static AnimationState capture(HamsterEntity hamster) {
            return new AnimationState(
                    hamster.isNoAi(),
                    hamster.isProjectileDummy,
                    hamster.getDozingPhase(),
                    hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID),
                    hamster.isShoulderPet(),
                    hamster.getEntityData().get(HamsterEntity.SHOULDER_ANIMATION_STATE),
                    hamster.shoulderLocation,
                    hamster.getHamsterFlag(HamsterEntity.CLEANING_FLAG),
                    hamster.isKnockedOut(),
                    hamster.isSulking(),
                    hamster.isInWater(),
                    hamster.onGround(),
                    hamster.shouldRenderFlying(),
                    hamster.isTaunting(),
                    hamster.isPresentingItem(),
                    hamster.getActiveCustomGoalName(),
                    hamster.getDeltaMovement().horizontalDistanceSqr(),
                    hamster.isOreTargetAbove(),
                    hamster.isCelebratingDiamond(),
                    hamster.isDancing(),
                    hamster.isTame(),
                    hamster.getEntityData().get(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID),
                    hamster.isOrderedToSit(),
                    hamster.isSleeping(),
                    hamster.isBegging(),
                    hamster.getEntityData().get(HamsterEntity.CURRENT_LOOK_UP_ANIM_ID),
                    hamster.hasRedstoneFever());
        }
    }
}
