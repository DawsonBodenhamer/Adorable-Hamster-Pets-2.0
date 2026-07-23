package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.AhpMainConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Coordinates hamster shoulder release, restoration, and placement.
 */
public final class HamsterShoulderUtil {

    private HamsterShoulderUtil() {}

    public static void spawnFromNbt(ServerWorld world, PlayerEntity player, NbtCompound nbt,
                                    boolean wasDiamondAlertActive, boolean forceStand) {
        HamsterEntity hamster = HamsterNbtUtil.createFromNbt(world, player, nbt);
        if (hamster == null) {
            return;
        }

        if (forceStand) {
            hamster.setSitting(false, true);
        }

        hamster.suffocationGracePeriod = 200;

        if (wasDiamondAlertActive && Configs.AHP_MAIN.enableIndependentDiamondSeeking) {
            hamster.isPrimedToSeekDiamonds = true;
            AdorableHamsterPets.LOGGER.debug("[HamsterEntity {}] Primed for diamond seeking upon dismount.", hamster.getId());
        }

        BlockPos fallbackPos = player.getBlockPos();
        HitResult hitResult = player.raycast(4.5, 0.0f, false);
        BlockPos initialSearchPos = hitResult.getType() == HitResult.Type.BLOCK
                ? ((net.minecraft.util.hit.BlockHitResult) hitResult).getBlockPos()
                : fallbackPos;
        Optional<BlockPos> safePos = HamsterPlacementUtil.findSafeSpawnPosition(initialSearchPos, world, 5, hamster);

        safePos.ifPresentOrElse(
                pos -> {
                    hamster.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
                    AdorableHamsterPets.LOGGER.debug("[HamsterDismount] Found safe spawn at {} for player {}.", pos, player.getName().getString());
                },
                () -> {
                    AdorableHamsterPets.LOGGER.warn("[HamsterDismount] Could not find a safe spawn position for player {}. Spawning at player's feet as a fallback.", player.getName().getString());
                    hamster.refreshPositionAndAngles(fallbackPos.getX() + 0.5, fallbackPos.getY(), fallbackPos.getZ() + 0.5, player.getYaw(), player.getPitch());
                }
        );

        world.spawnEntityAndPassengers(hamster);
        AdorableHamsterPets.LOGGER.debug("[HamsterEntity] Spawned Hamster ID {} from NBT data near Player {}.", hamster.getId(), player.getName().getString());
    }

    public static void tryThrowFromShoulder(ServerPlayerEntity player) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;
        AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;

        if (!config.enableHamsterThrowing) {
            player.sendMessage(Text.translatable("message.adorablehamsterpets.throwing_disabled"), true);
            return;
        }

        if (!playerAccessor.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.warn("[HamsterThrow] Player {} tried to throw, but has no shoulder hamster.", player.getName().getString());
            return;
        }

        playerAccessor.adorablehamsterpets$dismountShoulderHamster(true);
    }
}
