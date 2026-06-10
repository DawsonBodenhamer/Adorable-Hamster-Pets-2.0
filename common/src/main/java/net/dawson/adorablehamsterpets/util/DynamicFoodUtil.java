package net.dawson.adorablehamsterpets.util;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Utility for abstracting common food item tooltips.
 * In 1.20.1, primarily handles tooltip formatting and AppleSkin fallback warnings.
 */
public final class DynamicFoodUtil {

    private DynamicFoodUtil() {}

    /**
     * Appends standard tooltips and <s>dynamic nutrition stats</s> (only dynamic on 1.21.1+).
     */
    public static void appendTooltip(List<Text> tooltip, String tooltipBaseKey, int nutrition, float saturation, int defaultNutrition, float defaultSaturation) {
        if (Configs.AHP_UI.enableItemTooltips) {
            tooltip.add(Text.translatable(tooltipBaseKey + ".hint1").formatted(Formatting.GOLD));
            tooltip.add(Text.translatable(tooltipBaseKey + ".hint2").formatted(Formatting.GRAY));

            if (Platform.isModLoaded("appleskin")) {
                // If values have been altered from defaults, display a warning that AppleSkin
                // cannot read live config changes on 1.20.1 without a restart
                if (nutrition != defaultNutrition || Float.compare(saturation, defaultSaturation) != 0) {
                    tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.appleskin_warning").formatted(Formatting.DARK_GRAY));
                }
            } else {
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.appleskin.hint",
                        nutrition,
                        String.format("%.1f", saturation * nutrition * 2.0F)
                ).formatted(Formatting.DARK_GRAY));
            }
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
    }
}