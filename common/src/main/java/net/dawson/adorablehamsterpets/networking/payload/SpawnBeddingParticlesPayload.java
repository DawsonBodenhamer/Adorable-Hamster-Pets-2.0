package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record SpawnBeddingParticlesPayload(BlockPos pos, Direction direction, WoodVariant variant) implements CustomPayload {
    public static final Id<SpawnBeddingParticlesPayload> ID = new Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "spawn_bedding_particles"));

    public static final PacketCodec<RegistryByteBuf, SpawnBeddingParticlesPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, SpawnBeddingParticlesPayload::pos,
            PacketCodecs.indexed(Direction::byId, Direction::getId), SpawnBeddingParticlesPayload::direction,
            PacketCodecs.indexed(i -> WoodVariant.values()[i], WoodVariant::ordinal), SpawnBeddingParticlesPayload::variant,
            SpawnBeddingParticlesPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}