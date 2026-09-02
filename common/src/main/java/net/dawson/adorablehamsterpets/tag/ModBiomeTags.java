package net.dawson.adorablehamsterpets.tag;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Centralizes all custom "union" biome tags for the mod.
 * Each tag aggregates vanilla, 'c', and 'forge' tags for maximum compatibility across Minecraft versions.
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
    public static final TagKey<Biome> IS_HOT = of("is_hot");
    public static final TagKey<Biome> IS_TEMPERATE = of("is_temperate");
    public static final TagKey<Biome> IS_DRY = of("is_dry");
    public static final TagKey<Biome> IS_PLAINS = of("is_plains");

    private static TagKey<Biome> of(String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, path));
    }
}