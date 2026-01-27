package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkChannel;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static net.dawson.adorablehamsterpets.AdorableHamsterPets.MOD_ID;

/**
 * Manages network packet registration and handling for the 1.20.1 version of the mod.
 * Utilizes Architectury API's {@link NetworkChannel} to handle cross-platform networking.
 * <p>
 * Note: In Architectury 9 (1.20.1), packets must be registered on both logical sides
 * to ensure the NetworkChannel knows how to encode/decode them. Client-side handlers
 * are wrapped in {@link EnvExecutor} to prevent classloading crashes on the server.
 */
public class ModPackets {

    // --- 1. Create Network Channel ---
    public static final NetworkChannel CHANNEL = NetworkChannel.create(new Identifier(MOD_ID, "main"));

    // --- 2. Define Packet Data as Records ---
    // C2S (Client-to-Server)
    public record ThrowHamsterC2SPacket() {}
    public record DismountHamsterC2SPacket() {}
    public record UpdateRenderStateC2SPacket(int entityId, boolean isRendering) {}
    public record RequestGuidebookC2SPacket() {}
    public record RequestHamsterMountC2SPacket(int entityId) {}
    public record ResetHeistHistoryC2SPacket() {}

    // S2C (Server-to-Client)
    public record PlayGuidebookEffectsS2CPacket() {}
    public record SpawnBeddingParticlesS2CPacket(BlockPos pos, Direction direction, WoodVariant variant) {}
    public record SyncShoulderDataS2CPacket(int entityId, NbtCompound data) {}
    public record PlayDistantSoundS2CPacket(Identifier soundId, float volume, float pitch) {}

    /**
     * Registers all packet definitions and their handlers.
     * This method must be called during common setup on both client and server.
     */
    public static void registerCommonPackets() {

        // --- Client to Server (C2S) ---
        CHANNEL.register(ThrowHamsterC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new ThrowHamsterC2SPacket(),
                (packet, context) -> context.get().queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.get().getPlayer()))
        );

        CHANNEL.register(DismountHamsterC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new DismountHamsterC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof ServerPlayerEntity player) {
                        ((PlayerEntityAccessor) player).adorablehamsterpets$dismountShoulderHamster(false);
                    }
                })
        );

        CHANNEL.register(UpdateRenderStateC2SPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.entityId());
                    buf.writeBoolean(packet.isRendering());
                },
                (buf) -> new UpdateRenderStateC2SPacket(buf.readInt(), buf.readBoolean()),
                (packet, context) -> context.get().queue(() -> {
                    if (packet.isRendering()) {
                        HamsterRenderTracker.addPlayer(packet.entityId(), context.get().getPlayer().getUuid());
                    } else {
                        HamsterRenderTracker.removePlayer(packet.entityId(), context.get().getPlayer().getUuid());
                    }
                })
        );

        CHANNEL.register(RequestGuidebookC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new RequestGuidebookC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    ServerPlayerEntity player = (ServerPlayerEntity) context.get().getPlayer();
                    ItemStack bookStack = new ItemStack(ModItems.HAMSTER_GUIDE_BOOK.get());

                    // In 1.20.1, add the Patchouli ID to NBT
                    NbtCompound nbt = bookStack.getOrCreateNbt();
                    nbt.putString("patchouli:book", "adorablehamsterpets:hamster_tips_guide_book");

                    player.getInventory().offerOrDrop(bookStack);

                    // Send effects packet back to the player
                    CHANNEL.sendToPlayer(player, new PlayGuidebookEffectsS2CPacket());
                })
        );

        CHANNEL.register(RequestHamsterMountC2SPacket.class,
                (packet, buf) -> buf.writeInt(packet.entityId()),
                (buf) -> new RequestHamsterMountC2SPacket(buf.readInt()),
                (packet, context) -> context.get().queue(() -> {
                    net.minecraft.entity.player.PlayerEntity player = context.get().getPlayer();
                    net.minecraft.entity.Entity entity = player.getWorld().getEntityById(packet.entityId());
                    if (entity instanceof HamsterEntity hamster && hamster.isOwner(player)) {
                        // Distance check for security
                        if (hamster.squaredDistanceTo(player) < 64.0) {
                            // Use the 1.20.1 version of tryShoulderMount (check signature)
                            hamster.tryShoulderMount(player, net.minecraft.item.ItemStack.EMPTY);
                        }
                    }
                })
        );

        CHANNEL.register(ResetHeistHistoryC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new ResetHeistHistoryC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$clearHeistHistory();
                    }
                })
        );

        // --- Server to Client (S2C) ---
        CHANNEL.register(SpawnBeddingParticlesS2CPacket.class,
                (packet, buf) -> {
                    buf.writeBlockPos(packet.pos());
                    buf.writeEnumConstant(packet.direction());
                    buf.writeEnumConstant(packet.variant());
                },
                (buf) -> new SpawnBeddingParticlesS2CPacket(
                        buf.readBlockPos(),
                        buf.readEnumConstant(Direction.class),
                        buf.readEnumConstant(WoodVariant.class)
                ),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handleSpawnBeddingParticles(packet))
                )
        );

        CHANNEL.register(PlayGuidebookEffectsS2CPacket.class,
                (packet, buf) -> {},
                (buf) -> new PlayGuidebookEffectsS2CPacket(),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handlePlayGuidebookEffects())
                )
        );

        CHANNEL.register(SyncShoulderDataS2CPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.entityId());
                    buf.writeNbt(packet.data());
                },
                (buf) -> new SyncShoulderDataS2CPacket(buf.readInt(), buf.readNbt()),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handleSyncShoulderData(packet.entityId(), packet.data()))
                )
        );

        CHANNEL.register(PlayDistantSoundS2CPacket.class,
                (packet, buf) -> {
                    buf.writeIdentifier(packet.soundId());
                    buf.writeFloat(packet.volume());
                    buf.writeFloat(packet.pitch());
                },
                (buf) -> new PlayDistantSoundS2CPacket(
                        buf.readIdentifier(),
                        buf.readFloat(),
                        buf.readFloat()
                ),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handlePlayDistantSound(packet))
                )
        );
    }
}