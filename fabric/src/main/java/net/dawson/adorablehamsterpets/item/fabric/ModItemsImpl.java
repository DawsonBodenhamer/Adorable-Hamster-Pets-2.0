package net.dawson.adorablehamsterpets.item.fabric;



/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;


public class ModItemsImpl {
    /**
     * Provides the Fabric implementation for registering the spawn egg.
     * It uses the vanilla SpawnEggItem and can safely call .get() on the EntityType
     * because Fabric's loader order allows it.
     */
    public static RegistrySupplier<Item> registerSpawnEgg() {
        return ModItems.ITEMS.register("hamster_spawn_egg",
                () -> new SpawnEggItem(ModEntities.HAMSTER.get(), 0x9c631f, 0xffffff, new Item.Settings()));
    }
}