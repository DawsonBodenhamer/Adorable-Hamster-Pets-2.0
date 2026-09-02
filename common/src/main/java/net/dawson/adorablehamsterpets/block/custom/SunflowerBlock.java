package net.dawson.adorablehamsterpets.block.custom;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.util.ClientParticleManager;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SunflowerBlock extends TallFlowerBlock implements BonemealableBlock {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final MapCodec<SunflowerBlock> CODEC = TallFlowerBlock.simpleCodec(SunflowerBlock::new);
    public static final BooleanProperty HAS_SEEDS = BooleanProperty.create("has_seeds");
    public static final BooleanProperty LIT = BlockStateProperties.LIT; // For glowing sunflower Easter egg

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public SunflowerBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(HAS_SEEDS, true)
                .setValue(LIT, false));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<TallFlowerBlock> codec() {
        return (MapCodec<TallFlowerBlock>) (Object) CODEC;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // Only upper half needs ticks for regrowth or glowing
        return state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER) return;

        // --- 1. Seed Regrowth ---
        if (!state.getValue(HAS_SEEDS)) {
            final AhpWorldGenConfig config = AdorableHamsterPets.WORLD_GEN_CONFIG;

            double modifier = config.sunflowerRegrowthModifier.get();
            modifier = Math.max(0.1, modifier);

            int baseRegrowthChanceDenominator = 10;
            int effectiveDenominator = (int) Math.round(baseRegrowthChanceDenominator * modifier);
            effectiveDenominator = Math.max(1, effectiveDenominator);

            if (random.nextInt(effectiveDenominator) == 0) {
                world.setBlock(pos, state.setValue(HAS_SEEDS, true), Block.UPDATE_CLIENTS);
            }
        }

        // --- 2. Glowing Easter Egg ---
        // Only activate if enabled, at night, and currently unlit
        if (AdorableHamsterPets.WORLD_GEN_CONFIG.enableGlowingSunflowers && !world.isBrightOutside() && !state.getValue(LIT)) {
            int chance = AdorableHamsterPets.WORLD_GEN_CONFIG.glowingSunflowerChance.get();
            if (random.nextInt(chance) == 0) {
                world.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
                // Schedule turn-off (10-30 seconds)
                world.scheduleTick(pos, this, random.nextIntBetweenInclusive(200, 600));
            }
        }

        // --- 3. Safety Cleanup ---
        // Ensure not lit during day
        if (state.getValue(LIT) && world.isBrightOutside()) {
            world.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // Handles the scheduled turn-off for the glowing effect
        BlockState currentState = world.getBlockState(pos);
        if (currentState.is(this) && currentState.getValue(LIT)) {
            world.setBlock(pos, currentState.setValue(LIT, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        // Prevent lighting glitches if a lit block is broken
        if (state.getValue(LIT)) {
            world.updateNeighborsAt(pos, this);
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        super.animateTick(state, world, pos, random);

        if (state.getValue(LIT) && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos immutablePos = pos.immutable();

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
                return current.is(this) && current.getValue(LIT);
            });
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        // --- 1. Redirect Lower Clicks to Upper Half ---
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockPos topPos = pos.above();
            BlockState topState = world.getBlockState(topPos);
            if (topState.is(this) && topState.getValue(HALF) == DoubleBlockHalf.UPPER) {
                return this.useWithoutItem(topState, world, topPos, player, hit);
            }
            return InteractionResult.PASS;
        }

        // --- 2. Harvest Seeds ---
        if (state.getValue(HAS_SEEDS)) {
            if (!world.isClientSide()) {
                int seedAmount = world.getRandom().nextInt(3) + 1; // 1-3 seeds
                ItemStack seedStack = new ItemStack(ModItems.SUNFLOWER_SEEDS.get(), seedAmount);
                Containers.dropItemStack(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, seedStack);

                world.setBlock(pos, state.setValue(HAS_SEEDS, false), Block.UPDATE_CLIENTS);
                world.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            BlockPos topPos = pos.above();
            BlockState topState = world.getBlockState(topPos);
            // Newly placed sunflowers start without seeds
            if (topState.is(this) && topState.getValue(HALF) == DoubleBlockHalf.UPPER) {
                world.setBlock(topPos, topState.setValue(HAS_SEEDS, false), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ModItems.SUNFLOWER_BLOCK_ITEM.get());
    }


    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        // Allow bonemeal anywhere to duplicate the flower (vanilla behavior for tall flowers)
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        // Vanilla behavior: drop a copy of the flower
        popResource(world, pos, new ItemStack(ModItems.SUNFLOWER_BLOCK_ITEM.get()));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Protected Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_SEEDS, LIT);
    }
}