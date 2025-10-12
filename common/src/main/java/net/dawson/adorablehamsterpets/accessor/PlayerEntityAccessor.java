package net.dawson.adorablehamsterpets.accessor;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayDeque;

/**
 * Accessor interface to expose custom methods injected into PlayerEntity by PlayerEntityMixin.
 * This allows other parts of the mod to safely call these methods without illegally
 * referencing the mixin class directly.
 */
public interface PlayerEntityAccessor {
    NbtCompound getShoulderHamster(ShoulderLocation location);
    void setShoulderHamster(ShoulderLocation location, NbtCompound nbt);

    boolean hasAnyShoulderHamster();

    int ahp_getLastGoldMessageIndex();
    void ahp_setLastGoldMessageIndex(int index);

    void adorablehamsterpets$dismountShoulderHamster(boolean isThrow);

    default void adorablehamsterpets$dismountShoulderHamster() {
        adorablehamsterpets$dismountShoulderHamster(false);
    }

    ArrayDeque<ShoulderLocation> adorablehamsterpets$getMountOrderQueue();

    ClientShoulderHamsterData adorablehamsterpets$getClientShoulderData();
}