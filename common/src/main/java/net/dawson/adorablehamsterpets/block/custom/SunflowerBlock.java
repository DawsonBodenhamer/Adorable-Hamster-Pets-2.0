package net.dawson.adorablehamsterpets.block.custom;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.ClientParticleManager;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final MapCodec<SunflowerBlock> CODEC = TallFlowerBlock.createCodec(SunflowerBlock::new);
    public static final BooleanProperty HAS_SEEDS = BooleanProperty.of("has_seeds");
    public static final BooleanProperty LIT = Properties.LIT; // For glowing sunflower Easter egg

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public SunflowerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(HAS_SEEDS, true)
                .with(LIT, false));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<TallFlowerBlock> getCodec() {
        return (MapCodec<TallFlowerBlock>) (Object) CODEC;
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        // Only upper half needs ticks for regrowth or glowing
        return state.get(HALF) == DoubleBlockHalf.UPPER;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) != DoubleBlockHalf.UPPER) return;

        // --- 1. Seed Regrowth ---
        if (!state.get(HAS_SEEDS)) {
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

        // --- 2. Glowing Easter Egg ---
        // Only activate if enabled, at night, and currently unlit
        if (AdorableHamsterPets.WORLD_GEN_CONFIG.enableGlowingSunflowers && !world.isDay() && !state.get(LIT)) {
            int chance = AdorableHamsterPets.WORLD_GEN_CONFIG.glowingSunflowerChance.get();
            if (random.nextInt(chance) == 0) {
                world.setBlockState(pos, state.with(LIT, true), Block.NOTIFY_LISTENERS);
                // Schedule turn-off (10-30 seconds)
                world.scheduleBlockTick(pos, this, random.nextBetween(200, 600));
            }
        }

        // --- 3. Safety Cleanup ---
        // Ensure not lit during day
        if (state.get(LIT) && world.isDay()) {
            world.setBlockState(pos, state.with(LIT, false), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Handles the scheduled turn-off for the glowing effect
        BlockState currentState = world.getBlockState(pos);
        if (currentState.isOf(this) && currentState.get(LIT)) {
            world.setBlockState(pos, currentState.with(LIT, false), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // Prevent lighting glitches if a lit block is broken
        if (!state.isOf(newState.getBlock())) {
            if (state.get(LIT)) {
                world.updateNeighborsAlways(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);

        if (state.get(LIT) && state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos immutablePos = pos.toImmutable();

            // Delegate to centralized particle manager
            ClientParticleManager.INSTANCE.addOrUpdate(immutablePos, "sunflower_glow_ring", (w) -> {
                ParticleEffectsUtil.spawnSpinningRing(
                        w,
                        immutablePos,
                        ParticleTypes.WAX_ON,
                        1,
                        0.5,
                        0.65,
                        0.4,
                        0.5,
                        5,
                        -0.4
                );
            }, (w, p) -> {
                BlockState current = w.getBlockState(p);
                return current.isOf(this) && current.get(LIT);
            });
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // --- 1. Redirect Lower Clicks to Upper Half ---
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            BlockPos topPos = pos.up();
            BlockState topState = world.getBlockState(topPos);
            if (topState.isOf(this) && topState.get(HALF) == DoubleBlockHalf.UPPER) {
                return this.onUse(topState, world, topPos, player, hit);
            }
            return ActionResult.PASS;
        }

        // --- 2. Harvest Seeds ---
        if (state.get(HAS_SEEDS)) {
            if (!world.isClient) {
                int seedAmount = world.random.nextInt(3) + 1; // 1-3 seeds
                ItemStack seedStack = new ItemStack(ModItems.SUNFLOWER_SEEDS.get(), seedAmount);
                ItemScatterer.spawn(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, seedStack);

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
            // Newly placed sunflowers start without seeds
            if (topState.isOf(this) && topState.get(HALF) == DoubleBlockHalf.UPPER) {
                world.setBlockState(topPos, topState.with(HAS_SEEDS, false), Block.NOTIFY_LISTENERS);
            }
        }
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.SUNFLOWER_BLOCK_ITEM);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

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
        // Vanilla behavior: drop a copy of the flower
        dropStack(world, pos, new ItemStack(ModItems.SUNFLOWER_BLOCK_ITEM));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Protected Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_SEEDS, LIT);
    }
}