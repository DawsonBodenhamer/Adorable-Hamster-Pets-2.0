package net.dawson.adorablehamsterpets.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.networking.payload.PlayGuidebookEffectsPayload;
import net.dawson.adorablehamsterpets.util.EntityTargetingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.Collection;
import java.util.List;

public class PlayerCommandUtil {

    /**
     * Grants all Adorable Hamster Pets advancements to the commanding player.
     */
    public static int executeUnlockAllModAdvancements(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAdvancements tracker = player.getAdvancements();
        Collection<AdvancementHolder> allAdvancements = source.getServer().getAdvancements().getAllAdvancements();
        int count = 0;

        for (AdvancementHolder advancementEntry : allAdvancements) {
            ResourceLocation id = advancementEntry.id();
            if (id.getNamespace().equals(AdorableHamsterPets.MOD_ID) &&
                    (id.getPath().startsWith("husbandry/"))) {

                AdvancementProgress progress = tracker.getOrStartProgress(advancementEntry);
                if (!progress.isDone()) {
                    for (String criterion : advancementEntry.value().criteria().keySet()) {
                        tracker.award(advancementEntry, criterion);
                    }
                    count++;
                }
            }
        }

        final int finalCount = count;
        if (finalCount > 0) {
            source.sendSuccess(() -> Component.literal("Unlocked " + finalCount + " Adorable Hamster Pets advancements."), true);
        } else {
            source.sendSuccess(() -> Component.literal("No new Adorable Hamster Pets advancements to unlock or all already unlocked."), true);
        }
        return finalCount;
    }

    /**
     * Executes visual effects and sound intended for discovering the guidebook.
     */
    public static int executeTriggerBookEffects(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NetworkManager.sendToPlayer(player, new PlayGuidebookEffectsPayload(false));
        return 1;
    }

    /**
     * Spawns a new copy of the Hamster Tips guide book into the player's inventory.
     */
    public static int executeGiveGuidebook(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        // Deliver guidebook: no advancement flag, no fallback message, play effect, do not close GUI.
        AdorableHamsterPets.deliverGuidebook(player, false, false, true, false);

        // Update tracking cache so the warning doesn't trigger
        ((PlayerEntityAccessor) player).ahp$initGuideBookTracking(true);
        return 1;
    }

    /**
     * Wipes the player's local NBT memory regarding which trees they've exhausted.
     */
    public static int executeResetHeistHistory(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ((PlayerEntityAccessor) player).ahp$clearHeistHistory();
        return 1;
    }

    /**
     * Clears the player's daily or lifetime limit on hamster breeding.
     */
    public static int executeResetPlayerBreedingHistory(CommandSourceStack source, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        for (ServerPlayer target : targets) {
            ((PlayerEntityAccessor) target).ahp$resetBreedingHistory();
        }
        return targets.size();
    }

    /**
     * Resets the breeding history for specific hamsters.
     * If no targets are provided, targets the nearest hamster within 5 blocks.
     */
    public static int executeResetHamsterBreedingHistory(CommandSourceStack source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int count = 0;

        if (targets.isEmpty()) {
            HamsterEntity target = getTargetHamster(source.getPlayerOrException());
            if (target == null) {
                source.sendSuccess(() -> Component.literal("No hamster found within 5 blocks. Look closer or specify a target.").withStyle(ChatFormatting.RED), false);
                return 0;
            }
            target.timesBred = 0;
            count++;
        } else {
            for (Entity entity : targets) {
                if (entity instanceof HamsterEntity hamster) {
                    hamster.timesBred = 0;
                    count++;
                }
            }
        }

        final int finalCount = count;
        source.sendSuccess(() -> Component.literal("Reset times bred to 0 for " + finalCount + " hamster(s).").withStyle(ChatFormatting.GREEN), true);
        return finalCount;
    }

    /**
     * Manually overrides the tracked age for a specific hamster or group of hamsters.
     *
     * @param source  The command source invoking this command.
     * @param amount  The desired age amount.
     * @param unit    The unit of time ("days", "months", "years").
     * @param targets The collection of entities to apply this age to. If empty,
     *                the command will target the hamster being looked at, or the nearest.
     * @return The number of hamsters that were successfully updated.
     */
    public static int executeSetAge(CommandSourceStack source, double amount, String unit, Collection<? extends Entity> targets) throws CommandSyntaxException {
        // Calculate multipliers based on 24,000 ticks = 1 in-game day
        long multiplier = switch (unit.toLowerCase()) {
            case "months" -> 24000L * 30L;
            case "years" -> 24000L * 365L;
            default -> 24000L; // "days"
        };

        long newAgeTicks = (long) (amount * multiplier);
        int count = 0;

        if (targets.isEmpty()) {
            HamsterEntity target = getTargetHamster(source.getPlayerOrException());
            if (target == null) {
                source.sendSuccess(() -> Component.literal("No hamster found within 5 blocks. Look closer or specify a target.").withStyle(ChatFormatting.RED), false);
                return 0;
            }
            target.totalAgeTicks = newAgeTicks;
            count++;
        } else {
            for (Entity entity : targets) {
                if (entity instanceof HamsterEntity hamster) {
                    hamster.totalAgeTicks = newAgeTicks;
                    count++;
                }
            }
        }

        final int finalCount = count;
        source.sendSuccess(() -> Component.literal("Set age to " + amount + " " + unit + " (" + newAgeTicks + " ticks) for " + finalCount + " hamster(s).").withStyle(ChatFormatting.GREEN), true);
        return finalCount;
    }

    /**
     * Triggers the Genetics Engine to recalculate and print the current mathematical status.
     */
    public static int executeGeneticsReport(CommandSourceStack source, String outputType) {
        boolean toChat = outputType.equalsIgnoreCase("chat");
        HamsterPaletteManager.printGeneticsReport(source, toChat);

        if (!toChat) {
            source.sendSuccess(() -> Component.literal("Genetics Engine report printed to server console (check your 'latest.log' file)").withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static HamsterEntity getTargetHamster(ServerPlayer player) {
        List<HamsterEntity> nearby = player.level().getEntitiesOfClass(
                HamsterEntity.class,
                player.getBoundingBox().inflate(5.0),
                e -> true
        );

        HamsterEntity lookedAt = null;
        double minLookedAtDistSq = Double.MAX_VALUE;

        HamsterEntity nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (HamsterEntity h : nearby) {
            double distSq = h.distanceToSqr(player);

            // Prioritize hamsters player is actively looking at
            if (EntityTargetingUtil.isLookingAt(player, h, 10.0, 0.2)) {
                if (distSq < minLookedAtDistSq) {
                    minLookedAtDistSq = distSq;
                    lookedAt = h;
                }
            }

            // Track nearest overall as fallback
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                nearest = h;
            }
        }

        return lookedAt != null ? lookedAt : nearest;
    }
}