package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates logic for evaluating crop maturity and handling automatic replanting.
 */
public final class HamsterHarvestUtil {

    private HamsterHarvestUtil() {}

    /**
     * Evaluates if a given crop block state is fully mature.
     * Supports both standard CropBlocks and non-standard plant blocks by scanning for an age property.
     */
    public static boolean isMature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMature(state);
        }

        // Fallback for non-standard crops (e.g. Nether Wart)
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("age") && property instanceof IntProperty intProp) {
                // Return true if age is at max allowed value
                return state.get(intProp).equals(intProp.getValues().stream().max(Integer::compareTo).orElse(0));
            }
        }
        return false;
    }

    /**
     * Simulates breaking the crop, intercepts the dropped items, securely resets the block
     * to growth stage 0 (if replanting), deducts a seed from the drops to account for the replanting,
     * and spawns the remaining items as entities in the world.
     *
     * @return A list of the spawned ItemEntities for the AI to track down.
     */
    public static List<ItemEntity> harvestAndReplant(ServerWorld world, BlockPos pos, BlockState state) {
        List<ItemEntity> spawnedItems = new ArrayList<>();

        boolean shouldReplant = world.random.nextFloat() < AdorableHamsterPets.CONFIG.cropReplantChance.get();

        // --- 1. Calculate standard loot drops ---
        LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                .add(LootContextParameters.TOOL, ItemStack.EMPTY)
                .add(LootContextParameters.BLOCK_STATE, state);

        List<ItemStack> drops = state.getDroppedStacks(builder);

        // --- 2. Evaluate Block Reset or Removal ---
        if (shouldReplant) {
            // Remove one seed-like item to simulate the cost of replanting
            boolean seedRemoved = false;
            for (ItemStack stack : drops) {
                if (stack.isIn(ItemTags.VILLAGER_PLANTABLE_SEEDS) || stack.getTranslationKey().contains("seed")) {
                    stack.decrement(1);
                    seedRemoved = true;
                    break;
                }
            }
            // Fallback: If no seeds dropped, just subtract 1 of whatever dropped
            if (!seedRemoved && !drops.isEmpty()) {
                drops.get(0).decrement(1);
            }

            // Reset block state dynamically by hunting for age property
            BlockState newState = state;
            for (Property<?> property : state.getProperties()) {
                if (property.getName().equals("age") && property instanceof IntProperty intProp) {
                    newState = state.with(intProp, 0); // Reset to age 0
                    break;
                }
            }

            // Apply fallback if no age property found (just replace with default state)
            if (newState == state) {
                newState = state.getBlock().getDefaultState();
            }

            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
        } else {
            // Hamster didn't replant, completely uproot the crop
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        // --- 3. Spawn remaining drops ---
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack);
                world.spawnEntity(itemEntity);
                spawnedItems.add(itemEntity);
            }
        }

        return spawnedItems;
    }
}