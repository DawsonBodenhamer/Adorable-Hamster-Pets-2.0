package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.EntityTargetingUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;

public class HamsterTagGoal extends Goal {

    private final HamsterEntity hamster;
    private PlayerEntity targetPlayer;

    private enum State {
        FLEEING,
        TAUNTING
    }

    private State currentState = State.FLEEING;

    public HamsterTagGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        // --- 1. Config Check ---
        if (!Configs.AHP.enableTagGame) return false;

        // --- 2. Cooldown Check ---
        if (this.hamster.getWorld().getTime() < this.hamster.tagGameCooldownEndTick) return false;

        // --- 3. Entity State Checks ---
        // TODO: Prevent start if hamster is being petted by owner. (Petting is future planned feature)
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() ||
                this.hamster.isHoldingMouthItem() || this.hamster.isThrown()) return false;

        // --- 4. Context Check ---
        // Must be currently "sustaining eye contact" with the player
        if (!HamsterLookAtEntityGoal.class.getSimpleName().equals(this.hamster.getActiveCustomGoalDebugName())) {
            return false;
        }

        // Find nearest player within 5 blocks
        PlayerEntity player = this.hamster.getWorld().getClosestPlayer(this.hamster, 5.0);
        if (player == null) return false;
        this.targetPlayer = player;

        // --- 5. Taming Check ---
        // If untamed and player is holding taming food, prioritize taming over tag
        if (!this.hamster.isTamed()) {
            if (ConfigDataCache.isTamingFood(player.getMainHandStack()) ||
                    ConfigDataCache.isTamingFood(player.getOffHandStack())) {
                return false;
            }
        }

        // --- 6. Permission Check ---
        boolean isOwner = this.hamster.isOwner(player);
        if (!isOwner && !Configs.AHP.allowStrangerTag) return false;

        // --- 7. Player Limit Check ---
        if (player instanceof PlayerEntityAccessor accessor) {
            if (!accessor.ahp$canPlayTagGame()) return false;
        }

        // --- 8. Reciprocity Check (Eye Contact) ---
        // Hamster must be facing player
        if (!EntityTargetingUtil.isFacing(this.hamster, player, 0.8)) return false;
        // Player must be looking at hamster
        if (!EntityTargetingUtil.isLookingAt(player, this.hamster, 5.0, 0.0)) return false;

        // --- 9. RNG Check ---
        return this.hamster.getRandom().nextInt(Configs.AHP.tagGameChanceDenominator.get()) == 0;
    }

    @Override
    public void start() {
        this.hamster.setPlayingTag(true);
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());

        // Play start sound
        SoundEvent startSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BEG_SOUNDS, this.hamster.getRandom());
        if (startSound != null) {
            this.hamster.playSound(startSound, 1.0f, 1.2f);
        }

        // Init timeout from config
        int minDuration = Configs.AHP.minMiniGameFleeDurationSeconds.get() * 20;
        int maxDuration = Configs.AHP.maxMiniGameFleeDurationSeconds.get() * 20;
        int gameDuration = this.hamster.getRandom().nextBetween(minDuration, maxDuration);

        this.hamster.setGenericInteractionTimer(gameDuration);
        this.currentState = State.FLEEING;
    }

    @Override
    public boolean shouldContinue() {
        if (!this.hamster.isPlayingTag()) return false; // Terminated by interaction
        if (this.hamster.getGenericInteractionTimer() <= 0) return false; // Timed out
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) return false;
        if (this.hamster.isSitting() || this.hamster.isKnockedOut()) return false;
        return true;
    }

    @Override
    public void stop() {
        this.hamster.setPlayingTag(false);
        this.hamster.setTaunting(false); // Stop animation
        this.hamster.getNavigation().stop();
        this.hamster.setGenericInteractionTimer(0);
        this.targetPlayer = null;

        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        // Timer Logic
        int timer = this.hamster.getGenericInteractionTimer();
        if (timer > 0) this.hamster.setGenericInteractionTimer(timer - 1);

        HamsterMovementUtil.faceEntity(this.hamster, this.targetPlayer);

        // Config Values
        double minFleeDist = Configs.AHP.minMiniGameFleeDistance.get();
        double maxFleeDist = Configs.AHP.maxMiniGameFleeDistance.get();

        switch (this.currentState) {
            case FLEEING -> {
                this.hamster.setTaunting(false);

                if (HamsterMovementUtil.shouldStopFleeing(this.hamster, this.targetPlayer, maxFleeDist)) {
                    this.currentState = State.TAUNTING;
                    this.hamster.getNavigation().stop();
                } else if (HamsterMovementUtil.shouldFlee(this.hamster, this.targetPlayer, minFleeDist)) {
                    Vec3d fleePos = HamsterMovementUtil.findFleePosition(this.hamster, this.targetPlayer, minFleeDist, maxFleeDist);
                    if (fleePos != null) {
                        this.hamster.getNavigation().startMovingTo(fleePos.x, fleePos.y, fleePos.z, 1.5D);
                    }
                }
            }
            case TAUNTING -> {
                if (HamsterMovementUtil.shouldFlee(this.hamster, this.targetPlayer, minFleeDist)) {
                    this.currentState = State.FLEEING;
                    this.hamster.setTaunting(false);
                } else {
                    this.hamster.setTaunting(true);
                    this.hamster.getNavigation().stop();
                }
            }
        }
    }
}