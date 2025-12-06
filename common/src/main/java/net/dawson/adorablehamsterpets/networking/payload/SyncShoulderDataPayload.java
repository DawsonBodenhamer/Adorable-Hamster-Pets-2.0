package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncShoulderDataPayload(int entityId, NbtCompound data) implements CustomPayload {
    public static final CustomPayload.Id<SyncShoulderDataPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "sync_shoulder_data"));

    public static final PacketCodec<RegistryByteBuf, SyncShoulderDataPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SyncShoulderDataPayload::entityId,
            PacketCodecs.NBT_COMPOUND, SyncShoulderDataPayload::data,
            SyncShoulderDataPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}