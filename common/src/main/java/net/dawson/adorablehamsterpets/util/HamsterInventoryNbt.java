package net.dawson.adorablehamsterpets.util;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.2 port: ContainerHelper only speaks ValueInput/ValueOutput now, but the
 * hamster's NBT (and the item-form HamsterState built from it) is still a
 * CompoundTag. Slots are stored as a codec list under "Items"; empty slots are
 * kept in place so indices survive the round trip.
 */
public final class HamsterInventoryNbt {
    private static final String KEY = "Items";
    private HamsterInventoryNbt() {}

    public static void save(CompoundTag nbt, NonNullList<ItemStack> items) {
        nbt.store(KEY, ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(items));
    }

    public static void load(CompoundTag nbt, NonNullList<ItemStack> items) {
        nbt.read(KEY, ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < Math.min(list.size(), items.size()); i++) {
                items.set(i, list.get(i));
            }
        });
    }
}
