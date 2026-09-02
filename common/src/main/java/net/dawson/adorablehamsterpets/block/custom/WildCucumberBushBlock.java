package net.dawson.adorablehamsterpets.block.custom;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Represents a wild cucumber bush block that can be harvested for seeds and regrows over time.
 */
public class WildCucumberBushBlock extends BushBlock {
    // --- Constants and Static Fields ---
    public static final MapCodec<WildCucumberBushBlock> CODEC = simpleCodec(WildCucumberBushBlock::new);
    public static final BooleanProperty SEEDED = BooleanProperty.create("seeded");

    private static final VoxelShape SEEDLESS_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    private static final VoxelShape SEEDED_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    // --- Constructor ---
    public WildCucumberBushBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(SEEDED, true));
    }

    // --- Overridden Methods ---
    @Override
    public MapCodec<WildCucumberBushBlock> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.CUCUMBER_SEEDS.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(SEEDED) ? SEEDED_SHAPE : SEEDLESS_SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(SEEDED); // Only ticks when seedless to regrow
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // --- Regrowth Logic ---
        if (!state.getValue(SEEDED)) {
            final AhpWorldGenConfig config = AdorableHamsterPets.WORLD_GEN_CONFIG;
            double modifier = config.wildBushRegrowthModifier.get();
            modifier = Math.max(0.1, modifier); // Ensure positive modifier

            int baseRegrowthChanceDenominator = 5;
            int effectiveDenominator = (int) Math.round(baseRegrowthChanceDenominator * modifier);
            effectiveDenominator = Math.max(1, effectiveDenominator);

            if (world.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(effectiveDenominator) == 0) {
                BlockState newState = state.setValue(SEEDED, true);
                world.setBlock(pos, newState, Block.UPDATE_CLIENTS);
                world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        // --- Harvesting Logic ---
        if (state.getValue(SEEDED)) {
            if (!world.isClientSide) {
                int seedAmount = 1 + world.random.nextInt(2); // Drop 1 or 2 seeds
                popResource(world, pos, new ItemStack(ModItems.CUCUMBER_SEEDS.get(), seedAmount));

                world.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);

                BlockState newState = state.setValue(SEEDED, false);
                world.setBlock(pos, newState, Block.UPDATE_CLIENTS);
                world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));

                return InteractionResult.SUCCESS;
            }
            return InteractionResult.sidedSuccess(world.isClientSide); // Indicate client-side success
        }
        return InteractionResult.PASS; // Not seeded, pass interaction
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEEDED);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.GRASS_BLOCK) || floor.is(Blocks.DIRT) || floor.is(Blocks.COARSE_DIRT)
                || floor.is(Blocks.PODZOL) || floor.is(Blocks.FARMLAND) || floor.is(Blocks.CLAY)
                || floor.is(Blocks.MOSS_BLOCK) || floor.is(Blocks.MUD);
    }
}