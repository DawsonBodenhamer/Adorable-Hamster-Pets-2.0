package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.client.ClientInputUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.UUIDUtil;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.block.client.HamsterBedItemRenderer;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.AhpMainConfig;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import com.geckolib.renderer.GeoItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class HamsterBedItem extends BlockItem implements GeoItem {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Map<UUID, Long> UNLINKED_WARNING_COOLDOWNS = new ConcurrentHashMap<>();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final WoodVariant variant;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterBedItem(Block block, WoodVariant variant, Properties settings) {
        super(block, settings);
        this.variant = variant;
        GeoItem.registerSyncedAnimatable(this);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public WoodVariant getVariant() {
        return this.variant;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Event Handlers
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        // 26.2: inventoryTick is server-only now; the linked-name sync works fine from the server

        UUID linkedUuid = stack.get(ModDataComponentTypes.LINKED_HAMSTER_UUID.get());
        if (linkedUuid != null && entity instanceof Player) {
            // Search nearby entities for linked hamster
            world.getEntitiesOfClass(HamsterEntity.class, entity.getBoundingBox().inflate(16), e -> e.getUUID().equals(linkedUuid))
                    .stream().findFirst().ifPresent(hamster -> {
                        Component newName;
                        if (hamster.hasCustomName()) {
                            newName = hamster.getName();
                        } else {
                            newName = hamster.getDisplayName().copy().append(" " + hamster.getId());
                        }

                        Component currentNameOnStack = stack.get(ModDataComponentTypes.LINKED_HAMSTER_NAME.get());
                        if (currentNameOnStack == null || !currentNameOnStack.equals(newName)) {
                            stack.set(ModDataComponentTypes.LINKED_HAMSTER_NAME.get(), newName);
                        }
                    });
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();

        // --- Prevent placing if bed not linked & first time trying ---
        if (!stack.has(ModDataComponentTypes.LINKED_HAMSTER_UUID.get())) {
            Player player = context.getPlayer();
            Level world = context.getLevel();

            if (player != null) {
                AhpMainConfig config = Configs.AHP_MAIN;

                // 1. Check Master Toggle
                if (!config.warnOnUnlinkedBedPlacement) {
                    return super.useOn(context);
                }

                String username = player.getGameProfile().name();

                // 2. Check if player has already seen warning and waited
                if (config.playersWhoHaveSeenUnlinkedBedWarning.contains(username)) {
                    return super.useOn(context);
                }

                // 3. Client hand off authority to Server
                if (world.isClientSide()) {
                    return InteractionResult.SUCCESS; // Swing arm, send packet to server, do not place
                }

                UUID uuid = player.getUUID();
                long currentTime = world.getGameTime();

                // 3. Process Cooldown
                if (UNLINKED_WARNING_COOLDOWNS.containsKey(uuid)) {
                    long warningTime = UNLINKED_WARNING_COOLDOWNS.get(uuid);

                    if (currentTime - warningTime >= 40) { // 2 seconds
                        config.playersWhoHaveSeenUnlinkedBedWarning.add(username);
                        config.save();
                        UNLINKED_WARNING_COOLDOWNS.remove(uuid);
                        return super.useOn(context); // Place block
                    } else {
                        // Still on cooldown. Silently fail
                        return InteractionResult.FAIL;
                    }
                } else {
                    // 4. First time trying. Show warning, start cooldown
                    UNLINKED_WARNING_COOLDOWNS.put(uuid, currentTime);

                    world.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.2f, 0.5f);

                    MutableComponent msg = Component.literal("\n").append(Component.translatable("message.adorablehamsterpets.unlinked_bed_placement.1").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    msg.append("\n\n").append(Component.translatable("message.adorablehamsterpets.unlinked_bed_placement.2").withStyle(ChatFormatting.GRAY));
                    msg.append("\n");

                    player.sendSystemMessage(msg);

                    return InteractionResult.SUCCESS; // Consume action to prevent placement
                }
            }

            // If placed by a dispenser or some other non-player entity, just fail
            return InteractionResult.FAIL;
        }

        return super.useOn(context);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
        if (!world.isClientSide()) {
            // Sound and particle logic
            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
            if (rustleSound != null) {
                world.playSound(null, pos, rustleSound, SoundSource.BLOCKS, 0.5f, 1.5f);
            }

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof HamsterBedBlockEntity bedEntity) {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_being_placed");

                // Spawn particles with wood variant
                WoodVariant variant = stack.getOrDefault(ModDataComponentTypes.WOOD_VARIANT.get(), this.variant);
                ParticleEffectsUtil.spawnParticles(
                        world,
                        pos,
                        0.3,
                        ModParticles.getForVariant(variant),
                        30,
                        0.1, 0.2, 0.1, 0.0
                );
            }
        }
        // Set block state with correct wood variant after placement
        return world.setBlock(pos, state.setValue(HamsterBedBlock.WOOD_VARIANT, this.variant), Block.UPDATE_ALL | Block.UPDATE_IMMEDIATE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        if (Configs.AHP_UI.enableItemTooltips) {
            if (ClientInputUtil.hasShiftDown()) {
                // --- Expanded Tooltip ---

                // Main hints
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.description1").withStyle(ChatFormatting.GOLD));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.description2").withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.jade.wander_controls1").withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.jade.wander_controls2").withStyle(ChatFormatting.GRAY));

                // Dynamic interaction hints
                Component lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.lureItems).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                Component repellentName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.bedAvoidanceFoods).copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.jade.lure_hint", lureName).withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.jade.repellent_hint", repellentName).withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.jade.unlink_hint", repellentName).withStyle(ChatFormatting.GRAY));

                // Respawn status and hint
                boolean configEnabled = Configs.AHP_MAIN.enableRespawnInBed.get();
                boolean freeRespawns = Configs.AHP_MAIN.freeBedRespawns.get();

                Component statusText;
                Component hintText;

                if (!configEnabled) {
                    statusText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.disabled_config");
                    hintText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.disabled_config");
                } else if (freeRespawns) {
                    statusText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active_free");
                } else {
                    // Inventory items always inactive regarding respawn state
                    statusText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.inactive");
                    Component tributeName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.resurrectionTributes);
                    hintText = Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.inactive", tributeName.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }

                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.label", statusText));
                tooltip.accept(hintText);

                // Conditional linked info
                UUID hamsterUuid = stack.get(ModDataComponentTypes.LINKED_HAMSTER_UUID.get());
                Component hamsterName = stack.get(ModDataComponentTypes.LINKED_HAMSTER_NAME.get());
                WanderDistance wanderDistance = stack.get(ModDataComponentTypes.WANDER_DISTANCE.get());

                if (hamsterUuid != null && hamsterName != null) {
                    // Blank line for spacing
                    tooltip.accept(Component.literal(""));
                    tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.linked_to", hamsterName).withStyle(ChatFormatting.GREEN));
                    if (wanderDistance != null) {
                        int radius = switch (wanderDistance) {
                            case NEAR -> Configs.AHP_MAIN.wanderDistanceNear.get();
                            case FAR -> Configs.AHP_MAIN.wanderDistanceFar.get();
                            default -> Configs.AHP_MAIN.wanderDistanceMedium.get();
                        };
                        tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.wander_distance", Component.translatable(wanderDistance.translationKey()), radius).withStyle(ChatFormatting.AQUA));
                    }
                }
            } else {
                // Default condensed tooltip
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.hamster_bed.description1").withStyle(ChatFormatting.GOLD));
                tooltip.accept(Component.translatable("tooltip.adorablehamsterpets.shift_for_info").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.accept(Component.literal("Adorable Hamster Pets").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, context, display, tooltip, type);
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No item animations needed
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HamsterBedItemRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null)
                    renderer = new HamsterBedItemRenderer();
                return renderer;
            }
        });
    }
}