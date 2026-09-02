package net.dawson.adorablehamsterpets.block.entity;

import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterBedUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class HamsterBedBlockEntity extends BlockEntity implements GeoBlockEntity {

    // --- Fields ---
    private Optional<UUID> linkedHamsterUuid = Optional.empty();
    private Optional<Component> linkedHamsterName = Optional.empty();
    private WanderDistance wanderDistance = WanderDistance.MEDIUM;
    private boolean isNewlyPlaced = true;
    private boolean allowSleep = true;
    private boolean respawnEnabled = false;
    private int failSoundTimer = 0;

    // --- Geckolib Stuff ---
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        AnimationController<HamsterBedBlockEntity> controller = new AnimationController<>(this, "hamster_bed_controller", 5, state -> {
            BlockState blockState = state.getAnimatable().getBlockState();
            if (blockState.getValue(HamsterBedBlock.OCCUPIED)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("anim_bed_idle_waving_occupied"));
            } else {
                return state.setAndContinue(RawAnimation.begin().thenLoop("anim_bed_idle_waving_unoccupied"));
            }
        });

        // Register one-shot animations
        controller.triggerableAnim("anim_bed_being_placed", RawAnimation.begin().thenPlay("anim_bed_being_placed"));
        controller.triggerableAnim("anim_bed_becoming_occupied", RawAnimation.begin().thenPlay("anim_bed_becoming_occupied"));
        controller.triggerableAnim("anim_bed_becoming_unoccupied", RawAnimation.begin().thenPlay("anim_bed_becoming_unoccupied"));
        controller.triggerableAnim("anim_bed_interact_occupied", RawAnimation.begin().thenPlay("anim_bed_interact_occupied"));
        controller.triggerableAnim("anim_bed_interact_unoccupied", RawAnimation.begin().thenPlay("anim_bed_interact_unoccupied"));

        registrar.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // --- Jade HUD info ---
    public Optional<UUID> getLinkedHamsterUuid() {
        return this.linkedHamsterUuid;
    }

    public Optional<Component> getLinkedHamsterName() {
        return this.linkedHamsterName;
    }

    public boolean isWanderModeActive() {
        if (level instanceof ServerLevel serverWorld && linkedHamsterUuid.isPresent()) {
            Entity entity = serverWorld.getEntity(linkedHamsterUuid.get());
            if (entity instanceof HamsterEntity hamster) {
                return hamster.isWanderModeActive();
            }
        }
        return false; // Default to false if hamster not found or on client
    }

    // --- Everything Else ---
    public boolean isRespawnEnabled() {
        return this.respawnEnabled;
    }

    public void setRespawnEnabled(boolean enabled) {
        this.respawnEnabled = enabled;
        setChanged();
        // Force a block update to sync with client immediately
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void triggerFailSound() {
        this.failSoundTimer = 7; // Start sequence
    }

    public boolean isNewlyPlaced() {
        return this.isNewlyPlaced;
    }

    public void markAsUsed() {
        this.isNewlyPlaced = false;
    }

    public boolean isSleepingAllowed() {
        return this.allowSleep;
    }

    public void setAllowSleep(boolean allow) {
        this.allowSleep = allow;
        setChanged();
    }

    public void applyRepellentEffect() {
        this.setAllowSleep(false);
    }


    public HamsterBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), pos, state);
    }

    public void setLinkedHamster(UUID uuid, Component name, WanderDistance distance) {
        this.linkedHamsterUuid = Optional.of(uuid);
        this.linkedHamsterName = Optional.of(name);
        this.wanderDistance = distance;
        setChanged();
    }

    public WanderDistance getWanderDistance() {
        return this.wanderDistance;
    }

    public void toggleWanderMode(Player player) {
        if (level instanceof ServerLevel serverWorld && linkedHamsterUuid.isPresent()) {
            Entity entity = serverWorld.getEntity(linkedHamsterUuid.get());
            if (entity instanceof HamsterEntity hamster) {
                boolean newMode = !hamster.isWanderModeActive();
                hamster.setWanderModeActive(newMode);

                // If disabling wander mode while hamster is in bed, wake it up
                if (!newMode && getBlockState().getValue(HamsterBedBlock.OCCUPIED) && hamster.isSleeping()) {
                    HamsterBedUtil.wakeUpFromBed(hamster, true); // Manual wakeup
                }

                Component status = newMode ? Component.literal("ENABLED") : Component.literal("DISABLED");
                player.displayClientMessage(Component.translatable("message.adorablehamsterpets.wander_mode_set", hamster.getName(), status), true);
                level.playSound(null, getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, newMode ? 1.2f : 0.8f);
            }
        }
    }

    public void cycleWanderDistance(Player player) {
        WanderDistance[] values = WanderDistance.values();
        this.wanderDistance = values[(this.wanderDistance.ordinal() + 1) % values.length];
        setChanged();
        if (linkedHamsterName.isPresent()) {
            player.displayClientMessage(Component.translatable("message.adorablehamsterpets.wander_distance_set", linkedHamsterName.get(), this.wanderDistance.getSerializedName()), true);
            level.playSound(null, getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.0f);
        }
    }

    public boolean lureHamsterToBed(Player player, ItemStack lureItem) {
        if (level instanceof ServerLevel serverWorld && linkedHamsterUuid.isPresent()) {
            Entity entity = serverWorld.getEntity(linkedHamsterUuid.get());
            if (entity instanceof HamsterEntity hamster) {
                // Re-enable wander mode for this specific hamster/bed pair
                if (!hamster.isWanderModeActive()) {
                    hamster.setWanderModeActive(true);
                }

                if (hamster.isOrderedToSit() || hamster.isSleeping()) {
                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.lure_to_bed_fail").withStyle(ChatFormatting.RED), true);
                    return false;
                }

                hamster.lureToBed();

                // Feedback
                SoundEvent lureSound = ModSounds.getDynamicItemSound(lureItem);
                float volume = ModSounds.getDynamicSoundVolume(lureSound);
                level.playSound(null, getBlockPos(), lureSound, SoundSource.BLOCKS, volume, 1.0f);
                ParticleEffectsUtil.spawnParticles(
                        level,
                        getBlockPos(),
                        0.7,
                        new ItemParticleOption(ParticleTypes.ITEM, lureItem),
                        8,
                        0.25, 0.25, 0.25, 0.05
                );

                return true;
            }
        }
        return false;
    }

    public void unlinkHamster(Player player) {
        if (level instanceof ServerLevel serverWorld && linkedHamsterUuid.isPresent()) {
            UUID uuidToUnlink = linkedHamsterUuid.get();
            Component hamsterNameToUnlink = linkedHamsterName.orElse(Component.literal("A hamster"));

            // Wake up the hamster if it's sleeping in the bed
            if (getBlockState().getValue(HamsterBedBlock.OCCUPIED)) {
                Entity entity = serverWorld.getEntity(uuidToUnlink);
                if (entity instanceof HamsterEntity hamster && hamster.isSleeping()) {
                    HamsterBedUtil.wakeUpFromBed(hamster, true); // Manual wakeup
                }
            }

            // Clear data on the block entity
            this.linkedHamsterUuid = Optional.empty();
            this.linkedHamsterName = Optional.empty();
            this.wanderDistance = Configs.AHP_MAIN.defaultWanderDistance.get(); // Reset to default
            setChanged();

            // Update the hamster entity
            Entity entity = serverWorld.getEntity(uuidToUnlink);
            if (entity instanceof HamsterEntity hamster) {
                hamster.setWanderModeActive(false);
                hamster.setLinkedBedPos(Optional.empty());
            }

            // Feedback
            player.displayClientMessage(Component.translatable("message.adorablehamsterpets.bed_unlinked", hamsterNameToUnlink).withStyle(ChatFormatting.YELLOW), true);
            level.playSound(null, getBlockPos(), SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 1.2f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        linkedHamsterUuid.ifPresent(uuid -> nbt.putUUID("LinkedHamsterUuid", uuid));
        linkedHamsterName.ifPresent(name -> nbt.putString("LinkedHamsterName", Component.Serializer.toJson(name, registryLookup)));
        nbt.putString("WanderDistance", wanderDistance.getSerializedName());
        nbt.putBoolean("IsNewlyPlaced", this.isNewlyPlaced);
        nbt.putBoolean("RespawnEnabled", this.respawnEnabled);
        nbt.putBoolean("AllowSleep", this.allowSleep);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        if (nbt.hasUUID("LinkedHamsterUuid")) {
            this.linkedHamsterUuid = Optional.of(nbt.getUUID("LinkedHamsterUuid"));
        } else {
            this.linkedHamsterUuid = Optional.empty();
        }

        if (nbt.contains("LinkedHamsterName")) {
            this.linkedHamsterName = Optional.ofNullable(Component.Serializer.fromJson(nbt.getString("LinkedHamsterName"), registryLookup));
        } else {
            this.linkedHamsterName = Optional.empty();
        }

        String distanceStr = nbt.getString("WanderDistance").toUpperCase(Locale.ROOT);
        try {
            this.wanderDistance = distanceStr.isEmpty() ? Configs.AHP_MAIN.defaultWanderDistance.get() : WanderDistance.valueOf(distanceStr);
        } catch (IllegalArgumentException e) {
            this.wanderDistance = Configs.AHP_MAIN.defaultWanderDistance.get();
        }

        this.isNewlyPlaced = nbt.contains("IsNewlyPlaced") ? nbt.getBoolean("IsNewlyPlaced") : false;
        this.allowSleep = !nbt.contains("AllowSleep") || nbt.getBoolean("AllowSleep");
        this.respawnEnabled = nbt.getBoolean("RespawnEnabled");

        // Force sleep to false if bed is upside down
        if (this.getBlockState().hasProperty(HamsterBedBlock.UPSIDE_DOWN) && this.getBlockState().getValue(HamsterBedBlock.UPSIDE_DOWN)) {
            this.allowSleep = false;
        }
    }

    public static void tick(Level world, BlockPos pos, BlockState state, HamsterBedBlockEntity be) {
        if (be.failSoundTimer > 0) {
            be.failSoundTimer--;

            // Note: 5 ticks apart.
            // Start at 6.
            // Tick 6 -> 5: Play First Sound
            // Tick 1 -> 0: Play Second Sound

            if (be.failSoundTimer == 6) {
                world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
            } else if (be.failSoundTimer == 0) {
                world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0f, 0.2f);
            }
        }
    }
}