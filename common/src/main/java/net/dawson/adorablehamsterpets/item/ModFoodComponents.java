package net.dawson.adorablehamsterpets.item;

import net.minecraft.component.type.FoodComponent;

public class ModFoodComponents {

    public static final FoodComponent CUCUMBER = new FoodComponent.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .build();

    public static final FoodComponent SLICED_CUCUMBER = new FoodComponent.Builder()
            .nutrition(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodComponent GREEN_BEANS = new FoodComponent.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .build();

    public static final FoodComponent STEAMED_GREEN_BEANS = new FoodComponent.Builder()
            .nutrition(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodComponent HAMSTER_FOOD_MIX = new FoodComponent.Builder()
            .nutrition(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodComponent CHEESE = new FoodComponent.Builder()
            .nutrition(8) // Like cooked porkchop
            .saturationModifier(0.8F) // Like cooked porkchop
            .build();
}
