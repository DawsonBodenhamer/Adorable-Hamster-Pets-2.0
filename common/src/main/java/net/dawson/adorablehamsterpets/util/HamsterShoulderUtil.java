package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.AhpMainConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import java.util.Optional;

/**
 * Coordinates hamster shoulder release, restoration, and placement.
 */
public final class HamsterShoulderUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Static Shoulder Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Shoulder Restoration and Placement ---

    public static void spawnFromNbt(
            ServerLevel world,
            Player player,
            CompoundTag nbt,
            boolean wasDiamondAlertActive,
            boolean forceStand) {
        // --- Restore Hamster State ---
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
            AdorableHamsterPets.LOGGER.debug(
                    "[HamsterEntity {}] Primed for diamond seeking upon dismount.",
                    hamster.getId());
        }

        // --- Find a Safe Placement ---
        BlockPos fallbackPos = player.blockPosition();
        HitResult hitResult = player.pick(4.5, 0.0f, false);
        BlockPos initialSearchPos =
                hitResult.getType() == HitResult.Type.BLOCK
                        ? ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos()
                        : fallbackPos;
        Optional<BlockPos> safePos =
                HamsterPlacementUtil.findSafeSpawnPosition(initialSearchPos, world, 5, hamster);

        safePos.ifPresentOrElse(
                pos -> {
                    hamster.moveTo(
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            player.getYRot(),
                            player.getXRot());
                    AdorableHamsterPets.LOGGER.debug(
                            "[HamsterDismount] Found safe spawn at {} for player {}.",
                            pos,
                            player.getName().getString());
                },
                () -> {
                    AdorableHamsterPets.LOGGER.warn(
                            "[HamsterDismount] Could not find a safe spawn position for player {}."
                                + " Spawning at player's feet as a fallback.",
                            player.getName().getString());
                    hamster.moveTo(
                            fallbackPos.getX() + 0.5,
                            fallbackPos.getY(),
                            fallbackPos.getZ() + 0.5,
                            player.getYRot(),
                            player.getXRot());
                });

        // --- Complete World Restoration ---
        world.addFreshEntityWithPassengers(hamster);
        AdorableHamsterPets.LOGGER.debug(
                "[HamsterEntity] Spawned Hamster ID {} from NBT data near Player {}.",
                hamster.getId(),
                player.getName().getString());
    }

    // --- Player-Initiated Throwing ---

    public static void tryThrowFromShoulder(ServerPlayer player) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;
        AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;

        if (!config.enableHamsterThrowing) {
            player.displayClientMessage(
                    Component.translatable("message.adorablehamsterpets.throwing_disabled"), true);
            return;
        }

        if (!playerAccessor.hasAnyShoulderHamster()) {
            AdorableHamsterPets.LOGGER.warn(
                    "[HamsterThrow] Player {} tried to throw, but has no shoulder hamster.",
                    player.getName().getString());
            return;
        }

        playerAccessor.adorablehamsterpets$dismountShoulderHamster(true);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterShoulderUtil() {}
}
