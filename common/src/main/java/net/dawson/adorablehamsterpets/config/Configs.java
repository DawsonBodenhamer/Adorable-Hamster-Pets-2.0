package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;

/**
 * Static holder for the single Adorable Hamster Pets config.
 * Touching {@code Configs.AHP} guarantees the config is registered,
 * loaded from file, and its sync/GUI channels are ready.
 */
public final class Configs {

    /** Global, sync-enabled, GUI-enabled config instance. */
    public static final AhpConfig AHP =
            ConfigApiJava.registerAndLoadConfig(AhpConfig::new, RegisterType.BOTH);

    private Configs() {} // prevent instantiation
}
