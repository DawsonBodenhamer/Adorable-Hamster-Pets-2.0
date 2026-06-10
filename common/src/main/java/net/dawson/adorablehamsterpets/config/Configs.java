package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;

/**
 * Static holder for the Adorable Hamster Pets configs.
 * Touching {@code Configs.AHP_MAIN} guarantees the config is registered,
 * loaded from file, and its sync/GUI channels are ready.
 */
public final class Configs {

    public static final AhpRootConfig AHP_ROOT = ConfigApiJava.registerAndLoadConfig(AhpRootConfig::new, RegisterType.BOTH);
    public static final AhpSupporterConfig AHP_SUPPORTER = ConfigApiJava.registerAndLoadConfig(AhpSupporterConfig::new, RegisterType.BOTH);
    public static final AhpMainConfig AHP_MAIN = ConfigApiJava.registerAndLoadConfig(AhpMainConfig::new, RegisterType.BOTH);
    public static final AhpItemConfig AHP_ITEMS = ConfigApiJava.registerAndLoadConfig(AhpItemConfig::new, RegisterType.BOTH);
    public static final AhpUiConfig AHP_UI = ConfigApiJava.registerAndLoadConfig(AhpUiConfig::new, RegisterType.BOTH);
    public static final AhpWorldGenConfig AHP_WORLDGEN = ConfigApiJava.registerAndLoadConfig(AhpWorldGenConfig::new, RegisterType.BOTH);

    private Configs() {} // prevent instantiation
}