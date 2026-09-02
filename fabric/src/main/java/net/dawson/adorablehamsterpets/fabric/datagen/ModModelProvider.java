package net.dawson.adorablehamsterpets.fabric.datagen;

import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.CucumberCropBlock;
import net.dawson.adorablehamsterpets.block.custom.GreenBeansCropBlock;
import net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock;
import net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.*;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import java.util.List;
import java.util.Optional;

public class ModModelProvider extends FabricModelProvider {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        // --- 1. Crops ---
        // Max age of 3
        blockStateModelGenerator.createCropBlock(ModBlocks.GREEN_BEANS_CROP.get(), GreenBeansCropBlock.AGE, 0, 1, 2, 3);
        blockStateModelGenerator.createCropBlock(ModBlocks.CUCUMBER_CROP.get(), CucumberCropBlock.AGE, 0, 1, 2, 3);

        // --- 2. Wild Bushes ---
        Identifier wildGreenBeanSeededTexture = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seeded");
        Identifier wildGreenBeanSeedlessTexture = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seedless");
        Identifier wildCucumberSeededTexture = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seeded");
        Identifier wildCucumberSeedlessTexture = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seedless");

        Identifier greenBeanSeededModelId = ModelTemplates.CROSS.createWithSuffix(
                ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seeded",
                TextureMapping.cross(wildGreenBeanSeededTexture), blockStateModelGenerator.modelOutput
        );
        Identifier greenBeanSeedlessModelId = ModelTemplates.CROSS.createWithSuffix(
                ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seedless",
                TextureMapping.cross(wildGreenBeanSeedlessTexture), blockStateModelGenerator.modelOutput
        );
        Identifier cucumberSeededModelId = ModelTemplates.CROSS.createWithSuffix(
                ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seeded",
                TextureMapping.cross(wildCucumberSeededTexture), blockStateModelGenerator.modelOutput
        );
        Identifier cucumberSeedlessModelId = ModelTemplates.CROSS.createWithSuffix(
                ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seedless",
                TextureMapping.cross(wildCucumberSeedlessTexture), blockStateModelGenerator.modelOutput
        );

        blockStateModelGenerator.blockStateOutput.accept(MultiVariantGenerator.multiVariant(ModBlocks.WILD_GREEN_BEAN_BUSH.get())
                .with(BlockModelGenerators.createBooleanModelDispatch(
                        WildGreenBeanBushBlock.SEEDED,
                        greenBeanSeededModelId,
                        greenBeanSeedlessModelId
                ))
        );

        blockStateModelGenerator.blockStateOutput.accept(MultiVariantGenerator.multiVariant(ModBlocks.WILD_CUCUMBER_BUSH.get())
                .with(BlockModelGenerators.createBooleanModelDispatch(
                        WildCucumberBushBlock.SEEDED,
                        cucumberSeededModelId,
                        cucumberSeedlessModelId
                ))
        );

        // --- 3. Hamster Beds ---
        generateHamsterBedVariantModels(blockStateModelGenerator);

        // --- 4. Crates ---
        blockStateModelGenerator.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(
                        ModBlocks.ACORN_CRATE.get(),
                        Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/acorn_crate")
                )
        );
        blockStateModelGenerator.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(
                        ModBlocks.CUCUMBER_CRATE.get(),
                        Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/cucumber_crate")
                )
        );
        blockStateModelGenerator.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(
                        ModBlocks.GREEN_BEANS_CRATE.get(),
                        Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/green_beans_crate")
                )
        );
        blockStateModelGenerator.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(
                        ModBlocks.HAMSTER_FOOD_MIX_CRATE.get(),
                        Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/hamster_food_mix_crate")
                )
        );
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        // --- 1. Core & Misc ---
        itemModelGenerator.generateFlatItem(ModItems.ANNOUNCEMENT_BELL_ICON.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_BEDDING.get(), ModelTemplates.FLAT_ITEM);

        // --- 2. Music Discs ---
        itemModelGenerator.generateFlatItem(ModItems.MUSIC_DISC_CHEESE.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.MUSIC_DISC_BLUE_CHEESE.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.MUSIC_DISC_PARMESAN.get(), ModelTemplates.FLAT_ITEM);

        // --- 3. Food & Crops ---
        itemModelGenerator.generateFlatItem(ModItems.CHEESE.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_FOOD_MIX.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.CUCUMBER.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SLICED_CUCUMBER.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.GREEN_BEANS.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.STEAMED_GREEN_BEANS.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SUNFLOWER_SEEDS.get(), ModelTemplates.FLAT_ITEM);

        // Block items
        itemModelGenerator.generateFlatItem(ModBlocks.WILD_GREEN_BEAN_BUSH.get().asItem(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModBlocks.WILD_CUCUMBER_BUSH.get().asItem(), ModelTemplates.FLAT_ITEM);

        // --- 4. Resources & Armor ---
        itemModelGenerator.generateFlatItem(ModItems.ACORN.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ACORN_SHARD.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ACORN_HAT.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ACORN_RING.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_ACORN.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_IRON.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_GOLD.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_DIAMOND.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_NETHERITE.get(), ModelTemplates.FLAT_ITEM);

        // --- 5. Smithing Templates ---
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get(), ModelTemplates.FLAT_ITEM);

        // --- 6. Hamster Beds ---
        // Each one uses main "hamster_bed" item model
        for (RegistrySupplier<Item> bedItemSupplier : ModItems.HAMSTER_BED_ITEMS.values()) {
            itemModelGenerator.generateFlatItem(bedItemSupplier.get(), new ModelTemplate(
                    Optional.of(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "item/hamster_bed")),
                    Optional.empty()
            ));
        }

        // --- 7. Crates ---
        itemModelGenerator.generateFlatItem(ModItems.ACORN_CRATE.get(), new ModelTemplate(
                Optional.of(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/acorn_crate")),
                Optional.empty()
        ));
        itemModelGenerator.generateFlatItem(ModItems.CUCUMBER_CRATE.get(), new ModelTemplate(
                Optional.of(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/cucumber_crate")),
                Optional.empty()
        ));
        itemModelGenerator.generateFlatItem(ModItems.GREEN_BEANS_CRATE.get(), new ModelTemplate(
                Optional.of(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/green_beans_crate")),
                Optional.empty()
        ));
        itemModelGenerator.generateFlatItem(ModItems.HAMSTER_FOOD_MIX_CRATE.get(), new ModelTemplate(
                Optional.of(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/hamster_food_mix_crate")),
                Optional.empty()
        ));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void generateHamsterBedVariantModels(BlockModelGenerators generator) {
        List<String> woodTypes = List.of(
                "oak", "spruce", "birch", "jungle", "acacia",
                "dark_oak", "mangrove", "cherry", "bamboo", "pale_oak"
        );

        Identifier baseModelId = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/hamster_bed");

        for (String wood : woodTypes) {
            Identifier variantModelId = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/hamster_bed_" + wood);
            Identifier particleTexture = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "block/hamster_bed_" + wood);

            TextureMapping textureMap = new TextureMapping().put(TextureSlot.PARTICLE, particleTexture);

            ModelTemplate variantModel = new ModelTemplate(
                    Optional.of(baseModelId),
                    Optional.empty(),
                    TextureSlot.PARTICLE
            );

            variantModel.create(variantModelId, textureMap, generator.modelOutput);
        }
    }
}
