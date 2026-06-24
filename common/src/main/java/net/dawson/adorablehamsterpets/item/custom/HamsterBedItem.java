package net.dawson.adorablehamsterpets.item.custom;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.AhpMainConfig;
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
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HamsterBedItem extends BlockItem implements GeoItem {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Map<UUID, Long> UNLINKED_WARNING_COOLDOWNS = new ConcurrentHashMap<>();

    /**
     * Platform-agnostic factory method.
     * On Fabric: Returns new HamsterBedItem()
     * On Forge: Returns new HamsterBedItem() { @Override initializeClient... }
     */
    @ExpectPlatform
    public static HamsterBedItem create(Block block, WoodVariant variant, Settings settings) {
        throw new AssertionError();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final WoodVariant variant;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterBedItem(Block block, WoodVariant variant, Settings settings) {
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

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Event Handlers
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            return;
        }

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

                        String currentJson = stack.getNbt().getString(ModNbtKeys.LINKED_HAMSTER_NAME);
                        String newJson = Text.Serializer.toJson(newName);

                        if (!currentJson.equals(newJson)) {
                            stack.getOrCreateNbt().putString(ModNbtKeys.LINKED_HAMSTER_NAME, newJson);
                        }
                    });
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    // On 1.20.1, initialize RenderProvider and implement getter
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
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();

        if (!stack.hasNbt() || !stack.getNbt().contains(ModNbtKeys.LINKED_HAMSTER_UUID)) {
            PlayerEntity player = context.getPlayer();
            World world = context.getWorld();

            if (player != null) {
                AhpMainConfig config = Configs.AHP_MAIN;

                // 1. Check Master Toggle
                if (!config.warnOnUnlinkedBedPlacement) {
                    return super.useOnBlock(context);
                }

                String username = player.getGameProfile().getName();

                // 2. Check if player has already seen warning and waited
                if (config.playersWhoHaveSeenUnlinkedBedWarning.contains(username)) {
                    return super.useOnBlock(context);
                }

                // 3. Client hand off authority to Server
                if (world.isClient()) {
                    return ActionResult.SUCCESS; // Swing arm, send packet to server, do not place
                }

                UUID uuid = player.getUuid();
                long currentTime = world.getTime();

                // 3. Process Cooldown
                if (UNLINKED_WARNING_COOLDOWNS.containsKey(uuid)) {
                    long warningTime = UNLINKED_WARNING_COOLDOWNS.get(uuid);

                    if (currentTime - warningTime >= 40) { // 2 seconds
                        config.playersWhoHaveSeenUnlinkedBedWarning.add(username);
                        config.save();
                        UNLINKED_WARNING_COOLDOWNS.remove(uuid);
                        return super.useOnBlock(context); // Place block
                    } else {
                        // Still on cooldown. Silently fail
                        return ActionResult.FAIL;
                    }
                } else {
                    // 4. First time trying. Show warning, start cooldown
                    UNLINKED_WARNING_COOLDOWNS.put(uuid, currentTime);

                    world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.2f, 0.5f);

                    MutableText msg = Text.literal("\n").append(Text.translatable("message.adorablehamsterpets.unlinked_bed_placement.1").formatted(Formatting.RED, Formatting.BOLD));
                    msg.append("\n\n").append(Text.translatable("message.adorablehamsterpets.unlinked_bed_placement.2").formatted(Formatting.GRAY));
                    msg.append("\n");

                    player.sendMessage(msg, false);

                    return ActionResult.SUCCESS; // Consume action to prevent placement
                }
            }

            // If placed by a dispenser or some other non-player entity, just fail
            return ActionResult.FAIL;
        }

        return super.useOnBlock(context);
    }

    @Override
    protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
        if (!world.isClient) {
            // Sound and particle logic
            SoundEvent rustleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BED_LEAVES_RUSTLE_SOUNDS, world.getRandom());
            if (rustleSound != null) {
                world.playSound(null, pos, rustleSound, SoundCategory.BLOCKS, 0.5f, 1.5f);
            }

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof HamsterBedBlockEntity bedEntity) {
                bedEntity.triggerAnim("hamster_bed_controller", "anim_bed_being_placed");

                // Spawn particles with the wood variant
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
        // Set block state with correct wood variant after placement
        return world.setBlockState(pos, state.with(HamsterBedBlock.WOOD_VARIANT, this.variant), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        WoodVariant stackVariant = this.variant;
        if (stack.hasNbt() && stack.getNbt().contains(ModNbtKeys.WOOD_VARIANT)) {
            try {
                stackVariant = WoodVariant.valueOf(stack.getNbt().getString(ModNbtKeys.WOOD_VARIANT));
            } catch (IllegalArgumentException ignored) {}
        }
        if (Configs.AHP_UI.enableItemTooltips) {
            if (Screen.hasShiftDown()) {
                // --- Expanded Tooltip ---

                // Main hints
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.description1").formatted(Formatting.GOLD));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.description2").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls1").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.wander_controls2").formatted(Formatting.GRAY));

                // Dynamic interaction hints
                Text lureName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.lureItems).copy().formatted(Formatting.GOLD, Formatting.BOLD);
                Text repellentName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_ITEMS.bedAvoidanceFoods).copy().formatted(Formatting.RED, Formatting.BOLD);
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.lure_hint", lureName).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.repellent_hint", repellentName).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.jade.unlink_hint", repellentName).formatted(Formatting.GRAY));

                // Respawn status and hint
                boolean configEnabled = Configs.AHP_MAIN.enableRespawnInBed.get();
                boolean freeRespawns = Configs.AHP_MAIN.freeBedRespawns.get();

                Text statusText;
                Text hintText;

                if (!configEnabled) {
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.disabled_config");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.disabled_config");
                } else if (freeRespawns) {
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.active");
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.active_free");
                } else {
                    // Inventory items always inactive regarding respawn state
                    statusText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.inactive");
                    Text tributeName = ConfigDataCache.getFirstItemNameFromList(Configs.AHP_MAIN.resurrectionTributes);
                    hintText = Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_hint.inactive", tributeName.copy().formatted(Formatting.GOLD, Formatting.BOLD));
                }

                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.respawn_status.label", statusText));
                tooltip.add(hintText);

                // Conditional linked info
                if (stack.hasNbt()) {
                    NbtCompound nbt = stack.getNbt();
                    if (nbt.contains(ModNbtKeys.LINKED_HAMSTER_UUID) && nbt.contains(ModNbtKeys.LINKED_HAMSTER_NAME)) {

                        Text hamsterName = Text.Serializer.fromJson(nbt.getString(ModNbtKeys.LINKED_HAMSTER_NAME));

                        // Blank line for spacing
                        tooltip.add(Text.literal(""));
                        if (hamsterName != null) {
                            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.linked_to", hamsterName).formatted(Formatting.GREEN));
                        }

                        if (nbt.contains(ModNbtKeys.WANDER_DISTANCE)) {
                            try {
                                WanderDistance wanderDistance = WanderDistance.valueOf(nbt.getString(ModNbtKeys.WANDER_DISTANCE));
                                int radius = switch (wanderDistance) {
                                    case NEAR -> Configs.AHP_MAIN.wanderDistanceNear.get();
                                    case FAR -> Configs.AHP_MAIN.wanderDistanceFar.get();
                                    default -> Configs.AHP_MAIN.wanderDistanceMedium.get();
                                };
                                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.wander_distance", Text.translatable(wanderDistance.translationKey()), radius).formatted(Formatting.AQUA));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
            } else {
                // Default condensed tooltip
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_bed.description1").formatted(Formatting.GOLD));
                tooltip.add(Text.translatable("tooltip.adorablehamsterpets.shift_for_info").formatted(Formatting.DARK_GRAY));
            }
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public String getTranslationKey() {
        // Forces item to use own unique translation key
        return this.getOrCreateTranslationKey();
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
    public void createRenderer(Consumer<Object> consumer) {
        // On 1.20.1, delegating to the separate client-only class prevents ClassNotFound errors on the server
        if (Platform.getEnvironment() == Env.CLIENT) {
            consumer.accept(net.dawson.adorablehamsterpets.item.client.HamsterBedRenderProvider.create());
        }
    }
}