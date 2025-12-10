package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

public enum DismountOrder implements EnumTranslatable {
    LIFO, // Last-In, First-Out
    FIFO; // First-In, First-Out

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.dismount_order";
    }
}