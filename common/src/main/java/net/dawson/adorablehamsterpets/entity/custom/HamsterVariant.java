package net.dawson.adorablehamsterpets.entity.custom;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Defines all available color variations and overlay patterns for Hamster entities.
 * Pre-caches texture identifiers to eliminate render-loop object allocations.
 */
public enum HamsterVariant {

    // --- Black Variants ---
    BLACK(1, "black", null),
    BLACK_OVERLAY1(15, "black", "overlay1"),
    BLACK_OVERLAY2(16, "black", "overlay2"),
    BLACK_OVERLAY3(17, "black", "overlay3"),
    BLACK_OVERLAY4(18, "black", "overlay4"),
    BLACK_OVERLAY5(19, "black", "overlay5"),
    BLACK_OVERLAY6(20, "black", "overlay6"),
    BLACK_OVERLAY7(21, "black", "overlay7"),
    BLACK_OVERLAY8(22, "black", "overlay8"),

    // --- Blue Variants ---
    BLUE(55, "blue", null),
    BLUE_OVERLAY1(57, "blue", "overlay1"),
    BLUE_OVERLAY2(58, "blue", "overlay2"),
    BLUE_OVERLAY3(59, "blue", "overlay3"),
    BLUE_OVERLAY4(60, "blue", "overlay4"),
    BLUE_OVERLAY5(61, "blue", "overlay5"),
    BLUE_OVERLAY6(62, "blue", "overlay6"),
    BLUE_OVERLAY7(63, "blue", "overlay7"),
    BLUE_OVERLAY8(64, "blue", "overlay8"),

    // --- Chocolate Variants ---
    CHOCOLATE(2, "chocolate", null),
    CHOCOLATE_OVERLAY1(23, "chocolate", "overlay1"),
    CHOCOLATE_OVERLAY2(24, "chocolate", "overlay2"),
    CHOCOLATE_OVERLAY3(25, "chocolate", "overlay3"),
    CHOCOLATE_OVERLAY4(26, "chocolate", "overlay4"),
    CHOCOLATE_OVERLAY5(27, "chocolate", "overlay5"),
    CHOCOLATE_OVERLAY6(28, "chocolate", "overlay6"),
    CHOCOLATE_OVERLAY7(29, "chocolate", "overlay7"),
    CHOCOLATE_OVERLAY8(30, "chocolate", "overlay8"),

    // --- Cream Variants ---
    CREAM(3, "cream", null),
    CREAM_OVERLAY1(31, "cream", "overlay1"),
    CREAM_OVERLAY2(32, "cream", "overlay2"),
    CREAM_OVERLAY3(33, "cream", "overlay3"),
    CREAM_OVERLAY4(34, "cream", "overlay4"),
    CREAM_OVERLAY5(35, "cream", "overlay5"),
    CREAM_OVERLAY6(36, "cream", "overlay6"),
    CREAM_OVERLAY7(37, "cream", "overlay7"),
    CREAM_OVERLAY8(38, "cream", "overlay8"),

    // --- Dark Gray Variants ---
    DARK_GRAY(4, "dark_gray", null),
    DARK_GRAY_OVERLAY1(39, "dark_gray", "overlay1"),
    DARK_GRAY_OVERLAY2(40, "dark_gray", "overlay2"),
    DARK_GRAY_OVERLAY3(41, "dark_gray", "overlay3"),
    DARK_GRAY_OVERLAY4(42, "dark_gray", "overlay4"),
    DARK_GRAY_OVERLAY5(43, "dark_gray", "overlay5"),
    DARK_GRAY_OVERLAY6(44, "dark_gray", "overlay6"),
    DARK_GRAY_OVERLAY7(45, "dark_gray", "overlay7"),
    DARK_GRAY_OVERLAY8(46, "dark_gray", "overlay8"),

    // --- Lavender Variants ---
    LAVENDER(56, "lavender", null),
    LAVENDER_OVERLAY1(65, "lavender", "overlay1"),
    LAVENDER_OVERLAY2(66, "lavender", "overlay2"),
    LAVENDER_OVERLAY3(67, "lavender", "overlay3"),
    LAVENDER_OVERLAY4(68, "lavender", "overlay4"),
    LAVENDER_OVERLAY5(69, "lavender", "overlay5"),
    LAVENDER_OVERLAY6(70, "lavender", "overlay6"),
    LAVENDER_OVERLAY7(71, "lavender", "overlay7"),
    LAVENDER_OVERLAY8(72, "lavender", "overlay8"),

    // --- Light Gray Variants ---
    LIGHT_GRAY(5, "light_gray", null),
    LIGHT_GRAY_OVERLAY1(47, "light_gray", "overlay1"),
    LIGHT_GRAY_OVERLAY2(48, "light_gray", "overlay2"),
    LIGHT_GRAY_OVERLAY3(49, "light_gray", "overlay3"),
    LIGHT_GRAY_OVERLAY4(50, "light_gray", "overlay4"),
    LIGHT_GRAY_OVERLAY5(51, "light_gray", "overlay5"),
    LIGHT_GRAY_OVERLAY6(52, "light_gray", "overlay6"),
    LIGHT_GRAY_OVERLAY7(53, "light_gray", "overlay7"),
    LIGHT_GRAY_OVERLAY8(54, "light_gray", "overlay8"),

    // --- Orange Variants ---
    ORANGE(0, "orange", null),
    ORANGE_OVERLAY1(7, "orange", "overlay1"),
    ORANGE_OVERLAY2(8, "orange", "overlay2"),
    ORANGE_OVERLAY3(9, "orange", "overlay3"),
    ORANGE_OVERLAY4(10, "orange", "overlay4"),
    ORANGE_OVERLAY5(11, "orange", "overlay5"),
    ORANGE_OVERLAY6(12, "orange", "overlay6"),
    ORANGE_OVERLAY7(13, "orange", "overlay7"),
    ORANGE_OVERLAY8(14, "orange", "overlay8"),

    // --- White Variant ---
    WHITE(6, "white", null); // White has no overlay

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration and Setup
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final HamsterVariant[] BY_ID = Arrays.stream(values())
            .sorted(Comparator.comparingInt(HamsterVariant::getId))
            .toArray(HamsterVariant[]::new);

    private static final Map<HamsterVariant, List<HamsterVariant>> VARIANTS_BY_BASE_CACHE = new EnumMap<>(HamsterVariant.class);
    private static final Map<BaseOverlayPair, HamsterVariant> VARIANT_BY_BASE_OVERLAY_CACHE = new HashMap<>();

    // Compound key for fast exact variant lookups
    private record BaseOverlayPair(HamsterVariant base, @Nullable String overlay) {}

    static {
        // Order matches alphabetical base colors
        List<HamsterVariant> baseColors = List.of(
                BLACK, BLUE, CHOCOLATE, CREAM, DARK_GRAY, LAVENDER, LIGHT_GRAY, ORANGE, WHITE
        );

        // Group variants by their base color
        for (HamsterVariant base : baseColors) {
            List<HamsterVariant> variants = new ArrayList<>();
            for (HamsterVariant currentVariant : values()) {
                if (currentVariant.getBaseVariant() == base) {
                    variants.add(currentVariant);
                }
            }
            VARIANTS_BY_BASE_CACHE.put(base, List.copyOf(variants));
        }

        // Map precise base+overlay combos to actual enum instances
        for (HamsterVariant variant : values()) {
            VARIANT_BY_BASE_OVERLAY_CACHE.put(new BaseOverlayPair(variant.getBaseVariant(), variant.getOverlayTextureName()), variant);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Safely retrieves a variant by its integer ID.
     */
    public static HamsterVariant byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return ORANGE; // fallback for safety
        }
        return BY_ID[id];
    }

    /**
     * Gets all valid overlay combinations for a specific base color.
     */
    public static List<HamsterVariant> getVariantsForBase(HamsterVariant baseColorEnum) {
        return VARIANTS_BY_BASE_CACHE.getOrDefault(baseColorEnum, List.of(baseColorEnum));
    }

    /**
     * Finds the exact variant matching a base color and overlay pattern.
     */
    public static HamsterVariant getVariantByBaseAndOverlay(HamsterVariant baseColorEnum, @Nullable String overlayName) {
        HamsterVariant result = VARIANT_BY_BASE_OVERLAY_CACHE.get(new BaseOverlayPair(baseColorEnum, overlayName));
        return result != null ? result : baseColorEnum; // fallback to plain base color
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final int id;
    private final String baseTextureName;
    @Nullable private final String overlayTextureName;
    private final Identifier baseTextureId;
    @Nullable private final Identifier overlayTextureId;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    HamsterVariant(int id, String baseTextureName, @Nullable String overlayTextureName) {
        this.id = id;
        this.baseTextureName = baseTextureName;
        this.overlayTextureName = overlayTextureName;

        // Pre-cache identifiers
        this.baseTextureId = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/" + baseTextureName + ".png");

        // White explicitly lacks overlays
        if ("white".equals(this.baseTextureName) && this.overlayTextureName == null) {
            this.overlayTextureId = null;
        } else if (this.overlayTextureName != null) {
            this.overlayTextureId = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/" + overlayTextureName + ".png");
        } else {
            this.overlayTextureId = null;
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public int getId() {
        return this.id;
    }

    public String getBaseTextureName() {
        return this.baseTextureName;
    }

    @Nullable
    public String getOverlayTextureName() {
        return this.overlayTextureName;
    }

    /**
     * Gets the pre-cached Identifier for the base texture.
     */
    public Identifier getBaseTextureId() {
        return this.baseTextureId;
    }

    /**
     * Gets the pre-cached Identifier for the overlay texture, if it exists.
     */
    @Nullable
    public Identifier getOverlayTextureId() {
        return this.overlayTextureId;
    }

    /**
     * Resolves the root base color variant for this specific instance.
     */
    public HamsterVariant getBaseVariant() {
        return switch (this.baseTextureName) {
            case "black" -> BLACK;
            case "blue" -> BLUE;
            case "chocolate" -> CHOCOLATE;
            case "cream" -> CREAM;
            case "dark_gray" -> DARK_GRAY;
            case "lavender" -> LAVENDER;
            case "light_gray" -> LIGHT_GRAY;
            case "white" -> WHITE;
            default -> ORANGE; // Ultimate fallback
        };
    }
}