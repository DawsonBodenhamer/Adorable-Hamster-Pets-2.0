package net.dawson.adorablehamsterpets.config;

import com.mojang.serialization.Codec;
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;

public enum WanderDistance implements StringIdentifiable, EnumTranslatable {
    NEAR("Near"),
    MEDIUM("Medium"),
    FAR("Far");

    public static final Codec<WanderDistance> CODEC = StringIdentifiable.createCodec(WanderDistance::values);
    private final String name;

    WanderDistance(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return this.name;
    }

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.wander_distance";
    }
}