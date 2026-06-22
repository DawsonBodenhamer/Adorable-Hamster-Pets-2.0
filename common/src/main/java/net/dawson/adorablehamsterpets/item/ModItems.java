package net.dawson.adorablehamsterpets.item;

import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.item.custom.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {

    // --- 1. Create a DeferredRegister for Items ---
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ITEM);

    // --- 2. Item Registrations ---

    // --- Core Items ---
    public static final RegistrySupplier<Item> HAMSTER_GUIDE_BOOK = registerItem("hamster_guide_book",
            () -> new PatchouliGuideBookItem(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> HAMSTER_SPAWN_EGG = registerItem("hamster_spawn_egg",
            () -> new SpawnEggItem(net.dawson.adorablehamsterpets.entity.ModEntities.HAMSTER.get(), 0x9c631f, 0xffffff, new Item.Settings()));

    // --- Crops & Food ---
    public static final RegistrySupplier<Item> GREEN_BEAN_SEEDS = registerItem("green_bean_seeds",
            () -> new AliasedBlockItem(ModBlocks.GREEN_BEANS_CROP.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER_SEEDS = registerItem("cucumber_seeds",
            () -> new AliasedBlockItem(ModBlocks.CUCUMBER_CROP.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_SEEDS = registerItem("sunflower_seeds",
            () -> new TallBlockItem(ModBlocks.SUNFLOWER_BLOCK.get(), new Item.Settings()) {
                @Override
                public String getTranslationKey() {
                    // Force the item to use its own ID instead of the block's ID so it displays the correct item name
                    return this.getOrCreateTranslationKey();
                }

                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER = registerItem("cucumber",
            () -> new ConfigurableFoodItem(new Item.Settings().food(ModFoodComponents.CUCUMBER),
                    Configs.AHP_ITEMS.cucumberNutrition, Configs.AHP_ITEMS.cucumberSaturation,
                    "tooltip.adorablehamsterpets.cucumber"));

    public static final RegistrySupplier<Item> SLICED_CUCUMBER = registerItem("sliced_cucumber",
            () -> new ConfigurableFoodItem(new Item.Settings().food(ModFoodComponents.SLICED_CUCUMBER),
                    Configs.AHP_ITEMS.slicedCucumberNutrition, Configs.AHP_ITEMS.slicedCucumberSaturation,
                    "tooltip.adorablehamsterpets.sliced_cucumber"));

    public static final RegistrySupplier<Item> GREEN_BEANS = registerItem("green_beans",
            () -> new ConfigurableFoodItem(new Item.Settings().food(ModFoodComponents.GREEN_BEANS),
                    Configs.AHP_ITEMS.greenBeansNutrition, Configs.AHP_ITEMS.greenBeansSaturation,
                    "tooltip.adorablehamsterpets.green_beans"));

    public static final RegistrySupplier<Item> STEAMED_GREEN_BEANS = registerItem("steamed_green_beans",
            () -> new ConfigurableFoodItem(new Item.Settings().food(ModFoodComponents.STEAMED_GREEN_BEANS),
                    Configs.AHP_ITEMS.steamedGreenBeansNutrition, Configs.AHP_ITEMS.steamedGreenBeansSaturation,
                    "tooltip.adorablehamsterpets.steamed_green_beans"));

    public static final RegistrySupplier<Item> HAMSTER_FOOD_MIX = registerItem("hamster_food_mix",
            () -> new ConfigurableFoodItem(new Item.Settings().food(ModFoodComponents.HAMSTER_FOOD_MIX).maxCount(16),
                    Configs.AHP_ITEMS.hamsterFoodMixNutrition, Configs.AHP_ITEMS.hamsterFoodMixSaturation,
                    "tooltip.adorablehamsterpets.hamster_food_mix"));

    public static final RegistrySupplier<Item> CHEESE = registerItem("cheese",
            () -> new CheeseItem(new Item.Settings().food(ModFoodComponents.CHEESE)));

    // --- Music Discs ---
    public static final RegistryKey<JukeboxSong> CHEESE_SONG_8_BIT_KEY = RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of(AdorableHamsterPets.MOD_ID, "ahp_theme_song_8_bit"));
    public static final RegistryKey<JukeboxSong> BLUE_CHEESE_SONG_LOW_FI_KEY = RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of(AdorableHamsterPets.MOD_ID, "ahp_theme_song_low_fi"));
    public static final RegistryKey<JukeboxSong> PARMESAN_SONG_ORCHESTRAL_KEY = RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of(AdorableHamsterPets.MOD_ID, "ahp_theme_song_orchestral"));

    public static final RegistrySupplier<Item> MUSIC_DISC_CHEESE = registerItem("music_disc_cheese",
            () -> new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE).jukeboxPlayable(CHEESE_SONG_8_BIT_KEY)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.music_disc_cheese.hint").formatted(Formatting.GOLD));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> MUSIC_DISC_BLUE_CHEESE = registerItem("music_disc_blue_cheese",
            () -> new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE).jukeboxPlayable(BLUE_CHEESE_SONG_LOW_FI_KEY)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.music_disc_blue_cheese.hint").formatted(Formatting.GOLD));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> MUSIC_DISC_PARMESAN = registerItem("music_disc_parmesan",
            () -> new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE).jukeboxPlayable(PARMESAN_SONG_ORCHESTRAL_KEY)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.music_disc_parmesan.hint").formatted(Formatting.GOLD));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    // --- Acorn & Resources ---
    public static final RegistrySupplier<Item> ACORN = registerItem("acorn",
            () -> new AliasedBlockItem(Blocks.OAK_SAPLING, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.acorn.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.acorn.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });
    public static final RegistrySupplier<Item> ACORN_HAT = registerItem("acorn_hat",
            () -> new Item(new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.acorn_hat.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.acorn_hat.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> ACORN_SHARD = registerItem("acorn_shard",
            () -> new Item(new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.acorn_shard.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.acorn_shard.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    // --- Hamster Armor ---
    public static final RegistrySupplier<Item> HAMSTER_ARMOR_ACORN = registerItem("hamster_armor_acorn",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.ACORN, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_acorn.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_acorn.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_IRON = registerItem("hamster_armor_iron",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.IRON, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_iron.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_iron.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_GOLD = registerItem("hamster_armor_gold",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.GOLD, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_gold.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_gold.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_DIAMOND = registerItem("hamster_armor_diamond",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.DIAMOND, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_diamond.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_diamond.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_NETHERITE = registerItem("hamster_armor_netherite",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.NETHERITE, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_netherite.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_armor_netherite.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });


    // --- Smithing Templates ---
    public static final RegistrySupplier<Item> HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_IRON = registerItem("hamster_armor_trim_smithing_template_iron",
            () -> createHamsterArmorTemplate("iron"));

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_GOLD = registerItem("hamster_armor_trim_smithing_template_gold",
            () -> createHamsterArmorTemplate("gold"));

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_DIAMOND = registerItem("hamster_armor_trim_smithing_template_diamond",
            () -> createHamsterArmorTemplate("diamond"));

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_TRIM_SMITHING_TEMPLATE_NETHERITE = registerItem("hamster_armor_trim_smithing_template_netherite",
            () -> createHamsterArmorTemplate("netherite"));


    // --- Block Item Registrations ---
    public static final RegistrySupplier<Item> WILD_GREEN_BEAN_BUSH_ITEM = registerBlockItem("wild_green_bean_bush",
            () -> new BlockItem(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> WILD_CUCUMBER_BUSH_ITEM = registerBlockItem("wild_cucumber_bush",
            () -> new BlockItem(ModBlocks.WILD_CUCUMBER_BUSH.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_BLOCK_ITEM = registerBlockItem("sunflower_block",
            () -> new BlockItem(ModBlocks.SUNFLOWER_BLOCK.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.sunflower_block.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.sunflower_block.hint2").formatted(Formatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_BEDDING = registerItem("hamster_bedding",
            () -> new HamsterBeddingItem(new Item.Settings()));

    // Hamster Bed
    public static final Map<WoodVariant, RegistrySupplier<Item>> HAMSTER_BED_ITEMS = new EnumMap<>(WoodVariant.class);
    static {
        for (WoodVariant variant : WoodVariant.values()) {
            HAMSTER_BED_ITEMS.put(variant, registerBlockItem("hamster_bed_" + variant.asString(),
                    () -> new HamsterBedItem(ModBlocks.HAMSTER_BED.get(), variant, new Item.Settings().maxCount(1))));
        }
    }

    public static final RegistrySupplier<Item> ACORN_CRATE = registerBlockItem("acorn_crate",
            () -> new BlockItem(ModBlocks.ACORN_CRATE.get(), new Item.Settings()));

    public static final RegistrySupplier<Item> CUCUMBER_CRATE = registerBlockItem("cucumber_crate",
            () -> new BlockItem(ModBlocks.CUCUMBER_CRATE.get(), new Item.Settings()));

    public static final RegistrySupplier<Item> GREEN_BEANS_CRATE = registerBlockItem("green_beans_crate",
            () -> new BlockItem(ModBlocks.GREEN_BEANS_CRATE.get(), new Item.Settings()));

    public static final RegistrySupplier<Item> HAMSTER_FOOD_MIX_CRATE = registerBlockItem("hamster_food_mix_crate",
            () -> new BlockItem(ModBlocks.HAMSTER_FOOD_MIX_CRATE.get(), new Item.Settings()));

    public static final RegistrySupplier<Item> UPSIDE_DOWN_HAMSTER_BED_ICON = registerItem("upside_down_hamster_bed_icon",
            () -> new HamsterBedItem(ModBlocks.HAMSTER_BED.get(), WoodVariant.OAK, new Item.Settings()));

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

    /**
     * Helper to create standard Hamster Armor Smithing Templates.
     * Uses vanilla assets for the empty slot icons to avoid needing new textures,
     * and anonymous subclass to inject the tooltip hints.
     *
     * @param materialName The name of the material (e.g., "iron").
     * @return A configured SmithingTemplateItem.
     */
    private static Item createHamsterArmorTemplate(String materialName) {
        return new SmithingTemplateItem(
                Text.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template.applies_to").formatted(Formatting.BLUE),                       // Applies to
                Text.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template." + materialName + ".ingredients").formatted(Formatting.BLUE), // Ingredients
                Text.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template_" + materialName + ".title").formatted(Formatting.GRAY),       // Title
                Text.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template.base_slot_description"),                                       // Base Slot Desc
                Text.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template.additions_slot_description"),                                  // Additions Slot Desc
                List.of(Identifier.of("minecraft", "item/empty_armor_slot_helmet")),                                                                 // Empty Base Slot Icon
                List.of(Identifier.of("minecraft", "item/empty_slot_ingot"))                                                                         // Empty Additions Slot Icon
        ) {
            @Override
            public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                // Vanilla SmithingTemplateItem adds its own tooltip info first.
                super.appendTooltip(stack, context, tooltip, type);

                if (Configs.AHP_UI.enableItemTooltips) {
                    tooltip.add(Text.empty()); // Spacer
                    // Use dynamic keys based on the material name (iron, gold, diamond, netherite)
                    tooltip.add(Text.translatable("tooltip.adorablehamsterpets.smithing_template." + materialName + ".hint1").formatted(Formatting.GOLD));
                    tooltip.add(Text.translatable("tooltip.adorablehamsterpets.smithing_template." + materialName + ".hint2").formatted(Formatting.GRAY));
                } else if (!Platform.isModLoaded("emi")) {
                    tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                }
            }
        };
    }

    // --- 4. Main registration call ---
    public static void register() {
        ITEMS.register();
    }
}