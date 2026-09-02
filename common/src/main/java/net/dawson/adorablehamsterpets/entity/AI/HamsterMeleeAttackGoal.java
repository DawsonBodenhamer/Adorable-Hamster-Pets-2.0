package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.MeleeAttackGoalAccessor;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

public class HamsterMeleeAttackGoal extends MeleeAttackGoal {
    private final HamsterEntity hamster;
    private static final int CUSTOM_ATTACK_COOLDOWN_TICKS = 35;

    public HamsterMeleeAttackGoal(HamsterEntity hamster, double speed, boolean pauseWhenMobIdle) {
        super(hamster, speed, pauseWhenMobIdle);
        this.hamster = hamster;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            // --- Code inside this block only runs if cooldown is ready AND target is in range/visible ---

            // Reset cooldown using the custom duration
            this.resetAttackCooldown();
            AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Attack condition met (cooldown {}, in range), attacking target {}. Cooldown reset to {}.",
                    this.hamster.getId(), this.hamster.level().getGameTime(), this.getTicksUntilNextAttack(), // Log cooldown *before* reset for clarity
                    target.getId(), this.getAttackInterval()); // Log the value it's being reset to

            // Play Sound
            SoundEvent attackSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_ATTACK_SOUNDS, this.hamster.getRandom());
            if (attackSound != null) {
                this.hamster.playSound(attackSound, 1.2F, this.hamster.getVoicePitch());
                AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Played attack sound: {}", this.hamster.getId(), this.hamster.level().getGameTime(), attackSound.getLocation());
            }

            // Trigger Attack Animation (Server-Side)
            this.hamster.triggerAnimOnServer("mainController", "attack");

            // --- DAMAGE LOGIC ---
            // 1. Create a DamageSource where the hamster is the attacker.
            DamageSource damageSource = this.hamster.damageSources().mobAttack(this.hamster);
            // 2. Get the damage amount from the hamster's attributes.
            float damageAmount = (float)this.hamster.getAttributeValue(Attributes.ATTACK_DAMAGE);
            // 3. Deal the damage to the target using the correct source.
            target.hurt(damageSource, damageAmount);

            AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Called tryAttack() on target {}.", this.hamster.getId(), this.hamster.level().getGameTime(), target.getId());

        }
    }

    @Override
    protected int getAttackInterval() {
        return CUSTOM_ATTACK_COOLDOWN_TICKS;
    }

    @Override
    protected void resetAttackCooldown() {
        // Cast 'this' to the accessor interface and call the public setter method.
        ((MeleeAttackGoalAccessor) this).setCooldown(this.getAttackInterval());
    }

    @Override
    public boolean canUse() {
        // Check the master sitting state
        if (HamsterMovementUtil.shouldNotMove(this.hamster)) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public void start() {
        super.start();
        AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Goal started.", this.hamster.getId(), this.hamster.level().getGameTime());
        // Use the accessor to set the cooldown to 0, making the hamster able to attack immediately.
        ((MeleeAttackGoalAccessor) this).setCooldown(0);
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Goal stopped.", this.hamster.getId(), this.hamster.level().getGameTime());
        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }

    @Override
    public void tick() {
        super.tick(); // Handles pathing updates and cooldown decrementing

        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            LivingEntity owner = this.hamster.getOwner();
            if (owner != null) {
                // Base tether = 12 blocks + 5 blocks for certain states
                double maxDist = this.hamster.hasGreenBeanBuff() || this.hamster.getAggressionState() == HamsterEntity.AggressionState.MENACE ? 17.0 : 12.0;

                if (this.hamster.distanceToSqr(owner) > maxDist * maxDist) {
                    // Hamster too far from owner while trying to attack
                    this.hamster.getNavigation().stop();

                    // Alternate looking between target and owner
                    if ((this.hamster.tickCount / 40) % 2 == 0) { // 40 tick tempo
                        HamsterMovementUtil.faceEntity(this.hamster, target);
                    } else {
                        HamsterMovementUtil.faceEntity(this.hamster, owner);
                    }

                    // Frantic behavior
                    if (this.hamster.getRandom().nextInt(5) == 0 && this.hamster.onGround()) {
                        // Small erratic jumps to stay in bounds, pushing slightly towards owner
                        Vec3 bounceVec = owner.position().subtract(this.hamster.position()).normalize().scale(0.5);
                        this.hamster.setDeltaMovement(this.hamster.getDeltaMovement().add(bounceVec.x, 0.5, bounceVec.z));
                        this.hamster.hasImpulse = true;

                        // SFX
                        SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, this.hamster.getRandom());
                        if (bounceSound != null) {
                            this.hamster.playSound(bounceSound, 1.0F, this.hamster.getVoicePitch());
                        }
                    }
                }
            }
            this.checkAndPerformAttack(target); // Call attack logic check every tick
        }
    }
}
