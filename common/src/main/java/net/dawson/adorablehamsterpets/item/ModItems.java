package net.dawson.adorablehamsterpets.item;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.client.item.TooltipContext;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.dawson.adorablehamsterpets.item.custom.HamsterBeddingItem;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ITEM);

    public static final RegistrySupplier<Item> HAMSTER_GUIDE_BOOK = registerItem("hamster_guide_book",
            () -> new net.dawson.adorablehamsterpets.item.custom.PatchouliGuideBookItem(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> HAMSTER_SPAWN_EGG = registerSpawnEgg();

    public static final RegistrySupplier<Item> GREEN_BEAN_SEEDS = registerItem("green_bean_seeds",
            () -> new AliasedBlockItem(ModBlocks.GREEN_BEANS_CROP.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER_SEEDS = registerItem("cucumber_seeds",
            () -> new AliasedBlockItem(ModBlocks.CUCUMBER_CROP.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_SEEDS = registerItem("sunflower_seeds",
            () -> new Item(new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER = registerItem("cucumber",
            () -> new Item(new Item.Settings().food(ModFoodComponents.CUCUMBER)) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> SLICED_CUCUMBER = registerItem("sliced_cucumber",
            () -> new Item(new Item.Settings().food(ModFoodComponents.SLICED_CUCUMBER)) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sliced_cucumber.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sliced_cucumber.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> GREEN_BEANS = registerItem("green_beans",
            () -> new Item(new Item.Settings().food(ModFoodComponents.GREEN_BEANS)) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_beans.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_beans.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> STEAMED_GREEN_BEANS = registerItem("steamed_green_beans",
            () -> new Item(new Item.Settings().food(ModFoodComponents.STEAMED_GREEN_BEANS)) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.steamed_green_beans.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.steamed_green_beans.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_FOOD_MIX = registerItem("hamster_food_mix",
            () -> new Item(new Item.Settings().food(ModFoodComponents.HAMSTER_FOOD_MIX).maxCount(16)) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_food_mix.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_food_mix.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> CHEESE = registerItem("cheese",
            () -> new net.dawson.adorablehamsterpets.item.custom.CheeseItem(new Item.Settings().food(ModFoodComponents.CHEESE)));

    public static final RegistrySupplier<Item> WILD_GREEN_BEAN_BUSH_ITEM = registerBlockItem("wild_green_bean_bush",
            () -> new BlockItem(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> WILD_CUCUMBER_BUSH_ITEM = registerBlockItem("wild_cucumber_bush",
            () -> new BlockItem(ModBlocks.WILD_CUCUMBER_BUSH.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_BLOCK_ITEM = registerBlockItem("sunflower_block",
            () -> new BlockItem(ModBlocks.SUNFLOWER_BLOCK.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.sunflower_block.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.sunflower_block.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, world, tooltip, context);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_BEDDING = registerItem("hamster_bedding",
            () -> new HamsterBeddingItem(new Item.Settings()));

    // Hamster Bed
    public static final Map<WoodVariant, RegistrySupplier<Item>> HAMSTER_BED_ITEMS = new EnumMap<>(WoodVariant.class);
    static {
        for (WoodVariant variant : WoodVariant.values()) {
            HAMSTER_BED_ITEMS.put(variant, registerBlockItem("hamster_bed_" + variant.asString(),
                    // On 1.20.1, use HamsterBedItem.create(...) instead of new HamsterBedItem(...)
                    () -> HamsterBedItem.create(ModBlocks.HAMSTER_BED.get(), variant, new Item.Settings())));
        }
    }

    public static final RegistrySupplier<Item> UPSIDE_DOWN_HAMSTER_BED_ICON = registerItem("upside_down_hamster_bed_icon",
            // On 1.20.1, use HamsterBedItem.create(...)
            () -> HamsterBedItem.create(ModBlocks.HAMSTER_BED.get(), WoodVariant.OAK, new Item.Settings()));

    // So Patchouli can display custom bell icon in its category list
    public static final RegistrySupplier<Item> ANNOUNCEMENT_BELL_ICON = registerItem("announcement_bell_icon",
            () -> new Item(new Item.Settings()));

    // --- 3. Helper methods for registration ---
    private static RegistrySupplier<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(Identifier.of(AdorableHamsterPets.MOD_ID, name), itemSupplier);
    }

    private static RegistrySupplier<Item> registerBlockItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }

    public static void register() {
        ITEMS.register();
    }

    @ExpectPlatform
    private static RegistrySupplier<Item> registerSpawnEgg() {
        throw new AssertionError();
    }
}