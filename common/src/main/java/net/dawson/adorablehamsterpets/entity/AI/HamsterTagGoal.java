package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class HamsterTagGoal extends Goal {

    private final HamsterEntity hamster;
    private Player targetPlayer;

    private enum State {
        FLEEING,
        TAUNTING
    }

    private State currentState = State.FLEEING;
    private boolean hasPlayedStartEffects = false;

    public HamsterTagGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // --- 1. Config Check ---
        if (!Configs.AHP_MAIN.enableTagGame) return false;

        // --- 2. Cooldown Check ---
        if (this.hamster.level().getGameTime() < this.hamster.tagGameCooldownEndTick) return false;

        // --- 3. Entity State Checks ---
        if (!this.hamster.isTame()
                || HamsterMovementUtil.shouldNotMove(this.hamster)
                || this.hamster.isHoldingMouthItem())
            return false;

        // --- 4. Context Check ---
        // Must be currently "sustaining eye contact" with the player
        if (!HamsterLookAtEntityGoal.class.getSimpleName().equals(this.hamster.getActiveCustomGoalName())) {
            return false;
        }

        // Find nearest player within 5 blocks
        Player player = this.hamster.level().getNearestPlayer(this.hamster, 5.0);
        if (player == null) return false;

        // Prevent start if player is looking inside a GUI
        if (player.containerMenu != player.inventoryMenu) {
            return false;
        }

        this.targetPlayer = player;

        // --- 5. Sneak Check & Manual Initiation ---
        // 8 toggles = press, release, press, release, press, release, press, release
        boolean isSpammingSneak = PlayerGestureUtil.isSpammingSneak(player, 8);

        // Prevent automatic tag initiation if sneaking
        if (player.isShiftKeyDown() && !isSpammingSneak) {
            return false;
        }

        // --- 6. Permission Check ---
        boolean isOwner = this.hamster.isOwnedBy(player);
        if (!isOwner && !Configs.AHP_MAIN.allowStrangerTag) return false;

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
        return this.hamster.getRandom().nextInt(Configs.AHP_MAIN.tagChanceDenominator.get()) == 0;
    }

    @Override
    public void start() {
        this.hamster.setPlayingTag(true);
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());

        // Prevent accidental instant-catch
        this.hamster.interactionCooldown = 20;

        // Feedback
        this.hamster.triggerAnimOnServer("mainController", "attack");
        this.hamster.level().playSound(null, this.hamster.blockPosition(), ModSounds.HAMSTER_SLAP.get(), SoundSource.NEUTRAL, 0.5f, 1.0f);

        if (!this.hamster.level().isClientSide() && this.targetPlayer instanceof ServerPlayer serverPlayer) {
            // Instant feedback
            MiscUtil.PlayerPhysicsUtil.applyKnockback(serverPlayer, this.hamster.position());
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 140, 1, false, false, false));

            // Randomly select one of 4 messages
            int msgIndex = this.hamster.getRandom().nextInt(4) + 1; // 1 to 4
            serverPlayer.sendOverlayMessage(Component.translatable("message.adorablehamsterpets.tag_game_start." + msgIndex).withStyle(ChatFormatting.WHITE));

            // Delayed feedback
            this.hamster.scheduleTask(this.hamster.level().getGameTime() + 20, "tag_game_start_effects", () -> {
                if (this.hamster.isPlayingTag() && this.targetPlayer != null && this.targetPlayer.isAlive()) {
                    SoundEvent startSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BEG_SOUNDS, this.hamster.getRandom());
                    if (startSound != null) {
                        this.hamster.playSound(startSound, 1.0f, 1.2f);
                    }
                    this.hamster.level().playSound(null, this.hamster.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.5f, 1.5f);
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
        int minDuration = Configs.AHP_MAIN.minMiniGameFleeDurationSeconds.get() * 20;
        int maxDuration = Configs.AHP_MAIN.maxMiniGameFleeDurationSeconds.get() * 20;
        int gameDuration = this.hamster.getRandom().nextIntBetweenInclusive(minDuration, maxDuration);

        this.hamster.setGenericInteractionTimer(gameDuration);
        this.currentState = State.FLEEING;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.hamster.isPlayingTag()) return false; // Terminated by interaction
        if (this.hamster.getGenericInteractionTimer() <= 0) return false; // Timed out
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) return false;
        if (HamsterMovementUtil.shouldNotMove(this.hamster)) return false;
        return true;
    }

    @Override
    public void stop() {
        this.hamster.setPlayingTag(false);
        this.hamster.setTaunting(false); // Stop animation
        this.hamster.getNavigation().stop();
        this.hamster.setGenericInteractionTimer(0);
        this.targetPlayer = null;

        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }

    @Override
    public void tick() {
        // Timer Logic
        int timer = this.hamster.getGenericInteractionTimer();
        if (timer > 0) this.hamster.setGenericInteractionTimer(timer - 1);

        HamsterMovementUtil.faceEntity(this.hamster, this.targetPlayer);

        // Config Values
        double minFleeDist = Configs.AHP_MAIN.minMiniGameFleeDistance.get();
        double maxFleeDist = Configs.AHP_MAIN.maxMiniGameFleeDistance.get();

        switch (this.currentState) {
            case FLEEING -> {
                this.hamster.setTaunting(false);

                if (HamsterMovementUtil.shouldStopFleeing(this.hamster, this.targetPlayer, maxFleeDist)) {
                    this.currentState = State.TAUNTING;
                    this.hamster.getNavigation().stop();
                } else if (HamsterMovementUtil.shouldFlee(this.hamster, this.targetPlayer, minFleeDist)) {
                    Vec3 fleePos = HamsterMovementUtil.findFleePosition(this.hamster, this.targetPlayer, minFleeDist, maxFleeDist);
                    if (fleePos != null) {
                        this.hamster.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.5D);
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
