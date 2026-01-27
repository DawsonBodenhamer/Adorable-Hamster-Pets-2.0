package net.dawson.adorablehamsterpets.block.custom;

import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.ModNbtKeys;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class HamsterBedBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final BooleanProperty OCCUPIED = BooleanProperty.of("occupied");
    public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.of("upside_down");
    public static final DirectionProperty ORIENTATION = DirectionProperty.of("orientation", dir -> dir.getAxis().isHorizontal());
    public static final EnumProperty<WoodVariant> WOOD_VARIANT = EnumProperty.of("wood_variant", WoodVariant.class);

    // --- VoxelShape Definitions ---
    private static final VoxelShape SHAPE_NORMAL = Stream.of(
            Block.createCuboidShape(1, 0, 1, 15, 1, 15), // Floor
            Block.createCuboidShape(1, 0, 1, 15, 3, 2),   // North Wall
            Block.createCuboidShape(1, 0, 14, 15, 3, 15), // South Wall
            Block.createCuboidShape(1, 0, 2, 2, 3, 14),   // West Wall
            Block.createCuboidShape(14, 0, 2, 15, 3, 14)  // East Wall
    ).reduce(VoxelShapes::union).get();

    private static final VoxelShape SHAPE_UPSIDE_DOWN = Stream.of(
            Block.createCuboidShape(1, 15, 1, 15, 16, 15), // Flipped Floor (now ceiling)
            Block.createCuboidShape(1, 13, 1, 15, 16, 2),   // Flipped North Wall
            Block.createCuboidShape(1, 13, 14, 15, 16, 15), // Flipped South Wall
            Block.createCuboidShape(1, 13, 2, 2, 16, 14),   // Flipped West Wall
            Block.createCuboidShape(14, 13, 2, 15, 16, 14)  // Flipped East Wall
    ).reduce(VoxelShapes::union).get();

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(UPSIDE_DOWN) ? SHAPE_UPSIDE_DOWN : SHAPE_NORMAL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(UPSIDE_DOWN) ? SHAPE_UPSIDE_DOWN : SHAPE_NORMAL;
    }

    public HamsterBedBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(OCCUPIED, false)
                .with(UPSIDE_DOWN, false)
                .with(ORIENTATION, Direction.NORTH)
                .with(WOOD_VARIANT, WoodVariant.OAK));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HamsterBedBlockEntity(pos, state);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OCCUPIED, UPSIDE_DOWN, ORIENTATION, WOOD_VARIANT);
    }

    // Pick a random but deterministic orientation when the block is placed
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        boolean isUpsideDown = ctx.getSide() == Direction.DOWN;
        // 1.20.1 NBT Logic for WoodVariant
        WoodVariant variant = WoodVariant.OAK; // Default
        ItemStack stack = ctx.getStack();
        if (stack.hasNbt() && stack.getNbt().contains(ModNbtKeys.WOOD_VARIANT)) {
            try {
                variant = WoodVariant.valueOf(stack.getNbt().getString(ModNbtKeys.WOOD_VARIANT));
            } catch (IllegalArgumentException ignored) {}
        }

        BlockPos pos = ctx.getBlockPos();
        // Derive a pseudo‑random but deterministic orientation from the block position
        long s = pos.asLong() ^ 0x9E3779B97F4A7C15L; // Noise so lines/grids don’t align
        s ^= (s >>> 30);  s *= 0xBF58476D1CE4E5B9L;
        s ^= (s >>> 27);  s *= 0x94D049BB133111EBL;
        s ^= (s >>> 31);

        int idx = (int)(s & 3L); // 0..3 → N/E/S/W
        Direction direction = Direction.fromHorizontal(idx);

        return getDefaultState()
                .with(OCCUPIED, false)
                .with(UPSIDE_DOWN, isUpsideDown)
                .with(ORIENTATION, direction)
                .with(WOOD_VARIANT, variant);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof HamsterBedBlockEntity bedEntity) {
                // If placed upside down, disable sleeping for this specific bed and trigger advancement
                if (state.get(UPSIDE_DOWN)) {
                    bedEntity.setAllowSleep(false);
                    if (placer instanceof ServerPlayerEntity serverPlayer) {
                        ModCriteria.HAMSTER_BED_PLACED_UPSIDE_DOWN.trigger(serverPlayer);
                    }
                }

                // 1.20.1 NBT Logic
                if (itemStack.hasNbt()) {
                    NbtCompound nbt = itemStack.getNbt();

                    if (nbt.contains(ModNbtKeys.LINKED_HAMSTER_UUID)) {
                        UUID hamsterUuid = nbt.getUuid(ModNbtKeys.LINKED_HAMSTER_UUID);

                        // Name extraction
                        Text hamsterName = null;
                        if (nbt.contains(ModNbtKeys.LINKED_HAMSTER_NAME)) {
                            hamsterName = Text.Serializer.fromJson(nbt.getString(ModNbtKeys.LINKED_HAMSTER_NAME));
                        }

                        // Wander Distance extraction
                        WanderDistance wanderDistance = Configs.AHP.defaultWanderDistance.get();
                        if (nbt.contains(ModNbtKeys.WANDER_DISTANCE)) {
                            try {
                                wanderDistance = WanderDistance.valueOf(nbt.getString(ModNbtKeys.WANDER_DISTANCE));
                            } catch (IllegalArgumentException ignored) {}
                        }

                        bedEntity.setLinkedHamster(hamsterUuid, hamsterName != null ? hamsterName : Text.literal("Hamster"), wanderDistance);

                        // Find and update hamster
                        Entity entity = ((ServerWorld) world).getEntity(hamsterUuid);
                        if (entity instanceof HamsterEntity hamster) {
                            hamster.setLinkedBedPos(Optional.of(GlobalPos.create(world.getRegistryKey(), pos)));
                            hamster.setWanderModeActive(true);
                        }
                    }
                }
            }

            if (be instanceof HamsterBedBlockEntity bedEntity) {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_being_placed");
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            // --- 1. Trigger hand swing ---
            // In 1.20.1, use the 'hand' passed to the method
            ItemStack heldStack = player.getStackInHand(hand);
            boolean isLureItem = ConfigDataCache.isLureItem(heldStack);
            boolean isAvoidanceItem = ConfigDataCache.isBedAvoidanceFood(heldStack);
            return (isLureItem || isAvoidanceItem) ? ActionResult.SUCCESS : ActionResult.CONSUME;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof HamsterBedBlockEntity bedEntity) {

            // --- 2. Trigger Animation, Particles, and Sound on any interaction ---
            if (state.get(OCCUPIED)) {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_interact_occupied");
            } else {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_interact_unoccupied");
            }

            // Spawn Particles with wood variant
            ((ServerWorld)world).spawnParticles(ModParticles.getForVariant(state.get(WOOD_VARIANT)),
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    30, 0.1, 0.1, 0.1, 0.0);

            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
            if (rustleSound != null) {
                world.playSound(null, pos, rustleSound, SoundCategory.BLOCKS, 0.2f, 1.8f);
            }

            // In 1.20.1, use the 'hand' passed to the method
            ItemStack heldStack = player.getStackInHand(hand);

            // --- 3. Unlinking Logic (Sneak + Bed Avoidance Item) ---
            if (player.isSneaking() && ConfigDataCache.isBedAvoidanceFood(heldStack)) {
                bedEntity.unlinkHamster(player);
                return ActionResult.SUCCESS;
            }

            // --- 4. Bed Avoidance Food Interaction ---
            if (ConfigDataCache.isBedAvoidanceFood(heldStack)) {
                // Wake up hamster if bed is occupied
                if (state.get(OCCUPIED)) {
                    bedEntity.getLinkedHamsterUuid().ifPresent(uuid -> {
                        Entity entity = ((ServerWorld) world).getEntity(uuid);
                        if (entity instanceof HamsterEntity hamster && hamster.isSleeping()) {
                            hamster.wakeUpFromBed(true); // Categorize as manual wake-up
                        }
                    });
                }

                // --- 5. Apply repellent effect ---
                bedEntity.applyRepellentEffect();
                player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_repellent_applied").formatted(Formatting.RED), true);
                world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 1.2f, 0.8f);
                ((ServerWorld)world).spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 15, 0.4, 0.3, 0.4, 0.01);
                if (!player.getAbilities().creativeMode) {
                    heldStack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            // --- 6. Lure Item Interaction ---
            if (ConfigDataCache.isLureItem(heldStack)) {
                if (state.get(UPSIDE_DOWN)) {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_upside_down_lure_fail").formatted(Formatting.RED), true);
                    world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.5f, 1.5f);
                    return ActionResult.SUCCESS;
                }

                boolean wasRepellentActive = !bedEntity.isSleepingAllowed();
                if (wasRepellentActive) {
                    bedEntity.setAllowSleep(true);
                }

                boolean lureWasSuccessful = bedEntity.lureHamsterToBed(player, heldStack);

                if (wasRepellentActive) {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_repellent_removed").formatted(Formatting.WHITE), true);
                } else if (lureWasSuccessful) {
                    bedEntity.getLinkedHamsterName().ifPresent(name ->
                            player.sendMessage(Text.translatable("message.adorablehamsterpets.lure_to_bed_success", name), true)
                    );
                }

                if (!player.getAbilities().creativeMode && Configs.AHP.consumeLureItem) {
                    heldStack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            // --- 7. Sneak Action: Cycle Wander Distance ---
            if (player.isSneaking()) {
                bedEntity.cycleWanderDistance(player);
                return ActionResult.SUCCESS;
            }

            // --- 8. Default Action: Toggle Wander Mode ---
            bedEntity.toggleWanderMode(player);
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient()) {
                // Play Leaf Rustling sound, spawn particles with wood variant
                SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
                if (rustleSound != null) {
                    world.playSound(null, pos, rustleSound, SoundCategory.BLOCKS, 0.3f, 1.5f);
                }
                ((ServerWorld)world).spawnParticles(ModParticles.getForVariant(state.get(WOOD_VARIANT)),
                        pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                        30, 0.1, 0.1, 0.1, 0.0);

                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof HamsterBedBlockEntity bedEntity) {
                    ServerWorld serverWorld = (ServerWorld) world;
                    bedEntity.getLinkedHamsterUuid().ifPresent(uuid -> {
                        Entity entity = serverWorld.getEntity(uuid);
                        if (entity instanceof HamsterEntity hamster) {
                            hamster.setWanderModeActive(false);
                            hamster.setLinkedBedPos(Optional.empty());
                            if (hamster.isSleeping()) {
                                hamster.wakeUpFromBed(true); // Manual wake-up
                            }
                            if (hamster.getOwner() instanceof PlayerEntity owner) {
                                if (Configs.AHP.enableBedBreakMessage) {
                                    owner.sendMessage(Text.translatable("message.adorablehamsterpets.bed_broken").formatted(Formatting.RED), true);
                                }
                            }
                        }
                    });
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return checkType(type, ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), (world1, pos, state1, be) -> {
                if (state1.get(UPSIDE_DOWN)) {
                    if (world1.random.nextInt(35) == 0) { // Chance to spawn particles
                        double x = pos.getX() + world1.random.nextDouble();
                        double y = pos.getY() + 0.6; // Spawn slightly below the block, offset inverted since block is inverted
                        double z = pos.getZ() + world1.random.nextDouble();

                        // Spawn Particles with wood variant
                        world1.addParticle(ModParticles.getForVariant(state1.get(WOOD_VARIANT)), x, y, z, 0, HamsterBeddingParticle.BEDDING_ITEM_FLAG, 0);
                    }
                }
            });
        }
        return null;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        // Called by Jade and middle-mouse-click; should return the correct item variant.
        WoodVariant variant = state.get(WOOD_VARIANT);
        Item item = ModItems.HAMSTER_BED_ITEMS.get(variant).get();
        return new ItemStack(item);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        WoodVariant variant = state.get(WOOD_VARIANT);
        Item itemToDrop = ModItems.HAMSTER_BED_ITEMS.get(variant).get();
        ItemStack stack = new ItemStack(itemToDrop);

        BlockEntity blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY);
        // 1.20.1 NBT logic
        if (blockEntity instanceof HamsterBedBlockEntity bedEntity) {
            NbtCompound nbt = stack.getOrCreateNbt(); // Prepare NBT

            bedEntity.getLinkedHamsterUuid().ifPresent(uuid ->
                    nbt.putUuid(ModNbtKeys.LINKED_HAMSTER_UUID, uuid));
            bedEntity.getLinkedHamsterName().ifPresent(name ->
                    nbt.putString(ModNbtKeys.LINKED_HAMSTER_NAME, Text.Serializer.toJson(name)));
            nbt.putString(ModNbtKeys.WANDER_DISTANCE, bedEntity.getWanderDistance().asString());
        }
        return List.of(stack);
    }
}