package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.annotations.NonSync;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@Translatable.Name("Supporter Perks")
@Translatable.Desc("For the generous souls who keep the hamster wheels spinning. Here are your exclusive settings for tweaking cosmetics.")
public class AhpSupporterConfig extends Config {

    public AhpSupporterConfig() {
        super(Identifier.of(AdorableHamsterPets.MOD_ID, "supporter_perks"));
    }

    @Override
    @NotNull
    public SaveType saveType() {
        return SaveType.SEPARATE;
    }

    // --- The Pixie Dust Crown ---
    @Translatable.Name("The Pixie Dust Crown")
    @Translatable.Desc("Settings for the majestic, spinning ring of pixie dust hovering above your head.")
    public ConfigGroup supporterParticleCrown = new ConfigGroup("supporterParticleCrown", false);

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

}