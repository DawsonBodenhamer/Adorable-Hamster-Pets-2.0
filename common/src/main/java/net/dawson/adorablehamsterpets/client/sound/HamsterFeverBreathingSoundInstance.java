package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;

/**
 * Follows one fevered hamster for the duration of one breathing clip.
 */
public final class HamsterFeverBreathingSoundInstance extends MovingSoundInstance {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance State
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private final int durationTicks;
    private int ticksPlaying;

    public HamsterFeverBreathingSoundInstance(HamsterEntity hamster, ModSounds.TimedSound timedSound) {
        super(timedSound.sound().get(), SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.hamster = hamster;
        this.repeat = false;
        this.volume = 0.2F;
        this.pitch = 1.0F;
        this.durationTicks = (int) timedSound.durationTicks();
        this.x = hamster.getX();
        this.y = hamster.getY();
        this.z = hamster.getZ();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void tick() {
        if (!this.shouldRemainActive()) {
            this.setDone();
            return;
        }

        this.x = this.hamster.getX();
        this.y = this.hamster.getY();
        this.z = this.hamster.getZ();

        this.ticksPlaying++;
        if (this.ticksPlaying >= this.durationTicks) {
            this.setDone();
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Manager Contract
     * ────────────────────────────────────────────────────────────────────────────*/

    boolean shouldRemainActive() {
        return !this.hamster.isRemoved()
                && this.hamster.isAlive()
                && this.hamster.hasRedstoneFever();
    }

    boolean belongsTo(HamsterEntity hamster) {
        return this.hamster == hamster;
    }

    boolean belongsTo(World world) {
        return this.hamster.getWorld() == world;
    }

    void markDone() {
        this.setDone();
    }
}
