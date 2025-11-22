package net.dawson.adorablehamsterpets.item.custom.fabric;

import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.item.custom.HamsterBedItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class HamsterBedItemImpl {
    public static HamsterBedItem create(Block block, WoodVariant variant, Item.Settings settings) {
        // Fabric just returns the normal item, as it uses createRenderer inside the class
        return new HamsterBedItem(block, variant, settings);
    }
}