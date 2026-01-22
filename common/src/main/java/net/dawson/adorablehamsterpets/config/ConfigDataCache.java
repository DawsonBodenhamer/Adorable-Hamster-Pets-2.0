package net.dawson.adorablehamsterpets.config;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A static cache for data parsed from the mod's configuration files.
 * <p>
 * This class loads user-defined lists of items and biomes from AhpConfig.java
 * into high-performance {@code Set} collections on startup. It provides static
 * checker methods (e.g., {@code isStandardFood()}, {@code isBlueBiome()}) for fast,
 * O(1) lookups during gameplay, avoiding repeated config parsing.
 */
public class ConfigDataCache {

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

    // --- Cached Sets for Block Performance ---
    private static final Set<Block> celebrationOreBlocks = new HashSet<>();
    private static final Set<TagKey<Block>> celebrationOreTags = new HashSet<>();
    private static final Set<Block> sulkingOreBlocks = new HashSet<>();
    private static final Set<TagKey<Block>> sulkingOreTags = new HashSet<>();

    // --- Cached Sets for Biome Variant Performance ---
    private static final Set<Identifier> blueBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> blueBiomeTags = new HashSet<>();
    private static final Set<Identifier> blueExclusionBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> blueExclusionBiomeTags = new HashSet<>();
    private static final Set<Identifier> lavenderBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> lavenderBiomeTags = new HashSet<>();
    private static final Set<Identifier> lavenderExclusionBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> lavenderExclusionBiomeTags = new HashSet<>();
    private static final Set<Identifier> whiteBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> whiteBiomeTags = new HashSet<>();
    private static final Set<Identifier> whiteExclusionBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> whiteExclusionBiomeTags = new HashSet<>();
    private static final Set<Identifier> grayBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> grayBiomeTags = new HashSet<>();
    private static final Set<Identifier> grayExclusionBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> grayExclusionBiomeTags = new HashSet<>();
    private static final Set<Identifier> blackBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> blackBiomeTags = new HashSet<>();
    private static final Set<Identifier> blackExclusionBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> blackExclusionBiomeTags = new HashSet<>();
    private static final Set<Identifier> creamBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> creamBiomeTags = new HashSet<>();
    private static final Set<Identifier> creamExclusionBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> creamExclusionBiomeTags = new HashSet<>();
    private static final Set<Identifier> chocolateBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> chocolateBiomeTags = new HashSet<>();
    private static final Set<Identifier> chocolateExclusionBiomeIds = new HashSet<>();
    private static final Set<TagKey<Biome>> chocolateExclusionBiomeTags = new HashSet<>();


    /**
     * Parses all item and biome tag lists from the config file.
     * This should be called once on startup and on config reload.
     */
    public static void parseConfig() {
        clearAllItemSets();
        clearAllBlockSets();
        clearAllBiomeSets();

        // --- Parse Item Lists ---
        parseItemList(Configs.AHP.tamingFoods, tamingItems, tamingTags, "tamingFoods");
        parseItemList(Configs.AHP.standardFoods, standardFoodItems, standardFoodTags, "standardFoods");
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

        // --- Parse Block Lists ---
        parseBlockList(Configs.AHP.celebrationOres, celebrationOreBlocks, celebrationOreTags, "celebrationOres");
        parseBlockList(Configs.AHP.sulkingOres, sulkingOreBlocks, sulkingOreTags, "sulkingOres");

        // --- Parse Biome Lists ---
        parseBiomeIdList(Configs.AHP_WORLDGEN.blueBiomes, blueBiomeIds, "blueBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.blueTags, blueBiomeTags, "blueTags");
        parseBiomeIdList(Configs.AHP_WORLDGEN.blueExclusionBiomes, blueExclusionBiomeIds, "blueExclusionBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.blueExclusionTags, blueExclusionBiomeTags, "blueExclusionTags");

        parseBiomeIdList(Configs.AHP_WORLDGEN.lavenderBiomes, lavenderBiomeIds, "lavenderBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.lavenderTags, lavenderBiomeTags, "lavenderTags");
        parseBiomeIdList(Configs.AHP_WORLDGEN.lavenderExclusionBiomes, lavenderExclusionBiomeIds, "lavenderExclusionBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.lavenderExclusionTags, lavenderExclusionBiomeTags, "lavenderExclusionTags");

        parseBiomeIdList(Configs.AHP_WORLDGEN.whiteBiomes, whiteBiomeIds, "whiteBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.whiteTags, whiteBiomeTags, "whiteTags");
        parseBiomeIdList(Configs.AHP_WORLDGEN.whiteExclusionBiomes, whiteExclusionBiomeIds, "whiteExclusionBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.whiteExclusionTags, whiteExclusionBiomeTags, "whiteExclusionTags");

        parseBiomeIdList(Configs.AHP_WORLDGEN.grayBiomes, grayBiomeIds, "grayBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.grayTags, grayBiomeTags, "grayTags");
        parseBiomeIdList(Configs.AHP_WORLDGEN.grayExclusionBiomes, grayExclusionBiomeIds, "grayExclusionBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.grayExclusionTags, grayExclusionBiomeTags, "grayExclusionTags");

        parseBiomeIdList(Configs.AHP_WORLDGEN.blackBiomes, blackBiomeIds, "blackBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.blackTags, blackBiomeTags, "blackTags");
        parseBiomeIdList(Configs.AHP_WORLDGEN.blackExclusionBiomes, blackExclusionBiomeIds, "blackExclusionBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.blackExclusionTags, blackExclusionBiomeTags, "blackExclusionTags");

        parseBiomeIdList(Configs.AHP_WORLDGEN.creamBiomes, creamBiomeIds, "creamBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.creamTags, creamBiomeTags, "creamTags");
        parseBiomeIdList(Configs.AHP_WORLDGEN.creamExclusionBiomes, creamExclusionBiomeIds, "creamExclusionBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.creamExclusionTags, creamExclusionBiomeTags, "creamExclusionTags");

        parseBiomeIdList(Configs.AHP_WORLDGEN.chocolateBiomes, chocolateBiomeIds, "chocolateBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.chocolateTags, chocolateBiomeTags, "chocolateTags");
        parseBiomeIdList(Configs.AHP_WORLDGEN.chocolateExclusionBiomes, chocolateExclusionBiomeIds, "chocolateExclusionBiomes");
        parseBiomeTagList(Configs.AHP_WORLDGEN.chocolateExclusionTags, chocolateExclusionBiomeTags, "chocolateExclusionTags");

        AdorableHamsterPets.LOGGER.info("Parsed all item and biome tag overrides from config.");
    }

    // --- Public Item Checker Methods ---
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

    // --- Public Block Checker Methods ---
    public static boolean isCelebrationOre(BlockState state) { return matchesBlock(state, celebrationOreBlocks, celebrationOreTags); }
    public static boolean isSulkingOre(BlockState state) { return matchesBlock(state, sulkingOreBlocks, sulkingOreTags); }

    // --- Public Biome Checker Methods ---
    public static boolean isBlueBiome(RegistryEntry<Biome> biomeEntry) { return matchesBiome(biomeEntry, blueBiomeIds, blueBiomeTags, blueExclusionBiomeIds, blueExclusionBiomeTags); }
    public static boolean isLavenderBiome(RegistryEntry<Biome> biomeEntry) { return matchesBiome(biomeEntry, lavenderBiomeIds, lavenderBiomeTags, lavenderExclusionBiomeIds, lavenderExclusionBiomeTags); }
    public static boolean isWhiteBiome(RegistryEntry<Biome> biomeEntry) { return matchesBiome(biomeEntry, whiteBiomeIds, whiteBiomeTags, whiteExclusionBiomeIds, whiteExclusionBiomeTags); }
    public static boolean isGrayBiome(RegistryEntry<Biome> biomeEntry) { return matchesBiome(biomeEntry, grayBiomeIds, grayBiomeTags, grayExclusionBiomeIds, grayExclusionBiomeTags); }
    public static boolean isBlackBiome(RegistryEntry<Biome> biomeEntry) { return matchesBiome(biomeEntry, blackBiomeIds, blackBiomeTags, blackExclusionBiomeIds, blackExclusionBiomeTags); }
    public static boolean isCreamBiome(RegistryEntry<Biome> biomeEntry) { return matchesBiome(biomeEntry, creamBiomeIds, creamBiomeTags, creamExclusionBiomeIds, creamExclusionBiomeTags); }
    public static boolean isChocolateBiome(RegistryEntry<Biome> biomeEntry) { return matchesBiome(biomeEntry, chocolateBiomeIds, chocolateBiomeTags, chocolateExclusionBiomeIds, chocolateExclusionBiomeTags); }

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
    }

    private static void clearAllBlockSets() {
        celebrationOreBlocks.clear();
        celebrationOreTags.clear();
        sulkingOreBlocks.clear();
        sulkingOreTags.clear();
    }

    private static void clearAllBiomeSets() {
        blueBiomeIds.clear();
        blueBiomeTags.clear();
        blueExclusionBiomeIds.clear();
        blueExclusionBiomeTags.clear();
        lavenderBiomeIds.clear();
        lavenderBiomeTags.clear();
        lavenderExclusionBiomeIds.clear();
        lavenderExclusionBiomeTags.clear();
        whiteBiomeIds.clear();
        whiteBiomeTags.clear();
        whiteExclusionBiomeIds.clear();
        whiteExclusionBiomeTags.clear();
        grayBiomeIds.clear();
        grayBiomeTags.clear();
        grayExclusionBiomeIds.clear();
        grayExclusionBiomeTags.clear();
        blackBiomeIds.clear();
        blackBiomeTags.clear();
        blackExclusionBiomeIds.clear();
        blackExclusionBiomeTags.clear();
        creamBiomeIds.clear();
        creamBiomeTags.clear();
        creamExclusionBiomeIds.clear();
        creamExclusionBiomeTags.clear();
        chocolateBiomeIds.clear();
        chocolateBiomeTags.clear();
        chocolateExclusionBiomeIds.clear();
        chocolateExclusionBiomeTags.clear();
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