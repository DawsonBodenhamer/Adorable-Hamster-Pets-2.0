package net.dawson.adorablehamsterpets.fabric;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;

public final class AdorableHamsterPetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AdorableHamsterPets.initRegistries();
        AdorableHamsterPets.initAttributes();
        AdorableHamsterPets.initCommonSetup();
        BiomeModifications.addProperties(
                ModEntitySpawns::shouldSpawnInBiome, // Use the common decider method
                (context, props) -> {
                    props.getSpawnProperties().addSpawn(
                            SpawnGroup.CREATURE,
                            new SpawnSettings.SpawnEntry(
                                    ModEntities.HAMSTER.get(),
                                    Configs.AHP.spawnWeight.get(),
                                    1,
                                    Configs.AHP.maxGroupSize.get()
                            )
                    );
                }
        );
    }
}










