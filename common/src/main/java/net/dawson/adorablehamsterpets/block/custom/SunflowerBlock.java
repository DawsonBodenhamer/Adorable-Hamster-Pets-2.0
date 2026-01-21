package net.dawson.adorablehamsterpets.block.custom;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class SunflowerBlock extends TallFlowerBlock implements Fertilizable {

    public static final BooleanProperty HAS_SEEDS = BooleanProperty.of("has_seeds");
    public static final MapCodec<TallFlowerBlock> CODEC = TallFlowerBlock.createCodec(SunflowerBlock::new);

    public SunflowerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(HAS_SEEDS, true));
    }

    @Override
    public MapCodec<TallFlowerBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_SEEDS);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        // Random ticks on upper half to regrow seeds
        return state.get(HALF) == DoubleBlockHalf.UPPER && !state.get(HAS_SEEDS);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER && !state.get(HAS_SEEDS)) {
            final AhpWorldGenConfig config = AdorableHamsterPets.WORLD_GEN_CONFIG;

            double modifier = config.sunflowerRegrowthModifier.get();
            modifier = Math.max(0.1, modifier);

            int baseRegrowthChanceDenominator = 10;
            int effectiveDenominator = (int) Math.round(baseRegrowthChanceDenominator * modifier);
            effectiveDenominator = Math.max(1, effectiveDenominator);

            if (random.nextInt(effectiveDenominator) == 0) {
                world.setBlockState(pos, state.with(HAS_SEEDS, true), Block.NOTIFY_LISTENERS);
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Redirect clicks on lower half to upper half
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            BlockPos topPos = pos.up();
            BlockState topState = world.getBlockState(topPos);
            if (topState.isOf(this) && topState.get(HALF) == DoubleBlockHalf.UPPER) {
                return this.onUse(topState, world, topPos, player, hit);
            }
            return ActionResult.PASS;
        }

        // Logic for the UPPER half
        if (state.get(HAS_SEEDS)) {
            if (!world.isClient) {
                int seedAmount = world.random.nextInt(3) + 1; // 1-3 seeds
                ItemStack seedStack = new ItemStack(ModItems.SUNFLOWER_SEEDS.get(), seedAmount);
                ItemScatterer.spawn(world, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, seedStack);

                world.setBlockState(pos, state.with(HAS_SEEDS, false), Block.NOTIFY_LISTENERS);
                world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            BlockPos topPos = pos.up();
            BlockState topState = world.getBlockState(topPos);
            // Newly placed sunflowers start seedless (require growth time)
            if (topState.isOf(this) && topState.get(HALF) == DoubleBlockHalf.UPPER) {
                world.setBlockState(topPos, topState.with(HAS_SEEDS, false), Block.NOTIFY_LISTENERS);
            }
        }
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.SUNFLOWER_BLOCK_ITEM); // Pick block gives sunflower item
    }

    // --- Fertilizable Implementation ---
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        // Allow bonemeal anywhere to duplicate the flower (vanilla behavior for tall flowers)
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Drop a copy of the sunflower item
        dropStack(world, pos, new ItemStack(ModItems.SUNFLOWER_BLOCK_ITEM));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Standard TallPlantBlock neighbor update logic handles breaking
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}