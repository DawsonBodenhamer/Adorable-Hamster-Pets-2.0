package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;

/**
 * Static holder for the Adorable Hamster Pets configs.
 * Touching {@code Configs.AHP_MAIN} guarantees the config is registered,
 * loaded from file, and its sync/GUI channels are ready.
 */
public final class Configs {

    /** Global, sync-enabled, GUI-enabled config instance. */
    public static final AhpRootConfig AHP_ROOT = ConfigApiJava.registerAndLoadConfig(AhpRootConfig::new);
    public static final AhpSupporterConfig AHP_SUPPORTER = ConfigApiJava.registerAndLoadConfig(AhpSupporterConfig::new);
    public static final AhpMainConfig AHP_MAIN = ConfigApiJava.registerAndLoadConfig(AhpMainConfig::new);
    public static final AhpItemConfig AHP_ITEMS = ConfigApiJava.registerAndLoadConfig(AhpItemConfig::new);
    public static final AhpUiConfig AHP_UI = ConfigApiJava.registerAndLoadConfig(AhpUiConfig::new);
    public static final AhpWorldGenConfig AHP_WORLDGEN = ConfigApiJava.registerAndLoadConfig(AhpWorldGenConfig::new);

    private Configs() {} // prevent instantiation
}