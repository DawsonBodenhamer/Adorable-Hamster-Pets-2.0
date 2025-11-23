package net.dawson.adorablehamsterpets.entity;

import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface ImplementedInventory extends Inventory {
    /**
     * Retrieves the item list of this inventory.
     * Must return the same instance every time it's called.
     */
    DefaultedList<ItemStack> getItems();

    /**
     * Creates and returns a new DefaultedList of the correct size
     * for this inventory.
     */
    static DefaultedList<ItemStack> create(int size) {
        return DefaultedList.ofSize(size, ItemStack.EMPTY);
    }

    /**
     * Gets the inventory size.
     * Defaults to the item list size.
     * @see Inventory#size()
     */
    @Override
    default int size() {
        return getItems().size();
    }

    /**
     * Checks if the inventory is empty.
     * Defaults to checking if every stack in the item list is empty.
     * @see Inventory#isEmpty()
     */
    @Override
    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the stack currently at the given slot.
     * Defaults to simply getting it from the item list.
     * @see Inventory#getStack(int)
     */
    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    /**
     * Removes a stack from the given slot and returns it.
     * This implementation uses Inventories.splitStack to correctly handle the amount.
     * @see Inventory#removeStack(int, int)
     * @param slot  The slot to remove from.
     * @param amount  The amount to remove.
     * @return The removed stack.
     */
    @Override
    default ItemStack removeStack(int slot, int amount) {
        // Use helper method that respects the 'amount' parameter.
        ItemStack result = Inventories.splitStack(getItems(), slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    /**
     * Removes the stack currently at the given slot and returns it.
     * Defaults to {@link DefaultedList removeStack(int)} and marks the inventory as dirty.
     * @see Inventory#removeStack(int)
     * @param slot  The slot to remove from.
     * @return The removed stack.
     */
    @Override
    default ItemStack removeStack(int slot) {
        markDirty();
        return Inventories.removeStack(getItems(), slot);
    }

    /**
     * Replaces the stack in the given slot with the provided stack.
     * Defaults to {@link DefaultedList#set(int, Object)} and marks the inventory as dirty.
     * @see Inventory#setStack(int, ItemStack)
     * @param slot  The slot to set in.
     * @param stack  The stack to set to.
     */
    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        markDirty();
    }

    /**
     * Clears all stacks in the inventory.
     * Defaults to {@link DefaultedList#clear()} and marks the inventory as dirty.
     * @see Inventory#clear()
     */
    @Override
    default void clear() {
        getItems().clear();
        markDirty();
    }

    /**
     * Marks the state as dirty.
     * Must be called after doing anything that modifies the inventory.
     * <p>
     * For implementors, this method is usually implemented simply by calling {@link net.minecraft.block.entity.BlockEntity#markDirty()}.
     * @see Inventory#markDirty()
     */
    @Override
    default void markDirty() {
        // Client-side inventories do not need to be saved.
    }

    /**
     * Gets the maximum stack size for items in this Inventory.
     * Defaults to 64
     * @see Inventory#getMaxCountPerStack()
     */
    @Override
    default int getMaxCountPerStack() {
        return 64;
    }

    /**
     * Called when a user starts using the inventory.
     * No default implementation.
     * @see Inventory#onOpen(net.minecraft.entity.player.PlayerEntity)
     */
    @Override
    default void onOpen(net.minecraft.entity.player.PlayerEntity player) {
    }

    /**
     * Called when a user stops using the inventory.
     * No default implementation.
     * @see Inventory#onClose(net.minecraft.entity.player.PlayerEntity)
     */
    @Override
    default void onClose(net.minecraft.entity.player.PlayerEntity player) {
    }

    /**
     * Check if the given player can use this inventory.
     * Defaults to always return true because BlockInventories are generally available to be used by anyone.
     * @see Inventory#canPlayerUse(net.minecraft.entity.player.PlayerEntity)
     */
    @Override
    default boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) {
        return true;
    }

    /**
     * Returns {@code true} if automation (specifically hopper) can insert to the bottom of the inventory.
     * @return {@code true} if automation can insert to the bottom of the inventory, {@code false} otherwise.
     * @see Inventory#isValid(int, ItemStack)
     */
    @Override
    default boolean isValid(int slot, ItemStack stack) {
        return true;
    }

    /**
     * Returns the property delegate of this inventory.
     * @return the property delegate of this inventory.
     * @see Inventory getPropertyDelegate()
     */

    default net.minecraft.screen.PropertyDelegate getPropertyDelegate() {
        return new net.minecraft.screen.ArrayPropertyDelegate(0);
    }
}