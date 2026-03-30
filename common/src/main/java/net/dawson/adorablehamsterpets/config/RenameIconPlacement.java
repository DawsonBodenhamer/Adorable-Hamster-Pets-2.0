package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum RenameIconPlacement implements EnumTranslatable {
    LEFT,
    RIGHT;

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.rename_icon_placement";
    }

    @NotNull
    @Override
    public String translationKey() {
        return prefix() + "." + this.name().toLowerCase(Locale.ROOT);
    }
}