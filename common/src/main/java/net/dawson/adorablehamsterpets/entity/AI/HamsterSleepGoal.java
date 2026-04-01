package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.EnumSet;
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
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        // Only wild hamsters sleep via this goal
        if (this.hamster.isTamed() ||
                this.hamster.isSleeping() ||
                this.hamster.isSitting() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isPlayingTag()) {
            return false;
        }
        if (!this.hamster.getWorld().isDay()) {
            return false;
        }
        if (!this.hamster.isOnGround()) {
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
        boolean threatNearby = !this.hamster.getWorld().getOtherEntities(
                this.hamster,
                this.hamster.getBoundingBox().expand(radius),
                this::isThreat
        ).isEmpty();
        return !threatNearby;
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.isTamed() || !this.hamster.getWorld().isDay()) {
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
        boolean threatNearby = !this.hamster.getWorld().getOtherEntities(
                this.hamster,
                this.hamster.getBoundingBox().expand(radius),
                this::isThreat
        ).isEmpty();
        return !threatNearby;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());

        // --- Set Sleep State ---
        this.hamster.getNavigation().stop();
        this.hamster.setTarget(null);
        this.isActuallySleeping = false;

        // Randomized delay for wild babies
        if (this.hamster.isBaby()) {
            this.delayTimer = this.hamster.getRandom().nextBetween(20, 60); // 1 to 3 sec
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

        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
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
        if (!this.hamster.getWorld().isClient()) {
            // Randomly select sleep pose
            int choice = this.hamster.getRandom().nextInt(3);
            String settleAnimId;
            String deepSleepAnimIdForTracker;

            switch (choice) {
                case 0 -> {
                    settleAnimId = "anim_hamster_stand_settle_sleep1";
                    deepSleepAnimIdForTracker = "anim_hamster_sleep_pose1";
                }
                case 1 -> {
                    settleAnimId = "anim_hamster_stand_settle_sleep2";
                    deepSleepAnimIdForTracker = "anim_hamster_sleep_pose2";
                }
                default -> { // case 2
                    settleAnimId = "anim_hamster_stand_settle_sleep3";
                    deepSleepAnimIdForTracker = "anim_hamster_sleep_pose3";
                }
            }

            // Store deep sleep animation name
            this.hamster.getDataTracker().set(HamsterEntity.CURRENT_DEEP_SLEEP_ANIM_ID, deepSleepAnimIdForTracker);

            // Trigger corresponding settle anim
            this.hamster.triggerAnimOnServer("mainController", settleAnimId);

            // Sound effects
            this.hamster.triggerSettleEffects(0.24f, 14, 0.27f);
            SoundEvent sleepSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SLEEP_SOUNDS, this.hamster.getRandom());
            if (sleepSound != null) {
                this.hamster.getWorld().playSound(
                        null,
                        this.hamster.getBlockPos(),
                        sleepSound,
                        SoundCategory.NEUTRAL,
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
        if (entity instanceof HostileEntity) {
            return true;
        }
        if (entity instanceof PlayerEntity) {
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

        if (this.hamster.getWorld() instanceof ServerWorld serverWorld) {
            // Retrieve parent entity
            Entity parent = serverWorld.getEntity(parentUuid);

            // If parent exists and is alive, check distance
            if (parent != null && parent.isAlive()) {
                // Wake up if parent is more than 4 blocks away
                return this.hamster.squaredDistanceTo(parent) > 16.0;
            }
        }
        return false;
    }
}