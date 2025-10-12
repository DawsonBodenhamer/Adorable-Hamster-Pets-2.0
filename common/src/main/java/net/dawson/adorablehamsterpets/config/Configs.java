package net.dawson.adorablehamsterpets.config;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

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
