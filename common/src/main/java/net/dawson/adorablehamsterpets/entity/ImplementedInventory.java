package net.dawson.adorablehamsterpets.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public interface ImplementedInventory extends Container {
    /**
     * Retrieves the item list of this inventory.
     * Must return the same instance every time it's called.
     */
    NonNullList<ItemStack> getItems();

    /**
     * Creates and returns a new DefaultedList of the correct size
     * for this inventory.
     */
    static NonNullList<ItemStack> create(int size) {
        return NonNullList.withSize(size, ItemStack.EMPTY);
    }

    /**
     * Gets the inventory size.
     * Defaults to the item list size.
     * @see Container#getContainerSize()
     */
    @Override
    default int getContainerSize() {
        return getItems().size();
    }

    /**
     * Checks if the inventory is empty.
     * Defaults to checking if every stack in the item list is empty.
     * @see Container#isEmpty()
     */
    @Override
    default boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the stack currently at the given slot.
     * Defaults to simply getting it from the item list.
     * @see Container#getItem(int)
     */
    @Override
    default ItemStack getItem(int slot) {
        return getItems().get(slot);
    }

    /**
     * Removes a stack from the given slot and returns it.
     * This implementation uses Inventories.splitStack to correctly handle the amount.
     * @see Container#removeItem(int, int)
     * @param slot  The slot to remove from.
     * @param amount  The amount to remove.
     * @return The removed stack.
     */
    @Override
    default ItemStack removeItem(int slot, int amount) {
        // Use helper method that respects the 'amount' parameter.
        ItemStack result = ContainerHelper.removeItem(getItems(), slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    /**
     * Removes the stack currently at the given slot and returns it.
     * Defaults to {@link NonNullList removeStack(int)} and marks the inventory as dirty.
     * @see Container#removeItemNoUpdate(int)
     * @param slot  The slot to remove from.
     * @return The removed stack.
     */
    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        setChanged();
        return ContainerHelper.takeItem(getItems(), slot);
    }

    /**
     * Replaces the stack in the given slot with the provided stack.
     * Defaults to {@link NonNullList#set(int, Object)} and marks the inventory as dirty.
     * @see Container#setItem(int, ItemStack)
     * @param slot  The slot to set in.
     * @param stack  The stack to set to.
     */
    @Override
    default void setItem(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        setChanged();
    }

    /**
     * Clears all stacks in the inventory.
     * Defaults to {@link NonNullList#clear()} and marks the inventory as dirty.
     * @see Container#clearContent()
     */
    @Override
    default void clearContent() {
        getItems().clear();
        setChanged();
    }

    /**
     * Marks the state as dirty.
     * Must be called after doing anything that modifies the inventory.
     * <p>
     * For implementors, this method is usually implemented simply by calling {@link net.minecraft.world.level.block.entity.BlockEntity#setChanged()}.
     * @see Container#setChanged()
     */
    @Override
    default void setChanged() {
        // Client-side inventories do not need to be saved.
    }

    /**
     * Gets the maximum stack size for items in this Inventory.
     * Defaults to 64
     * @see Container#getMaxStackSize()
     */
    @Override
    default int getMaxStackSize() {
        return 64;
    }

    /**
     * Called when a user starts using the inventory.
     * No default implementation.
     * @see Container#startOpen(net.minecraft.world.entity.player.Player)
     */
    @Override
    default void startOpen(net.minecraft.world.entity.player.Player player) {
    }

    /**
     * Called when a user stops using the inventory.
     * No default implementation.
     * @see Container#stopOpen(net.minecraft.world.entity.player.Player)
     */
    @Override
    default void stopOpen(net.minecraft.world.entity.player.Player player) {
    }

    /**
     * Check if the given player can use this inventory.
     * Defaults to always return true because BlockInventories are generally available to be used by anyone.
     * @see Container#stillValid(net.minecraft.world.entity.player.Player)
     */
    @Override
    default boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    /**
     * Returns {@code true} if automation (specifically hopper) can insert to the bottom of the inventory.
     * @return {@code true} if automation can insert to the bottom of the inventory, {@code false} otherwise.
     * @see Container#canPlaceItem(int, ItemStack)
     */
    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    /**
     * Returns the property delegate of this inventory.
     * @return the property delegate of this inventory.
     * @see Container getPropertyDelegate()
     */

    default net.minecraft.world.inventory.ContainerData getPropertyDelegate() {
        return new net.minecraft.world.inventory.SimpleContainerData(0);
    }
}