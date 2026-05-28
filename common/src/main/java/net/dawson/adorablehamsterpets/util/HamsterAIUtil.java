package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;

/**
 * Encapsulates passive, environment-scanning AI logic for Hamsters.
 */
public final class HamsterAIUtil {

    private HamsterAIUtil() {}

    /**
     * Centralized state machine for handling ambient/idle behaviors when a tamed hamster is sitting.
     * Manages a shared timer to prevent multiple actions from overlapping, and a shared cooldown
     * to prevent spamming.
     */
    public static void tickAmbientSittingBehaviors(HamsterEntity hamster) {
        // --- 1. Decrement Timers & Handle Expiration ---
        if (hamster.ambientSittingCooldown > 0) {
            hamster.ambientSittingCooldown--;
        }

        if (hamster.ambientSittingTimer > 0) {
            hamster.ambientSittingTimer--;
            if (hamster.ambientSittingTimer == 0) {
                // Action complete, clear flags for loops
                if (hamster.getHamsterFlag(HamsterEntity.CLEANING_FLAG)) {
                    hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
                }
                hamster.ambientSittingCooldown = 200; // Shared cooldown
            }
        }

        // --- 2. Handle External Interruptions ---
        if (hamster.isKnockedOut() || !hamster.isSitting()) {
            if (hamster.getHamsterFlag(HamsterEntity.CLEANING_FLAG)) {
                hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
            }
            hamster.ambientSittingTimer = 0;
            return;
        }

        // --- 3. Evaluate Start Conditions ---
        if (!hamster.isTamed() || hamster.ambientSittingCooldown > 0 || hamster.ambientSittingTimer > 0) {
            return;
        }

        // Prevent ambient behaviors if starting to doze off
        HamsterEntity.DozingPhase currentPhase = hamster.getDozingPhase();
        if (currentPhase != HamsterEntity.DozingPhase.NONE && currentPhase != HamsterEntity.DozingPhase.QUIESCENT_SITTING) {
            return;
        }

        // --- 4. Roll for Behaviors ---
        // Order technically dictates priority if both hit on same tick
        Random random = hamster.getRandom();

        // Behavior A: Looping Cleaning Animation
        int cleaningChance = Configs.AHP.cleaningChanceDenominator.get();
        if (cleaningChance > 0 && random.nextInt(cleaningChance) == 0) {
            hamster.ambientSittingTimer = random.nextBetween(30, 60);
            hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, true);
            return; // Exit after successful trigger
        }

        // Behavior B: Triggerable Rolling Animation
        int rollingChance = Configs.AHP.rollingChanceDenominator.get();
        if (rollingChance > 0 && random.nextInt(rollingChance) == 0) {
            // Assign a timer lock so it isn't interrupted by other ambient systems
            hamster.ambientSittingTimer = 60; // Anim length
            hamster.triggerAnimOnServer("mainController", "sitting_roll");
            return; // Exit after successful trigger
        }
    }

    /**
     * Scans for a nearby jukebox actively playing the Cheese Music Disc.
     */
    public static boolean isCheeseSongPlayingNearby(HamsterEntity hamster) {
        World world = hamster.getWorld();

        for (BlockPos p : BlockPos.iterateOutwards(hamster.getBlockPos(), 8, 4, 8)) {
            if (world.getBlockState(p).isOf(Blocks.JUKEBOX)) {
                if (world.getBlockEntity(p) instanceof JukeboxBlockEntity jbe) {
                    if (jbe.getManager().isPlaying() && jbe.getManager().getSong() != null) {
                        // Check if the currently playing song's SoundEvent matches my custom music disc sound
                        if (jbe.getManager().getSong().soundEvent().value().equals(ModSounds.AHP_THEME_SONG.get())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}