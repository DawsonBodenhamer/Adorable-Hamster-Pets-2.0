package net.dawson.adorablehamsterpets.screen.slot;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class HamsterSlot extends Slot {

    public HamsterSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    /**
     * Checks if the given ItemStack can be inserted into this slot.
     * Uses the HamsterEntity's disallowed item logic.
     * @param stack The ItemStack to check.
     * @return True if the item is allowed, false otherwise.
     */
    @Override
    public boolean canInsert(ItemStack stack) {
        // --- Description: Check if the item is allowed using the entity's logic ---
        // We need access to the HamsterEntity instance to call the non-static helper.
        // The inventory field of the Slot class holds the inventory it's linked to.
        if (this.inventory instanceof HamsterEntity hamsterEntity) {
            // Call the instance method on the specific hamster entity
            return !hamsterEntity.isItemDisallowed(stack);
        }
        // Fallback: If for some reason the inventory isn't a HamsterEntity (e.g., client-side issue),
        // disallow insertion to be safe. This shouldn't normally happen with the current setup.
        return false;
        // --- End Description ---
    }
}