package net.dawson.adorablehamsterpets.screen;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.item.custom.HamsterArmorItem;
import net.dawson.adorablehamsterpets.screen.slot.HamsterSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the inventory screen for a Hamster entity.
 * This screen handler synchronizes the hamster's 8-slot inventory (6 Pouch + Bling + Armor)
 * with the client and handles item transfers between the hamster and the player.
 */
public class HamsterInventoryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    @Nullable
    private final HamsterEntity hamsterEntityInstance;

    private static final int INVENTORY_SIZE = 8;
    private static final int BLING_SLOT_INDEX = 6;
    private static final int ARMOR_SLOT_INDEX = 7;


    /**
     * This single constructor is used by both the server
     * and the client. On the client, the hamster entity is provided by Architectury's
     * extended menu factory system.
     */
    public HamsterInventoryScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable HamsterEntity hamsterEntity) {
        super(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), syncId);

        if (hamsterEntity != null) {
            this.inventory = hamsterEntity;
            this.hamsterEntityInstance = hamsterEntity;
            checkSize(this.inventory, INVENTORY_SIZE);
        } else {
            // Fallback for client if entity is somehow not found
            // Must match server size (8) to prevent crash
            this.inventory = new SimpleInventory(INVENTORY_SIZE);
            this.hamsterEntityInstance = null;
        }

        this.inventory.onOpen(playerInventory.player);
        setupSlots(playerInventory);
    }

    /**
     * Returns the HamsterEntity instance associated with this screen handler.
     * This is used by the client-side screen to know which entity to render.
     *
     * @return The hamster entity instance, or null if not available.
     */
    @Nullable
    public HamsterEntity getHamsterEntity() {
        return this.hamsterEntityInstance;
    }

    /**
     * Sets up the slots for the hamster's inventory and the player's inventory.
     * @param playerInventory The player's inventory.
     */
    private void setupSlots(PlayerInventory playerInventory) {
        // --- 1. Cheek Pouch Slots (0-5) ---
        // Row 1: Left Cheeks
        this.addSlot(new HamsterSlot(this.inventory, 0, 26, 95));
        this.addSlot(new HamsterSlot(this.inventory, 1, 44, 95));
        this.addSlot(new HamsterSlot(this.inventory, 2, 62, 95));

        // Visual Gap Slot (Dummy)
        this.addSlot(new Slot(new SimpleInventory(1), 0, 80, 95) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity playerEntity) { return false; }
            @Override public boolean isEnabled() { return false; }
        });

        // Row 1: Right Cheek
        this.addSlot(new HamsterSlot(this.inventory, 3, 98, 95));
        this.addSlot(new HamsterSlot(this.inventory, 4, 116, 95));
        this.addSlot(new HamsterSlot(this.inventory, 5, 134, 95));

        // --- 2. Equipment Slots (6-7) ---
        // Bling Slot (Index 6)
        this.addSlot(new Slot(this.inventory, BLING_SLOT_INDEX, 82, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.ACORN_HAT.get()) || stack.isOf(Items.PINK_PETALS);
            }
            @Override
            public int getMaxItemCount() { return 1; }
        });

        // Armor Slot (Index 7)
        this.addSlot(new Slot(this.inventory, ARMOR_SLOT_INDEX, 134, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof HamsterArmorItem;
            }
            @Override
            public int getMaxItemCount() { return 1; }
        });

        // --- 3. Player Inventory & Hotbar ---
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack sourceStack = slot.getStack();
            itemStack = sourceStack.copy();

            // Calculate slot ranges
            // 6 pouch slots + 1 gap slot + 2 equipment slots = 9 slots total in "Hamster Area"
            // Indices:
            // 0: Pouch 0
            // 1: Pouch 1
            // 2: Pouch 2
            // 3: GAP (Visual)
            // 4: Pouch 3
            // 5: Pouch 4
            // 6: Pouch 5
            // 7: Bling (Inv Index 6)
            // 8: Armor (Inv Index 7)

            int totalHamsterSlots = 9; // 0 to 8
            int playerStart = totalHamsterSlots;
            int playerEnd = playerStart + 36;

            // --- Case 1: Transfer FROM Hamster TO Player ---
            if (slotIndex < totalHamsterSlots) {
                if (slotIndex == 3) return ItemStack.EMPTY; // Cannot move the gap slot

                // --- Sound Logic ---
                // Detect if moving from an equipment slot before the move happens
                boolean isFromHamster = slot.inventory == this.inventory;
                boolean isArmorSlot = isFromHamster && slot.getIndex() == ARMOR_SLOT_INDEX;
                boolean isBlingSlot = isFromHamster && slot.getIndex() == BLING_SLOT_INDEX;

                if (!this.insertItem(sourceStack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }

                // If the move was successful, manually trigger the unequip sound.
                if ((isArmorSlot || isBlingSlot) && this.hamsterEntityInstance != null && !this.hamsterEntityInstance.getWorld().isClient) {
                    // 1.20.1: Use Generic equip sound for both (Wolf armor sound didn't exist here)
                    SoundEvent soundEvent = SoundEvents.ITEM_ARMOR_EQUIP_GENERIC;
                    // Play with unequip pitch (0.8f) and lower volume (0.4f)
                    this.hamsterEntityInstance.playSound(soundEvent, 0.4f, 0.8f);
                }
            }
            // --- Case 2: Transfer FROM Player TO Hamster ---
            else {
                // Priority 1: Armor Slot
                if (sourceStack.getItem() instanceof HamsterArmorItem) {
                    if (!this.insertItem(sourceStack, 8, 9, false)) {
                        if (!insertIntoPouches(sourceStack)) return ItemStack.EMPTY;
                    }
                }
                // Priority 2: Bling Slot
                else if (sourceStack.isOf(ModItems.ACORN_HAT.get()) || sourceStack.isOf(Items.PINK_PETALS)) {
                    if (!this.insertItem(sourceStack, 7, 8, false)) {
                        if (!insertIntoPouches(sourceStack)) return ItemStack.EMPTY;
                    }
                }
                // Priority 3: Cheek Pouches
                else {
                    if (this.hamsterEntityInstance != null && this.hamsterEntityInstance.isItemDisallowed(sourceStack)) {
                        return ItemStack.EMPTY;
                    }
                    if (!insertIntoPouches(sourceStack)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (sourceStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (sourceStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, sourceStack);
        }

        return itemStack;
    }

    /**
     * Helper to insert items into the split cheek pouch slots (skipping the visual gap).
     */
    private boolean insertIntoPouches(ItemStack stack) {
        // Try Left Pouch (Slots 0-2)
        if (this.insertItem(stack, 0, 3, false)) return true;
        // Try Right Pouch (Slots 4-6) - Note: index 3 is gap
        return this.insertItem(stack, 4, 7, false);
    }
}