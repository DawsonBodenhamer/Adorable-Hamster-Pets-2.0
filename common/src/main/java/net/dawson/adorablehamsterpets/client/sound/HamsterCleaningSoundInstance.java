package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;

public class HamsterCleaningSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    private final HamsterEntity hamster;
    private boolean done = false;

    public HamsterCleaningSoundInstance(HamsterEntity hamster) {
        super(ModSounds.HAMSTER_SCRATCH.get(), SoundCategory.NEUTRAL, hamster.getRandom());
        this.hamster = hamster;
        this.x = hamster.getX();
        this.y = hamster.getY();
        this.z = hamster.getZ();

        // --- Properties ---
        this.repeat = true; // Loop the sound
        this.repeatDelay = 0;
        this.volume = 2.0F;
        this.pitch = 1.0F;

        this.relative = false;
        this.attenuationType = AttenuationType.LINEAR;
    }

    public void stop() {
        this.done = true;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    @Override
    public void tick() {
        // The only job of this tick method is to check if the sound should stop.
        // It stops if the hamster is no longer cleaning OR if the entity is invalid.
        if (!this.hamster.isCleaning() || !this.hamster.isAlive()) {
            this.done = true;
            return;
        }

        // Update position to follow the hamster
        this.x = this.hamster.getX();
        this.y = this.hamster.getY();
        this.z = this.hamster.getZ();
    }
}