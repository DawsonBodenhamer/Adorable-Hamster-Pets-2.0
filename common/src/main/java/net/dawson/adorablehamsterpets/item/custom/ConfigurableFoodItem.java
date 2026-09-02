package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.util.DynamicFoodUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import java.util.function.Supplier;

/**
 * A generic food item that dynamically ties its nutrition and saturation values
 * to live configuration settings.
 */
public class ConfigurableFoodItem extends Item {
    private final Supplier<Integer> nutritionSupplier;
    private final Supplier<Float> saturationSupplier;
    private final String tooltipBaseKey;

    // Cache dynamically generated component map to preserve object identity
    private DataComponentMap cachedComponents = null;
    private int lastNutrition = -1;
    private float lastSaturation = -1.0f;

    public ConfigurableFoodItem(Properties settings, Supplier<Integer> nutritionSupplier, Supplier<Float> saturationSupplier, String tooltipBaseKey) {
        super(settings);
        this.nutritionSupplier = nutritionSupplier;
        this.saturationSupplier = saturationSupplier;
        this.tooltipBaseKey = tooltipBaseKey;
    }

    @Override
    public DataComponentMap components() {
        int currentNutrition = this.nutritionSupplier.get();
        float currentSaturation = this.saturationSupplier.get();

        // Rebuild and cache only when the configuration values change or on first run
        if (this.cachedComponents == null || currentNutrition != this.lastNutrition || currentSaturation != this.lastSaturation) {
            this.lastNutrition = currentNutrition;
            this.lastSaturation = currentSaturation;
            this.cachedComponents = DynamicFoodUtil.getDynamicComponents(super.components(), currentNutrition, currentSaturation);
        }

        return this.cachedComponents;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        DynamicFoodUtil.appendTooltip(tooltip, this.tooltipBaseKey, this.nutritionSupplier.get(), this.saturationSupplier.get());
        super.appendHoverText(stack, context, tooltip, type);
    }
}