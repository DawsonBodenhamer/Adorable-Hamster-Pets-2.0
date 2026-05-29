package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.MeleeAttackGoalAccessor;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

public class HamsterMeleeAttackGoal extends MeleeAttackGoal {
    private final HamsterEntity hamster;
    private static final int CUSTOM_ATTACK_COOLDOWN_TICKS = 35;

    public HamsterMeleeAttackGoal(HamsterEntity hamster, double speed, boolean pauseWhenMobIdle) {
        super(hamster, speed, pauseWhenMobIdle);
        this.hamster = hamster;
    }

    @Override
    protected void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            // --- Code inside this block only runs if cooldown is ready AND target is in range/visible ---

            // Reset cooldown using the custom duration
            this.resetCooldown();
            AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Attack condition met (cooldown {}, in range), attacking target {}. Cooldown reset to {}.",
                    this.hamster.getId(), this.hamster.getWorld().getTime(), this.getCooldown(), // Log cooldown *before* reset for clarity
                    target.getId(), this.getMaxCooldown()); // Log the value it's being reset to

            // Play Sound
            SoundEvent attackSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_ATTACK_SOUNDS, this.hamster.getRandom());
            if (attackSound != null) {
                this.hamster.playSound(attackSound, 1.2F, this.hamster.getSoundPitch());
                AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Played attack sound: {}", this.hamster.getId(), this.hamster.getWorld().getTime(), attackSound.getId());
            }

            // Trigger Attack Animation (Server-Side)
            this.hamster.triggerAnimOnServer("mainController", "attack");

            // --- DAMAGE LOGIC ---
            // 1. Create a DamageSource where the hamster is the attacker.
            DamageSource damageSource = this.hamster.getDamageSources().mobAttack(this.hamster);
            // 2. Get the damage amount from the hamster's attributes.
            float damageAmount = (float)this.hamster.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            // 3. Deal the damage to the target using the correct source.
            target.damage(damageSource, damageAmount);

            AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Called tryAttack() on target {}.", this.hamster.getId(), this.hamster.getWorld().getTime(), target.getId());

        }
    }

    @Override
    protected int getMaxCooldown() {
        return CUSTOM_ATTACK_COOLDOWN_TICKS;
    }

    @Override
    protected void resetCooldown() {
        // Cast 'this' to the accessor interface and call the public setter method.
        ((MeleeAttackGoalAccessor) this).setCooldown(this.getMaxCooldown());
    }

    @Override
    public boolean canStart() {
        // Check the master sitting state
        if (this.hamster.isSitting()) {
            return false;
        }
        return super.canStart();
    }

    @Override
    public void start() {
        super.start();
        AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Goal started.", this.hamster.getId(), this.hamster.getWorld().getTime());
        // Use the accessor to set the cooldown to 0, making the hamster able to attack immediately.
        ((MeleeAttackGoalAccessor) this).setCooldown(0);
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Goal stopped.", this.hamster.getId(), this.hamster.getWorld().getTime());
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

                if (this.hamster.squaredDistanceTo(owner) > maxDist * maxDist) {
                    // Hamster too far from owner while trying to attack
                    this.hamster.getNavigation().stop();

                    // Alternate looking between target and owner
                    if ((this.hamster.age / 40) % 2 == 0) { // 40 tick tempo
                        HamsterMovementUtil.faceEntity(this.hamster, target);
                    } else {
                        HamsterMovementUtil.faceEntity(this.hamster, owner);
                    }

                    // Frantic behavior
                    if (this.hamster.getRandom().nextInt(5) == 0 && this.hamster.isOnGround()) {
                        // Small erratic jumps to stay in bounds, pushing slightly towards owner
                        Vec3d bounceVec = owner.getPos().subtract(this.hamster.getPos()).normalize().multiply(0.5);
                        this.hamster.setVelocity(this.hamster.getVelocity().add(bounceVec.x, 0.5, bounceVec.z));
                        this.hamster.velocityDirty = true;

                        // SFX
                        SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, this.hamster.getRandom());
                        if (bounceSound != null) {
                            this.hamster.playSound(bounceSound, 1.0F, this.hamster.getSoundPitch());
                        }
                    }
                }
            }
            this.attack(target); // Call attack logic check every tick
        }
    }
}