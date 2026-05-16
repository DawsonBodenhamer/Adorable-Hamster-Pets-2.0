package net.dawson.adorablehamsterpets.config;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterColorZone;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;

import java.util.*;

/**
 * A static cache for data parsed from the mod's configuration files.
 * <p>
 * This class loads user-defined lists of items and biomes from AhpConfig.java
 * into high-performance {@code Set} collections on startup. It provides static
 * checker methods (e.g., {@code isStandardFood()}, {@code isBlueBiome()}) for fast,
 * O(1) lookups during gameplay, avoiding repeated config parsing.
 */
public class ConfigDataCache {

    // Inner record for spawning
    public record EnvironmentDefinition(
            Set<Identifier> biomes,
            Set<TagKey<Biome>> tags,
            Set<Identifier> excludedBiomes,
            Set<TagKey<Biome>> excludedTags,
            Map<HamsterColorZone, Integer> weights
    ) {}

    // --- Cached Sets for Item Performance ---
    private static final Set<Item> tamingItems = new HashSet<>();
    private static final Set<TagKey<Item>> tamingTags = new HashSet<>();
    private static final Set<Item> standardFoodItems = new HashSet<>();
    private static final Set<TagKey<Item>> standardFoodTags = new HashSet<>();
    private static final Set<Item> stealableItems = new HashSet<>();
    private static final Set<TagKey<Item>> stealableTags = new HashSet<>();
    private static final Set<Item> retrievableItems = new HashSet<>();
    private static final Set<TagKey<Item>> retrievableItemTags = new HashSet<>();
    private static final Set<Item> buffFoodItems = new HashSet<>();
    private static final Set<TagKey<Item>> buffFoodTags = new HashSet<>();
    private static final Set<Item> lureItems = new HashSet<>();
    private static final Set<TagKey<Item>> lureItemTags = new HashSet<>();
    private static final Set<Item> bedAvoidanceFoodItems = new HashSet<>();
    private static final Set<TagKey<Item>> bedAvoidanceFoodTags = new HashSet<>();
    private static final Set<Item> pouchUnlockItems = new HashSet<>();
    private static final Set<TagKey<Item>> pouchUnlockTags = new HashSet<>();
    private static final Set<Item> repeatableFoodItems = new HashSet<>();
    private static final Set<TagKey<Item>> repeatableFoodTags = new HashSet<>();
    private static final Set<Item> pouchAllowedItems = new HashSet<>();
    private static final Set<Item> autoHealFoodItems = new HashSet<>();
    private static final Set<TagKey<Item>> autoHealFoodTags = new HashSet<>();
    private static final Set<TagKey<Item>> pouchAllowedTags = new HashSet<>();
    private static final Set<Item> pouchDisallowedItems = new HashSet<>();
    private static final Set<TagKey<Item>> pouchDisallowedTags = new HashSet<>();
    private static final Set<Item> resurrectionTributeItems = new HashSet<>();
    private static final Set<TagKey<Item>> resurrectionTributeTags = new HashSet<>();
    private static final Set<Item> becomePacifistItems = new HashSet<>();
    private static final Set<TagKey<Item>> becomePacifistTags = new HashSet<>();
    private static final Set<Item> becomeNeutralItems = new HashSet<>();
    private static final Set<TagKey<Item>> becomeNeutralTags = new HashSet<>();
    private static final Set<Item> becomeMenaceItems = new HashSet<>();
    private static final Set<TagKey<Item>> becomeMenaceTags = new HashSet<>();

    // --- Cached Sets for Entity Performance ---
    private static final Set<EntityType<?>> menaceTargetEntities = new HashSet<>();
    private static final Set<TagKey<EntityType<?>>> menaceTargetTags = new HashSet<>();

    // --- Cached Sets for Block Performance ---
    private static final Set<Block> celebrationOreBlocks = new HashSet<>();
    private static final Set<TagKey<Block>> celebrationOreTags = new HashSet<>();
    private static final Set<Block> sulkingOreBlocks = new HashSet<>();
    private static final Set<TagKey<Block>> sulkingOreTags = new HashSet<>();
    private static final Set<Block> heistableLeavesBlocks = new HashSet<>();
    private static final Set<TagKey<Block>> heistableLeavesTags = new HashSet<>();
    private static final Set<Block> heistableLogsBlocks = new HashSet<>();
    private static final Set<TagKey<Block>> heistableLogsTags = new HashSet<>();

    // --- Cached Lists for Environment-Spawning Performance ---
    private static final List<EnvironmentDefinition> ENVIRONMENTS = new ArrayList<>();
    private static Map<HamsterColorZone, Integer> FALLBACK_WEIGHTS = new EnumMap<>(HamsterColorZone.class);
    private static final Set<HamsterColorZone> allowedWildOverlayZones = new HashSet<>();
    private static final Set<HamsterColorZone> restrictedBaseZones = new HashSet<>();
    private static final Set<HamsterColorZone> clashingOverlayZones = new HashSet<>();

    public static Set<HamsterColorZone> getAllowedWildOverlayZones() { return allowedWildOverlayZones; }
    public static Set<HamsterColorZone> getRestrictedBaseZones() { return restrictedBaseZones; }
    public static Set<HamsterColorZone> getClashingOverlayZones() { return clashingOverlayZones; }

    // --- Cached Lists for Loot Generation ---
    // Tags are expanded into individual items for generation logic
    private static final List<Item> flattenedDefaultCheekLoot = new ArrayList<>();
    private static final List<Item> flattenedExtraCheekLoot = new ArrayList<>();
    private static final List<Item> flattenedCaveCheekLoot = new ArrayList<>();
    private static final List<Item> flattenedCustomTagRewards = new ArrayList<>();

    /**
     * Parses all item and biome tag lists from the config file.
     * This should be called once on startup and on config reload.
     */
    public static void parseConfig() {
        clearAllItemSets();
        clearAllEntitySets();
        clearAllBlockSets();

        // --- Parse Item Lists ---
        parseItemList(Configs.AHP.tamingFoods, tamingItems, tamingTags, "tamingFoods");
        parseItemList(Configs.AHP.standardDiet, standardFoodItems, standardFoodTags, "standardDiet");
        parseItemList(Configs.AHP.stealableItems, stealableItems, stealableTags, "stealableItems");
        parseItemList(Configs.AHP.retrievableItems, retrievableItems, retrievableItemTags, "retrievableItems");
        parseItemList(Configs.AHP.buffFoods, buffFoodItems, buffFoodTags, "buffFoods");
        parseItemList(Configs.AHP.lureItems, lureItems, lureItemTags, "lureItems");
        parseItemList(Configs.AHP.bedAvoidanceFoods, bedAvoidanceFoodItems, bedAvoidanceFoodTags, "bedAvoidanceFoods");
        parseItemList(Configs.AHP.pouchUnlockFoods, pouchUnlockItems, pouchUnlockTags, "pouchUnlockFoods");
        parseItemList(Configs.AHP.repeatableFoods, repeatableFoodItems, repeatableFoodTags, "repeatableFoods");
        parseItemList(Configs.AHP.pouchAllowedItems, pouchAllowedItems, pouchAllowedTags, "pouchAllowedItems");
        parseItemList(Configs.AHP.pouchDisallowedItems, pouchDisallowedItems, pouchDisallowedTags, "pouchDisallowedItems");
        parseItemList(Configs.AHP.pouchDisallowedTags, pouchDisallowedItems, pouchDisallowedTags, "pouchDisallowedTags");
        parseItemList(Configs.AHP.autoHealFoods, autoHealFoodItems, autoHealFoodTags, "autoHealFoods");
        parseItemList(Configs.AHP.resurrectionTributes, resurrectionTributeItems, resurrectionTributeTags, "resurrectionTributes");
        parseLootGenerationList(Configs.AHP_WORLDGEN.defaultCheekLootList, flattenedDefaultCheekLoot, "defaultCheekLootList");
        parseLootGenerationList(Configs.AHP_WORLDGEN.extraCheekLootList, flattenedExtraCheekLoot, "extraCheekLootList");
        parseLootGenerationList(Configs.AHP_WORLDGEN.caveCheekLootList, flattenedCaveCheekLoot, "caveCheekLootList");
        parseLootGenerationList(Configs.AHP.customTagRewards, flattenedCustomTagRewards, "customTagRewards");
        parseItemList(Configs.AHP.becomePacifistItems, becomePacifistItems, becomePacifistTags, "becomePacifistItems");
        parseItemList(Configs.AHP.becomeNeutralItems, becomeNeutralItems, becomeNeutralTags, "becomeNeutralItems");
        parseItemList(Configs.AHP.becomeMenaceItems, becomeMenaceItems, becomeMenaceTags, "becomeMenaceItems");

        // --- Parse Entity Lists ---
        parseEntityList(Configs.AHP.menaceTargetEntities, menaceTargetEntities, menaceTargetTags, "menaceTargetEntities");

        // --- Parse Block Lists ---
        parseBlockList(Configs.AHP.celebrationOres, celebrationOreBlocks, celebrationOreTags, "celebrationOres");
        parseBlockList(Configs.AHP.sulkingOres, sulkingOreBlocks, sulkingOreTags, "sulkingOres");
        parseBlockList(Configs.AHP.heistableLeaves, heistableLeavesBlocks, heistableLeavesTags, "heistableLeaves");
        parseBlockList(Configs.AHP.heistableLogs, heistableLogsBlocks, heistableLogsTags, "heistableLogs");

        // --- Parse Region-Based Color Filters ---
        ENVIRONMENTS.clear();
        AhpWorldGenConfig wgc = Configs.AHP_WORLDGEN;

        ENVIRONMENTS.add(parseEnvironment(wgc.wildcardBiomes, wgc.wildcardTags, wgc.wildcardExclusionBiomes, wgc.wildcardExclusionTags, wgc.wildcardWeights, "Wildcard"));
        ENVIRONMENTS.add(parseEnvironment(wgc.icyBiomes, wgc.icyTags, wgc.icyExclusionBiomes, wgc.icyExclusionTags, wgc.icyWeights, "Icy"));
        ENVIRONMENTS.add(parseEnvironment(wgc.magicalBiomes, wgc.magicalTags, wgc.magicalExclusionBiomes, wgc.magicalExclusionTags, wgc.magicalWeights, "Magical"));
        ENVIRONMENTS.add(parseEnvironment(wgc.cherryBiomes, wgc.cherryTags, wgc.cherryExclusionBiomes, wgc.cherryExclusionTags, wgc.cherryWeights, "Cherry"));
        ENVIRONMENTS.add(parseEnvironment(wgc.snowyBiomes, wgc.snowyTags, wgc.snowyExclusionBiomes, wgc.snowyExclusionTags, wgc.snowyWeights, "Snowy"));
        ENVIRONMENTS.add(parseEnvironment(wgc.skyBiomes, wgc.skyTags, wgc.skyExclusionBiomes, wgc.skyExclusionTags, wgc.skyWeights, "Sky"));
        ENVIRONMENTS.add(parseEnvironment(wgc.rockyBiomes, wgc.rockyTags, wgc.rockyExclusionBiomes, wgc.rockyExclusionTags, wgc.rockyWeights, "Rocky"));
        ENVIRONMENTS.add(parseEnvironment(wgc.darkBiomes, wgc.darkTags, wgc.darkExclusionBiomes, wgc.darkExclusionTags, wgc.darkWeights, "Cave"));
        ENVIRONMENTS.add(parseEnvironment(wgc.sandyBiomes, wgc.sandyTags, wgc.sandyExclusionBiomes, wgc.sandyExclusionTags, wgc.sandyWeights, "Sandy"));
        ENVIRONMENTS.add(parseEnvironment(wgc.forestBiomes, wgc.forestTags, wgc.forestExclusionBiomes, wgc.forestExclusionTags, wgc.forestWeights, "Forested"));
        ENVIRONMENTS.add(parseEnvironment(wgc.auburnBiomes, wgc.auburnTags, wgc.auburnExclusionBiomes, wgc.auburnExclusionTags, wgc.auburnWeights, "Auburn"));

        // Fallback: Plains environment doesn't need biome checks, just the weights
        FALLBACK_WEIGHTS = parseWeights(wgc.plainsWeights, "Plains");

        // --- Parse Wild Overlays ---
        allowedWildOverlayZones.clear();
        for (String zoneStr : Configs.AHP_WORLDGEN.allowedWildOverlayZones) {
            try { allowedWildOverlayZones.add(HamsterColorZone.valueOf(zoneStr.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException e) { AdorableHamsterPets.LOGGER.warn("[ConfigDataCache] Invalid wild overlay zone '{}' in config.", zoneStr); }
        }

        restrictedBaseZones.clear();
        for (String zoneStr : Configs.AHP_WORLDGEN.restrictedBaseZones) {
            try { restrictedBaseZones.add(HamsterColorZone.valueOf(zoneStr.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException e) { AdorableHamsterPets.LOGGER.warn("[ConfigDataCache] Invalid restricted base zone '{}' in config.", zoneStr); }
        }

        clashingOverlayZones.clear();
        for (String zoneStr : Configs.AHP_WORLDGEN.clashingOverlayZones) {
            try { clashingOverlayZones.add(HamsterColorZone.valueOf(zoneStr.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException e) { AdorableHamsterPets.LOGGER.warn("[ConfigDataCache] Invalid clashing overlay zone '{}' in config.", zoneStr); }
        }

        AdorableHamsterPets.LOGGER.info("Parsed all config data into caches.");
    }

    // --- Public Item Checker Methods ---
    /**
     * Bundles all ingestible config lists into one check.
     */
    public static boolean isDietaryItem(ItemStack stack) {
        return isStandardFood(stack)
                || isBuffFood(stack)
                || isPouchUnlockFood(stack)
                || isTamingFood(stack)
                || isPacifistItem(stack)
                || isStandardAggressionItem(stack)
                || isMenaceItem(stack);
    }
    public static boolean isTamingFood(ItemStack stack) { return matchesItem(stack, tamingItems, tamingTags); }
    public static boolean isStandardFood(ItemStack stack) { return matchesItem(stack, standardFoodItems, standardFoodTags); }
    public static boolean isStealableItem(ItemStack stack) { return matchesItem(stack, stealableItems, stealableTags); }
    public static boolean isRetrievableItem(ItemStack stack) { return matchesItem(stack, retrievableItems, retrievableItemTags); }
    public static boolean isBuffFood(ItemStack stack) { return matchesItem(stack, buffFoodItems, buffFoodTags); }
    public static boolean isLureItem(ItemStack stack) { return matchesItem(stack, lureItems, lureItemTags); }
    public static boolean isBedAvoidanceFood(ItemStack stack) {return matchesItem(stack, bedAvoidanceFoodItems, bedAvoidanceFoodTags);}
    public static boolean isPouchUnlockFood(ItemStack stack) { return matchesItem(stack, pouchUnlockItems, pouchUnlockTags); }
    public static boolean isRepeatableFood(ItemStack stack) { return matchesItem(stack, repeatableFoodItems, repeatableFoodTags); }
    public static boolean isAutoHealFood(ItemStack stack) { return matchesItem(stack, autoHealFoodItems, autoHealFoodTags); }
    public static boolean isPouchAllowed(ItemStack stack) { return matchesItem(stack, pouchAllowedItems, pouchAllowedTags); }
    public static boolean isPouchDisallowed(ItemStack stack) { return matchesItem(stack, pouchDisallowedItems, pouchDisallowedTags); }
    public static boolean isResurrectionTribute(ItemStack stack) { return matchesItem(stack, resurrectionTributeItems, resurrectionTributeTags); }
    public static Item getRandomDefaultLootItem(Random random) {if (flattenedDefaultCheekLoot.isEmpty()) return Items.AIR;return flattenedDefaultCheekLoot.get(random.nextInt(flattenedDefaultCheekLoot.size()));}
    public static Item getRandomCustomLootItem(Random random) {if (flattenedExtraCheekLoot.isEmpty()) return Items.AIR;return flattenedExtraCheekLoot.get(random.nextInt(flattenedExtraCheekLoot.size()));}
    public static Item getRandomCaveLootItem(Random random) {if (flattenedCaveCheekLoot.isEmpty()) return Items.AIR;return flattenedCaveCheekLoot.get(random.nextInt(flattenedCaveCheekLoot.size()));}
    public static Item getRandomCustomTagReward(Random random) {if (flattenedCustomTagRewards.isEmpty()) return Items.AIR;return flattenedCustomTagRewards.get(random.nextInt(flattenedCustomTagRewards.size()));}
    public static boolean isPacifistItem(ItemStack stack) { return matchesItem(stack, becomePacifistItems, becomePacifistTags); }
    public static boolean isStandardAggressionItem(ItemStack stack) { return matchesItem(stack, becomeNeutralItems, becomeNeutralTags); }
    public static boolean isMenaceItem(ItemStack stack) { return matchesItem(stack, becomeMenaceItems, becomeMenaceTags); }

    // --- Public Entity Checker Methods ---
    public static boolean isMenaceTarget(LivingEntity entity) {
        if (entity == null) return false;
        EntityType<?> type = entity.getType();

        if (menaceTargetEntities.contains(type)) return true;
        for (TagKey<EntityType<?>> tag : menaceTargetTags) {
            if (type.isIn(tag)) return true;
        }

        // Smart fallback: If user included custom AHP monster tag,
        // fall back to the Monster interface to ensure all hostiles are caught
        if (Configs.AHP.menaceTargetEntities.contains("#adorablehamsterpets:monsters")) {
            if (entity instanceof Monster) return true;
        }
        return false;
    }

    // --- Public Block Checker Methods ---
    public static boolean isCelebrationOre(BlockState state) { return matchesBlock(state, celebrationOreBlocks, celebrationOreTags); }
    public static boolean isSulkingOre(BlockState state) { return matchesBlock(state, sulkingOreBlocks, sulkingOreTags); }
    public static boolean isHeistableLeaf(BlockState state) { return matchesBlock(state, heistableLeavesBlocks, heistableLeavesTags); }
    public static boolean isHeistableLog(BlockState state) { return matchesBlock(state, heistableLogsBlocks, heistableLogsTags); }

    // --- Public Environment Checker Methods ---
    /**
     * Determines which environment a biome belongs to and returns its configured zone weights.
     */
    public static Map<HamsterColorZone, Integer> getWeightsForBiome(RegistryEntry<Biome> biomeEntry) {
        for (EnvironmentDefinition env : ENVIRONMENTS) {
            if (matchesBiome(biomeEntry, env.biomes(), env.tags(), env.excludedBiomes(), env.excludedTags())) {
                return env.weights();
            }
        }
        return FALLBACK_WEIGHTS; // Fallback to Plains
    }

    // --- Private Helper Methods ---
    private static void parseItemList(List<String> configList, Set<Item> itemSet, Set<TagKey<Item>> tagSet, String listName) {
        for (String entry : configList) {
            if (entry.startsWith("#")) {
                try {
                    Identifier tagId = Identifier.of(entry.substring(1));
                    tagSet.add(TagKey.of(RegistryKeys.ITEM, tagId));
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[ItemTagManager] Invalid item tag identifier in '{}' config list: '{}'", listName, entry);
                }
            } else {
                try {
                    Identifier itemId = Identifier.of(entry);
                    Registries.ITEM.getOrEmpty(itemId).ifPresent(itemSet::add);
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[ItemTagManager] Invalid item identifier in '{}' config list: '{}'", listName, entry);
                }
            }
        }
    }

    /**
     * Parses a config list into a flat list of Items for generation purposes.
     * Tags (#) are resolved to all their contained items.
     */
    private static void parseLootGenerationList(List<String> configList, List<Item> targetList, String listName) {
        for (String entry : configList) {
            if (entry.startsWith("#")) {
                try {
                    Identifier tagId = Identifier.of(entry.substring(1));
                    TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, tagId);

                    Registries.ITEM.getEntryList(tagKey).ifPresent(entries -> {
                        for (var itemEntry : entries) {
                            targetList.add(itemEntry.value());
                        }
                    });
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[LootConfig] Invalid item tag in '{}': '{}'", listName, entry);
                }
            } else {
                try {
                    Identifier itemId = Identifier.of(entry);
                    Registries.ITEM.getOrEmpty(itemId).ifPresent(targetList::add);
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[LootConfig] Invalid item ID in '{}': '{}'", listName, entry);
                }
            }
        }
    }

    private static void parseEntityList(List<String> configList, Set<EntityType<?>> entitySet, Set<TagKey<EntityType<?>>> tagSet, String listName) {
        for (String entry : configList) {
            if (entry.startsWith("#")) {
                try {
                    Identifier tagId = Identifier.of(entry.substring(1));
                    tagSet.add(TagKey.of(RegistryKeys.ENTITY_TYPE, tagId));
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[EntityTagManager] Invalid entity tag identifier in '{}' config list: '{}'", listName, entry);
                }
            } else {
                try {
                    Identifier entityId = Identifier.of(entry);
                    Registries.ENTITY_TYPE.getOrEmpty(entityId).ifPresent(entitySet::add);
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[EntityTagManager] Invalid entity identifier in '{}' config list: '{}'", listName, entry);
                }
            }
        }
    }

    private static void parseBlockList(List<String> configList, Set<Block> blockSet, Set<TagKey<Block>> tagSet, String listName) {
        for (String entry : configList) {
            if (entry.startsWith("#")) {
                try {
                    Identifier tagId = Identifier.of(entry.substring(1));
                    tagSet.add(TagKey.of(RegistryKeys.BLOCK, tagId));
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[BlockTagManager] Invalid block tag identifier in '{}' config list: '{}'", listName, entry);
                }
            } else {
                try {
                    Identifier blockId = Identifier.of(entry);
                    Registries.BLOCK.getOrEmpty(blockId).ifPresent(blockSet::add);
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[BlockTagManager] Invalid block identifier in '{}' config list: '{}'", listName, entry);
                }
            }
        }
    }

    private static EnvironmentDefinition parseEnvironment(List<String> biomes, List<String> tags, List<String> exBiomes, List<String> exTags, List<String> weightStrings, String name) {
        Set<Identifier> bIds = new HashSet<>();
        Set<TagKey<Biome>> bTags = new HashSet<>();
        Set<Identifier> eIds = new HashSet<>();
        Set<TagKey<Biome>> eTags = new HashSet<>();

        parseBiomeIdList(biomes, bIds, name + " Biomes");
        parseBiomeTagList(tags, bTags, name + " Tags");
        parseBiomeIdList(exBiomes, eIds, name + " Exclusions");
        parseBiomeTagList(exTags, eTags, name + " Exclusion Tags");

        Map<HamsterColorZone, Integer> weights = parseWeights(weightStrings, name);
        return new EnvironmentDefinition(bIds, bTags, eIds, eTags, weights);
    }

    private static Map<HamsterColorZone, Integer> parseWeights(List<String> weightStrings, String envName) {
        Map<HamsterColorZone, Integer> weights = new EnumMap<>(HamsterColorZone.class);
        for (String str : weightStrings) {
            String[] parts = str.split(":");
            if (parts.length == 2) {
                try {
                    HamsterColorZone zone = HamsterColorZone.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
                    int weight = Integer.parseInt(parts[1].trim());
                    if (weight > 0) weights.put(zone, weight);
                } catch (IllegalArgumentException e) {
                    AdorableHamsterPets.LOGGER.warn("[ConfigDataCache] Invalid weight zone '{}' in {}", str, envName);
                }
            }
        }
        if (weights.isEmpty()) weights.put(HamsterColorZone.ORANGE, 100); // Absolute safety fallback
        return weights;
    }

    private static void parseBiomeIdList(List<String> configList, Set<Identifier> idSet, String listName) {
        for (String entry : configList) {
            try {
                idSet.add(Identifier.of(entry));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.warn("[BiomeTagManager] Invalid biome identifier in '{}' config list: '{}'", listName, entry);
            }
        }
    }

    private static void parseBiomeTagList(List<String> configList, Set<TagKey<Biome>> tagSet, String listName) {
        for (String entry : configList) {
            String tagName = entry.startsWith("#") ? entry.substring(1) : entry;
            try {
                tagSet.add(TagKey.of(RegistryKeys.BIOME, Identifier.of(tagName)));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.warn("[BiomeTagManager] Invalid biome tag in '{}' config list: '{}'", listName, entry);
            }
        }
    }

    private static boolean matchesItem(ItemStack stack, Set<Item> itemSet, Set<TagKey<Item>> tagSet) {
        if (stack.isEmpty()) return false;
        if (itemSet.contains(stack.getItem())) return true;
        for (TagKey<Item> tag : tagSet) {
            if (stack.isIn(tag)) return true;
        }
        return false;
    }

    private static boolean matchesBlock(BlockState state, Set<Block> blockSet, Set<TagKey<Block>> tagSet) {
        if (state == null) return false;
        // Check exact block ID
        if (blockSet.contains(state.getBlock())) return true;
        // Check tags
        for (TagKey<Block> tag : tagSet) {
            if (state.isIn(tag)) return true;
        }
        return false;
    }

    private static boolean matchesBiome(RegistryEntry<Biome> biomeEntry, Set<Identifier> ids, Set<TagKey<Biome>> tags, Set<Identifier> exclusionIds, Set<TagKey<Biome>> exclusionTags) {
        Identifier biomeId = biomeEntry.getKey().map(RegistryKey::getValue).orElse(null);
        if (biomeId == null) return false;

        // --- Exclusion Check (Highest Priority) ---
        if (exclusionIds.contains(biomeId)) return false;
        for (TagKey<Biome> tag : exclusionTags) {
            if (biomeEntry.isIn(tag)) return false;
        }

        // --- Inclusion Check ---
        if (ids.contains(biomeId)) return true;
        for (TagKey<Biome> tag : tags) {
            if (biomeEntry.isIn(tag)) return true;
        }

        return false;
    }

    private static void clearAllItemSets() {
        tamingItems.clear();
        tamingTags.clear();
        standardFoodItems.clear();
        standardFoodTags.clear();
        stealableItems.clear();
        stealableTags.clear();
        retrievableItems.clear();
        retrievableItemTags.clear();
        buffFoodItems.clear();
        buffFoodTags.clear();
        lureItems.clear();
        lureItemTags.clear();
        bedAvoidanceFoodItems.clear();
        bedAvoidanceFoodTags.clear();
        pouchUnlockItems.clear();
        pouchUnlockTags.clear();
        repeatableFoodItems.clear();
        repeatableFoodTags.clear();
        autoHealFoodItems.clear();
        autoHealFoodTags.clear();
        pouchAllowedItems.clear();
        pouchAllowedTags.clear();
        pouchDisallowedItems.clear();
        pouchDisallowedTags.clear();
        resurrectionTributeItems.clear();
        resurrectionTributeTags.clear();
        flattenedDefaultCheekLoot.clear();
        flattenedExtraCheekLoot.clear();
        flattenedCaveCheekLoot.clear();
        flattenedCustomTagRewards.clear();
        becomePacifistItems.clear();
        becomePacifistTags.clear();
        becomeNeutralItems.clear();
        becomeNeutralTags.clear();
        becomeMenaceItems.clear();
        becomeMenaceTags.clear();
    }

    private static void clearAllEntitySets() {
        menaceTargetEntities.clear();
        menaceTargetTags.clear();
    }

    private static void clearAllBlockSets() {
        celebrationOreBlocks.clear();
        celebrationOreTags.clear();
        sulkingOreBlocks.clear();
        sulkingOreTags.clear();
        heistableLeavesBlocks.clear();
        heistableLeavesTags.clear();
        heistableLogsBlocks.clear();
        heistableLogsTags.clear();
    }

    /**
     * Retrieves the localized display name of the first item found in a string configuration list.
     * <p>
     * This is designed for dynamic tooltips that need to reference a specific item required for an action
     * (e.g., "Right-click with [Item]"), ensuring the text adapts automatically if the user changes the config.
     *
     * @param configList The list of strings (Item IDs or Tags) from the config.
     * @return The formatted {@link Text} component of the item name. Returns the raw string if it is a tag
     *         or an invalid ID. Returns "Air" if the list is empty.
     */
    public static Text getFirstItemNameFromList(List<String> configList) {
        if (configList.isEmpty()) {
            return Text.translatable("block.minecraft.air");
        }

        String firstEntry = configList.get(0);
        if (firstEntry.startsWith("#")) {
            // If it's a tag, just return the tag string visually
            return Text.literal(firstEntry);
        } else {
            try {
                // Try to resolve the Item ID to a localized name
                Identifier itemId = Identifier.of(firstEntry);
                Item item = Registries.ITEM.get(itemId);

                // If registry returns default (Air) and input wasn't explicitly air, it's invalid or from a missing mod
                if (item == Items.AIR && !firstEntry.equals("minecraft:air")) {
                    return Text.literal(firstEntry);
                }
                return item.getName();
            } catch (Exception e) {
                // Fallback if ID is malformed
                return Text.literal(firstEntry);
            }
        }
    }
}