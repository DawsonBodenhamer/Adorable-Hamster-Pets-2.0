package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterGeneticsAdvancementUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class HamsterMateGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int BREEDING_TIME_TICKS = 60;
    private static final int CELEBRATION_DELAY_BASE_TICKS = 20;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private final double speed;
    private HamsterEntity targetMate;
    private int timer;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterMateGoal(HamsterEntity hamster, double speed) {
        this.hamster = hamster;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        // Master siting check
        if (this.hamster.isSitting()) {
            return false;
        }

        // Check if already in love and find partner
        if (this.hamster.isInCustomLove()) {
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
                && this.targetMate.isInCustomLove()
                && this.timer < BREEDING_TIME_TICKS;
    }

    @Override
    public void start() {
        this.timer = 0;
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        // Clear debug HUD
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
        this.targetMate = null;
    }

    @Override
    public void tick() {
        // Move towards partner; stare lovingly etc. ;D
        this.hamster.getNavigation().startMovingTo(this.targetMate, this.speed);
        HamsterMovementUtil.faceEntity(this.hamster, this.targetMate);
        this.timer++;

        if (this.timer >= BREEDING_TIME_TICKS) {
            this.breed();
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterEntity getNearbyMate() {
        // Scan area for willing partners
        List<HamsterEntity> candidates = this.hamster.getWorld().getEntitiesByClass(
                HamsterEntity.class,
                this.hamster.getBoundingBox().expand(8.0D),
                hamster -> hamster != this.hamster && hamster.isInCustomLove() && hamster.getBreedingAge() == 0
        );

        return candidates.stream().findAny().orElse(null);
    }

    private void breed() {
        // --- 1. Safety Check ---
        if (!this.hamster.isInCustomLove() || !this.targetMate.isInCustomLove()) return; // Prevent love triangles

        // --- 2. Cooldown Application ---
        final AhpConfig config = AdorableHamsterPets.CONFIG;
        int cooldown = config.breedingCooldownSeconds.get() * 20;
        this.hamster.setBreedingAge(cooldown);
        this.targetMate.setBreedingAge(cooldown);

        // Clear love status
        this.hamster.customLoveTimer = 0;
        this.targetMate.customLoveTimer = 0;
        this.hamster.setInLove(false);
        this.targetMate.setInLove(false);

        // --- 3. Update Breeding Counters ---
        this.hamster.timesBred++;
        this.targetMate.timesBred++;

        // --- 4. Determine Litter Size ---
        int min = config.litterSizeMin.get();
        int max = Math.max(min, config.litterSizeMax.get());
        int litterSize = min >= max ? min : this.hamster.getRandom().nextBetween(min, max);

        // --- 5. Spawn Litter ---
        ServerWorld world = (ServerWorld) this.hamster.getWorld();
        List<HamsterEntity> spawnedBabies = new ArrayList<>();

        for (int i = 0; i < litterSize; i++) {
            HamsterEntity baby = (HamsterEntity) this.hamster.createChild(world, this.targetMate);
            if (baby != null) {
                // Random scatter so they don't merge into mega-baby
                double offsetX = (this.hamster.getRandom().nextDouble() - 0.5) * 0.5;
                double offsetZ = (this.hamster.getRandom().nextDouble() - 0.5) * 0.5;

                baby.refreshPositionAndAngles(this.hamster.getX() + offsetX, this.hamster.getY(), this.hamster.getZ() + offsetZ, 0.0F, 0.0F);
                world.spawnEntity(baby);
                spawnedBabies.add(baby);

                // --- Track Genetics for Advancements ---
                // Credit owner(s) of the parents
                if (this.hamster.getOwner() instanceof ServerPlayerEntity sp1) {
                    HamsterGeneticsAdvancementUtil.trackBredHamster(sp1, baby);
                }
                if (this.targetMate.getOwner() instanceof ServerPlayerEntity sp2 && sp2 != this.hamster.getOwner()) {
                    HamsterGeneticsAdvancementUtil.trackBredHamster(sp2, baby);
                }
            }
        }

        // --- 6. Schedule Post-Breeding Feedback ---
        if (!spawnedBabies.isEmpty()) {
            // Grab the first baby, and the second baby (if it exists)
            HamsterEntity babyA = spawnedBabies.get(0);
            HamsterEntity babyB = spawnedBabies.size() > 1 ? spawnedBabies.get(1) : babyA;

            // Randomize which parent looks at which baby so it feels organic
            if (this.hamster.getRandom().nextBoolean()) {
                this.scheduleCelebration(this.hamster, babyA);
                this.scheduleCelebration(this.targetMate, babyB);
            } else {
                this.scheduleCelebration(this.hamster, babyB);
                this.scheduleCelebration(this.targetMate, babyA);
            }
        }
    }

    private void scheduleCelebration(HamsterEntity parent, HamsterEntity targetBaby) {
        long currentTime = parent.getWorld().getTime();

        // Lock out AI goals and face designated baby
        parent.setCelebratingBaby(true);
        parent.getNavigation().stop();
        if (targetBaby != null && targetBaby.isAlive()) {
            parent.setCelebrationTarget(targetBaby);
            HamsterMovementUtil.faceEntity(parent, targetBaby);
        }

        // Start a bit after breeding finishes with slight random offset
        long startTick = currentTime + CELEBRATION_DELAY_BASE_TICKS + parent.getRandom().nextInt(40);

        parent.scheduleTask(startTick, "breed_celebration_start", () -> {
            // Trigger animation
            parent.triggerAnimOnServer("mainController", "anim_hamster_crouch_and_investigate");

            // Particles
            ParticleEffectsUtil.spawnParticlesOnEntity(
                    parent,
                    ParticleTypes.HEART,
                    5,
                    0.5,
                    0.5,
                    0.0,
                    0.5
            );

            // Sound Effects
            SoundEvent affectionSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_AFFECTION_SOUNDS, parent.getRandom());
            if (affectionSound != null) {
                parent.playSound(affectionSound, 1.0f, parent.getSoundPitch());
            }
        });

        // Release AI lock after animation finishes
        parent.scheduleTask(startTick + 60, "breed_celebration_end", () -> {
            parent.setCelebratingBaby(false);
            parent.setCelebrationTarget(null);
        });
    }
}