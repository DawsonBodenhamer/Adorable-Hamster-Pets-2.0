package net.dawson.adorablehamsterpets.entity.custom.genetics;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.util.ColorSpaceUtil;
import net.dawson.adorablehamsterpets.util.HamsterGeneticsUtil;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * The central brain of the hamster genetics system.
 * Initializes and caches the 3D mathematical representations of all programmatic and static textures.
 */
public class HamsterPaletteManager {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final Map<String, PaletteDefinition> PALETTE_REGISTRY = new HashMap<>();

    // --- Programmatic Core Bases ---
    // Left to right = brightest to darkest (B1 → B8)
    private static final int[] CREAM = {0xf0d9a9, 0xe7cd99, 0xe0c493, 0xd8be8a, 0xd5b785, 0xd2b279, 0xcfae75, 0xc8a66c};
    private static final int[] WHITE = {0xececec, 0xe7e7e7, 0xe5e5e5, 0xe3e3e3, 0xdfdfdf, 0xdcdcdc, 0xdadada, 0xd3d3d3};
    private static final int[] BLACK = {0x3e3e3e, 0x333333, 0x2f2f2f, 0x2d2d2d, 0x282828, 0x252525, 0x232323, 0x1e1e1e};
    private static final int[] BLUE = {0x63808d, 0x57717c, 0x526a75, 0x4f6671, 0x465b64, 0x42555d, 0x394a51, 0x36464d};
    private static final int[] LAVENDER = {0xa391b5, 0x9582a9, 0x8f7da2, 0x8d7ba0, 0x827193, 0x7d6c8d, 0x786888, 0x70627f};
    private static final int[] LIGHT_GRAY = {0xcacaca, 0xb6b6b6, 0xb2b2b2, 0xb0b0b0, 0xa5a5a5, 0x9b9b9b, 0x949494, 0x898989};
    private static final int[] DARK_GRAY = {0x787878, 0x696969, 0x636363, 0x606060, 0x555555, 0x4f4f4f, 0x414141, 0x414141};
    private static final int[] CHOCOLATE = {0x765231, 0x60442a, 0x5b422a, 0x5a4129, 0x523a25, 0x4a3521, 0x46311e, 0x3e2d1b};
    private static final int[] ORANGE = {0xedb572, 0xe0a058, 0xd99b56, 0xd49653, 0xd09251, 0xcd8d4e, 0xc9874a, 0xc08049};

    // --- Static Community Textures ---
    // Organized by contributor
    private static final Map<String, List<String>> COMMUNITY_TEXTURES = Map.of(
        "jimcerberus", List.of(
                "beige",
                "blue_fawn",
                "cheesecake_mocha",
                "cinnamon",
                "classic_mocha",
                "coconut",
                "diluted_blue",
                "homo_silver_gray",
                "opal",
                "pearl_rose",
                "pearl_white",
                "rust",
                "sable",
                "silver",
                "silver_gray_dark",
                "silver_gray_het",
                "smoke_pearl",
                "tortoise_shell"
        )
    );

    // --- Genetic Dictionaries ---
    public static final List<String> BASE_PATTERN_NAMES = List.of("fur_pattern");
    public static final List<String> OVERLAY_PATTERN_NAMES = List.of("none", "splotched_body", "body", "overlay_spots", "belly", "torso_fold", "torso_wrap", "splotched_ears", "inverted_spots", "piebald");
    public static final List<String> EYE_GENOTYPE_NAMES = List.of("black", "carrier_black", "red");

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration and Setup
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Called during mod setup to build the genetic neural network.
     */
    public static void init() {
        PALETTE_REGISTRY.clear();

        // Pipeline A: Programmatic Processing
        registerProgrammatic("cream", CREAM);
        registerProgrammatic("white", WHITE);
        registerProgrammatic("black", BLACK);
        registerProgrammatic("blue", BLUE);
        registerProgrammatic("lavender", LAVENDER);
        registerProgrammatic("light_gray", LIGHT_GRAY);
        registerProgrammatic("dark_gray", DARK_GRAY);
        registerProgrammatic("chocolate", CHOCOLATE);
        registerProgrammatic("orange", ORANGE);

        // Pipeline B: Static Processing
        for (Map.Entry<String, List<String>> entry : COMMUNITY_TEXTURES.entrySet()) {
            String author = entry.getKey();
            for (String textureName : entry.getValue()) {
                registerStatic(author, textureName);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Triggers the initial genetics report if it hasn't been printed yet.
     */
    public static void triggerInitialReport() {
        if (!initialReportPrinted) {
            printGeneticsReport(null, false);
            initialReportPrinted = true;
        }
    }

    /**
     * Calculates the total number of visually distinct wild hamster variants that can naturally spawn,
     * dynamically accounting for all current user config restrictions (allowed zones, clash rules, etc).
     */
    public static long calculateTotalWildVariants() {
        long wildVisuallyDistinct = 0;
        Set<HamsterColorZone> allowedWildZonesSet = ConfigDataCache.getAllowedWildOverlayZones();
        long overlayPatterns = OVERLAY_PATTERN_NAMES.size() - 1; // Subtract 1 to exclude 'none'

        for (PaletteDefinition baseDef : PALETTE_REGISTRY.values()) {
            long baseWildCombs = 1; // Just base coat

            // --- Wild Overlay Rules ---
            List<HamsterColorZone> allowedWildZones = new ArrayList<>(allowedWildZonesSet);
            allowedWildZones.remove(baseDef.zone());

            // Clash prevention filter
            if (ConfigDataCache.getRestrictedBaseZones().contains(baseDef.zone())) {
                allowedWildZones.removeAll(ConfigDataCache.getClashingOverlayZones());
            }

            long validWildOverlays = PALETTE_REGISTRY.values().stream()
                    .filter(p -> allowedWildZones.contains(p.zone()))
                    .filter(p -> HamsterGeneticsUtil.isValidWildOverlay(baseDef, p))
                    .count();

            baseWildCombs += (validWildOverlays * overlayPatterns);
            wildVisuallyDistinct += baseWildCombs;
        }

        return wildVisuallyDistinct;
    }

    /**
     * Calculates the absolute total number of visually distinct hamster variants possible through breeding,
     * factoring in all current wild-spawn restrictions and available breeding overlays.
     */
    public static long calculateTotalPossibleVariants() {
        long basePalettes = PALETTE_REGISTRY.size();
        long overlayPatterns = OVERLAY_PATTERN_NAMES.size() - 1; // Exclude 'none'
        // Filter out hidden carrier genes since they don't change appearance
        long visualEyeStates = EYE_GENOTYPE_NAMES.stream().filter(name -> !name.contains("carrier")).count();

        long visuallyDistinct = 0;

        for (PaletteDefinition baseDef : PALETTE_REGISTRY.values()) {
            List<HamsterColorZone> allowedWildZones = new ArrayList<>(ConfigDataCache.getAllowedWildOverlayZones());
            allowedWildZones.remove(baseDef.zone());

            // Clash prevention filter
            if (ConfigDataCache.getRestrictedBaseZones().contains(baseDef.zone())) {
                allowedWildZones.removeAll(ConfigDataCache.getClashingOverlayZones());
            }

            long validWildOverlays = PALETTE_REGISTRY.values().stream()
                    .filter(p -> allowedWildZones.contains(p.zone()))
                    .filter(p -> HamsterGeneticsUtil.isValidWildOverlay(baseDef, p))
                    .count();

            // Count valid breeding palettes for this specific base coat
            long validBreedingPalettes = PALETTE_REGISTRY.values().stream()
                    .filter(p -> HamsterGeneticsUtil.isValidBreedingOverlay(baseDef, p))
                    .count();

            // 1. States with no wild overlay
            long noWildOverlayStates = 1;
            long breedingCombsIfNoWild = 1 + (validBreedingPalettes * overlayPatterns);
            long totalNoWildBranch = noWildOverlayStates * breedingCombsIfNoWild;

            // 2. States with wild overlay
            long withWildOverlayStates = validWildOverlays * overlayPatterns;
            // Breeding pattern must not overlap wild pattern
            long breedingCombsIfWild = 1 + (validBreedingPalettes * (overlayPatterns - 1));
            long totalWithWildBranch = withWildOverlayStates * breedingCombsIfWild;

            // Combine branches and multiply by visual eye states
            visuallyDistinct += (totalNoWildBranch + totalWithWildBranch) * visualEyeStates;
        }

        return visuallyDistinct;
    }

    /**
     * Finds the closest palette to a target 3D coordinate.
     */
    public static PaletteDefinition getClosestPalette(Vec3d targetPos, Set<String> exclusions, Set<HamsterColorZone> allowedZones, boolean requireProgrammatic) {
        PaletteDefinition closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PaletteDefinition def : PALETTE_REGISTRY.values()) {
            if (exclusions != null && exclusions.contains(def.id())) continue;
            if (allowedZones != null && !allowedZones.contains(def.zone())) continue;
            if (requireProgrammatic && def.type() != TextureType.PROGRAMMATIC) continue;

            double dist = ColorSpaceUtil.getColorDistance(targetPos, def.colorSpacePos());
            if (dist < minDistance) {
                minDistance = dist;
                closest = def;
            }
        }

        // Fallback if constraints were too strict
        if (closest == null && !PALETTE_REGISTRY.isEmpty()) {
            return PALETTE_REGISTRY.values().iterator().next();
        }

        return closest;
    }

    /**
     * Selects a random palette, filtered by zone and type constraints.
     */
    public static PaletteDefinition getRandomPalette(Random random, Set<HamsterColorZone> allowedZones, boolean requireProgrammatic) {
        List<PaletteDefinition> valid = new ArrayList<>();
        for (PaletteDefinition def : PALETTE_REGISTRY.values()) {
            if (allowedZones != null && !allowedZones.contains(def.zone())) continue;
            if (requireProgrammatic && def.type() != TextureType.PROGRAMMATIC) continue;
            valid.add(def);
        }

        if (valid.isEmpty()) {
            return PALETTE_REGISTRY.values().iterator().next();
        }
        return valid.get(random.nextInt(valid.size()));
    }

    /**
     * Calculates and prints the current status of the genetics engine.
     * Can be routed to the server console or directly to a player's chat.
     */
    public static void printGeneticsReport(@Nullable ServerCommandSource source, boolean toChat) {
        Consumer<String> output = line -> {
            if (toChat && source != null) {
                // Strip ASCII box formatting and normalize dotted leaders
                final String chatLine = line.replaceAll("^\\s*\\|\\s*", "")
                        .replaceAll("\\.{2,}", "...");

                if (!chatLine.trim().isEmpty()) {
                    source.sendFeedback(() -> Text.literal(chatLine).formatted(Formatting.WHITE), false);
                }
            } else {
                AdorableHamsterPets.LOGGER.info(line);
            }
        };

        long basePalettes = PALETTE_REGISTRY.size();
        long overlayPatterns = OVERLAY_PATTERN_NAMES.size() - 1; // Subtract 1 to exclude 'none'
        long visualEyeStates = EYE_GENOTYPE_NAMES.stream().filter(name -> !name.contains("carrier")).count();

        Set<HamsterColorZone> allowedWildZonesSet = ConfigDataCache.getAllowedWildOverlayZones();
        long wildAllowedPalettesCount = PALETTE_REGISTRY.values().stream()
                .filter(p -> allowedWildZonesSet.contains(p.zone()))
                .count();

        long maxWildOverlays = wildAllowedPalettesCount * overlayPatterns + 1;
        long maxBreedingOverlays = basePalettes * overlayPatterns + 1;

        // Calculate variants
        long wildVisuallyDistinct = calculateTotalWildVariants();
        long visuallyDistinct = calculateTotalPossibleVariants();

        // Connections = N * (N + 1) / 2. Using BigInteger because it exceeds 9 quintillion
        BigInteger bigN = BigInteger.valueOf(visuallyDistinct);
        BigInteger connections = bigN.multiply(bigN.add(BigInteger.ONE)).divide(BigInteger.TWO);
        NumberFormat formatter = NumberFormat.getIntegerInstance();

        output.accept("  |                                                                                      ");
        output.accept("  |                                                                                      ");
        output.accept("  |                      Adorable Hamster Pets Procedural Genetics Engine                ");
        output.accept(toChat
                ? "-------------------------------------------"
                : "  |      --------------------------------------------------------------------------------");
        output.accept(String.format("  |      Base Fur Palettes ................. | %d", basePalettes));
        output.accept(String.format("  |      Base Fur Patterns ................. | x %d", BASE_PATTERN_NAMES.size()));
        output.accept(String.format("  |      Potential Wild Overlay Types ...... | x %d (%d Palettes x %d Patterns + 1 blank)", maxWildOverlays, wildAllowedPalettesCount, overlayPatterns));
        output.accept(String.format("  |      Potential Breeding Overlay Types .. | x %d (%d Palettes x %d Patterns + 1 blank)", maxBreedingOverlays, basePalettes, overlayPatterns));
        output.accept(String.format("  |      Potential Eye Color Types ......... | x %d", visualEyeStates));
        output.accept(String.format("  |      Visually Distinct Wild Variants ... | = %s", formatter.format(wildVisuallyDistinct)));
        output.accept("  |        ↑ Filtered: Overlays must...");

        String allowedZonesStr = String.join(", ", Configs.AHP_WORLDGEN.allowedWildOverlayZones);
        output.accept(String.format("  |          - Be allowed (%s) ← default: neutrals", allowedZonesStr));

        if (Configs.AHP_WORLDGEN.enforceBrighterOverlays) {
            output.accept("  |          - Be brighter than base color");
        }
        if (Configs.AHP_WORLDGEN.enforceMoreMutedOverlays) {
            output.accept("  |          - Be less saturated than base color");
        }

        boolean hasClashRules = !ConfigDataCache.getRestrictedBaseZones().isEmpty() && !ConfigDataCache.getClashingOverlayZones().isEmpty();
        if (hasClashRules) {
            String restrictedStr = String.join(", ", Configs.AHP_WORLDGEN.restrictedBaseZones);
            output.accept(String.format("  |          - Not clash with the %s color zones", restrictedStr));
        }

        output.accept(String.format("  |      Total Possible After Breeding ..... | = %s", formatter.format(visuallyDistinct)));
        output.accept(String.format("  |      Number of 3D Color Relationships .. | = %s", formatter.format(connections)));

        // --- Community Textures by Creator and Zone ---
        Map<String, Map<HamsterColorZone, List<String>>> communityTextures = new TreeMap<>();

        for (PaletteDefinition def : PALETTE_REGISTRY.values()) {
            if (def.type() == TextureType.STATIC) {
                communityTextures
                        .computeIfAbsent(def.author(), k -> new EnumMap<>(HamsterColorZone.class))
                        .computeIfAbsent(def.zone(), k -> new ArrayList<>())
                        .add(MiscUtil.formatHumanReadableName(def.id()));
            }
        }

        if (!communityTextures.isEmpty()) {
            output.accept("  |                                                                                      ");
            output.accept("  |                                                                                      ");
            output.accept("  |                   Community Hamster Textures Automatically Sorted into               ");
            output.accept("  |                   3D Color Space Based on Hue, Saturation & Brightness               ");
            output.accept("  |                                                                                      ");

            for (Map.Entry<String, Map<HamsterColorZone, List<String>>> authorEntry : communityTextures.entrySet()) {
                output.accept(String.format("  |      > @%s", authorEntry.getKey()));

                for (Map.Entry<HamsterColorZone, List<String>> zoneEntry : authorEntry.getValue().entrySet()) {
                    output.accept(String.format("  |        [%s Zone] ←-- %s", zoneEntry.getKey().name(), String.join(", ", zoneEntry.getValue())));
                }
            }
        }
        output.accept("  |                                                                                      ");
        output.accept("  |                                                                                      ");
        output.accept("  |                                                                                      ");
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private static boolean initialReportPrinted = false;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static void registerProgrammatic(String baseName, int[] baseHex) {
        // -1 = Warm, 0 = Normal, 1 = Cool
        int[] shiftModes = {-1, 0, 1};
        String[] prefixes = {"warm_", "", "cool_"};

        for (int i = 0; i < shiftModes.length; i++) {
            int shiftMode = shiftModes[i];
            String id = prefixes[i].isEmpty() ? baseName : prefixes[i] + baseName;

            int[] shiftedHex = ColorSpaceUtil.applyHueShiftToPalette(baseHex, shiftMode);
            ColorSpaceUtil.ColorData data = ColorSpaceUtil.analyzePalette(shiftedHex);
            HamsterColorZone zone = ColorSpaceUtil.determineZone(data.position());

            PALETTE_REGISTRY.put(id, new PaletteDefinition(id, "default", TextureType.PROGRAMMATIC, shiftedHex, data.position(), data.dilutenessScore(), zone));
        }
    }

    private static void registerStatic(String author, String name) {
        String resourcePath = "assets/" + AdorableHamsterPets.MOD_ID + "/textures/entity/hamster/" + author + "/" + name + ".png";
        ColorSpaceUtil.ColorData data = ColorSpaceUtil.analyzeImage(resourcePath);
        HamsterColorZone zone = ColorSpaceUtil.determineZone(data.position());

        PALETTE_REGISTRY.put(name, new PaletteDefinition(name, author, TextureType.STATIC, null, data.position(), data.dilutenessScore(), zone));
    }
}