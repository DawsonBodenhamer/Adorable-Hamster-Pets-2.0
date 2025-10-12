package net.dawson.adorablehamsterpets.client.sound; 
/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

// Or your preferred package

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;

public class HamsterFlightSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    private final HamsterEntity hamster;
    private boolean done = false;
    private int ticksElapsed = 0;
    // Keep duration relatively short so it finishes soon after impact
    private final int effectDuration = 30; // Duration in ticks (1.5 seconds)

    // --- Sound Parameters Adjusted ---
    private final float initialPitch = 1.0f; // Start at NORMAL pitch
    private final float basePitch = 0.5f;    // End low (0.5 is the lowest the game generally allows.)
    private final float initialVolume = 1.0f;  // Start at full volume
    private final float endVolume = 0.2f;
    // --- End Adjustments ---

    public HamsterFlightSoundInstance(SoundEvent sound, SoundCategory category, HamsterEntity hamster) {
        super(sound, category, hamster.getRandom());
        this.hamster = hamster;
        this.x = hamster.getX();
        this.y = hamster.getY();
        this.z = hamster.getZ();
        this.repeat = false;
        this.repeatDelay = 0;
        this.volume = initialVolume; // Set initial volume
        this.pitch = initialPitch;   // Set initial pitch
        this.relative = false;
        this.attenuationType = AttenuationType.LINEAR;
    }

    @Override
    public boolean isDone() {
        // --- Add Logging: isDone Check ---
         AdorableHamsterPets.LOGGER.debug("HamsterFlightSoundInstance isDone() checked, returning: {}", this.done);
        // --- End Logging ---
        return this.done;
    }

    @Override
    public void tick() {
        // --- Add Logging: Tick Start ---
        // AdorableHamsterPets.LOGGER.debug("HamsterFlightSoundInstance tick() called. Done: {}, Ticks: {}", this.done, this.ticksElapsed);
        // --- End Logging ---

        // --- Check if entity is invalid first ---
        if (!this.hamster.isAlive() || this.hamster.isRemoved()) {
            // --- Add Logging: Entity Invalid ---
            if (!this.done) AdorableHamsterPets.LOGGER.debug("HamsterFlightSoundInstance: Hamster invalid, setting done=true.");
            // --- End Logging ---
            this.done = true;
            return; // Stop processing if entity is gone
        }
        // --- End Check ---

        // Check if duration exceeded
        if (this.ticksElapsed > this.effectDuration) {
            // --- Add Logging: Duration Exceeded ---
            if (!this.done) AdorableHamsterPets.LOGGER.debug("HamsterFlightSoundInstance: Duration exceeded ({}), setting done=true.", this.effectDuration);
            // --- End Logging ---
            this.done = true;
            return; // Stop processing if duration is over
        }

        // Only increment ticks and update sound if not yet done
        this.ticksElapsed++;

        // Update position to follow the hamster
        this.x = this.hamster.getX();
        this.y = this.hamster.getY();
        this.z = this.hamster.getZ();

        // --- Curved Doppler Effect ---
        float progress = MathHelper.clamp((float) this.ticksElapsed / (float) this.effectDuration, 0.0f, 1.0f);
        float curveFactor = MathHelper.cos(progress * MathHelper.HALF_PI); // Cosine curve (1 -> 0)

        // --- Calculate Pitch ---
        this.pitch = this.basePitch + (this.initialPitch - this.basePitch) * curveFactor;

        // --- Calculate Volume ---
        this.volume = this.endVolume + (this.initialVolume - this.endVolume) * curveFactor;

        // Clamp values
        this.pitch = MathHelper.clamp(this.pitch, 0.5f, 2.0f);
        this.volume = MathHelper.clamp(this.volume, 0.0f, 1.0f);

        // --- Add Logging: Calculated Values ---
        // AdorableHamsterPets.LOGGER.debug("HamsterFlightSoundInstance tick {}: Progress={}, Curve={}, Pitch={}, Volume={}, Pos=({}, {}, {})",
        //         this.ticksElapsed, progress, curveFactor, this.pitch, this.volume, this.x, this.y, this.z);
        // --- End Logging ---
    }
}