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

    private final int[] colors = new int[7];

    PixieDustParticleTheme(int themeHueOffset) {
        float hOffset = themeHueOffset;

        // Slot math
        colors[0] = toRgb(47 + hOffset, 15, 100);
        colors[1] = toRgb(50 + hOffset, 56, 99);
        colors[2] = toRgb(49 + hOffset, 50, 90);
        colors[3] = toRgb(41 + hOffset, 99, 81);
        colors[4] = toRgb(45 + hOffset, 75, 75);
        colors[5] = toRgb(42 + hOffset, 72, 39);
        colors[6] = toRgb(44 + hOffset, 68, 29);
    }

    private static int toRgb(float h, float s, float b) {
        // Normalize hue to 0-360 range
        h = (h % 360f);
        if (h < 0) h += 360f;

        // Clamp saturation and brightness to 0-100%
        s = Math.max(0f, Math.min(100f, s));
        b = Math.max(0f, Math.min(100f, b));

        return Color.HSBtoRGB(h / 360f, s / 100f, b / 100f) & 0xFFFFFF;
    }

    public int getRandomColor(Random random) {
        return colors[random.nextInt(colors.length)];
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