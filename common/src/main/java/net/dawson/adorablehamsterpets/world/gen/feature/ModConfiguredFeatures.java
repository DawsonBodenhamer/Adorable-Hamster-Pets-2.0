package net.dawson.adorablehamsterpets.world.gen.feature;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class ModConfiguredFeatures {

    public static final RegistryKey<ConfiguredFeature<?, ?>> CUSTOM_SUNFLOWER_PATCH_KEY = registerKey("custom_sunflower_patch");

    // --- Add Keys for Bushes ---
    public static final RegistryKey<ConfiguredFeature<?, ?>> WILD_GREEN_BEAN_BUSH_KEY = registerKey("wild_green_bean_bush_patch");
    public static final RegistryKey<ConfiguredFeature<?, ?>> WILD_CUCUMBER_BUSH_KEY = registerKey("wild_cucumber_bush_patch");
    // --- End Add Keys ---

    public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> context) {
        // Sunflower (Existing)
        register(context, CUSTOM_SUNFLOWER_PATCH_KEY, Feature.RANDOM_PATCH,
                ConfiguredFeatures.createRandomPatchFeatureConfig(
                        64, // Tries per patch for sunflower
                        PlacedFeatures.createEntry(Feature.SIMPLE_BLOCK,
                                new SimpleBlockFeatureConfig(BlockStateProvider.of(ModBlocks.SUNFLOWER_BLOCK.get()))
                        )
                ));

        // --- Register Green Bean Bush Patch ---
        register(context, WILD_GREEN_BEAN_BUSH_KEY, Feature.RANDOM_PATCH,
                ConfiguredFeatures.createRandomPatchFeatureConfig(
                        18, // Fewer tries per patch than sunflowers, adjust as needed
                        PlacedFeatures.createEntry(Feature.SIMPLE_BLOCK,
                                // Ensure the bush starts seeded when generated naturally
                                new SimpleBlockFeatureConfig(BlockStateProvider.of(ModBlocks.WILD_GREEN_BEAN_BUSH.get().getDefaultState().with(net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock.SEEDED, true)))
                        )
                ));
        // --- End Register Green Bean ---

        // --- Register Cucumber Bush Patch ---
        register(context, WILD_CUCUMBER_BUSH_KEY, Feature.RANDOM_PATCH,
                ConfiguredFeatures.createRandomPatchFeatureConfig(
                        18, // Fewer tries per patch, adjust as needed
                        PlacedFeatures.createEntry(Feature.SIMPLE_BLOCK,
                                // Ensure the bush starts seeded when generated naturally
                                new SimpleBlockFeatureConfig(BlockStateProvider.of(ModBlocks.WILD_CUCUMBER_BUSH.get().getDefaultState().with(net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock.SEEDED, true)))
                        )
                ));
        // --- End Register Cucumber ---
    }

    // Helper methods (Existing)
    public static RegistryKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(AdorableHamsterPets.MOD_ID, name));
    }

    private static <FC extends FeatureConfig, F extends Feature<FC>> void register(Registerable<ConfiguredFeature<?, ?>> context,
                                                                                   RegistryKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}