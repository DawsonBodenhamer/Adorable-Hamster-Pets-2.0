package net.dawson.adorablehamsterpets.block.custom;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class GreenBeansCropBlock extends CropBlock {
    // --- CORRECTED: Set MAX_AGE to 3 ---
    public static final int MAX_AGE = 3;
    // --- CORRECTED: Set IntProperty range to 0-3 ---
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);


    public GreenBeansCropBlock(Properties settings) {
        super(settings);
    }


    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.GREEN_BEAN_SEEDS.get();
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        // --- CORRECTED: Return the updated MAX_AGE ---
        return MAX_AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}