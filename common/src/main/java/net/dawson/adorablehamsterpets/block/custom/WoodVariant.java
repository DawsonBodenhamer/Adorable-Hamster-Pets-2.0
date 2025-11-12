package net.dawson.adorablehamsterpets.block.custom;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum WoodVariant implements StringIdentifiable {
    ACACIA("acacia"),
    BAMBOO("bamboo"),
    BIRCH("birch"),
    CHERRY("cherry"),
    DARK_OAK("dark_oak"),
    JUNGLE("jungle"),
    MANGROVE("mangrove"),
    OAK("oak"),
    PALE_OAK("pale_oak"),
    SPRUCE("spruce");

    public static final Codec<WoodVariant> CODEC = StringIdentifiable.createCodec(WoodVariant::values);
    private final String name;

    WoodVariant(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return this.name;
    }
}