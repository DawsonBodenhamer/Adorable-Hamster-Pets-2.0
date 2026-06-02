package net.dawson.adorablehamsterpets.util;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.render.HamsterPBRTexture;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.entity.custom.genetics.PaletteDefinition;
import net.dawson.adorablehamsterpets.entity.custom.genetics.TextureType;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final Map<String, int[]> TRIM_PALETTE_CACHE = new ConcurrentHashMap<>();

    // --- LabPBR Constants ---
    private static final int HAMSTER_MEDIUM_SSS = 100; // 65 = 0%, 255 = 100%
    private static final int HAMSTER_LOW_SSS = 80;
    public static final float HAMSTER_MAX_POM_DEPTH = 0.08f; // 8% max displacement

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Dumps the generated NativeImages to disk before they are registered.
     */
    public static void dumpAllCachedTextures(PlayerEntity player) {
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        int count = 0;

        for (Map.Entry<String, Identifier> entry : CACHED_TEXTURES.entrySet()) {
            String cacheKey = entry.getKey();
            Identifier id = entry.getValue();
            AbstractTexture texture = tm.getOrDefault(id, null);

            if (texture instanceof HamsterPBRTexture pbrTexture) {
                NativeImage base = pbrTexture.getImage();
                NativeImage normal = pbrTexture.getNormalTexture().getImage();
                NativeImage specular = pbrTexture.getSpecularTexture().getImage();

                if (base != null && normal != null && specular != null) {
                    dumpDebugTextures(cacheKey, base, specular, normal);
                    count++;
                }
            }
        }

        // Output clickable path to player chat
        if (player != null) {
            Path debugDir = Platform.getGameFolder().resolve("ahp_printed_textures");
            player.sendMessage(Text.literal("Dumped " + count + " hamster textures to: " + debugDir.toAbsolutePath()).formatted(Formatting.WHITE), false);
        }
    }

    /**
     * Clears in-memory dynamic textures and explicitly destroys their OpenGL bindings
     * to prevent VRAM memory leaks during resource reloads or rapid config changes.
     */
    public static void clearCaches() {
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        for (Identifier id : CACHED_TEXTURES.values()) {
            tm.destroyTexture(id);
            tm.destroyTexture(Identifier.of(id.getNamespace(), id.getPath() + "_s"));
            tm.destroyTexture(Identifier.of(id.getNamespace(), id.getPath() + "_n"));
        }
        CACHED_TEXTURES.clear();
        TRIM_PALETTE_CACHE.clear();
    }

    /**
     * Generates or retrieves a dynamic, composite texture for a specific hamster.
     * This combines the base coat, overlays, skin, eyes, accessories, and armor
     * into a single image to reduce render layers and draw calls.
     * Supports both programmatic color replacement and static image alpha masking.
     */
    public static Identifier getHamsterTexture(HamsterEntity hamster) {
        if (Configs.AHP.performanceMode) {
            return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/performance_mode.png");
        }

        HamsterGenome genome = hamster.getGenome();
        boolean redEyes = genome.eyeGenotype() == 2 && Configs.AHP.enableRedEyes;
        boolean isSweetPotato = hamster.isSweetPotato();
        boolean isHamtaro = hamster.isHamtaro();

        // --- Extract Equipment States ---
        ItemStack armorStack = hamster.getArmorStack();
        String armorMaterial = "none";
        Identifier armorTextureId = null;

        if (Configs.AHP.enableArmorVisuals && !armorStack.isEmpty() && armorStack.getItem() instanceof HamsterArmorItem armorItem) {
            armorMaterial = armorItem.getMaterial().getName();
            armorTextureId = armorItem.getEntityTexture();
        }

        ArmorTrim trim = armorStack.isEmpty() ? null : armorStack.get(DataComponentTypes.TRIM);
        String trimPattern = trim != null ? trim.getPattern().getKey().map(key -> key.getValue().getPath()).orElse("none") : "none";
        String trimMaterialAsset = trim != null ? trim.getMaterial().value().assetName() : "none";

        boolean hasAcornHat = false;
        ItemStack accessoryStack = hamster.getAccessoryStack();

        if (accessoryStack.isOf(ModItems.ACORN_HAT.get())) {
            hasAcornHat = true;
        } else if (!armorStack.isEmpty() && armorStack.isOf(ModItems.HAMSTER_ARMOR_ACORN.get()) && Configs.AHP.renderAcornHat.get()) {
            hasAcornHat = true;
        }

        boolean hasPinkPetals = hamster.getDataTracker().get(HamsterEntity.PINK_PETAL_TYPE) > 0;

        // --- Generate Cache Key ---
        String cacheKey = String.format("comp_%s_w%d%s_b%d%s_e%b_a%s_tp%s_tm%s_h%b_p%b_sp%b_hm%b_pbr%b_em%b",
                genome.basePaletteId(),
                genome.wildOverlayPattern(),
                genome.wildOverlayPaletteId() != null ? genome.wildOverlayPaletteId() : "",
                genome.breedingOverlayPattern(),
                genome.breedingOverlayPaletteId() != null ? genome.breedingOverlayPaletteId() : "",
                redEyes,
                armorMaterial,
                trimPattern,
                trimMaterialAsset,
                hasAcornHat,
                hasPinkPetals,
                isSweetPotato,
                isHamtaro,
                Configs.AHP.enableArmorPbr.get(),
                Configs.AHP.emissiveArmorTrims.get());

        Identifier cachedId = CACHED_TEXTURES.get(cacheKey);
        if (cachedId != null) {
            return cachedId;
        }

        Identifier dynamicId = Identifier.of(AdorableHamsterPets.MOD_ID, cacheKey);

        try {
            // --- 1. Base Coat ---
            NativeImage composite;
            if (isSweetPotato) {
                composite = readRawImage("textures/entity/hamster/easter_egg/sweet_potato.png");
            } else if (isHamtaro) {
                composite = readRawImage("textures/entity/hamster/easter_egg/hamtaro.png");
            } else {
                composite = createLayerImage("fur_base_pattern/fur_pattern.png", genome.basePaletteId());
            }

            if (composite == null) {
                return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png"); // Ultimate fallback
            }

            // --- PBR Setup ---
            NativeImage specularImg = new NativeImage(composite.getWidth(), composite.getHeight(), false);
            NativeImage normalImg = new NativeImage(composite.getWidth(), composite.getHeight(), false);

            // Normal Texture ↓
            // Flat Normals → R:127, G:127
            // 0% Ambient Occlusion → B:255
            // 0% Displacement → A:255
            int defaultNormal = ColorHelper.Abgr.getAbgr(255, 255, 127, 127);

            // Specular Texture (Fur) ↓
            // Matte → R:0
            // No Reflectance → G:0
            // Low SSS → B:80
            // No Emissiveness → A:255
            int furSpecular = ColorHelper.Abgr.getAbgr(255, HAMSTER_LOW_SSS, 0, 0);

            // Fill base PBR values for opaque pixels on base coat,
            // clear transparent pixels to prevent memory garbage
            for (int y = 0; y < composite.getHeight(); y++) {
                for (int x = 0; x < composite.getWidth(); x++) {
                    if (ColorHelper.Abgr.getAlpha(composite.getColor(x, y)) > 0) {
                        specularImg.setColor(x, y, furSpecular);
                        normalImg.setColor(x, y, defaultNormal);
                    } else {
                        specularImg.setColor(x, y, 0x00000000);
                        normalImg.setColor(x, y, 0x00000000);
                    }
                }
            }

            // Apply procedural POM to base coat
            applyProceduralPom(composite, normalImg);

            // Prevent overlays if Sweet Potato or Hamtaro
            if (!isSweetPotato && !isHamtaro) {
                // --- 2. Wild Overlay ---
                if (genome.wildOverlayPattern() > 0 && genome.wildOverlayPaletteId() != null) {
                    String patternName = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.get(genome.wildOverlayPattern());
                    NativeImage wildLayer = createLayerImage("overlays/fur_overlay_pattern/" + patternName + ".png", genome.wildOverlayPaletteId());
                    if (wildLayer != null) {
                        blendLayer(composite, specularImg, normalImg, wildLayer, furSpecular);
                        applyProceduralPom(wildLayer, normalImg);
                        wildLayer.close();
                    }
                }

                // --- 3. Breeding Overlay ---
                if (genome.breedingOverlayPattern() > 0 && genome.breedingOverlayPaletteId() != null) {
                    String patternName = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.get(genome.breedingOverlayPattern());
                    NativeImage breedLayer = createLayerImage("overlays/fur_overlay_pattern/" + patternName + ".png", genome.breedingOverlayPaletteId());
                    if (breedLayer != null) {
                        blendLayer(composite, specularImg, normalImg, breedLayer, furSpecular);
                        applyProceduralPom(breedLayer, normalImg);
                        breedLayer.close();
                    }
                }
            }

            // --- 4. Skin Layer ---
            // Medium Glossy → R:120
            // No Reflectance → G:0
            // Medium SSS → B:100
            // No Emissiveness → A:255
            int skinSpecular = ColorHelper.Abgr.getAbgr(255, HAMSTER_MEDIUM_SSS, 0, 120);
            NativeImage skinLayer = readRawImage("textures/entity/hamster/overlays/skin/skin.png");
            if (skinLayer != null) {
                blendLayer(composite, specularImg, normalImg, skinLayer, skinSpecular);
                skinLayer.close();
            }

            // --- 5. Eye Layer ---
            // Matte → R:0
            // No Reflectance → G:0
            // No SSS → B:0
            // No Emissiveness → A:255
            int eyeSpecular = ColorHelper.Abgr.getAbgr(255, 0, 0, 0);
            String eyeTexture = redEyes ? "textures/entity/hamster/overlays/eyes/red_eyes.png" : "textures/entity/hamster/overlays/eyes/black_eyes.png";
            NativeImage eyeLayer = readRawImage(eyeTexture);
            if (eyeLayer != null) {
                blendLayer(composite, specularImg, normalImg, eyeLayer, eyeSpecular);
                eyeLayer.close();
            }

            // --- 6. Armor Layer ---
            if (armorTextureId != null) {
                int armorSpecular = Configs.AHP.enableArmorPbr.get() ? getArmorSpecular(armorMaterial) : ColorHelper.Abgr.getAbgr(255, 0, 0, 0);
                NativeImage armorLayer = readRawImage(armorTextureId.getPath());
                if (armorLayer != null) {
                    blendLayer(composite, specularImg, normalImg, armorLayer, armorSpecular);
                    if (Configs.AHP.enableArmorPbr.get()) {
                        applyProceduralPom(armorLayer, normalImg);
                    }
                    armorLayer.close();
                }

                // --- 7. Armor Trim Layer ---
                if (!trimPattern.equals("none") && !trimMaterialAsset.equals("none") && Configs.AHP.enableArmorVisuals) {
                    NativeImage trimLayer = createTrimLayerImage(trimPattern, trimMaterialAsset);
                    if (trimLayer != null) {
                        int emissiveValue = Configs.AHP.emissiveArmorTrims.get() ? 254 : 255;

                        int trimSpecular;
                        if (Configs.AHP.enableArmorPbr.get()) {
                            // High Glossy → R:220
                            // Medium Reflectance → G:50
                            // No SSS → B:0
                            trimSpecular = ColorHelper.Abgr.getAbgr(emissiveValue, 0, 50, 220);
                        } else {
                            // Keep emissive value but drop physical PBR traits
                            trimSpecular = ColorHelper.Abgr.getAbgr(emissiveValue, 0, 0, 0);
                        }

                        blendLayer(composite, specularImg, normalImg, trimLayer, trimSpecular);
                        if (Configs.AHP.enableArmorPbr.get()) {
                            applyProceduralPom(trimLayer, normalImg);
                        }
                        trimLayer.close();
                    }
                }
            }

            // --- Accessories ---
            // Medium Matte → R:50
            // No Reflectance → G:0
            // Medium SSS → B:100
            // No Emissiveness → A:255
            int accessorySpecular = ColorHelper.Abgr.getAbgr(255, HAMSTER_LOW_SSS, 0, 50);

            // --- 8. Acorn Hat Layer ---
            if (hasAcornHat) {
                NativeImage hatLayer = readRawImage("textures/entity/hamster/armor/acorn_hat.png");
                if (hatLayer != null) {
                    blendLayer(composite, specularImg, normalImg, hatLayer, accessorySpecular);
                    applyProceduralPom(hatLayer, normalImg);
                    hatLayer.close();
                }
            }

            // --- 9. Pink Petals Layer ---
            if (hasPinkPetals) {
                NativeImage petalLayer = readRawImage("textures/entity/hamster/overlays/accessories/overlay_pink_petal.png");
                if (petalLayer != null) {
                    blendLayer(composite, specularImg, normalImg, petalLayer, accessorySpecular);
                    applyProceduralPom(petalLayer, normalImg);
                    petalLayer.close();
                }
            }

            // --- 10. Register Textures ---
            // Register PBR Maps
            Identifier specularId = Identifier.of(dynamicId.getNamespace(), dynamicId.getPath() + "_s");
            NativeImageBackedTexture specularTexture = new NativeImageBackedTexture(specularImg);
            MinecraftClient.getInstance().getTextureManager().registerTexture(specularId, specularTexture);

            Identifier normalId = Identifier.of(dynamicId.getNamespace(), dynamicId.getPath() + "_n");
            NativeImageBackedTexture normalTexture = new NativeImageBackedTexture(normalImg);
            MinecraftClient.getInstance().getTextureManager().registerTexture(normalId, normalTexture);

            // Create custom base texture wrapper and register
            HamsterPBRTexture baseTexture = new HamsterPBRTexture(composite, normalTexture, specularTexture);
            MinecraftClient.getInstance().getTextureManager().registerTexture(dynamicId, baseTexture);
            CACHED_TEXTURES.put(cacheKey, dynamicId);

            return dynamicId;

        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.error("Failed to generate composite texture for " + cacheKey, e);
            return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/fur_base_pattern/fur_pattern.png");
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Calculates and applies Parallax Occlusion Mapping (POM) depth to the Normal Map's Alpha channel.
     * Evaluates brightness specifically for the provided layer, ensuring different layers (e.g., fur vs armor)
     * don't skew each other's displacement scales.
     */
    private static void applyProceduralPom(NativeImage layer, NativeImage normalImg) {
        float minBrightness = 255.0f;
        float maxBrightness = 0.0f;

        // --- 1. Find brightness boundaries ---
        // Evaluate only opaque pixels for this specific layer
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                int color = layer.getColor(x, y);
                if (ColorHelper.Abgr.getAlpha(color) > 127) {
                    int r = ColorHelper.Abgr.getRed(color);
                    int g = ColorHelper.Abgr.getGreen(color);
                    int b = ColorHelper.Abgr.getBlue(color);
                    float brightness = Math.max(r, Math.max(g, b));

                    if (brightness < minBrightness) minBrightness = brightness;
                    if (brightness > maxBrightness) maxBrightness = brightness;
                }
            }
        }

        float brightnessRange = maxBrightness - minBrightness;

        // --- 2. Apply POM depth ---
        // Map depth to the Normal Alpha channel for opaque pixels
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                if (ColorHelper.Abgr.getAlpha(layer.getColor(x, y)) > 127) {
                    int color = layer.getColor(x, y);
                    float brightness = Math.max(ColorHelper.Abgr.getRed(color), Math.max(ColorHelper.Abgr.getGreen(color), ColorHelper.Abgr.getBlue(color)));

                    float normalized = brightnessRange > 0.0f ? (brightness - minBrightness) / brightnessRange : 1.0f;

                    // 1.0 (Brightest) -> 0% depth -> 255 alpha
                    // 0.0 (Darkest) -> HAMSTER_MAX_POM_DEPTH -> scaled alpha
                    float depthFraction = (1.0f - normalized) * (HAMSTER_MAX_POM_DEPTH / 0.25f);
                    int pomAlpha = Math.max(1, 255 - (int)(depthFraction * 254));

                    int normColor = normalImg.getColor(x, y);
                    int newNormColor = (normColor & 0x00FFFFFF) | (pomAlpha << 24);
                    normalImg.setColor(x, y, newNormColor);
                }
            }
        }
    }

    /**
     * Determines the LabPBR specular map value for different armor materials.
     * Uses ABGR format (Alpha=Emissive, Blue=Porosity/SSS, Green=Metal/F0, Red=Smoothness)
     */
    private static int getArmorSpecular(String material) {
        var pbr = switch (material) {
            case "acorn" -> Configs.AHP.acornPbr.get();
            case "iron" -> Configs.AHP.ironPbr.get();
            case "gold" -> Configs.AHP.goldPbr.get();
            case "diamond" -> Configs.AHP.diamondPbr.get();
            case "netherite" -> Configs.AHP.netheritePbr.get();
            default -> null;
        };

        if (pbr != null) {
            return ColorHelper.Abgr.getAbgr(
                    pbr.emissive.get(),
                    pbr.sss.get(),
                    pbr.metallic.get(),
                    pbr.smoothness.get()
            );
        }

        return ColorHelper.Abgr.getAbgr(255, 0, 150, 240); // Fallback
    }

    /**
     * Alpha-blends the top layer onto the base layer and overwrites PBR values for opaque pixels.
     */
    private static void blendLayer(NativeImage composite, NativeImage specularImg, NativeImage normalImg, NativeImage layer, int specularAbgr) {
        int width = Math.min(composite.getWidth(), layer.getWidth());
        int height = Math.min(composite.getHeight(), layer.getHeight());
        int defaultNormal = ColorHelper.Abgr.getAbgr(255, 255, 127, 127);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int topColor = layer.getColor(x, y);
                int topA = ColorHelper.Abgr.getAlpha(topColor);

                if (topA == 0) continue;

                if (topA == 255) {
                    composite.setColor(x, y, topColor);
                    specularImg.setColor(x, y, specularAbgr);
                    normalImg.setColor(x, y, defaultNormal);
                    continue;
                }

                int bottomColor = composite.getColor(x, y);
                int botA = ColorHelper.Abgr.getAlpha(bottomColor);

                if (botA == 0) {
                    composite.setColor(x, y, topColor);
                    specularImg.setColor(x, y, specularAbgr);
                    normalImg.setColor(x, y, defaultNormal);
                    continue;
                }

                float alpha = topA / 255.0f;
                float invAlpha = 1.0f - alpha;

                int r = (int) (ColorHelper.Abgr.getRed(topColor) * alpha + ColorHelper.Abgr.getRed(bottomColor) * invAlpha);
                int g = (int) (ColorHelper.Abgr.getGreen(topColor) * alpha + ColorHelper.Abgr.getGreen(bottomColor) * invAlpha);
                int b = (int) (ColorHelper.Abgr.getBlue(topColor) * alpha + ColorHelper.Abgr.getBlue(bottomColor) * invAlpha);
                int a = Math.max(topA, botA);

                composite.setColor(x, y, ColorHelper.Abgr.getAbgr(a, b, g, r));

                // Hard-overwrite PBR maps if the layer is mostly opaque to avoid messy material blending
                if (topA > 127) {
                    specularImg.setColor(x, y, specularAbgr);
                    normalImg.setColor(x, y, defaultNormal);
                }
            }
        }
    }

    /**
     * Reads a mask and colorizes it according to the assigned palette.
     */
    private static NativeImage createLayerImage(String relativeMaskPath, String paletteId) {
        PaletteDefinition palette = HamsterPaletteManager.PALETTE_REGISTRY.get(paletteId);
        if (palette == null) return null;

        NativeImage maskImage = readRawImage("textures/entity/hamster/" + relativeMaskPath);
        if (maskImage == null) return null;

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
            NativeImage sourceImage = readRawImage("textures/entity/hamster/" + palette.author() + "/" + palette.id() + ".png");
            if (sourceImage != null) {
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
        }
        return maskImage;
    }

    /**
     * Takes Hamster Armor Trim grayscale images and swaps the pixels with vanilla's material palette.
     */
    private static NativeImage createTrimLayerImage(String patternName, String materialAssetName) {
        // 1. Get 8-color base palette and 8-color target material palette
        int[] basePalette = getOrLoadVanillaTrimPalette("trim_palette");
        int[] materialPalette = getOrLoadVanillaTrimPalette(materialAssetName);

        if (basePalette == null || materialPalette == null) {
            return null; // Missing vanilla resource, silently fail
        }

        // 2. Load grayscale pattern mask
        NativeImage trimMask = readRawImage("textures/entity/hamster/armor/trims/hamster_armor_trim_" + patternName + ".png");
        if (trimMask == null) {
            return null; // If player applied trim I don't have a texture for yet
        }

        // 3. Pixel-by-pixel swap
        for (int y = 0; y < trimMask.getHeight(); y++) {
            for (int x = 0; x < trimMask.getWidth(); x++) {
                int pixelColor = trimMask.getColor(x, y);
                int alpha = ColorHelper.Abgr.getAlpha(pixelColor);

                if (alpha == 0) {
                    trimMask.setColor(x, y, 0x00000000); // Force pure transparent black
                    continue; // Skip transparent pixels
                }

                // Strip alpha for exact RGB comparison (NativeImage uses ABGR)
                int rgb = pixelColor & 0x00FFFFFF;

                // Check if this pixel matches any of the 8 vanilla grays
                for (int i = 0; i < 8; i++) {
                    if (rgb == (basePalette[i] & 0x00FFFFFF)) {
                        // Match found, swap RGB from material palette but keep my mask's alpha
                        int newRgb = materialPalette[i] & 0x00FFFFFF;
                        trimMask.setColor(x, y, newRgb | (alpha << 24));
                        break;
                    }
                }
            }
        }

        return trimMask;
    }

    /**
     * Fetches and caches the 8-pixel wide color palette from vanilla Minecraft's files
     */
    private static int[] getOrLoadVanillaTrimPalette(String assetName) {
        return TRIM_PALETTE_CACHE.computeIfAbsent(assetName, key -> {
            Identifier paletteId = Identifier.of("minecraft", "textures/trims/color_palettes/" + key + ".png");

            try {
                var resource = MinecraftClient.getInstance().getResourceManager().getResource(paletteId);
                if (resource.isPresent()) {
                    try (NativeImage image = NativeImage.read(resource.get().getInputStream())) {
                        // Ensure image is at least 8 pixels wide
                        int width = Math.min(8, image.getWidth());
                        int[] colors = new int[width];

                        for (int i = 0; i < width; i++) {
                            colors[i] = image.getColor(i, 0);
                        }
                        return colors;
                    }
                }
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.error("Failed to load vanilla trim palette: {}", paletteId, e);
            }
            return null; // Return null if failed to load to prevent caching an empty array
        });
    }

    /**
     * Reads a raw PNG image directly from the resource manager.
     */
    private static NativeImage readRawImage(String path) {
        Identifier id = Identifier.of(AdorableHamsterPets.MOD_ID, path);
        try {
            var resource = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().getInputStream()) {
                    return NativeImage.read(stream);
                }
            }
        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.error("Failed to read raw image: " + path, e);
        }
        return null;
    }

    /**
     * Converts a standard RGB hex integer into Minecraft's required ABGR format.
     */
    private static int applyHexToAbgr(int alpha, int hexRgb) {
        int r = (hexRgb >> 16) & 0xFF;
        int g = (hexRgb >> 8) & 0xFF;
        int b = hexRgb & 0xFF;
        return ColorHelper.Abgr.getAbgr(alpha, b, g, r);
    }

    private static void dumpDebugTextures(String cacheKey, NativeImage base, NativeImage specular, NativeImage normal) {
        try {
            Path debugDir = Platform.getGameFolder().resolve("ahp_printed_textures");
            Files.createDirectories(debugDir);

            base.writeTo(debugDir.resolve(cacheKey + "_base.png"));
            specular.writeTo(debugDir.resolve(cacheKey + "_s.png"));
            normal.writeTo(debugDir.resolve(cacheKey + "_n.png"));

            AdorableHamsterPets.LOGGER.trace("Dumped debug textures to: {}", debugDir.toAbsolutePath());
        } catch (IOException e) {
            AdorableHamsterPets.LOGGER.error("Failed to dump debug textures", e);
        }
    }
}