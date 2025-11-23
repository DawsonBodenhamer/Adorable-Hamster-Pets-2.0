package net.dawson.adorablehamsterpets.world.fabric;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.feature.ModPlacedFeatures;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

import java.util.function.Predicate;

public class ModWorldGenerationImpl {
    public static void registerBiomeModifications() {
        // A universal predicate that allows our decider logic to run for every biome.
        Predicate<BiomeModifications.BiomeContext> universalSelector = context -> true;

        BiomeModifications.addProperties(universalSelector, (context, props) -> {
            // --- 1. Sunflower Replacement Logic ---
            // First, check if the biome is even a candidate according to the config.
            if (ModWorldGeneration.shouldFeatureSpawnInBiome(ModPlacedFeatures.CUSTOM_SUNFLOWER_PLACED_KEY, context)) {
                // Second, check if this biome actually has the vanilla sunflower feature.
                boolean hasVanillaSunflower = false;
                for (RegistryEntry<PlacedFeature> entry : props.getGenerationProperties().getFeatures(GenerationStep.Feature.VEGETAL_DECORATION)) {
                    if (entry.matchesKey(VegetationPlacedFeatures.PATCH_SUNFLOWER)) {
                        hasVanillaSunflower = true;
                        break;
                    }
                }

                // Only if BOTH conditions are true, perform the replacement.
                if (hasVanillaSunflower) {
                    props.getGenerationProperties().removeFeature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_SUNFLOWER);
                    props.getGenerationProperties().addFeature(GenerationStep.Feature.VEGETAL_DECORATION, ModPlacedFeatures.CUSTOM_SUNFLOWER_PLACED_KEY);
                }
            }

            // --- 2. Bush Addition Logic ---
            if (ModWorldGeneration.shouldFeatureSpawnInBiome(ModPlacedFeatures.WILD_GREEN_BEAN_BUSH_PLACED_KEY, context)) {
                props.getGenerationProperties().addFeature(GenerationStep.Feature.VEGETAL_DECORATION, ModPlacedFeatures.WILD_GREEN_BEAN_BUSH_PLACED_KEY);
            }
            if (ModWorldGeneration.shouldFeatureSpawnInBiome(ModPlacedFeatures.WILD_CUCUMBER_BUSH_PLACED_KEY, context)) {
                props.getGenerationProperties().addFeature(GenerationStep.Feature.VEGETAL_DECORATION, ModPlacedFeatures.WILD_CUCUMBER_BUSH_PLACED_KEY);
            }
        });
    }
}