package net.dawson.adorablehamsterpets.tag;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Centralizes all custom "union" block tags for the mod.
 * Aggregates vanilla, 'c', and 'forge' tags for maximum compatibility.
 */
public class ModBlockTags {

    public static final TagKey<Block> CROPS = of("crops");
    public static final TagKey<Block> BUSHES = of("bushes");

    private static TagKey<Block> of(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, path));
    }
}