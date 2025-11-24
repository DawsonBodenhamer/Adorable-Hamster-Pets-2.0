package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.annotations.RootConfig;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
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
}
