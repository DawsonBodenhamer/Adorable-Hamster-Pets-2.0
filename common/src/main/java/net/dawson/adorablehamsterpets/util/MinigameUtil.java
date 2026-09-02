package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates reusable reward and failure logic for various hamster mini-games.
 */
public final class MinigameUtil {

    private MinigameUtil() {}

    /**
     * Sends a jiggle packet and spawns sound/particles that guides the player
     * towards a specific block where an entity is hiding.
     */
    public static void executePeriodicBlockLocationHint(Entity hiderEntity, BlockPos anchorPos, SoundEvent sound, float soundVol, float soundPitch, ParticleOptions particle, int particleCount, Vec3 particleSpread, double particleSpeed) {
        Level world = hiderEntity.level();
        if (world.isClientSide()) return;

        // Trigger client side matrix deformation
        world.broadcastEntityEvent(hiderEntity, (byte) 60);

        if (anchorPos != null) {
            world.playSound(null, anchorPos, sound, SoundSource.NEUTRAL, soundVol, soundPitch);

            ParticleEffectsUtil.spawnParticles(
                    world,
                    Vec3.atCenterOf(anchorPos),
                    particle,
                    particleCount,
                    particleSpread,
                    particleSpeed
            );
        }
    }

    /**
     * Spawns particles directly on a hidden entity.
     */
    public static void executeOngoingBlockLocationHint(Entity hiderEntity, ParticleOptions particle, int count, double widthScale, double heightScale, double speed, double yOffset) {
        if (hiderEntity.level().isClientSide()) return;

        ParticleEffectsUtil.spawnParticlesOnEntity(
                hiderEntity,
                particle,
                count,
                widthScale,
                heightScale,
                speed,
                yOffset
        );
    }

    /**
     * Spawns particles along a path between the seeker and the hidden entity.
     */
    public static void executeBreadcrumbHint(Entity hiderEntity, Player seeker, double minDistanceSq, double startPercent, double percentRange, ParticleOptions particle, int count, Vec3 spread, double speed) {
        Level world = hiderEntity.level();
        if (world.isClientSide() || seeker == null) return;

        if (seeker.distanceToSqr(hiderEntity) < minDistanceSq) {
            // Pick random percentage along path, skipping portion nearest player
            double pct = startPercent + (world.getRandom().nextDouble() * percentRange);

            // Calculate coordinates between player and hidden hamster
            double spawnX = seeker.getX() + (hiderEntity.getX() - seeker.getX()) * pct;
            double spawnY = (seeker.getY() + 0.3) + (hiderEntity.getY() - (seeker.getY() + 0.3)) * pct; // Slight Y boost
            double spawnZ = seeker.getZ() + (hiderEntity.getZ() - seeker.getZ()) * pct;

            ParticleEffectsUtil.spawnParticles(
                    world,
                    new Vec3(spawnX, spawnY, spawnZ),
                    particle,
                    count,
                    spread.x, spread.y, spread.z,
                    speed
            );
        }
    }

    /**
     * Executes the 3-stage gift delivery sequence:
     * 1. Unload animation
     * 2. Item appears in mouth
     * 3. Item is spat out
     */
    public static void executeGiftDeliverySequence(HamsterEntity hamster, ItemStack giftStack, @Nullable Player targetPlayer) {
        // Protect the sequence from being interrupted by other AI goals
        hamster.setFrozenMovement(true);

        // If target player provided, wait until close to them
        if (targetPlayer != null && targetPlayer.isAlive() && hamster.distanceToSqr(targetPlayer) > 4.0) {
            hamster.setCelebrationTicks(15); // Buffer until next check
            hamster.getNavigation().moveTo(targetPlayer, 1.25D);
            HamsterMovementUtil.faceEntity(hamster, targetPlayer);

            // Re-evaluate distance in 10 ticks
            hamster.scheduleTask(hamster.level().getGameTime() + 10, "move_to_gift_target", () -> {
                executeGiftDeliverySequence(hamster, giftStack, targetPlayer);
            });
            return;
        }

        // Close enough (or no target). Stop moving and deliver
        hamster.getNavigation().stop();
        if (targetPlayer != null) {
            hamster.setCelebrationTarget(targetPlayer);

            HamsterMovementUtil.faceEntity(hamster, targetPlayer);
        }

        long currentTime = hamster.level().getGameTime();

        // Trigger Unload Animation
        hamster.triggerAnimOnServer("mainController", "anim_hamster_cheek_unload");

        // Lock movement for the duration of the sequence
        hamster.setFrozenMovement(true);
        hamster.setCelebrationTicks(43); // Animation is 43 ticks

        // T+10 (relative to start of gift sequence): Hamster "moves item" from cheek to mouth
        hamster.scheduleTask(currentTime + 10, "gift_appear", () -> {
            hamster.setMouthItemStack(giftStack);
            hamster.setHoldingMouthItem(true);
            hamster.setGenericInteractionTimer(0);
        });

        // T+33 (relative to start of gift sequence): Hamster spits out item
        hamster.scheduleTask(currentTime + 33, "gift_spit", () -> {
            if (hamster.isHoldingMouthItem() && !hamster.getMouthItemStack().isEmpty()) {
                Vec3 look = hamster.getViewVector(1.0f);
                ItemEntity itemEntity = new ItemEntity(hamster.level(),
                        hamster.getX() + look.x * 0.5,
                        hamster.getY() + 0.3,
                        hamster.getZ() + look.z * 0.5,
                        hamster.getMouthItemStack().copy()
                );
                // Forward velocity to item
                itemEntity.setDeltaMovement(look.x * 0.2, 0.2, look.z * 0.2);
                hamster.level().addFreshEntity(itemEntity);
            }
            // Cleanup
            hamster.setMouthItemStack(ItemStack.EMPTY);
            hamster.setHoldingMouthItem(false);
        });
    }

    /**
     * Executes the generic failure reaction when a minigame goes wrong.
     * Causes the hamster to jump back, play a startled sound, and enter the sulking state.
     */
    public static void executeSulkFailure(HamsterEntity hamster, @Nullable Vec3 sourceOfStartle) {
        // Apply small backward and upward startled jump velocity
        Vec3 away;
        if (sourceOfStartle != null) {
            away = hamster.position().subtract(sourceOfStartle).normalize();
        } else {
            away = new Vec3(0, 0, 0);
        }

        hamster.setDeltaMovement(away.x * 0.1, 0.5, away.z * 0.1);
        hamster.needsSync = true;

        SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, hamster.getRandom());
        if (bounceSound != null) {
            hamster.level().playSound(null, hamster.blockPosition(), bounceSound, SoundSource.NEUTRAL, 0.6f, hamster.getVoicePitch());
        }

        hamster.setSulking(true);
        hamster.setFrozenMovement(true);
        hamster.setCelebrationTicks(63); // 63 ticks for anim_hamster_sulk
        hamster.triggerAnimOnServer("mainController", "anim_hamster_sulk");
    }

    /**
     * Selects a random item from the Default or Extra cheek pouch loot lists.
     * Prioritizes lists that actually contain items. If configured,
     * it pulls exclusively from a custom mini-game rewards list.
     */
    public static Item getRandomMiniGameReward(HamsterEntity hamster) {
        if (!Configs.AHP_MAIN.usePouchLootForMiniGameRewards) {
            return ConfigDataCache.getRandomCustomMiniGameReward(hamster.getRandom());
        }

        List<Integer> validPools = new ArrayList<>();
        validPools.add(0); // Default is always valid

        // Check if Extra Loot list has entries
        if (!Configs.AHP_WORLDGEN.extraCheekLootList.isEmpty()) {
            validPools.add(1);
        }

        int selectedPool = validPools.get(hamster.getRandom().nextInt(validPools.size()));

        return (selectedPool == 1)
                ? ConfigDataCache.getRandomCustomLootItem(hamster.getRandom())
                : ConfigDataCache.getRandomDefaultLootItem(hamster.getRandom());
    }
}