package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum DismountPressType implements EnumTranslatable {
    SINGLE_PRESS,
    DOUBLE_TAP;

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.dismount_press_type";
    }

    @NotNull
    @Override
    public String translationKey() {
        return prefix() + "." + this.name().toLowerCase(Locale.ROOT);
    }
}