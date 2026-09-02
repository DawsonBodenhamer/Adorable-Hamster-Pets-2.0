package net.dawson.adorablehamsterpets.world.fabric;

import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.function.Supplier;

/**
 * The Fabric-specific implementation for {@link ModSpawnPlacements}.
 * This class is called by the @ExpectPlatform bridge.
 */
public class ModSpawnPlacementsImpl {
    // Fabric accepts Supplier; call .get() immediately
    public static <T extends Mob> void register(Supplier<? extends EntityType<T>> entityTypeSupplier, SpawnPlacementType location, Heightmap.Types heightmapType, SpawnPlacements.SpawnPredicate<T> predicate) {
        SpawnPlacements.register(entityTypeSupplier.get(), location, heightmapType, predicate);
    }
}