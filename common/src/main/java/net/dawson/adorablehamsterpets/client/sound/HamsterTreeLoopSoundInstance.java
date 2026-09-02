package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class HamsterTreeLoopSoundInstance extends AbstractTickableSoundInstance {
    private final HamsterTreeSearcherEntity entity;

    public HamsterTreeLoopSoundInstance(HamsterTreeSearcherEntity entity) {
        super(ModSounds.HAMSTER_ACORN_SEARCH_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        // Start at the entity's location
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
    }

    @Override
    public void tick() {
        // Stop if the entity is removed (search finished or aborted) or dead
        if (this.entity.isRemoved() || !this.entity.isAlive()) {
            this.stop();
            return;
        }

        // Update position to follow the entity as it teleports around the tree
        this.x = this.entity.getX();
        this.y = this.entity.getY();
        this.z = this.entity.getZ();
    }
}