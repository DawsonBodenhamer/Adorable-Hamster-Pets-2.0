package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.entity.custom.genetics.PaletteDefinition;
import net.dawson.adorablehamsterpets.entity.custom.genetics.TextureType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for dynamically generating hamster textures via palette swapping.
 */
public class HamsterTextureUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Map<String, Identifier> CACHED_TEXTURES = new ConcurrentHashMap<>();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Generates or retrieves a dynamic texture using a registered palette.
     * Supports both programmatic color replacement and static image alpha masking.
     */
    public static Identifier getOrCreateDynamicTexture(String textureName, String paletteId) {
        String cacheKey = textureName + "_" + paletteId;
        Identifier cachedId = CACHED_TEXTURES.get(cacheKey);
        if (cachedId != null) {
            return cachedId;
        }

        // Fetch palette from manager
        PaletteDefinition palette = HamsterPaletteManager.PALETTE_REGISTRY.get(paletteId);
        Identifier baseTextureId = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/" + textureName + ".png");

        if (palette == null) {
            return baseTextureId;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Identifier dynamicId = Identifier.of(AdorableHamsterPets.MOD_ID, "dynamic_" + cacheKey);

        try {
            Resource maskResource = client.getResourceManager().getResource(baseTextureId).orElseThrow();
            try (InputStream maskStream = maskResource.getInputStream()) {
                NativeImage maskImage = NativeImage.read(maskStream);

                if (palette.type() == TextureType.PROGRAMMATIC && palette.hexCodes() != null) {
                    // --- Programmatic Palette Swapping ---
                    int[] hexCodes = palette.hexCodes();

                    // Iterate through mask pixels and map brightness to palette hex codes
                    for (int y = 0; y < maskImage.getHeight(); y++) {
                        for (int x = 0; x < maskImage.getWidth(); x++) {
                            int color = maskImage.getColor(x, y);
                            int a = ColorHelper.Abgr.getAlpha(color);
                            if (a == 0) continue; // Skip transparent pixels

                            int r = ColorHelper.Abgr.getRed(color);
                            float brightness = r / 255.0f;
                            int newHexRgb;

                            // Map grayscale brightness to specific hex codes
                            if (brightness >= 0.89f) newHexRgb = hexCodes[0]; // B1 (100%)
                            else if (brightness >= 0.72f) newHexRgb = hexCodes[1]; // B2 (78%)
                            else if (brightness >= 0.61f) newHexRgb = hexCodes[2]; // B3 (67%)
                            else if (brightness >= 0.49f) newHexRgb = hexCodes[3]; // B4 (55%)
                            else if (brightness >= 0.38f) newHexRgb = hexCodes[4]; // B5 (44%)
                            else if (brightness >= 0.27f) newHexRgb = hexCodes[5]; // B6 (33%)
                            else if (brightness >= 0.16f) newHexRgb = hexCodes[6]; // B7 (22%)
                            else newHexRgb = hexCodes[7]; // B8 (11%)

                            maskImage.setColor(x, y, applyHexToAbgr(a, newHexRgb));
                        }
                    }
                } else if (palette.type() == TextureType.STATIC) {
                    // --- Static Image Masking ---
                    Identifier sourceTextureId = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/" + palette.author() + "/" + palette.id() + ".png");
                    Resource sourceResource = client.getResourceManager().getResource(sourceTextureId).orElseThrow();

                    try (InputStream sourceStream = sourceResource.getInputStream()) {
                        NativeImage sourceImage = NativeImage.read(sourceStream);

                        // Ensure dimensions match to prevent out-of-bounds exceptions
                        int width = Math.min(maskImage.getWidth(), sourceImage.getWidth());
                        int height = Math.min(maskImage.getHeight(), sourceImage.getHeight());

                        // Iterate through pixels and composite source color with mask alpha
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                int maskColor = maskImage.getColor(x, y);
                                int maskAlpha = ColorHelper.Abgr.getAlpha(maskColor);

                                if (maskAlpha == 0) continue;

                                int sourceColor = sourceImage.getColor(x, y);

                                // Preserve source RGB but overwrite its alpha with the mask's alpha
                                int newColor = (sourceColor & 0x00FFFFFF) | (maskAlpha << 24);
                                maskImage.setColor(x, y, newColor);
                            }
                        }
                        // Free native memory for the source image
                        sourceImage.close();
                    }
                } else {
                    maskImage.close();
                    return baseTextureId;
                }

                // Register composited image as a new dynamic texture
                NativeImageBackedTexture dynamicTex = new NativeImageBackedTexture(maskImage);
                client.getTextureManager().registerTexture(dynamicId, dynamicTex);
                CACHED_TEXTURES.put(cacheKey, dynamicId);
                return dynamicId;
            }
        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.error("Failed to generate dynamic texture for " + cacheKey, e);
            return baseTextureId;
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Converts a standard RGB hex integer into Minecraft's required ABGR format.
     */
    private static int applyHexToAbgr(int alpha, int hexRgb) {
        int r = (hexRgb >> 16) & 0xFF;
        int g = (hexRgb >> 8) & 0xFF;
        int b = hexRgb & 0xFF;
        return ColorHelper.Abgr.getAbgr(alpha, b, g, r);
    }
}