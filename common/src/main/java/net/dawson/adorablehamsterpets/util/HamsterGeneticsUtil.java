package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles stateless variant selection and genetic inheritance calculations for Hamsters.
 */
public final class HamsterGeneticsUtil {

    private HamsterGeneticsUtil() {}

    private static final List<HamsterVariant> ORANGE_VARIANTS = List.of(
            HamsterVariant.ORANGE, HamsterVariant.ORANGE_OVERLAY1, HamsterVariant.ORANGE_OVERLAY2,
            HamsterVariant.ORANGE_OVERLAY3, HamsterVariant.ORANGE_OVERLAY4, HamsterVariant.ORANGE_OVERLAY5,
            HamsterVariant.ORANGE_OVERLAY6, HamsterVariant.ORANGE_OVERLAY7, HamsterVariant.ORANGE_OVERLAY8
    );
    private static final List<HamsterVariant> BLUE_VARIANTS = List.of(
            HamsterVariant.BLUE, HamsterVariant.BLUE_OVERLAY1, HamsterVariant.BLUE_OVERLAY2,
            HamsterVariant.BLUE_OVERLAY3, HamsterVariant.BLUE_OVERLAY4, HamsterVariant.BLUE_OVERLAY5,
            HamsterVariant.BLUE_OVERLAY6, HamsterVariant.BLUE_OVERLAY7, HamsterVariant.BLUE_OVERLAY8
    );
    private static final List<HamsterVariant> CHOCOLATE_VARIANTS = List.of(
            HamsterVariant.CHOCOLATE, HamsterVariant.CHOCOLATE_OVERLAY1, HamsterVariant.CHOCOLATE_OVERLAY2,
            HamsterVariant.CHOCOLATE_OVERLAY3, HamsterVariant.CHOCOLATE_OVERLAY4, HamsterVariant.CHOCOLATE_OVERLAY5,
            HamsterVariant.CHOCOLATE_OVERLAY6, HamsterVariant.CHOCOLATE_OVERLAY7, HamsterVariant.CHOCOLATE_OVERLAY8
    );
    private static final List<HamsterVariant> CREAM_VARIANTS = List.of(
            HamsterVariant.CREAM, HamsterVariant.CREAM_OVERLAY1, HamsterVariant.CREAM_OVERLAY2,
            HamsterVariant.CREAM_OVERLAY3, HamsterVariant.CREAM_OVERLAY4, HamsterVariant.CREAM_OVERLAY5,
            HamsterVariant.CREAM_OVERLAY6, HamsterVariant.CREAM_OVERLAY7, HamsterVariant.CREAM_OVERLAY8
    );
    private static final List<HamsterVariant> DARK_GRAY_VARIANTS = List.of(
            HamsterVariant.DARK_GRAY, HamsterVariant.DARK_GRAY_OVERLAY1, HamsterVariant.DARK_GRAY_OVERLAY2,
            HamsterVariant.DARK_GRAY_OVERLAY3, HamsterVariant.DARK_GRAY_OVERLAY4, HamsterVariant.DARK_GRAY_OVERLAY5,
            HamsterVariant.DARK_GRAY_OVERLAY6, HamsterVariant.DARK_GRAY_OVERLAY7, HamsterVariant.DARK_GRAY_OVERLAY8
    );
    private static final List<HamsterVariant> LAVENDER_VARIANTS = List.of(
            HamsterVariant.LAVENDER, HamsterVariant.LAVENDER_OVERLAY1, HamsterVariant.LAVENDER_OVERLAY2,
            HamsterVariant.LAVENDER_OVERLAY3, HamsterVariant.LAVENDER_OVERLAY4, HamsterVariant.LAVENDER_OVERLAY5,
            HamsterVariant.LAVENDER_OVERLAY6, HamsterVariant.LAVENDER_OVERLAY7, HamsterVariant.LAVENDER_OVERLAY8
    );
    private static final List<HamsterVariant> LIGHT_GRAY_VARIANTS = List.of(
            HamsterVariant.LIGHT_GRAY, HamsterVariant.LIGHT_GRAY_OVERLAY1, HamsterVariant.LIGHT_GRAY_OVERLAY2,
            HamsterVariant.LIGHT_GRAY_OVERLAY3, HamsterVariant.LIGHT_GRAY_OVERLAY4, HamsterVariant.LIGHT_GRAY_OVERLAY5,
            HamsterVariant.LIGHT_GRAY_OVERLAY6, HamsterVariant.LIGHT_GRAY_OVERLAY7, HamsterVariant.LIGHT_GRAY_OVERLAY8
    );

    /**
     * Determines the appropriate HamsterVariant for a given biome, using a prioritized approach.
     * Checks for variants from most specific/rare to most common.
     *
     * @param biomeEntry The RegistryEntry of the biome to check
     * @param random     A Random instance for variant selection
     * @return The chosen HamsterVariant
     */
    public static HamsterVariant determineVariantForBiome(RegistryEntry<Biome> biomeEntry, Random random) {
        String biomeName = biomeEntry.getKey().map(k -> k.getValue().toString()).orElse("unknown");
        AdorableHamsterPets.LOGGER.debug("[AHP Spawn Debug] determineVariantForBiome called for biome: {}", biomeName);

        HamsterVariant result;

        // --- Check from most specific/rare to most common ---
        if (canSpawnBlue(biomeEntry)) {
            // Ice Spikes has a 70% chance for Blue, 30% for White
            result = random.nextInt(10) < 7 ? getRandomVariant(BLUE_VARIANTS, random) : HamsterVariant.WHITE;
        } else if (canSpawnLavender(biomeEntry)) {
            result = getRandomVariant(LAVENDER_VARIANTS, random);
        } else if (canSpawnWhite(biomeEntry)) {
            result = HamsterVariant.WHITE; // White has no overlays
        } else if (canSpawnGray(biomeEntry)) {
            result = random.nextBoolean() ? getRandomVariant(LIGHT_GRAY_VARIANTS, random) : getRandomVariant(DARK_GRAY_VARIANTS, random);
        } else if (canSpawnBlack(biomeEntry)) {
            // Black hamsters should not spawn with overlays in the wild (breaks the camouflage effect)
            result = HamsterVariant.BLACK;
        } else if (canSpawnCream(biomeEntry)) {
            result = getRandomVariant(CREAM_VARIANTS, random);
        } else if (canSpawnChocolate(biomeEntry)) {
            result = getRandomVariant(CHOCOLATE_VARIANTS, random);
        } else {
            // Default Fallback: Orange is the most common, covering Plains, Savanna, etc
            result = getRandomVariant(ORANGE_VARIANTS, random);
        }

        AdorableHamsterPets.LOGGER.debug("[AHP Spawn Debug] Determined variant for {} is {}", biomeName, result.name());
        return result;
    }

    /**
     * Calculates the variant ID for a newborn hamster based on inheritance rules.
     *
     * @param parentA The first parent
     * @param parentB The second parent
     * @param random  A Random instance for variant selection
     * @return The integer ID of the calculated variant
     */
    public static int calculateBabyVariant(HamsterEntity parentA, PassiveEntity parentB, Random random) {
        if (!(parentB instanceof HamsterEntity mother)) {
            AdorableHamsterPets.LOGGER.warn("Hamster breeding attempted with non-hamster mate. Assigning random variant to baby.");
            return random.nextInt(HamsterVariant.values().length);
        }

        HamsterVariant parentProvidingBaseColor = random.nextBoolean() ? parentA.getVariantEnum() : mother.getVariantEnum();
        HamsterVariant babyBaseColorEnum = parentProvidingBaseColor.getBaseVariant();

        @Nullable String fatherOverlayName = parentA.getVariantEnum().getOverlayTextureName();
        @Nullable String motherOverlayName = mother.getVariantEnum().getOverlayTextureName();

        List<HamsterVariant> allVariantsForBabyBase = HamsterVariant.getVariantsForBase(babyBaseColorEnum);

        // Build a list of overlay names that are NOT used by either parent
        List<@Nullable String> eligibleOverlayNames = new ArrayList<>();
        for (HamsterVariant variant : allVariantsForBabyBase) {
            @Nullable String candidateOverlay = variant.getOverlayTextureName();
            boolean matchesFather = fatherOverlayName != null && fatherOverlayName.equals(candidateOverlay);
            boolean matchesMother = motherOverlayName != null && motherOverlayName.equals(candidateOverlay);
            if (!matchesFather && !matchesMother) {
                eligibleOverlayNames.add(candidateOverlay);
            }
        }

        List<@Nullable String> finalSelectableOverlayNames = new ArrayList<>();
        boolean fatherHasOverlay = fatherOverlayName != null;
        boolean motherHasOverlay = motherOverlayName != null;

        if (fatherHasOverlay && motherHasOverlay) {
            // Baby MUST have an overlay. Prioritize overlays different from parents
            for (@Nullable String overlayName : eligibleOverlayNames) {
                if (overlayName != null) {
                    finalSelectableOverlayNames.add(overlayName);
                }
            }
            // If no different overlay is available, relax the rule and allow any overlay for that base color
            if (finalSelectableOverlayNames.isEmpty() && babyBaseColorEnum != HamsterVariant.WHITE) {
                for (HamsterVariant variant : allVariantsForBabyBase) {
                    if (variant.getOverlayTextureName() != null) {
                        finalSelectableOverlayNames.add(variant.getOverlayTextureName());
                    }
                }
            }
        } else {
            // If one or neither parent has an overlay, the baby can have no overlay
            finalSelectableOverlayNames.addAll(eligibleOverlayNames);
        }

        HamsterVariant babyFinalVariant;
        if (!finalSelectableOverlayNames.isEmpty()) {
            @Nullable String chosenOverlayName = finalSelectableOverlayNames.get(random.nextInt(finalSelectableOverlayNames.size()));
            babyFinalVariant = HamsterVariant.getVariantByBaseAndOverlay(babyBaseColorEnum, chosenOverlayName);
        } else {
            // Fallback case
            babyFinalVariant = babyBaseColorEnum;
        }

        return babyFinalVariant.getId();
    }

    private static boolean canSpawnBlue(RegistryEntry<Biome> biomeEntry) { return ConfigDataCache.isBlueBiome(biomeEntry); }
    private static boolean canSpawnLavender(RegistryEntry<Biome> biomeEntry) { return ConfigDataCache.isLavenderBiome(biomeEntry); }
    private static boolean canSpawnWhite(RegistryEntry<Biome> biomeEntry) { return ConfigDataCache.isWhiteBiome(biomeEntry); }
    private static boolean canSpawnGray(RegistryEntry<Biome> biomeEntry) { return ConfigDataCache.isGrayBiome(biomeEntry); }
    private static boolean canSpawnBlack(RegistryEntry<Biome> biomeEntry) { return ConfigDataCache.isBlackBiome(biomeEntry); }
    private static boolean canSpawnCream(RegistryEntry<Biome> biomeEntry) { return ConfigDataCache.isCreamBiome(biomeEntry); }
    private static boolean canSpawnChocolate(RegistryEntry<Biome> biomeEntry) { return ConfigDataCache.isChocolateBiome(biomeEntry); }

    private static HamsterVariant getRandomVariant(List<HamsterVariant> variantPool, Random random) {
        if (variantPool == null || variantPool.isEmpty()) {
            return HamsterVariant.ORANGE; // Fallback
        }
        return variantPool.get(random.nextInt(variantPool.size()));
    }
}