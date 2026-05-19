package net.dawson.adorablehamsterpets.fabric.datagen;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends FabricRecipeProvider {

    // --- 1. Constructor ---
    // 1.20.1 signature
    public ModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    // --- 2. Helpers ---
    // For Hamster Bed variants
    // 1.20.1 signature
    private void offerHamsterBedRecipe(Consumer<RecipeJsonProvider> exporter, Item planks, WoodVariant variant) {
        // Result is the specific item for this variant
        Item resultItem = ModItems.HAMSTER_BED_ITEMS.get(variant).get();

        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, resultItem)
                .pattern(" H ")
                .pattern("HHH")
                .pattern("PPP")
                .input('H', ModItems.HAMSTER_BEDDING.get())
                .input('P', planks)
                .group("hamster_bed")
                .criterion("has_hamster_bedding", conditionsFromItem(ModItems.HAMSTER_BEDDING.get()))
                // 1.20.1: Use new Identifier()
                .offerTo(exporter, new Identifier(AdorableHamsterPets.MOD_ID, "hamster_bed_" + variant.asString()));
    }

    // Helper for Smithing Upgrades
    // 1.20.1 signature
    private void offerHamsterArmorUpgrade(Consumer<RecipeJsonProvider> exporter, Item template, Item material, Item result) {
        SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(template),
                        Ingredient.fromTag(ModItemTagProvider.HAMSTER_ARMOR_ENCHANTABLE), // Allow any armor tier as base
                        Ingredient.ofItems(material),
                        RecipeCategory.COMBAT,
                        result
                )
                .criterion("has_acorn_armor", conditionsFromItem(ModItems.HAMSTER_ARMOR_ACORN.get()))
                .criterion("has_material", conditionsFromItem(material))
                .offerTo(exporter, new Identifier(getItemPath(result) + "_smithing"));
    }

    // --- 3. Public Methods ---
    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        // --- Smelting Recipes ---
        // Smelting Green Beans to Steamed Green Beans
        offerSmelting(exporter, List.of(ModItems.GREEN_BEANS.get()), RecipeCategory.FOOD, ModItems.STEAMED_GREEN_BEANS.get(),
                0.35f, 200, "steamed_green_beans");

        // Smoking Green Beans to Steamed Green Beans
        CookingRecipeJsonBuilder.createSmoking(Ingredient.ofItems(ModItems.GREEN_BEANS.get()), RecipeCategory.FOOD, ModItems.STEAMED_GREEN_BEANS.get(), 0.35f, 100)
                .group("steamed_green_beans")
                .criterion("has_green_beans", conditionsFromItem(ModItems.GREEN_BEANS.get()))
                .offerTo(exporter, Identifier.of(AdorableHamsterPets.MOD_ID, "steamed_green_beans_from_smoking_green_beans"));

        // Smelting Acorns to Charcoal
        offerSmelting(exporter, List.of(ModItems.ACORN.get()), RecipeCategory.MISC, Items.CHARCOAL,
                0.15f, 200, "charcoal");

        // --- Shaped Crafting Recipes ---
        // Hamster Bed
        // On 1.20.1, pass the 'exporter' consumer directly
        offerHamsterBedRecipe(exporter, Items.OAK_PLANKS, WoodVariant.OAK);
        offerHamsterBedRecipe(exporter, Items.SPRUCE_PLANKS, WoodVariant.SPRUCE);
        offerHamsterBedRecipe(exporter, Items.BIRCH_PLANKS, WoodVariant.BIRCH);
        offerHamsterBedRecipe(exporter, Items.JUNGLE_PLANKS, WoodVariant.JUNGLE);
        offerHamsterBedRecipe(exporter, Items.ACACIA_PLANKS, WoodVariant.ACACIA);
        offerHamsterBedRecipe(exporter, Items.DARK_OAK_PLANKS, WoodVariant.DARK_OAK);
        offerHamsterBedRecipe(exporter, Items.MANGROVE_PLANKS, WoodVariant.MANGROVE);
        offerHamsterBedRecipe(exporter, Items.CHERRY_PLANKS, WoodVariant.CHERRY);
        offerHamsterBedRecipe(exporter, Items.BAMBOO_PLANKS, WoodVariant.BAMBOO);

        // Hamster Food Mix
        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.HAMSTER_FOOD_MIX.get(), 1)
                .pattern("SSS")
                .pattern("PCP")
                .pattern("WWW")
                .input('S', ModItems.SUNFLOWER_SEEDS.get())
                .input('P', Items.PUMPKIN_SEEDS)
                .input('C', Items.CARROT)
                .input('W', Items.WHEAT_SEEDS)
                .criterion("has_sunflower_seeds", conditionsFromItem(ModItems.SUNFLOWER_SEEDS.get()))
                .offerTo(exporter, Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_food_mix_from_ingredients"));

        // Hamster Bedding
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HAMSTER_BEDDING.get(), 2)
                .input(Items.OAK_LEAVES)
                .input(Items.BIRCH_LEAVES)
                .input(Items.DEAD_BUSH)
                .input(Items.PODZOL)
                .criterion("has_oak_leaves", conditionsFromItem(Items.OAK_LEAVES))
                .criterion("has_birch_leaves", conditionsFromItem(Items.BIRCH_LEAVES))
                .criterion("has_dead_bush", conditionsFromItem(Items.DEAD_BUSH))
                .criterion("has_podzol", conditionsFromItem(Items.PODZOL))
                .offerTo(exporter);

        // --- Shapeless Crafting Recipes ---
        // Sliced Cucumber
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.SLICED_CUCUMBER.get(), 3)
                .input(ModItems.CUCUMBER.get())
                .criterion("has_cucumber", conditionsFromItem(ModItems.CUCUMBER.get()))
                .offerTo(exporter);

        // Cheese
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.CHEESE.get(), 3)
                .input(Items.MILK_BUCKET)
                .criterion("has_milk_bucket", conditionsFromItem(Items.MILK_BUCKET))
                .offerTo(exporter);

        // Modded Sunflower to Vanilla Sunflower
        ShapelessRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, Items.SUNFLOWER, 1)
                .input(ModBlocks.SUNFLOWER_BLOCK.get())
                .criterion("has_modded_sunflower", conditionsFromItem(ModBlocks.SUNFLOWER_BLOCK.get()))
                .offerTo(exporter, Identifier.of(AdorableHamsterPets.MOD_ID, "vanilla_sunflower_from_modded"));

        // --- Acorn & Armor Recipes ---
        // Acorn Shard and Hat (Stonecutting)
        offerStonecuttingRecipe(exporter, RecipeCategory.MISC, ModItems.ACORN_SHARD.get(), ModItems.ACORN.get(), 2);
        offerStonecuttingRecipe(exporter, RecipeCategory.MISC, ModItems.ACORN_HAT.get(), ModItems.ACORN.get(), 1);

        // Acorn Armor (Shaped)
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.HAMSTER_ARMOR_ACORN.get(), 1)
                .pattern(" H ")
                .pattern("SSS")
                .pattern("SSS")
                .input('H', ModItems.ACORN_HAT.get())
                .input('S', ModItems.ACORN_SHARD.get())
                .criterion("has_acorn_hat", conditionsFromItem(ModItems.ACORN_HAT.get()))
                .criterion("has_acorn_shard", conditionsFromItem(ModItems.ACORN_SHARD.get()))
                .offerTo(exporter);

        // Smithing Upgrades
        offerHamsterArmorUpgrade(exporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get(), Items.IRON_INGOT, ModItems.HAMSTER_ARMOR_IRON.get());
        offerHamsterArmorUpgrade(exporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get(), Items.GOLD_INGOT, ModItems.HAMSTER_ARMOR_GOLD.get());
        offerHamsterArmorUpgrade(exporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get(), Items.DIAMOND, ModItems.HAMSTER_ARMOR_DIAMOND.get());
        offerHamsterArmorUpgrade(exporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get(), Items.NETHERITE_INGOT, ModItems.HAMSTER_ARMOR_NETHERITE.get());
    }
}