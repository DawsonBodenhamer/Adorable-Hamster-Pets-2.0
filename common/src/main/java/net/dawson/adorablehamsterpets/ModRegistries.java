package net.dawson.adorablehamsterpets;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.ComposterBlock;

/**
 * Handles miscellaneous registrations that need to occur at specific lifecycle events.
 */
public class ModRegistries {

    /**
     * Registers items with the vanilla composter.
     * This is called directly during the common setup phase.
     */
    public static void registerCompostables() {
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEANS.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEAN_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.SUNFLOWER_SEEDS.get(), 0.25f);
    }
}