package net.dawson.adorablehamsterpets.item;



/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;


public class ModItemGroups {


    public static final DeferredRegister<ItemGroup> ITEM_GROUPS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ITEM_GROUP);


    public static final RegistrySupplier<ItemGroup> ADORABLE_HAMSTER_PETS_GROUP = ITEM_GROUPS.register(
            "adorable_hamster_pets",
            () -> CreativeTabRegistry.create(builder -> builder
                    .displayName(Text.translatable("itemgroup.adorablehamsterpets.main"))
                    .icon(() -> new ItemStack(ModItems.HAMSTER_SPAWN_EGG.get()))
                    .entries((featureSet, output) -> {
                        // This is where we add all the items to the tab's contents.
                        output.add(ModItems.CHEESE.get());
                        output.add(ModItems.HAMSTER_FOOD_MIX.get());
                        output.add(ModItems.CUCUMBER.get());
                        output.add(ModItems.CUCUMBER_SEEDS.get());
                        output.add(ModItems.SLICED_CUCUMBER.get());
                        output.add(ModItems.GREEN_BEANS.get());
                        output.add(ModItems.GREEN_BEAN_SEEDS.get());
                        output.add(ModItems.STEAMED_GREEN_BEANS.get());
                        output.add(ModItems.SUNFLOWER_SEEDS.get());
                        output.add(ModItems.HAMSTER_SPAWN_EGG.get());
                        output.add(ModItems.SUNFLOWER_BLOCK_ITEM.get());
                        output.add(ModItems.WILD_GREEN_BEAN_BUSH_ITEM.get());
                        output.add(ModItems.WILD_CUCUMBER_BUSH_ITEM.get());
                    }))
    );


    public static void register() {
        ITEM_GROUPS.register();
    }
}