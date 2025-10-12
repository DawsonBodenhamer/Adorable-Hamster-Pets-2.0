package net.dawson.adorablehamsterpets.world;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

/**
 * Common class for registering entity spawn restrictions using an @ExpectPlatform bridge.
 * The actual implementation is provided by each loader.
 */
public final class ModSpawnPlacements {
    @ExpectPlatform
    public static <T extends MobEntity> void register(EntityType<T> entityType, SpawnRestriction.Location location, Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate) {
        // This is a placeholder that will be replaced by the platform-specific implementation at runtime.
        throw new AssertionError();
    }
}