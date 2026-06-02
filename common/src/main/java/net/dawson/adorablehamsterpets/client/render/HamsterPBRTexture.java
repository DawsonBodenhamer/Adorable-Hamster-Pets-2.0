package net.dawson.adorablehamsterpets.client.render;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * A custom texture wrapper that holds references to its generated Normal and Specular maps.
 * This class allows external shader mods (like Iris) to safely extract the PBR data
 * from in-memory generated textures without relying on file discovery.
 */
public class HamsterPBRTexture extends NativeImageBackedTexture {

    private final NativeImageBackedTexture normalTexture;
    private final NativeImageBackedTexture specularTexture;

    public HamsterPBRTexture(NativeImage image, NativeImageBackedTexture normalTexture, NativeImageBackedTexture specularTexture) {
        super(image);
        this.normalTexture = normalTexture;
        this.specularTexture = specularTexture;
    }

    public NativeImageBackedTexture getNormalTexture() {
        return this.normalTexture;
    }

    public NativeImageBackedTexture getSpecularTexture() {
        return this.specularTexture;
    }
}