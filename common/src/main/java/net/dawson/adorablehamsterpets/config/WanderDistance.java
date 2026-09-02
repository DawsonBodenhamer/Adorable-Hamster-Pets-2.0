package net.dawson.adorablehamsterpets.config;

import com.mojang.serialization.Codec;
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum WanderDistance implements StringRepresentable, EnumTranslatable {
    NEAR("Near"),
    MEDIUM("Medium"),
    FAR("Far");

    public static final Codec<WanderDistance> CODEC = StringRepresentable.fromEnum(WanderDistance::values);
    private final String name;

    WanderDistance(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.wander_distance";
    }

    @NotNull
    @Override
    public String translationKey() {
        return prefix() + "." + this.name().toLowerCase(Locale.ROOT);
    }
}