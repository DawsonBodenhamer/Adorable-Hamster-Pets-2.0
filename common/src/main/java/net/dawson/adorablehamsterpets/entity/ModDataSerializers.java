package net.dawson.adorablehamsterpets.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;

/**
 * 26.2 port: Fabric API forbids EntityDataSerializers.registerSerializer from
 * mods (sync-id drift), so registration goes through the platform hook.
 */
public final class ModDataSerializers {
    private ModDataSerializers() {}

    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG =
            EntityDataSerializer.forValueType(ByteBufCodecs.COMPOUND_TAG);

    static {
        registerPlatform("compound_tag", COMPOUND_TAG);
    }

    @ExpectPlatform
    public static void registerPlatform(String name, EntityDataSerializer<?> serializer) {
        throw new AssertionError();
    }
}
