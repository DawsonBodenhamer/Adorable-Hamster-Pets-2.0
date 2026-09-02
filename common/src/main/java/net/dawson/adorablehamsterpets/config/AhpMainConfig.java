package net.dawson.adorablehamsterpets.config;

import dev.architectury.networking.NetworkManager;
import me.fzzyhmstrs.fzzy_config.annotations.NonSync;
import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigAction;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.networking.payload.ResetHeistHistoryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Translatable.Name("General Settings")
@Translatable.Desc("The command center for rodent-based chaos. Tweak physics, nerf cheese, and generally play god with small furry creatures.")
public class AhpMainConfig extends Config {

    public AhpMainConfig() {
        super(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "main"));
    }

    @Override
    @NotNull
    public SaveType saveType() {
        return SaveType.SEPARATE;
    }

    // --- Core Feature Toggles ---
    @Translatable.Name("Core Feature Toggles")
    @Translatable.Desc("Fundamental hamster hijinks— fiddle at your own risk.")
    public ConfigGroup core = new ConfigGroup("core", true);

    @Translatable.Name("Enable Redstone Fever")
    @Translatable.Desc("Controls whether Redstone Fever can appear. Turning this off also cures every infected hamster.")
    public boolean enableRedstoneFever = true;

    @Translatable.Name("Enable Breeding")
    @Translatable.Desc("Whether hamsters are allowed to multiply. Turn this off if you fear someone on your server plans to create a rodent horde.")
    public boolean enableBreeding = true;

    @Translatable.Name("Enable Teleport Rescue")
    @Translatable.Desc("If true, hamsters that are actively following you (not sitting or wandering) will instantly teleport with you, across dimensions and even if their current chunk becomes unloaded. WARNING: do not turn this off unless you want to risk your hamsters being left behind on long teleports.")
    public boolean enableTeleportRescue = true;

    @Translatable.Name("Prevent End Portal Travel")
    @Translatable.Desc("If true, hamsters that wander into the exit End Portal will bounce right back out instead of teleporting to the Overworld and getting lost. (Make sure 'Enable Teleport Rescue' is on so they can come with you when you go through).")
    public boolean preventHamsterEndPortalTravel = true;

    @Translatable.Name("Enable Hamster Throwing")
    @Translatable.Desc("Do we yeet the hamster? ('G' by default).")
    public boolean enableHamsterThrowing = true;

    @Translatable.Name("Enable Wander Mode")
    @Translatable.Desc("For when you need some personal space. Allows tamed hamsters to be linked to a Hamster Bed, letting them wander freely within a set radius instead of clinging to you like melted duct-tape. You're welcome.")
    public ValidatedBoolean enableWanderMode = new ValidatedBoolean(true);

    @Translatable.Name("Enable Crop Snacking")
    @Translatable.Desc("Whether wandering hamsters are allowed to snack on nearby crops. If enabled, they will occasionally pillage your fully grown crops, accidentally replant seeds, and stuff the profits into their face.")
    public boolean enableCropSnacking = true;

    @Translatable.Name("Enable Stealing/Fetching")
    @Translatable.Desc("Permits hamsters to engage in spontaneous, high-stakes games of keep-away with your valuables.")
    public boolean enableItemStealing = true;

    @Translatable.Name("Enable Hamster-vs-Player Tag")
    @Translatable.Desc("Allow hamsters to give in to their playful urges and help you recover from your chronic stoicism.")
    public boolean enableTagGame = true;

    @Translatable.Name("Enable Hamster-vs-Hamster Tag")
    @Translatable.Desc("Allow hamsters to start a game of tag with each other.")
    public boolean enableInterHamsterTag = true;

    @Translatable.Name("Enable Hide & Seek")
    @Translatable.Desc("Allow hamsters to spontaneously hide in bushes or containers.")
    public boolean enableHideAndSeek = true;

    @Translatable.Name("Enable Creeper Sniffing")
    @Translatable.Desc("May save your inventory. Or your ears. Allows hamsters to sniff for any aggressive creepers that have begun hunting you.")
    public boolean enableShoulderCreeperDetection = true;

    @Translatable.Name("Enable Diamond Sniffing")
    @Translatable.Desc("Because we all enjoy unsolicited financial advice from rodents. Allows hamsters to sniff for shinies from their shoulder perch.")
    public boolean enableShoulderDiamondDetection = true;

    @Translatable.Name("Enable Diamond Seeking")
    @Translatable.Desc("Permit hamsters to embark on solo get-rich-quick schemes?")
    public boolean enableIndependentDiamondSeeking = true;

    @Translatable.Name("Enable Armor Perks")
    @Translatable.Desc("If true, upgraded armor grants special perks. If false (because you hate fun?), armor acts only as a damage shield/visual. Each perk can also be individually configured in 'Armor Settings.'")
    public ValidatedBoolean enableArmorPerks = new ValidatedBoolean(true);

    // Helper predicate for the sliders in "Armor Perks"
    private final ValidatedField<Boolean> areArmorPerksEnabled = enableArmorPerks.map(b -> b, b -> b);

    @Translatable.Name("Disable Wild Loot Drops")
    @Translatable.Desc("If true, wild hamsters take their cheek-treasures to the grave. Prevents players from creating 'ethical' hamster recycling farms for seeds and nuggets.")
    public boolean disableWildLootDrops = false;

    @Translatable.Name("Inventory Access Starts Locked")
    @Translatable.Desc("Gate cheek-pouch storage behind gourmet cuisine, because drama.")
    public boolean requireFoodMixToUnlockCheeks = true;

    @Translatable.Name("Use 'Hampter' as Default Name")
    @Translatable.Desc("Changes the default entity name from 'Hamster' to 'Hampter'. Note: This has no visible effect in vanilla Minecraft, as mobs don't show nameplates by default. It's primarily for use with mods like Jade or Auto Leveling that display entity names.")
    public boolean useHampterName = false;

    @Translatable.Name("Enable Petting")
    @Translatable.Desc("If true, looking affectionately at your hamster might result in spontaneous petting. Also enables the Pet Hamster keybind.")
    public boolean enablePetting = true;

    @Translatable.Name("Enable GUI Renaming")
    @Translatable.Desc("Lets you rename hamsters directly from their inventory screen. Much more civilized than slapping them with a name tag.")
    public boolean enableGuiRenaming = true;

    @Translatable.Name("Allow Taming to Re-Enable AI")
    @Translatable.Desc("If true, players can tame frozen, AI-disabled hamsters, instantly breathing life into them like some sort of furry necromancer. Turn this off if you're using command-spawned hamsters as statues or shop displays and don't want your patrons walking off with the merchandise happily following.")
    public boolean allowTamingAiDisabled = true;

    @Translatable.Name("Prevent Owner Friendly Fire")
    @Translatable.Desc("If true, your weapons will magically pass through your own tamed hamsters. Perfect for those with butter fingers who keep 'accidentally' sending their companions to the great hamster wheel in the sky.")
    public boolean preventOwnerFriendlyFire = false;

    @Translatable.Name("Mob Interactions")
    @Translatable.Desc("Configure how hamsters interact with (or terrify) other creatures.")
    public ConfigGroup mobInteractions = new ConfigGroup("mobInteractions", true);

    @Translatable.Name("Frighten Ravagers")
    @Translatable.Desc("Should Ravagers, the hulking beasts of destruction, flee in terror from a tiny ball of fluff? Yes. Yes, they should.")
    public boolean enableRavagerFlee = true;

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Frighten Spiders")
    @Translatable.Desc("Turns your hamster into a mobile arachnid-repellent unit. Highly effective.")
    public boolean enableSpiderFlee = true;

    // --- Core Cooldown Settings ---
    @Translatable.Name("Core Cooldown Settings")
    @Translatable.Desc("Mandatory hamster union breaks between heroic stunts.")
    public ConfigGroup cooldowns = new ConfigGroup("cooldowns", true);

    @Translatable.Name("Throw Cooldown")
    @Translatable.Desc("Time-out after using your living projectile. (20 ticks = 1 second)")
    public ValidatedInt hamsterThrowCooldown = new ValidatedInt(2400, 20 * 60 * 10, 20);

    @Translatable.Name("Green Bean Buff Cooldown")
    @Translatable.Desc("When the sugar rush ends, force a breather. (20 ticks = 1 second)")
    public ValidatedInt steamedGreenBeansBuffCooldown = new ValidatedInt(6000, 20 * 60 * 10, 20);

    @Translatable.Name("Diamond Seeking Cooldown")
    @Translatable.Desc("Force a cool-down after striking it rich. Off by default, since this can't happen again anyway without another mount/dismount on the shoulder.")
    public boolean enableIndependentDiamondSeekCooldown = false;

    @Translatable.Name("Crop-Snacking Cooldown")
    @Translatable.Desc("How long a hamster must wait after a successfully de-foresting your garden before it decides to have another midnight snack. (20 ticks = 1 second, default = 3 minutes).")
    public ValidatedInt cropSnackCooldownTicks = new ValidatedInt(3600, 24000, 20);

    @Translatable.Name("Diamond Seeking Cooldown")
    @Translatable.Desc("Cooldown before your hamster can go on another treasure hunt. (20 ticks = 1 second)")
    public ValidatedInt independentOreSeekCooldownTicks = new ValidatedInt(2400, 6000, 20);

    @Translatable.Name("Item Thievery Cooldown")
    @Translatable.Desc("Mandatory time-out after a successful heist to prevent serial kleptomania. (20 ticks = 1 second). WARNING: Increasing this cooldown can dramatically change the item stealing mechanic, since that AI goal sometimes re-runs multiple times in a row when the hamster has trouble pathfinding to the item that it wants to steal. So instead of increasing this, you should probably just stop dropping your diamonds on the ground everywhere, butter fingers.")
    public ValidatedInt stealCooldownTicks = new ValidatedInt(100, 6000, 20);

    @Translatable.Name("Hamster-vs-Player Tag")
    @Translatable.Desc("How long (in ticks) a specific hamster needs to recover after being chased by you. Remember, they have tiny lungs. (20 ticks = 1 second; default = 10 minutes)")
    public ValidatedInt hamsterVersusPlayerTagCooldown = new ValidatedInt(12000, 36000, 160);

    @Translatable.Name("Hamster-vs-Hamster Tag")
    @Translatable.Desc("The average time (in seconds) between hamster-vs-hamster tag games. If you set this lower than the actual duration of a game (15 seconds by default), you will allow multiple games of tag to occur simultaneously within the same group of hamsters.")
    public ValidatedInt interHamsterTagAverageSeconds = new ValidatedInt(60, 3600, 5);

    @Translatable.Name("Hide & Seek Cooldown")
    @Translatable.Desc("How long (in seconds) a hamster waits before trying to hide again. Default = 10 minutes.")
    public ValidatedInt hideAndSeekCooldownSeconds = new ValidatedInt(600, 3600, 1);

    @Translatable.Name("Auto-Petting Rarity")
    @Translatable.Desc("The 1-in-X chance per tick (20 ticks per second) to initiate petting when sneaking and looking directly at your hamster. The hamster must be either standing or sitting, and not involved in any other activity. The default is 500. This means on average, it will require about ~15 seconds of uninterrupted staring to trigger, so it's somewhat rare. If you're impatient, it also comes with a keybind to trigger it manually. You don't need to be sneaking to use the keybind.")
    public ValidatedInt pettingChanceDenominator = new ValidatedInt(500, 3000, 20);

    @ConfigGroup.Pop
    @Translatable.Name("Breeding Cooldown (Seconds)")
    @Translatable.Desc("Hamsters need their space. Here's where you give them a break between litters.")
    public ValidatedInt breedingCooldownSeconds = new ValidatedInt(300, 1200, 1);

    // --- Core Hamster Attributes ---
    @Translatable.Name("Core Hamster Attributes")
    @Translatable.Desc("All the knobs and dials that make your hamster the majestic (or chaotic) creature it is.")
    public ConfigGroup hamsterAttributes = new ConfigGroup("hamsterAttributes", true);

    @Translatable.Name("Max Health (Wild)")
    @Translatable.Desc("How much abuse a wild hamster can take before it gives up the ghost. Vanilla animals are around 8-10. Set it to 200 (100 hearts) if you enjoy a challenge, or 1 if you're a monster.")
    public ValidatedDouble wildMaxHealth = new ValidatedDouble(8.0, 200.0, 1.0);

    @Translatable.Name("Max Health (Tamed)")
    @Translatable.Desc("How beefy your tamed fuzzball is. Defaults to double its wild health, because love makes you stronger. Or something. Vanilla wolves have 20 (10 hearts).")
    public ValidatedDouble tamedMaxHealth = new ValidatedDouble(16.0, 200.0, 1.0);

    @Translatable.Name("Wander Interval")
    @Translatable.Desc("Controls how frequently your hamster feels the urge to walk around. The higher the number, the lazier the hamster (1 in X chance to wander per tick; 20 ticks = 1 second). Set to 0 to disable wandering entirely— perfect for keeping them still for photoshoots or interrogation. Vanilla's default chance for Wolves is 1 in 120 ticks.")
    public ValidatedInt wanderChanceInterval = new ValidatedInt(120, 10000, 0);

    @Translatable.Name("Look-At Duration")
    @Translatable.Desc("The minimum time (in ticks) your hamster stares into your soul. Actual duration = this value + a random extra 0 to 4 seconds. Default is 40. Increase to simulate deep contemplation (or emptiness).")
    public ValidatedInt lookAtDuration = new ValidatedInt(20, 600, 20);

    @Translatable.Name("Taming Chance")
    @Translatable.Desc("Convince a hamster to love you. Taming difficulty (1 in X chance). Higher = more cucumbers sacrificed to fuzzy freeloaders.")
    public ValidatedInt tamingChanceDenominator = new ValidatedInt(3, 20, 1);

    @Translatable.Name("Melee Damage")
    @Translatable.Desc("Tamed hamster melee damage. Squeak-first, ask questions later.")
    public ValidatedDouble meleeDamage = new ValidatedDouble(2.0, 40.0, 0.0);

    @Translatable.Name("Throw Damage")
    @Translatable.Desc("Damage dealt by thrown hamster. Surprisingly effective against Creepers. How convenient.")
    public ValidatedDouble hamsterThrowDamage = new ValidatedDouble(20.0, 40.0, 0.0);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Enable Red Eyes")
    @Translatable.Desc("Toggle this off if you find the rare, recessive red-eyed hamsters to be a bit too... scary. Reverts them to standard black, but only for you. They will still appear red for everyone else.")
    public boolean enableRedEyes = true;

    // --- Aggression States & Diets ---
    @Translatable.Name("Aggression States & Diets")
    @Translatable.Desc("Configure what foods turns your adorable pet into a pacifist, a standard follower, or a bloodthirsty menace.")
    public ConfigGroup aggressionSettings = new ConfigGroup("aggressionSettings", true);

    @Translatable.Name("Acorn Ring Settings")
    @Translatable.Desc("Configure who and what the Acorn Ring protects.")
    public ConfigGroup acornRingSettings = new ConfigGroup("acornRingSettings", true);

    @Translatable.Name("Only Protects Hamsters")
    @Translatable.Desc("If true, Acorn Ring contracts only cover hamsters. If false, they cover all conventionally owned pets.")
    public boolean acornRingOnlyProtectsHamsters = false;

    @Translatable.Name("Prevents Harming Your Pets")
    @Translatable.Desc("If true, wearing an Acorn Ring prevents your direct attacks from damaging pets you own.")
    public boolean acornRingPreventsDamageToOwnPets = true;

    @Translatable.Name("Prevents Harming Other Pets")
    @Translatable.Desc("If true, wearing an Acorn Ring prevents your direct attacks from damaging pets owned by another ring wearer.")
    public boolean acornRingPreventsDamageToOtherPets = false;

    @ConfigGroup.Pop
    @Translatable.Name("Pacifist Break on Attack")
    @Translatable.Desc("If true, a Passive hamster will automatically revert to Neutral if it sees its owner attacking something.")
    public boolean pacifistBreakOnOwnerAttack = false;

    @Translatable.Name("Pacifist Snacks")
    @Translatable.Desc("Items that turn the hamster into a total hippie. They will refuse to attack anything, even to defend you.")
    public List<String> becomePacifistItems = new ArrayList<>(List.of("#minecraft:flowers"));

    @Translatable.Name("Amnesia Snacks")
    @Translatable.Desc("Items that factory-reset the hamster to normal wolf-like neutral behavior (defends you, attacks what you attack).")
    public List<String> becomeNeutralItems = new ArrayList<>(List.of("adorablehamsterpets:sunflower_seeds"));

    @Translatable.Name("Menace Snacks")
    @Translatable.Desc("Items that unleash the hamster's inner demon. They will actively hunt down anything in the Menace Targets list.")
    public List<String> becomeMenaceItems = new ArrayList<>(List.of("minecraft:spider_eye"));

    @ConfigGroup.Pop
    @Translatable.Name("Menace Targets")
    @Translatable.Desc("The hit list. What should the hamster hunt when in Menace mode? Add 'minecraft:cow' if you want a tiny slaughterhouse. Accepts Entity IDs or Tags. NOTE: The default '#adorablehamsterpets:monsters' tag is a custom tag that targets all hostile mobs.")
    public List<String> menaceTargetEntities = new ArrayList<>(List.of("#adorablehamsterpets:monsters", "#adorablehamsterpets:bosses", "minecraft:slime", "minecraft:magma_cube"));

    // --- Redstone Fever ---
    @Translatable.Name("Redstone Fever Settings")
    @Translatable.Desc("Control how Redstone Fever behaves, from sunlight treatment to who infected hamsters attack and how often they burst with energy.")
    public ConfigGroup redstoneFever = new ConfigGroup("redstoneFever", true);

    @Translatable.Name("Enable Sunlight Curing")
    @Translatable.Desc("Allows infected hamsters to recover by spending enough time in direct sunlight above the depths where Redstone Fever naturally appears.")
    public boolean enableRedstoneFeverSunlightCuring = true;

    @Translatable.Name("Required Curing Days")
    @Translatable.Desc("Number of Minecraft days an infected hamster must spend in direct sunlight to be cured. Progress pauses underground, in the shade and at night.")
    public ValidatedInt redstoneFeverSunlightCureDays = new ValidatedInt(3, 30, 1);

    @Translatable.Name("Aggression Range")
    @Translatable.Desc("Maximum range in blocks at which an infected hamster can acquire a target.")
    public ValidatedInt redstoneFeverTargetingRange = new ValidatedInt(16, 64, 1);

    @Translatable.Name("Enable Global Aggression")
    @Translatable.Desc("Allows infected hamsters to attack nearly all types of nearby living entities. Accessible Survival and Adventure players remain first on the hit list.")
    public boolean redstoneFeverAttackMostLivingMobs = true;

    @Translatable.Name("Enable Energy Bursts")
    @Translatable.Desc("Allows infected hamsters to occasionally race in tight circles. Perfectly normal medical behavior.")
    public boolean enableRedstoneFeverEnergyBursts = true;

    @Translatable.Name("Minimum Burst Interval")
    @Translatable.Desc("Shortest possible wait (in seconds) before another energy burst begins.")
    public ValidatedInt redstoneFeverMinBurstIntervalSeconds = new ValidatedInt(2, 300, 1);

    @Translatable.Name("Maximum Burst Interval")
    @Translatable.Desc("Longest possible wait (in seconds) before another energy burst begins.")
    public ValidatedInt redstoneFeverMaxBurstIntervalSeconds = new ValidatedInt(5, 300, 1);

    @Translatable.Name("Minimum Burst Duration")
    @Translatable.Desc("Shortest possible duration of an energy burst (in seconds).")
    public ValidatedInt redstoneFeverMinBurstDurationSeconds = new ValidatedInt(1, 30, 1);

    @ConfigGroup.Pop
    @Translatable.Name("Maximum Burst Duration")
    @Translatable.Desc("Longest possible duration of an energy burst (in seconds).")
    public ValidatedInt redstoneFeverMaxBurstDurationSeconds = new ValidatedInt(2, 30, 1);

    // --- Breeding & Litter Size ---
    @Translatable.Name("Breeding & Litter Size")
    @Translatable.Desc("Control the rate of hamster reproduction. Genetics are fun, but we don't want your server to start begging for mercy. Note: the global 'Enable Breeding' toggle is in the 'Core Feature Toggles' section.")
    public ConfigGroup breedingSettings = new ConfigGroup("breedingSettings", true);

    @Translatable.Name("Force Breeding Overlay")
    @Translatable.Desc("If true, all baby hamsters are guaranteed to receive a breeding overlay, regardless of whether their parents had them. Reduces the total number of possible outcomes.")
    public boolean forceBreedingOverlay = false;

    @Translatable.Name("Genetic Variance")
    @Translatable.Desc("How much the baby's base color tends to deviate from the exact mathematical center between its parents. Higher values mean the baby could look much more like Parent A or Parent B, rather than a perfect mix.")
    public ValidatedDouble geneticVariance = new ValidatedDouble(0.1, 1.0, 0.0);

    @Translatable.Name("Genetic Mutation Rate")
    @Translatable.Desc("How much random 'scatter' is added to the baby's color across all dimensions (Hue, Saturation, Brightness). Higher values mean the baby could end up a completely unexpected color instead of a mixture between the two parents.")
    public ValidatedDouble geneticMutationRate = new ValidatedDouble(0.3, 2.0, 0.0);

    @Translatable.Name("Simulated Offspring Per Tick")
    @Translatable.Desc("How many theoretical babies the 3D visualizer calculates every tick (20 ticks = 1 second) to build the probability particle cloud. Each particle spawned represents a baby hamster. Higher numbers create a denser, clearer picture of the genetic potential.")
    public ValidatedInt simulatedOffspringPerTick = new ValidatedInt(20, 300, 1);

    @Translatable.Name("Continuous Genetics Cylinder")
    @Translatable.Desc("If true, the particles used in the genetics visualization command to mark the edge of the cylinder will spawn continuously. If false, they will only spawn for the first second to indicate the edges, then they will disappear.")
    public boolean continuousGeneticsCylinder = true;

    @NonSync
    @Translatable.Name("Reset Your Breeding History")
    public ConfigAction resetBreedingHistory = new ConfigAction.Builder()
            .title(Component.translatable("config.adorablehamsterpets.main.breedingSettings.resetBreedingHistory"))
            .desc(Component.translatable("config.adorablehamsterpets.main.breedingSettings.resetBreedingHistory.desc"))
            .decoration(TextureIds.INSTANCE.getRESTORE())
            .build(() -> {
                if (Minecraft.getInstance().getConnection() != null) {
                    Minecraft.getInstance().getConnection().sendUnsignedCommand("ahp reset_player_breeding_history");
                }
            });

    @Translatable.Name("Babies Spawn Wild")
    @Translatable.Desc("If true, babies are born feral and unclaimed. You'll need to tame them yourself to earn their affections.")
    public boolean babiesSpawnWild = true;

    @Translatable.Name("Breeder Whitelist")
    @Translatable.Desc("A list of player usernames allowed to breed hamsters even if breeding is toggled off globally (see the 'Core Feature Toggles' section). Useful for server admins who want to keep breeding under control.")
    public List<String> allowedBreeders = new ArrayList<>();

    @Translatable.Name("Litters Per Hamster")
    @Translatable.Desc("How many times a single hamster can reproduce before its biological clock says 'absolutely not.'")
    public ValidatedInt maxLittersPerHamster = new ValidatedInt(50, 100, 1);

    @Translatable.Name("Min Litter Size")
    @Translatable.Desc("The minimum number of babies per litter.")
    public ValidatedInt litterSizeMin = new ValidatedInt(1, 15, 1);

    @Translatable.Name("Max Litter Size")
    @Translatable.Desc("The maximum number of babies per litter. Make sure this is greater than or equal to the minimum, obviously.")
    public ValidatedInt litterSizeMax = new ValidatedInt(3, 20, 1);

    @Translatable.Name("Limit Breeding By Player")
    @Translatable.Desc("Try as I might, I could not think of a better way to word that. Here's where you can cap the number of hamster litters a single player can breed.")
    public ValidatedBoolean playerBreedingLimit = new ValidatedBoolean(false);

    private final ValidatedField<Boolean> isPlayerBreedingLimitEnabled = playerBreedingLimit.map(b -> b, b -> b);

    @Translatable.Name("Player Breeding Limit Type")
    @Translatable.Desc("Whether the limit resets daily, or if it's a lifetime cap per player.")
    public ValidatedCondition<LitterLimitType> playerBreedingLimitType = new ValidatedEnum<>(LitterLimitType.DAILY)
            .toCondition(isPlayerBreedingLimitEnabled, Component.translatable("config.adorablehamsterpets.condition.litter_limit_enabled"), () -> LitterLimitType.DAILY);

    @Translatable.Name("Max Litters Per Player")
    @Translatable.Desc("How many litters a player can orchestrate before their breeding license is revoked.")
    public ValidatedCondition<Integer> maxLittersPerPlayer = new ValidatedInt(5, 100, 1)
            .toCondition(isPlayerBreedingLimitEnabled, Component.translatable("config.adorablehamsterpets.condition.litter_limit_enabled"), () -> 5);

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Use IRL Time")
    @Translatable.Desc("If true, 'Daily Player Breeding Limit' means 24 real-world hours. If false, it means 20 Minecraft minutes.")
    public ValidatedCondition<Boolean> useIrlTimeForBreedingLimit = new ValidatedBoolean(false)
            .toCondition(isPlayerBreedingLimitEnabled, Component.translatable("config.adorablehamsterpets.condition.litter_limit_enabled"), () -> false);

    // --- Armor Perks & Visuals ---
    @Translatable.Name("Armor Perks & Visuals")
    @Translatable.Desc("Here's where you make armor OP. Or turn it off. See if I care.")
    public ConfigGroup armorSettings = new ConfigGroup("armorSettings", true);

    @Translatable.Name("Armor Perks")
    @Translatable.Desc("Configure the buffs provided by specific armor materials. For Diamond armor perk configuration, remove or add items to the 'Retrievable Items' list in 'Core Item Tag Overrides.'")
    public ConfigGroup armorPerks = new ConfigGroup("armorPerks", true);

    @Translatable.Name("Iron")
    @Translatable.Desc("Aerodynamics provided by smooth Iron plating. Adds 0.5 to the throw velocity, which is 1.5 by default or 2.5 when under the influence of Steamed Green Beans.")
    public ValidatedCondition<Double> ironArmorThrowSpeedBoost = new ValidatedDouble(0.5, 5.0, 0.0)
            .toCondition(
                    areArmorPerksEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
                    () -> 0.0
            );

    @Translatable.Name("Gold")
    @Translatable.Desc("The zoom factor provided by Gold Armor. Because flimsier things go faster. Obviously. (+0.20 = +20% Speed)")
    public ValidatedCondition<Double> goldArmorSpeedBoost = new ValidatedDouble(0.20, 2.0, 0.0)
            .toCondition(
                    areArmorPerksEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
                    () -> 0.0
            );

    @Translatable.Name("Netherite")
    @Translatable.Desc("Configure the individual netherite buffs.")
    public ConfigGroup netheritePerks = new ConfigGroup("netheritePerks", true);


    @Translatable.Name("Knockback Resistance")
    @Translatable.Desc("The 'Immovable Object' density factor. 0.5 is 50% resistance. 1.0 makes them a neutron star.")
    public ValidatedCondition<Double> netheriteArmorKnockbackResist = new ValidatedDouble(0.5, 1.0, 0.0)
            .toCondition(
                    areArmorPerksEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
                    () -> 0.0
            );

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Throw Damage")
    @Translatable.Desc("Heavy things hit harder. Adds this much flat damage to the projectile impact. (1 = 0.5 hearts)")
    public ValidatedCondition<Double> netheriteArmorThrowDamageBonus = new ValidatedDouble(10.0, 100.0, 0.0)
            .toCondition(
                    areArmorPerksEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
                    () -> 0.0
            );

    @NonSync
    @Translatable.Name("Enable Armor Visuals")
    @Translatable.Desc("Master switch for armor rendering. If false, hamsters will appear unarmored even when equipped. Useful if you prefer the natural look but still want the protection.")
    public boolean enableArmorVisuals = true;

    // Helper field to gate the Acorn Hat setting
    private final ValidatedField<Boolean> isArmorVisualsEnabled = new ValidatedBoolean(true).map(b -> b, b -> enableArmorVisuals);

    @NonSync
    @Translatable.Name("Render Acorn Hat")
    @Translatable.Desc("Determines whether you are able to see the jaunty little Acorn Hat when hamsters are wearing the base Acorn Armor. Does not affect what other players see, and does not apply to the standalone Acorn Hat accessory.")
    public ValidatedCondition<Boolean> renderAcornHat = new ValidatedBoolean(true)
            .toCondition(
                    isArmorVisualsEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_visuals_enabled"),
                    () -> false
            );

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Render Flowers On Armor")
    @Translatable.Desc("If true, flower accessories will pop out to render on the outside of equipped armor. If false, they will be hidden under the armor.")
    public ValidatedCondition<Boolean> renderFlowersWithArmor = new ValidatedBoolean(true)
            .toCondition(
                    isArmorVisualsEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_visuals_enabled"),
                    () -> false
            );

    // --- Shoulder Hamster Settings ---
    @Translatable.Name("Shoulder Hamster Settings")
    @Translatable.Desc("Settings for your fuzzy parrot of doom.")
    public ConfigGroup shoulder = new ConfigGroup("shoulder", true);

    @Translatable.Name("Core Settings")
    @Translatable.Desc("Just the basic stuff. You know, detecting creepers, sniffing diamonds. Just average Minecraft stuff really. No big deal. Why are you clapping and squealing? Stop that. You look silly.")
    public ConfigGroup shoulderCore = new ConfigGroup("shoulderCore", true);

    @Translatable.Name("Max Mounts")
    @Translatable.Desc("The maximum number of hamsters you can carry on your shoulders and head. Limits the chaos. Changes take effect on next mount attempt.")
    public ValidatedInt maxShoulderHamsters = new ValidatedInt(3, 3, 1);

    @NonSync
    @Translatable.Name("Mount Priority")
    @Translatable.Desc("Where should the hamster go first? 'Shoulders First' fills the Right Shoulder, then Left, then Head. 'Head First' fills Head, then Right, then Left.")
    public ValidatedEnum<MountPriority> mountPriority = new ValidatedEnum<>(MountPriority.HEAD_FIRST);

    @Translatable.Name("Retain Shoulder Mounts")
    @Translatable.Desc("If true, any hamsters on your shoulder will remain there when you respawn. If false (default), they will remain at your death location, passed out from the sheer shock of seeing you die. They may need a quick pat to wake them up when you return.")
    public boolean keepHamstersOnShoulderOnDeath = false;

    @Translatable.Name("Consume Lure Item")
    @Translatable.Desc("Should luring a hamster to your shoulder consume the item (e.g., cheese)? Turn this off if you believe your charm alone should be enough. The item will still be required, just not eaten.")
    public boolean consumeLureItem = true;

    @Translatable.Name("Enable Force-Mount Keybind")
    @Translatable.Desc("Tired of wasting perfectly good cheese? Enable this to use a dedicated keybind (unbound by default). Hold down this key while right-clicking your hamster to hoist them onto your shoulder, no questions asked. Uses a separate key you must set in Settings > Controls > Key Binds.")
    public boolean enableShoulderMountKeybind = false;

    @NonSync
    @Translatable.Name("Creeper Detection Radius")
    @Translatable.Desc("Adjust paranoia levels. (Distance in blocks)")
    public ValidatedDouble shoulderCreeperDetectionRadius = new ValidatedDouble(16.0, 16.0, 1.0);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Diamond Detection Radius")
    @Translatable.Desc("How close (in blocks) you need to be before the squeak says \"bling.\"")
    public ValidatedDouble shoulderDiamondDetectionRadius = new ValidatedDouble(10.0, 20.0, 5.0);

    @Translatable.Name("Dismount Settings")
    @Translatable.Desc("Here's where you decide how to get the little rascals off your shoulders. Warning: they can be clingy.")
    public ConfigGroup shoulderDismount = new ConfigGroup("shoulderDismount", true);

    @NonSync
    @Translatable.Name("Dismount Order")
    @Translatable.Desc("Determines the sequence for dismounting hamsters with a key press. LIFO (Last-In, First-Out) dismounts the most recently added hamster. FIFO (First-In, First-Out) dismounts the oldest one.")
    public ValidatedEnum<DismountOrder> dismountOrder = new ValidatedEnum<>(DismountOrder.LIFO);

    @NonSync
    @Translatable.Name("Button‑Press Behavior")
    @Translatable.Desc("Choose whether a single press or a quick double‑tap dismounts the hamster.")
    public ValidatedEnum<DismountButtonPressBehavior> dismountButtonPressBehavior =
            new ValidatedEnum<>(DismountButtonPressBehavior.DOUBLE_TAP);

    private final ValidatedField<Boolean> isDoubleTap =
            dismountButtonPressBehavior.map(
                    pt -> pt == DismountButtonPressBehavior.DOUBLE_TAP,
                    b -> b ? DismountButtonPressBehavior.DOUBLE_TAP : DismountButtonPressBehavior.SINGLE_PRESS
            );

    @NonSync
    @Translatable.Name("Custom Key Behavior Override")
    @Translatable.Desc("If true, binding a custom key to 'Dismount Hamster' will automatically force it to be a single press, ignoring the Button-Press Behavior setting. I assume if you gave it a dedicated key, you don't want to tap it twice.")
    public boolean singlePressOverrideForCustomKey = true;

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Double-Tap Delay")
    @Translatable.Desc("Max time between sneak key presses to count as a double-tap. (20 ticks = 1 second)")
    public ValidatedCondition<Integer> doubleTapDelayTicks =
            new ValidatedInt(10, 40, 5)
                    .toCondition(
                            isDoubleTap,
                            Component.translatable("config.adorablehamsterpets.condition.double_tap"),
                            () -> 10
                    );

    @NonSync
    @Translatable.Name("Animation Settings")
    @Translatable.Desc("Control how lively your shoulder-mounted companions are. I mean, I don't like to toot my own horn or anything, but this is pretty great. Now please excuse me while I bask in my humility.")
    public ConfigGroup shoulderAnimations = new ConfigGroup("shoulderAnimations", true);

    @NonSync
    @Translatable.Name("Enable Dynamic Animations")
    @Translatable.Desc("If true, hamsters on your shoulder will randomly cycle through standing, sitting, and laying down while on the shoulder. If false, they will remain in a single state defined below.")
    public ValidatedBoolean enableDynamicShoulderAnimations = new ValidatedBoolean(true);

    private final ValidatedField<Boolean> dynamicShoulderDisabled =
            enableDynamicShoulderAnimations.map(
                    value -> !value,
                    value -> !value
            );

    @NonSync
    @Translatable.Name("Forced State (Head)")
    @Translatable.Desc("If dynamic animations are disabled, choose the state for the hamster on your head. Sometimes this setting can have a delay before kicking in, but switching states back and forth usually fixes it.")
    public ValidatedCondition<ForcedShoulderState> forcedHeadState =
            new ValidatedEnum<>(ForcedShoulderState.ALWAYS_STAND)
                    .toCondition(
                            // Use the inverted validated field as gating condition
                            dynamicShoulderDisabled,
                            // Message shown when condition fails
                            Component.translatable("config.adorablehamsterpets.condition.dynamic_shoulder_off"),
                            // Fallback
                            () -> ForcedShoulderState.ALWAYS_STAND
                    );

    @NonSync
    @Translatable.Name("Forced State (Right)")
    @Translatable.Desc("See description for 'Forced State (Head)'.")
    public ValidatedCondition<ForcedShoulderState> forcedRightShoulderState =
            new ValidatedEnum<>(ForcedShoulderState.ALWAYS_STAND)
                    .toCondition(
                            dynamicShoulderDisabled,
                            Component.translatable("config.adorablehamsterpets.condition.dynamic_shoulder_off"),
                            () -> ForcedShoulderState.ALWAYS_STAND
                    );

    @NonSync
    @Translatable.Name("Forced State (Left)")
    @Translatable.Desc("See description for 'Forced State (Head)'.")
    public ValidatedCondition<ForcedShoulderState> forcedLeftShoulderState =
            new ValidatedEnum<>(ForcedShoulderState.ALWAYS_STAND)
                    .toCondition(
                            dynamicShoulderDisabled,
                            Component.translatable("config.adorablehamsterpets.condition.dynamic_shoulder_off"),
                            () -> ForcedShoulderState.ALWAYS_STAND
                    );

    @NonSync
    @Translatable.Name("Force Lay Down on Walk")
    @Translatable.Desc("False by default. If true, shoulder hamsters will be forced into their 'laying down' animation when you move, as if trying not to fall off. If false, they will continue their normal animation cycle.")
    public boolean forceLayDownOnWalk = false;

    @NonSync
    @Translatable.Name("Force Lay Down on Sprint")
    @Translatable.Desc("If true, shoulder hamsters will be forced into their 'laying down' animation while you sprint, as if holding on for dear life. If false, they will continue their normal animation cycle.")
    public boolean forceLayDownOnSprint = true;

    @NonSync
    @Translatable.Name("Min Animation State Duration")
    @Translatable.Desc("The minimum time (in seconds) a shoulder hamster will stay in any one animation state (standing, sitting, or laying down). A random duration between the min and max is chosen for each transition.")
    public ValidatedInt shoulderMinStateSeconds = new ValidatedInt(20, 280, 5);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Max Animation State Duration")
    @Translatable.Desc("The maximum time (in seconds) a shoulder hamster will stay in any one animation state (standing, sitting, or laying down). A random duration between the min and max is chosen for each transition.")
    public ValidatedInt shoulderMaxStateSeconds = new ValidatedInt(45, 300, 6);

    @NonSync
    @Translatable.Name("Audio Settings")
    @Translatable.Desc("For when the squeaks become... a bit much.")
    public ConfigGroup shoulderAudio = new ConfigGroup("shoulderAudio", true);

    @NonSync
    @Translatable.Name("Silence Idle Sounds")
    @Translatable.Desc("Mutes the ambient squeaks from shoulder-mounted hamsters. The bounce and alert sounds will still play.")
    public boolean silenceShoulderIdleSounds = false;

    @NonSync
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Mute 1st-Person Physics SFX")
    @Translatable.Desc("Mutes the hamster landing/bounce sound effect from the physics simulation when you are in first-person view.")
    public boolean silencePhysicsSoundsInFirstPerson = false;

    // --- The Hamster Yeet ---
    @Translatable.Name("The Hamster Yeet")
    @Translatable.Desc("For when you need a furry, surprisingly aerodynamic solution.")
    public ConfigGroup yeetSettings = new ConfigGroup("yeetSettings", true);

    @Translatable.Name("Throw Velocity")
    @Translatable.Desc("The base throw speed of your furry projectile.")
    public ValidatedDouble hamsterThrowVelocity = new ValidatedDouble(1.5, 5.0, 0.1);

    @Translatable.Name("Throw Velocity (Buffed)")
    @Translatable.Desc("The throw speed of your furry projectile when under the influence of Steamed Green Beans. Goes from 'yeet' to 'yote'.")
    public ValidatedDouble hamsterThrowVelocityBuffed = new ValidatedDouble(2.5, 5.0, 0.1);

    @Translatable.Name("Downward Force (Gravity)")
    @Translatable.Desc("How fast the hamster falls while airborne. Lower values result in flatter, longer flights without increasing the initial thrust. Only affects the projectile entity during the throw.")
    public ValidatedDouble hamsterThrowGravity = new ValidatedDouble(0.035, 0.2, 0.0);

    @ConfigGroup.Pop
    @Translatable.Name("Friendly Fire")
    @Translatable.Desc("If true, throwing your own hamster straight up above your head will eventually result in a concussive reunion on the way back down. High-velocity rodents tend to hurt, even if they love you.")
    public boolean yeetFriendlyFire = true;

    // --- Sleepiness & Napping Behaviors ---
    @Translatable.Name("Sleepiness & Napping Behaviors")
    @Translatable.Desc("Even digital rodents need beauty sleep— adjust according to your patience levels.")
    public ConfigGroup tamedSleepSettings = new ConfigGroup("tamedSleepSettings", true);

    @Translatable.Name("Threat Radius")
    @Translatable.Desc("How close (in blocks) a hostile mob can get before a hamster wakes up from it's power nap.")
    public ValidatedInt tamedSleepThreatDetectionRadiusBlocks = new ValidatedInt(8, 32, 1);

    @Translatable.Name("Require Daytime?")
    @Translatable.Desc("Choose when your sitting hamster will succumb to drowsiness. 'True' means your sitting hamster will only doze off during the day— 'false' means it can doze off anytime. This setting does not affect the behavior of a hamster when sleeping in a bed.")
    public boolean requireDaytimeForTamedSleep = true;

    @Translatable.Name("Min Sit Time Before Drowsy (Secs)")
    @Translatable.Desc("Minimum seconds before a sitting hamster gets sleepy.")
    public ValidatedInt tamedQuiescentSitMinSeconds = new ValidatedInt(120, 300, 1);

    @ConfigGroup.Pop
    @Translatable.Name("Max Sit Time Before Drowsy (Secs)")
    @Translatable.Desc("Maximum seconds before the inevitable deep snooze.")
    public ValidatedInt tamedQuiescentSitMaxSeconds = new ValidatedInt(180, 600, 2);

    // --- Hamster Beds & Wander Mode ---
    @Translatable.Name("Hamster Beds & Wander Mode")
    @Translatable.Desc("For when 'following you into lava' is no longer a desirable trait. Tweak the settings for your hamster's newfound, bed-based independence.")
    public ConfigGroup wanderMode = new ConfigGroup("wanderMode", true);

    @Translatable.Name("Enable Respawn in Bed")
    @Translatable.Desc("The Master Switch. Affects all Hamster Beds. If true, hamsters linked to a bed can be resurrected there. Cheating? Maybe. Convenient? Absolutely.")
    public ValidatedBoolean enableRespawnInBed = new ValidatedBoolean(false);

    // Helper field for gating
    private final ValidatedField<Boolean> isRespawnInBedEnabled = enableRespawnInBed.map(b -> b, b -> b);

    @Translatable.Name("Tribute One-Time Use")
    @Translatable.Desc("If true, the bed only requires the resurrection tribute item once to be permanently activated for infinite respawns. (Ignored if 'Free Bed Respawns' is already enabled).")
    public ValidatedCondition<Boolean> infiniteRespawnsAfterTribute = new ValidatedBoolean(false)
            .toCondition(isRespawnInBedEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.respawn_enabled"),
                    () -> false);

    @Translatable.Name("Free Bed Respawns")
    @Translatable.Desc("If true, Hamster Beds do not require a tribute item to function as a respawn point. They will work indefinitely for free.")
    public ValidatedCondition<Boolean> freeBedRespawns = new ValidatedBoolean(false)
            .toCondition(isRespawnInBedEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.respawn_enabled"),
                    () -> false);

    @Translatable.Name("Resurrection Tributes")
    @Translatable.Desc("IGNORED IF 'Free Bed Respawns' IS ENABLED. The specific items accepted by the Hamster Bed to enable the Respawn Protocol. Defaults to the Totem of Undying. Accepts item IDs (e.g. 'minecraft:totem_of_undying') or tags.")
    public List<String> resurrectionTributes = new ArrayList<>(List.of("minecraft:totem_of_undying"));

    @Translatable.Name("Avoid Unlinked Beds")
    @Translatable.Desc("Should hamsters treat other hamsters' beds as sacred ground? If true, they'll try to politely path around them, but they will only try a few alternate paths before their tiny rodent-patience runs out. If false, they'll trample wherever they please.")
    public boolean avoidUnlinkedBeds = true;

    @Translatable.Name("Default Wander Distance")
    @Translatable.Desc("The initial wander distance set when a hamster is first linked to a bed. It defaults to medium, because that is the universally accepted starting point for all life choices.")
    public ValidatedEnum<WanderDistance> defaultWanderDistance = new ValidatedEnum<>(WanderDistance.MEDIUM);

    @Translatable.Name("Distance: Near")
    @Translatable.Desc("The radius (in blocks) for the 'Near' wander distance setting. For the clingy hamster who wants freedom, but not too much.")
    public ValidatedInt wanderDistanceNear = new ValidatedInt(8, 64, 1);

    @Translatable.Name("Distance: Medium")
    @Translatable.Desc("The radius (in blocks) for the 'Medium' wander distance setting. A respectable distance. Not too close, not too far. Perfectly balanced, as all things should be.")
    public ValidatedInt wanderDistanceMedium = new ValidatedInt(16, 64, 1);

    @Translatable.Name("Distance: Far")
    @Translatable.Desc("The radius (in blocks) for the 'Far' wander distance setting. For the adventurous hamster who might send you a postcard someday. Maybe.")
    public ValidatedInt wanderDistanceFar = new ValidatedInt(32, 64, 1);

    @Translatable.Name("Allow Sleeping in Bed")
    @Translatable.Desc("The global override for whether hamsters can sleep in their beds. If enabled, all hamsters in wander mode will seek out their bed to sleep at specific times, regardless of individual bed settings. If disabled, they'll just pass out when sitting, like your old uncle at family gatherings.")
    public ValidatedBoolean allowSleepInBed = new ValidatedBoolean(true);

    // Helper field to gate all other sleep settings
    private final ValidatedField<Boolean> isSleepInBedAllowed = allowSleepInBed.map(b -> b, b -> b);

    @Translatable.Name("Circadian Chaos")
    @Translatable.Desc("Tired of your hamsters adhering to the rigid tyranny of the day/night cycle? Enable this for a more... unpredictable napping schedule. When enabled, this will override the 'Sleep During the Day' setting.")
    public ValidatedCondition<Boolean> circadianChaos = new ValidatedBoolean(false)
            .toCondition(
                    isSleepInBedAllowed,
                    Component.translatable("config.adorablehamsterpets.condition.sleep_in_bed_allowed"),
                    () -> false
            );

    // Helper field to gate the min and max time settings
    private final ValidatedField<Boolean> isCircadianChaosEnabled = circadianChaos.map(b -> b, b -> b);

    @Translatable.Name("Min Nap Interval")
    @Translatable.Desc("The shortest possible time (in seconds) a hamster will stay awake or asleep in bed before considering a change. Defaults to 5 minutes— for the truly narcoleptic rodent. A random duration between the min and max is chosen each time, so move them further apart for more... unpredictable behavior.")
    public ValidatedCondition<Integer> minNapInBedIntervalSeconds  = new ValidatedInt(300, 7000, 5)
            .toCondition(
                    () -> allowSleepInBed.get() && circadianChaos.get(),
                    Component.translatable("config.adorablehamsterpets.condition.circadian_chaos_on"),
                    () -> 300
            );

    @Translatable.Name("Max Nap Interval")
    @Translatable.Desc("The longest amount of time (in seconds) a hamster can possibly stay awake or asleep in bed before it gets bored and switches things up. Defaults to 10 minutes.")
    public ValidatedCondition<Integer> maxNapInBedIntervalSeconds = new ValidatedInt(600, 7200, 10)
            .toCondition(
                    () -> allowSleepInBed.get() && circadianChaos.get(),
                    Component.translatable("config.adorablehamsterpets.condition.circadian_chaos_on"),
                    () -> 900
            );

    @Translatable.Name("Sleep During the Day")
    @Translatable.Desc("If false, wandering hamsters will sleep in their beds during the night. If true, they'll adopt a more nocturnal, goth-adjacent lifestyle and sleep in the daytime.")
    public ValidatedCondition<Boolean> sleepDuringDay = new ValidatedBoolean(true)
            .toCondition(
                    () -> allowSleepInBed.get() && !circadianChaos.get(),
                    Component.translatable("config.adorablehamsterpets.condition.circadian_chaos_overrides"),
                    () -> true
            );

    @Translatable.Name("Manual Wake-Up Duration")
    @Translatable.Desc("The mandatory grumpiness period if you rudely awaken a hamster from its bed before it was ready. It won't go back to sleep until this timer runs out. (20 ticks = 1 second)")
    public ValidatedCondition<Integer> bedWakeUpCooldown = new ValidatedInt(300, 1200, 20)
            .toCondition(
                    isSleepInBedAllowed,
                    Component.translatable("config.adorablehamsterpets.condition.sleep_in_bed_allowed"),
                    () -> 300
            );

    @Translatable.Name("Warn on Unlinked Placement")
    @Translatable.Desc("Warns you if you try to place a Hamster Bed before linking it to a hamster.")
    public boolean warnOnUnlinkedBedPlacement = true;

    @ConfigGroup.Pop
    @Translatable.Name("Seen Unlinked Bed Warning")
    @Translatable.Desc("A list of players who have already received the 'unlinked bed' warning. Delete your name to experience it all over again.")
    public List<String> playersWhoHaveSeenUnlinkedBedWarning = new ArrayList<>();

    // --- Ambient Sitting Behaviors ---
    @Translatable.Name("Ambient Sitting Behaviors")
    @Translatable.Desc("Configure the random, spontaneous things your hamster does while sitting.")
    public ConfigGroup ambientSittingBehaviors = new ConfigGroup("ambientSittingBehaviors", true);

    @Translatable.Name("Cleaning Frequency")
    @Translatable.Desc("How often a sitting hamster gets the sudden urge to clean. It's a 1-in-X chance per tick, so lower numbers mean a higher chance of it happening. (e.g., 1200 = roughly once per minute).")
    public ValidatedInt cleaningChanceDenominator = new ValidatedInt(1200, 5000, 100);

    @Translatable.Name("Rolling Frequency")
    @Translatable.Desc("How often a sitting hamster playfully rolls onto its back. It's a 1-in-X chance per tick, so lower numbers mean a higher chance of it happening. (e.g., 1200 = roughly once per minute)")
    public ValidatedInt rollingChanceDenominator = new ValidatedInt(1800, 5000, 100);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Cartoon Rolling Sound")
    @Translatable.Desc("If true, the hamster's rolling SFX will include a cartoon-ish slide whistle. It's subtle, but some people might get distracted easily or prefer more realism so I turned it off by default.")
    public boolean enableRollingSlideWhistle = false;

    // --- Shader Materials PBR ---
    @Translatable.Name("Shader Materials PBR")
    @Translatable.Desc("Configure LabPBR values for hamsters, accessories, and armor. These settings only affect the game when used alongside a LabPBR-compatible shader.")
    public ConfigGroup shaderPbrSettings = new ConfigGroup("shaderPbrSettings", true);

    @NonSync
    @Translatable.Name("Fur SSS")
    @Translatable.Desc("Subsurface Scattering, i.e, the amount of light that passes through the hamster's fur. 65 is solid, 255 is pure translucent gelatin.")
    public ValidatedInt furSss = new ValidatedInt(75, 255, 65);

    @NonSync
    @Translatable.Name("Skin SSS")
    @Translatable.Desc("Subsurface Scattering, i.e, the amount of light that passes through the hamster's skin (ears, nose, feet). 65 is solid, 255 is pure translucent gelatin.")
    public ValidatedInt skinSss = new ValidatedInt(100, 255, 65);

    @NonSync
    @Translatable.Name("Accessory SSS")
    @Translatable.Desc("Subsurface Scattering, i.e, the amount of light that passes through accessories like Pink Petals and Acorn Hats. 65 is solid, 255 is pure translucent gelatin.")
    public ValidatedInt accessorySss = new ValidatedInt(75, 255, 65);

    @NonSync
    @Translatable.Name("Max POM Depth")
    @Translatable.Desc("The maximum displacement depth for Parallax Occlusion Mapping (0.0 to 0.25). NOTE: This will have no effect unless your shader explicitly supports POM on entities, which is extremely rare.")
    public ValidatedFloat maxPomDepth = new ValidatedFloat(0.08f, 0.25f, 0.0f);

    @Translation(prefix = "adorablehamsterpets.main.armorPbrValues")
    public static class ArmorPbrValues {
        @NonSync
        @Translatable.Name("Emissiveness")
        @Translatable.Desc("The Alpha channel. How much it glows in the dark. 0 is dark, 254 is a tiny blinding sun. DO NOT set it to 255. LabPBR ignores 255 entirely, so it actually means zero glow.")
        public ValidatedInt emissive;

        @NonSync
        @Translatable.Name("Porosity & SSS")
        @Translatable.Desc("The Blue channel. 0-64 controls Porosity (how much darker it gets when wet). 65-255 controls Subsurface Scattering (light passing through, like skin/wax). 65 is completely solid, 255 is pure translucent rodent gelatin.")
        public ValidatedInt sss;

        @NonSync
        @Translatable.Name("Reflectance & Metallic")
        @Translatable.Desc("The Green channel. 0-229 controls standard reflectance. 230-254 triggers hardcoded metals in your shader (230=Iron, 231=Gold, etc). 255 tells the shader to tint the reflection based on the texture's color.")
        public ValidatedInt metallic;

        @NonSync
        @Translatable.Name("Smoothness")
        @Translatable.Desc("The Red channel. 0 is rough sandpaper. 255 is a perfectly polished mirror.")
        public ValidatedInt smoothness;

        public ArmorPbrValues(int emissive, int sss, int metallic, int smoothness) {
            this.emissive = new ValidatedInt(emissive, 255, 0);
            this.sss = new ValidatedInt(sss, 255, 0);
            this.metallic = new ValidatedInt(metallic, 255, 0);
            this.smoothness = new ValidatedInt(smoothness, 255, 0);
        }

        public ArmorPbrValues() {
            this(0, 0, 0, 0);
        }
    }

    @NonSync
    @Translatable.Name("Enable Armor PBR")
    @Translatable.Desc("If true, hamster armor generates and utilizes Specular maps for shiny metals and rough Acorn. Disable if you prefer boring, plastic textures.")
    public ValidatedCondition<Boolean> enableArmorPbr = new ValidatedBoolean(true)
            .toCondition(
                    isArmorVisualsEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_visuals_enabled"),
                    () -> false
            );

    // Helper field to gate PBR sliders
    private final ValidatedField<Boolean> isArmorPbrEnabled = enableArmorPbr.map(b -> b, b -> b);

    @Translation(prefix = "adorablehamsterpets.main.armorPbrValues")
    public static class AcornPbrValues extends ArmorPbrValues { public AcornPbrValues() { super(255, 50, 0, 90); } }

    @Translation(prefix = "adorablehamsterpets.main.armorPbrValues")
    public static class IronPbrValues extends ArmorPbrValues { public IronPbrValues() { super(0, 0, 130, 220); } }

    @Translation(prefix = "adorablehamsterpets.main.armorPbrValues")
    public static class GoldPbrValues extends ArmorPbrValues { public GoldPbrValues() { super(0, 0, 50, 220); } }

    @Translation(prefix = "adorablehamsterpets.main.armorPbrValues")
    public static class DiamondPbrValues extends ArmorPbrValues { public DiamondPbrValues() { super(0, 0, 50, 220); } }

    @Translation(prefix = "adorablehamsterpets.main.armorPbrValues")
    public static class NetheritePbrValues extends ArmorPbrValues { public NetheritePbrValues() { super(0, 0, 20, 120); } }

    @Translatable.Name("Armor PBR Values")
    @Translatable.Desc("Fine-tune the exact LabPBR specular map values for each armor tier. Because you definitely have an opinion on the precise refractive index of an acorn.")
    public ConfigGroup pbrMaterialSettings = new ConfigGroup("pbrMaterialSettings", true);

    @NonSync
    @Translatable.Name("Acorn Armor PBR")
    public ValidatedCondition<ArmorPbrValues> acornPbr =
            new ValidatedAny<ArmorPbrValues>(new AcornPbrValues())
                    .toCondition(
                            isArmorPbrEnabled,
                            Component.translatable("config.adorablehamsterpets.condition.armor_pbr_enabled"),
                            AcornPbrValues::new
                    );

    @NonSync
    @Translatable.Name("Iron Armor PBR")
    public ValidatedCondition<ArmorPbrValues> ironPbr =
            new ValidatedAny<ArmorPbrValues>(new IronPbrValues())
                    .toCondition(
                            isArmorPbrEnabled,
                            Component.translatable("config.adorablehamsterpets.condition.armor_pbr_enabled"),
                            IronPbrValues::new
                    );

    @NonSync
    @Translatable.Name("Gold Armor PBR")
    public ValidatedCondition<ArmorPbrValues> goldPbr =
            new ValidatedAny<ArmorPbrValues>(new GoldPbrValues())
                    .toCondition(
                            isArmorPbrEnabled,
                            Component.translatable("config.adorablehamsterpets.condition.armor_pbr_enabled"),
                            GoldPbrValues::new
                    );

    @NonSync
    @Translatable.Name("Diamond Armor PBR")
    public ValidatedCondition<ArmorPbrValues> diamondPbr =
            new ValidatedAny<ArmorPbrValues>(new DiamondPbrValues())
                    .toCondition(
                            isArmorPbrEnabled,
                            Component.translatable("config.adorablehamsterpets.condition.armor_pbr_enabled"),
                            DiamondPbrValues::new
                    );

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Netherite Armor PBR")
    public ValidatedCondition<ArmorPbrValues> netheritePbr =
            new ValidatedAny<ArmorPbrValues>(new NetheritePbrValues())
                    .toCondition(
                            isArmorPbrEnabled,
                            Component.translatable("config.adorablehamsterpets.condition.armor_pbr_enabled"),
                            NetheritePbrValues::new
                    );

    @NonSync
    @Translatable.Name("Emissive Armor Trims")
    @Translatable.Desc("If true, armor trims will naturally glow in the dark and trigger bloom effects, which will be especially visible if you're using shaders. Turn it off if you prefer your rodents to remain grounded in a dull, non-luminescent reality.")
    public ValidatedCondition<Boolean> emissiveArmorTrims = new ValidatedBoolean(true)
            .toCondition(
                    isArmorVisualsEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.armor_visuals_enabled"),
                    () -> false
            );

    // Helper field to gate trim emissive brightness
    private final ValidatedField<Boolean> isTrimEmissiveEnabled = emissiveArmorTrims.map(b -> b, b -> b);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Trim Emissive Brightness")
    @Translatable.Desc("How bright the armor trims glow. 0 is barely visible, 254 is maximum brightness. (Note: LabPBR ignores 255 entirely).")
    public ValidatedCondition<Integer> trimEmissiveBrightness = new ValidatedInt(254, 254, 0)
            .toCondition(
                    isTrimEmissiveEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.trim_emissive_enabled"),
                    () -> 254
            );

    // --- Mini-Game Settings ---
    @Translatable.Name("Mini-Game Settings")
    @Translatable.Desc("Rules for when your hamster gets bored and decides to create its own entertainment.")
    public ConfigGroup miniGames = new ConfigGroup("miniGames", true);

    @Translatable.Name("Minimum Flee Distance")
    @Translatable.Desc("The personal space bubble (in blocks) hamsters maintain if the mini-game involves running away.")
    public ValidatedInt minMiniGameFleeDistance = new ValidatedInt(7, 20, 1);

    @Translatable.Name("Maximum Flee Distance")
    @Translatable.Desc("The maximum distance (in blocks) before hamsters stop running. If they get further than this, they will stop and begin taunting (if taunting is part of the mini-game).")
    public ValidatedInt maxMiniGameFleeDistance = new ValidatedInt(10, 30, 5);

    @Translatable.Name("Minimum Game Duration")
    @Translatable.Desc("The shortest amount of time (in seconds) a mini-game lasts before hamsters get bored. Randomly chosen between Min and Max.")
    public ValidatedInt minMiniGameFleeDurationSeconds = new ValidatedInt(5, 240, 1);

    @Translatable.Name("Maximum Game Duration")
    @Translatable.Desc("The longest amount of time (in seconds) a mini-game lasts. Randomly chosen between Min and Max.")
    public ValidatedInt maxMiniGameFleeDurationSeconds = new ValidatedInt(15, 300, 5);

    @Translatable.Name("Use Pouch Loot for Rewards")
    @Translatable.Desc("If true, after a successful mini-game, your hamster will reward you with whatever random pocket lint it is allowed to spawn with in its cheek pouches. (Configurable: see 'Cheek Pouch Loot' in the World Gen Config.) If false, rewards will be pulled directly from the custom list below.")
    public boolean usePouchLootForMiniGameRewards = true;

    @Translatable.Name("Custom Rewards")
    @Translatable.Desc("The specific items your hamster will gift you after a successful game, assuming you disabled the toggle above. Format specific item names like this: 'minecraft:diamond' and you can use tags like this: '#minecraft:flowers'. If you leave this empty while custom rewards are active, your hamster will just stare at you awkwardly and not give you anything. Maybe that's what you want? I'm not your boss.")
    public List<String> customMiniGameRewards = new ArrayList<>();

    // --- The Tree Heist ---
    @Translatable.Name("The Tree Heist")
    @Translatable.Desc("Configure the acorn-gathering operations.")
    public ConfigGroup treeHeist = new ConfigGroup("treeHeist", true);

    @Translatable.Name("Acorn Drop Chance")
    @Translatable.Desc("The likelihood (0.0 to 1.0) of an acorn dropping each time your hamster rummages. Default is very low at 0.03 (3%), because the hamster rummages roughly ~5 times per second. Crank it up if you want to crash the local squirrel economy. (Maximum output is 1 acorn per second even if you turn it all the way up).")
    public ValidatedFloat acornDropChance = new ValidatedFloat(0.03f, 1.0f, 0.0f);

    @Translatable.Name("Heistable Leaves")
    @Translatable.Desc("A list of block IDs or tags that are considered valid leaves for the Tree Heist. Defaults to vanilla Oak Leaves and Dynamic Trees' Oak Leaves. Note: The Dynamic Trees mod adds their own type of acorns and drop methods. When that mod is installed, the only way to get the specific acorns from my mod is through the Tree Heist.")
    public List<String> heistableLeaves = new ArrayList<>(List.of(
            "minecraft:oak_leaves", "dynamictrees:oak_leaves"
    ));

    @Translatable.Name("Heistable Logs")
    @Translatable.Desc("A list of block IDs or tags that are considered valid logs/branches for the Tree Heist. Throwing a hamster at these will start a heist.")
    public List<String> heistableLogs = new ArrayList<>(List.of(
            "#minecraft:oak_logs", "dynamictrees:oak_branch"
    ));

    @NonSync
    @Translatable.Name("Reset History")
    public ConfigAction resetHeistHistory = new ConfigAction.Builder()
            .title(Component.translatable("config.adorablehamsterpets.main.treeHeist.resetHeistHistory"))
            .desc(Component.translatable("config.adorablehamsterpets.main.treeHeist.resetHeistHistory.desc"))
            .decoration(TextureIds.INSTANCE.getRESTORE())
            .build(() -> {
                NetworkManager.sendToServer(new ResetHeistHistoryPayload());
            });

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Debug Mode")
    @Translatable.Desc("Shows visual particles for the detected Tree ID (the lowest block of the trunk) and leaf canopy during a heist, and turns on extra logging in the console. Useful for seeing exactly which tree your hamster is searching and/or debugging if things are being weird.")
    public boolean debugTreeDetection = false;

    // --- Crop & Item Snacking ---
    @Translatable.Name("Crop & Item Snacking")
    @Translatable.Desc("Sometimes a hamster gets the munchies while wandering. If enabled, they will occasionally pillage your fully grown crops, accidentally replant seeds, and stuff the profits into their face. To see a list of crops and items hamsters may snack on, see the 'Core Item Tag Overrides' settings.")
    public ConfigGroup cropSnacking = new ConfigGroup("cropSnacking", true);

    @Translatable.Name("Snacking Chance")
    @Translatable.Desc("How likely a wandering hamster is to attempt to steal crops. (1-in-X chance per tick). Default is 300 which equates to every 15 seconds on average. Set lower to increase the likelihood.")
    public ValidatedInt cropSnackingChanceDenominator = new ValidatedInt(300, 10000, 1);

    @Translatable.Name("Messiness")
    @Translatable.Desc("The probability (0.0 to 1.0) that a hamster will accidentally replant a seed while aggressively harvesting a crop. At 0.0, they will perfectly uproot the crop, not spilling any seeds. At 1.0, they will be extremely messy and replant every time. Defaults to 0.75 (75%).")
    public ValidatedFloat cropReplantChance = new ValidatedFloat(0.75f, 1.0f, 0.0f);

    @Translatable.Name("Ignore Seeds")
    @Translatable.Desc("If true, your hamster will ignore the seeds that drop during a midnight snack.")
    public boolean ignoreSeeds = false;

    @ConfigGroup.Pop
    @Translatable.Name("Restrict to Wander Mode")
    @Translatable.Desc("If true, hamsters will only scavenge for dropped snack items if they are actively in Wander Mode. If false, they might snatch up your dropped snackables while following you around.")
    public boolean restrictItemSnackingToWanderMode = true;

    // --- Independent Diamond Seeking ---
    @Translatable.Name("Independent Diamond Seeking")
    @Translatable.Desc("Unleash free-range prospectors. What could go wrong?")
    public ConfigGroup independentDiamondSeeking = new ConfigGroup("independentDiamondSeeking", true);

    @Translatable.Name("Diamond Seek Scan Radius")
    @Translatable.Desc("How far (in blocks) a hamster scans once it’s decided to play prospector.")
    public ValidatedInt diamondSeekRadius = new ValidatedInt(10, 20, 5);

    @Translatable.Name("Gold 'Mistake' Chance")
    @Translatable.Desc("The probability (0.0 to 1.0) that a hamster will seek gold instead of diamond, if both are available. At 0.5, it's a coin toss. At 1.0, it's guaranteed hamster sulking.")
    public ValidatedFloat goldMistakeChance = new ValidatedFloat(0.33f, 1.0f, 0.0f);

    @Translatable.Name("Desirable Ores")
    @Translatable.Desc("The list of blocks that smell... shiny. Especially to greedy hamsters. Diamonds by default, but maybe you want them to sniff for Zinc ore? Accepts block IDs (e.g. 'create:zinc_ore') and #tags (e.g. '#c:ores/diamond').")
    public List<String> celebrationOres = new ArrayList<>(List.of(
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Disappointing Ores")
    @Translatable.Desc("Blocks that look promising but ultimately lead to a dramatic emotional breakdown. Gold by default, but if you want them to sniff for dirt, I'm not your conscience. Accepts block IDs (e.g. 'create:copper_ore') and #tags (e.g. '#c:ores/gold').")
    public List<String> sulkingOres = new ArrayList<>(List.of(
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore"
    ));

    @Translatable.Name("Item Stealing")
    @Translatable.Desc("For when your hamster develops a taste for the finer things in life. Can be configured so they steal or fetch any item— even from other mods. They steal diamonds and fetch acorns by default.")
    public ConfigGroup itemStealing = new ConfigGroup("itemStealing", true);

    @ConfigGroup.Pop
    @Translatable.Name("Thievery Pounce Chance")
    @Translatable.Desc("Probability (0.1 to 1.0) a hamster will succumb to temptation when seeing a stealable item. High by default, since the only default stealable item is a diamond.")
    public ValidatedFloat itemThieveryChance = new ValidatedFloat(0.75f, 1.0f, 0.1f);

    @Translatable.Name("Tag Game")
    @Translatable.Desc("Hamsters get bored. Sometimes they want to play. Configure the rules of engagement here.")
    public ConfigGroup tagGame = new ConfigGroup("tagGame", true);

    @Translatable.Name("Allow Stranger Danger")
    @Translatable.Desc("If true, hamsters can ask any player to play. If false, they can only ask their owners.")
    public boolean allowStrangerTag = true;

    @Translatable.Name("Game Initiation Chance")
    @Translatable.Desc("The 1-in-X chance per tick a hamster will start a game of tag with you if you make too much eye contact. Since the game runs at 20 ticks per second, a 1-in-500-tick chance means you'll need to maintain eye contact for about ~15 seconds on average. Set to 1 if you want the game to start instantly upon eye contact.")
    public ValidatedInt tagChanceDenominator = new ValidatedInt(500, 1200, 1);

    @Translatable.Name("Enable Player Daily Limit")
    @Translatable.Desc("If true, players are capped on how many games they can play per day. If false, you can play until your legs fall off.")
    public ValidatedBoolean enableTagGamePlayerLimit = new ValidatedBoolean(true);

    private final ValidatedField<Boolean> isTagLimitEnabled = enableTagGamePlayerLimit.map(b -> b, b -> b);

    @Translatable.Name("Max Games Per Day")
    @Translatable.Desc("How many times a single player can be 'It' per Minecraft day before telling the rodents to go find a hobby. Prevents infinite reward farming.")
    public ValidatedCondition<Integer> maxDailyTagGamesPerPlayer = new ValidatedInt(3, 100, 0)
            .toCondition(
                    isTagLimitEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.tag_limit_enabled"),
                    () -> 3
            );

    @ConfigGroup.Pop
    @Translatable.Name("Inter-Hamster Tag Duration")
    @Translatable.Desc("How long (in seconds) the chase lasts before the hamsters get bored and the game ends. This only applies if the Chaser fails to catch the Instigator.")
    public ValidatedInt interHamsterTagMaxDurationSeconds = new ValidatedInt(10, 60, 3);

    @Translatable.Name("Hide & Seek")
    @Translatable.Desc("Settings for when your hamster decides it's time to disappear into the woodwork.")
    public ConfigGroup hideAndSeek = new ConfigGroup("hideAndSeek", true);

    @Translatable.Name("Initiation Chance")
    @Translatable.Desc("1-in-X chance per tick to initiate a game of hide and seek. Default 7200 "
            + "(20 ticks per second, so default averages once every ~6 minutes).")
    public ValidatedInt hideAndSeekChanceDenominator = new ValidatedInt(7200, 72000, 20);

    @Translatable.Name("Min Duration")
    @Translatable.Desc("Minimum time (in seconds) the hamster will stay hidden.")
    public ValidatedInt hideAndSeekMinDurationSeconds = new ValidatedInt(45, 600, 1);

    @Translatable.Name("Max Duration")
    @Translatable.Desc("Maximum time (in seconds) before the hamster gets board, gives up and sulks.")
    public ValidatedInt hideAndSeekMaxDurationSeconds = new ValidatedInt(60, 600, 5);

    @Translatable.Name("Allow Storage Blocks")
    @Translatable.Desc("If true, hamsters can hide inside chests, barrels, and other valid blocks that have inventories. This checks whether the block implements an inventory interface, so it will work regardless of the content in the 'Valid Hiding Blocks' list.")
    public boolean allowInventoryHiding = true;

    @Translatable.Name("Valid Hiding Blocks")
    @Translatable.Desc("A list of blocks or tags where the hamster is allowed to hide.")
    public List<String> validHidingBlocks = new ArrayList<>(List.of("#adorablehamsterpets:bushes"));

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Hiding Blacklist")
    @Translatable.Desc("Blocks with inventories that the hamster should never hide inside. This overrides everything else.")
    public List<String> inventoryHidingBlacklist = new ArrayList<>(List.of("minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:dispenser", "minecraft:dropper", "minecraft:hopper", "minecraft:campfire", "minecraft:soul_campfire"));

    @Translatable.Name("Commissioned Features")
    @Translatable.Desc("Specialized, unofficial mechanics funded by supporters that don't necessarily fit the theme of the mod. Purposefully tucked away in the config to ensure most people don't notice them.")
    public ConfigGroup commissionedFeatures = new ConfigGroup("commissionedFeatures", true);

    // --- Ultimate Nightmare Redstone Fever ---
    @Translatable.Name("Surface Redstone Fever")
    @Translatable.Desc("Optional surface Redstone Fever settings. The settings here are subordinate to the global Redstone Fever feature toggle.")
    public ConfigGroup ultimateNightmareRedstoneFever = new ConfigGroup("ultimateNightmareRedstoneFever", true);

    @Translatable.Name("Enable Surface Fever")
    @Translatable.Desc("Allow healthy wild hamsters to randomly and instantly become infected with Redstone Fever when first approached by a Survival or Adventure player. The dice gets rolled once per hamster even if they are approached again later. So a hamster that doesn't turn on you after you approach can be trusted.")
    public ValidatedBoolean enableSurfaceSurpriseRedstoneFever = new ValidatedBoolean(false);

    private final ValidatedField<Boolean> isSurfaceSurpriseEnabled =
            enableSurfaceSurpriseRedstoneFever.map(value -> value, value -> value);

    @Translatable.Name("Infection Chance")
    @Translatable.Desc("Percentage chance that an undecided hamster contracts Redstone Fever on first approach.")
    public ValidatedCondition<Integer> surfaceSurpriseFeverChance = new ValidatedInt(25, 100, 0)
            .toCondition(
                    isSurfaceSurpriseEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.surface_surprise_enabled"),
                    () -> 25);

    @ConfigGroup.Pop
    @Translatable.Name("Trigger Distance")
    @Translatable.Desc("Distance in blocks at which the first eligible player causes the dice roll.")
    public ValidatedCondition<Integer> surfaceSurpriseRevealDistance = new ValidatedInt(8, 40, 1)
            .toCondition(
                    isSurfaceSurpriseEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.surface_surprise_enabled"),
                    () -> 8);

    @Translatable.Name("Hamster Riding Settings")
    @Translatable.Desc("Configure hamster-mounted cavalry. Tweak speeds, toggles, and physics. Don't blame me if you accidentally zoom off a cliff after turning up the speed too high.")
    public ConfigGroup hamsterRiding = new ConfigGroup("hamsterRiding", true);

    @Translatable.Name("Enable Hamster Riding")
    @Translatable.Desc("Adds a keybind to mount hamsters. (It's unbound by default). Allows riding any hamster, but you can only steer your own. \n\nCommissioned by @Saint_Victus.")
    public ValidatedBoolean enableMountableHamsters = new ValidatedBoolean(false);

    // Helper for condition: Maps the ValidatedBoolean to a ValidatedField<Boolean> for toCondition checks
    private final ValidatedField<Boolean> isRidingEnabled = enableMountableHamsters.map(b -> b, b -> b);

    @Translatable.Name("Base Ride Speed")
    @Translatable.Desc("The casual strolling speed multiplier. 0.25 is the default. Don't ask why.")
    public ValidatedCondition<Double> ridingBaseSpeedMultiplier = new ValidatedDouble(0.25, 0.8, 0.0)
            .toCondition(
                    isRidingEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.riding_enabled"),
                    () -> 0.25
            );

    @ConfigGroup.Pop
    @Translatable.Name("Sprint Ride Speed")
    @Translatable.Desc("The speed multiplier when sprinting. Hold on to your acorn hat. 0.35 is the default. I wish it was a nice round number, but alas, hamster riding is a complex enigma.")
    public ValidatedCondition<Double> ridingSprintSpeedMultiplier = new ValidatedDouble(0.35, 1.0, 0.0)
            .toCondition(
                    isRidingEnabled,
                    Component.translatable("config.adorablehamsterpets.condition.riding_enabled"),
                    () -> 0.8
            );

    @Translatable.Name("Feather Yeeting Settings")
    @Translatable.Desc("Configure the commissioned Feather Yeeting status effect.")
    public ConfigGroup featherYeeting = new ConfigGroup("featherYeeting", true);

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Throw Cooldown Reduction")
    @Translatable.Desc("Percentage removed from a hamster's base throw cooldown when the throwing player has Feather Yeeting. 0% preserves the base cooldown; 100% removes it.")
    public ValidatedInt featherYeetingCooldownReductionPercent = new ValidatedInt(50, 100, 0);
}
