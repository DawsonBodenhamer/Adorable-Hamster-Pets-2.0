package net.dawson.adorablehamsterpets.world.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import java.util.Optional;

public record ConfigurableFeatureModifier(
        RegistryEntryList<Biome> biomes,
        Optional<RegistryEntryList<PlacedFeature>> featuresToAdd,
        Optional<RegistryEntryList<PlacedFeature>> featuresToRemove
) implements BiomeModifier {

    public static final MapCodec<ConfigurableFeatureModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    RegistryCodecs.entryList(RegistryKeys.BIOME).fieldOf("biomes").forGetter(ConfigurableFeatureModifier::biomes),
                    RegistryCodecs.entryList(RegistryKeys.PLACED_FEATURE).optionalFieldOf("features_to_add").forGetter(ConfigurableFeatureModifier::featuresToAdd),
                    RegistryCodecs.entryList(RegistryKeys.PLACED_FEATURE).optionalFieldOf("features_to_remove").forGetter(ConfigurableFeatureModifier::featuresToRemove)
            ).apply(instance, ConfigurableFeatureModifier::new)
    );

    @Override
    public void modify(RegistryEntry<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            this.featuresToAdd.ifPresent(additions -> {
                for (RegistryEntry<PlacedFeature> feature : additions) {
                    // Use centralized helper methods to decide if the feature should be added to this specific biome
                    if (ModWorldGeneration.shouldFeatureSpawnInBiome(feature, biome)) {
                        builder.getGenerationSettings().getFeatures(GenerationStep.Feature.VEGETAL_DECORATION).add(feature);
                    }
                }
            });
        }
        if (phase == Phase.REMOVE && this.biomes.contains(biome)) {
            this.featuresToRemove.ifPresent(removals -> {
                for (RegistryEntry<PlacedFeature> feature : removals) {
                    if (ModWorldGeneration.shouldFeatureSpawnInBiome(feature, biome)) {
                        builder.getGenerationSettings().getFeatures(GenerationStep.Feature.VEGETAL_DECORATION).remove(feature);
                    }
                }
            });
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.CONFIGURABLE_FEATURE_MODIFIER.get();
    }
}