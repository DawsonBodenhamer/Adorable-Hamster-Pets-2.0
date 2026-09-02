package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Translatable.Name("Items & Diets")
@Translatable.Desc("Food values, item tags, armor stats, and more. Changes here dictate what items your hamsters interact with.")
public class AhpItemConfig extends Config {

    public AhpItemConfig() {
        super(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "items"));
    }

    @Override
    @NotNull
    public SaveType saveType() {
        return SaveType.SEPARATE;
    }

    // --- Core Item Tag Overrides ---
    @Translatable.Name("Core Item Tag Overrides")
    @Translatable.Desc("For the advanced user who looks at a perfectly functional system and thinks, 'I can make this weirder.' Edit these lists to change what items your hamsters consider food, bait, treasure, and all other interactions. Use item IDs (e.g., 'minecraft:diamond') or tags (e.g., '#minecraft:fishes'). Mess it up? That's a you problem.")
    public ConfigGroup itemTags = new ConfigGroup("itemTags", true);

    @Translatable.Name("Dancing Music Discs")
    @Translatable.Desc("A list of strings. If a jukebox plays a music disc containing any of these strings in its name, description, or tooltips (not case-sensitive), nearby hamsters will dance to it.")
    public List<String> dancingMusicDiscStrings = new ArrayList<>(List.of(
            "hamtaro", "hamster", "hampter"
    ));

    @Translatable.Name("Cheek Pouch Smuggling List")
    @Translatable.Desc("Fine-tune exactly what your hamster is (and isn't) allowed to carry. The 'Allowed' list acts as a high-priority override to the 'Disallowed' lists and general rules.")
    public ConfigGroup pouchRestrictions = new ConfigGroup("pouchRestrictions", true);

    @Translatable.Name("Allowed Items")
    @Translatable.Desc("A specific list of items and tags that are allowed in the hamster's cheek pouch. You can add things to this list to bypass the default 'no tools or big blocks' rule, since this overrides the 'disallowed' settings.")
    public List<String> pouchAllowedItems = new ArrayList<>(List.of(
            "minecraft:torch", "minecraft:soul_torch", "minecraft:redstone_torch", "minecraft:repeater", "minecraft:comparator", "minecraft:lever", "#minecraft:buttons", "#minecraft:pressure_plates", "minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "minecraft:melon_seeds", "minecraft:pitcher_pod", "minecraft:torchflower_seeds", "#c:seeds", "#forge:seeds"
    ));

    @Translatable.Name("Pouch Disallowed Items")
    @Translatable.Desc("A list of specific item IDs that are NEVER allowed in the cheek pouch, unless they are on the 'Allowed' list above. Mostly stuff that's too big, too pointy, or just plain illogical. Lol.")
    public List<String> pouchDisallowedItems = new ArrayList<>(List.of(
            "minecraft:bow", "minecraft:crossbow", "minecraft:trident", "minecraft:fishing_rod",
            "minecraft:shield", "minecraft:elytra", "minecraft:turtle_helmet", "minecraft:carved_pumpkin",
            "minecraft:player_head", "minecraft:zombie_head", "minecraft:skeleton_skull", "minecraft:wither_skeleton_skull", "minecraft:creeper_head", "minecraft:dragon_head", "minecraft:piglin_head",
            "minecraft:minecart", "minecraft:chest_minecart", "minecraft:furnace_minecart", "minecraft:tnt_minecart", "minecraft:hopper_minecart", "minecraft:command_block_minecart",
            "minecraft:saddle", "minecraft:bucket", "minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:milk_bucket", "minecraft:powder_snow_bucket",
            "minecraft:axolotl_bucket", "minecraft:tadpole_bucket", "minecraft:cod_bucket", "minecraft:pufferfish_bucket", "minecraft:salmon_bucket", "minecraft:tropical_fish_bucket",
            "minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:painting", "minecraft:armor_stand",
            "minecraft:end_crystal", "minecraft:spyglass", "minecraft:nether_star", "minecraft:dragon_egg", "minecraft:bundle"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Pouch Disallowed Tags")
    @Translatable.Desc("A list of item tags that are NEVER allowed in the cheek pouch, unless they are on the 'Allowed' list above. A broad-spectrum approach to preventing your hamster from swallowing an entire sword.")
    public List<String> pouchDisallowedTags = new ArrayList<>(List.of(
            "#minecraft:axes", "#minecraft:hoes", "#minecraft:pickaxes", "#minecraft:shovels", "#minecraft:swords",
            "#minecraft:trimmable_armor", "#minecraft:beds", "#minecraft:banners", "#minecraft:doors",
            "#minecraft:boats"
    ));

    @Translatable.Name("Taming Baits")
    @Translatable.Desc("The official list of bribes for convincing wild fluffballs to join your cause. By default, it's just sliced cucumbers. Feel free to add 'minecraft:nether_star' if you enjoy making poor life choices. Compatible with Cultural Delights by default!")
    public List<String> tamingFoods = new ArrayList<>(List.of("adorablehamsterpets:sliced_cucumber", "culturaldelights:cut_cucumber"));

    @Translatable.Name("Standard Diet")
    @Translatable.Desc("The hamster's everyday menu. These items will heal them or, if they're at full health, might give them... ideas about starting a family. Don't make it weird.")
    public List<String> standardDiet = new ArrayList<>(List.of(
            "#adorablehamsterpets:seeds", "adorablehamsterpets:hamster_food_mix", "adorablehamsterpets:green_beans",
            "adorablehamsterpets:cucumber",

            // Farmer's Delight
            "farmersdelight:cabbage_leaf",
            "farmersdelight:cooked_rice",
            "farmersdelight:pumpkin_slice",

            // Cultural Delights
            "culturaldelights:cut_cucumber", "culturaldelights:corn_kernels"
    ));

    @Translatable.Name("High-Value Heistables")
    @Translatable.Desc("The list of items a hamster might try to... 'borrow' if you leave them on the ground. A chase will ensue. You have been warned.")
    public List<String> stealableItems = new ArrayList<>(List.of("minecraft:diamond"));

    @Translatable.Name("Retrievable Items")
    @Translatable.Desc("Items the hamster views as gifts or toys to bring back to you. Picking these up triggers Delivery Mode. Default: Acorns.")
    public List<String> retrievableItems = new ArrayList<>(List.of("adorablehamsterpets:acorn"));

    @Translatable.Name("Stuffable Items")
    @Translatable.Desc("The list of dropped items a hamster may hunt down and stuff in its cheeks. Accepts item IDs (e.g. 'minecraft:wheat') or tags (e.g. '#adorablehamsterpets:seeds'). Note that this is my own custom union tag which points to Fabric's Convention tag on 1.21 like 'c:seeds' and Forge tags on 1.20 like 'forge:seeds.'")
    public List<String> snackableItems = new ArrayList<>(List.of(
            "#adorablehamsterpets:seeds", "#adorablehamsterpets:crop_items"
    ));

    @Translatable.Name("Stuffable Items Blacklist")
    @Translatable.Desc("Items or tags that hamsters are NOT allowed to hunt down and stuff in their cheeks. Overrides the 'Stuffable Items' list.")
    public List<String> snackableItemsBlacklist = new ArrayList<>();

    @Translatable.Name("Delicious Crop Blocks")
    @Translatable.Desc("The list of crop blocks a hamster may decide to harvest and replant. Accepts specific block IDs (e.g. 'minecraft:wheat') or tags (e.g. '#adorablehamsterpets:crops'). Note that this is my own custom union tag which points to Fabric's Convention tag on 1.21 like 'c:crops' and Forge tags on 1.20 like 'forge:crops.'")
    public List<String> snackableCrops = new ArrayList<>(List.of(
            "#adorablehamsterpets:crops"
    ));

    @Translatable.Name("Delicious Crop Blacklist")
    @Translatable.Desc("Crop blocks or tags that hamsters are NOT allowed to harvest. Overrides the 'Delicious Crop Blocks' list.")
    public List<String> snackableCropsBlacklist = new ArrayList<>();

    @Translatable.Name("Performance-Enhancers")
    @Translatable.Desc("The list of questionable substances that grant your hamster temporary superpowers. By default, it's just steamed green beans.")
    public List<String> buffFoods = new ArrayList<>(List.of("adorablehamsterpets:steamed_green_beans"));

    @Translatable.Name("Lure Items")
    @Translatable.Desc("The specific items that convince a tamed hamster your shoulder is the best seat in the house. Also acts as a bribe to lure them into their linked bed. Defaults to cheese, because of course it does.")
    public List<String> lureItems = new ArrayList<>(List.of("adorablehamsterpets:cheese"));

    @Translatable.Name("Rodent Repellent")
    @Translatable.Desc("The list of specific items that, when used on a Hamster Bed, will set 'Wander Mode Settings > Allow Sleeping in Bed' to false. For when you need your hamster to stay awake and wander around for... reasons. This can be reversed by using a lure item (cheese by default) on the bed. Sneaking before right-clicking with this item will unlink the bed entirely.")
    public List<String> bedAvoidanceFoods = new ArrayList<>(List.of("minecraft:rotten_flesh"));

    @Translatable.Name("Cheek Pouch Keys")
    @Translatable.Desc("The one-time offering required to earn a hamster's ultimate trust, unlocking their cheek inventory. Make it something special. Or don't. See if I care.")
    public List<String> pouchUnlockFoods = new ArrayList<>(List.of("adorablehamsterpets:hamster_food_mix"));

    @Translatable.Name("Picky Eater Solutions")
    @Translatable.Desc("Items on this list are so delicious, your hamster will never refuse them, even if you feed it to them twice. For the truly spoiled rodent.")
    public List<String> repeatableFoods = new ArrayList<>(List.of("adorablehamsterpets:hamster_food_mix", "adorablehamsterpets:steamed_green_beans"));

    @ConfigGroup.Pop
    @Translatable.Name("Passively Munchable Items")
    @Translatable.Desc("The specific items a hamster will eat directly from its cheek pouch to heal itself when injured. Keep it exclusive, or let them feast on enchanted apples. Your call.")
    public List<String> autoHealFoods = new ArrayList<>(List.of("adorablehamsterpets:hamster_food_mix"));

    // --- Food Settings ---
    @Translatable.Name("Food Settings")
    @Translatable.Desc("Nutrition— isn't it wonderful. Tweaks to snacks. Includes settings for hamster's and player's food.")
    public ConfigGroup foodHealing = new ConfigGroup("foodHealing", true);

    @Translatable.Name("Hamster Food Mix")
    @Translatable.Desc("Healing amount from Hamster Food Mix. The good stuff.")
    public ValidatedFloat hamsterFoodMixHealing = new ValidatedFloat(4.0f, 10.0f, 0.0f);

    @Translatable.Name("Standard Hamster Food")
    @Translatable.Desc("Healing from basic seeds/crops. Better than nothing… probably.")
    public ValidatedFloat standardFoodHealing = new ValidatedFloat(2.0f, 5.0f, 0.0f);

    @Translatable.Name("Disable Baby Food Refusal")
    @Translatable.Desc("If true, baby hamsters lose their refined palates and will eagerly gorge themselves on the same seeds repeatedly, in case you want to grow them up faster.")
    public boolean disableBabyFoodRefusal = false;

    // --- Nutrition and Saturation Settings ---
    @Translatable.Name("Nutrition and Saturation Settings")
    @Translatable.Desc("Some people think cheese is overpowered. Here's where you can flex your disagreement.")
    public ConfigGroup playerFood = new ConfigGroup("playerFood", true);

    @Translatable.Name("Cheese Nutrition")
    @Translatable.Desc("How many little hunger shanks the cheese restores. Vanilla cooked steak is 8. I know you're thinking of moving it to 20, you monster.")
    public ValidatedInt cheeseNutrition = new ValidatedInt(8, 20, 0);

    @Translatable.Name("Cheese Saturation")
    @Translatable.Desc("How long the hunger effect lasts. Cooked steak is 0.8. Don't get too crazy. Or do. I'm not your conscience.")
    public ValidatedFloat cheeseSaturation = new ValidatedFloat(0.8f, 2.0f, 0.0f);

    @Translatable.Name("Sliced Cucumber Nutrition")
    @Translatable.Desc("Vanilla dried kelp is 1 (0.5 hearts).")
    public ValidatedInt slicedCucumberNutrition = new ValidatedInt(1, 20, 0);

    @Translatable.Name("Sliced Cucumber Saturation")
    @Translatable.Desc("Dried kelp is 0.3.")
    public ValidatedFloat slicedCucumberSaturation = new ValidatedFloat(0.3f, 2.0f, 0.0f);

    @Translatable.Name("Cucumber Nutrition")
    public ValidatedInt cucumberNutrition = new ValidatedInt(2, 20, 0);

    @Translatable.Name("Cucumber Saturation")
    public ValidatedFloat cucumberSaturation = new ValidatedFloat(0.3f, 2.0f, 0.0f);

    @Translatable.Name("Green Beans Nutrition")
    public ValidatedInt greenBeansNutrition = new ValidatedInt(2, 20, 0);

    @Translatable.Name("Green Beans Saturation")
    public ValidatedFloat greenBeansSaturation = new ValidatedFloat(0.3f, 2.0f, 0.0f);

    @Translatable.Name("Steamed Green Beans Nutrition")
    public ValidatedInt steamedGreenBeansNutrition = new ValidatedInt(3, 20, 0);

    @Translatable.Name("Steamed Green Beans Saturation")
    public ValidatedFloat steamedGreenBeansSaturation = new ValidatedFloat(0.6f, 2.0f, 0.0f);

    @Translatable.Name("Hamster Food Mix Nutrition")
    public ValidatedInt hamsterFoodMixNutrition = new ValidatedInt(4, 20, 0);

    @ConfigGroup.Pop
    @Translatable.Name("Hamster Food Mix Saturation")
    public ValidatedFloat hamsterFoodMixSaturation = new ValidatedFloat(0.4f, 2.0f, 0.0f);

    // --- Green Bean Buff Settings ---
    @Translatable.Name("Green Bean Buff Settings")
    @Translatable.Desc("Nutrition, but make it dramatic. Tweaks to caffeine-bean highs for hamsters.")
    public ConfigGroup greenBeanBuffs = new ConfigGroup("greenBeanBuffs", true);

    @Translatable.Name("Duration")
    @Translatable.Desc("Steamed beans: power that fades faster than their attention span. (20 ticks = 1 second)")
    public ValidatedInt greenBeanBuffDuration = new ValidatedInt(3600, 20 * 60 * 10, 20);

    @Translatable.Name("Speed Level")
    @Translatable.Desc("Because someone gotta go fast.")
    public ValidatedInt greenBeanBuffAmplifierSpeed = new ValidatedInt(1, 4, 0);

    @Translatable.Name("Strength Level")
    @Translatable.Desc("Slightly mightier nibbles.")
    public ValidatedInt greenBeanBuffAmplifierStrength = new ValidatedInt(1, 4, 0);

    @Translatable.Name("Absorption Level")
    @Translatable.Desc("Extra fluff padding for those daring dives.")
    public ValidatedInt greenBeanBuffAmplifierAbsorption = new ValidatedInt(1, 4, 0);

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Regen Level")
    @Translatable.Desc("Heals minor paper-cuts (and fragile egos).")
    public ValidatedInt greenBeanBuffAmplifierRegen = new ValidatedInt(1, 4, 0);
}