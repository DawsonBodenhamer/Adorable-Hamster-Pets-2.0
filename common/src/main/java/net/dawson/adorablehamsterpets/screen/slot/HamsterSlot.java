package net.dawson.adorablehamsterpets.screen.slot;

import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class HamsterSlot extends Slot {

    public HamsterSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    /**
     * Checks if the given ItemStack can be inserted into this slot.
     * Uses centralized inventory utility logic.
     * @param stack The ItemStack to check.
     * @return True if the item is allowed, false otherwise.
     */
    @Override
    public boolean canInsert(ItemStack stack) {
        return HamsterInventoryUtil.isValidForSlot(this.getIndex(), stack);
    }
}