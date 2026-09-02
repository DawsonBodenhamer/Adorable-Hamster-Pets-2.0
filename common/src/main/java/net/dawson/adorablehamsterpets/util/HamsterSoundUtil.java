package net.dawson.adorablehamsterpets.util;

import static net.dawson.adorablehamsterpets.sound.ModSounds.getRandomSoundFrom;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

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
        if (hamster.isNoAi() || hamster.isKnockedOut()) {
            return null;
        }

        if (hamster.hasRedstoneFever()) {
            return getRandomSoundFrom(ModSounds.HAMSTER_SNORT_SOUNDS, hamster.getRandom());
        }

        if (hamster.isBegging() || hamster.isTaunting()) {
            return getRandomSoundFrom(ModSounds.HAMSTER_BEG_SOUNDS, hamster.getRandom());
        }

        boolean playSleepSounds;
        if (hamster.isTame()) {
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
        return ModSounds.HAMSTER_BEG_SOUNDS.stream()
                .anyMatch(soundSupplier -> soundSupplier.get().equals(sound));
    }

    public static boolean isRedstoneFeverSnort(SoundEvent sound) {
        return ModSounds.HAMSTER_SNORT_SOUNDS.stream()
                .anyMatch(soundSupplier -> soundSupplier.get().equals(sound));
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
        if (hamster.level().isClientSide()
                || HamsterRenderTracker.isBeingRendered(hamster.getId())) {
            return;
        }

        try {
            SoundType group = state.getSoundType();
            float volume =
                    state.is(Blocks.GRAVEL)
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
