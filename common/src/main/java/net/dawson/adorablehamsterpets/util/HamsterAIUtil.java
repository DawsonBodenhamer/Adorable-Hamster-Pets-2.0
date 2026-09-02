package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.pathfinder.PathType;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Encapsulates passive, environment-scanning AI logic for Hamsters.
 */
public final class HamsterAIUtil {

    private HamsterAIUtil() {}

    /**
     * Scans for a nearby dropped item that matches the given filter and is physically reachable.
     * Temporarily bypasses the water pathfinding penalty to ensure accurate reachability checks.
     *
     * @param hamster The hamster searching for items.
     * @param radius  The search radius in blocks.
     * @param filter  A predicate to determine if the item is desirable.
     * @return An Optional containing the closest reachable ItemEntity, or empty if none found.
     */
    public static Optional<ItemEntity> findReachableItem(HamsterEntity hamster, double radius, Predicate<ItemEntity> filter) {
        // Temporarily clear water penalty to allow pathing into water if that's where item is
        float oldWaterPenalty = hamster.getPathfindingMalus(PathType.WATER);
        hamster.setPathfindingMalus(PathType.WATER, 0.0F);

        List<ItemEntity> nearbyItems = hamster.level().getEntitiesOfClass(
                ItemEntity.class,
                hamster.getBoundingBox().inflate(radius),
                itemEntity -> !itemEntity.isRemoved() && filter.test(itemEntity)
        );

        Optional<ItemEntity> closestItem = nearbyItems.stream()
                .filter(item -> hamster.getNavigation().createPath(item, 1) != null)
                .min((item1, item2) -> Float.compare(item1.distanceTo(hamster), item2.distanceTo(hamster)));

        // Restore penalty after evaluation
        hamster.setPathfindingMalus(PathType.WATER, oldWaterPenalty);

        return closestItem;
    }

    /**
     * Centralized state machine for handling ambient/idle behaviors when a tamed hamster is sitting.
     * Manages a shared timer to prevent multiple actions from overlapping, and a shared cooldown
     * to prevent spamming.
     */
    public static void tickAmbientSittingBehaviors(HamsterEntity hamster) {
        // --- 1. Decrement Timers & Handle Expiration ---
        if (hamster.ambientSittingCooldown > 0) {
            hamster.ambientSittingCooldown--;
        }

        if (hamster.ambientSittingTimer > 0) {
            hamster.ambientSittingTimer--;
            if (hamster.ambientSittingTimer == 0) {
                // Action complete, clear flags for loops
                if (hamster.getHamsterFlag(HamsterEntity.CLEANING_FLAG)) {
                    hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
                }
                hamster.ambientSittingCooldown = 200; // Shared cooldown
            }
        }

        // --- 2. Handle External Interruptions ---
        if (hamster.isKnockedOut() || !hamster.isOrderedToSit()) {
            if (hamster.getHamsterFlag(HamsterEntity.CLEANING_FLAG)) {
                hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
            }
            hamster.ambientSittingTimer = 0;
            return;
        }

        // --- 3. Evaluate Start Conditions ---
        if (!hamster.isTame() || hamster.ambientSittingCooldown > 0 || hamster.ambientSittingTimer > 0) {
            return;
        }

        // Prevent ambient behaviors if starting to doze off
        HamsterEntity.DozingPhase currentPhase = hamster.getDozingPhase();
        if (currentPhase != HamsterEntity.DozingPhase.NONE && currentPhase != HamsterEntity.DozingPhase.QUIESCENT_SITTING) {
            return;
        }

        // --- 4. Roll for Behaviors ---
        // Order technically dictates priority if both hit on same tick
        RandomSource random = hamster.getRandom();

        // Behavior A: Looping Cleaning Animation
        int cleaningChance = Configs.AHP_MAIN.cleaningChanceDenominator.get();
        if (cleaningChance > 0 && random.nextInt(cleaningChance) == 0) {
            hamster.ambientSittingTimer = random.nextIntBetweenInclusive(30, 60);
            hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, true);
            return; // Exit after successful trigger
        }

        // Behavior B: Triggerable Rolling Animation
        int rollingChance = Configs.AHP_MAIN.rollingChanceDenominator.get();
        if (rollingChance > 0 && random.nextInt(rollingChance) == 0) {
            // Assign a timer lock so it isn't interrupted by other ambient systems
            hamster.ambientSittingTimer = 60; // Anim length
            hamster.triggerAnimOnServer("mainController", "sitting_roll");
            return; // Exit after successful trigger
        }
    }

    /**
     * Scans for a nearby jukebox actively playing the Cheese Music Disc or any configured
     * custom discs to which the hamster is intended to dance.
     */
    public static boolean isDancingSongPlayingNearby(HamsterEntity hamster) {
        Level world = hamster.level();

        for (BlockPos p : BlockPos.withinManhattan(hamster.blockPosition(), 8, 4, 8)) {
            if (world.getBlockState(p).is(Blocks.JUKEBOX)) {
                if (world.getBlockEntity(p) instanceof JukeboxBlockEntity jbe) {
                    if (jbe.getSongPlayer().isPlaying() && jbe.getSongPlayer().getSong() != null) {
                        JukeboxSong song = jbe.getSongPlayer().getSong();

                        // Check AHP theme song
                        SoundEvent currentSong = song.soundEvent().value();
                        if (currentSong.equals(ModSounds.AHP_THEME_SONG_8_BIT.get()) ||
                                currentSong.equals(ModSounds.AHP_THEME_SONG_LOW_FI.get()) ||
                                currentSong.equals(ModSounds.AHP_THEME_SONG_ORCHESTRAL.get())) {
                            return true;
                        }

                        // Check dynamic config strings
                        if (!Configs.AHP_ITEMS.dancingMusicDiscStrings.isEmpty()) {
                            ItemStack discStack = jbe.getTheItem();
                            String songDesc = song.description().getString().toLowerCase(Locale.ROOT);
                            String itemName = discStack.getHoverName().getString().toLowerCase(Locale.ROOT);
                            String itemKey = discStack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);

                            ItemLore lore = discStack.get(DataComponents.LORE);

                            for (String searchStr : Configs.AHP_ITEMS.dancingMusicDiscStrings) {
                                String lowerSearch = searchStr.toLowerCase(Locale.ROOT);

                                if (songDesc.contains(lowerSearch) || itemName.contains(lowerSearch) || itemKey.contains(lowerSearch)) {
                                    return true;
                                }

                                if (lore != null) {
                                    for (Component line : lore.lines()) {
                                        if (line.getString().toLowerCase(Locale.ROOT).contains(lowerSearch)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}