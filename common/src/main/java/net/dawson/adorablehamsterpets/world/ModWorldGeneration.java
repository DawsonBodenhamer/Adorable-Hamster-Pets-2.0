package net.dawson.adorablehamsterpets.world;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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
        Configs.AHP_WORLDGEN.sunflowerBiomes.forEach(idStr -> parseIdentifier(idStr, SUNFLOWER_IDS, "sunflowerBiomes"));
        Configs.AHP_WORLDGEN.sunflowerBiomeTags.forEach(tagStr -> parseTag(tagStr, SUNFLOWER_TAGS, "sunflowerBiomeTags"));

        // --- Parse Green Bean Bushes ---
        Configs.AHP_WORLDGEN.greenBeanBushBiomes.forEach(idStr -> parseIdentifier(idStr, GREEN_BEAN_BUSH_IDS, "greenBeanBushBiomes"));
        Configs.AHP_WORLDGEN.greenBeanBushTags.forEach(tagStr -> parseTag(tagStr, GREEN_BEAN_BUSH_TAGS, "greenBeanBushTags"));
        Configs.AHP_WORLDGEN.greenBeanBushConventionTags.forEach(tagStr -> parseTag(tagStr, GREEN_BEAN_BUSH_CONVENTION_TAGS, "greenBeanBushConventionTags"));
        Configs.AHP_WORLDGEN.greenBeanBushExclusions.forEach(idStr -> parseIdentifier(idStr, GREEN_BEAN_BUSH_EXCLUSIONS, "greenBeanBushExclusions"));

        // --- Parse Cucumber Bushes ---
        Configs.AHP_WORLDGEN.cucumberBushBiomes.forEach(idStr -> parseIdentifier(idStr, CUCUMBER_BUSH_IDS, "cucumberBushBiomes"));
        Configs.AHP_WORLDGEN.cucumberBushTags.forEach(tagStr -> parseTag(tagStr, CUCUMBER_BUSH_TAGS, "cucumberBushTags"));
        Configs.AHP_WORLDGEN.cucumberBushConventionTags.forEach(tagStr -> parseTag(tagStr, CUCUMBER_BUSH_CONVENTION_TAGS, "cucumberBushConventionTags"));
        Configs.AHP_WORLDGEN.cucumberBushExclusions.forEach(idStr -> parseIdentifier(idStr, CUCUMBER_BUSH_EXCLUSIONS, "cucumberBushExclusions"));

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
    public static boolean shouldFeatureSpawnInBiome(Holder<PlacedFeature> feature, Holder<Biome> biome) {
        Identifier featureId = feature.unwrapKey().map(ResourceKey::identifier).orElse(null);
        Identifier biomeId = biome.unwrapKey().map(ResourceKey::identifier).orElse(null);

        if (featureId == null || biomeId == null) {
            return false;
        }

        String featurePath = featureId.getPath();

        boolean isCandidate = switch (featurePath) {
            case "custom_sunflower_placed", "patch_sunflower" -> {
                boolean isAllowedByConfig = SUNFLOWER_IDS.contains(biomeId) || SUNFLOWER_TAGS.stream().anyMatch(biome::is);
                if (!isAllowedByConfig) yield false;

                List<HolderSet<PlacedFeature>> allFeaturesByStep = biome.value().getGenerationSettings().features();
                int vegetalStep = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();

                if (vegetalStep >= allFeaturesByStep.size()) yield false;

                HolderSet<PlacedFeature> vegetalFeatures = allFeaturesByStep.get(vegetalStep);
                // Iterate through the entries and check their keys.
                for (Holder<PlacedFeature> entry : vegetalFeatures) {
                    if (entry.is(VegetationPlacements.PATCH_SUNFLOWER)) {
                        yield true;
                    }
                }
                yield false;
            }
            case "wild_green_bean_bush_placed" -> GREEN_BEAN_BUSH_IDS.contains(biomeId) ||
                    GREEN_BEAN_BUSH_TAGS.stream().anyMatch(biome::is) ||
                    GREEN_BEAN_BUSH_CONVENTION_TAGS.stream().anyMatch(biome::is);
            case "wild_cucumber_bush_placed" -> CUCUMBER_BUSH_IDS.contains(biomeId) ||
                    CUCUMBER_BUSH_TAGS.stream().anyMatch(biome::is) ||
                    CUCUMBER_BUSH_CONVENTION_TAGS.stream().anyMatch(biome::is);
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
    public static boolean shouldFeatureSpawnInBiome(ResourceKey<PlacedFeature> featureKey, BiomeModifications.BiomeContext context) {
        Identifier biomeId = context.getKey().orElse(null);
        if (biomeId == null) {
            return false;
        }

        String featurePath = featureKey.identifier().getPath();

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
            set.add(Identifier.parse(idStr));
        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.info("[FeatureConfig] Invalid identifier in '{}' config list: '{}'", configListName, idStr);
        }
    }

    private static void parseTag(String tagStr, Set<TagKey<Biome>> set, String configListName) {
        try {
            set.add(TagKey.create(Registries.BIOME, Identifier.parse(tagStr)));
        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.info("[FeatureConfig] Invalid biome tag identifier in '{}' config list: '{}'", configListName, tagStr);
        }
    }
}