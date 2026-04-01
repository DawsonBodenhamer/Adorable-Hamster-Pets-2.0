package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Handles tracking and triggering advancements related to the Hamster Genetics system.
 */
public final class HamsterGeneticsAdvancementUtil {

    private HamsterGeneticsAdvancementUtil() {}

    /**
     * Records a newly tamed hamster's genome for the player and checks collector milestones.
     */
    public static void trackTamedHamster(ServerPlayerEntity player, HamsterEntity hamster) {
        int hash = hamster.getGenome().hashCode();

        // Try adding the hash. If it returns true, it's a new unique variant for this player.
        if (((PlayerEntityAccessor) player).ahp$addTamedGenome(hash)) {
            int count = ((PlayerEntityAccessor) player).ahp$getTamedGenomeCount();
            checkTamedMilestones(player, count);
        }
    }

    /**
     * Records a newly bred baby hamster's genome for the player and checks scientist milestones.
     */
    public static void trackBredHamster(ServerPlayerEntity player, HamsterEntity baby) {
        int hash = baby.getGenome().hashCode();

        // Check for The Mad Scientist milestones
        if (((PlayerEntityAccessor) player).ahp$addBredGenome(hash)) {
            int count = ((PlayerEntityAccessor) player).ahp$getBredGenomeCount();
            checkBredMilestones(player, count);
        }

        // Check for Recessive Red Eyes (only trigger if the config allows them to be seen)
        if (baby.getGenome().eyeGenotype() == 2 && Configs.AHP.enableRedEyes) {
            grantAdvancement(player, "husbandry/seeing_red");
        }
    }

    private static void checkTamedMilestones(ServerPlayerEntity player, int count) {
        if (count >= 10) grantAdvancement(player, "husbandry/collector_10");
        if (count >= 50) grantAdvancement(player, "husbandry/collector_50");
        if (count >= 100) grantAdvancement(player, "husbandry/collector_100");

        long maxWild = HamsterPaletteManager.calculateTotalWildVariants();

        // Dynamically skip the 1,000 milestone if the user's config makes it impossible to reach
        if (maxWild >= 1000 && count >= 1000) {
            grantAdvancement(player, "husbandry/collector_1000");
        }

        // Dynamic "Catch 'Em All" milestone
        if (count >= maxWild) {
            grantAdvancement(player, "husbandry/collector_max");
        }
    }

    private static void checkBredMilestones(ServerPlayerEntity player, int count) {
        if (count >= 1) grantAdvancement(player, "husbandry/breeder_1");
        if (count >= 100) grantAdvancement(player, "husbandry/breeder_100");
        if (count >= 500) grantAdvancement(player, "husbandry/breeder_500");
        if (count >= 1000) grantAdvancement(player, "husbandry/breeder_1000");
        if (count >= 1000000) grantAdvancement(player, "husbandry/breeder_1000000");
    }

    /**
     * Explicitly grants a specific advancement to the player by its path.
     */
    private static void grantAdvancement(ServerPlayerEntity player, String path) {
        Identifier id = Identifier.of(AdorableHamsterPets.MOD_ID, path);
        Advancement advancement = player.server.getAdvancementLoader().get(id);

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : advancement.getCriteria().keySet()) {
                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }
        }
    }
}