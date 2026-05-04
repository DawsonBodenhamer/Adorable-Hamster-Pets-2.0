package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.util.DynamicFoodUtil;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

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

    public ConfigurableFoodItem(Settings settings, Supplier<Integer> nutritionSupplier, Supplier<Float> saturationSupplier, String tooltipBaseKey) {
        super(settings);
        this.nutritionSupplier = nutritionSupplier;
        this.saturationSupplier = saturationSupplier;
        this.tooltipBaseKey = tooltipBaseKey;
    }

    @Override
    public ComponentMap getComponents() {
        return DynamicFoodUtil.getDynamicComponents(super.getComponents(), nutritionSupplier.get(), saturationSupplier.get());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DynamicFoodUtil.appendTooltip(tooltip, tooltipBaseKey, nutritionSupplier.get(), saturationSupplier.get());
        super.appendTooltip(stack, context, tooltip, type);
    }
}