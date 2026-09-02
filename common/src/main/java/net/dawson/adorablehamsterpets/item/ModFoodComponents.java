package net.dawson.adorablehamsterpets.item;

import net.minecraft.world.food.FoodProperties;

public class ModFoodComponents {

    public static final FoodProperties CUCUMBER = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .build();

    public static final FoodProperties SLICED_CUCUMBER = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.3F)
            .build();

    public static final FoodProperties GREEN_BEANS = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .build();

    public static final FoodProperties STEAMED_GREEN_BEANS = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodProperties HAMSTER_FOOD_MIX = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodProperties CHEESE = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(0.8F)
            .build();
}
