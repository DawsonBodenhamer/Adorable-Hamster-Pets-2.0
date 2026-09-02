package net.dawson.adorablehamsterpets.entity.custom.genetics;

import net.minecraft.world.phys.Vec3;

/**
 * Defines a single hamster color palette, storing its physical texture data (if programmatic)
 * and its genetic mathematical data (for both programmatic and static textures).
 */
public record PaletteDefinition(
        String id,
        String author,
        TextureType type,
        int[] hexCodes,
        Vec3 colorSpacePos,
        float diluteness,
        HamsterColorZone zone
) {}