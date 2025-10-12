package net.dawson.adorablehamsterpets.tag;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModItemTags {

    // --- Cached Sets for Performance ---
    private static final Set<Item> tamingItems = new HashSet<>();
    private static final Set<TagKey<Item>> tamingTags = new HashSet<>();
    private static final Set<Item> standardFoodItems = new HashSet<>();
    private static final Set<TagKey<Item>> standardFoodTags = new HashSet<>();
    private static final Set<Item> stealableItems = new HashSet<>();
    private static final Set<TagKey<Item>> stealableTags = new HashSet<>();
    private static final Set<Item> buffFoodItems = new HashSet<>();
    private static final Set<TagKey<Item>> buffFoodTags = new HashSet<>();
    private static final Set<Item> shoulderMountItems = new HashSet<>();
    private static final Set<TagKey<Item>> shoulderMountTags = new HashSet<>();
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


    /**
     * Parses all item tag lists from the config file.
     * This should be called once on startup and on config reload.
     */
    public static void parseConfig() {
        clearAll();
        parseList(Configs.AHP.tamingFoods, tamingItems, tamingTags, "tamingFoods");
        parseList(Configs.AHP.standardFoods, standardFoodItems, standardFoodTags, "standardFoods");
        parseList(Configs.AHP.stealableItems, stealableItems, stealableTags, "stealableItems");
        parseList(Configs.AHP.buffFoods, buffFoodItems, buffFoodTags, "buffFoods");
        parseList(Configs.AHP.shoulderMountFoods, shoulderMountItems, shoulderMountTags, "shoulderMountFoods");
        parseList(Configs.AHP.pouchUnlockFoods, pouchUnlockItems, pouchUnlockTags, "pouchUnlockFoods");
        parseList(Configs.AHP.repeatableFoods, repeatableFoodItems, repeatableFoodTags, "repeatableFoods");
        parseList(Configs.AHP.pouchAllowedItems, pouchAllowedItems, pouchAllowedTags, "pouchAllowedItems");
        parseList(Configs.AHP.pouchDisallowedItems, pouchDisallowedItems, pouchDisallowedTags, "pouchDisallowedItems");
        parseList(Configs.AHP.pouchDisallowedTags, pouchDisallowedItems, pouchDisallowedTags, "pouchDisallowedTags");
        parseList(Configs.AHP.autoHealFoods, autoHealFoodItems, autoHealFoodTags, "autoHealFoods");
        AdorableHamsterPets.LOGGER.info("Parsed all item tag overrides from config.");
    }

    // --- Public Checker Methods ---
    public static boolean isTamingFood(ItemStack stack) {
        return matches(stack, tamingItems, tamingTags);
    }

    public static boolean isStandardFood(ItemStack stack) {
        return matches(stack, standardFoodItems, standardFoodTags);
    }

    public static boolean isStealableItem(ItemStack stack) {
        return matches(stack, stealableItems, stealableTags);
    }

    public static boolean isBuffFood(ItemStack stack) {
        return matches(stack, buffFoodItems, buffFoodTags);
    }

    public static boolean isShoulderMountFood(ItemStack stack) {
        return matches(stack, shoulderMountItems, shoulderMountTags);
    }

    public static boolean isPouchUnlockFood(ItemStack stack) {
        return matches(stack, pouchUnlockItems, pouchUnlockTags);
    }

    public static boolean isRepeatableFood(ItemStack stack) {
        return matches(stack, repeatableFoodItems, repeatableFoodTags);
    }

    public static boolean isAutoHealFood(ItemStack stack) {
        return matches(stack, autoHealFoodItems, autoHealFoodTags);
    }

    public static boolean isPouchAllowed(ItemStack stack) {
        return matches(stack, pouchAllowedItems, pouchAllowedTags);
    }

    public static boolean isPouchDisallowed(ItemStack stack) {
        return matches(stack, pouchDisallowedItems, pouchDisallowedTags);
    }

    // --- Private Helper Methods ---
    private static void parseList(List<String> configList, Set<Item> itemSet, Set<TagKey<Item>> tagSet, String listName) {
        for (String entry : configList) {
            if (entry.startsWith("#")) {
                try {
                    Identifier tagId = new Identifier(entry.substring(1));
                    tagSet.add(TagKey.of(Registries.ITEM.getKey(), tagId));
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[ItemTagManager] Invalid item tag identifier in '{}' config list: '{}'", listName, entry);
                }
            } else {
                try {
                    Identifier itemId = new Identifier(entry);
                    Registries.ITEM.getOrEmpty(itemId).ifPresent(itemSet::add);
                } catch (Exception e) {
                    AdorableHamsterPets.LOGGER.warn("[ItemTagManager] Invalid item identifier in '{}' config list: '{}'", listName, entry);
                }
            }
        }
    }

    private static boolean matches(ItemStack stack, Set<Item> itemSet, Set<TagKey<Item>> tagSet) {
        if (stack.isEmpty()) return false;
        if (itemSet.contains(stack.getItem())) return true;
        for (TagKey<Item> tag : tagSet) {
            if (stack.isIn(tag)) return true;
        }
        return false;
    }

    private static void clearAll() {
        tamingItems.clear();
        tamingTags.clear();
        standardFoodItems.clear();
        standardFoodTags.clear();
        stealableItems.clear();
        stealableTags.clear();
        buffFoodItems.clear();
        buffFoodTags.clear();
        shoulderMountItems.clear();
        shoulderMountTags.clear();
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
    }
}