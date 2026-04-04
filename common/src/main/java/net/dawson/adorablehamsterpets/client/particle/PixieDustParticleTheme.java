package net.dawson.adorablehamsterpets.client.particle;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Locale;

/**
 * Defines the fixed color palettes for the Pixie Dust particle.
 * Calculates an 8-slot array of RGB integers at initialization based on strict
 * HSB offsets from a base reference hue of 49 (Gold).
 */
public enum PixieDustParticleTheme implements EnumTranslatable {
    GOLD(0),
    CRIMSON(-42),
    LAVENDER(-115),
    ICE(145),
    EMERALD(100);

    private final int[] colors = new int[8];

    PixieDustParticleTheme(int themeHueOffset) {
        float base = 49f;
        float hOffset = themeHueOffset;

        // Slot math
        colors[0] = toRgb(base + hOffset + 3, 30, 99);
        colors[1] = toRgb(base + hOffset + 5, 42, 94);
        colors[2] = toRgb(base + hOffset - 8, 77, 83);
        colors[3] = toRgb(base + hOffset - 5, 67, 29);
        colors[4] = toRgb(base + hOffset, 49, 98);
        colors[5] = toRgb(base + hOffset + 5, 57, 67);
        colors[6] = 0xFFFFFF; // Pure white
        colors[7] = toRgb(base + hOffset - 2, 26, 96);
    }

    private static int toRgb(float h, float s, float b) {
        // Normalize hue to 0-360 range to prevent negative hue errors
        h = (h % 360f);
        if (h < 0) h += 360f;
        return Color.HSBtoRGB(h / 360f, s / 100f, b / 100f) & 0xFFFFFF;
    }

    public int getRandomColor(Random random) {
        return colors[random.nextInt(8)];
    }

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.pixie_dust_color_theme";
    }

    @NotNull
    @Override
    public String translationKey() {
        return prefix() + "." + this.name().toLowerCase(Locale.ROOT);
    }
}