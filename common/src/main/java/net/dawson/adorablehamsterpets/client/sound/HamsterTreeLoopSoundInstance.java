package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

public class HamsterTreeLoopSoundInstance extends MovingSoundInstance {
    private final HamsterTreeSearcherEntity entity;

    public HamsterTreeLoopSoundInstance(HamsterTreeSearcherEntity entity) {
        super(ModSounds.HAMSTER_ACORN_SEARCH_LOOP.get(), SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.entity = entity;
        this.repeat = true;
        this.repeatDelay = 0;
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
            this.setDone();
            return;
        }

        // Update position to follow the entity as it teleports around the tree
        this.x = this.entity.getX();
        this.y = this.entity.getY();
        this.z = this.entity.getZ();
    }
}