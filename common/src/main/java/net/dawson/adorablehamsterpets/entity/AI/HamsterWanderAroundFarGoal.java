package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.EntityTargetingUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterPlacementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class HamsterWanderAroundFarGoal extends WaterAvoidingRandomStrollGoal {
    private final HamsterEntity hamster;
    private static final double BUFFED_WANDER_SPEED = 0.9D;

    public HamsterWanderAroundFarGoal(HamsterEntity hamster, double speed) {
        super(hamster, speed);
        this.hamster = hamster;
        this.setInterval(110); // Chance for non-buffed state
    }

    public HamsterWanderAroundFarGoal(HamsterEntity hamster, double speed, float probability) {
        super(hamster, speed, probability);
        this.hamster = hamster;
        this.setInterval(110); // Chance for non-buffed state
    }

    @Override
    public boolean canUse() {
        // --- 1. Initial State Checks ---
        if (HamsterMovementUtil.shouldNotMove(this.hamster)
                || this.hamster.hasRedstoneFever()
                || this.hamster.getActiveCustomGoalName().equals("Escaping Water")) {
            return false;
        }

        // Prevent wandering if mutual gaze established and not zooming
        if (this.hamster.hasMutualGaze && !this.hamster.hasGreenBeanBuff()) {
            return false;
        }

        // --- 2. "Zoomies" vs. Normal Activation Logic ---
        if (this.hamster.hasGreenBeanBuff()) {

            // For zoomies, use a high-frequency check and bypass the superclass's internal cooldown.
            if (this.mob.getRandom().nextInt(3) != 0) {
                AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Zoomies): FAILED - On cooldown.", this.hamster.getId());
                return false;
            }
            // We must manually find a target here because we are not calling super.canStart().
            Vec3 target = getPosition();
            if (target == null) {
                AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Zoomies): FAILED - No valid target found.", this.hamster.getId());
                return false;
            }
            // Set the target coordinates that the superclass would normally set.
            this.wantedX = target.x;
            this.wantedY = target.y;
            this.wantedZ = target.z;
            AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Zoomies): SUCCEEDED. Target: ({}, {}, {})", this.hamster.getId(), String.format("%.2f", target.x), String.format("%.2f", target.y), String.format("%.2f", target.z));
            return true; // A valid target was found.
        } else {
            // --- Normal Wandering ---
            int interval = Configs.AHP_MAIN.wanderChanceInterval.get();

            // If configured to 0, disable wandering entirely.
            if (interval <= 0) {
                return false;
            }

            // Reduce wander frequency by 50% if dancing to music disc
            if (this.hamster.isDancing()) {
                interval *= 2;
            }

            // Dynamically update the chance based on config
            this.setInterval(interval);

            // Defer to the superclass, which includes the cooldown and random chance check.
            boolean canStartNormal = super.canUse();
            AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] canStart (Normal): Result: {}", this.hamster.getId(), canStartNormal);
            return canStartNormal;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (HamsterMovementUtil.shouldNotMove(this.hamster)
                || this.hamster.hasRedstoneFever()) return false;

        if (this.hamster.hasGreenBeanBuff()) {
            // For zoomies, the goal should now stop if it's interrupted OR if it has reached its destination.
            // This allows the canStart() cooldown to be checked again.
            return !this.mob.getNavigation().isDone()
                    && !this.hamster.getActiveCustomGoalName().equals("Escaping Water");
        } else {
            // Stop wandering if mutual eye contact established
            if (this.hamster.hasMutualGaze) {
                return false;
            }
            // For normal wandering, use default behavior
            return super.canContinueToUse() && !this.hamster.isCelebratingBaby();
        }
    }

    @Override
    public void tick() {
        // For "zoomies" mode, if the hamster reaches its destination, immediately find a new one.
        if (this.hamster.hasGreenBeanBuff() && this.mob.getNavigation().isDone()) {
            AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] tick (Zoomies): Navigation is idle. Finding new target.", this.hamster.getId());
            Vec3 newTarget = this.getPosition();
            if (newTarget != null) {
                // Apply 50% speed reduction if dancing
                double activeSpeed = BUFFED_WANDER_SPEED;
                if (this.hamster.isDancing()) {
                    activeSpeed *= 0.5;
                }
                this.mob.getNavigation().moveTo(newTarget.x, newTarget.y, newTarget.z, activeSpeed);
            }
        }
        // For normal wandering, the superclass tick is empty, so we don't need to call it.
    }


    @Override
    public void start() {
        // --- Determine Speed Dynamically ---
        double currentSpeed = this.hamster.hasGreenBeanBuff() ? BUFFED_WANDER_SPEED : this.speedModifier;

        // Reduce speed by 50% if dancing
        if (this.hamster.isDancing()) {
            currentSpeed *= 0.5;
        }

        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, currentSpeed);

        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName() + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
        AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] start: Goal has started. IsBuffed: {}", this.hamster.getId(), this.hamster.hasGreenBeanBuff());
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        // --- 1. Priority: Green Bean Buff "Zoomies" ---
        if (this.hamster.hasGreenBeanBuff()) {
            // Convert the BlockPos from our precise helper to a Vec3d for the goal.
            return getPreciseZoomiesTarget().map(Vec3::atCenterOf).orElse(null);
        }

        // --- 2. Priority: Wander Mode (around bed) ---
        if (this.hamster.isWanderModeActive()) {
            Optional<GlobalPos> bedPosOptional = this.hamster.getLinkedBedPos();
            if (bedPosOptional.isPresent()) {
                GlobalPos bedGlobalPos = bedPosOptional.get();
                if (this.hamster.level().dimension() == bedGlobalPos.dimension()) {
                    BlockPos bedPos = bedGlobalPos.pos();
                    BlockEntity be = this.hamster.level().getBlockEntity(bedPos);
                    if (be instanceof HamsterBedBlockEntity bedEntity) {
                        WanderDistance distance = bedEntity.getWanderDistance();
                        int radius = switch (distance) {
                            case NEAR -> Configs.AHP_MAIN.wanderDistanceNear.get();
                            case FAR -> Configs.AHP_MAIN.wanderDistanceFar.get();
                            default -> Configs.AHP_MAIN.wanderDistanceMedium.get();
                        };

                        // If hamster is outside its radius, path back towards the bed
                        if (this.hamster.blockPosition().distSqr(bedPos) > radius * radius) {
                            Vec3 directionToBed = Vec3.atCenterOf(bedPos).subtract(this.hamster.position());
                            // Use findTo with a reasonable range to find a point in the direction of the bed
                            return LandRandomPos.getPosTowards(this.mob, 7, 7, directionToBed);
                        } else {
                            // Hamster is inside the radius, find a random point centered on the bed
                            for (int i = 0; i < 10; ++i) { // Try up to 10 times
                                int dx = this.hamster.getRandom().nextInt(2 * radius + 1) - radius;
                                int dz = this.hamster.getRandom().nextInt(2 * radius + 1) - radius;

                                BlockPos potentialTarget = bedPos.offset(dx, 0, dz);

                                if (bedPos.distSqr(potentialTarget) <= radius * radius) {
                                    BlockPos validatedPos = LandRandomPos.movePosUpOutOfSolid(this.mob, potentialTarget);
                                    if (validatedPos != null) {
                                        return Vec3.atBottomCenterOf(validatedPos);
                                    }
                                }
                            }
                            return null; // Failed to find a point
                        }
                    }
                }
            }
        }

        // --- 3. Fallback: Default Wandering ---
        return super.getPosition();
    }

    /**
     * Calculates a precise point on a circle around the owner and finds the nearest safe,
     * reachable block.
     *
     * @return An Optional containing the safe BlockPos, or empty if none is found.
     */
    private Optional<BlockPos> getPreciseZoomiesTarget() {
        if (!(this.hamster.getOwner() instanceof Player owner)) {
            return Optional.empty();
        }

        int radiusModifier = this.hamster.getZoomiesRadiusModifier();

        // 3-5 block-radius, 40-70 degree angle steps
        return HamsterMovementUtil.findOrbitingTarget(
                this.hamster,
                owner,
                3.0 + radiusModifier,
                5.0 + radiusModifier,
                40,
                70
        );
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
        AdorableHamsterPets.LOGGER.trace("[WanderGoal-{}] stop: Goal has stopped.", this.hamster.getId());
    }
}
