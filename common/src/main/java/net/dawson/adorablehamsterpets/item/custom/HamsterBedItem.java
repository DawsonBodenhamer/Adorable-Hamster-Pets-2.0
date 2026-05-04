package net.dawson.adorablehamsterpets.item.custom;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.ModNbtKeys;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HamsterBedItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final WoodVariant variant;

    /**
     * Platform-agnostic factory method.
     * On Fabric: Returns new HamsterBedItem()
     * On Forge: Returns new HamsterBedItem() { @Override initializeClient... }
     */
    @ExpectPlatform
    public static HamsterBedItem create(Block block, WoodVariant variant, Settings settings) {
        throw new AssertionError();
    }

    // On 1.20.1, initialize the RenderProvider and implement the getter
    private final Supplier<Object> renderProvider = new Supplier<Object>() {
        private Object provider;

        @Override
        public Object get() {
            if (provider == null) {
                if (Platform.getEnvironment() == Env.CLIENT) {
                    // Only load HamsterBedRenderProvider on the client.
                    provider = net.dawson.adorablehamsterpets.item.client.HamsterBedRenderProvider.create();
                }
            }
            return provider;
        }
    };

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

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
                // 1.20.1 Wood variant Logic
                WoodVariant variant = this.variant; // Default to item's variant
                if (stack.hasNbt() && stack.getNbt().contains(ModNbtKeys.WOOD_VARIANT)) {
                    try {
                        variant = WoodVariant.valueOf(stack.getNbt().getString(ModNbtKeys.WOOD_VARIANT));
                    } catch (IllegalArgumentException ignored) {}
                }
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
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 1.20.1 Variant read logic
        WoodVariant stackVariant = this.variant;
        if (stack.hasNbt() && stack.getNbt().contains(ModNbtKeys.WOOD_VARIANT)) {
            try {
                stackVariant = WoodVariant.valueOf(stack.getNbt().getString(ModNbtKeys.WOOD_VARIANT));
            } catch (IllegalArgumentException ignored) {}
        }
        if (Configs.AHP.enableItemTooltips) {
            if (Screen.hasShiftDown()) {
                // --- Expanded Tooltip (Sneaking) ---
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
                if (stack.hasNbt()) {
                    NbtCompound nbt = stack.getNbt();
                    if (nbt.contains(ModNbtKeys.LINKED_HAMSTER_UUID) && nbt.contains(ModNbtKeys.LINKED_HAMSTER_NAME)) {

                        Text hamsterName = Text.Serializer.fromJson(nbt.getString(ModNbtKeys.LINKED_HAMSTER_NAME));

                        tooltip.add(Text.literal(""));
                        if (hamsterName != null) {
                            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.linked_to", hamsterName).formatted(Formatting.GREEN));
                        }

                        if (nbt.contains(ModNbtKeys.WANDER_DISTANCE)) {
                            try {
                                WanderDistance wanderDistance = WanderDistance.valueOf(nbt.getString(ModNbtKeys.WANDER_DISTANCE));
                                int radius = switch (wanderDistance) {
                                    case NEAR -> Configs.AHP.wanderDistanceNear.get();
                                    case FAR -> Configs.AHP.wanderDistanceFar.get();
                                    default -> Configs.AHP.wanderDistanceMedium.get();
                                };
                                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.wander_distance", Text.translatable(wanderDistance.translationKey()), radius).formatted(Formatting.AQUA));
                            } catch (IllegalArgumentException ignored) {}
                        }
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
        super.appendTooltip(stack, world, tooltip, context);
    }

    // InventoryTick override for dynamic tooltip
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            return;
        }

        // 1.20.1 Check logic
        if (!stack.hasNbt() || !stack.getNbt().contains(ModNbtKeys.LINKED_HAMSTER_UUID)) {
            return;
        }
        UUID linkedUuid = stack.getNbt().getUuid(ModNbtKeys.LINKED_HAMSTER_UUID);

        if (entity instanceof PlayerEntity) {
            // Search nearby entities for the linked hamster
            world.getEntitiesByClass(HamsterEntity.class, entity.getBoundingBox().expand(16), e -> e.getUuid().equals(linkedUuid))
                    .stream().findFirst().ifPresent(hamster -> {
                        Text newName;
                        if (hamster.hasCustomName()) {
                            newName = hamster.getName();
                        } else {
                            newName = hamster.getDisplayName().copy().append(" " + hamster.getId());
                        }

                        // 1.20.1 Check and update logic
                        String currentJson = stack.getNbt().getString(ModNbtKeys.LINKED_HAMSTER_NAME);
                        String newJson = Text.Serializer.toJson(newName);

                        if (!currentJson.equals(newJson)) {
                            stack.getOrCreateNbt().putString(ModNbtKeys.LINKED_HAMSTER_NAME, newJson);
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
    public void createRenderer(Consumer<Object> consumer) {
        // On 1.20.1, delegating to the separate client-only class prevents ClassNotFound errors on the server
        if (Platform.getEnvironment() == Env.CLIENT) {
            consumer.accept(net.dawson.adorablehamsterpets.item.client.HamsterBedRenderProvider.create());
        }
    }
}