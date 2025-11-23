package net.dawson.adorablehamsterpets.world.fabric;

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