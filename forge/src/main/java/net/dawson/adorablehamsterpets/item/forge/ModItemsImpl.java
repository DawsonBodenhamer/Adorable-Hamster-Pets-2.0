package net.dawson.adorablehamsterpets.item.forge;

import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;


public class ModItemsImpl {
    /**
     * Provides the Forge implementation for registering the spawn egg.
     * It uses ForgeSpawnEggItem, which correctly handles the deferred entity type supplier.
     */
    public static RegistrySupplier<Item> registerSpawnEgg() {
        return ModItems.ITEMS.register("hamster_spawn_egg",
                () -> new ForgeSpawnEggItem(ModEntities.HAMSTER, 0x9c631f, 0xffffff, new Item.Settings()));
    }
}