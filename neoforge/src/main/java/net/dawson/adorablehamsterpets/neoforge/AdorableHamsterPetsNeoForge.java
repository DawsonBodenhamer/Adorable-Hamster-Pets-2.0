package net.dawson.adorablehamsterpets.neoforge;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.neoforge.client.AdorableHamsterPetsNeoForgeClient;
import net.dawson.adorablehamsterpets.world.neoforge.ModBiomeModifiers;
import net.dawson.adorablehamsterpets.world.neoforge.ModSpawnPlacementsImpl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(AdorableHamsterPets.MOD_ID)
public final class AdorableHamsterPetsNeoForge {

    /**
     * The main entrypoint for the mod on the NeoForge platform.
     * NeoForge injects the mod-specific event bus into this constructor.
     * @param modEventBus The event bus for this mod, provided by NeoForge.
     */
    public AdorableHamsterPetsNeoForge(IEventBus modEventBus) {
        // --- Register Event Handlers ---
        modEventBus.register(this);
        modEventBus.register(ModSpawnPlacementsImpl.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(AdorableHamsterPetsNeoForgeClient.class);
        }

        // --- Initialize Registries & Attributes during Construction ---
        AdorableHamsterPets.initRegistries();
        AdorableHamsterPets.initAttributes();

        // --- Register Spawn Placements ---
        AdorableHamsterPets.registerSpawnPlacements();

        // --- Register custom biome modifier ---
        ModBiomeModifiers.register(modEventBus);
    }
    /**
     * Listens for the FMLCommonSetupEvent to run logic that must occur after
     * registries are populated.
     * @param event The common setup event.
     */
    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        // Run the common setup logic, which now includes spawn registration.
        // This runs synchronously within the event handler, not deferred.
        AdorableHamsterPets.initCommonSetup();
    }
}