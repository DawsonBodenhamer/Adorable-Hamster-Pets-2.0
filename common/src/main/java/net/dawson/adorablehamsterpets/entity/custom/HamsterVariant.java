package net.dawson.adorablehamsterpets.entity.custom;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public enum HamsterVariant {

    // --- BLACK Variants (Base + Overlays) ---
    BLACK(1, "black", null),
    BLACK_OVERLAY1(15, "black", "overlay1"),
    BLACK_OVERLAY2(16, "black", "overlay2"),
    BLACK_OVERLAY3(17, "black", "overlay3"),
    BLACK_OVERLAY4(18, "black", "overlay4"),
    BLACK_OVERLAY5(19, "black", "overlay5"),
    BLACK_OVERLAY6(20, "black", "overlay6"),
    BLACK_OVERLAY7(21, "black", "overlay7"),
    BLACK_OVERLAY8(22, "black", "overlay8"),

    // --- BLUE Variants (Base + Overlays) ---
    BLUE(55, "blue", null),
    BLUE_OVERLAY1(57, "blue", "overlay1"),
    BLUE_OVERLAY2(58, "blue", "overlay2"),
    BLUE_OVERLAY3(59, "blue", "overlay3"),
    BLUE_OVERLAY4(60, "blue", "overlay4"),
    BLUE_OVERLAY5(61, "blue", "overlay5"),
    BLUE_OVERLAY6(62, "blue", "overlay6"),
    BLUE_OVERLAY7(63, "blue", "overlay7"),
    BLUE_OVERLAY8(64, "blue", "overlay8"),

    // --- CHOCOLATE Variants (Base + Overlays) ---
    CHOCOLATE(2, "chocolate", null),
    CHOCOLATE_OVERLAY1(23, "chocolate", "overlay1"),
    CHOCOLATE_OVERLAY2(24, "chocolate", "overlay2"),
    CHOCOLATE_OVERLAY3(25, "chocolate", "overlay3"),
    CHOCOLATE_OVERLAY4(26, "chocolate", "overlay4"),
    CHOCOLATE_OVERLAY5(27, "chocolate", "overlay5"),
    CHOCOLATE_OVERLAY6(28, "chocolate", "overlay6"),
    CHOCOLATE_OVERLAY7(29, "chocolate", "overlay7"),
    CHOCOLATE_OVERLAY8(30, "chocolate", "overlay8"),

    // --- CREAM Variants (Base + Overlays) ---
    CREAM(3, "cream", null),
    CREAM_OVERLAY1(31, "cream", "overlay1"),
    CREAM_OVERLAY2(32, "cream", "overlay2"),
    CREAM_OVERLAY3(33, "cream", "overlay3"),
    CREAM_OVERLAY4(34, "cream", "overlay4"),
    CREAM_OVERLAY5(35, "cream", "overlay5"),
    CREAM_OVERLAY6(36, "cream", "overlay6"),
    CREAM_OVERLAY7(37, "cream", "overlay7"),
    CREAM_OVERLAY8(38, "cream", "overlay8"),

    // --- DARK_GRAY Variants (Base + Overlays) ---
    DARK_GRAY(4, "dark_gray", null),
    DARK_GRAY_OVERLAY1(39, "dark_gray", "overlay1"),
    DARK_GRAY_OVERLAY2(40, "dark_gray", "overlay2"),
    DARK_GRAY_OVERLAY3(41, "dark_gray", "overlay3"),
    DARK_GRAY_OVERLAY4(42, "dark_gray", "overlay4"),
    DARK_GRAY_OVERLAY5(43, "dark_gray", "overlay5"),
    DARK_GRAY_OVERLAY6(44, "dark_gray", "overlay6"),
    DARK_GRAY_OVERLAY7(45, "dark_gray", "overlay7"),
    DARK_GRAY_OVERLAY8(46, "dark_gray", "overlay8"),

    // --- LAVENDER Variants (Base + Overlays) ---
    LAVENDER(56, "lavender", null),
    LAVENDER_OVERLAY1(65, "lavender", "overlay1"),
    LAVENDER_OVERLAY2(66, "lavender", "overlay2"),
    LAVENDER_OVERLAY3(67, "lavender", "overlay3"),
    LAVENDER_OVERLAY4(68, "lavender", "overlay4"),
    LAVENDER_OVERLAY5(69, "lavender", "overlay5"),
    LAVENDER_OVERLAY6(70, "lavender", "overlay6"),
    LAVENDER_OVERLAY7(71, "lavender", "overlay7"),
    LAVENDER_OVERLAY8(72, "lavender", "overlay8"),

    // --- LIGHT_GRAY Variants (Base + Overlays) ---
    LIGHT_GRAY(5, "light_gray", null),
    LIGHT_GRAY_OVERLAY1(47, "light_gray", "overlay1"),
    LIGHT_GRAY_OVERLAY2(48, "light_gray", "overlay2"),
    LIGHT_GRAY_OVERLAY3(49, "light_gray", "overlay3"),
    LIGHT_GRAY_OVERLAY4(50, "light_gray", "overlay4"),
    LIGHT_GRAY_OVERLAY5(51, "light_gray", "overlay5"),
    LIGHT_GRAY_OVERLAY6(52, "light_gray", "overlay6"),
    LIGHT_GRAY_OVERLAY7(53, "light_gray", "overlay7"),
    LIGHT_GRAY_OVERLAY8(54, "light_gray", "overlay8"),

    // --- ORANGE Variants (Base + Overlays) ---
    ORANGE(0, "orange", null),
    ORANGE_OVERLAY1(7, "orange", "overlay1"),
    ORANGE_OVERLAY2(8, "orange", "overlay2"),
    ORANGE_OVERLAY3(9, "orange", "overlay3"),
    ORANGE_OVERLAY4(10, "orange", "overlay4"),
    ORANGE_OVERLAY5(11, "orange", "overlay5"),
    ORANGE_OVERLAY6(12, "orange", "overlay6"),
    ORANGE_OVERLAY7(13, "orange", "overlay7"),
    ORANGE_OVERLAY8(14, "orange", "overlay8"),

    // --- WHITE (Base Only) ---
    WHITE(6, "white", null); // White has no overlay by design


    private static final HamsterVariant[] BY_ID = Arrays.stream(values())
            .sorted(Comparator.comparingInt(HamsterVariant::getId))
            .toArray(HamsterVariant[]::new);

    private final int id;
    private final String baseTextureName;
    @Nullable
    private final String overlayTextureName;

    // --- Caches for helper methods ---
    private static final Map<HamsterVariant, List<HamsterVariant>> VARIANTS_BY_BASE_CACHE = new EnumMap<>(HamsterVariant.class);
    private record BaseOverlayPair(HamsterVariant base, @Nullable String overlay) {}
    private static final Map<BaseOverlayPair, HamsterVariant> VARIANT_BY_BASE_OVERLAY_CACHE = new HashMap<>();

    static {
        // Populate VARIANTS_BY_BASE_CACHE
        // Ensure this list matches the alphabetical order of base colors
        List<HamsterVariant> baseColors = Arrays.asList(
                BLACK, BLUE, CHOCOLATE, CREAM, DARK_GRAY, LAVENDER, LIGHT_GRAY, ORANGE, WHITE
        );
        for (HamsterVariant base : baseColors) {
            List<HamsterVariant> variants = new ArrayList<>();
            for (HamsterVariant currentVariant : values()) {
                if (currentVariant.getBaseVariant() == base) {
                    variants.add(currentVariant);
                }
            }
            VARIANTS_BY_BASE_CACHE.put(base, List.copyOf(variants));
        }

        // Populate VARIANT_BY_BASE_OVERLAY_CACHE
        for (HamsterVariant variant : values()) {
            VARIANT_BY_BASE_OVERLAY_CACHE.put(new BaseOverlayPair(variant.getBaseVariant(), variant.getOverlayTextureName()), variant);
        }
    }

    HamsterVariant(int id, String baseTextureName, @Nullable String overlayTextureName) {
        this.id = id;
        this.baseTextureName = baseTextureName;
        this.overlayTextureName = overlayTextureName;
    }

    public int getId() {
        return this.id;
    }

    public String getBaseTextureName() {
        return this.baseTextureName;
    }

    @Nullable
    public String getOverlayTextureName() {
        // White variant never has an overlay, this check is more robust
        if ("white".equals(this.baseTextureName) && this.overlayTextureName == null) {
            return null;
        }
        return this.overlayTextureName;
    }

    public HamsterVariant getBaseVariant() {
        return switch (this.baseTextureName) {
            case "black" -> BLACK;
            case "blue" -> BLUE;
            case "chocolate" -> CHOCOLATE;
            case "cream" -> CREAM;
            case "dark_gray" -> DARK_GRAY;
            case "lavender" -> LAVENDER;
            case "light_gray" -> LIGHT_GRAY;
            case "orange" -> ORANGE;
            case "white" -> WHITE;
            default -> ORANGE; // Fallback, should ideally not be reached
        };
    }

    public static HamsterVariant byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return ORANGE; // Default fallback
        }
        return BY_ID[id];
    }

    /**
     * Gets a list of all variants (including the base itself) for a given base color.
     * @param baseColorEnum The base color (e.g., HamsterVariant.CHOCOLATE).
     * @return A list of matching HamsterVariant enums.
     */
    public static List<HamsterVariant> getVariantsForBase(HamsterVariant baseColorEnum) {
        return VARIANTS_BY_BASE_CACHE.getOrDefault(baseColorEnum, List.of(baseColorEnum));
    }

    /**
     * Gets a specific variant by its base color and overlay name.
     * @param baseColorEnum The base color enum (e.g., HamsterVariant.LAVENDER).
     * @param overlayName The name of the overlay (e.g., "overlay1"), or null for no overlay.
     * @return The matching HamsterVariant, or the baseColorEnum if no exact match is found.
     */
    public static HamsterVariant getVariantByBaseAndOverlay(HamsterVariant baseColorEnum, @Nullable String overlayName) {
        HamsterVariant result = VARIANT_BY_BASE_OVERLAY_CACHE.get(new BaseOverlayPair(baseColorEnum, overlayName));
        return result != null ? result : baseColorEnum; // Fallback to base color if no specific overlay match
    }
}