package net.dawson.adorablehamsterpets.block.custom;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.item.ItemConvertible;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

public class GreenBeansCropBlock extends CropBlock {
    // --- CORRECTED: Set MAX_AGE to 3 ---
    public static final int MAX_AGE = 3;
    // --- CORRECTED: Set IntProperty range to 0-3 ---
    public static final IntProperty AGE = IntProperty.of("age", 0, 3);


    public GreenBeansCropBlock(Settings settings) {
        super(settings);
    }


    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.GREEN_BEAN_SEEDS.get();
    }

    @Override
    protected IntProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        // --- CORRECTED: Return the updated MAX_AGE ---
        return MAX_AGE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}