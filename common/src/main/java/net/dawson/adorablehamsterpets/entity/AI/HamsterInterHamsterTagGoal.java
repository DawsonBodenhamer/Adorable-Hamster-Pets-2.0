package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Handles the logic for hamsters playing tag with each other.
 * This single goal manages both the Instigator and the Chaser roles.
 */
public class HamsterInterHamsterTagGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Fields / Constants / Enums
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double TETHER_DISTANCE = 14.0;

    private final HamsterEntity hamster;

    private enum State {
        APPROACHING, // Instigator: Moving to slap partner
        WAITING,     // Chaser: Waiting to be slapped
        STUNNED,     // Chaser: Stunned, lying on back
        FLEEING,     // Instigator: Running away
        TAUNTING,    // Instigator: "nanny nanny boo boo"
        CHASING,     // Chaser: Pursuing instigator
        RETURNING_TO_OWNER // Both: Returning to normal follow distance
    }

    private State currentState;
    private int gameTimerTicks;
    private int stunTimer;
    private int pathUpdateTimer;

    public HamsterInterHamsterTagGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        // --- 1. Partner Activation Override ---
        // If this hamster was selected by another hamster to be the Chaser, bypass RNG roll and start immediately
        if (this.hamster.isPlayingTag() && this.hamster.isInterHamsterTagActive && this.hamster.tagGamePartner != null) {
            return true;
        }

        // --- 2. Master Config Check ---
        if (!Configs.AHP_MAIN.enableInterHamsterTag) return false;

        // --- 3. Restrict Roll Frequency ---
        // Only roll dice once per second for performance
        if (this.hamster.tickCount % 20 != 0) return false;

        // --- 4. Personal Validity Check ---
        if (!isValidForTag(this.hamster)) return false;

        // --- 5. Population-Independent RNG Calculation ---
        // Scan 16-block radius for other valid hamsters
        List<HamsterEntity> validNearbyHamsters = this.hamster.level().getEntitiesOfClass(
                HamsterEntity.class,
                this.hamster.getBoundingBox().inflate(16.0),
                h -> h != this.hamster && isValidForTag(h)
        );

        int populationSize = validNearbyHamsters.size() + 1; // Others + Self
        if (populationSize < 2) return false;

        // Calculate chance: 1 game per X seconds for the entire room
        // Rolls happen every 20 ticks (1 roll per second per hamster)
        // Chance = 1 / (seconds * populationSize)
        int averageSeconds = Configs.AHP_MAIN.interHamsterTagAverageSeconds.get();
        int chanceDenominator = averageSeconds * populationSize;

        // Roll dice
        if (this.hamster.getRandom().nextInt(chanceDenominator) == 0) {
            // Success. Pick random partner
            HamsterEntity partner = validNearbyHamsters.get(this.hamster.getRandom().nextInt(validNearbyHamsters.size()));

            // Setup Instigator (Self)
            this.hamster.tagGamePartner = partner;
            this.hamster.isTagChaser = false;
            this.hamster.isInterHamsterTagActive = true;
            this.hamster.setPlayingTag(true);

            // Setup Chaser (Partner)
            partner.tagGamePartner = this.hamster;
            partner.isTagChaser = true;
            partner.isInterHamsterTagActive = true;
            partner.setPlayingTag(true);

            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // If the player right-clicks either hamster, InteractionUtil sets isPlayingTag to false
        if (!this.hamster.isPlayingTag() || !this.hamster.isInterHamsterTagActive) return false;

        // Check if partner died, vanished, or had their game interrupted
        HamsterEntity partner = this.hamster.tagGamePartner;
        if (partner == null || !partner.isAlive() || !partner.isPlayingTag() || !partner.isInterHamsterTagActive) {
            return false;
        }

        // Allow extra time for returning to player
        if (this.currentState == State.RETURNING_TO_OWNER) {
            return true;
        }

        return this.gameTimerTicks > 0;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName() + (this.hamster.isTagChaser ? "_Chaser" : "_Instigator"));
        this.gameTimerTicks = Configs.AHP_MAIN.interHamsterTagMaxDurationSeconds.get() * 20;
        this.pathUpdateTimer = 0;

        if (this.hamster.isTagChaser) {
            this.currentState = State.WAITING;
        } else {
            this.currentState = State.APPROACHING;
        }
    }

    @Override
    public void stop() {
        this.hamster.getNavigation().stop();

        // 1. Clear active flags
        this.hamster.isInterHamsterTagActive = false;
        this.hamster.tagGameSlapped = false;
        this.hamster.tagGameWon = false;
        this.hamster.setTaunting(false);

        if (this.hamster.getActiveCustomGoalName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }

        // 2. Check if game ended naturally or via player interruption
        if (this.hamster.isPlayingTag()) {
            this.hamster.setPlayingTag(false); // Clean up for next cycle
        }

        // 3. Clear partner reference
        this.hamster.tagGamePartner = null;
    }

    @Override
    public void tick() {
        // Synchronize end of game
        if (this.hamster.tagGameCooldownEndTick > this.hamster.level().getGameTime() && this.gameTimerTicks > 0) {
            this.gameTimerTicks = 0;
        }

        if (this.currentState != State.RETURNING_TO_OWNER) {
            this.gameTimerTicks--;
        }

        HamsterEntity partner = this.hamster.tagGamePartner;

        // Secure against start-of-tick race conditions and handle transition to returning
        if (this.gameTimerTicks <= 0 && this.currentState != State.RETURNING_TO_OWNER) {
            long cooldownEnd = this.hamster.level().getGameTime() + 100;
            this.hamster.tagGameCooldownEndTick = cooldownEnd;
            if (partner != null) partner.tagGameCooldownEndTick = cooldownEnd;

            // Only Instigator schedules celebration if caught to prevent duplicate calls
            if (!this.hamster.isTagChaser && partner != null && partner.isAlive()) {
                if (this.hamster.tagGameWon) {
                    triggerEndGameCelebration();
                }
            }

            this.currentState = State.RETURNING_TO_OWNER;
            this.pathUpdateTimer = 0;
        }

        if (partner == null && this.currentState != State.RETURNING_TO_OWNER) return;

        if (this.currentState != State.RETURNING_TO_OWNER) {
            HamsterMovementUtil.faceEntity(this.hamster, partner);
        }

        this.pathUpdateTimer--;

        switch (this.currentState) {
            case APPROACHING -> {
                // Instigator: move to partner to start game
                if (this.pathUpdateTimer <= 0) {
                    this.pathUpdateTimer = 10;
                    this.hamster.getNavigation().moveTo(partner, 1.5D);
                }
                if (this.hamster.distanceTo(partner) < 0.6) { // Distance required for "physical contact"
                    executeFakeAttack();
                    if (partner != null) partner.tagGameSlapped = true; // Signal chaser
                    this.currentState = State.FLEEING;
                    this.pathUpdateTimer = 0;
                }
            }
            case FLEEING -> {
                this.hamster.setTaunting(false);
                LivingEntity owner = this.hamster.getOwner();
                double minFleeDist = Configs.AHP_MAIN.minMiniGameFleeDistance.get();
                double maxFleeDist = Configs.AHP_MAIN.maxMiniGameFleeDistance.get();

                if (owner != null) {
                    // Instigator: run away but stay tethered to owner if tamed
                    if (this.hamster.distanceToSqr(owner) > (TETHER_DISTANCE * TETHER_DISTANCE)) {
                        // Too far from owner, run back towards owner instead
                        if (this.pathUpdateTimer <= 0) {
                            this.pathUpdateTimer = 10;
                            this.hamster.getNavigation().moveTo(owner, 1.5D);
                        }
                    } else if (HamsterMovementUtil.shouldStopFleeing(this.hamster, partner, maxFleeDist)) {
                        // Safe distance reached
                        this.currentState = State.TAUNTING;
                        this.hamster.getNavigation().stop();
                    } else if (HamsterMovementUtil.shouldFlee(this.hamster, partner, minFleeDist)) {
                        // Run erratically around player
                        if (this.pathUpdateTimer <= 0) {
                            this.pathUpdateTimer = 10;
                            // Drastic angle step to force crossing the circle
                            Optional<BlockPos> targetOpt = HamsterMovementUtil.findOrbitingTarget(
                                    this.hamster,
                                    owner,
                                    2.0,
                                    TETHER_DISTANCE,
                                    130,
                                    230
                            );
                            targetOpt.ifPresent(pos -> this.hamster.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.5D));
                        }
                    }
                } else {
                    // Untethered fleeing
                    if (HamsterMovementUtil.shouldStopFleeing(this.hamster, partner, maxFleeDist)) {
                        // Safe distance reached
                        this.currentState = State.TAUNTING;
                        this.hamster.getNavigation().stop();
                    } else if (HamsterMovementUtil.shouldFlee(this.hamster, partner, minFleeDist)) {
                        // Run away from Chaser
                        if (this.pathUpdateTimer <= 0) {
                            this.pathUpdateTimer = 10;
                            Vec3 fleePos = HamsterMovementUtil.findFleePosition(this.hamster, partner, minFleeDist, maxFleeDist);
                            if (fleePos != null) {
                                this.hamster.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.5D);
                            }
                        }
                    }
                }
            }
            case TAUNTING -> {
                LivingEntity owner = this.hamster.getOwner();
                double minFleeDist = Configs.AHP_MAIN.minMiniGameFleeDistance.get();

                if (owner != null && this.hamster.distanceToSqr(owner) > (TETHER_DISTANCE * TETHER_DISTANCE)) {
                    // Prioritize returning to owner if tether broken
                    this.currentState = State.FLEEING;
                    this.hamster.setTaunting(false);
                    this.pathUpdateTimer = 0;
                } else if (HamsterMovementUtil.shouldFlee(this.hamster, partner, minFleeDist)) {
                    // Chaser got too close, resume fleeing
                    this.currentState = State.FLEEING;
                    this.hamster.setTaunting(false);
                    this.pathUpdateTimer = 0;
                } else {
                    // Keep taunting
                    this.hamster.setTaunting(true);
                    this.hamster.getNavigation().stop();
                }
            }
            case WAITING -> {
                // Chaser: Wait to be slapped
                this.hamster.getNavigation().stop();
                if (this.hamster.tagGameSlapped) {
                    this.hamster.tagGameSlapped = false; // Consume signal
                    this.hamster.triggerAnimOnServer("mainController", "stun");
                    this.stunTimer = 26; // 26 ticks for stun
                    this.currentState = State.STUNNED;
                }
            }
            case STUNNED -> {
                // Chaser: dazed on ground then waking up
                this.stunTimer--;
                if (this.stunTimer <= 0) {
                    this.currentState = State.CHASING;
                    this.pathUpdateTimer = 0;
                }
            }
            case CHASING -> {
                // Chaser: pursue Instigator
                if (this.pathUpdateTimer <= 0) {
                    this.pathUpdateTimer = 10;
                    this.hamster.getNavigation().moveTo(partner, 1.45D);
                }

                if (this.hamster.distanceTo(partner) < 0.6) { // Distance required for "physical contact"
                    // Caught, end game
                    this.gameTimerTicks = 0;
                    this.hamster.tagGameWon = true;
                    if (partner != null) partner.tagGameWon = true;

                    // Feedback
                    this.hamster.level().playSound(null, this.hamster.blockPosition(), ModSounds.HAMSTER_DING.get(), SoundSource.NEUTRAL, 0.4F, this.hamster.getVoicePitch());
                    SoundEvent contactSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SCRATCH_SOUNDS, hamster.getRandom());
                    if (contactSound != null) {
                        hamster.playSound(contactSound, 1.0f, hamster.getVoicePitch());
                    }

                    if (!this.hamster.level().isClientSide()) {
                        ParticleEffectsUtil.spawnParticlesOnEntity(
                                this.hamster,
                                ParticleTypes.HEART,
                                3,
                                0.5,
                                0.5,
                                0.0,
                                0.5
                        );
                    }

                    // Secure against race conditions
                    long cooldownEnd = this.hamster.level().getGameTime() + 100;
                    this.hamster.tagGameCooldownEndTick = cooldownEnd;
                    if (partner != null) partner.tagGameCooldownEndTick = cooldownEnd;
                }
            }
            case RETURNING_TO_OWNER -> {
                // Wait for celebration
                if (this.hamster.isFrozenMovement()) {
                    return;
                }

                LivingEntity owner = this.hamster.getOwner();
                if (owner != null && this.hamster.distanceToSqr(owner) > (8.0 * 8.0)) {
                    if (this.pathUpdateTimer <= 0) {
                        this.pathUpdateTimer = 10;
                        this.hamster.getNavigation().moveTo(owner, 1.2D);
                    }
                } else {
                    // Reached normal follow distance
                    this.hamster.setPlayingTag(false);
                }
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Checks if a hamster is in a valid state to play tag.
     */
    private boolean isValidForTag(HamsterEntity hamster) {
        if (HamsterMovementUtil.shouldNotMove(hamster)
                || hamster.isPlayingTag()
                || hamster.isWanderModeActive()
                || hamster.isShoulderPet()
        ) {
            return false;
        }

        return hamster.level().getGameTime() >= hamster.tagGameCooldownEndTick;
    }

    /**
     * Performs a visual attack to initiate the chase without dealing actual damage.
     */
    private void executeFakeAttack() {
        this.hamster.triggerAnimOnServer("mainController", "attack");

        // Feedback
        this.hamster.level().playSound(null, this.hamster.blockPosition(), ModSounds.HAMSTER_SLAP.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);

        // Apply small physical knockback to partner
        HamsterEntity partner = this.hamster.tagGamePartner;
        if (partner != null && partner.isAlive()) {
            Vec3 knockbackDir = partner.position().subtract(this.hamster.position()).normalize();
            partner.setDeltaMovement(partner.getDeltaMovement().add(knockbackDir.x * 0.3, 0.3, knockbackDir.z * 0.3));
            partner.needsSync = true;
        }
    }

    /**
     * Schedules the synchronized celebration animations for both hamsters.
     */
    private void triggerEndGameCelebration() {
        HamsterEntity partner = this.hamster.tagGamePartner;
        if (partner == null || !partner.isAlive()) return;

        long currentTime = this.hamster.level().getGameTime();

        // Apply cooldown
        long cooldownEnd = currentTime + 100;
        this.hamster.tagGameCooldownEndTick = cooldownEnd;
        partner.tagGameCooldownEndTick = cooldownEnd;

        // Schedule celebrations
        setupCelebration(this.hamster, partner, currentTime, 0);
        setupCelebration(partner, this.hamster, currentTime, 7 + partner.getRandom().nextInt(20));
    }

    /**
     * Utilizes the existing FREEZING_MOVEMENT_FLAG to lock the AI's rotation
     * so they stare directly at each other during the animation.
     */
    private void setupCelebration(HamsterEntity hamster1, HamsterEntity hamster2, long currentTime, int delayTicks) {
        // Lock AI and rotation
        hamster1.setFrozenMovement(true);
        hamster1.setCelebrationTarget(hamster2);
        hamster1.setCelebrationTicks(100);
        hamster1.getNavigation().stop();

        // Stagger animation start times slightly
        long startTick = currentTime + delayTicks;

        hamster1.scheduleTask(startTick, "inter_tag_celebrate", () -> {
            if (hamster1.isAlive() && hamster1.isFrozenMovement()) {
                hamster1.triggerAnimOnServer("mainController", "anim_hamster_crouch_and_investigate");
                SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, hamster1.getRandom());
                if (affectionSound != null) {
                    hamster1.playSound(affectionSound, 1.0f, hamster1.getVoicePitch());
                }
            }
        });

        // Release AI lock when anim finishes
        hamster1.scheduleTask(startTick + 63, "inter_tag_end", () -> {
            hamster1.setFrozenMovement(false);
            hamster1.setCelebrationTarget(null);
        });
    }
}
