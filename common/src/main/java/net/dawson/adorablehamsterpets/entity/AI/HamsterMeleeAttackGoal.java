package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.MeleeAttackGoalAccessor;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundEvent;

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
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        AdorableHamsterPets.LOGGER.trace("[AttackGoal {} Tick {}] Goal stopped.", this.hamster.getId(), this.hamster.getWorld().getTime());
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        super.tick(); // Handles pathing updates and cooldown decrementing
        // We need to call attack() every tick because the superclass doesn't call it automatically
        // if we override tick() without calling super.tick() *first*.
        // However, the actual attack logic is now correctly gated by canAttack().
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.attack(target); // Call attack logic check every tick
        }
    }
}