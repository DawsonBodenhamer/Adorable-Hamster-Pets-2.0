package net.dawson.adorablehamsterpets.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * 26.2 port: {@code ItemTags.FLOWERS} no longer exists. Flowers are still
 * tagged as blocks, so an item counts as a flower when it places a block in
 * {@link BlockTags#FLOWERS}.
 */
public final class FlowerItemUtil {
    private FlowerItemUtil() {}

    public static boolean isFlower(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(BlockTags.FLOWERS);
    }
}
