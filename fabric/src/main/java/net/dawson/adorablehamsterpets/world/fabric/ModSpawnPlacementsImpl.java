package net.dawson.adorablehamsterpets.world.fabric;

import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

/**
 * The Fabric-specific implementation for {@link ModSpawnPlacements}.
 * This class is called by the @ExpectPlatform bridge.
 */
public class ModSpawnPlacementsImpl {
    public static <T extends MobEntity> void register(EntityType<T> entityType, SpawnLocation location, Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate) {
        // On Fabric, we can call the vanilla/Fabric static method directly.
        SpawnRestriction.register(entityType, location, heightmapType, predicate);
    }
}