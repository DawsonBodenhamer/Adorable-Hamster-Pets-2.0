package net.dawson.adorablehamsterpets.item;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.minecraft.item.FoodComponent;

public class ModFoodComponents {

    public static final FoodComponent CUCUMBER = new FoodComponent.Builder()
            .hunger(2)
            .saturationModifier(0.3F)
            .build();

    public static final FoodComponent SLICED_CUCUMBER = new FoodComponent.Builder()
            .hunger(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodComponent GREEN_BEANS = new FoodComponent.Builder()
            .hunger(2)
            .saturationModifier(0.3F)
            .build();

    public static final FoodComponent STEAMED_GREEN_BEANS = new FoodComponent.Builder()
            .hunger(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodComponent HAMSTER_FOOD_MIX = new FoodComponent.Builder()
            .hunger(4)
            .saturationModifier(0.6F)
            .build();

    public static final FoodComponent CHEESE = new FoodComponent.Builder()
            .hunger(8) // Like cooked porkchop
            .saturationModifier(0.8F) // Like cooked porkchop
            .build();
}
