package net.dawson.adorablehamsterpets.block.custom;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterBedUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class HamsterBedBlock extends BaseEntityBlock implements EntityBlock {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final MapCodec<HamsterBedBlock> CODEC = simpleCodec(HamsterBedBlock::new);

    // Block State Properties
    public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");
    public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.create("upside_down");
    public static final DirectionProperty ORIENTATION = DirectionProperty.create("orientation", dir -> dir.getAxis().isHorizontal());
    public static final EnumProperty<WoodVariant> WOOD_VARIANT = EnumProperty.create("wood_variant", WoodVariant.class);

    // Voxel Shapes
    private static final VoxelShape SHAPE_NORMAL = Stream.of(
            Block.box(1, 0, 1, 15, 1, 15), // Floor
            Block.box(1, 0, 1, 15, 3, 2),   // North Wall
            Block.box(1, 0, 14, 15, 3, 15), // South Wall
            Block.box(1, 0, 2, 2, 3, 14),   // West Wall
            Block.box(14, 0, 2, 15, 3, 14)  // East Wall
    ).reduce(Shapes::or).get();

    private static final VoxelShape SHAPE_UPSIDE_DOWN = Stream.of(
            Block.box(1, 15, 1, 15, 16, 15), // Flipped Floor (ceiling)
            Block.box(1, 13, 1, 15, 16, 2),   // Flipped North Wall
            Block.box(1, 13, 14, 15, 16, 15), // Flipped South Wall
            Block.box(1, 13, 2, 2, 16, 14),   // Flipped West Wall
            Block.box(14, 13, 2, 15, 16, 14)  // Flipped East Wall
    ).reduce(Shapes::or).get();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterBedBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(OCCUPIED, false)
                .setValue(UPSIDE_DOWN, false)
                .setValue(ORIENTATION, Direction.NORTH)
                .setValue(WOOD_VARIANT, WoodVariant.OAK));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(UPSIDE_DOWN) ? SHAPE_UPSIDE_DOWN : SHAPE_NORMAL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(UPSIDE_DOWN) ? SHAPE_UPSIDE_DOWN : SHAPE_NORMAL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HamsterBedBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean isUpsideDown = ctx.getClickedFace() == Direction.DOWN;
        WoodVariant variant = ctx.getItemInHand().getOrDefault(ModDataComponentTypes.WOOD_VARIANT.get(), WoodVariant.OAK);
        BlockPos pos = ctx.getClickedPos();

        // Derive a pseudo‑random but deterministic orientation from the block position
        long s = pos.asLong() ^ 0x9E3779B97F4A7C15L; // Noise so lines/grids don’t align
        s ^= (s >>> 30); s *= 0xBF58476D1CE4E5B9L;
        s ^= (s >>> 27); s *= 0x94D049BB133111EBL;
        s ^= (s >>> 31);

        int idx = (int)(s & 3L); // 0..3 → N/E/S/W
        Direction direction = Direction.from2DDataValue(idx);

        return defaultBlockState()
                .setValue(OCCUPIED, false)
                .setValue(UPSIDE_DOWN, isUpsideDown)
                .setValue(ORIENTATION, direction)
                .setValue(WOOD_VARIANT, variant);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof HamsterBedBlockEntity bedEntity) {
                // If placed upside down, disable sleeping for this specific bed and trigger advancement
                if (state.getValue(UPSIDE_DOWN)) {
                    bedEntity.setAllowSleep(false);
                    if (placer instanceof ServerPlayer serverPlayer) {
                        ModCriteria.HAMSTER_BED_PLACED_UPSIDE_DOWN.get().trigger(serverPlayer);
                    }
                }
            }

            if (be instanceof HamsterBedBlockEntity bedEntity) {
                // Trigger placement animation
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_being_placed");

                UUID hamsterUuid = itemStack.get(ModDataComponentTypes.LINKED_HAMSTER_UUID.get());
                Component hamsterName = itemStack.get(ModDataComponentTypes.LINKED_HAMSTER_NAME.get());
                WanderDistance wanderDistance = itemStack.get(ModDataComponentTypes.WANDER_DISTANCE.get());

                if (hamsterUuid != null) {
                    bedEntity.setLinkedHamster(hamsterUuid, hamsterName, wanderDistance);

                    // Find the hamster in the world and activate its wander mode
                    Entity entity = ((ServerLevel) world).getEntity(hamsterUuid);
                    if (entity instanceof HamsterEntity hamster) {
                        hamster.setLinkedBedPos(Optional.of(GlobalPos.of(world.dimension(), pos)));
                        hamster.setWanderModeActive(true);
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide) {
            // --- 1. Client-Side Validation ---
            ItemStack heldStack = player.getItemInHand(player.getUsedItemHand());
            boolean isLureItem = ConfigDataCache.isLureItem(heldStack);
            boolean isAvoidanceItem = ConfigDataCache.isBedAvoidanceFood(heldStack);
            boolean isTributeItem = ConfigDataCache.isResurrectionTribute(heldStack);
            return (isLureItem || isAvoidanceItem || isTributeItem) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof HamsterBedBlockEntity bedEntity) {

            // --- 2. Generic Interaction Feedback ---
            if (state.getValue(OCCUPIED)) {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_interact_occupied");
            } else {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_interact_unoccupied");
            }

            // Spawn Particles with wood variant
            ParticleEffectsUtil.spawnParticles(
                    world,
                    pos,
                    0.2,
                    ModParticles.getForVariant(state.getValue(WOOD_VARIANT)),
                    30,
                    0.1, 0.1, 0.1, 0.0
            );

            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
            if (rustleSound != null) {
                world.playSound(null, pos, rustleSound, SoundSource.BLOCKS, 0.2f, 1.8f);
            }

            ItemStack heldStack = player.getItemInHand(player.getUsedItemHand());

            // --- 3. Unlinking Logic (Sneak + Repellent) ---
            if (player.isShiftKeyDown() && ConfigDataCache.isBedAvoidanceFood(heldStack)) {
                bedEntity.unlinkHamster(player);
                return InteractionResult.SUCCESS;
            }

            // --- 4. Handle Bed Avoidance/Repellent ---
            if (ConfigDataCache.isBedAvoidanceFood(heldStack)) {
                // Wake up hamster if bed is occupied
                if (state.getValue(OCCUPIED)) {
                    bedEntity.getLinkedHamsterUuid().ifPresent(uuid -> {
                        Entity entity = ((ServerLevel) world).getEntity(uuid);
                        if (entity instanceof HamsterEntity hamster && hamster.isSleeping()) {
                            HamsterBedUtil.wakeUpFromBed(hamster, true); // Manual wakeup
                        }
                    });
                }

                // Apply repellent
                bedEntity.applyRepellentEffect();
                player.displayClientMessage(Component.translatable("message.adorablehamsterpets.bed_repellent_applied").withStyle(ChatFormatting.RED), true);
                world.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1.2f, 0.8f);
                ParticleEffectsUtil.spawnParticles(
                        world,
                        pos,
                        0.7,
                        ParticleTypes.SMOKE,
                        15,
                        0.4, 0.3, 0.4, 0.01
                );
                if (!player.getAbilities().instabuild) {
                    heldStack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }

            // --- 5. Lure Item Logic ---
            if (ConfigDataCache.isLureItem(heldStack)) {
                if (state.getValue(UPSIDE_DOWN)) {
                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.bed_upside_down_lure_fail").withStyle(ChatFormatting.RED), true);
                    world.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.5f, 1.5f);
                    return InteractionResult.SUCCESS;
                }

                boolean wasRepellentActive = !bedEntity.isSleepingAllowed();
                if (wasRepellentActive) {
                    bedEntity.setAllowSleep(true);
                }

                boolean lureWasSuccessful = bedEntity.lureHamsterToBed(player, heldStack);

                if (wasRepellentActive) {
                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.bed_repellent_removed").withStyle(ChatFormatting.WHITE), true);
                } else if (lureWasSuccessful) {
                    bedEntity.getLinkedHamsterName().ifPresent(name ->
                            player.displayClientMessage(Component.translatable("message.adorablehamsterpets.lure_to_bed_success", name), true)
                    );
                }

                if (!player.getAbilities().instabuild && Configs.AHP_MAIN.consumeLureItem) {
                    heldStack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }

            // --- 6. Resurrection Tribute Logic ---
            if (ConfigDataCache.isResurrectionTribute(heldStack)) {
                // A. Check Global Config
                if (!Configs.AHP_MAIN.enableRespawnInBed.get()) {
                    bedEntity.triggerFailSound();
                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.respawn.disabled_by_config").withStyle(ChatFormatting.RED), true);
                    return InteractionResult.SUCCESS;
                }

                if (Configs.AHP_MAIN.freeBedRespawns.get()) {
                    return InteractionResult.PASS;
                }

                // B. Toggle Logic
                if (!bedEntity.isRespawnEnabled()) {
                    // Activate
                    bedEntity.setRespawnEnabled(true);

                    if (!player.getAbilities().instabuild) {
                        heldStack.shrink(1);
                    }

                    world.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);

                    // Spawn Totem particles if it's a totem, otherwise generic happy particles + item particles
                    if (heldStack.is(Items.TOTEM_OF_UNDYING)) {
                        ParticleEffectsUtil.spawnParticles(
                                world,
                                pos,
                                0.5,
                                ParticleTypes.TOTEM_OF_UNDYING,
                                50,
                                0.3, 0.3, 0.3, 0.0
                        );
                    } else {
                        ParticleEffectsUtil.spawnParticles(
                                world,
                                pos,
                                0.5,
                                ParticleTypes.HAPPY_VILLAGER,
                                25,
                                0.3, 0.3, 0.3, 0.0
                        );
                        // Use a copy of the stack for particles since it may have just been emptied
                        ItemStack particleStack = heldStack.isEmpty() ? new ItemStack(heldStack.getItem()) : heldStack;

                        ParticleEffectsUtil.spawnParticles(
                                world,
                                pos,
                                0.5,
                                new ItemParticleOption(ParticleTypes.ITEM, particleStack),
                                25,
                                0.2, 0.2, 0.2, 0.0
                        );
                    }

                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.respawn.activated").withStyle(ChatFormatting.GREEN), true);

                } else {
                    // Deactivate and refund
                    bedEntity.setRespawnEnabled(false);

                    // Refund the specific item held in hand to match the "key" used.
                    ItemStack refundStack = new ItemStack(heldStack.getItem());
                    Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, refundStack);

                    world.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.respawn.deactivated").withStyle(ChatFormatting.YELLOW), true);
                }

                return InteractionResult.SUCCESS;
            }

            // --- 7. Configuration Actions (Sneak Cycle / Toggle Wander) ---
            if (player.isShiftKeyDown()) {
                bedEntity.cycleWanderDistance(player);
                return InteractionResult.SUCCESS;
            }

            // --- 8. Default Action: Toggle Wander Mode ---
            bedEntity.toggleWanderMode(player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide()) {
                // Play Leaf Rustling sound, spawn particles with wood variant
                SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
                if (rustleSound != null) {
                    world.playSound(null, pos, rustleSound, SoundSource.BLOCKS, 0.3f, 1.5f);
                }
                ParticleEffectsUtil.spawnParticles(
                        world,
                        pos,
                        0.2,
                        ModParticles.getForVariant(state.getValue(WOOD_VARIANT)),
                        30,
                        0.1, 0.1, 0.1, 0.0
                );

                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof HamsterBedBlockEntity bedEntity) {
                    ServerLevel serverWorld = (ServerLevel) world;
                    bedEntity.getLinkedHamsterUuid().ifPresent(uuid -> {
                        Entity entity = serverWorld.getEntity(uuid);
                        if (entity instanceof HamsterEntity hamster) {
                            hamster.setWanderModeActive(false);
                            hamster.setLinkedBedPos(Optional.empty());
                            if (hamster.isSleeping()) {
                                HamsterBedUtil.wakeUpFromBed(hamster, true); // Manual wakeup
                            }
                            if (hamster.getOwner() instanceof Player owner) {
                                if (Configs.AHP_UI.enableBedBreakMessage) {
                                    owner.displayClientMessage(Component.translatable("message.adorablehamsterpets.bed_broken").withStyle(ChatFormatting.RED), true);
                                }
                            }
                        }
                    });
                }
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), (world1, pos, state1, be) -> {
                if (state1.getValue(UPSIDE_DOWN)) {
                    if (world1.random.nextInt(35) == 0) { // Chance to spawn particles
                        double x = pos.getX() + world1.random.nextDouble();
                        double y = pos.getY() + 0.6; // Spawn slightly below the block, offset inverted since block is inverted
                        double z = pos.getZ() + world1.random.nextDouble();

                        // Spawn Particles with wood variant
                        world1.addParticle(ModParticles.getForVariant(state1.getValue(WOOD_VARIANT)), x, y, z, 0, HamsterBeddingParticle.BEDDING_ITEM_FLAG, 0);
                    }
                }
            });
        } else {
            return createTickerHelper(type, ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), HamsterBedBlockEntity::tick);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        // Called by Jade and middle-mouse-click; should return the correct item variant.
        WoodVariant variant = state.getValue(WOOD_VARIANT);
        Item item = ModItems.HAMSTER_BED_ITEMS.get(variant).get();
        return new ItemStack(item);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        WoodVariant variant = state.getValue(WOOD_VARIANT);
        Item itemToDrop = ModItems.HAMSTER_BED_ITEMS.get(variant).get();
        ItemStack stack = new ItemStack(itemToDrop);

        BlockEntity blockEntity = builder.getParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof HamsterBedBlockEntity bedEntity) {
            bedEntity.getLinkedHamsterUuid().ifPresent(uuid -> stack.set(ModDataComponentTypes.LINKED_HAMSTER_UUID.get(), uuid));
            bedEntity.getLinkedHamsterName().ifPresent(name -> stack.set(ModDataComponentTypes.LINKED_HAMSTER_NAME.get(), name));
            stack.set(ModDataComponentTypes.WANDER_DISTANCE.get(), bedEntity.getWanderDistance());
        }
        return List.of(stack);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Protected Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OCCUPIED, UPSIDE_DOWN, ORIENTATION, WOOD_VARIANT);
    }
}