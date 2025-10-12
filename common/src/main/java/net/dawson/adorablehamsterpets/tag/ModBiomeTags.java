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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Centralizes all custom "union" biome tags for the mod.
 * Each tag aggregates vanilla, 'c', and 'forge' tags for maximum cross-loader compatibility.
 */
public class ModBiomeTags {

    public static final TagKey<Biome> IS_ICY = of("is_icy");
    public static final TagKey<Biome> IS_MUSHROOM = of("is_mushroom");
    public static final TagKey<Biome> IS_MAGICAL = of("is_magical");
    public static final TagKey<Biome> IS_COLD = of("is_cold");
    public static final TagKey<Biome> IS_SNOWY = of("is_snowy");
    public static final TagKey<Biome> IS_MOUNTAIN = of("is_mountain");
    public static final TagKey<Biome> IS_SPARSE_VEGETATION = of("is_sparse_vegetation");
    public static final TagKey<Biome> IS_WET = of("is_wet");
    public static final TagKey<Biome> IS_CAVE = of("is_cave");
    public static final TagKey<Biome> IS_SANDY = of("is_sandy");
    public static final TagKey<Biome> IS_FOREST = of("is_forest");
    public static final TagKey<Biome> IS_DENSE_VEGETATION = of("is_dense_vegetation");

    private static TagKey<Biome> of(String path) {
        return TagKey.of(RegistryKeys.BIOME, Identifier.of(AdorableHamsterPets.MOD_ID, path));
    }
}