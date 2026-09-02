package net.dawson.adorablehamsterpets.fabric;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

public final class AdorableHamsterPetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AdorableHamsterPets.initRegistries();
        AdorableHamsterPets.initAttributes();
        AdorableHamsterPets.initCommonSetup();
        AdorableHamsterPets.registerSpawnPlacements();
        BiomeModifications.addProperties(
                ModEntitySpawns::shouldAddFabricSpawn,
                (context, props) -> {
                    props.getSpawnProperties().addSpawn(
                            MobCategory.CREATURE,
                            new MobSpawnSettings.SpawnerData(
                                    ModEntities.HAMSTER.get(),
                                    Configs.AHP_WORLDGEN.spawnWeight.get(),
                                    1,
                                    Configs.AHP_WORLDGEN.maxGroupSize.get()
                            )
                    );
                }
        );
    }
}









