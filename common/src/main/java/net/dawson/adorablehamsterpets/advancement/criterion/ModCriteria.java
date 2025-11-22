package net.dawson.adorablehamsterpets.advancement.criterion;

/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.util.Identifier;

public class ModCriteria {

    // --- Define Criterion Instances ---
    public static final HamsterOnShoulderCriterion HAMSTER_ON_SHOULDER = new HamsterOnShoulderCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_on_shoulder"));
    public static final HamsterThrownCriterion HAMSTER_THROWN = new HamsterThrownCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_thrown"));
    public static final FirstJoinCriterion FIRST_JOIN_GUIDEBOOK_CHECK = new FirstJoinCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "first_join_guidebook_check"));
    public static final FedHamsterSteamedBeansCriterion FED_HAMSTER_STEAMED_BEANS = new FedHamsterSteamedBeansCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "fed_hamster_steamed_beans"));
    public static final CheekPouchUnlockedCriterion CHEEK_POUCH_UNLOCKED = new CheekPouchUnlockedCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "cheek_pouch_unlocked"));
    public static final AppliedPinkPetalCriterion APPLIED_PINK_PETAL = new AppliedPinkPetalCriterion(Identifier.of(AdorableHamsterPets.MOD_ID, "applied_pink_petal"));
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
        register(FIRST_JOIN_GUIDEBOOK_CHECK);
        register(FED_HAMSTER_STEAMED_BEANS);
        register(CHEEK_POUCH_UNLOCKED);
        register(APPLIED_PINK_PETAL);
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

        AdorableHamsterPets.LOGGER.info("Registering Mod Criteria for " + AdorableHamsterPets.MOD_ID);
    }
}