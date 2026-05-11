package net.dawson.adorablehamsterpets.entity.custom.genetics;

import net.dawson.adorablehamsterpets.util.ColorSpaceUtil;
import net.minecraft.util.math.Vec3d;

/**
 * Defines 12 abstract color categories used for spawning environments and overlay exclusion rules.
 * Each zone mathematically calculates its "ideal center" in 3D HSB color space using the core palettes or static images.
 */
public enum HamsterColorZone {
    WHITE(new int[]{0xe7e7e7, 0xe2e2e2, 0xdfdfdf, 0xdddddd, 0xd7d7d7, 0xd4d4d4, 0xd1d1d1, 0xc8c8c8}),
    BLUE(new int[]{0x63808d, 0x57717c, 0x526a75, 0x4f6671, 0x465b64, 0x42555d, 0x394a51, 0x36464d}),
    SKY("assets/adorablehamsterpets/textures/entity/hamster/jimcerberus/diluted_blue.png"),
    LAVENDER(new int[]{0x897a9c, 0x7c6c91, 0x76678a, 0x746588, 0x6a5c7c, 0x655876, 0x615471, 0x5a4e69}),
    CHERRY("assets/adorablehamsterpets/textures/entity/hamster/jimcerberus/pearl_rose.png"),
    LIGHT_GRAY(new int[]{0xcacaca, 0xb6b6b6, 0xb2b2b2, 0xb0b0b0, 0xa5a5a5, 0x9b9b9b, 0x949494, 0x898989}),
    DARK_GRAY(new int[]{0x787878, 0x696969, 0x636363, 0x606060, 0x555555, 0x4f4f4f, 0x414141, 0x414141}),
    CREAM(new int[]{0xf0d9a9, 0xe7cd99, 0xe0c493, 0xd8be8a, 0xd5b785, 0xd2b279, 0xcfae75, 0xc8a66c}),
    BLACK(new int[]{0x3e3e3e, 0x333333, 0x2f2f2f, 0x2d2d2d, 0x282828, 0x252525, 0x232323, 0x1e1e1e}),
    CHOCOLATE(new int[]{0x765231, 0x60442a, 0x5b422a, 0x5a4129, 0x523a25, 0x4a3521, 0x46311e, 0x3e2d1b}),
    RUST("assets/adorablehamsterpets/textures/entity/hamster/jimcerberus/rust.png"),
    ORANGE(new int[]{0xedb572, 0xe0a058, 0xd99b56, 0xd49653, 0xd09251, 0xcd8d4e, 0xc9874a, 0xc08049});

    private final Vec3d idealCenter;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    // Programmatic
    HamsterColorZone(int[] referenceHex) {
        this.idealCenter = ColorSpaceUtil.analyzePalette(referenceHex).position();
    }

    // Static image
    HamsterColorZone(String resourcePath) {
        this.idealCenter = ColorSpaceUtil.analyzeImage(resourcePath).position();
    }

    public Vec3d getIdealCenter() {
        return this.idealCenter;
    }
}