package net.dawson.adorablehamsterpets.block.custom;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Represents a wild cucumber bush block that can be harvested for seeds and regrows over time.
 */
public class WildCucumberBushBlock extends PlantBlock {
    // --- Constants and Static Fields ---
    public static final BooleanProperty SEEDED = BooleanProperty.of("seeded");

    private static final VoxelShape SEEDLESS_SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    private static final VoxelShape SEEDED_SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    // --- Constructor ---
    public WildCucumberBushBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(SEEDED, true));
    }

    // --- Overridden Methods ---
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.CUCUMBER_SEEDS.get());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(SEEDED) ? SEEDED_SHAPE : SEEDLESS_SHAPE;
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return !state.get(SEEDED); // Only ticks when seedless to regrow
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // --- Regrowth Logic ---
        if (!state.get(SEEDED)) {
            final AhpConfig config = AdorableHamsterPets.CONFIG;
            double modifier = config.wildBushRegrowthModifier.get();
            modifier = Math.max(0.1, modifier); // Ensure positive modifier

            int baseRegrowthChanceDenominator = 5;
            int effectiveDenominator = (int) Math.round(baseRegrowthChanceDenominator * modifier);
            effectiveDenominator = Math.max(1, effectiveDenominator);

            if (world.getBaseLightLevel(pos.up(), 0) >= 9 && random.nextInt(effectiveDenominator) == 0) {
                BlockState newState = state.with(SEEDED, true);
                world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
                world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(newState));
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (state.get(SEEDED)) {
            if (!world.isClient) {
                int seedAmount = 1 + world.random.nextInt(2);
                dropStack(world, pos, new ItemStack(ModItems.CUCUMBER_SEEDS.get(), seedAmount));

                world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);

                BlockState newState = state.with(SEEDED, false);
                world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
                world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, newState));

                return ActionResult.SUCCESS;
            }
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SEEDED);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(Blocks.GRASS_BLOCK) || floor.isOf(Blocks.DIRT) || floor.isOf(Blocks.COARSE_DIRT)
                || floor.isOf(Blocks.PODZOL) || floor.isOf(Blocks.FARMLAND) || floor.isOf(Blocks.CLAY)
                || floor.isOf(Blocks.MOSS_BLOCK) || floor.isOf(Blocks.MUD);
    }
}