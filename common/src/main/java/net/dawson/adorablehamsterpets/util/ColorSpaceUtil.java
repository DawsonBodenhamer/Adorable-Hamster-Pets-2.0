package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterColorZone;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * A mathematical utility for mapping and comparing colors within a 3D Cartesian space.
 * Converts Hue, Saturation and Brightness values into a cylindrical coordinate system (X, Y, Z)
 * to allow for accurate distance calculations and genetic blending between distinct texture palettes.
 */
public final class ColorSpaceUtil {

    /**
     * Tuning constants for dynamic hue and saturation adjustments.
     */
    public static final class ColorTuning {
        public static final float WARM_NEUTRAL_HUE = 30.0f / 360.0f;     // Warm gold tone
        public static final float COOL_NEUTRAL_HUE = 210.0f / 360.0f;    // Cool cyan tone
        public static final float SAT_BOOST_MIN_B = 0.2f;                // Saturation boost multiplier for darkest colors
        public static final float SAT_BOOST_MAX_B = 0.06f;               // Saturation boost multiplier for brightest colors
        public static final float SAT_BOOST_MIN_S = 1.4f;                // Saturation boost multiplier for least saturated colors
        public static final float SAT_BOOST_MAX_S = 0.3f;                // Saturation boost multiplier for most saturated colors
        public static final float HUE_SHIFT_MAX_B = 13.0f;               // Hue shift degrees at 0.0 brightness
        public static final float HUE_SHIFT_MIN_B = 2.0f;                // Hue shift degrees at 1.0 brightness
        public static final float HUE_SHIFT_MAX_S = 11.0f;               // Hue shift degrees at 0.0 saturation
        public static final float HUE_SHIFT_MIN_S = 1.2f;                // Hue shift degrees at 1.0 saturation
        public static final float NEUTRAL_THRESHOLD = 0.05f;             // Below this is considered "neutral"
        public static final float DAMPEN_SAT_F0R_HUE = 276.0f / 360.0f;  // Lavender needs less color boost
        public static final float DAMPENING_MULTIPLIER = 0.6f;           // Reduce boost

        public static float getShiftDegrees(float brightness, float saturation, boolean isWarm) {
            // Sliding scale for brightness
            float shiftB = MathHelper.lerp(brightness, HUE_SHIFT_MAX_B, HUE_SHIFT_MIN_B);
            // Sliding scale for saturation
            float shiftS = MathHelper.lerp(saturation, HUE_SHIFT_MAX_S, HUE_SHIFT_MIN_S);

            // Average the two shifts
            float finalShift = (shiftB + shiftS) / 2.0f;

            return isWarm ? -(finalShift / 360.0f) : (finalShift / 360.0f);
        }
    }

    private ColorSpaceUtil() {}

    /**
     * A record representing a color's position in 3D space and its genetic diluteness.
     */
    public record ColorData(Vec3d position, float dilutenessScore) {}

    /**
     * Analyzes an array of Hex color codes and returns its averaged 3D spatial coordinate and diluteness score.
     */
    public static ColorData analyzePalette(int[] paletteHexCodes) {
        if (paletteHexCodes == null || paletteHexCodes.length == 0) {
            return new ColorData(Vec3d.ZERO, 0.0f);
        }

        double sumX = 0, sumY = 0, sumZ = 0;
        double totalDiluteness = 0.0;

        for (int hex : paletteHexCodes) {
            // Extract RGB
            int r = (hex >> 16) & 0xFF;
            int g = (hex >> 8) & 0xFF;
            int b = hex & 0xFF;

            // Convert to HSB
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            double angleRadians = hsb[0] * Math.PI * 2.0;

            sumX += hsb[1] * Math.cos(angleRadians);
            sumY += hsb[1] * Math.sin(angleRadians);
            sumZ += hsb[2];
            totalDiluteness += hsb[2] * (1.0f - hsb[1]);
        }

        int count = paletteHexCodes.length;
        return new ColorData(
                new Vec3d(sumX / count, sumY / count, sumZ / count),
                (float) (totalDiluteness / count)
        );
    }

    /**
     * Reads a PNG file directly from the mod JAR.
     * Averages the HSB of all fully opaque pixels to generate genetic data.
     */
    public static ColorData analyzeImage(String resourcePath) {
        // Read directly from classpath to bypass Minecraft's client-only texture manager
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }

        try (InputStream stream = ColorSpaceUtil.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                AdorableHamsterPets.LOGGER.error("Could not find image for genetic analysis: {}", resourcePath);
                return new ColorData(Vec3d.ZERO, 0.0f);
            }

            BufferedImage image = ImageIO.read(stream);
            double sumX = 0, sumY = 0, sumZ = 0;
            double totalDiluteness = 0.0;
            int validPixels = 0;

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >> 24) & 0xFF;

                    // Skip transparent or semi-transparent pixels
                    // (mostly for anti-aliased edges; probably not 100% necessary for 6-pixel Minecraft textures)
                    if (alpha < 255) continue;

                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    float[] hsb = Color.RGBtoHSB(r, g, b, null);
                    double angleRadians = hsb[0] * Math.PI * 2.0;

                    sumX += hsb[1] * Math.cos(angleRadians);
                    sumY += hsb[1] * Math.sin(angleRadians);
                    sumZ += hsb[2];
                    totalDiluteness += hsb[2] * (1.0f - hsb[1]);
                    validPixels++;
                }
            }

            if (validPixels == 0) return new ColorData(Vec3d.ZERO, 0.0f);

            return new ColorData(
                    new Vec3d(sumX / validPixels, sumY / validPixels, sumZ / validPixels),
                    (float) (totalDiluteness / validPixels)
            );

        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.error("Failed to analyze image for genetics: {}", resourcePath, e);
            return new ColorData(Vec3d.ZERO, 0.0f);
        }
    }

    /**
     * Shifts the hue of an array of hex RGB colors.
     * Intelligently detects grayscale palettes to "hop" the hue instead of nudging it.
     * Dynamically boosts saturation based on the original color's brightness and saturation.
     */
    public static int[] applyHueShiftToPalette(int[] originalHexCodes, int shiftMode) {
        if (shiftMode == 0) return originalHexCodes.clone();

        boolean isWarm = shiftMode < 0;
        int[] shifted = new int[originalHexCodes.length];

        // --- Calculate Average Saturation ---
        double totalSat = 0;
        for (int hex : originalHexCodes) {
            totalSat += Color.RGBtoHSB((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF, null)[1];
        }
        boolean isNeutral = (totalSat / originalHexCodes.length) < ColorTuning.NEUTRAL_THRESHOLD;

        // --- Apply Transformations ---
        for (int i = 0; i < originalHexCodes.length; i++) {
            int hexRgb = originalHexCodes[i];
            float[] hsb = Color.RGBtoHSB((hexRgb >> 16) & 0xFF, (hexRgb >> 8) & 0xFF, hexRgb & 0xFF, null);

            // --- Apply Hue Shift ---
            if (isNeutral) {
                // "Hop" the hue to new location for grays
                hsb[0] = isWarm ? ColorTuning.WARM_NEUTRAL_HUE : ColorTuning.COOL_NEUTRAL_HUE;
            } else {
                // Shift the hue for other colors
                hsb[0] = (hsb[0] + ColorTuning.getShiftDegrees(hsb[2], hsb[1], isWarm)) % 1.0f;
                if (hsb[0] < 0) hsb[0] += 1.0f;
            }

            // --- Apply Dynamic Saturation Boost ---
            // 1. Calculate base boost based on brightness
            float brightnessBoost = MathHelper.lerp(
                    hsb[2],
                    ColorTuning.SAT_BOOST_MIN_B,
                    ColorTuning.SAT_BOOST_MAX_B
            );

            // 2. Calculate multiplier based on the original saturation
            float saturationMultiplier = MathHelper.lerp(
                    hsb[1],
                    ColorTuning.SAT_BOOST_MIN_S,
                    ColorTuning.SAT_BOOST_MAX_S
            );

            // 3. Combine for final calculated boost
            float saturationBoost = brightnessBoost * saturationMultiplier;

            // Dampen saturation boost for hues close to Lavender
            float hueDistance = Math.abs(hsb[0] - ColorTuning.DAMPEN_SAT_F0R_HUE);
            if (hueDistance > 0.5f) hueDistance = 1.0f - hueDistance;

            float normalizedDistance = hueDistance * 2.0f;

            float hueMultiplier = net.minecraft.util.math.MathHelper.lerp(
                    normalizedDistance,
                    ColorTuning.DAMPENING_MULTIPLIER,
                    1.0f
            );

            hsb[1] = Math.min(1.0f, hsb[1] + (saturationBoost * hueMultiplier));

            shifted[i] = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xFFFFFF;
        }
        return shifted;
    }

    /**
     * Calculates a straight line between two colors in a 3D Cartesian space.
     * Represents a "probabilistic slider" for the offspring of two parents.
     * Adds a randomized jitter to "thicken" the line, expanding the pool of potential genetic results.
     */
    public static Vec3d calculateGeneticMidpoint(Vec3d parentA, Vec3d parentB, Random random) {
        double variance = AdorableHamsterPets.CONFIG.geneticVariance.get();
        double mutationRate = AdorableHamsterPets.CONFIG.geneticMutationRate.get();

        // Gaussian distribution centered at 0.5 (midpoint) with slight configurable deviation
        double t = 0.5 + (random.nextGaussian() * variance);

        // Clamp to [0, 1] to prevent moving past parental boundaries
        t = Math.max(0.0, Math.min(1.0, t));

        // Lerp along the 3D line segment
        Vec3d mid = parentA.lerp(parentB, t);

        // Add random scatter offset on all axes
        double jx = (random.nextDouble() - 0.5) * mutationRate;
        double jy = (random.nextDouble() - 0.5) * mutationRate;
        double jz = (random.nextDouble() - 0.5) * mutationRate;

        return mid.add(jx, jy, jz);
    }

    /**
     * Determines the closest abstract HamsterColorZone for a given 3D color coordinate.
     * Uses a rule-based HSB categorization to prioritize Hue.
     */
    public static HamsterColorZone determineZone(Vec3d colorSpacePos) {
        HamsterColorZone closestZone = HamsterColorZone.ORANGE;
        double minDistance = Double.MAX_VALUE;

        // Iterate all zones to find the mathematically closest center
        for (HamsterColorZone zone : HamsterColorZone.values()) {
            double distance = colorSpacePos.distanceTo(zone.getIdealCenter());
            if (distance < minDistance) {
                minDistance = distance;
                closestZone = zone;
            }
        }

        return closestZone;
    }

    /**
     * Calculates the Euclidean distance between two colors in a 3D Cartesian space.
     * The smaller the distance, the closer the colors are visually.
     */
    public static double getColorDistance(Vec3d colorA, Vec3d colorB) {
        return colorA.distanceTo(colorB);
    }

    /**
     * Extracts the average saturation from a 3D color space coordinate.
     */
    public static double getSaturation(Vec3d colorSpacePos) {
        return Math.sqrt(colorSpacePos.x * colorSpacePos.x + colorSpacePos.y * colorSpacePos.y);
    }

    /**
     * Extracts the average brightness from a 3D color space coordinate.
     */
    public static double getBrightness(Vec3d colorSpacePos) {
        return colorSpacePos.z;
    }
}