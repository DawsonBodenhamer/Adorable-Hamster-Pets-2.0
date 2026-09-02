package net.dawson.adorablehamsterpets.fabric;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.entity.MobCategory;

public final class AdorableHamsterPetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AdorableHamsterPets.initRegistries();
        AdorableHamsterPets.initAttributes();
        AdorableHamsterPets.initCommonSetup();
        AdorableHamsterPets.registerSpawnPlacements();
        BiomeModifications.addSpawn(
                ctx -> ModEntitySpawns.shouldAddFabricSpawn(ctx.getBiomeKey().identifier(), ctx.getBiomeHolder()::is),
                MobCategory.CREATURE,
                ModEntities.HAMSTER.get(),
                Configs.AHP_WORLDGEN.spawnWeight.get(),
                1,
                Configs.AHP_WORLDGEN.maxGroupSize.get()
        );
    }
}









