package net.dawson.adorablehamsterpets.screen;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.screen.slot.HamsterSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the inventory screen for a Hamster entity.
 * This screen handler synchronizes the hamster's 6-slot inventory with the client
 * and handles item transfers between the hamster and the player.
 */
public class HamsterInventoryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    @Nullable
    private final HamsterEntity hamsterEntityInstance;

    /**
     * Constructs the screen handler. This single constructor is used by both the server
     * and the client. On the client, the hamster entity is provided by Architectury's
     * extended menu factory system.
     *
     * @param syncId The synchronization ID for the screen handler.
     * @param playerInventory The player's inventory.
     * @param hamsterEntity The hamster entity whose inventory is being opened. Can be null on the client if the entity isn't found.
     */
    public HamsterInventoryScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable HamsterEntity hamsterEntity) {
        super(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), syncId);

        if (hamsterEntity != null) {
            this.inventory = hamsterEntity;
            this.hamsterEntityInstance = hamsterEntity;
            checkSize(this.inventory, 6);
        } else {
            // Fallback for client if entity is somehow not found
            AdorableHamsterPets.LOGGER.warn("Could not find HamsterEntity on client, using empty inventory.");
            this.inventory = new SimpleInventory(6);
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
        // --- Hamster Cheek Pouch Slots ---
        this.addSlot(new HamsterSlot(this.inventory, 0, 26, 95));
        this.addSlot(new HamsterSlot(this.inventory, 1, 44, 95));
        this.addSlot(new HamsterSlot(this.inventory, 2, 62, 95));
        this.addSlot(new Slot(new SimpleInventory(1), 0, 80, 95) { // Visual Gap Slot
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity playerEntity) { return false; }
            @Override public boolean isEnabled() { return false; }
        });
        this.addSlot(new HamsterSlot(this.inventory, 3, 98, 95));
        this.addSlot(new HamsterSlot(this.inventory, 4, 116, 95));
        this.addSlot(new HamsterSlot(this.inventory, 5, 134, 95));

        // --- Player Inventory & Hotbar ---
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
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        super.onSlotClick(slotIndex, button, actionType, player);
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

            int hamsterInvSize = 6;
            int gapSlotIndex = 3;
            int totalHamsterAreaSlots = hamsterInvSize + 1; // Includes the gap slot

            // --- Case 1: Moving FROM Hamster Inventory TO Player ---
            if (slotIndex < totalHamsterAreaSlots && slotIndex != gapSlotIndex) {
                if (!this.insertItem(sourceStack, totalHamsterAreaSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // --- Case 2: Moving FROM Player Inventory TO Hamster ---
            else if (slotIndex >= totalHamsterAreaSlots) {
                if (this.hamsterEntityInstance != null && this.hamsterEntityInstance.isItemDisallowed(sourceStack)) {
                    return ItemStack.EMPTY;
                }
                // Try to insert into the hamster's inventory, skipping the gap
                if (!this.insertItem(sourceStack, 0, gapSlotIndex, false) &&
                        !this.insertItem(sourceStack, gapSlotIndex + 1, totalHamsterAreaSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY; // Clicked the gap slot
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

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}