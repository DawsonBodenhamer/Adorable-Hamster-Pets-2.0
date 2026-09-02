package net.dawson.adorablehamsterpets.config;

import com.mojang.serialization.Codec;
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum LitterLimitType implements StringRepresentable, EnumTranslatable {
    DAILY("Daily"),
    LIFETIME("Lifetime");

    public static final Codec<LitterLimitType> CODEC = StringRepresentable.fromEnum(LitterLimitType::values);
    private final String name;

    LitterLimitType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.litter_limit_type";
    }

    @NotNull
    @Override
    public String translationKey() {
        return prefix() + "." + this.name().toLowerCase(Locale.ROOT);
    }
}