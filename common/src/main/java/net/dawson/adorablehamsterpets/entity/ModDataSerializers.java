package net.dawson.adorablehamsterpets.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

/**
 * 26.2 port: vanilla dropped its CompoundTag synched-data serializer. The
 * hamster syncs its genome as NBT, so one is registered here. Referencing this
 * class from an entity's static initialiser is enough to register it before
 * any defineId() call runs.
 */
public final class ModDataSerializers {
    private ModDataSerializers() {}

    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG =
            EntityDataSerializer.forValueType(ByteBufCodecs.COMPOUND_TAG);

    static {
        EntityDataSerializers.registerSerializer(COMPOUND_TAG);
    }
}
