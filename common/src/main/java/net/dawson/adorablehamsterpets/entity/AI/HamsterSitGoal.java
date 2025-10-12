package net.dawson.adorablehamsterpets.entity.AI;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.ai.goal.SitGoal;

public class HamsterSitGoal extends SitGoal {
    private final HamsterEntity hamster;

    public HamsterSitGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        // Prevent this goal from starting if the hamster is knocked out.
        if (this.hamster.isKnockedOut()) {
            return false;
        }
        return super.canStart();
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }
}