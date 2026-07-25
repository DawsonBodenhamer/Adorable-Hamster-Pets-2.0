package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.function.BiConsumer;

/**
 * Manages inventory validation, equipment synchronization, cheek pouch state, and wild loot
 * generation for Hamsters.
 */
public final class HamsterInventoryUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final int INVENTORY_SIZE = 8;
    public static final int CHEEK_POUCH_SIZE = 6;
    public static final int ACCESSORY_SLOT_INDEX = 6;
    public static final int ARMOR_SLOT_INDEX = 7;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Slot Validation ---

    /**
     * Verifies if an item can be placed in a specific inventory slot.
     */
    public static boolean isValidForSlot(int slot, ItemStack stack) {
        // --- 1. Cheek Pouches ---
        if (slot < CHEEK_POUCH_SIZE) {
            return canInsertIntoPouch(stack);
        }

        // --- 2. Accessory Slot ---
        if (slot == ACCESSORY_SLOT_INDEX) {
            return stack.isOf(ModItems.ACORN_HAT.get()) || stack.isIn(ItemTags.FLOWERS);
        }

        // --- 3. Armor Slot ---
        if (slot == ARMOR_SLOT_INDEX) {
            return stack.getItem() instanceof HamsterArmorItem;
        }
        return false;
    }

    /**
     * Determines if an item is allowed inside the cheek pouches based on config rules.
     */
    public static boolean canInsertIntoPouch(ItemStack stack) {
        if (stack.isEmpty()) return true;

        // --- 1. Explicit Allow List ---
        if (ConfigDataCache.isPouchAllowed(stack)) return true;

        // --- 2. Configured Exclusions ---
        if (ConfigDataCache.isPouchDisallowed(stack)) return false;

        // --- 3. Mod Food Classifications ---
        if (ConfigDataCache.isStandardFood(stack)
                || ConfigDataCache.isTamingFood(stack)
                || ConfigDataCache.isBuffFood(stack)
                || ConfigDataCache.isPouchUnlockFood(stack)
                || ConfigDataCache.isAutoHealFood(stack)
                || ConfigDataCache.isSnackableItem(stack)) {
            return true;
        }

        // --- 4. Vanilla Food Status ---
        // 1.20.1 exposes food status directly on ItemStack
        if (stack.isFood()) {
            return true;
        }

        Item item = stack.getItem();

        // --- 5. Global Block-Item Exclusion ---
        if (item instanceof BlockItem) return false;

        // --- 6. Spawn Egg Exclusion ---
        return !(item instanceof SpawnEggItem);
    }

    // --- Cheek Pouch Operations ---

    /**
     * Checks if the hamster has at least one cheek pouch slot available that can accept the item.
     */
    public static boolean hasRoomInCheeks(HamsterEntity hamster, ItemStack stack) {
        for (int i = 0; i < CHEEK_POUCH_SIZE; i++) {
            ItemStack slotStack = hamster.getItems().get(i);
            if (slotStack.isEmpty()
                    || (ItemStack.areItemsEqual(slotStack, stack)
                            && slotStack.getCount() < slotStack.getMaxCount())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to forcefully insert an item into the hamster's cheek pouches. Returns the remaining
     * stack if it couldn't all fit.
     */
    public static ItemStack insertIntoCheeks(HamsterEntity hamster, ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int i = 0; i < CHEEK_POUCH_SIZE; i++) {
            if (remaining.isEmpty()) break;

            ItemStack slotStack = hamster.getItems().get(i);

            if (slotStack.isEmpty()) {
                hamster.setStack(i, remaining.split(remaining.getCount()));
            } else if (ItemStack.areItemsEqual(slotStack, remaining)
                    && slotStack.getCount() < slotStack.getMaxCount()) {
                int spaceLeft = slotStack.getMaxCount() - slotStack.getCount();
                int amountToMove = Math.min(spaceLeft, remaining.getCount());

                slotStack.increment(amountToMove);
                remaining.decrement(amountToMove);
                hamster.setStack(i, slotStack); // Trigger sync
            }
        }

        return remaining;
    }

    /**
     * Updates visual and logic states for cheek fullness based on inventory content.
     */
    public static void updateCheekStates(HamsterEntity hamster) {
        // --- Cheek Occupancy ---
        boolean leftFull = false;
        for (int i = 0; i < 3; i++) {
            if (!hamster.getItems().get(i).isEmpty()) {
                leftFull = true;
                break;
            }
        }

        boolean rightFull = false;
        for (int i = 3; i < CHEEK_POUCH_SIZE; i++) {
            if (!hamster.getItems().get(i).isEmpty()) {
                rightFull = true;
                break;
            }
        }

        if (hamster.isLeftCheekFull() != leftFull) hamster.setLeftCheekFull(leftFull);
        if (hamster.isRightCheekFull() != rightFull) hamster.setRightCheekFull(rightFull);

        // --- Full-Pouch Advancement ---
        if (!hamster.getWorld().isClient()
                && hamster.getOwner() instanceof ServerPlayerEntity serverPlayer) {
            boolean allSlotsFilled = true;
            for (int i = 0; i < CHEEK_POUCH_SIZE; i++) {
                if (hamster.getItems().get(i).isEmpty()) {
                    allSlotsFilled = false;
                    break;
                }
            }
            if (allSlotsFilled) {
                ModCriteria.HAMSTER_POUCH_FILLED.trigger(serverPlayer, hamster);
            }
        }
    }

    /**
     * Synchronizes inventory-derived visual state after an inventory mutation.
     */
    public static void synchronizeVisualState(HamsterEntity hamster) {
        updateCheekStates(hamster);
        updateAccessoryState(hamster);
    }

    // --- Equipment Synchronization and Feedback ---

    /**
     * Synchronizes the flower position tracker with the accessory slot.
     */
    public static void updateAccessoryState(HamsterEntity hamster) {
        ItemStack accessory = hamster.getItems().get(ACCESSORY_SLOT_INDEX);
        int flowerPosition = hamster.getDataTracker().get(HamsterEntity.FLOWER_POS);

        if (accessory.isIn(ItemTags.FLOWERS)) {
            if (flowerPosition == 0) {
                hamster.getDataTracker().set(HamsterEntity.FLOWER_POS, 1);
            }
        } else if (flowerPosition != 0) {
            hamster.getDataTracker().set(HamsterEntity.FLOWER_POS, 0);
        }
    }

    /**
     * Syncs equipment visually onto the client DataTrackers.
     */
    public static void syncEquipmentTrackers(HamsterEntity hamster) {
        if (hamster.getWorld().isClient() && !hamster.isShoulderPet() && !hamster.isProjectileDummy)
            return;

        ItemStack accessory = hamster.getItems().get(ACCESSORY_SLOT_INDEX);
        ItemStack armor = hamster.getItems().get(ARMOR_SLOT_INDEX);

        hamster.setTrackedAccessoryStack(accessory);
        hamster.setTrackedArmorStack(armor);
    }

    /**
     * Plays appropriate equip/unequip sounds when equipment slots change.
     */
    public static void handleSlotUpdateSounds(
            HamsterEntity hamster, int slot, ItemStack oldStack, ItemStack newStack) {
        boolean isEmpty = newStack.isEmpty();
        boolean wasEmpty = oldStack.isEmpty();

        if (ItemStack.areEqual(oldStack, newStack)) return;

        if (slot == ARMOR_SLOT_INDEX) {
            if (wasEmpty && !isEmpty) {
                hamster.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            } else if (!wasEmpty && isEmpty) {
                hamster.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.4f, 0.8f);
            } else if (!wasEmpty && !isEmpty) {
                hamster.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            }
        } else if (slot == ACCESSORY_SLOT_INDEX) {
            if (wasEmpty && !isEmpty) {
                hamster.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            } else if (!wasEmpty && isEmpty) {
                hamster.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.4f, 0.8f);
            } else if (!wasEmpty && !isEmpty) {
                hamster.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
            }
        }
    }

    // --- Wild Loot Generation ---

    /**
     * Generates context-aware wild loot for newly spawned untamed hamsters.
     */
    public static void generateWildLoot(HamsterEntity hamster, Random random) {
        // --- 1. Global Eligibility ---
        if (hamster.isTamed() || !hamster.getItems().get(0).isEmpty()) return;

        float globalChance = Configs.AHP_WORLDGEN.globalCheekLootChance.get();
        if (random.nextFloat() > globalChance) return;

        boolean fillBothCheeks = random.nextFloat() < 0.4f;

        // --- 2. Cheek Fill Operation ---
        BiConsumer<Integer, Integer> fillCheek =
                (startSlot, mode) -> {
                    int count = 1 + random.nextInt(3);
                    int specificSlot = startSlot + random.nextInt(3);

                    Item item =
                            switch (mode) {
                                case 1 -> ConfigDataCache.getRandomCustomLootItem(random);
                                case 2 -> ConfigDataCache.getRandomCaveLootItem(random);
                                default -> ConfigDataCache.getRandomDefaultLootItem(random);
                            };

                    if (item != Items.AIR) {
                        ItemStack stack = new ItemStack(item, count);
                        if (canInsertIntoPouch(stack)) {
                            if (hamster.getItems().get(specificSlot).isEmpty()) {
                                // Set directly to bypass tick overhead during initialization
                                hamster.getItems().set(specificSlot, stack);
                            }
                        }
                    }
                };

        // --- 3. Cave Loot ---
        boolean isCaveHamster = false;
        World world = hamster.getWorld();
        if (!world.isClient()) {
            isCaveHamster = HamsterGeneticsUtil.isCaveEnvironment(world, hamster.getBlockPos());
        }

        boolean caveLootSelected = false;
        if (isCaveHamster) {
            float caveChance = Configs.AHP_WORLDGEN.caveCheekLootChance.get();
            if (random.nextFloat() < caveChance) {
                caveLootSelected = true;
                if (fillBothCheeks) {
                    fillCheek.accept(0, 2);
                    fillCheek.accept(3, 2);
                } else {
                    fillCheek.accept(random.nextBoolean() ? 0 : 3, 2);
                }
            }
        }

        if (!caveLootSelected) {
            // --- 4. Default Loot ---
            float defaultChance = Configs.AHP_WORLDGEN.defaultCheekLootChance.get();
            if (random.nextFloat() < defaultChance) {
                if (fillBothCheeks) {
                    fillCheek.accept(0, 0);
                    fillCheek.accept(3, 0);
                } else {
                    fillCheek.accept(random.nextBoolean() ? 0 : 3, 0);
                }
            }

            // --- 5. Custom Loot ---
            float customChance = Configs.AHP_WORLDGEN.extraCheekLootChance.get();
            if (!Configs.AHP_WORLDGEN.extraCheekLootList.isEmpty()
                    && random.nextFloat() < customChance) {
                if (fillBothCheeks) {
                    fillCheek.accept(0, 1);
                    fillCheek.accept(3, 1);
                } else {
                    fillCheek.accept(random.nextBoolean() ? 0 : 3, 1);
                }
            }
        }

        updateCheekStates(hamster);
    }

    // --- Inventory Rule Enforcement ---

    /**
     * Checks the inventory for disallowed items and safely drops them at the hamster's feet.
     */
    public static boolean enforceInventoryRules(HamsterEntity hamster) {
        boolean inventoryChanged = false;
        World world = hamster.getWorld();

        for (int i = 0; i < INVENTORY_SIZE; ++i) {
            ItemStack stack = hamster.getItems().get(i);
            if (!stack.isEmpty() && !isValidForSlot(i, stack)) {
                AdorableHamsterPets.LOGGER.warn(
                        "[HamsterTick {}] Ejecting invalid item {} from slot {}.",
                        hamster.getId(),
                        stack.getItem(),
                        i);

                ItemScatterer.spawn(
                        world, hamster.getX(), hamster.getY(), hamster.getZ(), stack.copy());
                hamster.getItems().set(i, ItemStack.EMPTY);

                inventoryChanged = true;
            }
        }

        return inventoryChanged;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterInventoryUtil() {}
}
