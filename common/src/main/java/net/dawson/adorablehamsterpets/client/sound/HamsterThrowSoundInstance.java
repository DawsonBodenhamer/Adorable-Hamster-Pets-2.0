package net.dawson.adorablehamsterpets.client.sound;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;

public class HamsterThrowSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    private final HamsterEntity hamster;
    private boolean done = false;
    private boolean fadingOut = false;
    private int fadeOutTicksRemaining = 0;
    private static final int FADE_OUT_DURATION = 10; // Ticks to fade out over
    private static final float FIXED_VOLUME = 1.0f;
    private static final float FIXED_PITCH = 1.0f;

    public HamsterThrowSoundInstance(SoundEvent sound, SoundCategory category, HamsterEntity hamster) {
        super(sound, category, hamster.getRandom());
        this.hamster = hamster;
        this.x = hamster.getX();
        this.y = hamster.getY();
        this.z = hamster.getZ();
        this.repeat = false;
        this.repeatDelay = 0;
        this.volume = FIXED_VOLUME; // Set fixed initial volume
        this.pitch = FIXED_PITCH;   // Set fixed pitch
        this.relative = false;      // Position is absolute world coordinates
        this.attenuationType = AttenuationType.LINEAR; // Standard distance falloff
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    @Override
    public void tick() {
        // Check if entity is invalid first
        if (!this.hamster.isAlive() || this.hamster.isRemoved()) {
            this.done = true;
            return; // Stop processing if entity is gone
        }

        // Update position to follow the hamster
        this.x = this.hamster.getX();
        this.y = this.hamster.getY();
        this.z = this.hamster.getZ();

        // Check if we should start fading out
        if (!fadingOut && !this.hamster.isThrown()) {
            fadingOut = true;
            fadeOutTicksRemaining = FADE_OUT_DURATION;
        }

        // Handle fade-out
        if (fadingOut) {
            if (fadeOutTicksRemaining > 0) {
                // Calculate volume based on remaining fade ticks
                this.volume = FIXED_VOLUME * ((float) fadeOutTicksRemaining / FADE_OUT_DURATION);
                this.volume = MathHelper.clamp(this.volume, 0.0f, FIXED_VOLUME); // Ensure volume doesn't exceed initial
                fadeOutTicksRemaining--;
            } else {
                // Fade-out complete
                this.volume = 0.0f;
                this.done = true;
            }
        }
        // If not fading out, volume remains FIXED_VOLUME (set in constructor)
        // Pitch remains FIXED_PITCH throughout
    }
}