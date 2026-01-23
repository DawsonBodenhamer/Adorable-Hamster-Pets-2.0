package net.dawson.adorablehamsterpets.world.fabric;

import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

import java.util.function.Supplier;

/**
 * The Fabric-specific implementation for {@link ModSpawnPlacements}.
 * This class is called by the @ExpectPlatform bridge.
 */
public class ModSpawnPlacementsImpl {
    // Fabric accepts Supplier; call .get() immediately
    // Use SpawnRestriction.Location on 1.20.1
    public static <T extends MobEntity> void register(Supplier<? extends EntityType<T>> entityTypeSupplier, SpawnRestriction.Location location, Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate) {
        SpawnRestriction.register(entityTypeSupplier.get(), location, heightmapType, predicate);
    }
}