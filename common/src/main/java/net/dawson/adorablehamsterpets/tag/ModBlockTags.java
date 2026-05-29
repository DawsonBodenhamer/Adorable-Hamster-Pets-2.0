package net.dawson.adorablehamsterpets.tag;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Centralizes all custom "union" block tags for the mod.
 * Aggregates vanilla, 'c', and 'forge' tags for maximum compatibility.
 */
public class ModBlockTags {

    public static final TagKey<Block> CROPS = of("crops");

    private static TagKey<Block> of(String path) {
        return TagKey.of(RegistryKeys.BLOCK, Identifier.of(AdorableHamsterPets.MOD_ID, path));
    }
}