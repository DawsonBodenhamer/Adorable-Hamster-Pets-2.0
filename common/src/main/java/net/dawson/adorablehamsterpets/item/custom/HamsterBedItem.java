package net.dawson.adorablehamsterpets.item.custom;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.block.client.HamsterBedItemRenderer;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class HamsterBedItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final WoodVariant variant;

    public HamsterBedItem(Block block, WoodVariant variant, Settings settings) {
        super(block, settings);
        this.variant = variant;
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
        if (!world.isClient) {
            // --- Sound and Particle Logic ---
            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
            if (rustleSound != null) {
                world.playSound(null, pos, rustleSound, SoundCategory.BLOCKS, 0.5f, 1.5f);
            }

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof HamsterBedBlockEntity bedEntity) {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_being_placed");

                // Spawn particles with the wood variant
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
        // Set the block state with the correct wood variant after it has been placed.
        return world.setBlockState(pos, state.with(HamsterBedBlock.WOOD_VARIANT, this.variant), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        WoodVariant stackVariant = stack.getOrDefault(ModDataComponentTypes.WOOD_VARIANT.get(), this.variant);
        if (Configs.AHP.enableItemTooltips) {
            if (Screen.hasShiftDown()) {
                // --- Expanded Tooltip (Holding shift) ---
                // --- 1. Main Hints ---
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.description1").formatted(Formatting.GOLD));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.description2").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls1").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls2").formatted(Formatting.GRAY));

                // --- 2. Dynamic Interaction Hints ---
                Text lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP.lureItems).copy().formatted(Formatting.GOLD, Formatting.BOLD);
                Text repellentName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP.bedAvoidanceFoods).copy().formatted(Formatting.RED, Formatting.BOLD);
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.lure_hint", lureName).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.repellent_hint", repellentName).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.unlink_hint", repellentName).formatted(Formatting.GRAY));

                // --- 3. Respawn Status & Hint ---
                boolean configEnabled = Configs.AHP.enableRespawnInBed.get();
                boolean freeRespawns = Configs.AHP.freeBedRespawns.get();

                Text statusText;
                Text hintText;

                if (!configEnabled) {
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.disabled_config");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.disabled_config");
                } else if (freeRespawns) {
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active_free");
                } else {
                    // Items in inventory are always "Inactive" regarding respawn state
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.inactive");
                    Text tributeName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP.resurrectionTributes);
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.inactive", tributeName.copy().formatted(Formatting.GOLD, Formatting.BOLD));
                }

                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.label", statusText));
                tooltip.add(hintText);

                // --- 4. Conditional Linked Info ---
                UUID hamsterUuid = stack.get(ModDataComponentTypes.LINKED_HAMSTER_UUID.get());
                Text hamsterName = stack.get(ModDataComponentTypes.LINKED_HAMSTER_NAME.get());
                WanderDistance wanderDistance = stack.get(ModDataComponentTypes.WANDER_DISTANCE.get());

                if (hamsterUuid != null && hamsterName != null) {
                    tooltip.add(Text.literal("")); // Blank line for spacing
                    tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.linked_to", hamsterName).formatted(Formatting.GREEN));
                    if (wanderDistance != null) {
                        int radius = switch (wanderDistance) {
                            case NEAR -> Configs.AHP.wanderDistanceNear.get();
                            case FAR -> Configs.AHP.wanderDistanceFar.get();
                            default -> Configs.AHP.wanderDistanceMedium.get();
                        };
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.wander_distance", Text.translatable(wanderDistance.translationKey()), radius).formatted(Formatting.AQUA));
                    }
                }
            } else {
                // --- Default (Condensed) Tooltip ---
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.description1").formatted(Formatting.GOLD));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.shift_for_info").formatted(Formatting.DARK_GRAY));
            }
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    // InventoryTick override for dynamic tooltip
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            return;
        }

        UUID linkedUuid = stack.get(ModDataComponentTypes.LINKED_HAMSTER_UUID.get());
        if (linkedUuid != null && entity instanceof PlayerEntity) {
            // Search nearby entities for the linked hamster
            world.getEntitiesByClass(HamsterEntity.class, entity.getBoundingBox().expand(16), e -> e.getUuid().equals(linkedUuid))
                    .stream().findFirst().ifPresent(hamster -> {
                        Text newName;
                        if (hamster.hasCustomName()) {
                            newName = hamster.getName();
                        } else {
                            newName = hamster.getDisplayName().copy().append(" " + hamster.getId());
                        }

                        Text currentNameOnStack = stack.get(ModDataComponentTypes.LINKED_HAMSTER_NAME.get());
                        if (currentNameOnStack == null || !currentNameOnStack.equals(newName)) {
                            stack.set(ModDataComponentTypes.LINKED_HAMSTER_NAME.get(), newName);
                        }
                    });
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No item animations needed, so this is empty.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    public WoodVariant getVariant() {
        return this.variant;
    }

    @Override
    public String getTranslationKey() {
        // Forces the item to use its own unique translation key
        return this.getOrCreateTranslationKey();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HamsterBedItemRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getGeoItemRenderer() {
                if (renderer == null)
                    renderer = new HamsterBedItemRenderer();
                return renderer;
            }
        });
    }
}