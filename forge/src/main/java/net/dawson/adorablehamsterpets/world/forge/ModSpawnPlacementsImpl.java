package net.dawson.adorablehamsterpets.world.forge;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Forge-specific implementation for spawn placements.
 * This class uses the Forge event bus to register spawn restrictions.
 * NOTE: This class is registered to the event bus in the main Forge entrypoint.
 */
public class ModSpawnPlacementsImpl {

    private static final List<SpawnPlacementData<?>> PENDING_PLACEMENTS = new ArrayList<>();

    /**
     * Caches the spawn placement data passed from the common module.
     */
    public static <T extends MobEntity> void register(Supplier<? extends EntityType<T>> entityTypeSupplier, SpawnRestriction.Location location, Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate) {
        PENDING_PLACEMENTS.add(new SpawnPlacementData<>(entityTypeSupplier, location, heightmapType, predicate));
    }

    /**
     * Listens for the Forge event and registers all cached spawn placements.
     * This method is static and is automatically called by Forge because its class is registered to the event bus.
     */
    @SubscribeEvent
    public static void onRegisterSpawnPlacements(SpawnPlacementRegisterEvent event) {
        for (SpawnPlacementData<?> data : PENDING_PLACEMENTS) {
            data.register(event);
        }
        PENDING_PLACEMENTS.clear();
    }

    /**
     * A helper record to store the parameters for a single spawn placement registration.
     */
    private record SpawnPlacementData<T extends MobEntity>(
            Supplier<? extends EntityType<T>> entityTypeSupplier,
            SpawnRestriction.Location location,
            Heightmap.Type heightmapType,
            SpawnRestriction.SpawnPredicate<T> predicate
    ) {
        /**
         * Registers the cached data to the event.
         * @param event The spawn placement registration event.
         */
        void register(SpawnPlacementRegisterEvent event) {
            event.register(this.entityTypeSupplier.get(), this.location, this.heightmapType, this.predicate, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}