package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
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
            return crop.isMaxAge(state);
        }

        // Fallback for non-standard crops (e.g. Nether Wart)
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("age") && property instanceof IntegerProperty intProp) {
                // Return true if age is at max allowed value
                return state.getValue(intProp).equals(intProp.getPossibleValues().stream().max(Integer::compareTo).orElse(0));
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
    public static List<ItemEntity> harvestAndReplant(ServerLevel world, BlockPos pos, BlockState state) {
        List<ItemEntity> spawnedItems = new ArrayList<>();

        boolean shouldReplant = world.getRandom().nextFloat() < AdorableHamsterPets.MAIN_CONFIG.cropReplantChance.get();

        // --- 1. Calculate standard loot drops ---
        LootParams.Builder builder = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withParameter(LootContextParams.BLOCK_STATE, state);

        List<ItemStack> drops = state.getDrops(builder);

        // --- 2. Evaluate Block Reset or Removal ---
        if (shouldReplant) {
            // Remove one seed-like item to simulate the cost of replanting
            boolean seedRemoved = false;
            for (ItemStack stack : drops) {
                if (stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) || stack.getItem().getDescriptionId().contains("seed")) {
                    stack.shrink(1);
                    seedRemoved = true;
                    break;
                }
            }
            // Fallback: If no seeds dropped, just subtract 1 of whatever dropped
            if (!seedRemoved && !drops.isEmpty()) {
                drops.get(0).shrink(1);
            }

            // Reset block state dynamically by hunting for age property
            BlockState newState = state;
            for (Property<?> property : state.getProperties()) {
                if (property.getName().equals("age") && property instanceof IntegerProperty intProp) {
                    newState = state.setValue(intProp, 0); // Reset to age 0
                    break;
                }
            }

            // Apply fallback if no age property found (just replace with default state)
            if (newState == state) {
                newState = state.getBlock().defaultBlockState();
            }

            world.setBlock(pos, newState, Block.UPDATE_ALL);
        } else {
            // Hamster didn't replant, completely uproot the crop
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        // --- 3. Spawn remaining drops ---
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack);
                world.addFreshEntity(itemEntity);
                spawnedItems.add(itemEntity);
            }
        }

        return spawnedItems;
    }
}