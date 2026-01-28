package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Translation(prefix = "adorablehamsterpets.worldgen") // TODO: Remove @Translation annotation in Fzzy Config 0.7.4+ (See EnUsGenerator.java)
@Translatable.Name("World Gen & Loot")
@Translatable.Desc("Control the rodent population density, where their food grows, and what they scavenge for. Changes here usually require a restart, or at least a deep breath. Note: If you join a server, its world gen settings will override yours.")
public class AhpWorldGenConfig extends Config {

    public AhpWorldGenConfig() {
        super(Identifier.of(AdorableHamsterPets.MOD_ID, "worldgen"));
    }

    @Override
    @NotNull
    public SaveType saveType() {
        return SaveType.SEPARATE;
    }

    // --- Hamster Spawn Settings ---
    @Translatable.Name("Hamster Spawn Settings")
    @Translatable.Desc("How Many, Where, and How Often?  Note: Some of these settings require re-logging into your world to take effect.")
    public ConfigGroup hamsterSpawning = new ConfigGroup("hamsterSpawning", true);

    @Translatable.Name("Spawn Weight")
    @Translatable.Desc("Adjusts hamster spawn frequency. Higher = more chaos. 1 = blissful silence.")
    public ValidatedInt spawnWeight = new ValidatedInt(30, 100, 1);

    @Translatable.Name("Max Group Size")
    @Translatable.Desc("Maximum hamsters per spawn group. Because sometimes one just isn't cute enough.")
    public ValidatedInt maxGroupSize = new ValidatedInt(1, 10, 1);

    @Translatable.Name("Vanilla Biome Tags")
    @Translatable.Desc("A list of biome tags where hamsters can spawn. Format: 'mod_id:tag_name'. For example, 'minecraft:is_forest'.")
    public List<String> spawnBiomeTags = new ArrayList<>(List.of(
            "minecraft:is_beach",
            "minecraft:is_badlands",
            "minecraft:is_savanna",
            "minecraft:is_jungle",
            "minecraft:is_forest",
            "minecraft:is_taiga",
            "minecraft:is_mountain"
    ));

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("A list of convention biome tags where hamsters can spawn. Used for broad mod compatibility.")
    public List<String> spawnBiomeConventionTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_cold",
            "adorablehamsterpets:is_hot",
            "adorablehamsterpets:is_temperate",
            "adorablehamsterpets:is_dry",
            "adorablehamsterpets:is_wet",
            "adorablehamsterpets:is_dense_vegetation",
            "adorablehamsterpets:is_sparse_vegetation"
    ));

    @Translatable.Name("Include Specific Biomes")
    @Translatable.Desc("A list of specific biome IDs to ALWAYS allow spawns in, even if they don't match the tags above. Format: 'mod_id:biome_name'. For example, 'minecraft:plains'.")
    public List<String> includeBiomes = new ArrayList<>(List.of(
            // Specific Biomes from old isKeyInSpawnList
            "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:snowy_slopes",
            "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:grove",
            "minecraft:frozen_river", "minecraft:snowy_beach", "minecraft:frozen_ocean",
            "minecraft:deep_frozen_ocean", "minecraft:ice_spikes", "minecraft:cherry_grove",
            "minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark",
            "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:desert",
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
            "minecraft:old_growth_birch_forest", "minecraft:windswept_hills",
            "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
            "minecraft:windswept_savanna", "minecraft:stony_peaks", "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle", "minecraft:stony_shore", "minecraft:mushroom_fields",
            "minecraft:deep_dark", "minecraft:forest", "minecraft:birch_forest", "minecraft:dark_forest",
            "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
            "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:badlands",
            "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:beach",

            "terralith:desert_canyon", "terralith:cave/andesite_caves", "terralith:cave/crystal_caves", "terralith:cave/deep_caves", "terralith:cave/desert_caves", "terralith:cave/diorite_caves", "terralith:cave/frostfire_caves", "terralith:cave/fungal_caves", "terralith:cave/granite_caves", "terralith:cave/ice_caves", "terralith:cave/infested_caves", "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves", "terralith:cave/underground_jungle",
            "terralith:alpha_islands_winter", "terralith:alpha_islands", "terralith:alpine_grove", "terralith:alpine_highlands", "terralith:amethyst_canyon", "terralith:amethyst_rainforest", "terralith:ancient_sands", "terralith:arid_highlands", "terralith:ashen_savanna",
            "terralith:basalt_cliffs", "terralith:birch_taiga", "terralith:blooming_plateau", "terralith:blooming_valley", "terralith:brushland", "terralith:bryce_canyon", "terralith:caldera", "terralith:cloud_forest", "terralith:cold_shrubland",
            "terralith:desert_oasis", "terralith:desert_spires", "terralith:emerald_peaks", "terralith:forested_highlands", "terralith:fractured_savanna", "terralith:frozen_cliffs", "terralith:glacial_chasm", "terralith:granite_cliffs",
            "terralith:gravel_beach", "terralith:gravel_desert", "terralith:haze_mountain", "terralith:highlands", "terralith:hot_shrubland", "terralith:ice_marsh", "terralith:jungle_mountains",
            "terralith:lavender_forest", "terralith:lavender_valley", "terralith:lush_desert", "terralith:lush_valley", "terralith:mirage_isles", "terralith:moonlight_grove", "terralith:moonlight_valley", "terralith:mountain_steppe",
            "terralith:orchid_swamp", "terralith:painted_mountains", "terralith:red_oasis", "terralith:rocky_jungle", "terralith:rocky_mountains", "terralith:rocky_shrubland",
            "terralith:sakura_grove", "terralith:sakura_valley", "terralith:sandstone_valley", "terralith:savanna_badlands", "terralith:savanna_slopes", "terralith:scarlet_mountains",
            "terralith:shield_clearing", "terralith:shield", "terralith:shrubland", "terralith:siberian_grove", "terralith:siberian_taiga",
            "terralith:skylands_autumn", "terralith:skylands_spring", "terralith:skylands_summer", "terralith:skylands_winter", "terralith:skylands",
            "terralith:snowy_badlands", "terralith:snowy_cherry_grove", "terralith:snowy_maple_forest", "terralith:snowy_shield",
            "terralith:steppe", "terralith:stony_spires", "terralith:temperate_highlands", "terralith:tropical_jungle", "terralith:valley_clearing",
            "terralith:volcanic_crater", "terralith:volcanic_peaks", "terralith:warm_river", "terralith:warped_mesa", "terralith:white_cliffs", "terralith:white_mesa",
            "terralith:windswept_spires", "terralith:wintry_forest", "terralith:wintry_lowlands", "terralith:yellowstone", "terralith:yosemite_cliffs", "terralith:yosemite_lowlands",

            "biomesoplenty:wasteland", "biomesoplenty:wasteland_steppe",
            "biomesoplenty:mediterranean_forest", "biomesoplenty:mystic_grove", "biomesoplenty:orchard", "biomesoplenty:pumpkin_patch",
            "biomesoplenty:redwood_forest", "biomesoplenty:seasonal_forest", "biomesoplenty:woodland",
            "biomesoplenty:floodplain", "biomesoplenty:fungal_jungle", "biomesoplenty:rainforest", "biomesoplenty:rocky_rainforest",

            "byg:lush_stacks", "byg:orchard", "byg:frosted_coniferous_forest", "byg:allium_fields", "byg:amaranth_fields", "byg:rose_fields",
            "byg:temperate_grove", "byg:coconino_meadow", "byg:skyris_vale", "byg:prairie", "byg:autumnal_valley", "byg:cardinal_tundra", "byg:firecracker_shrubland",
            "byg:allium_shrubland", "byg:amaranth_grassland", "byg:araucaria_savanna", "byg:aspen_boreal", "byg:atacama_outback", "byg:baobab_savanna",
            "byg:basalt_barrera", "byg:bayou", "byg:black_forest", "byg:canadian_shield", "byg:cika_woods", "byg:coniferous_forest",
            "byg:crimson_tundra", "byg:cypress_swamplands", "byg:dacite_ridges", "byg:dacite_shore", "byg:dead_sea", "byg:ebony_woods",
            "byg:enchanted_tangle", "byg:eroded_borealis", "byg:firecracker_chaparral", "byg:forgotten_forest", "byg:fragment_jungle",
            "byg:frosted_taiga", "byg:howling_peaks", "byg:ironwood_gour", "byg:jacaranda_jungle", "byg:maple_taiga", "byg:mojave_desert",
            "byg:overgrowth_woodlands", "byg:pumpkin_valley", "byg:rainbow_beach", "byg:red_rock_valley", "byg:redwood_thicket",
            "byg:rugged_badlands", "byg:sakura_grove", "byg:shattered_glacier", "byg:sierra_badlands", "byg:skyrise_vale",
            "byg:tropical_rainforest", "byg:weeping_witch_forest", "byg:white_mangrove_marshes", "byg:windswept_desert", "byg:zelkova_forest"
    ));

    @Translatable.Name("Exclude Specific Biomes")
    @Translatable.Desc("A list of specific biome IDs to NEVER allow spawns in, even if they match a tag. This overrides all other settings. Format: 'mod_id:biome_name'. For example, 'minecraft:plains'.")
    public List<String> excludeBiomes = new ArrayList<>(List.of("mod_id:biome_name"));

    @Translatable.Name("Exclude Biome Tags")
    @Translatable.Desc("A list of Biome Tags to NEVER allow spawns in. For excluding broad categories like Oceans or Rivers without listing every single ID. Overrides inclusions.")
    public List<String> excludeBiomeTags = new ArrayList<>(List.of(
            "minecraft:is_ocean",
            "minecraft:is_river",
            "minecraft:is_deep_ocean"
    ));

    @Translatable.Name("Variant Spawning by Biome")
    @Translatable.Desc("For the aspiring digital zoologist. This is where you control exactly which hamster colors appear in which biomes. The system checks each color group below in order, from top to bottom (rarest to most common). The first base color that a biome qualifies for is the one that will spawn there. 'Why no settings for orange hamsters?' Because orange is the default fallback if no other rules match.")
    public ConfigGroup variantSpawning = new ConfigGroup("variantSpawning", true);

    @Translatable.Name("Priority 1: Blue Variants")
    @Translatable.Desc("The icy ones. Checked before all other colors. If a biome matches these rules, it will get blue hamsters, even if it also matches rules for other colors below.")
    public ConfigGroup blueVariant = new ConfigGroup("blueVariant", true);
    @Translatable.Name("Included Biomes")
    public List<String> blueBiomes = new ArrayList<>(List.of(
            "terralith:glacial_chasm", "terralith:mirage_isles", "terralith:moonlight_valley", "biomesoplenty:enchanted_garden"
    ));
    @Translatable.Name("Included Tags")
    public List<String> blueTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_icy"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> blueExclusionBiomes = new ArrayList<>(List.of(
            "namespace:id"
    ));
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> blueExclusionTags = new ArrayList<>(List.of(
            "c:tag_name"
    ));

    @Translatable.Name("Priority 2: Lavender Variants")
    @Translatable.Desc("The magical ones. Checked after Blue, but before all others.")
    public ConfigGroup lavenderVariant = new ConfigGroup("lavenderVariant", true);
    @Translatable.Name("Included Biomes")
    public List<String> lavenderBiomes = new ArrayList<>(List.of(
            "minecraft:cherry_grove", "terralith:sakura_valley", "biomesoplenty:fungi_forest", "biomesoplenty:mystic_grove"
    ));
    @Translatable.Name("Included Tags")
    public List<String> lavenderTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_magical", "adorablehamsterpets:is_mushroom", "terralith:mystical"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> lavenderExclusionBiomes = new ArrayList<>(List.of(
            "namespace:id"
    ));
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> lavenderExclusionTags = new ArrayList<>(List.of(
            "c:tag_name"
    ));

    @Translatable.Name("Priority 3: White Variants")
    @Translatable.Desc("The snowy ones. Checked after Blue and Lavender.")
    public ConfigGroup whiteVariant = new ConfigGroup("whiteVariant", true);
    @Translatable.Name("Included Biomes")
    public List<String> whiteBiomes = new ArrayList<>(List.of(
            "terralith:snowy_maple_forest", "terralith:wintry_forest", "terralith:alpine_grove", "terralith:siberian_grove"
    ));
    @Translatable.Name("Included Tags")
    public List<String> whiteTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_cold", "adorablehamsterpets:is_snowy"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> whiteExclusionBiomes = new ArrayList<>(List.of(
            "minecraft:deep_frozen_ocean", "minecraft:frozen_ocean", "minecraft:stony_shore", "minecraft:windswept_forest",
            "minecraft:windswept_gravelly_hills", "minecraft:windswept_hills", "minecraft:taiga",
            "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"
    ));
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> whiteExclusionTags = new ArrayList<>(List.of(
            "c:tag_name"
    ));

    @Translatable.Name("Priority 4: Gray Variants") // Includes both light and dark gray
    @Translatable.Desc("The rocky ones. For mountains, cliffs, and other places you're likely to twist an ankle.")
    public ConfigGroup grayVariant = new ConfigGroup("grayVariant", true);
    @Translatable.Name("Included Biomes")
    public List<String> grayBiomes = new ArrayList<>(List.of(
            "minecraft:stony_shore", "terralith:stony_spires"
    ));
    @Translatable.Name("Included Tags")
    public List<String> grayTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_mountain", "adorablehamsterpets:is_sparse_vegetation", "terralith:cliffs"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> grayExclusionBiomes = new ArrayList<>(List.of(
            "namespace:id"
    ));
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> grayExclusionTags = new ArrayList<>(List.of(
            "minecraft:is_badlands", "minecraft:is_jungle", "minecraft:is_savanna",
            "minecraft:is_beach", "adorablehamsterpets:is_sandy"
    ));

    @Translatable.Name("Priority 5: Black Variants")
    @Translatable.Desc("The damp ones. Found in swamps, caves, and other places that are probably bad for your sinuses.")
    public ConfigGroup blackVariant = new ConfigGroup("blackVariant", true);
    @Translatable.Name("Included Biomes")
    public List<String> blackBiomes = new ArrayList<>(List.of(
            "minecraft:deep_dark"
    ));
    @Translatable.Name("Included Tags")
    public List<String> blackTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_wet", "adorablehamsterpets:is_cave"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> blackExclusionBiomes = new ArrayList<>(List.of(
    ));
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> blackExclusionTags = new ArrayList<>(List.of(
            "minecraft:is_jungle", "minecraft:is_beach"
    ));

    @Translatable.Name("Priority 6: Cream Variants")
    @Translatable.Desc("The sandy ones. For deserts, beaches, and birch forests. Don't ask why birch forests. They just like them.")
    public ConfigGroup creamVariant = new ConfigGroup("creamVariant", true);
    @Translatable.Name("Included Biomes")
    public List<String> creamBiomes = new ArrayList<>(List.of(
            "minecraft:old_growth_birch_forest", "minecraft:birch_forest", "terralith:ancient_sands",
            "terralith:sandstone_valley", "biomesoplenty:wasteland"
    ));
    @Translatable.Name("Included Tags")
    public List<String> creamTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_sandy"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> creamExclusionBiomes = new ArrayList<>(List.of(
            "terralith:gravel_beach"
    ));
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> creamExclusionTags = new ArrayList<>(List.of(
            "minecraft:is_badlands"
    ));

    @Translatable.Name("Priority 7: Chocolate Variants")
    @Translatable.Desc("The forest dwellers. If it's a generic forest and doesn't fit any of the fancy categories above, you'll probably find these guys.")
    public ConfigGroup chocolateVariant = new ConfigGroup("chocolateVariant", true);

    @Translatable.Name("Included Biomes")
    public List<String> chocolateBiomes = new ArrayList<>(List.of(
            "terralith:cloud_forest", "biomesoplenty:redwood_forest"
    ));
    @Translatable.Name("Included Tags")
    public List<String> chocolateTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_forest", "adorablehamsterpets:is_dense_vegetation"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> chocolateExclusionBiomes = new ArrayList<>(List.of(
            "namespace:id"
    ));
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> chocolateExclusionTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_plains"
    ));

    // --- Cheek Pouch & World Loot Settings ---
    @Translatable.Name("Cheek Pouch & World Loot Settings")
    @Translatable.Desc("Control the economy of hamster-related trash. Adjust chest loot rarities and define exactly what wild hamsters are stuffing in their faces. Requires a world reload to apply.")
    public ConfigGroup lootSettings = new ConfigGroup("lootSettings", true);

    @Translatable.Name("Cheek Pouch Loot")
    @Translatable.Desc("Configure the contents and rarity of wild hamster cheek pouch loot. Requires a world reload to apply.")
    public ConfigGroup cheekPouchLoot = new ConfigGroup("cheekPouchLoot", true);

    @Translatable.Name("Global Loot Chance")
    @Translatable.Desc("The overall probability (0.0 to 1.0) that ANY wild hamster has stuffed something in its face. Set to 0.0 to enforce strict economic austerity.")
    public ValidatedFloat globalCheekLootChance = new ValidatedFloat(0.5f, 1.0f, 0.0f);

    @Translatable.Name("Default Loot")
    @Translatable.Desc("The curated selection of pocket lint, seeds, and snacks that hamsters naturally forage. Edit this if you think hamsters should naturally forage for diamonds.")
    public List<String> defaultCheekLootList = new ArrayList<>(List.of(
            "minecraft:wheat_seeds", "minecraft:pumpkin_seeds", "minecraft:melon_seeds",
            "minecraft:beetroot_seeds", "minecraft:carrot", "minecraft:potato", "minecraft:poisonous_potato",
            "minecraft:apple", "minecraft:stick", "minecraft:feather", "minecraft:string", "minecraft:gold_nugget",
            "minecraft:iron_nugget", "minecraft:flint", "adorablehamsterpets:sunflower_seeds",
            "adorablehamsterpets:cucumber_seeds", "adorablehamsterpets:green_bean_seeds", "adorablehamsterpets:acorn"
    ));

    @Translatable.Name("Default Loot Chance")
    @Translatable.Desc("The chance (0.0 to 1.0) that a wild hamster spawns with the default junk (seeds, nuggets, carrots, etc). 0.5 = 50%.")
    public ValidatedFloat defaultCheekLootChance = new ValidatedFloat(0.5f, 1.0f, 0.0f);

    @Translatable.Name("Extra Loot")
    @Translatable.Desc("A list of item IDs (e.g. 'minecraft:cookie') or tags (e.g. '#c:fruits') that wild hamsters can spawn with. If you want them to spawn with Nether Stars, this is where you make that bad decision.")
    public List<String> extraCheekLootList = new ArrayList<>();

    @Translatable.Name("Extra Loot Chance")
    @Translatable.Desc("The chance (0.0 to 1.0) that a wild hamster spawns with items from your Extra Loot list above. Independent of the default loot check.")
    public ValidatedFloat extraCheekLootChance = new ValidatedFloat(0.5f, 1.0f, 0.0f);

    @Translatable.Name("Cave Hamster Loot")
    @Translatable.Desc("The shiny rocks, glowing berries, and industrial debris found in the cheeks of cave-dwelling hamsters. Spelunking rodents have expensive tastes.")
    public List<String> caveCheekLootList = new ArrayList<>(List.of(
            "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:raw_gold",
            "minecraft:coal", "minecraft:amethyst_shard", "minecraft:glow_berries",
            "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:flint",
            "minecraft:clay_ball"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Cave Loot Chance")
    @Translatable.Desc("The chance (0.0 to 1.0) that a hamster spawning in a cave actually found something cool instead of just a rock. Applies ONLY to hamsters spawning in valid cave biomes or deep underground.")
    public ValidatedFloat caveCheekLootChance = new ValidatedFloat(0.5f, 1.0f, 0.0f);

    @Translatable.Name("Chest & World Loot")
    @Translatable.Desc("Control the economy of hamster-related trash. Adjust chest loot rarities and define exactly what wild hamsters are stuffing in their faces. Requires a world reload to apply.\n\nWondering where each loot type spawns? Check the mod page or the Hamster Tips guidebook.")
    public ConfigGroup worldLoot = new ConfigGroup("worldLoot", true);

    @Translatable.Name("Seed Loot Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) to find Cucumber, Green Bean, or Sunflower seeds in general loot chests.")
    public ValidatedFloat seedLootChance = new ValidatedFloat(0.60f, 1.0f, 0.0f);

    @Translatable.Name("Standard Armor Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) to find Acorn, Iron, or Gold hamster armor in standard Dungeons, Temples, Mineshafts, etc.")
    public ValidatedFloat standardArmorLootChance = new ValidatedFloat(0.30f, 1.0f, 0.0f);

    @Translatable.Name("High-Tier Armor Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) to find Diamond hamster armor in high-level chests (End City, Nether Fortress).")
    public ValidatedFloat highTierArmorLootChance = new ValidatedFloat(0.15f, 1.0f, 0.0f);

    @Translatable.Name("Allow Netherite Loot")
    @Translatable.Desc("Vanilla horse armor doesn't spawn in Netherite, but maybe you think that's stupid. Enable this to allow Netherite Hamster Armor to appear in chests.")
    public ValidatedBoolean enableNetheriteArmorLoot = new ValidatedBoolean(false);

    // Helper to gate the slider
    private final ValidatedField<Boolean> isNetheriteLootEnabled = enableNetheriteArmorLoot.map(b -> b, b -> b);

    @Translatable.Name("Netherite Loot Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) to find Netherite Hamster Armor in high-level chests. Only applies if the toggle above is ON. Don't be too liberal with it. Or do. See if I care.")
    public ValidatedCondition<Float> netheriteArmorLootChance = new ValidatedFloat(0.1f, 1.0f, 0.0f)
            .toCondition(
                    isNetheriteLootEnabled,
                    Text.translatable("config.adorablehamsterpets.condition.netherite_loot_enabled"),
                    () -> 0.0f
            );

    @Translatable.Name("Basic Template Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) to find Iron and Gold upgrade templates in High-Tier chests (Bastions, Fortresses, etc).")
    public ValidatedFloat basicSmithingTemplateLootChance = new ValidatedFloat(0.2f, 1.0f, 0.0f);

    @Translatable.Name("Advanced Template Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) to find Diamond and Netherite upgrade templates in Legendary chests (Ancient Cities, Ominous Vaults, etc).")
    public ValidatedFloat advancedSmithingTemplateLootChance = new ValidatedFloat(0.1f, 1.0f, 0.0f);

    @Translatable.Name("Accessory Loot Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) to find accessories in rare chests. Default is low.")
    public ValidatedFloat accessoryLootChance = new ValidatedFloat(0.05f, 1.0f, 0.0f);

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Oak Leaf Acorn Chance")
    @Translatable.Desc("Chance (0.0 to 1.0) for an Oak Leaf block to drop an Acorn when broken. Default is low, similar to Apples.")
    public ValidatedFloat oakLeavesAcornChance = new ValidatedFloat(0.05f, 1.0f, 0.0f);

    // --- Worldgen: Bush & Sunflower Stuff ---
    @Translatable.Name("Worldgen: Bush & Sunflower Stuff")
    @Translatable.Desc("For The Aspiring Landscape Artist. Note: Most of these settings require re-logging into your world to take effect, and it's unlikely you will see changes in chunks that have already been generated.")
    public ConfigGroup worldGenMisc = new ConfigGroup("worldGenMisc", true);

    @Translatable.Name("Wild Bush Regrowth Modifier")
    @Translatable.Desc("Higher = slower, lower = faster. Makes perfect sense.")
    public ValidatedDouble wildBushRegrowthModifier = new ValidatedDouble(1.0, 5.0, 0.1);

    // --- Sunflower Settings ---
    @Translatable.Name("Sunflower Settings")
    @Translatable.Desc("Custom sunflowers, because the vanilla ones just weren’t fabulous enough. Only changes fresh chunks.")
    public ConfigGroup sunflowerSettings = new ConfigGroup("sunflowerSettings", true);

    @Translatable.Name("Seed Regrowth Speed")
    @Translatable.Desc("Higher = slower, lower = faster. Photosynthesis is hard, okay?")
    public ValidatedDouble sunflowerRegrowthModifier = new ValidatedDouble(1.0, 5.0, 0.1);

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("A list of biome tags where these custom Sunflowers can replace vanilla ones. My custom union tag 'adorablehamsterpets:is_plains' points to 'c:is_plains', which provides wide compatibility with modded biomes.")
    public List<String> sunflowerBiomeTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_plains",
            "adorablehamsterpets:is_temperate",
            "adorablehamsterpets:is_hot",
            "adorablehamsterpets:is_dry"
    ));

    @Translatable.Name("Specific Biomes")
    @Translatable.Desc("Specific biome IDs where these sunflowers can replace the vanilla ones. Format: 'mod_id:biome_name'. They’re picky.")
    public List<String> sunflowerBiomes = new ArrayList<>(List.of("minecraft:sunflower_plains"));

    @Translatable.Name("Enable Glowing Sunflowers")
    @Translatable.Desc("Enables the 'CasualAnimalEnjoyer' Protocol. A small chance for a random nearby sunflower to become a bioluminescent beacon at night— a reference to the Kikoriki cartoon, 'It Can't Be True' episode, around the 3:22 mark. Added upon request as a thank-you to CasualAnimalEnjoyer for their massive help with mod compatibility.")
    public boolean enableGlowingSunflowers = true;

    @ConfigGroup.Pop
    @Translatable.Name("Glow Rarity")
    @Translatable.Desc("1 in X random ticks (20 ticks per second). Lower = more likely. Higher = more rare. Note: This only runs at night.")
    public ValidatedInt glowingSunflowerChance = new ValidatedInt(1000, 10000, 10);


    // --- Cucumber Bush Settings ---
    @Translatable.Name("Cucumber Bush Settings")
    @Translatable.Desc("Wild cucumbers, for when you need emergency salads in the savanna. Only changes fresh chunks.")
    public ConfigGroup cucumberBushSettings = new ConfigGroup("cucumberBushSettings", true);

    @Translatable.Name("Cucumber Bush Rarity")
    @Translatable.Desc("1 in X chunks. Lower numbers means cucumbers take over the planet.")
    public ValidatedInt wildCucumberBushRarity = new ValidatedInt(24, 100, 1);

    @Translatable.Name("Vanilla Biome Tags")
    @Translatable.Desc("Biome tags where cucumbers feel at home. Format: 'mod_id:tag_name', for example: 'minecraft:is_jungle'.")
    public List<String> cucumberBushTags = new ArrayList<>(List.of("minecraft:is_jungle"));

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("Convention tags for maximum mod-pack harmony. Format: 'namespace:tag_name', for example: 'adorablehamsterpets:is_temperate', which points to the 'c:is_temperate' convention tag.")
    public List<String> cucumberBushConventionTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_temperate",
            "adorablehamsterpets:is_hot",
            "adorablehamsterpets:is_dry"
    ));

    @Translatable.Name("Specific Biomes")
    @Translatable.Desc("Specific biome IDs where cucumbers can sprout. Format: 'mod_id:biome_name', for example: 'minecraft:savanna'.")
    public List<String> cucumberBushBiomes = new ArrayList<>(List.of(
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:savanna",
            "minecraft:savanna_plateau",
            "minecraft:forest",
            "minecraft:birch_forest",
            "minecraft:meadow",
            "minecraft:wooded_badlands",
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Specific Exclusions")
    @Translatable.Desc("Biomes where cucumbers are absolutely NOT allowed. Overrides everything else. Format: 'mod_id:biome_name', for example: 'minecraft:ocean'.")
    public List<String> cucumberBushExclusions = new ArrayList<>(List.of(
            "minecraft:swamp",
            "minecraft:mangrove_swamp",
            "minecraft:mushroom_fields",
            "minecraft:ocean",
            "minecraft:deep_ocean",
            "minecraft:warm_ocean",
            "minecraft:stony_peaks"
    ));

    // --- Green Bean Bush Settings ---
    @Translatable.Name("Green Bean Bush Settings")
    @Translatable.Desc("Legumes with attitude. Tuned for that perfect mid-game caffeine hit. Only changes fresh chunks.")
    public ConfigGroup greenBeanBushSettings = new ConfigGroup("greenBeanBushSettings", true);

    @Translatable.Name("Green Bean Bush Rarity")
    @Translatable.Desc("1 in X chunks. Lower = beanpocalypse. For those of you in the back, it means they'll spam everywhere.")
    public ValidatedInt wildGreenBeanBushRarity = new ValidatedInt(24, 100, 1);

    @Translatable.Name("Vanilla Biome Tags")
    @Translatable.Desc("Biome tags for bean growth. Empty by default—choose wisely. Format: 'mod_id:tag_name', for example: 'minecraft:is_jungle'.")
    public List<String> greenBeanBushTags = new ArrayList<>(List.of("mod_id:biome_name"));

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("Convention tags for mod-friendly bean spam. Format: 'namespace:tag_name', for example: 'adorablehamsterpets:is_wet', which points to the 'c:is_wet' convention tag.")
    public List<String> greenBeanBushConventionTags = new ArrayList<>(List.of(
            "adorablehamsterpets:is_wet",
            "adorablehamsterpets:is_temperate"
    ));

    @Translatable.Name("Specific Biomes")
    @Translatable.Desc("Specific biomes where beans sprout like gossip in chat. Format: 'mod_id:biome_name', for example: 'minecraft:swamp'.")
    public List<String> greenBeanBushBiomes = new ArrayList<>(List.of(
            "minecraft:swamp",
            "minecraft:mangrove_swamp",
            "minecraft:lush_caves",
            "minecraft:flower_forest"
    ));

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Specific Exclusions")
    @Translatable.Desc("Absolutely no beans here, thank you very much. Overrides all other settings. Format: 'mod_id:biome_name', for example: 'minecraft:beach'.")
    public List<String> greenBeanBushExclusions = new ArrayList<>(List.of(
            "minecraft:beach",
            "minecraft:birch_forest",
            "minecraft:cherry_grove",
            "minecraft:dark_forest",
            "minecraft:deep_ocean",
            "minecraft:dripstone_caves",
            "minecraft:forest",
            "minecraft:meadow",
            "minecraft:ocean",
            "minecraft:old_growth_birch_forest",
            "minecraft:plains",
            "minecraft:river",
            "minecraft:sunflower_plains"
    ));
}