package net.dawson.adorablehamsterpets.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

public class PlayerCommandUtil {

    /**
     * Grants all Adorable Hamster Pets advancements to the commanding player.
     */
    public static int executeUnlockAllModAdvancements(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAdvancementTracker tracker = player.getAdvancementTracker();
        Collection<Advancement> allAdvancements = source.getServer().getAdvancementLoader().getAdvancements();
        int count = 0;

        for (Advancement advancement : allAdvancements) {
            Identifier id = advancement.getId();
            if (id.getNamespace().equals(AdorableHamsterPets.MOD_ID) &&
                    (id.getPath().startsWith("husbandry/"))) {

                AdvancementProgress progress = tracker.getProgress(advancement);
                if (!progress.isDone()) {
                    for (String criterion : advancement.getCriteria().keySet()) {
                        tracker.grantCriterion(advancement, criterion);
                    }
                    count++;
                }
            }
        }

        final int finalCount = count;
        if (finalCount > 0) {
            source.sendFeedback(() -> Text.literal("Unlocked " + finalCount + " Adorable Hamster Pets advancements."), true);
        } else {
            source.sendFeedback(() -> Text.literal("No new Adorable Hamster Pets advancements to unlock or all already unlocked."), true);
        }
        return finalCount;
    }

    /**
     * Executes visual effects and sound intended for discovering the guidebook.
     */
    public static int executeTriggerBookEffects(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ModPackets.CHANNEL.sendToPlayer(player, new ModPackets.PlayGuidebookEffectsS2CPacket(false));
        return 1;
    }

    /**
     * Spawns a new copy of the Hamster Tips guide book into the player's inventory.
     */
    public static int executeGiveGuidebook(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        // Deliver guidebook: no advancement flag, no fallback message, play effect, do not close GUI.
        AdorableHamsterPets.deliverGuidebook(player, false, false, true, false);

        // Update tracking cache so the warning doesn't trigger
        ((PlayerEntityAccessor) player).ahp$initGuideBookTracking(true);
        return 1;
    }

    /**
     * Wipes the player's local NBT memory regarding which trees they've exhausted.
     */
    public static int executeResetHeistHistory(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ((PlayerEntityAccessor) player).ahp$clearHeistHistory();
        return 1;
    }

    /**
     * Clears the player's daily or lifetime limit on hamster breeding.
     */
    public static int executeResetPlayerBreedingHistory(ServerCommandSource source, Collection<ServerPlayerEntity> targets) throws CommandSyntaxException {
        for (ServerPlayerEntity target : targets) {
            ((PlayerEntityAccessor) target).ahp$resetBreedingHistory();
        }
        return targets.size();
    }

    /**
     * Resets the breeding history for specific hamsters.
     * If no targets are provided, targets the nearest hamster within 5 blocks.
     */
    public static int executeResetHamsterBreedingHistory(ServerCommandSource source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int count = 0;

        if (targets.isEmpty()) {
            HamsterEntity nearest = getNearestHamster(source.getPlayerOrThrow());
            if (nearest == null) {
                source.sendFeedback(() -> Text.literal("No hamster found within 5 blocks. Look closer or specify a target.").formatted(Formatting.RED), false);
                return 0;
            }
            nearest.timesBred = 0;
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
        source.sendFeedback(() -> Text.literal("Reset times bred to 0 for " + finalCount + " hamster(s).").formatted(Formatting.GREEN), true);
        return finalCount;
    }

    /**
     * Manually overrides the tracked age for a specific hamster or group of hamsters.
     *
     * @param source  The command source invoking this command.
     * @param amount  The desired age amount.
     * @param unit    The unit of time ("days", "months", "years").
     * @param targets The collection of entities to apply this age to. If empty, the command
     *                will target the absolute nearest hamster within a 5-block radius.
     * @return The number of hamsters that were successfully updated.
     */
    public static int executeSetAge(ServerCommandSource source, double amount, String unit, Collection<? extends Entity> targets) throws CommandSyntaxException {
        // Calculate multipliers based on 24,000 ticks = 1 in-game day
        long multiplier = switch (unit.toLowerCase()) {
            case "months" -> 24000L * 30L;
            case "years" -> 24000L * 365L;
            default -> 24000L; // "days"
        };

        long newAgeTicks = (long) (amount * multiplier);
        int count = 0;

        if (targets.isEmpty()) {
            HamsterEntity nearest = getNearestHamster(source.getPlayerOrThrow());
            if (nearest == null) {
                source.sendFeedback(() -> Text.literal("No hamster found within 5 blocks. Look closer or specify a target.").formatted(Formatting.RED), false);
                return 0;
            }
            nearest.totalAgeTicks = newAgeTicks;
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
        source.sendFeedback(() -> Text.literal("Set age to " + amount + " " + unit + " (" + newAgeTicks + " ticks) for " + finalCount + " hamster(s).").formatted(Formatting.GREEN), true);
        return finalCount;
    }

    /**
     * Triggers the Genetics Engine to recalculate and print the current mathematical status.
     */
    public static int executeGeneticsReport(ServerCommandSource source, String outputType) {
        boolean toChat = outputType.equalsIgnoreCase("chat");
        HamsterPaletteManager.printGeneticsReport(source, toChat);

        if (!toChat) {
            source.sendFeedback(() -> Text.literal("Genetics Engine report printed to server console (check your 'latest.log' file)").formatted(Formatting.GREEN), false);
        }
        return 1;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static HamsterEntity getNearestHamster(ServerPlayerEntity player) {
        List<HamsterEntity> nearby = player.getWorld().getEntitiesByClass(
                HamsterEntity.class,
                player.getBoundingBox().expand(5.0),
                e -> true
        );

        HamsterEntity nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (HamsterEntity h : nearby) {
            double distSq = h.squaredDistanceTo(player);
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                nearest = h;
            }
        }

        return nearest;
    }
}