package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

public enum ForcedShoulderState implements EnumTranslatable {
    ALWAYS_STAND,
    ALWAYS_SIT,
    ALWAYS_LAY_DOWN;

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.forced_shoulder_state";
    }
}