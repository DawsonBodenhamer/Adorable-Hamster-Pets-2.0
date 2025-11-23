package net.dawson.adorablehamsterpets.forge;

import dev.architectury.platform.forge.EventBuses;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.forge.client.AdorableHamsterPetsForgeClient;
import net.dawson.adorablehamsterpets.world.forge.ModBiomeModifiers;
import net.dawson.adorablehamsterpets.world.forge.ModSpawnPlacementsImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(AdorableHamsterPets.MOD_ID)
public final class AdorableHamsterPetsForge {

    public AdorableHamsterPetsForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Submit event bus to let architectury handle its own registrations.
        EventBuses.registerModEventBus(AdorableHamsterPets.MOD_ID, modEventBus);

        // Register all items, blocks, etc.
        AdorableHamsterPets.initRegistries();
        ModBiomeModifiers.register(modEventBus);

        // Register event listeners for the mod lifecycle
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onAttributeCreate); // listener for attributes
        modEventBus.register(ModSpawnPlacementsImpl.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(AdorableHamsterPetsForgeClient.class);
        }
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // initCommonSetup is correct here.
        event.enqueueWork(AdorableHamsterPets::initCommonSetup);
    }

    // This handles the attribute creation event at the correct time.
    private void onAttributeCreate(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.HAMSTER.get(), HamsterEntity.createHamsterAttributes().build());
    }
}