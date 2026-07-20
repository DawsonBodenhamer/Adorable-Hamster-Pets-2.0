package net.dawson.adorablehamsterpets.config;

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
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Translatable.Name("Visuals & UI")
@Translatable.Desc("Client-side aesthetics, performance toggles, and HUD elements.")
public class AhpUiConfig extends Config {

    public AhpUiConfig() {
        super(Identifier.of(AdorableHamsterPets.MOD_ID, "ui"));

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

    @Override
    @NotNull
    public SaveType saveType() {
        return SaveType.SEPARATE;
    }

    // --- UI & Quality of Life ---
    @Translatable.Name("UI & Quality of Life")
    @Translatable.Desc("Here's where you can tweak the knobs and dials for anything that pops up on your screen.")
    public ConfigGroup uiPreferences = new ConfigGroup("uiPreferences", true);

    @Translatable.Name("Guidebook Settings")
    @Translatable.Desc("Settings related to the 'Hamster Tips' guide book and how aggressively I nag you about it.")
    public ConfigGroup guidebookSettings = new ConfigGroup("guidebookSettings", true);

    @Translatable.Name("Auto Delivery")
    @Translatable.Desc("Hand-delivers the sacred texts on first login.")
    public boolean enableAutoGuidebookDelivery = true;

    @Translatable.Name("Auto Delivery Fallback")
    @Translatable.Desc("If Auto Delivery is disabled (like in most modpacks), give the guidebook the first time the player actually spots a wild hamster from 10 blocks away. This will only trigger once, and only if the player has never received the guidebook before.")
    public boolean enableAutoGuidebookDeliveryFallback = true;

    @Translatable.Name("Seen Warning Players")
    @Translatable.Desc("A list of usernames for players who have already seen the missing guidebook warning. Ensures that everyone sees this, but only once unless you delete your name from this list.")
    @NonSync
    public List<String> playersWhoHaveSeenGuidebookWarning = new ArrayList<>();

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Warning Timer")
    @Translatable.Desc("How long (in ticks) I wait before realizing you're book-less and panicking. 3600 = 3 minutes.")
    public ValidatedInt guidebookWarningTimer = new ValidatedInt(3600, 6000, 100);

    @Translatable.Name("Jade Overlay Settings")
    @Translatable.Desc("Fine-tune the voyeuristic amount of genetic data you see when staring at a hamster. Also includes toggles for Jade's default info. Note that these toggles do not affect any entities except hamsters.")
    public ConfigGroup jadeOverlaySettings = new ConfigGroup("jadeOverlaySettings", true);

    @NonSync
    @Translatable.Name("Require Sneak for Custom Info")
    @Translatable.Desc("If true, you must hold sneak to expose their genetic secrets. If false, their DNA is proudly broadcasted to you at all times. Set to False by default so I don't get spammed on Discord.")
    public boolean requireSneakForCustomJadeInfo = false;

    @NonSync
    @Translatable.Name("Require Sneak for Default Info")
    @Translatable.Desc("If true, the default Jade info will also be hidden unless you are actively sneaking. This setting only matters if 'Require Sneaking' is enabled above.")
    public boolean requireSneakForDefaultJadeInfo = false;

    @NonSync
    @Translatable.Name("Show Entity Name")
    @Translatable.Desc("Display the default Jade entity name. You know, 'Hampter'.")
    public boolean showJadeEntityName = true;

    @NonSync
    @Translatable.Name("Show Health")
    @Translatable.Desc("Display the default Jade health hearts. In case you want to know exactly how close your hamster is to the great beyond.")
    public boolean showJadeEntityHealth = true;

    @NonSync
    @Translatable.Name("Show Growth Time")
    @Translatable.Desc("Display the default Jade growth timer that shows how long until babies become adults. Useful if you believe age is just a feeling.")
    public boolean showJadeGrowthTime = true;

    @NonSync
    @Translatable.Name("Show Owner")
    @Translatable.Desc("Display the default Jade owner name. So everyone knows who is responsible when it happens.")
    public boolean showJadeOwner = true;

    @NonSync
    @Translatable.Name("Show Age")
    @Translatable.Desc("Display how long this hamster has managed to survive your world.")
    public boolean showJadeAge = true;

    @NonSync
    @Translatable.Name("Show Base Coat")
    @Translatable.Desc("Display the underlying fur palette.")
    public boolean showJadeBaseCoat = true;

    @NonSync
    @Translatable.Name("Show Wild Overlay")
    @Translatable.Desc("Display the naturally occurring fur overlay patterns.")
    public boolean showJadeWildOverlay = true;

    @NonSync
    @Translatable.Name("Show Breeding Overlay")
    @Translatable.Desc("Display the mutations you caused by genetic engineering.")
    public boolean showJadeBreedingOverlay = true;

    @NonSync
    @Translatable.Name("Show Eye Color")
    @Translatable.Desc("Display whether they possess the recessive red eye gene. Helpful since the eyes still appear black.")
    public boolean showJadeEyeColor = true;

    @NonSync
    @Translatable.Name("Show Aggression State")
    @Translatable.Desc("Display whether the hamster is currently in Pacifist, Menace, or just Standard aggression mode.")
    public boolean showJadeAggressionState = true;

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Show Inventory")
    @Translatable.Desc("Display the default Jade inventory contents so you can see what your hamster is hoarding.")
    public boolean showJadeInventory = true;

    @Translatable.Name("Action Bar Messages")
    @Translatable.Desc("Toggle and change the duration of the various little status mumbles that appear above your hotbar.")
    public ConfigGroup actionBarMessages = new ConfigGroup("actionBarMessages", true);

    @NonSync
    @Translatable.Name("Action Bar Duration")
    @Translatable.Desc("The duration (in ticks) that action bar messages stay on screen. Only affects your personal computer. Vanilla is 60 (3 seconds), which is barely enough time to realize you're reading. Crank this up to savor the text. (20 ticks = 1 second).")
    public ValidatedInt actionBarDuration = new ValidatedInt(100, 300, 40);

    @NonSync
    @Translatable.Name("Throw Cancellation Warning")
    @Translatable.Desc("Whether to display the chat warning when you cancel a hamster throw by releasing the button too early.")
    public boolean enableThrowCancellationWarning = true;

    @NonSync
    @Translatable.Name("Shoulder Dismount Messages")
    @Translatable.Desc("Little status mumbles when your co-pilot disembarks.")
    public boolean enableShoulderDismountMessages = true;

    @NonSync
    @Translatable.Name("Tree Heist Start Message")
    @Translatable.Desc("Whether to show an action bar message when a Tree Heist begins.")
    public boolean enableTreeHeistStartMessage = true;

    @NonSync
    @Translatable.Name("Bed Break Notification")
    @Translatable.Desc("Get an action bar message when your hamster's bed is broken. Here's where you can turn it off if you prefer... complete immersion.")
    public boolean enableBedBreakMessage = true;

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Tamed Baby Warning")
    @Translatable.Desc("Warns you when a newly tamed baby hamster refuses to follow you and informs you how to unlink them from their parent.")
    public boolean enableTamedBabyWarningMessage = true;

    @Translatable.Name("Hamster Renaming")
    @Translatable.Desc("Because 'Hamster #42' lacks a certain personal touch, here are some settings to control how hamsters get renamed.")
    public ConfigGroup naming = new ConfigGroup("naming", true);

    @Translatable.Name("Consume Name Tag")
    @Translatable.Desc("If true, you must have a Name Tag in your inventory (or the hamster's cheeks) to confirm the rename, and it will be consumed. True by default to keep things vanilla-friendly-ish.")
    public boolean consumeNameTagForGuiRename = true;

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Pencil Icon Placement")
    @Translatable.Desc("Which side of the name the little pencil icon sits on. For the easterners who prefer reading from right to left.")
    public ValidatedEnum<RenameIconPlacement> renameIconPlacement = new ValidatedEnum<>(RenameIconPlacement.LEFT);

    @NonSync
    @Translatable.Name("Mod Item Tooltips")
    @Translatable.Desc("Helpful whispers on what the heck that cucumber is for.")
    public boolean enableItemTooltips = true;

    @NonSync
    @Translatable.Name("Age Progression IRL Time")
    @Translatable.Desc("If true, the hamster's age progresses at real-world speed (24 hours = 1 day) instead of Minecraft speed (20 minutes = 1 day).")
    public boolean displayAgeInIrlTime = false;

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Jade Hamster Debug Info")
    @Translatable.Desc("More stats than anyone asked for. Defaults to off— mercifully.")
    public boolean enableJadeHamsterDebugInfo = false;

    // --- Announcements & Update Notes ---
    @Translatable.Name("Announcements & Update Notes")
    @Translatable.Desc("Tweak the little bell icon that appears when I have something important to tell you. Or turn if off if you hate fun. Aside from the 'Disable Announcements (Server)' setting, the rest of the notification settings in this group are client-side (meaning they only affect your personal computer).")
    public ConfigGroup announcements = new ConfigGroup("announcements", true);

    @NonSync
    @Translatable.Name("Enable Announcements (Client)")
    @Translatable.Desc("Your personal switch for all announcement notifications. Turning this off is the same as clicking 'Disable All' in the announcement screen. Announcements can still be viewed in the §f§lHamster Tips§r guide book.")
    public ValidatedBoolean enableNotificationIcons = new ValidatedBoolean(true); // plain boolean; with special functionality wired it up in the constructor

    @Translatable.Name("Disable Announcements (Server)")
    @Translatable.Desc("If true, completely disables the announcement bell icon for all players on the server, overriding their client settings, including 'Enable Announcements (Client).'")
    public boolean serverDisableAnnouncements = false;

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
                            Text.translatable("message.adorablehamsterpets.announcements_marked_read").formatted(Formatting.WHITE),
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
                            Text.translatable("message.adorablehamsterpets.announcements_reset").formatted(Formatting.WHITE),
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
                            Text.translatable("config.adorablehamsterpets.condition.hud_icon_enabled"),
                            () -> IconPositionPreset.TOP_LEFT
                    );

    @NonSync
    @Translatable.Name("HUD Icon Offset X")
    @Translatable.Desc("Shove the icon horizontally. For when 'top-left' isn't specific enough for your discerning taste.")
    public ValidatedCondition<Integer> hudIconOffsetX =
            new ValidatedInt(10, 500, -500)
                    .toCondition(
                            isHudIconEnabled,
                            Text.translatable("config.adorablehamsterpets.condition.hud_icon_enabled"),
                            () -> 10
                    );

    @NonSync
    @Translatable.Name("HUD Icon Offset Y")
    @Translatable.Desc("Adjust the vertical placement. Does it block your view? Is it not blocking your view enough? The power is yours.")
    public ValidatedCondition<Integer> hudIconOffsetY =
            new ValidatedInt(10, 500, -500)
                    .toCondition(
                            isHudIconEnabled,
                            Text.translatable("config.adorablehamsterpets.condition.hud_icon_enabled"),
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
                            Text.translatable("config.adorablehamsterpets.condition.hud_icon_enabled"),
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
                            Text.translatable("config.adorablehamsterpets.condition.widget_icon_enabled"),
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
                            Text.translatable("config.adorablehamsterpets.condition.widget_icon_enabled"),
                            WidgetIconOffsets::new
                    );


    // --- Falling Leaf Settings ---
    @Translatable.Name("Falling Leaf Settings")
    @Translatable.Desc("Here's where you tweak the behavior of the floaty leaf particles spawned from Hamster Bedding. Didn't know you could spawn particles from Hamster Bedding? Try to keep up.")
    public ConfigGroup particleEffects = new ConfigGroup("particleEffects", true);

    @NonSync
    @Translatable.Name("Gust Volume")
    @Translatable.Desc("How loud the wind gust sound effect is, for you overachievers who are running 15 different sound physics mods. 1.0 is default, 0.0 is silent.")
    public ValidatedFloat leafGustVolume = new ValidatedFloat(0.3f, 3.0f, 0.0f);

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
    @Translatable.Desc("Set a fixed direction for the universal leaf drift (0-360 degrees). 0 = South, 90 = West, 180 = North, 270 = East. Or just slide it until it looks cool. Whatever. Only works if 'Dynamic Drift' is off.")
    public ValidatedCondition<Integer> staticDriftAngle =
            new ValidatedInt(0, 360, 0)
                    .toCondition(
                            isDynamicDriftDisabled,
                            Text.translatable("config.adorablehamsterpets.condition.dynamic_drift_off"),
                            () -> 0
                    );
}
