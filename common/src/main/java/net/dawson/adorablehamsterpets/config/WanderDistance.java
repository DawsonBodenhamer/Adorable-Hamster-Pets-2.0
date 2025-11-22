package net.dawson.adorablehamsterpets.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum WanderDistance implements StringIdentifiable {
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
}