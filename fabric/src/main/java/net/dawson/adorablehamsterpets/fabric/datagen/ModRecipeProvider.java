package net.dawson.adorablehamsterpets.fabric.datagen;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

    // --- 1. Constructor ---
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    // --- 2. Helpers ---
    // For Hamster Bed variants
    private void offerHamsterBedRecipe(RecipeOutput exporter, Item planks, WoodVariant variant) {
        // Result is the specific item for this variant
        Item resultItem = ModItems.HAMSTER_BED_ITEMS.get(variant).get();

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultItem)
                .pattern(" H ")
                .pattern("HHH")
                .pattern("PPP")
                .define('H', ModItems.HAMSTER_BEDDING.get())
                .define('P', planks)
                .group("hamster_bed")
                .unlockedBy("has_hamster_bedding", has(ModItems.HAMSTER_BEDDING.get()))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_bed_" + variant.getSerializedName()));
    }

    // Helper for Smithing Upgrades
    private void offerHamsterArmorUpgrade(RecipeOutput exporter, Item template, Item material, Item result) {
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(template),
                        Ingredient.of(ModItemTagProvider.HAMSTER_ARMOR_ENCHANTABLE), // Allow any armor tier as base
                        Ingredient.of(material),
                        RecipeCategory.COMBAT,
                        result
                )
                .unlocks("has_acorn_armor", has(ModItems.HAMSTER_ARMOR_ACORN.get()))
                .unlocks("has_material", has(material))
                .save(exporter, getItemName(result) + "_smithing");
    }

    // Helper for Template Duplication
    private void offerHamsterTemplateDuplication(RecipeOutput exporter, Item template, Item material) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, template, 2)
                .pattern("STS")
                .pattern("SMS")
                .pattern("SSS")
                .define('T', template)
                .define('M', material)
                .define('S', ModItems.ACORN_SHARD.get())
                .unlockedBy("has_template", has(template))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, getItemName(template) + "_duplication"));
    }

    // --- 3. Public Methods ---
    @Override
    public void buildRecipes(RecipeOutput recipeExporter) {
        // --- Smelting Recipes ---
        // Smelting Green Beans to Steamed Green Beans
        oreSmelting(recipeExporter, List.of(ModItems.GREEN_BEANS.get()), RecipeCategory.FOOD, ModItems.STEAMED_GREEN_BEANS.get(),
                0.35f, 200, "steamed_green_beans");

        // Smoking Green Beans to Steamed Green Beans
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItems.GREEN_BEANS.get()), RecipeCategory.FOOD, ModItems.STEAMED_GREEN_BEANS.get(), 0.35f, 100)
                .group("steamed_green_beans")
                .unlockedBy("has_green_beans", has(ModItems.GREEN_BEANS.get()))
                .save(recipeExporter, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "steamed_green_beans_from_smoking_green_beans"));

        // Smelting Acorns to Charcoal
        oreSmelting(recipeExporter, List.of(ModItems.ACORN.get()), RecipeCategory.MISC, Items.CHARCOAL,
                0.15f, 200, "charcoal");

        // --- Shaped Crafting Recipes ---
        // Hamster Bed
        offerHamsterBedRecipe(recipeExporter, Items.OAK_PLANKS, WoodVariant.OAK);
        offerHamsterBedRecipe(recipeExporter, Items.SPRUCE_PLANKS, WoodVariant.SPRUCE);
        offerHamsterBedRecipe(recipeExporter, Items.BIRCH_PLANKS, WoodVariant.BIRCH);
        offerHamsterBedRecipe(recipeExporter, Items.JUNGLE_PLANKS, WoodVariant.JUNGLE);
        offerHamsterBedRecipe(recipeExporter, Items.ACACIA_PLANKS, WoodVariant.ACACIA);
        offerHamsterBedRecipe(recipeExporter, Items.DARK_OAK_PLANKS, WoodVariant.DARK_OAK);
        offerHamsterBedRecipe(recipeExporter, Items.MANGROVE_PLANKS, WoodVariant.MANGROVE);
        offerHamsterBedRecipe(recipeExporter, Items.CHERRY_PLANKS, WoodVariant.CHERRY);
        offerHamsterBedRecipe(recipeExporter, Items.BAMBOO_PLANKS, WoodVariant.BAMBOO);
        // offerHamsterBedRecipe(recipeExporter, Items.PALE_OAK_PLANKS, WoodVariant.PALE_OAK); // TODO: add pale oak when porting to 1.21.5

        // Hamster Food Mix
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.HAMSTER_FOOD_MIX.get(), 4)
                .pattern("SSS")
                .pattern("PCP")
                .pattern("WWW")
                .define('S', ModItems.SUNFLOWER_SEEDS.get())
                .define('P', Items.PUMPKIN_SEEDS)
                .define('C', Items.CARROT)
                .define('W', Items.WHEAT_SEEDS)
                .unlockedBy("has_sunflower_seeds", has(ModItems.SUNFLOWER_SEEDS.get()))
                .save(recipeExporter, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_food_mix_from_ingredients"));

        // Hamster Bedding
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.HAMSTER_BEDDING.get(), 2)
                .requires(Items.OAK_LEAVES)
                .requires(Items.BIRCH_LEAVES)
                .requires(Items.DEAD_BUSH)
                .requires(Items.PODZOL)
                .unlockedBy("has_oak_leaves", has(Items.OAK_LEAVES))
                .unlockedBy("has_birch_leaves", has(Items.BIRCH_LEAVES))
                .unlockedBy("has_dead_bush", has(Items.DEAD_BUSH))
                .unlockedBy("has_podzol", has(Items.PODZOL))
                .save(recipeExporter);

        // Restore Blue Cheese Disc to Regular
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MUSIC_DISC_CHEESE.get(), 1)
                .pattern("CCC")
                .pattern("CDC")
                .pattern("CCC")
                .define('C', ModItems.CHEESE.get())
                .define('D', ModItems.MUSIC_DISC_BLUE_CHEESE.get())
                .unlockedBy("has_blue_cheese_disc", has(ModItems.MUSIC_DISC_BLUE_CHEESE.get()))
                .save(recipeExporter, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "music_disc_cheese_from_blue_cheese"));

        // Restore Parmesan Disc to Regular
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MUSIC_DISC_CHEESE.get(), 1)
                .pattern("CCC")
                .pattern("CDC")
                .pattern("CCC")
                .define('C', ModItems.CHEESE.get())
                .define('D', ModItems.MUSIC_DISC_PARMESAN.get())
                .unlockedBy("has_parmesan_disc", has(ModItems.MUSIC_DISC_PARMESAN.get()))
                .save(recipeExporter, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "music_disc_cheese_from_parmesan"));

        // --- Shapeless Crafting Recipes ---
        // Sliced Cucumber
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.SLICED_CUCUMBER.get(), 3)
                .requires(ModItems.CUCUMBER.get())
                .unlockedBy("has_cucumber", has(ModItems.CUCUMBER.get()))
                .save(recipeExporter);

        // Cheese
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CHEESE.get(), 3)
                .requires(Items.MILK_BUCKET)
                .unlockedBy("has_milk_bucket", has(Items.MILK_BUCKET))
                .save(recipeExporter);

        // Modded Sunflower to Vanilla Sunflower
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, Items.SUNFLOWER, 1)
                .requires(ModBlocks.SUNFLOWER_BLOCK.get())
                .unlockedBy("has_modded_sunflower", has(ModBlocks.SUNFLOWER_BLOCK.get()))
                .save(recipeExporter, ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "vanilla_sunflower_from_modded"));

        // --- Acorn & Armor Recipes ---

        // Acorn Shard and Hat (Stonecutting)
        stonecutterResultFromBase(recipeExporter, RecipeCategory.MISC, ModItems.ACORN_SHARD.get(), ModItems.ACORN.get(), 2);
        stonecutterResultFromBase(recipeExporter, RecipeCategory.MISC, ModItems.ACORN_HAT.get(), ModItems.ACORN.get(), 1);

        // Acorn Armor (Shaped)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HAMSTER_ARMOR_ACORN.get(), 1)
                .pattern(" H ")
                .pattern("SSS")
                .pattern("SSS")
                .define('H', ModItems.ACORN_HAT.get())
                .define('S', ModItems.ACORN_SHARD.get())
                .unlockedBy("has_acorn_hat", has(ModItems.ACORN_HAT.get()))
                .unlockedBy("has_acorn_shard", has(ModItems.ACORN_SHARD.get()))
                .save(recipeExporter);

        // Acorn Ring
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ACORN_RING.get(), 2)
                .pattern(" C ")
                .pattern("CHC")
                .pattern(" C ")
                .define('C', Items.COPPER_INGOT)
                .define('H', ModItems.ACORN_HAT.get())
                .unlockedBy("has_acorn_hat", has(ModItems.ACORN_HAT.get()))
                .save(recipeExporter);

        // Smithing Upgrades
        offerHamsterArmorUpgrade(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get(), Items.IRON_INGOT, ModItems.HAMSTER_ARMOR_IRON.get());
        offerHamsterArmorUpgrade(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get(), Items.GOLD_INGOT, ModItems.HAMSTER_ARMOR_GOLD.get());
        offerHamsterArmorUpgrade(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get(), Items.DIAMOND, ModItems.HAMSTER_ARMOR_DIAMOND.get());
        offerHamsterArmorUpgrade(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get(), Items.NETHERITE_INGOT, ModItems.HAMSTER_ARMOR_NETHERITE.get());

        // Template Duplication
        offerHamsterTemplateDuplication(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON.get(), Items.IRON_INGOT);
        offerHamsterTemplateDuplication(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD.get(), Items.GOLD_INGOT);
        offerHamsterTemplateDuplication(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND.get(), Items.DIAMOND);
        offerHamsterTemplateDuplication(recipeExporter, ModItems.HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE.get(), Items.NETHERITE_INGOT);

        // --- Compacting Recipes (Crates) ---
        nineBlockStorageRecipes(recipeExporter, RecipeCategory.MISC, ModItems.ACORN.get(), RecipeCategory.BUILDING_BLOCKS, ModItems.ACORN_CRATE.get());
        nineBlockStorageRecipes(recipeExporter, RecipeCategory.FOOD, ModItems.CUCUMBER.get(), RecipeCategory.BUILDING_BLOCKS, ModItems.CUCUMBER_CRATE.get());
        nineBlockStorageRecipes(recipeExporter, RecipeCategory.FOOD, ModItems.GREEN_BEANS.get(), RecipeCategory.BUILDING_BLOCKS, ModItems.GREEN_BEANS_CRATE.get());
        nineBlockStorageRecipes(recipeExporter, RecipeCategory.FOOD, ModItems.HAMSTER_FOOD_MIX.get(), RecipeCategory.BUILDING_BLOCKS, ModItems.HAMSTER_FOOD_MIX_CRATE.get());
    }
}
