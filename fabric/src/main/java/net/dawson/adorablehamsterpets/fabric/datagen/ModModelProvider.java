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
import net.minecraft.data.client.*;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

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
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        // --- 1. Crops ---
        // Max age of 3
        blockStateModelGenerator.registerCrop(ModBlocks.GREEN_BEANS_CROP.get(), GreenBeansCropBlock.AGE, 0, 1, 2, 3);
        blockStateModelGenerator.registerCrop(ModBlocks.CUCUMBER_CROP.get(), CucumberCropBlock.AGE, 0, 1, 2, 3);

        // --- 2. Wild Bushes ---
        Identifier wildGreenBeanSeededTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seeded");
        Identifier wildGreenBeanSeedlessTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_green_bean_bush_seedless");
        Identifier wildCucumberSeededTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seeded");
        Identifier wildCucumberSeedlessTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/wild_cucumber_bush_seedless");

        Identifier greenBeanSeededModelId = Models.CROSS.upload(
                ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seeded",
                TextureMap.cross(wildGreenBeanSeededTexture), blockStateModelGenerator.modelCollector
        );
        Identifier greenBeanSeedlessModelId = Models.CROSS.upload(
                ModBlocks.WILD_GREEN_BEAN_BUSH.get(), "_seedless",
                TextureMap.cross(wildGreenBeanSeedlessTexture), blockStateModelGenerator.modelCollector
        );
        Identifier cucumberSeededModelId = Models.CROSS.upload(
                ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seeded",
                TextureMap.cross(wildCucumberSeededTexture), blockStateModelGenerator.modelCollector
        );
        Identifier cucumberSeedlessModelId = Models.CROSS.upload(
                ModBlocks.WILD_CUCUMBER_BUSH.get(), "_seedless",
                TextureMap.cross(wildCucumberSeedlessTexture), blockStateModelGenerator.modelCollector
        );

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

        // --- 3. Hamster Beds ---
        generateHamsterBedVariantModels(blockStateModelGenerator);

        // --- 4. Crates ---
        blockStateModelGenerator.blockStateCollector.accept(
                BlockStateModelGenerator.createSingletonBlockState(
                        ModBlocks.ACORN_CRATE.get(),
                        Identifier.of(AdorableHamsterPets.MOD_ID, "block/acorn_crate")
                )
        );
        blockStateModelGenerator.blockStateCollector.accept(
                BlockStateModelGenerator.createSingletonBlockState(
                        ModBlocks.CUCUMBER_CRATE.get(),
                        Identifier.of(AdorableHamsterPets.MOD_ID, "block/cucumber_crate")
                )
        );
        blockStateModelGenerator.blockStateCollector.accept(
                BlockStateModelGenerator.createSingletonBlockState(
                        ModBlocks.GREEN_BEANS_CRATE.get(),
                        Identifier.of(AdorableHamsterPets.MOD_ID, "block/green_beans_crate")
                )
        );
        blockStateModelGenerator.blockStateCollector.accept(
                BlockStateModelGenerator.createSingletonBlockState(
                        ModBlocks.HAMSTER_FOOD_MIX_CRATE.get(),
                        Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_food_mix_crate")
                )
        );
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        // --- 1. Core & Misc ---
        itemModelGenerator.register(ModItems.ANNOUNCEMENT_BELL_ICON.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_SPAWN_EGG.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_BEDDING.get(), Models.GENERATED);

        // --- 2. Music Discs ---
        itemModelGenerator.register(ModItems.MUSIC_DISC_CHEESE.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.MUSIC_DISC_BLUE_CHEESE.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.MUSIC_DISC_PARMESAN.get(), Models.GENERATED);

        // --- 3. Food & Crops ---
        itemModelGenerator.register(ModItems.CHEESE.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_FOOD_MIX.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.CUCUMBER.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.SLICED_CUCUMBER.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.GREEN_BEANS.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.STEAMED_GREEN_BEANS.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.SUNFLOWER_SEEDS.get(), Models.GENERATED);

        // Block items
        itemModelGenerator.register(ModBlocks.WILD_GREEN_BEAN_BUSH.get().asItem(), Models.GENERATED);
        itemModelGenerator.register(ModBlocks.WILD_CUCUMBER_BUSH.get().asItem(), Models.GENERATED);

        // --- 4. Resources & Armor ---
        itemModelGenerator.register(ModItems.ACORN.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.ACORN_SHARD.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.ACORN_HAT.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.ACORN_RING.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_ACORN.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_IRON.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_GOLD.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_DIAMOND.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_NETHERITE.get(), Models.GENERATED);

        // --- 5. Smithing Templates ---
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get(), Models.GENERATED);
        itemModelGenerator.register(ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get(), Models.GENERATED);

        // --- 6. Hamster Beds ---
        // Each one uses main "hamster_bed" item model
        for (RegistrySupplier<Item> bedItemSupplier : ModItems.HAMSTER_BED_ITEMS.values()) {
            itemModelGenerator.register(bedItemSupplier.get(), new Model(
                    Optional.of(Identifier.of(AdorableHamsterPets.MOD_ID, "item/hamster_bed")),
                    Optional.empty()
            ));
        }

        // --- 7. Crates ---
        itemModelGenerator.register(ModItems.ACORN_CRATE.get(), new Model(
                Optional.of(Identifier.of(AdorableHamsterPets.MOD_ID, "block/acorn_crate")),
                Optional.empty()
        ));
        itemModelGenerator.register(ModItems.CUCUMBER_CRATE.get(), new Model(
                Optional.of(Identifier.of(AdorableHamsterPets.MOD_ID, "block/cucumber_crate")),
                Optional.empty()
        ));
        itemModelGenerator.register(ModItems.GREEN_BEANS_CRATE.get(), new Model(
                Optional.of(Identifier.of(AdorableHamsterPets.MOD_ID, "block/green_beans_crate")),
                Optional.empty()
        ));
        itemModelGenerator.register(ModItems.HAMSTER_FOOD_MIX_CRATE.get(), new Model(
                Optional.of(Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_food_mix_crate")),
                Optional.empty()
        ));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void generateHamsterBedVariantModels(BlockStateModelGenerator generator) {
        List<String> woodTypes = List.of(
                "oak", "spruce", "birch", "jungle", "acacia",
                "dark_oak", "mangrove", "cherry", "bamboo", "pale_oak"
        );

        Identifier baseModelId = Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_bed");

        for (String wood : woodTypes) {
            Identifier variantModelId = Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_bed_" + wood);
            Identifier particleTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "block/hamster_bed_" + wood);

            TextureMap textureMap = new TextureMap().put(TextureKey.PARTICLE, particleTexture);

            Model variantModel = new Model(
                    Optional.of(baseModelId),
                    Optional.empty(),
                    TextureKey.PARTICLE
            );

            variantModel.upload(variantModelId, textureMap, generator.modelCollector);
        }
    }
}
