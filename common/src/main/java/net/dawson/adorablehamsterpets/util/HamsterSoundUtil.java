package net.dawson.adorablehamsterpets.util;

import static net.dawson.adorablehamsterpets.sound.ModSounds.getRandomSoundFrom;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;

import java.util.Arrays;

/**
 * Selects hamster vocalizations and server-side fallback footsteps.
 */
public final class HamsterSoundUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                  Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final float DEFAULT_FOOTSTEP_VOLUME = 0.10F;
    private static final float GRAVEL_VOLUME_MODIFIER = 0.60F;

    /* ──────────────────────────────────────────────────────────────────────────────
     *                              Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Vocalization Selection ---

    public static SoundEvent selectAmbientSound(HamsterEntity hamster) {
        // Statues and knocked-out hamsters remain silent.
        if (hamster.isAiDisabled() || hamster.isKnockedOut()) {
            return null;
        }

        if (hamster.isBegging() || hamster.isTaunting()) {
            return getRandomSoundFrom(ModSounds.HAMSTER_BEG_SOUNDS, hamster.getRandom());
        }

        boolean playSleepSounds;
        if (hamster.isTamed()) {
            HamsterEntity.DozingPhase phase = hamster.getDozingPhase();
            playSleepSounds =
                    phase == HamsterEntity.DozingPhase.DRIFTING_OFF
                            || phase == HamsterEntity.DozingPhase.SETTLING_INTO_SLUMBER
                            || phase == HamsterEntity.DozingPhase.DEEP_SLEEP;
        } else {
            playSleepSounds = hamster.isSleeping();
        }

        // Tamed dozing phases use sleep sounds; all other states fall back to idle sounds.
        return getRandomSoundFrom(
                playSleepSounds ? ModSounds.HAMSTER_SLEEP_SOUNDS : ModSounds.HAMSTER_IDLE_SOUNDS,
                hamster.getRandom());
    }

    public static boolean isBeggingSound(SoundEvent sound) {
        return Arrays.asList(ModSounds.HAMSTER_BEG_SOUNDS).contains(sound);
    }

    public static SoundEvent selectHurtSound(HamsterEntity hamster) {
        return getRandomSoundFrom(ModSounds.HAMSTER_HURT_SOUNDS, hamster.getRandom());
    }

    public static SoundEvent selectDeathSound(HamsterEntity hamster) {
        return getRandomSoundFrom(ModSounds.HAMSTER_DEATH_SOUNDS, hamster.getRandom());
    }

    // --- Fallback Footsteps ---

    public static void playFallbackStepSound(HamsterEntity hamster, BlockState state) {
        // Rendered hamsters produce their footsteps through the client animation path.
        if (hamster.getWorld().isClient()
                || HamsterRenderTracker.isBeingRendered(hamster.getId())) {
            return;
        }

        try {
            BlockSoundGroup group = state.getSoundGroup();
            float volume =
                    state.isOf(Blocks.GRAVEL)
                            ? DEFAULT_FOOTSTEP_VOLUME * GRAVEL_VOLUME_MODIFIER
                            : DEFAULT_FOOTSTEP_VOLUME;
            hamster.playSound(group.getStepSound(), volume, group.getPitch() * 1.5F);
        } catch (Exception ex) {
            AdorableHamsterPets.LOGGER.warn("Error playing fallback step sound", ex);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterSoundUtil() {}
}
