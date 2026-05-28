package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    private boolean hasPlayedStartEffects = false;

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
        if (!this.hamster.isTamed()
                || this.hamster.isSitting()
                || this.hamster.isSleeping()
                || this.hamster.isKnockedOut()
                || this.hamster.isHoldingMouthItem()
                || this.hamster.isCelebratingDiamond()
                || this.hamster.isSulking()
                || this.hamster.isCelebratingBaby())
            return false;

        // --- 4. Context Check ---
        // Must be currently "sustaining eye contact" with the player
        if (!HamsterLookAtEntityGoal.class.getSimpleName().equals(this.hamster.getActiveCustomGoalDebugName())) {
            return false;
        }

        // Find nearest player within 5 blocks
        PlayerEntity player = this.hamster.getWorld().getClosestPlayer(this.hamster, 5.0);
        if (player == null) return false;

        // Prevent start if player is looking inside a GUI
        if (player.currentScreenHandler != player.playerScreenHandler) {
            return false;
        }

        this.targetPlayer = player;

        // --- 5. Sneak Check & Manual Initiation ---
        // 8 toggles = press, release, press, release, press, release, press, release
        boolean isSpammingSneak = PlayerGestureUtil.isSpammingSneak(player, 8);

        // Prevent automatic tag initiation if sneaking
        if (player.isSneaking() && !isSpammingSneak) {
            return false;
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

        // --- 9. Final Initiation Check ---
        // Bypass RNG if explicitly requesting game
        if (isSpammingSneak) {
            PlayerGestureUtil.consumeSneakSpam(player);
            return true;
        }

        // RNG Check
        return this.hamster.getRandom().nextInt(Configs.AHP.tagChanceDenominator.get()) == 0;
    }

    @Override
    public void start() {
        this.hamster.setPlayingTag(true);
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());

        // Feedback
        this.hamster.triggerAnimOnServer("mainController", "attack");
        this.hamster.getWorld().playSound(null, this.hamster.getBlockPos(), ModSounds.HAMSTER_SLAP.get(), SoundCategory.NEUTRAL, 0.5f, 1.0f);

        if (!this.hamster.getWorld().isClient() && this.targetPlayer instanceof ServerPlayerEntity serverPlayer) {
            // Instant feedback
            MiscUtil.PlayerPhysicsUtil.applyKnockback(serverPlayer, this.hamster.getPos());
            serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 1, false, false, false));

            // Randomly select one of 4 messages
            int msgIndex = this.hamster.getRandom().nextInt(4) + 1; // 1 to 4
            serverPlayer.sendMessage(Text.translatable("message.adorablehamsterpets.tag_game_start." + msgIndex).formatted(Formatting.WHITE), true);

            // Delayed feedback
            this.hamster.scheduleTask(this.hamster.getWorld().getTime() + 20, "tag_game_start_effects", () -> {
                if (this.hamster.isPlayingTag() && this.targetPlayer != null && this.targetPlayer.isAlive()) {
                    SoundEvent startSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BEG_SOUNDS, this.hamster.getRandom());
                    if (startSound != null) {
                        this.hamster.playSound(startSound, 1.0f, 1.2f);
                    }
                    this.hamster.getWorld().playSound(null, this.hamster.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 0.5f, 1.5f);
                    ParticleEffectsUtil.spawnParticlesOnEntity(
                            this.hamster,
                            ParticleTypes.HEART,
                            3,
                            0.5,
                            0.5,
                            0.0,
                            0.2
                    );
                }
            });
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