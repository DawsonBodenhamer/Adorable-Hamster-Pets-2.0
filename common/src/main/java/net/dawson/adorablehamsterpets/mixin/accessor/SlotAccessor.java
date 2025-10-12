package net.dawson.adorablehamsterpets.mixin.accessor;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Provides safe, cross-loader access to the inventory/container field within a Slot.
 * The Architectury remapper will automatically change the target of the "inventory" accessor
 * to "container" when building for the NeoForge platform.
 */
@Mixin(Slot.class)
public interface SlotAccessor {
    /**
     * Gets the inventory associated with this slot.
     * @return The slot's inventory.
     */
    @Accessor("inventory")
    Inventory adorablehamsterpets$getInventory();
}