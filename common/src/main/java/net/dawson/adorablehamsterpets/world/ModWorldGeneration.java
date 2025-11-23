package net.dawson.adorablehamsterpets.world;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ModWorldGeneration {

    // --- Caches for Parsed Config Values ---
    private static final Set<Identifier> SUNFLOWER_IDS = new HashSet<>();
    private static final Set<TagKey<Biome>> SUNFLOWER_TAGS = new HashSet<>();
    private static final Set<Identifier> GREEN_BEAN_BUSH_IDS = new HashSet<>();
    private static final Set<TagKey<Biome>> GREEN_BEAN_BUSH_TAGS = new HashSet<>();
    private static final Set<TagKey<Biome>> GREEN_BEAN_BUSH_CONVENTION_TAGS = new HashSet<>();
    private static final Set<Identifier> GREEN_BEAN_BUSH_EXCLUSIONS = new HashSet<>();
    private static final Set<Identifier> CUCUMBER_BUSH_IDS = new HashSet<>();
    private static final Set<TagKey<Biome>> CUCUMBER_BUSH_TAGS = new HashSet<>();
    private static final Set<TagKey<Biome>> CUCUMBER_BUSH_CONVENTION_TAGS = new HashSet<>();
    private static final Set<Identifier> CUCUMBER_BUSH_EXCLUSIONS = new HashSet<>();

    public static void generateModWorldGen() {
        AdorableHamsterPets.LOGGER.info("Registering Biome Modifications for " + AdorableHamsterPets.MOD_ID);
        registerBiomeModifications();
    }

    @ExpectPlatform
    public static void registerBiomeModifications() {
        throw new AssertionError();
    }

    /**
     * Parses the feature generation lists from the config file into Sets for efficient lookup.
     * This should be called once during mod initialization.
     */
    public static void parseConfig() {
        // --- Clear all sets to allow for config reloading ---
        SUNFLOWER_IDS.clear();
        SUNFLOWER_TAGS.clear();
        GREEN_BEAN_BUSH_IDS.clear();
        GREEN_BEAN_BUSH_TAGS.clear();
        GREEN_BEAN_BUSH_CONVENTION_TAGS.clear();
        GREEN_BEAN_BUSH_EXCLUSIONS.clear();
        CUCUMBER_BUSH_IDS.clear();
        CUCUMBER_BUSH_TAGS.clear();
        CUCUMBER_BUSH_CONVENTION_TAGS.clear();
        CUCUMBER_BUSH_EXCLUSIONS.clear();

        // --- Parse Sunflowers ---
        Configs.AHP.sunflowerBiomes.forEach(idStr -> parseIdentifier(idStr, SUNFLOWER_IDS, "sunflowerBiomes"));
        Configs.AHP.sunflowerBiomeTags.forEach(tagStr -> parseTag(tagStr, SUNFLOWER_TAGS, "sunflowerBiomeTags"));

        // --- Parse Green Bean Bushes ---
        Configs.AHP.greenBeanBushBiomes.forEach(idStr -> parseIdentifier(idStr, GREEN_BEAN_BUSH_IDS, "greenBeanBushBiomes"));
        Configs.AHP.greenBeanBushTags.forEach(tagStr -> parseTag(tagStr, GREEN_BEAN_BUSH_TAGS, "greenBeanBushTags"));
        Configs.AHP.greenBeanBushConventionTags.forEach(tagStr -> parseTag(tagStr, GREEN_BEAN_BUSH_CONVENTION_TAGS, "greenBeanBushConventionTags"));
        Configs.AHP.greenBeanBushExclusions.forEach(idStr -> parseIdentifier(idStr, GREEN_BEAN_BUSH_EXCLUSIONS, "greenBeanBushExclusions"));

        // --- Parse Cucumber Bushes ---
        Configs.AHP.cucumberBushBiomes.forEach(idStr -> parseIdentifier(idStr, CUCUMBER_BUSH_IDS, "cucumberBushBiomes"));
        Configs.AHP.cucumberBushTags.forEach(tagStr -> parseTag(tagStr, CUCUMBER_BUSH_TAGS, "cucumberBushTags"));
        Configs.AHP.cucumberBushConventionTags.forEach(tagStr -> parseTag(tagStr, CUCUMBER_BUSH_CONVENTION_TAGS, "cucumberBushConventionTags"));
        Configs.AHP.cucumberBushExclusions.forEach(idStr -> parseIdentifier(idStr, CUCUMBER_BUSH_EXCLUSIONS, "cucumberBushExclusions"));

        AdorableHamsterPets.LOGGER.info("[FeatureConfig] Parsed feature generation settings from config.");
    }

    /**
     * The NeoForge-specific decider method for feature placement, driven by the parsed config.
     * For sunflowers, it also verifies that the biome already contains the vanilla sunflower feature. Fabric does this same filtering, but in the Fabric/`ModWorldGenerationImpl` class instead.
     *
     * @param feature The PlacedFeature being considered for generation.
     * @param biome   The Biome where the feature might be placed.
     * @return True if the feature should spawn in this biome according to config rules.
     */
    public static boolean shouldFeatureSpawnInBiome(RegistryEntry<PlacedFeature> feature, RegistryEntry<Biome> biome) {
        Identifier featureId = feature.getKey().map(RegistryKey::getValue).orElse(null);
        Identifier biomeId = biome.getKey().map(RegistryKey::getValue).orElse(null);

        if (featureId == null || biomeId == null) {
            return false;
        }

        String featurePath = featureId.getPath();

        boolean isCandidate = switch (featurePath) {
            case "custom_sunflower_placed", "patch_sunflower" -> {
                boolean isAllowedByConfig = SUNFLOWER_IDS.contains(biomeId) || SUNFLOWER_TAGS.stream().anyMatch(biome::isIn);
                if (!isAllowedByConfig) yield false;

                List<RegistryEntryList<PlacedFeature>> allFeaturesByStep = biome.value().getGenerationSettings().getFeatures();
                int vegetalStep = GenerationStep.Feature.VEGETAL_DECORATION.ordinal();

                if (vegetalStep >= allFeaturesByStep.size()) yield false;

                RegistryEntryList<PlacedFeature> vegetalFeatures = allFeaturesByStep.get(vegetalStep);
                // Iterate through the entries and check their keys.
                for (RegistryEntry<PlacedFeature> entry : vegetalFeatures) {
                    if (entry.matchesKey(VegetationPlacedFeatures.PATCH_SUNFLOWER)) {
                        yield true;
                    }
                }
                yield false;
            }
            case "wild_green_bean_bush_placed" -> GREEN_BEAN_BUSH_IDS.contains(biomeId) ||
                    GREEN_BEAN_BUSH_TAGS.stream().anyMatch(biome::isIn) ||
                    GREEN_BEAN_BUSH_CONVENTION_TAGS.stream().anyMatch(biome::isIn);
            case "wild_cucumber_bush_placed" -> CUCUMBER_BUSH_IDS.contains(biomeId) ||
                    CUCUMBER_BUSH_TAGS.stream().anyMatch(biome::isIn) ||
                    CUCUMBER_BUSH_CONVENTION_TAGS.stream().anyMatch(biome::isIn);
            default -> false;
        };

        if (!isCandidate) {
            return false;
        }

        // Apply exclusions as the final veto
        return switch (featurePath) {
            case "wild_green_bean_bush_placed" -> !GREEN_BEAN_BUSH_EXCLUSIONS.contains(biomeId);
            case "wild_cucumber_bush_placed" -> !CUCUMBER_BUSH_EXCLUSIONS.contains(biomeId);
            default -> true; // Sunflowers and vanilla features have no exclusion list in this system.
        };
    }

    /**
     * The Fabric-specific decider method for feature placement, driven by the parsed config.
     * This version uses Architectury's BiomeContext for compatibility.
     *
     * @param featureKey The RegistryKey of the PlacedFeature being considered.
     * @param context    The BiomeContext for the biome where the feature might be placed.
     * @return True if the feature should spawn in this biome according to config rules.
     */
    public static boolean shouldFeatureSpawnInBiome(RegistryKey<PlacedFeature> featureKey, BiomeModifications.BiomeContext context) {
        Identifier biomeId = context.getKey().orElse(null);
        if (biomeId == null) {
            return false;
        }

        String featurePath = featureKey.getValue().getPath();

        boolean isCandidate = switch (featurePath) {
            case "custom_sunflower_placed", "patch_sunflower" -> SUNFLOWER_IDS.contains(biomeId) ||
                    SUNFLOWER_TAGS.stream().anyMatch(context::hasTag);
            case "wild_green_bean_bush_placed" -> GREEN_BEAN_BUSH_IDS.contains(biomeId) ||
                    GREEN_BEAN_BUSH_TAGS.stream().anyMatch(context::hasTag) ||
                    GREEN_BEAN_BUSH_CONVENTION_TAGS.stream().anyMatch(context::hasTag);
            case "wild_cucumber_bush_placed" -> CUCUMBER_BUSH_IDS.contains(biomeId) ||
                    CUCUMBER_BUSH_TAGS.stream().anyMatch(context::hasTag) ||
                    CUCUMBER_BUSH_CONVENTION_TAGS.stream().anyMatch(context::hasTag);
            default -> false;
        };

        if (!isCandidate) {
            return false;
        }

        // Apply exclusions as the final veto
        return switch (featurePath) {
            case "wild_green_bean_bush_placed" -> !GREEN_BEAN_BUSH_EXCLUSIONS.contains(biomeId);
            case "wild_cucumber_bush_placed" -> !CUCUMBER_BUSH_EXCLUSIONS.contains(biomeId);
            default -> true;
        };
    }

    // --- Private Helper Methods for Parsing ---
    private static void parseIdentifier(String idStr, Set<Identifier> set, String configListName) {
        try {
            set.add(Identifier.of(idStr));
        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.info("[FeatureConfig] Invalid identifier in '{}' config list: '{}'", configListName, idStr);
        }
    }

    private static void parseTag(String tagStr, Set<TagKey<Biome>> set, String configListName) {
        try {
            set.add(TagKey.of(RegistryKeys.BIOME, Identifier.of(tagStr)));
        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.info("[FeatureConfig] Invalid biome tag identifier in '{}' config list: '{}'", configListName, tagStr);
        }
    }
}