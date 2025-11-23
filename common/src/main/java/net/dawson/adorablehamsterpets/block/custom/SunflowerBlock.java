package net.dawson.adorablehamsterpets.block.custom;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.*;
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
        return state.get(HALF) == DoubleBlockHalf.UPPER && !state.get(HAS_SEEDS);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER && !state.get(HAS_SEEDS)) {
            // Access the stored config instance from the main mod class
            final AhpConfig config = AdorableHamsterPets.CONFIG;

            double modifier = config.sunflowerRegrowthModifier.get();
            modifier = Math.max(0.1, modifier);

            int baseRegrowthChanceDenominator = 150;
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
            // Check if the top block is indeed the upper half of this sunflower type
            if (topState.isOf(this) && topState.get(HALF) == DoubleBlockHalf.UPPER) {
                // Call onUse on the top block's state and position
                return this.onUse(topState, world, topPos, player, hit); // Call on 'this' instance but pass top state/pos
            }
            // If the top isn't the correct block, pass the interaction
            return ActionResult.PASS;
        }

        // Logic for the UPPER half
        if (state.get(HAS_SEEDS)) {
            if (!world.isClient) {
                int seedAmount = world.random.nextInt(3) + 1; // 1-3 seeds
                ItemStack seedStack = new ItemStack(ModItems.SUNFLOWER_SEEDS.get(), seedAmount);
                ItemScatterer.spawn(world, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, seedStack);

                world.setBlockState(pos, state.with(HAS_SEEDS, false), Block.NOTIFY_LISTENERS); // Set seedless
                world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0f, 1.0f); // Play sound
            }
            // Consume the action on both client and server if seeds were present
            return ActionResult.success(world.isClient);
        }

        // If not seeded, pass the interaction
        return ActionResult.PASS;
    }


    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        // Let the parent class place the top half FIRST
        super.onPlaced(world, pos, state, placer, itemStack);

        // Now, find the top half and modify its state if on the server
        if (!world.isClient) {
            BlockPos topPos = pos.up();
            BlockState topState = world.getBlockState(topPos);

            if (topState.isOf(this) && topState.get(HALF) == DoubleBlockHalf.UPPER) {
                // Set the state to NO seeds initially
                world.setBlockState(topPos, topState.with(HAS_SEEDS, false), Block.NOTIFY_LISTENERS);
            }
        }
    }


    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(Items.SUNFLOWER); // Pick block gives vanilla sunflower
    }

    // --- Fertilizable Implementation (Keep vanilla behavior or disable) ---
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        // Only the bottom half can be bonemealed to grow the top
        return state.get(HALF) == DoubleBlockHalf.LOWER && world.getBlockState(pos.up()).isAir();
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        // Can only grow if it's the lower half and the space above is air
        return state.get(HALF) == DoubleBlockHalf.LOWER && world.isAir(pos.up());
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Standard TallPlantBlock grow logic places the top half
        TallPlantBlock.placeAt(world, this.getDefaultState().with(HALF, DoubleBlockHalf.UPPER).with(HAS_SEEDS, false), pos.up(), 2);
        // Ensure the newly placed top half starts WITHOUT seeds
    }
    // --- End Fertilizable ---

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Standard TallPlantBlock neighbor update logic handles breaking
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}