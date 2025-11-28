package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

public enum IconPositionPreset implements EnumTranslatable {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    @NotNull
    @Override
    public String prefix() {
        // This prefix is combined with the enum name (lowercase) to create the key.
        // Example: config.adorablehamsterpets.enum.icon_position.top_left
        return "config.adorablehamsterpets.enum.icon_position";
    }
}