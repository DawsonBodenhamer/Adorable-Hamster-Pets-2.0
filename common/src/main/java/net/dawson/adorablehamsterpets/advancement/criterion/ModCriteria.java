package net.dawson.adorablehamsterpets.advancement.criterion;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.util.Identifier;

public class ModCriteria {

    // --- Define Criterion Instances ---
    public static final HamsterOnShoulderCriterion HAMSTER_ON_SHOULDER = new HamsterOnShoulderCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_on_shoulder"));
    public static final HamsterThrownCriterion HAMSTER_THROWN = new HamsterThrownCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_thrown"));
    public static final FedHamsterSteamedBeansCriterion FED_HAMSTER_STEAMED_BEANS = new FedHamsterSteamedBeansCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "fed_hamster_steamed_beans"));
    public static final CheekPouchUnlockedCriterion CHEEK_POUCH_UNLOCKED = new CheekPouchUnlockedCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "cheek_pouch_unlocked"));
    public static final AppliedFlowerCriterion APPLIED_FLOWER = new AppliedFlowerCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "applied_flower"));
    public static final HamsterAutoFedCriterion HAMSTER_AUTO_FED = new HamsterAutoFedCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_auto_fed"));
    public static final HamsterDiamondAlertCriterion HAMSTER_DIAMOND_ALERT_TRIGGERED = new HamsterDiamondAlertCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_diamond_alert_triggered"));
    public static final HamsterCreeperAlertCriterion HAMSTER_CREEPER_ALERT_TRIGGERED = new HamsterCreeperAlertCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_creeper_alert_triggered"));
    public static final HamsterPouchFilledCriterion HAMSTER_POUCH_FILLED = new HamsterPouchFilledCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_pouch_filled"));
    public static final HamsterLedToDiamondCriterion HAMSTER_LED_TO_DIAMOND = new HamsterLedToDiamondCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_led_to_diamond"));
    public static final HamsterFoundGoldCriterion HAMSTER_FOUND_GOLD = new HamsterFoundGoldCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_found_gold"));
    public static final HamsterBedLinkedCriterion HAMSTER_BED_LINKED = new HamsterBedLinkedCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_bed_linked"));
    public static final HamsterSleptInBedCriterion HAMSTER_SLEPT_IN_BED = new HamsterSleptInBedCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_slept_in_bed"));
    public static final UsedHamsterBeddingCriterion USED_HAMSTER_BEDDING = new UsedHamsterBeddingCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "used_hamster_bedding"));
    public static final HamsterBedPlacedUpsideDownCriterion HAMSTER_BED_PLACED_UPSIDE_DOWN = new HamsterBedPlacedUpsideDownCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_bed_placed_upside_down"));
    public static final DispensedHamsterBeddingCriterion DISPENSED_HAMSTER_BEDDING = new DispensedHamsterBeddingCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "dispensed_hamster_bedding"));
    public static final TreeHeistDepletionCriterion TREE_HEIST_DEPLETION = new TreeHeistDepletionCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "tree_heist_depletion"));
    public static final TreeHeistStartedCriterion TREE_HEIST_STARTED = new TreeHeistStartedCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "tree_heist_started"));
    public static final HideAndSeekFoundCriterion HIDE_AND_SEEK_FOUND = new HideAndSeekFoundCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hide_and_seek_found"));
    public static final WitnessGlowingSunflowerCriterion WITNESS_GLOWING_SUNFLOWER = new WitnessGlowingSunflowerCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "witness_glowing_sunflower"));
    public static final MaxShoulderHamstersCriterion MAX_SHOULDER_HAMSTERS = new MaxShoulderHamstersCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "max_shoulder_hamsters"));

    /**
     * Registers a criterion with Minecraft's advancement system.
     * @param criterion The criterion instance to register.
     * @return The registered criterion instance.
     */
    private static <T extends Criterion<?>> T register(T criterion) {
        return Criteria.register(criterion);
    }

    /**
     * Main registration call. This method ensures all custom criteria are registered.
     */
    public static void register() {
        register(HAMSTER_ON_SHOULDER);
        register(HAMSTER_THROWN);
        register(FED_HAMSTER_STEAMED_BEANS);
        register(CHEEK_POUCH_UNLOCKED);
        register(APPLIED_FLOWER);
        register(HAMSTER_AUTO_FED);
        register(HAMSTER_DIAMOND_ALERT_TRIGGERED);
        register(HAMSTER_CREEPER_ALERT_TRIGGERED);
        register(HAMSTER_POUCH_FILLED);
        register(HAMSTER_LED_TO_DIAMOND);
        register(HAMSTER_FOUND_GOLD);
        register(HAMSTER_BED_LINKED);
        register(HAMSTER_SLEPT_IN_BED);
        register(USED_HAMSTER_BEDDING);
        register(HAMSTER_BED_PLACED_UPSIDE_DOWN);
        register(DISPENSED_HAMSTER_BEDDING);
        register(TREE_HEIST_DEPLETION);
        register(TREE_HEIST_STARTED);
        register(HIDE_AND_SEEK_FOUND);
        register(WITNESS_GLOWING_SUNFLOWER);
        register(MAX_SHOULDER_HAMSTERS);

        AdorableHamsterPets.LOGGER.info("Registering Mod Criteria for " + AdorableHamsterPets.MOD_ID);
    }
}
