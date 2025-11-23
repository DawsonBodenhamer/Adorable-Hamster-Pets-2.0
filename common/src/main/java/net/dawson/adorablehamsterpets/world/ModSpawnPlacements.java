package net.dawson.adorablehamsterpets.world;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;

/**
 * Common class for registering entity spawn restrictions using an @ExpectPlatform bridge.
 * The actual implementation is provided by each loader.
 */
public final class ModSpawnPlacements {
    @ExpectPlatform
    public static <T extends MobEntity> void register(EntityType<T> entityType, SpawnLocation location, Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate) {
        // This is a placeholder that will be replaced by the platform-specific implementation at runtime.
        throw new AssertionError();
    }
}