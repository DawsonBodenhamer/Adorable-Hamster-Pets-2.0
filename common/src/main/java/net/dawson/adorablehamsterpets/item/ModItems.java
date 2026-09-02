package net.dawson.adorablehamsterpets.item;

import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.item.custom.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {

    // --- 1. Create a DeferredRegister for Items ---
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, Registries.ITEM);

    // --- 2. Item Registrations ---

    // --- Core Items ---
    public static final RegistrySupplier<Item> HAMSTER_GUIDE_BOOK = registerItem("hamster_guide_book",
            () -> new PatchouliGuideBookItem(itemProps("hamster_guide_book", new Item.Properties().stacksTo(1))));

    public static final RegistrySupplier<Item> HAMSTER_SPAWN_EGG = registerItem("hamster_spawn_egg",
            () -> new SpawnEggItem(itemProps("hamster_spawn_egg", new Item.Properties().spawnEgg(net.dawson.adorablehamsterpets.entity.ModEntities.HAMSTER.get()))));

    // --- Crops & Food ---
    public static final RegistrySupplier<Item> GREEN_BEAN_SEEDS = registerItem("green_bean_seeds",
            () -> new BlockItem(ModBlocks.GREEN_BEANS_CROP.get(), itemProps("green_bean_seeds", new Item.Properties())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER_SEEDS = registerItem("cucumber_seeds",
            () -> new BlockItem(ModBlocks.CUCUMBER_CROP.get(), itemProps("cucumber_seeds", new Item.Properties())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_SEEDS = registerItem("sunflower_seeds",
            () -> new DoubleHighBlockItem(ModBlocks.SUNFLOWER_BLOCK.get(), itemProps("sunflower_seeds", new Item.Properties().useItemDescriptionPrefix())) {

                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER = registerItem("cucumber",
            () -> new ConfigurableFoodItem(itemProps("cucumber", new Item.Properties().food(ModFoodComponents.CUCUMBER)),
                    Configs.AHP_ITEMS.cucumberNutrition, Configs.AHP_ITEMS.cucumberSaturation,
                    "tooltip.adorablehamsterpets.cucumber"));

    public static final RegistrySupplier<Item> SLICED_CUCUMBER = registerItem("sliced_cucumber",
            () -> new ConfigurableFoodItem(itemProps("sliced_cucumber", new Item.Properties().food(ModFoodComponents.SLICED_CUCUMBER)),
                    Configs.AHP_ITEMS.slicedCucumberNutrition, Configs.AHP_ITEMS.slicedCucumberSaturation,
                    "tooltip.adorablehamsterpets.sliced_cucumber"));

    public static final RegistrySupplier<Item> GREEN_BEANS = registerItem("green_beans",
            () -> new ConfigurableFoodItem(itemProps("green_beans", new Item.Properties().food(ModFoodComponents.GREEN_BEANS)),
                    Configs.AHP_ITEMS.greenBeansNutrition, Configs.AHP_ITEMS.greenBeansSaturation,
                    "tooltip.adorablehamsterpets.green_beans"));

    public static final RegistrySupplier<Item> STEAMED_GREEN_BEANS = registerItem("steamed_green_beans",
            () -> new ConfigurableFoodItem(itemProps("steamed_green_beans", new Item.Properties().food(ModFoodComponents.STEAMED_GREEN_BEANS)),
                    Configs.AHP_ITEMS.steamedGreenBeansNutrition, Configs.AHP_ITEMS.steamedGreenBeansSaturation,
                    "tooltip.adorablehamsterpets.steamed_green_beans"));

    public static final RegistrySupplier<Item> HAMSTER_FOOD_MIX = registerItem("hamster_food_mix",
            () -> new ConfigurableFoodItem(itemProps("hamster_food_mix", new Item.Properties().food(ModFoodComponents.HAMSTER_FOOD_MIX).stacksTo(16)),
                    Configs.AHP_ITEMS.hamsterFoodMixNutrition, Configs.AHP_ITEMS.hamsterFoodMixSaturation,
                    "tooltip.adorablehamsterpets.hamster_food_mix"));

    public static final RegistrySupplier<Item> CHEESE = registerItem("cheese",
            () -> new CheeseItem(itemProps("cheese", new Item.Properties().food(ModFoodComponents.CHEESE, Consumable.builder().consumeSeconds(1.6F).animation(ItemUseAnimation.EAT).sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.CHEESE_EAT1.get())).hasConsumeParticles(true).build()))));

    // --- Music Discs ---
    public static final ResourceKey<JukeboxSong> CHEESE_SONG_8_BIT_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "ahp_theme_song_8_bit"));
    public static final ResourceKey<JukeboxSong> BLUE_CHEESE_SONG_LOW_FI_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "ahp_theme_song_low_fi"));
    public static final ResourceKey<JukeboxSong> PARMESAN_SONG_ORCHESTRAL_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "ahp_theme_song_orchestral"));

    public static final RegistrySupplier<Item> MUSIC_DISC_CHEESE = registerItem("music_disc_cheese",
            () -> new Item(itemProps("music_disc_cheese", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(CHEESE_SONG_8_BIT_KEY))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.music_disc_cheese.hint").withStyle(ChatFormatting.GOLD));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> MUSIC_DISC_BLUE_CHEESE = registerItem("music_disc_blue_cheese",
            () -> new Item(itemProps("music_disc_blue_cheese", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(BLUE_CHEESE_SONG_LOW_FI_KEY))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.music_disc_blue_cheese.hint").withStyle(ChatFormatting.GOLD));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> MUSIC_DISC_PARMESAN = registerItem("music_disc_parmesan",
            () -> new Item(itemProps("music_disc_parmesan", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(PARMESAN_SONG_ORCHESTRAL_KEY))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.music_disc_parmesan.hint").withStyle(ChatFormatting.GOLD));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    // --- Acorn & Resources ---
    public static final RegistrySupplier<Item> ACORN = registerItem("acorn",
            () -> new BlockItem(Blocks.OAK_SAPLING, itemProps("acorn", new Item.Properties())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });
    public static final RegistrySupplier<Item> ACORN_HAT = registerItem("acorn_hat",
            () -> new Item(itemProps("acorn_hat", new Item.Properties())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn_hat.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn_hat.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> ACORN_RING = registerItem("acorn_ring",
            () -> new Item(itemProps("acorn_ring", new Item.Properties().stacksTo(64))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn_ring.hint1")
                                .withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn_ring.hint2")
                                .withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> ACORN_SHARD = registerItem("acorn_shard",
            () -> new Item(itemProps("acorn_shard", new Item.Properties())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn_shard.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.acorn_shard.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    // --- Hamster Armor ---
    public static final RegistrySupplier<Item> HAMSTER_ARMOR_ACORN = registerItem("hamster_armor_acorn",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.ACORN, itemProps("hamster_armor_acorn", new Item.Properties().enchantable(HamsterArmorItem.HamsterArmorMaterial.ACORN.getEnchantability()))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_acorn.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_acorn.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_IRON = registerItem("hamster_armor_iron",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.IRON, itemProps("hamster_armor_iron", new Item.Properties().enchantable(HamsterArmorItem.HamsterArmorMaterial.IRON.getEnchantability()))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_iron.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_iron.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_GOLD = registerItem("hamster_armor_gold",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.GOLD, itemProps("hamster_armor_gold", new Item.Properties().enchantable(HamsterArmorItem.HamsterArmorMaterial.GOLD.getEnchantability()))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_gold.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_gold.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_DIAMOND = registerItem("hamster_armor_diamond",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.DIAMOND, itemProps("hamster_armor_diamond", new Item.Properties().enchantable(HamsterArmorItem.HamsterArmorMaterial.DIAMOND.getEnchantability()))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_diamond.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_diamond.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_ARMOR_NETHERITE = registerItem("hamster_armor_netherite",
            () -> new HamsterArmorItem(HamsterArmorItem.HamsterArmorMaterial.NETHERITE, itemProps("hamster_armor_netherite", new Item.Properties().enchantable(HamsterArmorItem.HamsterArmorMaterial.NETHERITE.getEnchantability()))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_netherite.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_armor_netherite.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
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
            () -> new BlockItem(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), itemProps("wild_green_bean_bush", new Item.Properties().useBlockDescriptionPrefix())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> WILD_CUCUMBER_BUSH_ITEM = registerBlockItem("wild_cucumber_bush",
            () -> new BlockItem(ModBlocks.WILD_CUCUMBER_BUSH.get(), itemProps("wild_cucumber_bush", new Item.Properties().useBlockDescriptionPrefix())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_BLOCK_ITEM = registerBlockItem("sunflower_block",
            () -> new BlockItem(ModBlocks.SUNFLOWER_BLOCK.get(), itemProps("sunflower_block", new Item.Properties().useBlockDescriptionPrefix())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                    if (Configs.AHP_UI.enableItemTooltips) {
                        tooltip.accept(Component.translatable("block.adorablehamsterpets.sunflower_block.hint1").withStyle(ChatFormatting.GOLD));
                        tooltip.accept(Component.translatable("block.adorablehamsterpets.sunflower_block.hint2").withStyle(ChatFormatting.GRAY));
                    } else if (!Platform.isModLoaded("emi")) {
                        tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                    }
                    super.appendHoverText(stack, context, display, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_BEDDING = registerItem("hamster_bedding",
            () -> new HamsterBeddingItem(itemProps("hamster_bedding", new Item.Properties())));

    // Hamster Bed
    public static final Map<WoodVariant, RegistrySupplier<Item>> HAMSTER_BED_ITEMS = new EnumMap<>(WoodVariant.class);
    static {
        for (WoodVariant variant : WoodVariant.values()) {
            HAMSTER_BED_ITEMS.put(variant, registerBlockItem("hamster_bed_" + variant.getSerializedName(),
                    () -> new HamsterBedItem(ModBlocks.HAMSTER_BED.get(), variant, itemProps("hamster_bed_" + variant.getSerializedName(), new Item.Properties().stacksTo(1).useItemDescriptionPrefix()))));
        }
    }

    public static final RegistrySupplier<Item> ACORN_CRATE = registerBlockItem("acorn_crate",
            () -> new BlockItem(ModBlocks.ACORN_CRATE.get(), itemProps("acorn_crate", new Item.Properties().useBlockDescriptionPrefix())));

    public static final RegistrySupplier<Item> CUCUMBER_CRATE = registerBlockItem("cucumber_crate",
            () -> new BlockItem(ModBlocks.CUCUMBER_CRATE.get(), itemProps("cucumber_crate", new Item.Properties().useBlockDescriptionPrefix())));

    public static final RegistrySupplier<Item> GREEN_BEANS_CRATE = registerBlockItem("green_beans_crate",
            () -> new BlockItem(ModBlocks.GREEN_BEANS_CRATE.get(), itemProps("green_beans_crate", new Item.Properties().useBlockDescriptionPrefix())));

    public static final RegistrySupplier<Item> HAMSTER_FOOD_MIX_CRATE = registerBlockItem("hamster_food_mix_crate",
            () -> new BlockItem(ModBlocks.HAMSTER_FOOD_MIX_CRATE.get(), itemProps("hamster_food_mix_crate", new Item.Properties().useBlockDescriptionPrefix())));

    public static final RegistrySupplier<Item> UPSIDE_DOWN_HAMSTER_BED_ICON = registerItem("upside_down_hamster_bed_icon",
            () -> new HamsterBedItem(ModBlocks.HAMSTER_BED.get(), WoodVariant.OAK, itemProps("upside_down_hamster_bed_icon", new Item.Properties().useItemDescriptionPrefix())));

    // So Patchouli can display custom bell icon in its category list
    public static final RegistrySupplier<Item> ANNOUNCEMENT_BELL_ICON = registerItem("announcement_bell_icon",
            () -> new Item(itemProps("announcement_bell_icon", new Item.Properties())));

    // --- 3. Helper methods for registration ---
    /** 26.2: items must know their registry key before construction. */
    private static Item.Properties itemProps(String name, Item.Properties properties) {
        return properties.setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, name)));
    }

    private static RegistrySupplier<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, name), itemSupplier);
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
                Component.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template.applies_to").withStyle(ChatFormatting.BLUE),                       // Applies to
                Component.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template." + materialName + ".ingredients").withStyle(ChatFormatting.BLUE), // Ingredients
                Component.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template.base_slot_description"),                                       // Base Slot Desc
                Component.translatable("item.adorablehamsterpets.hamster_armor_trim_smithing_template.additions_slot_description"),                                  // Additions Slot Desc
                List.of(Identifier.fromNamespaceAndPath("minecraft", "item/empty_armor_slot_helmet")),                                                                 // Empty Base Slot Icon
                List.of(Identifier.fromNamespaceAndPath("minecraft", "item/empty_slot_ingot")),
                itemProps("hamster_armor_trim_smithing_template_" + materialName, new Item.Properties())
        ) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
                // Vanilla SmithingTemplateItem adds its own tooltip info first.
                super.appendHoverText(stack, context, display, tooltip, type);

                if (Configs.AHP_UI.enableItemTooltips) {
                    tooltip.accept(Component.empty()); // Spacer
                    // Use dynamic keys based on the material name (iron, gold, diamond, netherite)
                    tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.smithing_template." + materialName + ".hint1").withStyle(ChatFormatting.GOLD));
                    tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.smithing_template." + materialName + ".hint2").withStyle(ChatFormatting.GRAY));
                } else if (!Platform.isModLoaded("emi")) {
                    tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                }
            }
        };
    }

    // --- 4. Main registration call ---
    public static void register() {
        ITEMS.register();
    }
}
