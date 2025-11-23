package net.dawson.adorablehamsterpets.config;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import me.fzzyhmstrs.fzzy_config.annotations.NonSync;
import me.fzzyhmstrs.fzzy_config.annotations.RootConfig;
import me.fzzyhmstrs.fzzy_config.annotations.Translation;
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
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Root-level, single-file config for Adorable Hamster Pets.
 */
@Translatable.Name("Adorable Hamster Pets")
@Translatable.Desc("Questionable Configuration Options")
@RootConfig
public class AhpConfig extends Config {

    public AhpConfig() {
        super(Identifier.of(AdorableHamsterPets.MOD_ID, "main"));

        // --- Two-Way Binding for Announcement Icon Toggles ---
        // This block ensures the master "Enable Announcements" toggle stays synchronized
        // with the individual "Enable HUD Icon" and "Enable GUI Widget Icon" toggles.

        // --- 1. Initial State Synchronization ---
        // On config load, set the master toggle's state based on the children.
        // If either the HUD icon OR the widget icon is enabled, the master toggle should show "ON".
        enableNotificationIcons.setAndUpdate(enableHudIcon.get() || enableWidgetIcon.get());

        // --- 2. Master -> Children Update Listener ---
        // When the master "Enable Announcements" toggle is changed by the user in the GUI...
        enableNotificationIcons.listenToEntry(ignored -> {
            // Re-entrancy guard: Prevents an infinite loop where listeners trigger each other.
            if (updatingAnnouncementToggles) return;
            updatingAnnouncementToggles = true;
            try {
                boolean masterValue = enableNotificationIcons.get();
                // Push the master state down to both individual toggles.
                enableHudIcon.setAndUpdate(masterValue);
                enableWidgetIcon.setAndUpdate(masterValue);
            } finally {
                updatingAnnouncementToggles = false;
            }
        });

        // --- 3. Children -> Master Update Listener ---
        // A single listener to handle changes from either of the individual toggles.
        var childListener = (java.util.function.Consumer<?>) ignored -> {
            if (updatingAnnouncementToggles) return;
            // Recalculate what the master toggle's state *should* be.
            boolean newMasterState = enableHudIcon.get() || enableWidgetIcon.get();
            // Only update the master if its current state is out of sync.
            if (enableNotificationIcons.get() != newMasterState) {
                enableNotificationIcons.setAndUpdate(newMasterState);
            }
        };

        // Attach the listener to both individual toggles.
        enableHudIcon.listenToEntry(e -> childListener.accept(null));
        enableWidgetIcon.listenToEntry(e -> childListener.accept(null));
    }

    // --- Help & Other Distractions ---
    @Translatable.Name("Help & Other Distractions")
    @Translatable.Desc("Buttons for when you’re lost, bored, or met a bug that’s not just existential hamster angst.")
    public ConfigGroup helpAndResources = new ConfigGroup("helpAndResources", false);

    @NonSync
    @Translatable.Name("I Lost My Book!")
    public ConfigAction giveGuideBook = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.giveGuideBook"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.giveGuideBook.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_BOOK())
            .build(() -> {
                // On 1.20.1, use the ModPackets channel and the 1.20.1 inner packet record
                ModPackets.CHANNEL.sendToServer(new ModPackets.RequestGuidebookC2SPacket());
            });

    @NonSync
    @Translatable.Name("Report a Bug")
    public ConfigAction reportBug = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.reportBug"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.reportBug.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_LINK())
            .build(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://github.com/DawsonBodenhamer/AdorableHamsterPets-Public/issues"));

    @NonSync
    @Translatable.Name("Join Discord")
    public ConfigAction joinDiscord = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.joinDiscord"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.joinDiscord.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_BUTTON_CLICK())
            .build(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://discord.gg/w54mk5bqdf"));

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Visit My Website")
    public ConfigAction visitWebsite = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.visitWebsite"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.visitWebsite.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_LINK())
            .build(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://www.fortheking.design"));

    // --- UI & Quality of Life ---
    @Translatable.Name("UI & Quality of Life")
    @Translatable.Desc("Because Sanity is Overrated")
    public ConfigGroup uiPreferences = new ConfigGroup("uiPreferences", true);

    @NonSync
    @Translatable.Name("Auto Guidebook Delivery")
    @Translatable.Desc("Hand-delivers the sacred texts on first login. Read it—or don’t. I'm not your conscience.")
    public boolean enableAutoGuidebookDelivery = true;

    @NonSync
    @Translatable.Name("Mod Item Tooltips")
    @Translatable.Desc("Helpful whispers on what the heck that cucumber is for.")
    public boolean enableItemTooltips = true;

    @NonSync
    @Translatable.Name("Shoulder Dismount Messages")
    @Translatable.Desc("Little status mumbles when your co-pilot disembarks.")
    public boolean enableShoulderDismountMessages = true;

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Jade Hamster Debug Info")
    @Translatable.Desc("More stats than anyone asked for. Defaults to off—mercifully.")
    public boolean enableJadeHamsterDebugInfo = false;

    // --- Announcements & Update Notes ---
    @Translatable.Name("Announcements & Update Notes")
    @Translatable.Desc("Tweak the little bell icon that appears when I have something important to tell you. Or turn if off if you hate fun.")
    public ConfigGroup announcements = new ConfigGroup("announcements", true);

    @NonSync
    @Translatable.Name("Enable Announcements")
    @Translatable.Desc("The master switch for all announcement notifications. Turning this off is the same as clicking 'Disable All' in the announcement screen. Announcements can still be viewed in the §f§lHamster Tips§r guide book.")
    public ValidatedBoolean enableNotificationIcons = new ValidatedBoolean(true); // plain boolean; with special functionality wired it up in the constructor

    // Re-entrancy guard so the listeners don’t bounce events back and forth forever.
    private boolean updatingAnnouncementToggles = false;

    @NonSync
    @Translatable.Name("Mark All as Read")
    public ConfigAction markAllAsRead = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.announcements.markAllAsRead"))
            .desc(Text.translatable("config.adorablehamsterpets.main.announcements.markAllAsRead.desc"))
            .decoration(TextureIds.INSTANCE.getADD())
            .build(() -> {
                // Custom runnable 'pressAction'
                AnnouncementManager.INSTANCE.markAllAsRead();
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                            Text.translatable("message.adorablehamsterpets.announcements_marked_read").formatted(Formatting.GREEN),
                            false
                    );
                }
            });

    @NonSync
    @Translatable.Name("Announcement History")
    public ConfigAction resetAllAnnouncementDismissals = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.announcements.resetAllAnnouncementDismissals"))
            .desc(Text.translatable("config.adorablehamsterpets.main.announcements.resetAllAnnouncementDismissals.desc"))
            .decoration(TextureIds.INSTANCE.getRESTORE())
            .build(() -> {
                // Custom runnable 'pressAction'
                AnnouncementManager.INSTANCE.resetClientState();
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                            Text.translatable("message.adorablehamsterpets.announcements_reset").formatted(Formatting.GREEN),
                            false
                    );
                }
            });

    @NonSync
    @Translatable.Name("Snooze Timer (Days)")
    @Translatable.Desc("For when you see the update notification and think, 'That's a problem for future me.' Future you will be so proud. This is where you select many days to hide the 'Update Available' notification when you click 'Remind Me Later'.")
    public ValidatedInt snoozeUpdateReminderDays = new ValidatedInt(5, 14, 1);

    @NonSync
    @Translatable.Name("HUD Icon Settings")
    @Translatable.Desc("Options for the little bell with hamster ears that just hangs out in the corner of your screen when notifications are pending.")
    public ConfigGroup hudIconSettings = new ConfigGroup("hudIconSettings", true);

    @NonSync
    @Translatable.Name("Enable HUD Icon")
    @Translatable.Desc("Decide if the bell haunts you full-time on the HUD or only ambushes you when you're trying to organize your inventory. If disabled, you will only see notifications when you open an inventory.")
    public ValidatedBoolean enableHudIcon = new ValidatedBoolean(true);

    private final ValidatedField<Boolean> isHudIconEnabled = enableHudIcon.map(b -> b, b -> b);

    @NonSync
    @Translatable.Name("HUD Icon Position Preset")
    @Translatable.Desc("Banish the bell to a corner of your choosing. It's your screen. Establish dominance.")
    public ValidatedCondition<IconPositionPreset> hudIconPositionPreset =
            new ValidatedEnum<>(IconPositionPreset.TOP_LEFT)
                    .toCondition(
                            isHudIconEnabled,
                            Text.literal("Only available when 'Enable HUD Icon' is ON."),
                            () -> IconPositionPreset.TOP_LEFT
                    );

    @NonSync
    @Translatable.Name("HUD Icon Offset X")
    @Translatable.Desc("Shove the icon horizontally. For when 'top-left' isn't specific enough for your discerning taste.")
    public ValidatedCondition<Integer> hudIconOffsetX =
            new ValidatedInt(10, 500, -500)
                    .toCondition(
                            isHudIconEnabled,
                            Text.literal("Only available when 'Enable HUD Icon' is ON."),
                            () -> 10
                    );

    @NonSync
    @Translatable.Name("HUD Icon Offset Y")
    @Translatable.Desc("Adjust the vertical placement. Does it block your view? Is it not blocking your view enough? The power is yours.")
    public ValidatedCondition<Integer> hudIconOffsetY =
            new ValidatedInt(10, 500, -500)
                    .toCondition(
                            isHudIconEnabled,
                            Text.literal("Only available when 'Enable HUD Icon' is ON."),
                            () -> 10
                    );

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("HUD Icon Scale")
    @Translatable.Desc("Make it bigger. Make it smaller. Make it an affront to good taste. I'm not your art director.")
    public ValidatedCondition<Float> hudIconScale =
            new ValidatedFloat(1.0f, 3.0f, 0.5f)
                    .toCondition(
                            isHudIconEnabled,
                            Text.literal("Only available when 'Enable HUD Icon' is ON."),
                            () -> 1.0f
                    );

    @NonSync
    @Translatable.Name("Widget Icon Settings")
    @Translatable.Desc("Configure the bell with hamster ears that haunts the corners of your inventory screens.")
    public ConfigGroup widgetIconSettings = new ConfigGroup("widgetIconSettings", true);

    @NonSync
    @Translatable.Name("Enable GUI Widget Icon")
    @Translatable.Desc("Decide if the bell icon should ambush you while you're sorting your inventory. If disabled, it will only bother you on the main menu or the game HUD if you have those enabled. Your screen, your rules.")
    public ValidatedBoolean enableWidgetIcon = new ValidatedBoolean(true);

    private final ValidatedField<Boolean> isWidgetIconEnabled = enableWidgetIcon.map(b -> b, b -> b);

    /**
     * A Plain-Old-Java-Object (POJO) to encapsulate the X and Y offset settings
     * for the announcement icon widget. This is wrapped by ValidatedAny to create
     * a pop-up "mini-config" screen.
     * <p>
     * NOTE FOR FUTURE SELF:
     * Without @Translation, each instance would look up separate, instance-specific
     * lang keys (based on the field path), or fall back to the annotation text.
     * <p>
     * Adding @Translation with a shared prefix forces BOTH instances to use the same
     * language keys:
     *   adorablehamsterpets.main.widgetIconOffsets.offsetX(.desc)
     *   adorablehamsterpets.main.widgetIconOffsets.offsetY(.desc)
     * This keeps the lang file DRY and guarantees consistent labels/tooltips across
     * all WidgetIconOffsets popups.
     */
    @Translation(prefix = "adorablehamsterpets.main.widgetIconOffsets")
    public static class WidgetIconOffsets {
        @NonSync
        @Translatable.Name("Offset X")
        @Translatable.Desc("Shove it sideways (in pixels). Increase the number to move it right, decrease to move left.")
        public ValidatedInt offsetX = new ValidatedInt(0, 500, -500);

        @NonSync
        @Translatable.Name("Offset Y")
        @Translatable.Desc("Shove it vertically (in pixels). Increase the number to move it down, decrease to move up.")
        public ValidatedInt offsetY = new ValidatedInt(0, 500, -500);
    }

    @NonSync
    @Translatable.Name("Survival Inventory")
    @Translatable.Desc("Control the icon's placement for standard GUIs. Because nothing says 'immersion' like a perfectly aligned notification bell. Position is relative to the GUI's top-right corner.")
    public ValidatedCondition<WidgetIconOffsets> survivalWidgetIconSettings =
            new ValidatedAny<>(new WidgetIconOffsets())
                    .toCondition(
                            isWidgetIconEnabled,
                            Text.literal("Only available when 'Enable GUI Widget Icon' is ON."),
                            WidgetIconOffsets::new
                    );

    @NonSync
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Creative Inventory")
    @Translatable.Desc("Control the icon's placement for the creative mode GUI. Because nothing says 'immersion' like a perfectly aligned notification bell. Position is relative to the GUI's top-right corner.")
    public ValidatedCondition<WidgetIconOffsets> creativeWidgetIconSettings =
            new ValidatedAny<>(new WidgetIconOffsets())
                    .toCondition(
                            isWidgetIconEnabled,
                            Text.literal("Only available when 'Enable GUI Widget Icon' is ON."),
                            WidgetIconOffsets::new
                    );


    // --- Falling Leaf Settings ---
    @Translatable.Name("Falling Leaf Settings")
    @Translatable.Desc("Here's where you tweak the behavior of the floaty leaf particles spawned from Hamster Bedding. Didn't know you could spawn particles from Hamster Bedding? Try to keep up.")
    public ConfigGroup particleEffects = new ConfigGroup("particleEffects", true);

    @NonSync
    @Translatable.Name("Dynamic Drift")
    @Translatable.Desc("Should the gentle, drift of Hamster Bedding leaf particles slowly change direction over time? If true, it's a slow, majestic rotation. (It takes a bout 3 minutes to make a full 360 degree rotation). If false, you get to pick a static wind direction below.")
    public ValidatedBoolean enableDynamicDriftAngle = new ValidatedBoolean(true);

    // Helper field to gate the static angle slider. This is true when the dynamic toggle is OFF.
    private final ValidatedField<Boolean> isDynamicDriftDisabled =
            enableDynamicDriftAngle.map(value -> !value, value -> !value);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Static Drift Angle")
    @Translatable.Desc("Set a fixed direction for the universal leaf drift (0-360 degrees). 0/360 is East, 90 is South, 180 is West, 270 is North, I think. Or just slide it until it looks cool. Whatever. Only works if 'Dynamic Drift' is off.")
    public ValidatedCondition<Integer> staticDriftAngle =
            new ValidatedInt(0, 360, 0)
                    .toCondition(
                            isDynamicDriftDisabled,
                            Text.literal("Only available when 'Dynamic Leaf Drift' is OFF."),
                            () -> 0
                    );

    // --- Core Feature Toggles ---
    @Translatable.Name("Core Feature Toggles")
    @Translatable.Desc("Fundamental hamster hijinks— fiddle at your own risk.")
    public ConfigGroup core = new ConfigGroup("core", true);

    @Translatable.Name("Enable Hamster Throwing")
    @Translatable.Desc("Do we yeet the hamster? ('G' by default).")
    public boolean enableHamsterThrowing = true;

    @Translatable.Name("Enable Wander Mode")
    @Translatable.Desc("For when you need some personal space. Allows tamed hamsters to be linked to a Hamster Bed, letting them wander freely within a set radius instead of clinging to you like melted duct-tape. You're welcome.")
    public ValidatedBoolean enableWanderMode = new ValidatedBoolean(true);

    @Translatable.Name("Require Food Mix to Unlock Cheeks")
    @Translatable.Desc("Gate cheek-pouch storage behind gourmet cuisine, because drama.")
    public boolean requireFoodMixToUnlockCheeks = true;

    @Translatable.Name("Use 'Hampter' as Default Name")
    @Translatable.Desc("Changes the default entity name from 'Hamster' to 'Hampter'. Note: This has no visible effect in vanilla Minecraft, as mobs don't show nameplates by default. It's primarily for use with mods like Auto Leveling that display entity names.")
    public boolean useHampterName = false;

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

    @Translatable.Name("Cleaning Frequency")
    @Translatable.Desc("How often a sitting hamster gets the sudden urge to clean. It's a 1-in-X chance per tick, so lower numbers mean a higher chance for cleaning. For example, 1200 means on average, it'll clean about once a minute. 300 ≈ every 15 secs, and 5000 ≈ every 4 mins. Congratulations— now you know enough to be dangerous.")
    public ValidatedInt cleaningChanceDenominator = new ValidatedInt(1200, 5000, 300);

    @Translatable.Name("Throw Cooldown (Ticks)")
    @Translatable.Desc("Time-out after using your living projectile. (20 ticks = 1 s)")
    public ValidatedInt hamsterThrowCooldown = new ValidatedInt(2400, 20 * 60 * 10, 20);

    @Translatable.Name("Green Bean Buff Cooldown (Ticks)")
    @Translatable.Desc("When the sugar rush ends, force a breather. (20 ticks = 1 s)")
    public ValidatedInt steamedGreenBeansBuffCooldown = new ValidatedInt(6000, 20 * 60 * 10, 20);

    @Translatable.Name("Enable Diamond Seeking Cooldown?")
    @Translatable.Desc("Force a cool-down after striking it rich. Off by default, since this can't happen again anyway without another mount/dismount on the shoulder.")
    public boolean enableIndependentDiamondSeekCooldown = false;

    @Translatable.Name("Diamond Seeking Cooldown (Ticks)")
    @Translatable.Desc("Cooldown before your hamster can go on another treasure hunt. (20 ticks = 1 s)")
    public ValidatedInt independentOreSeekCooldownTicks = new ValidatedInt(2400, 6000, 20);

    @Translatable.Name("Diamond Thievery Cooldown (Ticks)")
    @Translatable.Desc("Mandatory time-out after a successful heist to prevent serial kleptomania. (20 ticks = 1s). WARNING: Increasing this cooldown can dramatically change the diamond stealing mechanic, since that AI goal sometimes re-runs multiple times in a row when the hamster has trouble pathfinding to the item that it wants to steal. So instead of increasing this, you should probably just stop dropping your diamonds on the ground everywhere, butter fingers.")
    public ValidatedInt stealCooldownTicks = new ValidatedInt(100, 6000, 20);

    @ConfigGroup.Pop
    @Translatable.Name("Breeding Cooldown (Ticks)")
    @Translatable.Desc("Hamsters need their space. (20 ticks = 1 s)")
    public ValidatedInt breedingCooldownTicks = new ValidatedInt(6000, 24000, 600);

    // --- Core Item Tag Overrides ---
    @Translatable.Name("Core Item Tag Overrides")
    @Translatable.Desc("For the advanced user who looks at a perfectly functional system and thinks, 'I can make this weirder.' Edit these lists to change what items your hamsters consider food, bait, treasure, and all other interactions. Use item IDs (e.g., 'minecraft:diamond') or tags (e.g., '#minecraft:fishes'). Mess it up? That's a you problem.")
    public ConfigGroup itemTags = new ConfigGroup("itemTags", true);

    @Translatable.Name("Taming Baits")
    @Translatable.Desc("The official list of bribes for convincing wild fluffballs to join your cause. By default, it's just sliced cucumbers. Feel free to add 'minecraft:nether_star' if you enjoy making poor life choices. Compatible with Cultural Delights by default!")
    public List<String> tamingFoods = new ArrayList<>(List.of("adorablehamsterpets:sliced_cucumber", "culturaldelights:cut_cucumber"));

    @Translatable.Name("Standard Diet")
    @Translatable.Desc("The hamster's everyday menu. These items will heal them or, if they're at full health, might give them... ideas about starting a family. Don't make it weird.")
    public List<String> standardFoods = new ArrayList<>(List.of(
            "adorablehamsterpets:hamster_food_mix", "adorablehamsterpets:sunflower_seeds", "adorablehamsterpets:green_beans",
            "adorablehamsterpets:cucumber", "adorablehamsterpets:green_bean_seeds", "adorablehamsterpets:cucumber_seeds",
            "minecraft:apple", "minecraft:carrot", "minecraft:melon_slice", "minecraft:sweet_berries",
            "minecraft:beetroot", "minecraft:wheat", "minecraft:wheat_seeds",

            // Farmer's Delight
            "farmersdelight:cabbage_leaf", "farmersdelight:cabbage_seeds",
            "farmersdelight:tomato_seeds", "farmersdelight:cooked_rice",
            "farmersdelight:pumpkin_slice",

            // Cultural Delights
            "culturaldelights:cut_cucumber", "culturaldelights:cucumber_seeds", "culturaldelights:corn_kernels"
    ));

    @Translatable.Name("High-Value Heistables")
    @Translatable.Desc("The list of items a hamster might try to... 'borrow' if you leave them on the ground. A chase will ensue. You have been warned.")
    public List<String> stealableItems = new ArrayList<>(List.of("minecraft:diamond"));

    @Translatable.Name("Performance-Enhancers")
    @Translatable.Desc("The list of questionable substances that grant your hamster temporary superpowers. By default, it's just steamed green beans.")
    public List<String> buffFoods = new ArrayList<>(List.of("adorablehamsterpets:steamed_green_beans"));

    @Translatable.Name("Lure Items")
    @Translatable.Desc("The list of specific items that convinces a tamed hamster your shoulder is the best seat in the house or lures them to their linked bed. Defaults to cheese, because of course it does.")
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

    @Translatable.Name("Passively Munchable Snacks")
    @Translatable.Desc("The specific items a hamster will eat directly from its cheek pouch to heal itself when injured. Keep it exclusive, or let them feast on enchanted apples. Your call.")
    public List<String> autoHealFoods = new ArrayList<>(List.of("adorablehamsterpets:hamster_food_mix"));

    @Translatable.Name("Cheek Pouch Smuggling List")
    @Translatable.Desc("Fine-tune exactly what your hamster is (and isn't) allowed to carry. The 'Allowed' list acts as a high-priority override to the 'Disallowed' lists and general rules.")
    public ConfigGroup pouchRestrictions = new ConfigGroup("pouchRestrictions", true);

    @Translatable.Name("Allowed Items")
    @Translatable.Desc("A specific list of items and tags that are allowed in the hamster's cheek pouch. You can add things to this list to bypass the default 'no tools or big blocks' rule, since this overrides the 'disallowed' settings.")
    public List<String> pouchAllowedItems = new ArrayList<>(List.of(
            "minecraft:torch", "minecraft:soul_torch", "minecraft:redstone_torch",
            "minecraft:repeater", "minecraft:comparator", "minecraft:lever",
            "#minecraft:buttons", "#minecraft:rails",
            "#minecraft:pressure_plates"
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
            "minecraft:end_crystal", "minecraft:spyglass", "minecraft:nether_star", "minecraft:dragon_egg", "minecraft:bundle",
            "adorablehamsterpets:hamster_guide_book"
    ));

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Pouch Disallowed Tags")
    @Translatable.Desc("A list of item tags that are NEVER allowed in the cheek pouch, unless they are on the 'Allowed' list above. A broad-spectrum approach to preventing your hamster from swallowing an entire sword.")
    public List<String> pouchDisallowedTags = new ArrayList<>(List.of(
            "#minecraft:axes", "#minecraft:hoes", "#minecraft:pickaxes", "#minecraft:shovels", "#minecraft:swords",
            "#minecraft:trimmable_armor", "#minecraft:beds", "#minecraft:banners", "#minecraft:doors",
            "#minecraft:boats"
    ));

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

    @Translatable.Name("Taming Chance")
    @Translatable.Desc("Convince a hamster to love you. Taming difficulty (1 in X chance). Higher = more cucumbers sacrificed to fuzzy freeloaders.")
    public ValidatedInt tamingChanceDenominator = new ValidatedInt(3, 20, 1);

    @Translatable.Name("Melee Damage")
    @Translatable.Desc("Tamed hamster melee damage. Squeak-first, ask questions later.")
    public ValidatedDouble meleeDamage = new ValidatedDouble(2.0, 40.0, 0.0);

    @ConfigGroup.Pop
    @Translatable.Name("Throw Damage")
    @Translatable.Desc("Damage dealt by thrown hamster. Surprisingly effective against Creepers. How convenient.")
    public ValidatedDouble hamsterThrowDamage = new ValidatedDouble(20.0, 40.0, 0.0);

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
    @Translatable.Desc("A list of 'c:' convention biome tags where hamsters can spawn. Used for broad mod compatibility. By default, this includes most overworld tags as a 'catch-all', and the filtering for different hamster variants in each biome is hard coded.")
    public List<String> spawnBiomeConventionTags = new ArrayList<>(List.of(
            "c:is_cold",
            "c:is_hot",
            "c:is_temperate",
            "c:is_dry",
            "c:is_wet",
            "c:is_dense_vegetation",
            "c:is_sparse_vegetation"
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
            "c:is_icy"
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
            "c:is_magical", "c:is_mushroom", "terralith:mystical"
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
            "c:is_cold", "c:is_snowy"
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
            "c:is_mountain", "c:is_sparse_vegetation", "terralith:cliffs"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> grayExclusionBiomes = new ArrayList<>(List.of(
            "namespace:id"
    ));
    @ConfigGroup.Pop
    @Translatable.Name("Excluded Tags")
    public List<String> grayExclusionTags = new ArrayList<>(List.of(
            "minecraft:is_badlands", "minecraft:is_jungle", "minecraft:is_savanna", "c:is_sandy"
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
            "c:is_wet", "c:is_cave"
    ));
    @Translatable.Name("Excluded Biomes")
    public List<String> blackExclusionBiomes = new ArrayList<>(List.of(
            "minecraft:dripstone_caves", "minecraft:lush_caves"
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
            "c:is_sandy"
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
            "c:is_forest", "c:is_dense_vegetation"
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
            "c:tag_name"
    ));

    @Translatable.Name("Bed & Wander Mode Settings")
    @Translatable.Desc("For when 'following you into lava' is no longer a desirable trait. Tweak the settings for your hamster's newfound, bed-based independence.")
    public ConfigGroup wanderMode = new ConfigGroup("wanderMode", true);

    @Translatable.Name("Avoid Unlinked Beds")
    @Translatable.Desc("Should hamsters treat other hamsters' beds as sacred ground? If true, they'll try to politely path around them, but they will only try a few alternate paths before their tiny rodent-patience runs out. If false, they'll trample wherever they please.")
    public boolean avoidUnlinkedBeds = true;

    @Translatable.Name("Default Wander Distance")
    @Translatable.Desc("The initial wander distance set when a hamster is first linked to a bed. It defaults to medium, because that is the universally accepted starting point for all life choices.")
    public ValidatedEnum<WanderDistance> defaultWanderDistance = new ValidatedEnum<>(WanderDistance.MEDIUM);

    @Translatable.Name("Wander Distance: Near (Blocks)")
    @Translatable.Desc("The radius for the 'Near' wander distance setting. For the clingy hamster who wants freedom, but not too much.")
    public ValidatedInt wanderDistanceNear = new ValidatedInt(8, 64, 1);

    @Translatable.Name("Wander Distance: Medium (Blocks)")
    @Translatable.Desc("The radius for the 'Medium' wander distance setting. A respectable distance. Not too close, not too far. Perfectly balanced, as all things should be.")
    public ValidatedInt wanderDistanceMedium = new ValidatedInt(16, 64, 1);

    @Translatable.Name("Wander Distance: Far (Blocks)")
    @Translatable.Desc("The radius for the 'Far' wander distance setting. For the adventurous hamster who might send you a postcard someday. Maybe.")
    public ValidatedInt wanderDistanceFar = new ValidatedInt(32, 64, 1);

    @Translatable.Name("Bed Break Notification")
    @Translatable.Desc("Get an action bar message when your hamster's bed is broken. Here's where you can turn it off if you prefer... complete immersion.")
    public boolean enableBedBreakMessage = true;

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
                    Text.literal("Only available when 'Allow Sleeping in Bed' is true."),
                    () -> false
            );

    // Helper field to gate the min and max time settings
    private final ValidatedField<Boolean> isCircadianChaosEnabled = circadianChaos.map(b -> b, b -> b);

    @Translatable.Name("Min Nap Interval")
    @Translatable.Desc("The shortest possible time (in seconds) a hamster will stay awake or asleep in bed before considering a change. Defaults to 5 minutes— for the truly narcoleptic rodent. A random duration between the min and max is chosen each time, so move them further apart to increase randomness.")
    public ValidatedCondition<Integer> minNapInBedIntervalSeconds  = new ValidatedInt(300, 7000, 5)
            .toCondition(
                    () -> allowSleepInBed.get() && circadianChaos.get(),
                    Text.literal("Only available when 'Enable Circadian Chaos' is ON."),
                    () -> 300
            );

    @Translatable.Name("Max Nap Interval")
    @Translatable.Desc("The longest amount of time (in seconds) a hamster can possibly stay awake or asleep in bed before it gets bored and switches things up. Defaults to 10 minutes.")
    public ValidatedCondition<Integer> maxNapInBedIntervalSeconds = new ValidatedInt(600, 7200, 10)
            .toCondition(
                    () -> allowSleepInBed.get() && circadianChaos.get(),
                    Text.literal("Only available when 'Enable Circadian Chaos' is ON."),
                    () -> 900
            );

    @Translatable.Name("Sleep During the Day")
    @Translatable.Desc("If false, wandering hamsters will sleep in their beds during the night. If true, they'll adopt a more nocturnal, goth-adjacent lifestyle and sleep in the daytime.")
    public ValidatedCondition<Boolean> sleepDuringDay = new ValidatedBoolean(true)
            .toCondition(
                    () -> allowSleepInBed.get() && !circadianChaos.get(),
                    Text.literal("Overridden by 'Enable Circadian Chaos' or disabled by 'Allow Sleeping in Bed'."),
                    () -> true
            );

    @ConfigGroup.Pop
    @Translatable.Name("Manual Wake-Up Duration")
    @Translatable.Desc("The mandatory grumpiness period if you rudely awaken a hamster from its bed before it was ready. It won't go back to sleep until this timer runs out. (20 ticks = 1 second)")
    public ValidatedCondition<Integer> bedWakeUpCooldown = new ValidatedInt(300, 1200, 20)
            .toCondition(
                    isSleepInBedAllowed,
                    Text.literal("Only available when 'Allow Sleeping in Bed' is ON."),
                    () -> 300
            );

    // --- Shoulder Hamster Settings ---
    @Translatable.Name("Shoulder Hamster Settings")
    @Translatable.Desc("Settings for your fuzzy parrot of doom.")
    public ConfigGroup shoulder = new ConfigGroup("shoulder", true);

    @Translatable.Name("Core Settings")
    @Translatable.Desc("Just the basic stuff. You know, detecting creepers, sniffing diamonds. Just average Minecraft stuff really. No big deal. Why are you clapping and squealing? Stop that. You look silly.")
    public ConfigGroup shoulderCore = new ConfigGroup("shoulderCore", true);

    @Translatable.Name("Retain Shoulder Mounts")
    @Translatable.Desc("If true, any hamsters on your shoulder will remain there when you respawn. If false (default), they will remain at your death location, passed out from the sheer shock of seeing you die. They may need a quick pat to wake them up when you return.")
    public boolean keepHamstersOnShoulderOnDeath = false;

    @Translatable.Name("Consume Lure Item")
    @Translatable.Desc("Should luring a hamster to your shoulder consume the item (e.g., cheese)? Turn this off if you believe your charm alone should be enough. The item will still be required, just not eaten.")
    public boolean consumeLureItem = true;

    @Translatable.Name("Enable Force-Mount Keybind")
    @Translatable.Desc("Tired of wasting perfectly good cheese? Enable this to use a dedicated keybind (unbound by default). Hold down this key while right-clicking your hamster to hoist them onto your shoulder, no questions asked. Uses a separate key you must set in Controls > Key Binds.")
    public boolean enableShoulderMountKeybind = false;

    @NonSync
    @Translatable.Name("Enable Creeper Detection")
    @Translatable.Desc("May save your inventory. Or your ears.")
    public boolean enableShoulderCreeperDetection = true;

    @NonSync
    @Translatable.Name("Creeper Detection Radius")
    @Translatable.Desc("Adjust paranoia levels. (Distance in blocks)")
    public ValidatedDouble shoulderCreeperDetectionRadius = new ValidatedDouble(16.0, 16.0, 1.0);

    @NonSync
    @Translatable.Name("Enable Diamond Detection")
    @Translatable.Desc("Because who doesn’t enjoy unsolicited financial advice from a rodent?")
    public boolean enableShoulderDiamondDetection = true;

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
    @Translatable.Name("Dismount Button")
    @Translatable.Desc("Choose what action dismounts the hamster. 'SNEAK_KEY' uses your sneak key, obviously. 'CUSTOM_KEYBIND' uses a separate key you must set in Controls > Key Binds.")
    public DismountTriggerType dismountTriggerType = DismountTriggerType.SNEAK_KEY;

    @NonSync
    @Translatable.Name("Button‑Press Behavior")
    @Translatable.Desc("Choose whether a single press or a quick double‑tap dismounts the hamster.")
    public ValidatedEnum<DismountPressType> dismountPressType =
            new ValidatedEnum<>(DismountPressType.SINGLE_PRESS);

    private final ValidatedField<Boolean> isDoubleTap =
            dismountPressType.map(
                    pt -> pt == DismountPressType.DOUBLE_TAP,
                    b -> b ? DismountPressType.DOUBLE_TAP : DismountPressType.SINGLE_PRESS
            );

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Double-Tap Delay (Ticks)")
    @Translatable.Desc("Max time between sneak key presses to count as a double-tap. (20 ticks = 1 second)")
    public ValidatedCondition<Integer> doubleTapDelayTicks =
            new ValidatedInt(10, 40, 5)
                    .toCondition(
                            isDoubleTap,
                            Text.literal("Only available when Button-Press Behavior is set to DOUBLE_TAP."),
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
    @Translatable.Name("Forced Animation State")
    @Translatable.Desc("If dynamic animations are disabled, choose the single state shoulder pets should remain in. Sometimes this setting can have a delay before kicking in, but if it doesn't seem to be working at all, try switching the 'Forced State' from one option to another. Usually this just makes it \"work.\" I'm not sure why lol")
    public ValidatedCondition<ForcedShoulderState> forcedShoulderState =
            new ValidatedEnum<>(ForcedShoulderState.ALWAYS_STAND)
                    .toCondition(
                            // use the inverted validated field as the gating condition
                            dynamicShoulderDisabled,
                            // message shown when the condition fails
                            Text.literal("Only available when 'Enable Dynamic Shoulder Animations' is turned OFF."),
                            // fallback when the condition fails
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

    // --- Hamster Yeet Settings ---
    @Translatable.Name("Hamster Yeet Settings")
    @Translatable.Desc("For when you need a furry, surprisingly aerodynamic solution.")
    public ConfigGroup yeetSettings = new ConfigGroup("yeetSettings", true);

    @Translatable.Name("Throw Velocity")
    @Translatable.Desc("The base throw speed of your furry projectile.")
    public ValidatedDouble hamsterThrowVelocity = new ValidatedDouble(1.5, 5.0, 0.1);

    @ConfigGroup.Pop
    @Translatable.Name("Throw Velocity (Buffed)")
    @Translatable.Desc("The throw speed of your furry projectile when under the influence of Steamed Green Beans. Goes from 'yeet' to 'yote'.")
    public ValidatedDouble hamsterThrowVelocityBuffed = new ValidatedDouble(2.5, 5.0, 0.1);

    // --- Tamed Sleep Settings ---
    @Translatable.Name("Tamed Sleep Settings")
    @Translatable.Desc("Even digital rodents need beauty sleep— adjust according to your patience levels.")
    public ConfigGroup tamedSleepSettings = new ConfigGroup("tamedSleepSettings", true);

    @NonSync
    @Translatable.Name("Threat Radius (Blocks)")
    @Translatable.Desc("How close a hostile mob can get before a hamster wakes up from it's power nap.")
    public ValidatedInt tamedSleepThreatDetectionRadiusBlocks = new ValidatedInt(8, 32, 1);

    @NonSync
    @Translatable.Name("Require Daytime?")
    @Translatable.Desc("Choose when your sitting hamster will succumb to drowsiness. 'True' means your sitting hamster will only doze off during the day— 'false' means it can doze off anytime. This setting does not affect the behavior of a hamster when sleeping in a bed.")
    public boolean requireDaytimeForTamedSleep = true;

    @NonSync
    @Translatable.Name("Min Sit Time Before Drowsy (Secs)")
    @Translatable.Desc("Minimum seconds before a sitting hamster gets sleepy.")
    public ValidatedInt tamedQuiescentSitMinSeconds = new ValidatedInt(120, 300, 1);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Max Sit Time Before Drowsy (Secs)")
    @Translatable.Desc("Maximum seconds before the inevitable deep snooze.")
    public ValidatedInt tamedQuiescentSitMaxSeconds = new ValidatedInt(180, 600, 2);

    // --- Food Healing Settings ---
    @Translatable.Name("Food Healing Settings")
    @Translatable.Desc("Nutrition— isn't it wonderful. Tweaks to snacks.")
    public ConfigGroup foodHealing = new ConfigGroup("foodHealing", true);

    @Translatable.Name("Food Mix")
    @Translatable.Desc("Healing amount from Hamster Food Mix. The good stuff.")
    public ValidatedFloat hamsterFoodMixHealing = new ValidatedFloat(4.0f, 10.0f, 0.0f);

    @ConfigGroup.Pop
    @Translatable.Name("Standard Food")
    @Translatable.Desc("Healing from basic seeds/crops. Better than nothing… probably.")
    public ValidatedFloat standardFoodHealing = new ValidatedFloat(2.0f, 5.0f, 0.0f);

    // --- Cheese Food Settings ---
    @Translatable.Name("Cheese Settings")
    @Translatable.Desc("Cheese... the gooey wonder. Some people think it's overpowered. I disagree. Obviously.")
    public ConfigGroup cheeseHealing = new ConfigGroup("cheeseHealing", true);

    @Translatable.Name("Cheese Nutrition")
    @Translatable.Desc("How many little hunger shanks the cheese restores. Vanilla cooked steak is 8. I know you're thinking of moving it to 20, you monster.")
    public ValidatedInt cheeseNutrition = new ValidatedInt(8, 20, 0);

    @ConfigGroup.Pop
    @Translatable.Name("Cheese Saturation")
    @Translatable.Desc("How long the hunger effect lasts. Cooked steak is 0.8. Don't get too crazy. Or do. I'm not your conscience.")
    public ValidatedFloat cheeseSaturation = new ValidatedFloat(0.8f, 2.0f, 0.0f);

    // --- Green Bean Buff Settings ---
    @Translatable.Name("Green Bean Buff Settings")
    @Translatable.Desc("Nutrition, but make it dramatic. Tweaks to caffeine-bean highs.")
    public ConfigGroup greenBeanBuffs = new ConfigGroup("greenBeanBuffs", true);

    @Translatable.Name("Duration (Ticks)")
    @Translatable.Desc("Steamed beans: power that fades faster than your attention span.")
    public ValidatedInt greenBeanBuffDuration = new ValidatedInt(3600, 20 * 60 * 10, 20);

    @Translatable.Name("Speed Level")
    @Translatable.Desc("Because someone has to go fast.")
    public ValidatedInt greenBeanBuffAmplifierSpeed = new ValidatedInt(1, 4, 0);

    @Translatable.Name("Strength Level")
    @Translatable.Desc("Slightly mightier nibbles.")
    public ValidatedInt greenBeanBuffAmplifierStrength = new ValidatedInt(1, 4, 0);

    @Translatable.Name("Absorption Level")
    @Translatable.Desc("Extra fluff padding for those daring dives.")
    public ValidatedInt greenBeanBuffAmplifierAbsorption = new ValidatedInt(1, 4, 0);

    @ConfigGroup.Pop
    @Translatable.Name("Regen Level")
    @Translatable.Desc("Heals minor paper-cuts (and fragile egos).")
    public ValidatedInt greenBeanBuffAmplifierRegen = new ValidatedInt(0, 4, 0);

    // --- Independent Diamond Seeking Settings ---
    @Translatable.Name("Independent Diamond Seeking Settings")
    @Translatable.Desc("Unleash free-range prospectors. What could go wrong?")
    public ConfigGroup independentDiamondSeeking = new ConfigGroup("independentDiamondSeeking", true);

    @Translatable.Name("Enable Independent Diamond Seeking")
    @Translatable.Desc("Permit hamsters to embark on solo get-rich-quick schemes?")
    public boolean enableIndependentDiamondSeeking = true;

    @Translatable.Name("Diamond Seek Scan Radius (Blocks)")
    @Translatable.Desc("How far a hamster scans once it’s decided to play prospector.")
    public ValidatedInt diamondSeekRadius = new ValidatedInt(10, 20, 5);

    @ConfigGroup.Pop
    @Translatable.Name("Gold 'Mistake' Chance")
    @Translatable.Desc("The probability (0.0 to 1.0) that a hamster will seek gold instead of diamond, if both are available. At 0.5, it's a coin toss. At 1.0, it's guaranteed hamster sulking.")
    public ValidatedFloat goldMistakeChance = new ValidatedFloat(0.33f, 1.0f, 0.0f);

    // --- Diamond Stealing Behavior Settings---
    @Translatable.Name("Diamond Stealing Behavior Settings")
    @Translatable.Desc("For when your hamster develops a taste for the finer things in life. Can be configured so they steal any item— even from other mods, but they only steal diamonds by default.")
    public ConfigGroup diamondStealing = new ConfigGroup("diamondStealing", true);

    @Translatable.Name("Enable Diamond Stealing")
    @Translatable.Desc("Permits hamsters to engage in spontaneous, high-stakes games of keep-away with your valuables. A chase ensues. Obviously.")
    public boolean enableDiamondStealing = true;

    @Translatable.Name("Pounce Chance")
    @Translatable.Desc("Probability (0.1 to 1.0) a hamster will succumb to temptation. High by default. You shouldn't leave your diamonds lying around anyway.")
    public ValidatedFloat diamondPounceChance = new ValidatedFloat(0.75f, 1.0f, 0.1f);

    @Translatable.Name("Minimum Flee Distance (Blocks)")
    @Translatable.Desc("The hamster's personal space bubble.")
    public ValidatedInt minFleeDistance = new ValidatedInt(5, 20, 1);

    @Translatable.Name("Maximum Flee Distance (Blocks)")
    @Translatable.Desc("The maximum distance before the hamster gets bored and stops running to taunt you.")
    public ValidatedInt maxFleeDistance = new ValidatedInt(20, 40, 5);

    @Translatable.Name("Minimum Steal Duration (Seconds)")
    @Translatable.Desc("The shortest amount of time the hamster will entertain this little game before getting bored and dropping your stuff.")
    public ValidatedInt minStealDurationSeconds = new ValidatedInt(5, 240, 1);

    @ConfigGroup.Pop
    @Translatable.Name("Maximum Steal Duration (Seconds)")
    @Translatable.Desc("The longest your cardio session can last before the hamster's attention span gives out.")
    public ValidatedInt maxStealDurationSeconds = new ValidatedInt(15, 300, 5);

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

    @Translatable.Name("Sunflower Seed Regrowth Speed")
    @Translatable.Desc("Higher = slower, lower = faster. Photosynthesis is hard, okay?")
    public ValidatedDouble sunflowerRegrowthModifier = new ValidatedDouble(1.0, 5.0, 0.1);

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("A list of biome tags where these custom Sunflowers can replace vanilla ones. The 'c:is_plains' tag provides wide compatibility with modded biomes.")
    public List<String> sunflowerBiomeTags = new ArrayList<>(List.of(
            "c:is_plains",
            "c:is_temperate",
            "c:is_hot",
            "c:is_dry"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Specific Biomes")
    @Translatable.Desc("Specific biome IDs where these sunflowers can replace the vanilla ones. Format: 'mod_id:biome_name'. They’re picky.")
    public List<String> sunflowerBiomes = new ArrayList<>(List.of("minecraft:sunflower_plains"));

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
    @Translatable.Desc("Convention tags for maximum mod-pack harmony. Format: 'c:tag_name', for example: 'c:is_temperate'.")
    public List<String> cucumberBushConventionTags = new ArrayList<>(List.of(
            "c:is_temperate",
            "c:is_hot",
            "c:is_dry"
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
    @Translatable.Desc("Convention tags for mod-friendly bean spam. Format: 'c:tag_name', for example: 'c:is_wet'.")
    public List<String> greenBeanBushConventionTags = new ArrayList<>(List.of(
            "c:is_wet",
            "c:is_temperate"
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