package net.dawson.adorablehamsterpets.world.fabric;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

/**
 * The Fabric-specific implementation for {@link ModSpawnPlacements}.
 * This class is called by the @ExpectPlatform bridge.
 */
public class ModSpawnPlacementsImpl {
    public static <T extends MobEntity> void register(EntityType<T> entityType, SpawnRestriction.Location location, Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate) {
        // On Fabric, call the vanilla/Fabric static method directly.
        SpawnRestriction.register(entityType, location, heightmapType, predicate);
    }
}