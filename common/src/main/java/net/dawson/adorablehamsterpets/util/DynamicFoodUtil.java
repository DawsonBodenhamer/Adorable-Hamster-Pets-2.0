package net.dawson.adorablehamsterpets.util;

import java.util.function.Consumer;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
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
    public static DataComponentMap getDynamicComponents(DataComponentMap baseComponents, int nutrition, float saturation) {
        FoodProperties dynamicFoodComponent = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation)
                .build();

        DataComponentMap override = DataComponentMap.builder()
                .set(DataComponents.FOOD, dynamicFoodComponent)
                .build();

        return DataComponentMap.composite(baseComponents, override);
    }

    /**
     * Appends standard tooltips and dynamic nutrition stats (if AppleSkin is absent).
     */
    public static void appendTooltip(Consumer<Component> tooltip, String tooltipBaseKey, int nutrition, float saturation) {
        if (Configs.AHP_UI.enableItemTooltips) {
            tooltip.accept(Component.translatable(tooltipBaseKey + ".hint1").withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.translatable(tooltipBaseKey + ".hint2").withStyle(ChatFormatting.GRAY));

            if (!Platform.isModLoaded("appleskin")) {
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.appleskin.hint",
                        nutrition,
                        String.format("%.1f", saturation * nutrition * 2.0F)
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }
    }
}