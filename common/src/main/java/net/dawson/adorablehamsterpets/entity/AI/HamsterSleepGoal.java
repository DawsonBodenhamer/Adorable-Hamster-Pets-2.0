package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterPoseUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class HamsterSleepGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int CHECK_INTERVAL = 20;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private int checkTimer = 0;
    private int delayTimer = 0;
    private boolean isActuallySleeping = false;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterSleepGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        // Control movement and look to prevent interference
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        // Only wild hamsters sleep via this goal
        if (this.hamster.isTame()
                || this.hamster.hasRedstoneFever()
                || HamsterMovementUtil.shouldNotMove(this.hamster)
                || this.hamster.isPlayingTag()) {
            return false;
        }
        if (!this.hamster.level().isDay()) {
            return false;
        }
        if (!this.hamster.onGround()) {
            return false;
        }

        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = CHECK_INTERVAL;

        // Check if parent is leaving them behind
        if (isParentTooFar()) {
            return false;
        }

        double radius = 5.0;
        boolean threatNearby = !this.hamster.level().getEntities(
                this.hamster,
                this.hamster.getBoundingBox().inflate(radius),
                this::isThreat
        ).isEmpty();
        return !threatNearby;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.hamster.isTame()
                || this.hamster.hasRedstoneFever()
                || !this.hamster.level().isDay()) {
            return false;
        }

        // Throttle check for performance
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return true;
        }
        this.checkTimer = CHECK_INTERVAL;

        // Check if parent is leaving them behind
        if (isParentTooFar()) {
            return false;
        }

        double radius = 5.0;
        boolean threatNearby = !this.hamster.level().getEntities(
                this.hamster,
                this.hamster.getBoundingBox().inflate(radius),
                this::isThreat
        ).isEmpty();
        return !threatNearby;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());

        // --- Set Sleep State ---
        this.hamster.getNavigation().stop();
        this.hamster.setTarget(null);
        this.isActuallySleeping = false;

        // Randomized delay for wild babies
        if (this.hamster.isBaby()) {
            this.delayTimer = this.hamster.getRandom().nextIntBetweenInclusive(20, 60); // 1 to 3 sec
        } else {
            this.delayTimer = 0;
            fallAsleep(); // Sleep instantly if adult
        }
    }

    @Override
    public void tick() {
        if (this.delayTimer > 0 && !this.isActuallySleeping) {
            this.delayTimer--;
            if (this.delayTimer == 0) {
                fallAsleep();
            }
        }
    }

    @Override
    public void stop() {
        // If hamster was sleeping, trigger wake up animation and sound
        if (this.isActuallySleeping && this.hamster.isSleeping()) {
            this.hamster.triggerWakeUpFromSleepAnimation(false);
        }

        this.hamster.setSleeping(false);
        this.hamster.setInSittingPose(false);
        this.isActuallySleeping = false;
        this.delayTimer = 0;
        this.checkTimer = 0;

        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void fallAsleep() {
        this.isActuallySleeping = true;
        this.hamster.setSleeping(true);
        this.hamster.setInSittingPose(true); // Prevent other AI movement

        // --- Animation ---
        if (!this.hamster.level().isClientSide()) {
            // Select sleep pose based on personality
            int personalityId = this.hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
            String settleAnimId = HamsterPoseUtil.getSettleSleepAnimId(personalityId, false);
            String deepSleepAnimIdForTracker = HamsterPoseUtil.getDeepSleepAnimId(personalityId);

            // Store deep sleep animation name
            this.hamster.getEntityData().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, deepSleepAnimIdForTracker);

            // Trigger corresponding settle anim
            this.hamster.triggerAnimOnServer("mainController", settleAnimId);

            // Sound effects
            this.hamster.triggerSettleEffects(0.24f, 14, 0.27f);
            SoundEvent sleepSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SLEEP_SOUNDS, this.hamster.getRandom());
            if (sleepSound != null) {
                this.hamster.level().playSound(
                        null,
                        this.hamster.blockPosition(),
                        sleepSound,
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                );
            }
        }
    }

    /**
     * Determines if the given entity is considered a threat to a sleeping wild hamster,
     * which would cause it to wake up.
     */
    private boolean isThreat(Entity entity) {
        if (entity instanceof Monster) {
            return true;
        }
        if (entity instanceof Player) {
            // Baby hamsters don't care about nearby players
            return !this.hamster.isBaby();
        }
        return false;
    }

    /**
     * Checks if the baby's parent has wandered too far away.
     * Prevents wild babies from sleeping while their parent leaves them behind.
     */
    private boolean isParentTooFar() {
        if (!this.hamster.isBaby()) return false;

        UUID parentUuid = this.hamster.getParentUuid();
        if (parentUuid == null) return false;

        if (this.hamster.level() instanceof ServerLevel serverWorld) {
            // Retrieve parent entity
            Entity parent = serverWorld.getEntity(parentUuid);

            // If parent exists and is alive, check distance
            if (parent != null && parent.isAlive()) {
                // Wake up if parent is more than 4 blocks away
                return this.hamster.distanceToSqr(parent) > 16.0;
            } else {
                // Check if parent is on a player's shoulder
                for (Player player : serverWorld.players()) {
                    if (player instanceof PlayerEntityAccessor accessor) {
                        if (isParentOnShoulder(accessor, parentUuid)) {
                            return this.hamster.distanceToSqr(player) > 16.0;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isParentOnShoulder(PlayerEntityAccessor accessor, UUID parentUuid) {
        if (!accessor.hasAnyShoulderHamster()) return false;
        for (ShoulderLocation loc : ShoulderLocation.values()) {
            net.minecraft.nbt.CompoundTag nbt = accessor.getShoulderHamster(loc);
            if (!nbt.isEmpty()) {
                Optional<HamsterState> state = HamsterState.fromNbt(nbt);
                if (state.isPresent() && state.get().entityUuid().equals(parentUuid)) {
                    return true;
                }
            }
        }
        return false;
    }
}
