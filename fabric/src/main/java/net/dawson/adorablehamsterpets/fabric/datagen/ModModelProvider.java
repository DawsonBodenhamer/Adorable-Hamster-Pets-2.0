package net.dawson.adorablehamsterpets.fabric.datagen;

import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.*;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    // Helper for creating Hamster Bed models
    private void generateHamsterBedVariantModels(BlockStateModelGenerator generator) {
        List<String> woodTypes = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "pale_oak");
        // Base model ID
        Identifier baseModelId = Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_bed");
        for (String wood : woodTypes) {
            // Model ID for this variant
            Identifier variantModelId = Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_bed_" + wood);
            // Texture for this variant
            Identifier particleTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_bed_" + wood);
            // Create texture map with just the particle texture
            TextureMap textureMap = new TextureMap().put(TextureKey.PARTICLE, particleTexture);
            // Create model with base as parent and particle texture key
            Model variantModel = new Model(
                    Optional.of(baseModelId),  // parent
                    Optional.empty(),          // no variant suffix
                    TextureKey.PARTICLE        // required texture key
            );
            // Upload the model
            variantModel.upload(variantModelId, textureMap, generator.modelCollector);
        }
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        // Generate hamster bed variant models
        generateHamsterBedVariantModels(blockStateModelGenerator);

        // Generates models for crop blocks
        // (MAX_AGE = 3)
        blockStateModelGenerator.registerCrop(ModBlocks.GREEN_BEANS_CROP.get(), GreenBeansCropBlock.AGE, 0, 1, 2, 3);
        blockStateModelGenerator.registerCrop(ModBlocks.CUCUMBER_CROP.get(), CucumberCropBlock.AGE, 0, 1, 2, 3);

        // --- Step 1: Generate Block Models ---
        Identifier wildGreenBeanSeededTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seeded");
        Identifier wildGreenBeanSeedlessTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seedless");
        Identifier wildCucumberSeededTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seeded");
        Identifier wildCucumberSeedlessTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seedless");

        TextureMap greenBeanSeededMap = TextureMap.cross(wildGreenBeanSeededTexture);
        TextureMap greenBeanSeedlessMap = TextureMap.cross(wildGreenBeanSeedlessTexture);
        TextureMap cucumberSeededMap = TextureMap.cross(wildCucumberSeededTexture);
        TextureMap cucumberSeedlessMap = TextureMap.cross(wildCucumberSeedlessTexture);

        Identifier greenBeanSeededModelId = Models.CROSS.upload(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seeded", greenBeanSeededMap, blockStateModelGenerator.modelCollector);
        Identifier greenBeanSeedlessModelId = Models.CROSS.upload(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seedless", greenBeanSeedlessMap, blockStateModelGenerator.modelCollector);
        Identifier cucumberSeededModelId = Models.CROSS.upload(ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seeded", cucumberSeededMap, blockStateModelGenerator.modelCollector);
        Identifier cucumberSeedlessModelId = Models.CROSS.upload(ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seedless", cucumberSeedlessMap, blockStateModelGenerator.modelCollector);

        // --- Step 2: Generate Block States ---
        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(ModBlocks.WILD_GREEN_BEAN_BUSH.get())
                .coordinate(BlockStateModelGenerator.createBooleanModelMap(
                        WildGreenBeanBushBlock.SEEDED,
                        greenBeanSeededModelId,
                        greenBeanSeedlessModelId
                ))
        );

        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(ModBlocks.WILD_CUCUMBER_BUSH.get())
                .coordinate(BlockStateModelGenerator.createBooleanModelMap(
                        WildCucumberBushBlock.SEEDED,
                        cucumberSeededModelId,
                        cucumberSeedlessModelId
                ))
        );
        // Sunflower and wild bush models handled manually
    }


    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.MUSIC_DISC_CHEESE.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.MUSIC_DISC_BLUE_CHEESE.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.MUSIC_DISC_PARMESAN.get(), Models.GENERATED);
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
        itemModelGenerator.register(ModItems.HAMSTER_BEDDING.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.ACORN.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.ACORN_SHARD.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.ACORN_HAT.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_ACORN.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_IRON.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_GOLD.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_DIAMOND.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_NETHERITE.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get(), Models.GENERATED);


        for (RegistrySupplier<Item> bedItemSupplier : ModItems.HAMSTER_BED_ITEMS.values()) {
            // Register each bed item variant to use the 'hamster_bed' item model.
            itemModelGenerator.register(bedItemSupplier.get(), new Model(Optional.of(Identifier.of(AdorableHamsterPets.MOD_ID, "item/hamster_bed")), Optional.empty()));
        }
    }
}