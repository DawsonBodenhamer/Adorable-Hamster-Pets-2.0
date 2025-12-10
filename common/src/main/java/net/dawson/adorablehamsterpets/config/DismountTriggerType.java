package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

public enum DismountTriggerType implements EnumTranslatable {
    SNEAK_KEY,
    CUSTOM_KEYBIND;

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.dismount_trigger_type";
    }
}