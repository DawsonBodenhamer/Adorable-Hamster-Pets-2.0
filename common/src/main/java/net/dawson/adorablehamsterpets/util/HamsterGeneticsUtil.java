package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterColorZone;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.entity.custom.genetics.PaletteDefinition;
import net.dawson.adorablehamsterpets.tag.ModBiomeTags;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;

import java.util.*;

/**
 * Handles mathematical genetic inheritance mutation calculations and Punnett square logic
 */
public final class HamsterGeneticsUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Vec3d SWEET_POTATO_HSB;
    static {SWEET_POTATO_HSB = ColorSpaceUtil.analyzeImage("assets/adorablehamsterpets/textures/entity/hamster/easter_egg/sweet_potato.png").position();}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterGeneticsUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Determines if a palette is a valid wild overlay for a given base coat,
     * respecting the user's config settings for brightness and saturation.
     */
    public static boolean isValidWildOverlay(PaletteDefinition baseDef, PaletteDefinition overlayDef) {
        boolean enforceBrightness = Configs.AHP_WORLDGEN.enforceBrighterOverlays;
        boolean enforceSaturation = Configs.AHP_WORLDGEN.enforceMoreMutedOverlays;

        double baseSat = ColorSpaceUtil.getSaturation(baseDef.colorSpacePos());
        double baseBri = ColorSpaceUtil.getBrightness(baseDef.colorSpacePos());
        double paletteSat = ColorSpaceUtil.getSaturation(overlayDef.colorSpacePos());
        double paletteBri = ColorSpaceUtil.getBrightness(overlayDef.colorSpacePos());

        boolean briPass = !enforceBrightness || paletteBri > baseBri;
        boolean satPass = !enforceSaturation || paletteSat <= baseSat;

        return briPass && satPass;
    }

    /**
     * Determines if a palette is a valid breeding overlay for a given base coat.
     */
    public static boolean isValidBreedingOverlay(PaletteDefinition baseDef, PaletteDefinition overlayDef) {
        return !baseDef.id().equals(overlayDef.id());
    }

    /**
     * Calculates the genome for a newborn hamster based on inheritance rules and 3D color space midpoints.
     */
    public static HamsterGenome calculateBabyGenome(HamsterEntity parentAEntity, PassiveEntity parentBEntity, Random random) {
        if (!(parentBEntity instanceof HamsterEntity parentB)) {
            AdorableHamsterPets.LOGGER.warn("Hamster breeding attempted with non-hamster mate. Returning default genome.");
            return HamsterGenome.createDefault();
        }

        HamsterGenome genomeA = parentAEntity.getGenome();
        HamsterGenome genomeB = parentB.getGenome();

        // --- 1. Base Palette Blending ---
        PaletteDefinition baseA = HamsterPaletteManager.PALETTE_REGISTRY.get(genomeA.basePaletteId());
        PaletteDefinition baseB = HamsterPaletteManager.PALETTE_REGISTRY.get(genomeB.basePaletteId());

        Vec3d posA = parentAEntity.isSweetPotato() ? SWEET_POTATO_HSB : baseA.colorSpacePos();
        Vec3d posB = parentB.isSweetPotato() ? SWEET_POTATO_HSB : baseB.colorSpacePos();

        // Find exact mathematical center between parent colors
        Vec3d baseMidpoint = ColorSpaceUtil.calculateGeneticMidpoint(posA, posB, random);

        // HashSet avoids crash if both parents have same base color
        Set<String> baseExclusions = new HashSet<>();
        baseExclusions.add(baseA.id());
        baseExclusions.add(baseB.id());

        // Pick closest color to midpoint not identical to parents
        PaletteDefinition babyBase = HamsterPaletteManager.getClosestPalette(baseMidpoint, baseExclusions, null, false);

        // --- 2. Wild Overlay Inheritance ---
        int wildCount = (genomeA.wildOverlayPattern() > 0 ? 1 : 0) + (genomeB.wildOverlayPattern() > 0 ? 1 : 0);
        // 50% chance to inherit if at least one parent had one
        float wildChance = wildCount == 2 ? 1.0f : (wildCount == 1 ? 0.5f : 0.0f);

        int babyWildPattern = 0;
        String babyWildPaletteId = null;

        Set<HamsterColorZone> wildZones = new HashSet<>(ConfigDataCache.getAllowedWildOverlayZones());
        // Filter out clashing overlays if base is restricted
        if (ConfigDataCache.getRestrictedBaseZones().contains(babyBase.zone())) {
            wildZones.removeAll(ConfigDataCache.getClashingOverlayZones());
        }

        if (random.nextFloat() < wildChance) {
            Set<Integer> wildPatternExclusions = new HashSet<>();
            if (wildCount > 0) {
                wildPatternExclusions.add(genomeA.wildOverlayPattern());
                wildPatternExclusions.add(genomeB.wildOverlayPattern());
            }
            babyWildPattern = pickPattern(random, wildPatternExclusions);

            if (wildCount == 2) {
                PaletteDefinition wPalA = HamsterPaletteManager.PALETTE_REGISTRY.get(genomeA.wildOverlayPaletteId());
                PaletteDefinition wPalB = HamsterPaletteManager.PALETTE_REGISTRY.get(genomeB.wildOverlayPaletteId());
                if (wPalA != null && wPalB != null) {
                    Vec3d wildMidpoint = ColorSpaceUtil.calculateGeneticMidpoint(wPalA.colorSpacePos(), wPalB.colorSpacePos(), random);
                    babyWildPaletteId = HamsterPaletteManager.getClosestPalette(wildMidpoint, null, wildZones, true).id();
                }
            } else if (wildCount == 1) {
                babyWildPaletteId = genomeA.wildOverlayPattern() > 0 ? genomeA.wildOverlayPaletteId() : genomeB.wildOverlayPaletteId();
            } else {
                // This should theoretically never run
                babyWildPaletteId = HamsterPaletteManager.getRandomPalette(random, wildZones, true).id();
            }
        }

        // --- 3. Breeding Overlay Inheritance ---
        int breedCount = (genomeA.breedingOverlayPattern() > 0 ? 1 : 0) + (genomeB.breedingOverlayPattern() > 0 ? 1 : 0);
        float breedChance = breedCount == 2 ? 1.0f : (breedCount == 1 ? 0.5f : 0.45f);

        // Boost chance if parents are boring (no overlays)
        if (wildCount == 0 && breedCount == 0) {
            breedChance = 0.70f;
        }

        int babyBreedingPattern = 0;
        String babyBreedingPaletteId = null;

        if (random.nextFloat() < breedChance) {
            Set<Integer> breedPatternExclusions = new HashSet<>();
            breedPatternExclusions.add(babyWildPattern); // Prevent overlapping wild pattern perfectly
            if (breedCount > 0) {
                breedPatternExclusions.add(genomeA.breedingOverlayPattern());
                breedPatternExclusions.add(genomeB.breedingOverlayPattern());
            }
            babyBreedingPattern = pickPattern(random, breedPatternExclusions);

            if (breedCount == 2) {
                PaletteDefinition bPalA = HamsterPaletteManager.PALETTE_REGISTRY.get(genomeA.breedingOverlayPaletteId());
                PaletteDefinition bPalB = HamsterPaletteManager.PALETTE_REGISTRY.get(genomeB.breedingOverlayPaletteId());
                if (bPalA != null && bPalB != null) {
                    Vec3d breedMidpoint = ColorSpaceUtil.calculateGeneticMidpoint(bPalA.colorSpacePos(), bPalB.colorSpacePos(), random);
                    babyBreedingPaletteId = HamsterPaletteManager.getClosestPalette(breedMidpoint, null, null, true).id();
                }
            } else if (breedCount == 1) {
                babyBreedingPaletteId = genomeA.breedingOverlayPattern() > 0 ? genomeA.breedingOverlayPaletteId() : genomeB.breedingOverlayPaletteId();
            } else {
                // If parents are 1st gen, use the midpoint of their base colors to generate the breeding overlay
                babyBreedingPaletteId = HamsterPaletteManager.getClosestPalette(baseMidpoint, null, null, true).id();
            }
        }

        // --- 4. Prevent Breeding Overlay Matching Base Coat ---
        // If breeding overlay matches base coat, bump to different palette within same color zone
        if (babyBreedingPattern > 0 && babyBreedingPaletteId != null) {
            PaletteDefinition baseDef = HamsterPaletteManager.PALETTE_REGISTRY.get(babyBase.id());
            PaletteDefinition breedingDef = HamsterPaletteManager.PALETTE_REGISTRY.get(babyBreedingPaletteId);

            if (baseDef != null && breedingDef != null && !isValidBreedingOverlay(baseDef, breedingDef)) {
                Set<String> bumpExclusions = new HashSet<>();
                bumpExclusions.add(baseDef.id());

                PaletteDefinition bumpedPalette = HamsterPaletteManager.getClosestPalette(
                        breedingDef.colorSpacePos(),
                        bumpExclusions,
                        Set.of(breedingDef.zone()),
                        false
                );
                if (bumpedPalette != null) {
                    babyBreedingPaletteId = bumpedPalette.id();
                }
            }
        }

        // --- 5. Eye Genetics ---
        int babyEyeGenotype = calculateBabyEyeGenotype(genomeA.eyeGenotype(), genomeB.eyeGenotype(), random);

        return new HamsterGenome(babyBase.id(), babyWildPattern, babyWildPaletteId, babyBreedingPattern, babyBreedingPaletteId, babyEyeGenotype);
    }

    /**
     * Determines if a newly spawned wild hamster carries the recessive red-eye gene.
     * Driven dynamically by the diluteness score of its assigned color palette.
     */
    public static int generateWildEyeGenotype(String paletteId, Random random) {
        PaletteDefinition def = HamsterPaletteManager.PALETTE_REGISTRY.get(paletteId);
        if (def == null) return 0;

        // More dilute coat = higher chance to be carrier (up to 50%)
        float chanceOfBeingCarrier = def.diluteness() * 0.50f;
        return random.nextFloat() < chanceOfBeingCarrier ? 1 : 0;
    }

    /**
     * Calculates the genome for a newly spawned wild hamster based on its environment.
     * Selects a base color zone using weighted config probabilities, then selects a wild overlay.
     */
    public static HamsterGenome generateWildGenome(WorldView world, BlockPos pos, Random random) {
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);

        // --- 1. Find the Environment and Pick a Base Zone ---
        Map<HamsterColorZone, Integer> weights = ConfigDataCache.getWeightsForBiome(biomeEntry);
        HamsterColorZone baseZone = pickZoneFromWeights(weights, random);

        // Pick a random palette (static or programmatic) that belongs to this zone
        PaletteDefinition basePalette = HamsterPaletteManager.getRandomPalette(random, Set.of(baseZone), false);

        // --- 2. Determine Wild Overlay ---
        int wildPattern = 0;
        String wildPaletteId = null;

        boolean isCaveSpawning = isCaveEnvironment(world, pos);

        // 45% chance for wild hamsters to have an overlay
        if (random.nextFloat() < 0.45f) {
            int maxPattern = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;
            wildPattern = random.nextBetween(1, maxPattern);

            List<HamsterColorZone> allowedWildZones = new ArrayList<>(ConfigDataCache.getAllowedWildOverlayZones());

            // Prevent overlay from being same color zone as base
            allowedWildZones.remove(baseZone);

            // Clash prevention filter
            if (ConfigDataCache.getRestrictedBaseZones().contains(baseZone)) {
                allowedWildZones.removeAll(ConfigDataCache.getClashingOverlayZones());
            }

            // Prevent bright overlays on cave-spawned hamsters to help them blend in
            if (isCaveSpawning) {
                allowedWildZones.remove(HamsterColorZone.WHITE);
                allowedWildZones.remove(HamsterColorZone.LIGHT_GRAY);
            }

            // Default config: must be brighter and less saturated than base coat
            List<PaletteDefinition> validOverlays = HamsterPaletteManager.PALETTE_REGISTRY.values().stream()
                    .filter(p -> allowedWildZones.contains(p.zone()))
                    .filter(p -> isValidWildOverlay(basePalette, p))
                    .toList();

            if (!validOverlays.isEmpty()) {
                wildPaletteId = validOverlays.get(random.nextInt(validOverlays.size())).id();
            } else {
                // Fallback if no colors meet criteria
                HamsterColorZone fallbackZone = isCaveSpawning ? HamsterColorZone.LIGHT_GRAY : HamsterColorZone.WHITE;

                // In case user removed fallback zone from the allowed list, pick the first allowed zone
                if (!ConfigDataCache.getAllowedWildOverlayZones().contains(fallbackZone) && !ConfigDataCache.getAllowedWildOverlayZones().isEmpty()) {
                    fallbackZone = ConfigDataCache.getAllowedWildOverlayZones().iterator().next();
                }

                wildPaletteId = HamsterPaletteManager.getRandomPalette(random, Set.of(fallbackZone), false).id();
                AdorableHamsterPets.LOGGER.warn("[HamsterGenetics] Failed to find a brighter/less-saturated wild overlay for '{}'. Defaulting to {} Color Group: {}", basePalette.id(), fallbackZone.name(), wildPaletteId);
            }
        }

        // --- 3. Determine Eye Genetics ---
        int eyeGenotype = generateWildEyeGenotype(basePalette.id(), random);

        return new HamsterGenome(basePalette.id(), wildPattern, wildPaletteId, 0, null, eyeGenotype);
    }

    /**
     * Safely migrates legacy integer Variant IDs into the v3.6.0 HamsterGenome format.
     */
    public static HamsterGenome getGenomeForLegacyId(int id) {
        String base = "orange";
        int pattern = 0;
        String overlayPalette = "white"; // Old overlays were always white

        if (id == 0) { base = "orange"; }
        else if (id >= 7 && id <= 14) { base = "orange"; pattern = id - 6; }
        else if (id == 1) { base = "black"; }
        else if (id >= 15 && id <= 22) { base = "black"; pattern = id - 14; }
        else if (id == 2) { base = "chocolate"; }
        else if (id >= 23 && id <= 30) { base = "chocolate"; pattern = id - 22; }
        else if (id == 3) { base = "cream"; }
        else if (id >= 31 && id <= 38) { base = "cream"; pattern = id - 30; }
        else if (id == 4) { base = "dark_gray"; }
        else if (id >= 39 && id <= 46) { base = "dark_gray"; pattern = id - 38; }
        else if (id == 5) { base = "light_gray"; }
        else if (id >= 47 && id <= 54) { base = "light_gray"; pattern = id - 46; }
        else if (id == 6) { base = "white"; }
        else if (id == 55) { base = "blue"; }
        else if (id >= 57 && id <= 64) { base = "blue"; pattern = id - 56; }
        else if (id == 56) { base = "lavender"; }
        else if (id >= 65 && id <= 72) { base = "lavender"; pattern = id - 64; }

        // If the base is white, it technically shouldn't have a white overlay to prevent invisibility
        if (base.equals("white") && pattern > 0) {
            overlayPalette = "light_gray";
        }

        return new HamsterGenome(base, pattern, pattern > 0 ? overlayPalette : null, 0, null, 0);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isCaveEnvironment(WorldView world, BlockPos pos) {
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        boolean isCaveBiome = biomeEntry.isIn(ModBiomeTags.IS_CAVE);
        boolean isDeepAndDark = pos.getY() < 50 && !world.isSkyVisible(pos);

        return isCaveBiome || isDeepAndDark;
    }

    /**
     * Performs a weighted random selection of a HamsterColorZone.
     */
    private static HamsterColorZone pickZoneFromWeights(Map<HamsterColorZone, Integer> weights, Random random) {
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) return HamsterColorZone.ORANGE;

        int roll = random.nextInt(totalWeight);
        int currentSum = 0;

        for (Map.Entry<HamsterColorZone, Integer> entry : weights.entrySet()) {
            currentSum += entry.getValue();
            if (roll < currentSum) {
                return entry.getKey();
            }
        }
        return HamsterColorZone.ORANGE; // Fallback
    }

    /**
     * Resolves the Mendelian inheritance for eye colors with a boosted reward rate.
     * Genotypes: 0 = BB (Black), 1 = Br (Carrier), 2 = rr (Red)
     */
    private static int calculateBabyEyeGenotype(int genA, int genB, Random random) {
        if (genA == 0 && genB == 0) return 0;
        if (genA == 2 && genB == 2) return 2;

        // One pure dominant and one pure recessive makes it a carrier
        if ((genA == 2 && genB == 0) || (genA == 0 && genB == 2)) return 1;

        // Two carriers uses modified Punnett Square to reward breeders
        if (genA == 1 && genB == 1) {
            float roll = random.nextFloat();
            if (roll < 0.45f) return 2; // Red eyes
            if (roll < 0.90f) return 1; // Carrier
            return 0;                   // Black eyes
        }

        // One carrier and one recessive equals coin flip
        if ((genA == 2 && genB == 1) || (genA == 1 && genB == 2)) {
            return random.nextBoolean() ? 2 : 1;
        }

        // One carrier and one dominant equals coin flip
        if ((genA == 1 && genB == 0) || (genA == 0 && genB == 1)) {
            return random.nextBoolean() ? 1 : 0;
        }

        return 0; // Fallback
    }

    /**
     * Randomly selects a pattern (1-8) while ensuring it does not overlap with exclusions.
     */
    private static int pickPattern(Random random, Set<Integer> exclusions) {
        List<Integer> available = new ArrayList<>();
        int maxPattern = HamsterPaletteManager.OVERLAY_PATTERN_NAMES.size() - 1;

        for (int i = 1; i <= maxPattern; i++) {
            if (!exclusions.contains(i)) {
                available.add(i);
            }
        }
        if (available.isEmpty()) {
            return random.nextBetween(1, maxPattern); // Fallback if everything is excluded
        }
        return available.get(random.nextInt(available.size()));
    }
}