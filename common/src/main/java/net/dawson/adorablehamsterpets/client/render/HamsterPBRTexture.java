package net.dawson.adorablehamsterpets.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

/**
 * A custom texture wrapper that holds references to its generated Normal and Specular maps.
 * This class allows external shader mods (like Iris) to safely extract the PBR data
 * from in-memory generated textures without relying on file discovery.
 */
public class HamsterPBRTexture extends DynamicTexture {

    private final DynamicTexture normalTexture;
    private final DynamicTexture specularTexture;

    public HamsterPBRTexture(NativeImage image, DynamicTexture normalTexture, DynamicTexture specularTexture) {
        super(() -> "adorablehamsterpets_pbr", image); // 26.2: textures carry a debug label
        this.normalTexture = normalTexture;
        this.specularTexture = specularTexture;
    }

    public DynamicTexture getNormalTexture() {
        return this.normalTexture;
    }

    public DynamicTexture getSpecularTexture() {
        return this.specularTexture;
    }
}