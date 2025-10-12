package net.dawson.adorablehamsterpets.block;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

import java.util.function.Supplier;

public class ModBlocks {

    // --- 1. Create a DeferredRegister for Blocks ---
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.BLOCK);

    // --- 2. Change Block fields to RegistrySuppliers ---
    public static final RegistrySupplier<Block> GREEN_BEANS_CROP = registerBlock("green_beans_crop",
            () -> new GreenBeansCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT).sounds(BlockSoundGroup.CROP).nonOpaque().noCollision()));

    public static final RegistrySupplier<Block> CUCUMBER_CROP = registerBlock("cucumber_crop",
            () -> new CucumberCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT).sounds(BlockSoundGroup.CROP).nonOpaque().noCollision()));

    public static final RegistrySupplier<Block> WILD_GREEN_BEAN_BUSH = registerBlock("wild_green_bean_bush",
            () -> new WildGreenBeanBushBlock(AbstractBlock.Settings.copy(Blocks.SWEET_BERRY_BUSH)
                    .nonOpaque()
                    .noCollision()
                    .ticksRandomly()
                    .sounds(BlockSoundGroup.SWEET_BERRY_BUSH)));

    public static final RegistrySupplier<Block> WILD_CUCUMBER_BUSH = registerBlock("wild_cucumber_bush",
            () -> new WildCucumberBushBlock(AbstractBlock.Settings.copy(Blocks.SWEET_BERRY_BUSH)
                    .nonOpaque()
                    .noCollision()
                    .ticksRandomly()
                    .sounds(BlockSoundGroup.SWEET_BERRY_BUSH)));

    public static final RegistrySupplier<Block> SUNFLOWER_BLOCK = registerBlock("sunflower_block",
            () -> new SunflowerBlock(AbstractBlock.Settings.copy(Blocks.SUNFLOWER).nonOpaque()));

    // --- 3. Private Helper Method for Block Registration ---
    private static RegistrySupplier<Block> registerBlock(String name, Supplier<Block> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }

    // --- 4. Main Registration Call ---
    public static void register() {
        BLOCKS.register();
    }
}