package net.dawson.adorablehamsterpets.fabric.datagen;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.CucumberCropBlock;
import net.dawson.adorablehamsterpets.block.custom.GreenBeansCropBlock;
import net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock;
import net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    // generates block_states.json file, block_model.json file for all blocks
    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {


        // Generates models for crop blocks
        // (MAX_AGE = 3)
        blockStateModelGenerator.registerCrop(ModBlocks.GREEN_BEANS_CROP.get(), GreenBeansCropBlock.AGE, 0, 1, 2, 3);
        blockStateModelGenerator.registerCrop(ModBlocks.CUCUMBER_CROP.get(), CucumberCropBlock.AGE, 0, 1, 2, 3);


        // --- Step 1: Generate the specific block models for each state ---
        // Define texture identifiers (pointing to textures/block/)
        Identifier wildGreenBeanSeededTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seeded");
        Identifier wildGreenBeanSeedlessTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seedless");
        Identifier wildCucumberSeededTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seeded");
        Identifier wildCucumberSeedlessTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seedless");

        // Create TextureMaps using the correct helper for the "cross" model type
        TextureMap greenBeanSeededMap = TextureMap.cross(wildGreenBeanSeededTexture);
        TextureMap greenBeanSeedlessMap = TextureMap.cross(wildGreenBeanSeedlessTexture);
        TextureMap cucumberSeededMap = TextureMap.cross(wildCucumberSeededTexture);
        TextureMap cucumberSeedlessMap = TextureMap.cross(wildCucumberSeedlessTexture);

        // Upload the models using the correct parent (Models.CROSS) and the texture maps.
        // Use the upload overload: upload(Block block, String suffix, TextureMap textures, BiConsumer<Identifier, Supplier<JsonElement>> modelCollector)
        // Store the returned model IDs
        Identifier greenBeanSeededModelId = Models.CROSS.upload(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seeded", greenBeanSeededMap, blockStateModelGenerator.modelCollector);
        Identifier greenBeanSeedlessModelId = Models.CROSS.upload(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seedless", greenBeanSeedlessMap, blockStateModelGenerator.modelCollector);
        Identifier cucumberSeededModelId = Models.CROSS.upload(ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seeded", cucumberSeededMap, blockStateModelGenerator.modelCollector);
        Identifier cucumberSeedlessModelId = Models.CROSS.upload(ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seedless", cucumberSeedlessMap, blockStateModelGenerator.modelCollector);

        // --- Step 2: Generate the blockstates linking properties to the generated models ---
        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(ModBlocks.WILD_GREEN_BEAN_BUSH.get())
                .coordinate(BlockStateModelGenerator.createBooleanModelMap(
                        WildGreenBeanBushBlock.SEEDED, // Assumes this property exists in your block class
                        greenBeanSeededModelId,        // Model ID when SEEDED=true
                        greenBeanSeedlessModelId       // Model ID when SEEDED=false
                ))
        );

        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(ModBlocks.WILD_CUCUMBER_BUSH.get())
                .coordinate(BlockStateModelGenerator.createBooleanModelMap(
                        WildCucumberBushBlock.SEEDED, // Assumes this property exists in your block class
                        cucumberSeededModelId,       // Model ID when SEEDED=true
                        cucumberSeedlessModelId      // Model ID when SEEDED=false
                ))
        );

        // Sunflower and wild bush models handled manually
    }


    // generates model json files for all items
    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.ANNOUNCEMENT_BELL_ICON.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_SPAWN_EGG.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.CUCUMBER.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.SLICED_CUCUMBER.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.GREEN_BEANS.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.STEAMED_GREEN_BEANS.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.CHEESE.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_FOOD_MIX.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.SUNFLOWER_SEEDS.get(), Models.GENERATED);
        itemModelGenerator.register(ModBlocks.WILD_GREEN_BEAN_BUSH.get().asItem(), Models.GENERATED);
        itemModelGenerator.register(ModBlocks.WILD_CUCUMBER_BUSH.get().asItem(), Models.GENERATED);
    }
}