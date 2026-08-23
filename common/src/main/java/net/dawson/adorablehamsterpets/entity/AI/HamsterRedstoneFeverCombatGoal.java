package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.RedstoneFeverUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.Difficulty;

import java.util.EnumSet;

public final class HamsterRedstoneFeverCombatGoal extends Goal {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ─────────────────────────────────────────────────────────────────────────────*/

    private static final int ATTACK_COOLDOWN_TICKS = 35;

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ──────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private int attackCooldown;

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ─────────────────────────────────────────────────────────────────────────────*/

    public HamsterRedstoneFeverCombatGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        LivingEntity target = this.hamster.getTarget();
        return this.hamster.hasRedstoneFever()
                && this.hamster.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !HamsterMovementUtil.shouldNotMove(this.hamster)
                && RedstoneFeverUtil.isEligibleFeverTarget(this.hamster, target)
                && RedstoneFeverUtil.isWithinTargetingRange(this.hamster, target);
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = this.hamster.getTarget();
        boolean canContinue = this.hamster.hasRedstoneFever()
                && this.hamster.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !HamsterMovementUtil.shouldNotMove(this.hamster)
                && RedstoneFeverUtil.isEligibleFeverTarget(this.hamster, target)
                && RedstoneFeverUtil.isWithinTargetingRange(this.hamster, target);
        if (!canContinue
                && target != null
                && (!RedstoneFeverUtil.isEligibleFeverTarget(this.hamster, target)
                        || !RedstoneFeverUtil.isWithinTargetingRange(this.hamster, target))) {
            this.clearTarget();
        }
        return canContinue;
    }

    @Override
    public void tick() {
        // --- 1. Resolve Combat State ---
        if (this.attackCooldown > 0) this.attackCooldown--;
        LivingEntity target = this.hamster.getTarget();
        if (!RedstoneFeverUtil.isEligibleFeverTarget(this.hamster, target)
                || !RedstoneFeverUtil.isWithinTargetingRange(this.hamster, target)) {
            this.clearTarget();
            return;
        }

        // --- 2. Pursue Target ---
        this.hamster.getLookControl().lookAt(target, 30.0F, 30.0F);
        if (!this.hamster.getNavigation().startMovingTo(target, 1.5D)) {
            this.clearTarget();
            return;
        }
        double reach = this.hamster.getWidth() * 2.0F + target.getWidth();
        if (this.attackCooldown > 0 || this.hamster.squaredDistanceTo(target) > reach * reach) return;

        // --- 3. Commit Melee Hit ---
        this.attackCooldown = ATTACK_COOLDOWN_TICKS;
        SoundEvent attackSound = ModSounds.getRandomSoundFrom(
                ModSounds.HAMSTER_HISS_SOUNDS, this.hamster.getRandom());
        if (attackSound != null) {
            this.hamster.playSound(attackSound, 0.5F, this.hamster.getSoundPitch());
        }
        this.hamster.triggerAnimOnServer("mainController", "attack");
        DamageSource source = this.hamster.getDamageSources().mobAttack(this.hamster);
        float amount = (float) this.hamster.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (target.damage(source, amount) && target instanceof ServerPlayerEntity player) {
            ModCriteria.REDSTONE_FEVER_DISCOVERED.get().trigger(player);
        }
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        // Goal relinquishes movement when burst or cure interrupts it
        this.hamster.getNavigation().stop();
        this.hamster.setActiveCustomGoalName("None");
    }

    private void clearTarget() {
        this.hamster.getNavigation().stop();
        if (this.hamster.getTarget() != null) this.hamster.setTarget(null);
    }
}
