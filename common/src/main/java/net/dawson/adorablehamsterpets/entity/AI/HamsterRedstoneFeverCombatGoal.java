package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.RedstoneFeverUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
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
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        LivingEntity target = this.hamster.getTarget();
        return this.hamster.hasRedstoneFever()
                && this.hamster.level().getDifficulty() != Difficulty.PEACEFUL
                && !HamsterMovementUtil.shouldNotMove(this.hamster)
                && RedstoneFeverUtil.isEligibleFeverTarget(this.hamster, target)
                && RedstoneFeverUtil.isWithinTargetingRange(this.hamster, target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.hamster.getTarget();
        boolean canContinue = this.hamster.hasRedstoneFever()
                && this.hamster.level().getDifficulty() != Difficulty.PEACEFUL
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
        this.hamster.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!this.hamster.getNavigation().moveTo(target, 1.5D)) {
            this.clearTarget();
            return;
        }
        double reach = this.hamster.getBbWidth() * 2.0F + target.getBbWidth();
        if (this.attackCooldown > 0 || this.hamster.distanceToSqr(target) > reach * reach) return;

        // --- 3. Commit Melee Hit ---
        this.attackCooldown = ATTACK_COOLDOWN_TICKS;
        SoundEvent attackSound = ModSounds.getRandomSoundFrom(
                ModSounds.HAMSTER_HISS_SOUNDS, this.hamster.getRandom());
        if (attackSound != null) {
            this.hamster.playSound(attackSound, 0.7F, this.hamster.getVoicePitch());
        }
        this.hamster.triggerAnimOnServer("mainController", "attack");
        DamageSource source = this.hamster.damageSources().mobAttack(this.hamster);
        float amount = (float) this.hamster.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (target.hurtServer((net.minecraft.server.level.ServerLevel) target.level(), source, amount) && target instanceof ServerPlayer player) {
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
