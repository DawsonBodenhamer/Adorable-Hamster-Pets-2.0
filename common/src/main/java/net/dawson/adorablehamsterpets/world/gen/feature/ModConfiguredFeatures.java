package net.dawson.adorablehamsterpets.world.gen.feature;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.WildCucumberBushBlock;
import net.dawson.adorablehamsterpets.block.custom.WildGreenBeanBushBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * 26.2 port: {@code random_patch} no longer exists. Like vanilla's flower
 * patches, each configured feature is now a plain {@code simple_block} and the
 * patch spread (tries, xz/y offsets, air check) lives in the placed feature.
 */
public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> CUSTOM_SUNFLOWER_PATCH_KEY = registerKey("custom_sunflower_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_GREEN_BEAN_BUSH_KEY = registerKey("wild_green_bean_bush_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_CUCUMBER_BUSH_KEY = registerKey("wild_cucumber_bush_patch");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        register(context, CUSTOM_SUNFLOWER_PATCH_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.SUNFLOWER_BLOCK.get())));
        register(context, WILD_GREEN_BEAN_BUSH_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(
                        ModBlocks.WILD_GREEN_BEAN_BUSH.get().defaultBlockState().setValue(WildGreenBeanBushBlock.SEEDED, true))));
        register(context, WILD_CUCUMBER_BUSH_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(
                        ModBlocks.WILD_CUCUMBER_BUSH.get().defaultBlockState().setValue(WildCucumberBushBlock.SEEDED, true))));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                   ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
