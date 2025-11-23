package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.List;

public class HamsterMateGoal extends Goal {
    private final HamsterEntity hamster;
    private HamsterEntity targetMate;
    private final double speed;
    private int timer;

    public HamsterMateGoal(HamsterEntity hamster, double speed) {
        this.hamster = hamster;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Check the master sitting state
        if (this.hamster.isSitting()) {
            return false;
        }
        boolean inLove = this.hamster.isInCustomLove();
        if (inLove) {
            this.targetMate = this.getNearbyMate();
            return this.targetMate != null;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return this.targetMate != null
                && this.targetMate.isAlive()
                && this.hamster.isInCustomLove()
                && this.timer < 60;
        // let's say 3 seconds for them to breed
    }

    @Override
    public void start() {
        this.timer = 0;
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
        this.targetMate = null;
    }

    @Override
    public void tick() {
        this.hamster.getNavigation().startMovingTo(this.targetMate, this.speed);
        this.hamster.getLookControl().lookAt(this.targetMate, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);
        this.timer++;

        if (this.timer >= 60) {
            this.breed();
        }
    }

    private HamsterEntity getNearbyMate() {
        AdorableHamsterPets.LOGGER.trace("[MateGoal {} Tick {}] getNearbyMate() searching...", this.hamster.getId(), this.hamster.getWorld().getTime());
        List<HamsterEntity> candidates = this.hamster.getWorld().getEntitiesByClass(
                HamsterEntity.class,
                this.hamster.getBoundingBox().expand(8.0D),
                // --- Start of Predicate Lambda ---
                h -> { // Check each potential mate 'h'
                    boolean potential = h != this.hamster && h.isInCustomLove() && h.getBreedingAge() == 0;
                    // Log check for each candidate inside the lambda
                    AdorableHamsterPets.LOGGER.trace("  - Checking candidate {}: isInCustomLove={}, getBreedingAge={}, isSelf={}, Result={}", h.getId(), h.isInCustomLove(), h.getBreedingAge(), h == this.hamster, potential);
                    return potential; // Return the result of the check for this candidate
                }
        );

        // Find any candidate from the filtered list
        HamsterEntity found = candidates.stream().findAny().orElse(null);

        // Log the final result after checking all candidates
        AdorableHamsterPets.LOGGER.trace("[MateGoal {} Tick {}] getNearbyMate() found: {}", this.hamster.getId(), this.hamster.getWorld().getTime(), found != null ? found.getId() : "null");

        return found;
    }

    private void breed() {

        // --- Use Config Value for Breeding Cooldown ---
        final AhpConfig config = AdorableHamsterPets.CONFIG;
        int cooldown = config.breedingCooldownTicks.get();
        this.hamster.setBreedingAge(cooldown);
        this.targetMate.setBreedingAge(cooldown);
        // --- End Use Config Value ---

        this.hamster.customLoveTimer = 0;
        this.targetMate.customLoveTimer = 0;

        // Create baby
        HamsterEntity baby = (HamsterEntity)this.hamster.createChild((ServerWorld)this.hamster.getWorld(), this.targetMate);
        if (baby != null) {
            // Position baby
            baby.refreshPositionAndAngles(this.hamster.getX(), this.hamster.getY(), this.hamster.getZ(), 0.0F, 0.0F);
            this.hamster.getWorld().spawnEntity(baby);
        }
    }
}