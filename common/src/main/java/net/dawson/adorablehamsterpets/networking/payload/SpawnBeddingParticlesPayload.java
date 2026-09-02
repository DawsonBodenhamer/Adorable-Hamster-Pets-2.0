package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SpawnBeddingParticlesPayload(BlockPos pos, Direction direction, WoodVariant variant) implements CustomPacketPayload {
    public static final Type<SpawnBeddingParticlesPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "spawn_bedding_particles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnBeddingParticlesPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SpawnBeddingParticlesPayload::pos,
            ByteBufCodecs.idMapper(Direction::from3DDataValue, Direction::get3DDataValue), SpawnBeddingParticlesPayload::direction,
            ByteBufCodecs.idMapper(i -> WoodVariant.values()[i], WoodVariant::ordinal), SpawnBeddingParticlesPayload::variant,
            SpawnBeddingParticlesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}