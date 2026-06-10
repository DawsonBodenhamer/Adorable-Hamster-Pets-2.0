package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.annotations.NonSync;
import me.fzzyhmstrs.fzzy_config.annotations.RootConfig;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigAction;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Root-level configuration used purely as the entry point for this mod's
 * settings.  The @RootConfig annotation causes this config’s name and
 * description to be shown on the landing page, and the other configs in
 * the same namespace will be listed below it.
 */
@Translatable.Name("Main Menu")
@Translatable.Desc("Here's where your hamster experimentation begins. Don't forget to touch grass.")
@RootConfig
public class AhpRootConfig extends Config {
    public AhpRootConfig() {
        // Identifier path “root” gives the file name (root.toml) and the
        // translation key config.adorablehamsterpets.root
        super(Identifier.of(AdorableHamsterPets.MOD_ID, "root"));
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
    @Translatable.Name("Support the Mod")
    public ConfigAction visitWebsite = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.help_and_resources.support_the_mod"))
            .desc(Text.translatable("config.adorablehamsterpets.main.help_and_resources.support_the_mod.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_LINK())
            .build(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://www.fortheking.design/minecraft-modding"));
}
