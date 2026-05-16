package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.annotations.NonSync;
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
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@Translation(prefix = "adorablehamsterpets.main") // TODO: Remove @Translation annotation in Fzzy Config 0.7.4+ (See EnUsGenerator.java)
@Translatable.Name("Main Settings")
@Translatable.Desc("The command center for rodent-based chaos. Tweak physics, nerf cheese, and generally play god with small furry creatures.")
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

    // --- Supporter Perks ---
    @Translatable.Name("Supporter Perks")
    @Translatable.Desc("For the generous souls who keep the hamster wheels spinning. Here are your exclusive cosmetics settings.")
    public ConfigGroup supporterPerks = new ConfigGroup("supporterPerks", true);

    @Translatable.Name("Particle Crown")
    @Translatable.Desc("Settings for the majestic, spinning ring of pixie dust hovering above your head.")
    public ConfigGroup supporterParticleCrown = new ConfigGroup("supporterParticleCrown", true);

    @NonSync
    @Translatable.Name("Enable Crowns")
    @Translatable.Desc("Master switch. Turn this off if you hate fun. Disables all crowns, even those worn by others.")
    public boolean enableSupporterCrown = true;

    @NonSync
    @Translatable.Name("Enable Crown Audio")
    @Translatable.Desc("Turns off the sparkling sound effects the emanate from the crown. Affects all crowns, even those worn by others.")
    public boolean enableCrownAudio = true;

    @NonSync
    @Translatable.Name("Crown Audio Volume")
    @Translatable.Desc("How loud the crown sparkles are. Crank it up if your 50 sound physics mods made it inaudible, or turn it down if the majestic twinkling is eroding your sanity.")
    public ValidatedFloat crownAudioVolume = new ValidatedFloat(1.0f, 2.0f, 0.1f);

    @NonSync
    @Translatable.Name("Show My Crown")
    @Translatable.Desc("Toggle this off if you want to hide your own crown from yourself and everyone else. Only affects your own crown.")
    public boolean showMyCrown = true;

    @NonSync
    @Translatable.Name("Show in First Person")
    @Translatable.Desc("Should your own crown decorate your vision while you're trying to mine? Turn it off if you are tired of flexing on yourself. Only affects your own crown.")
    public boolean showCrownInFirstPerson = true;

    @NonSync
    @Translatable.Name("Color Theme")
    @Translatable.Desc("Pick your own crown's specific color. Other players will instantly see this color change above your head. Only affects your own crown.")
    public ValidatedEnum<PixieDustParticleTheme> crownTheme = new ValidatedEnum<>(PixieDustParticleTheme.GOLD);

    @NonSync
    @Translatable.Name("Particle Count")
    @Translatable.Desc("How many sparkly particles to spawn per tick (20 ticks per second). Makes the crown thicker or thinner. Affects all crowns.")
    public ValidatedInt crownParticleCount = new ValidatedInt(10, 30, 1);

    @NonSync
    @Translatable.Name("Radius")
    @Translatable.Desc("How wide (in blocks) the halo of superiority extends around your head. Affects all crowns.")
    public ValidatedDouble crownRadius = new ValidatedDouble(0.3, 1.0, 0.1);

    @NonSync
    @Translatable.Name("Thickness")
    @Translatable.Desc("How thick the crown is from the inner edge (the inside rim around your head) to the outer edge. Affects all crowns.")
    public ValidatedDouble crownHorizontalThickness = new ValidatedDouble(0.02, 1.0, 0.0);

    @NonSync
    @Translatable.Name("Height")
    @Translatable.Desc("How tall the crown is from the bottom edge to the top edge. Increase this to make the crown less of a hula-hoop and more of a cylinder. Affects all crowns.")
    public ValidatedDouble crownVerticalThickness = new ValidatedDouble(0.2, 1.0, 0.0);

    @NonSync
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Vertical Offset")
    @Translatable.Desc("Nudge the crown up or down. Move it high enough and you'll have a halo. Vanity for the win. Affects all crowns.")
    public ValidatedDouble crownYOffset = new ValidatedDouble(0.25, 2.0, -2.0);

    // --- Performance Settings ---
    @Translatable.Name("Performance Settings")
    @Translatable.Desc("Hamsters have a lot of render layers by default. Here's where you can turn them all off temporarily if you'd like to look at a huge group of 500 thousand hamsters without setting your PC on fire.")
    public ConfigGroup performanceSettings = new ConfigGroup("performanceSettings", true);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Performance Mode [WARNING]")
    @Translatable.Desc("[WARNING: THIS MAKES ALL HAMSTERS LOOK IDENTICAL]. Disables all texture overlays, accessories, and armor rendering on hamsters, forcing them to render with a single grayscale texture. Drastically improves FPS when hundreds of thousands of hamsters are on screen. Can be toggled on and off via a custom keybind.")
    public boolean performanceMode = false;

    // --- UI & Quality of Life ---
    @Translatable.Name("UI & Quality of Life")
    @Translatable.Desc("Because Sanity is Overrated")
    public ConfigGroup uiPreferences = new ConfigGroup("uiPreferences", true);

    @Translatable.Name("Guidebook Settings")
    @Translatable.Desc("Settings related to the 'Hamster Tips' guide book and how aggressively I nag you about it.")
    public ConfigGroup guidebookSettings = new ConfigGroup("guidebookSettings", true);

    @NonSync
    @Translatable.Name("Auto Delivery")
    @Translatable.Desc("Hand-delivers the sacred texts on first login.")
    public boolean enableAutoGuidebookDelivery = true;

    @NonSync
    @Translatable.Name("Auto Delivery Fallback")
    @Translatable.Desc("If Auto Delivery is disabled (like in most modpacks), give the guidebook the first time the player actually spots a wild hamster from 10 blocks away. This will only trigger once, and only if the player has never received the guidebook before.")
    public boolean enableAutoGuidebookDeliveryFallback = true;

    @NonSync
    @Translatable.Name("Seen Warning Players")
    @Translatable.Desc("A list of usernames for players who have already seen the missing guidebook warning. Ensures that everyone sees this, but only once unless you delete your name from this list.")
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

    // --- Core Feature Toggles ---
    @Translatable.Name("Core Feature Toggles")
    @Translatable.Desc("Fundamental hamster hijinks— fiddle at your own risk.")
    public ConfigGroup core = new ConfigGroup("core", true);

    @Translatable.Name("Enable Breeding")
    @Translatable.Desc("Whether hamsters are allowed to multiply. Turn this off if you fear someone on your server plans to create a rodent horde.")
    public boolean enableBreeding = true;

    @Translatable.Name("Enable Teleport Rescue")
    @Translatable.Desc("If true, hamsters that are actively following you (not sitting or wandering) will instantly teleport with you, across dimensions and even if their current chunk becomes unloaded. WARNING: do not turn this off unless you want to risk your hamsters being left behind on long teleports.")
    public boolean enableTeleportRescue = true;

    @Translatable.Name("Enable Hamster Throwing")
    @Translatable.Desc("Do we yeet the hamster? ('G' by default).")
    public boolean enableHamsterThrowing = true;

    @Translatable.Name("Enable Wander Mode")
    @Translatable.Desc("For when you need some personal space. Allows tamed hamsters to be linked to a Hamster Bed, letting them wander freely within a set radius instead of clinging to you like melted duct-tape. You're welcome.")
    public ValidatedBoolean enableWanderMode = new ValidatedBoolean(true);

    @Translatable.Name("Enable Stealing/Fetching")
    @Translatable.Desc("Permits hamsters to engage in spontaneous, high-stakes games of keep-away with your valuables.")
    public boolean enableItemStealing = true;

    @Translatable.Name("Enable Tag Game")
    @Translatable.Desc("Master switch. If false, hamsters will suppress their playful urges and remain stoic professionals.")
    public boolean enableTagGame = true;

    @NonSync
    @Translatable.Name("Enable Creeper Sniffing")
    @Translatable.Desc("May save your inventory. Or your ears. Allows hamsters to sniff for any aggressive creepers that have begun hunting you.")
    public boolean enableShoulderCreeperDetection = true;

    @NonSync
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

    @Translatable.Name("Require Food Mix to Unlock Cheeks")
    @Translatable.Desc("Gate cheek-pouch storage behind gourmet cuisine, because drama.")
    public boolean requireFoodMixToUnlockCheeks = true;

    @Translatable.Name("Use 'Hampter' as Default Name")
    @Translatable.Desc("Changes the default entity name from 'Hamster' to 'Hampter'. Note: This has no visible effect in vanilla Minecraft, as mobs don't show nameplates by default. It's primarily for use with mods like Auto Leveling that display entity names.")
    public boolean useHampterName = false;

    @Translatable.Name("Enable Petting")
    @Translatable.Desc("If true, looking affectionately at your hamster might result in spontaneous petting. Also enables the Pet Hamster keybind.")
    public boolean enablePetting = true;

    @Translatable.Name("Auto-Petting Chance")
    @Translatable.Desc("The 1-in-X chance per tick (20 ticks per second) to initiate petting when sneaking and looking directly at your hamster. The hamster must be either standing or sitting, and not involved in any other activity. The default is 500. This means on average, it will require about ~15 seconds of uninterrupted staring to trigger, so it's somewhat rare. If you're impatient, it also comes with a keybind to trigger it manually. You don't need to be sneaking to use the keybind.")
    public ValidatedInt pettingChanceDenominator = new ValidatedInt(500, 3000, 20);

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

    @Translatable.Name("Cleaning Frequency")
    @Translatable.Desc("How often a sitting hamster gets the sudden urge to clean. It's a 1-in-X chance per tick, so lower numbers mean a higher chance for cleaning. For example, 1200 means on average, it'll clean about once a minute. 300 ≈ every 15 secs, and 5000 ≈ every 4 mins. Congratulations— now you know enough to be dangerous.")
    public ValidatedInt cleaningChanceDenominator = new ValidatedInt(1200, 5000, 300);

    @Translatable.Name("Throw Cooldown")
    @Translatable.Desc("Time-out after using your living projectile. (20 ticks = 1 second)")
    public ValidatedInt hamsterThrowCooldown = new ValidatedInt(2400, 20 * 60 * 10, 20);

    @Translatable.Name("Green Bean Buff Cooldown")
    @Translatable.Desc("When the sugar rush ends, force a breather. (20 ticks = 1 second)")
    public ValidatedInt steamedGreenBeansBuffCooldown = new ValidatedInt(6000, 20 * 60 * 10, 20);

    @Translatable.Name("Diamond Seeking Cooldown")
    @Translatable.Desc("Force a cool-down after striking it rich. Off by default, since this can't happen again anyway without another mount/dismount on the shoulder.")
    public boolean enableIndependentDiamondSeekCooldown = false;

    @Translatable.Name("Diamond Seeking Cooldown")
    @Translatable.Desc("Cooldown before your hamster can go on another treasure hunt. (20 ticks = 1 second)")
    public ValidatedInt independentOreSeekCooldownTicks = new ValidatedInt(2400, 6000, 20);

    @Translatable.Name("Item Thievery Cooldown")
    @Translatable.Desc("Mandatory time-out after a successful heist to prevent serial kleptomania. (20 ticks = 1 second). WARNING: Increasing this cooldown can dramatically change the item stealing mechanic, since that AI goal sometimes re-runs multiple times in a row when the hamster has trouble pathfinding to the item that it wants to steal. So instead of increasing this, you should probably just stop dropping your diamonds on the ground everywhere, butter fingers.")
    public ValidatedInt stealCooldownTicks = new ValidatedInt(100, 6000, 20);

    @Translatable.Name("Tag Game Cooldown")
    @Translatable.Desc("How long a specific hamster needs to recover after being chased. Remember, they have tiny lungs. (20 ticks = 1 second; default = 10 minutes)")
    public ValidatedInt tagGameCooldown = new ValidatedInt(12000, 36000, 160);

    @ConfigGroup.Pop
    @Translatable.Name("Breeding Cooldown (Seconds)")
    @Translatable.Desc("Hamsters need their space. Here's where you give them a break between litters.")
    public ValidatedInt breedingCooldownSeconds = new ValidatedInt(300, 1200, 1);

    // --- Core Item Tag Overrides ---
    @Translatable.Name("Core Item Tag Overrides")
    @Translatable.Desc("For the advanced user who looks at a perfectly functional system and thinks, 'I can make this weirder.' Edit these lists to change what items your hamsters consider food, bait, treasure, and all other interactions. Use item IDs (e.g., 'minecraft:diamond') or tags (e.g., '#minecraft:fishes'). Mess it up? That's a you problem.")
    public ConfigGroup itemTags = new ConfigGroup("itemTags", true);

    @Translatable.Name("Taming Baits")
    @Translatable.Desc("The official list of bribes for convincing wild fluffballs to join your cause. By default, it's just sliced cucumbers. Feel free to add 'minecraft:nether_star' if you enjoy making poor life choices. Compatible with Cultural Delights by default!")
    public List<String> tamingFoods = new ArrayList<>(List.of("adorablehamsterpets:sliced_cucumber", "culturaldelights:cut_cucumber"));

    @Translatable.Name("Standard Diet")
    @Translatable.Desc("The hamster's everyday menu. These items will heal them or, if they're at full health, might give them... ideas about starting a family. Don't make it weird.")
    public List<String> standardDiet = new ArrayList<>(List.of(
            "adorablehamsterpets:hamster_food_mix", "adorablehamsterpets:sunflower_seeds", "adorablehamsterpets:green_beans",
            "adorablehamsterpets:cucumber", "adorablehamsterpets:green_bean_seeds", "adorablehamsterpets:cucumber_seeds",
            "minecraft:apple", "minecraft:carrot", "minecraft:melon_slice", "minecraft:sweet_berries",
            "minecraft:beetroot", "minecraft:wheat", "minecraft:wheat_seeds", "#adorablehamsterpets:seeds",

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

    @Translatable.Name("Passively Munchable Snacks")
    @Translatable.Desc("The specific items a hamster will eat directly from its cheek pouch to heal itself when injured. Keep it exclusive, or let them feast on enchanted apples. Your call.")
    public List<String> autoHealFoods = new ArrayList<>(List.of("adorablehamsterpets:hamster_food_mix"));

    @Translatable.Name("Cheek Pouch Smuggling List")
    @Translatable.Desc("Fine-tune exactly what your hamster is (and isn't) allowed to carry. The 'Allowed' list acts as a high-priority override to the 'Disallowed' lists and general rules.")
    public ConfigGroup pouchRestrictions = new ConfigGroup("pouchRestrictions", true);

    @Translatable.Name("Allowed Items")
    @Translatable.Desc("A specific list of items and tags that are allowed in the hamster's cheek pouch. You can add things to this list to bypass the default 'no tools or big blocks' rule, since this overrides the 'disallowed' settings.")
    public List<String> pouchAllowedItems = new ArrayList<>(List.of(
            "minecraft:torch", "minecraft:soul_torch", "minecraft:redstone_torch", "minecraft:repeater", "minecraft:comparator", "minecraft:lever", "#minecraft:buttons",
            "#minecraft:pressure_plates", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "minecraft:melon_seeds", "minecraft:pitcher_pod", "minecraft:torchflower_seeds", "#c:seeds", "#forge:seeds"
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

    // --- Breeding Settings ---
    @Translatable.Name("Breeding Settings")
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

    @NonSync
    @Translatable.Name("Reset Your Breeding History")
    public ConfigAction resetBreedingHistory = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.breedingSettings.resetBreedingHistory"))
            .desc(Text.translatable("config.adorablehamsterpets.main.breedingSettings.resetBreedingHistory.desc"))
            .decoration(TextureIds.INSTANCE.getRESTORE())
            .build(() -> {
                if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                    MinecraftClient.getInstance().getNetworkHandler().sendCommand("ahp reset_player_breeding_history");
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
            .toCondition(isPlayerBreedingLimitEnabled, Text.translatable("config.adorablehamsterpets.condition.litter_limit_enabled"), () -> LitterLimitType.DAILY);

    @Translatable.Name("Max Litters Per Player")
    @Translatable.Desc("How many litters a player can orchestrate before their breeding license is revoked.")
    public ValidatedCondition<Integer> maxLittersPerPlayer = new ValidatedInt(5, 100, 1)
            .toCondition(isPlayerBreedingLimitEnabled, Text.translatable("config.adorablehamsterpets.condition.litter_limit_enabled"), () -> 5);

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Use IRL Time")
    @Translatable.Desc("If true, 'Daily Player Breeding Limit' means 24 real-world hours. If false, it means 20 Minecraft minutes.")
    public ValidatedCondition<Boolean> useIrlTimeForBreedingLimit = new ValidatedBoolean(false)
            .toCondition(isPlayerBreedingLimitEnabled, Text.translatable("config.adorablehamsterpets.condition.litter_limit_enabled"), () -> false);

    // --- Armor Settings ---
    @Translatable.Name("Armor Settings")
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
                    Text.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
                    () -> 0.0
            );

    @Translatable.Name("Gold")
    @Translatable.Desc("The zoom factor provided by Gold Armor. Because flimsier things go faster. Obviously. (+0.20 = +20% Speed)")
    public ValidatedCondition<Double> goldArmorSpeedBoost = new ValidatedDouble(0.20, 2.0, 0.0)
            .toCondition(
                    areArmorPerksEnabled,
                    Text.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
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
                    Text.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
                    () -> 0.0
            );

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Throw Damage")
    @Translatable.Desc("Heavy things hit harder. Adds this much flat damage to the projectile impact. (1 = 0.5 hearts)")
    public ValidatedCondition<Double> netheriteArmorThrowDamageBonus = new ValidatedDouble(10.0, 100.0, 0.0)
            .toCondition(
                    areArmorPerksEnabled,
                    Text.translatable("config.adorablehamsterpets.condition.armor_perks_enabled"),
                    () -> 0.0
            );

    @NonSync
    @Translatable.Name("Enable Armor Visuals")
    @Translatable.Desc("Master switch for armor rendering. If false, hamsters will appear unarmored even when equipped. Useful if you prefer the natural look but still want the protection.")
    public boolean enableArmorVisuals = true;

    // Helper field to gate the Acorn Hat setting
    private final ValidatedField<Boolean> isArmorVisualsEnabled = new ValidatedBoolean(true).map(b -> b, b -> enableArmorVisuals);

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Render Acorn Hat")
    @Translatable.Desc("Determines whether you are able to see the jaunty little Acorn Hat when hamsters are wearing the base Acorn Armor. Does not affect what other players see, and does not apply to the standalone Acorn Hat accessory.")
    public ValidatedCondition<Boolean> renderAcornHat = new ValidatedBoolean(true)
            .toCondition(
                    isArmorVisualsEnabled,
                    Text.translatable("config.adorablehamsterpets.condition.armor_visuals_enabled"),
                    () -> false
            );

    @Translatable.Name("Tree Heist Settings")
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
            .title(Text.translatable("config.adorablehamsterpets.main.treeHeist.resetHeistHistory"))
            .desc(Text.translatable("config.adorablehamsterpets.main.treeHeist.resetHeistHistory.desc"))
            .decoration(TextureIds.INSTANCE.getRESTORE())
            .build(() -> {
                // 1.20.1: Send packet via Architectury Channel
                ModPackets.CHANNEL.sendToServer(new ModPackets.ResetHeistHistoryC2SPacket());
            });

    @NonSync
    @ConfigGroup.Pop
    @Translatable.Name("Debug Mode")
    @Translatable.Desc("Shows visual particles for the detected Tree ID (the lowest block of the trunk) and leaf canopy during a heist, and turns on extra logging in the console. Useful for seeing exactly which tree your hamster is searching and/or debugging if things are being weird.")
    public boolean debugTreeDetection = false;

    // --- Bed & Wander Mode Settings ---
    @Translatable.Name("Bed & Wander Mode Settings")
    @Translatable.Desc("For when 'following you into lava' is no longer a desirable trait. Tweak the settings for your hamster's newfound, bed-based independence.")
    public ConfigGroup wanderMode = new ConfigGroup("wanderMode", true);

    @Translatable.Name("Enable Respawn in Bed")
    @Translatable.Desc("The Master Switch. Affects all Hamster Beds. If true, hamsters linked to a bed can be resurrected there. Cheating? Maybe. Convenient? Absolutely.")
    public ValidatedBoolean enableRespawnInBed = new ValidatedBoolean(false);

    // Helper field for gating
    private final ValidatedField<Boolean> isRespawnInBedEnabled = enableRespawnInBed.map(b -> b, b -> b);

    @Translatable.Name("Free Bed Respawns")
    @Translatable.Desc("If true, Hamster Beds do not require a tribute item to function as a respawn point. They will work indefinitely for free.")
    public ValidatedCondition<Boolean> freeBedRespawns = new ValidatedBoolean(false)
            .toCondition(isRespawnInBedEnabled,
                    Text.translatable("config.adorablehamsterpets.condition.respawn_enabled"),
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
                    Text.translatable("config.adorablehamsterpets.condition.sleep_in_bed_allowed"),
                    () -> false
            );

    // Helper field to gate the min and max time settings
    private final ValidatedField<Boolean> isCircadianChaosEnabled = circadianChaos.map(b -> b, b -> b);

    @Translatable.Name("Min Nap Interval")
    @Translatable.Desc("The shortest possible time (in seconds) a hamster will stay awake or asleep in bed before considering a change. Defaults to 5 minutes— for the truly narcoleptic rodent. A random duration between the min and max is chosen each time, so move them further apart for more... unpredictable behavior.")
    public ValidatedCondition<Integer> minNapInBedIntervalSeconds  = new ValidatedInt(300, 7000, 5)
            .toCondition(
                    () -> allowSleepInBed.get() && circadianChaos.get(),
                    Text.translatable("config.adorablehamsterpets.condition.circadian_chaos_on"),
                    () -> 300
            );

    @Translatable.Name("Max Nap Interval")
    @Translatable.Desc("The longest amount of time (in seconds) a hamster can possibly stay awake or asleep in bed before it gets bored and switches things up. Defaults to 10 minutes.")
    public ValidatedCondition<Integer> maxNapInBedIntervalSeconds = new ValidatedInt(600, 7200, 10)
            .toCondition(
                    () -> allowSleepInBed.get() && circadianChaos.get(),
                    Text.translatable("config.adorablehamsterpets.condition.circadian_chaos_on"),
                    () -> 900
            );

    @Translatable.Name("Sleep During the Day")
    @Translatable.Desc("If false, wandering hamsters will sleep in their beds during the night. If true, they'll adopt a more nocturnal, goth-adjacent lifestyle and sleep in the daytime.")
    public ValidatedCondition<Boolean> sleepDuringDay = new ValidatedBoolean(true)
            .toCondition(
                    () -> allowSleepInBed.get() && !circadianChaos.get(),
                    Text.translatable("config.adorablehamsterpets.condition.circadian_chaos_overrides"),
                    () -> true
            );

    @ConfigGroup.Pop
    @Translatable.Name("Manual Wake-Up Duration")
    @Translatable.Desc("The mandatory grumpiness period if you rudely awaken a hamster from its bed before it was ready. It won't go back to sleep until this timer runs out. (20 ticks = 1 second)")
    public ValidatedCondition<Integer> bedWakeUpCooldown = new ValidatedInt(300, 1200, 20)
            .toCondition(
                    isSleepInBedAllowed,
                    Text.translatable("config.adorablehamsterpets.condition.sleep_in_bed_allowed"),
                    () -> 300
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
    @Translatable.Name("Double-Tap Delay")
    @Translatable.Desc("Max time between sneak key presses to count as a double-tap. (20 ticks = 1 second)")
    public ValidatedCondition<Integer> doubleTapDelayTicks =
            new ValidatedInt(10, 40, 5)
                    .toCondition(
                            isDoubleTap,
                            Text.translatable("config.adorablehamsterpets.condition.double_tap"),
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
                            Text.translatable("config.adorablehamsterpets.condition.dynamic_shoulder_off"),
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
                            Text.translatable("config.adorablehamsterpets.condition.dynamic_shoulder_off"),
                            () -> ForcedShoulderState.ALWAYS_STAND
                    );

    @NonSync
    @Translatable.Name("Forced State (Left)")
    @Translatable.Desc("See description for 'Forced State (Head)'.")
    public ValidatedCondition<ForcedShoulderState> forcedLeftShoulderState =
            new ValidatedEnum<>(ForcedShoulderState.ALWAYS_STAND)
                    .toCondition(
                            dynamicShoulderDisabled,
                            Text.translatable("config.adorablehamsterpets.condition.dynamic_shoulder_off"),
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

    @Translatable.Name("Throw Velocity (Buffed)")
    @Translatable.Desc("The throw speed of your furry projectile when under the influence of Steamed Green Beans. Goes from 'yeet' to 'yote'.")
    public ValidatedDouble hamsterThrowVelocityBuffed = new ValidatedDouble(2.5, 5.0, 0.1);

    @ConfigGroup.Pop
    @Translatable.Name("Friendly Fire")
    @Translatable.Desc("If true, throwing your own hamster straight up above your head will eventually result in a concussive reunion on the way back down. High-velocity rodents tend to hurt, even if they love you.")
    public boolean yeetFriendlyFire = true;

    // --- Tamed Sleep Settings ---
    @Translatable.Name("Tamed Sleep Settings")
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

    // --- Independent Diamond Seeking Settings ---
    @Translatable.Name("Independent Diamond Seeking Settings")
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

    // --- Mini-Game Settings ---
    @Translatable.Name("Mini-Game Settings")
    @Translatable.Desc("Rules for when your hamster gets bored and decides to create its own entertainment.")
    public ConfigGroup miniGames = new ConfigGroup("miniGames", true);

    @Translatable.Name("Minimum Flee Distance")
    @Translatable.Desc("The personal space bubble (in blocks) hamsters maintain while running away.")
    public ValidatedInt minMiniGameFleeDistance = new ValidatedInt(7, 20, 1);

    @Translatable.Name("Maximum Flee Distance")
    @Translatable.Desc("The maximum distance (in blocks) before hamsters stop running from you and start taunting. If they get further than this, they will wait for you to catch up.")
    public ValidatedInt maxMiniGameFleeDistance = new ValidatedInt(10, 30, 5);

    @Translatable.Name("Minimum Game Duration")
    @Translatable.Desc("The shortest amount of time (in seconds) a chase lasts before hamsters get bored. Randomly chosen between Min and Max.")
    public ValidatedInt minMiniGameFleeDurationSeconds = new ValidatedInt(5, 240, 1);

    @Translatable.Name("Maximum Game Duration")
    @Translatable.Desc("The longest amount of time (in seconds) a chase lasts. Randomly chosen between Min and Max.")
    public ValidatedInt maxMiniGameFleeDurationSeconds = new ValidatedInt(15, 300, 5);

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
    @Translatable.Desc("If true, hamsters can ask anyone to play. If false, they can only ask their owners.")
    public boolean allowStrangerTag = true;

    @Translatable.Name("Game Initiation Chance")
    @Translatable.Desc("The 1-in-X chance per tick a hamster will start a game of tag if you make too much eye contact. Since the game runs at 20 ticks per second, a 1-in-100-tick chance means you'll need to maintain eye contact for about 5-10 seconds on average. Set to 1 if you want the game to start instantly upon eye contact.")
    public ValidatedInt tagGameChanceDenominator = new ValidatedInt(100, 1200, 1);

    @Translatable.Name("Enable Player Daily Limit")
    @Translatable.Desc("If true, players are capped on how many games they can play per day. If false, you can play until your legs fall off.")
    public ValidatedBoolean enableTagGamePlayerLimit = new ValidatedBoolean(true);

    private final ValidatedField<Boolean> isTagLimitEnabled = enableTagGamePlayerLimit.map(b -> b, b -> b);

    @Translatable.Name("Max Games Per Day")
    @Translatable.Desc("How many times a single player can be 'It' per Minecraft day before telling the rodents to go find a hobby. Prevents infinite reward farming.")
    public ValidatedCondition<Integer> maxDailyTagGamesPerPlayer = new ValidatedInt(3, 100, 0)
            .toCondition(
                    isTagLimitEnabled,
                    Text.translatable("config.adorablehamsterpets.condition.tag_limit_enabled"),
                    () -> 3
            );

    @Translatable.Name("Use Pouch Loot for Rewards")
    @Translatable.Desc("If true, winning a game of tag rewards you with whatever random pocket lint generates in wild cheek pouches. (That list is configurable: see \"Cheek Pouch Loot\" in the World Gen Config.) If false, it strictly pulls from the custom list below.")
    public boolean usePouchLootForTagRewards = true;

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Custom Tag Rewards")
    @Translatable.Desc("The specific items your hamster will regurgitate as a prize, assuming you disabled the toggle above. Format specific item names like this: 'minecraft:diamond' and you can use tags like this: '#minecraft:flowers'. If you leave this empty while custom rewards are active, your hamster will just stare at you awkwardly after you win.")
    public List<String> customTagRewards = new ArrayList<>();

    @Translatable.Name("Commissioned Features")
    @Translatable.Desc("Specialized, unofficial mechanics that don't necessarily fit the theme of the mod, but were funded by various individuals in the community. Purposefully tucked away in the config to ensure most people don't notice them.")
    public ConfigGroup commissionedFeatures = new ConfigGroup("commissionedFeatures", true);

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
                    Text.translatable("config.adorablehamsterpets.condition.riding_enabled"),
                    () -> 0.25
            );

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Sprint Ride Speed")
    @Translatable.Desc("The speed multiplier when sprinting. Hold on to your acorn hat. 0.35 is the default. I wish it was a nice round number, but alas, hamster riding is a complex enigma.")
    public ValidatedCondition<Double> ridingSprintSpeedMultiplier = new ValidatedDouble(0.35, 1.0, 0.0)
            .toCondition(
                    isRidingEnabled,
                    Text.translatable("config.adorablehamsterpets.condition.riding_enabled"),
                    () -> 0.8
            );
}