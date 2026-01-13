package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

public enum MountPriority implements EnumTranslatable {
    SHOULDERS_FIRST,
    HEAD_FIRST;

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.mount_priority";
    }
}