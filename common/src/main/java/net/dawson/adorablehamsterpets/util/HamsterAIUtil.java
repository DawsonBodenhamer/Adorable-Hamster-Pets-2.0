package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

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
        float oldWaterPenalty = hamster.getPathfindingPenalty(PathNodeType.WATER);
        hamster.setPathfindingPenalty(PathNodeType.WATER, 0.0F);

        List<ItemEntity> nearbyItems = hamster.getWorld().getEntitiesByClass(
                ItemEntity.class,
                hamster.getBoundingBox().expand(radius),
                itemEntity -> !itemEntity.isRemoved() && filter.test(itemEntity)
        );

        Optional<ItemEntity> closestItem = nearbyItems.stream()
                .filter(item -> hamster.getNavigation().findPathTo(item, 1) != null)
                .min((item1, item2) -> Float.compare(item1.distanceTo(hamster), item2.distanceTo(hamster)));

        // Restore penalty after evaluation
        hamster.setPathfindingPenalty(PathNodeType.WATER, oldWaterPenalty);

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
        if (hamster.isKnockedOut() || !hamster.isSitting()) {
            if (hamster.getHamsterFlag(HamsterEntity.CLEANING_FLAG)) {
                hamster.setHamsterFlag(HamsterEntity.CLEANING_FLAG, false);
            }
            hamster.ambientSittingTimer = 0;
            return;
        }

        // --- 3. Evaluate Start Conditions ---
        if (!hamster.isTamed() || hamster.ambientSittingCooldown > 0 || hamster.ambientSittingTimer > 0) {
            return;
        }

        // Prevent ambient behaviors if starting to doze off
        HamsterEntity.DozingPhase currentPhase = hamster.getDozingPhase();
        if (currentPhase != HamsterEntity.DozingPhase.NONE && currentPhase != HamsterEntity.DozingPhase.QUIESCENT_SITTING) {
            return;
        }

        // --- 4. Roll for Behaviors ---
        // Order technically dictates priority if both hit on same tick
        Random random = hamster.getRandom();

        // Behavior A: Looping Cleaning Animation
        int cleaningChance = Configs.AHP_MAIN.cleaningChanceDenominator.get();
        if (cleaningChance > 0 && random.nextInt(cleaningChance) == 0) {
            hamster.ambientSittingTimer = random.nextBetween(30, 60);
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
        World world = hamster.getWorld();

        for (BlockPos p : BlockPos.iterateOutwards(hamster.getBlockPos(), 8, 4, 8)) {
            if (world.getBlockState(p).isOf(Blocks.JUKEBOX)) {
                if (world.getBlockEntity(p) instanceof JukeboxBlockEntity jbe) {

                    // 1.20.1: Check if jukebox is actively playing and has record
                    if (jbe.isPlayingRecord() && !jbe.getStack().isEmpty()) {
                        ItemStack discStack = jbe.getStack();

                        if (discStack.getItem() instanceof MusicDiscItem discItem) {

                            // Check AHP theme song
                            SoundEvent currentSound = discItem.getSound();

                            if (currentSound.equals(ModSounds.AHP_THEME_SONG_8_BIT.get()) ||
                                    currentSound.equals(ModSounds.AHP_THEME_SONG_LOW_FI.get()) ||
                                    currentSound.equals(ModSounds.AHP_THEME_SONG_ORCHESTRAL.get())) {
                                return true;
                            }

                            // Check dynamic config strings
                            if (!Configs.AHP_ITEMS.dancingMusicDiscStrings.isEmpty()) {
                                String songDesc = discItem.getDescription().getString().toLowerCase(Locale.ROOT);
                                String itemName = discStack.getName().getString().toLowerCase(Locale.ROOT);
                                String itemKey = discStack.getTranslationKey().toLowerCase(Locale.ROOT);

                                // 1.20.1: Parse Lore from NBT instead of Data Components
                                NbtList loreList = null;
                                NbtCompound nbt = discStack.getNbt();

                                if (nbt != null && nbt.contains(ItemStack.DISPLAY_KEY, NbtElement.COMPOUND_TYPE)) {
                                    NbtCompound display = nbt.getCompound(ItemStack.DISPLAY_KEY);
                                    if (display.contains(ItemStack.LORE_KEY, NbtElement.LIST_TYPE)) {
                                        loreList = display.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
                                    }
                                }

                                for (String searchStr : Configs.AHP_ITEMS.dancingMusicDiscStrings) {
                                    String lowerSearch = searchStr.toLowerCase(Locale.ROOT);

                                    if (songDesc.contains(lowerSearch) || itemName.contains(lowerSearch) || itemKey.contains(lowerSearch)) {
                                        return true;
                                    }

                                    if (loreList != null) {
                                        for (int i = 0; i < loreList.size(); i++) {
                                            String lineJson = loreList.getString(i);
                                            try {
                                                // 1.20.1: Lore is stored as JSON text
                                                Text lineText = Text.Serializer.fromJson(lineJson);
                                                if (lineText != null && lineText.getString().toLowerCase(Locale.ROOT).contains(lowerSearch)) {
                                                    return true;
                                                }
                                            } catch (Exception e) {
                                                // Fallback if raw string matches
                                                if (lineJson.toLowerCase(Locale.ROOT).contains(lowerSearch)) {
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
            }
        }
        return false;
    }
}