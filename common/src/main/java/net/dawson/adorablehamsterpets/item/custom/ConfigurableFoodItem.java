package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.util.DynamicFoodUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A generic food item that handles dynamic tooltips for configuration settings.
 * Dynamic nutrition and saturation values only work on 1.21.1+
 */
public class ConfigurableFoodItem extends Item {
    private final Supplier<Integer> nutritionSupplier;
    private final Supplier<Float> saturationSupplier;
    private final int defaultNutrition;
    private final float defaultSaturation;
    private final String tooltipBaseKey;

    public ConfigurableFoodItem(Settings settings, Supplier<Integer> nutritionSupplier, Supplier<Float> saturationSupplier, int defaultNutrition, float defaultSaturation, String tooltipBaseKey) {
        super(settings);
        this.nutritionSupplier = nutritionSupplier;
        this.saturationSupplier = saturationSupplier;
        this.defaultNutrition = defaultNutrition;
        this.defaultSaturation = defaultSaturation;
        this.tooltipBaseKey = tooltipBaseKey;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        DynamicFoodUtil.appendTooltip(tooltip, this.tooltipBaseKey, this.nutritionSupplier.get(), this.saturationSupplier.get(), this.defaultNutrition, this.defaultSaturation);
        super.appendTooltip(stack, world, tooltip, context);
    }
}