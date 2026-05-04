package net.dawson.adorablehamsterpets.util;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Utility for dynamically updating and syncing FoodComponents on items
 * based on live configuration values.
 */
public final class DynamicFoodUtil {

    private DynamicFoodUtil() {}

    /**
     * Generates a dynamic ComponentMap that overrides the base food component with live config values.
     */
    public static ComponentMap getDynamicComponents(ComponentMap baseComponents, int nutrition, float saturation) {
        FoodComponent dynamicFoodComponent = new FoodComponent.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation)
                .build();

        ComponentMap override = ComponentMap.builder()
                .add(DataComponentTypes.FOOD, dynamicFoodComponent)
                .build();

        return ComponentMap.of(baseComponents, override);
    }

    /**
     * Appends standard tooltips and dynamic nutrition stats (if AppleSkin is absent).
     */
    public static void appendTooltip(List<Text> tooltip, String tooltipBaseKey, int nutrition, float saturation) {
        if (Configs.AHP.enableItemTooltips) {
            tooltip.add(Text.translatable(tooltipBaseKey + ".hint1").formatted(Formatting.GOLD));
            tooltip.add(Text.translatable(tooltipBaseKey + ".hint2").formatted(Formatting.GRAY));

            if (!Platform.isModLoaded("appleskin")) {
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