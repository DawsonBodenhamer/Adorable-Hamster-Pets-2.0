package net.dawson.adorablehamsterpets.world.neoforge;

import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The NeoForge-specific implementation for {@link ModSpawnPlacements}.
 * This class uses the NeoForge event bus to register spawn restrictions.
 * NOTE: This class is registered to the event bus in the main NeoForge entrypoint.
 */
public class ModSpawnPlacementsImpl {

    private static final List<SpawnPlacementData<?>> PENDING_PLACEMENTS = new ArrayList<>();

    /**
     * Caches the spawn placement data passed from the common module.
     */
    public static <T extends MobEntity> void register(EntityType<T> entityType, SpawnLocation location, Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate) {
        PENDING_PLACEMENTS.add(new SpawnPlacementData<>(entityType, location, heightmapType, predicate));
    }

    /**
     * Listens for the NeoForge event and registers all cached spawn placements.
     */
    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (SpawnPlacementData<?> data : PENDING_PLACEMENTS) {
            data.register(event);
        }
        PENDING_PLACEMENTS.clear();
    }

    /**
     * A helper record to store the parameters for a single spawn placement registration.
     */
    private record SpawnPlacementData<T extends MobEntity>(
            EntityType<T> entityType,
            SpawnLocation location,
            Heightmap.Type heightmapType,
            SpawnRestriction.SpawnPredicate<T> predicate
    ) {
        void register(RegisterSpawnPlacementsEvent event) {
            event.register(this.entityType, this.location, this.heightmapType, this.predicate, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}