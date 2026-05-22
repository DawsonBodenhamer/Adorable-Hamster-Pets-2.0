package net.dawson.adorablehamsterpets.block.entity;

import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterBedUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    private Optional<Text> linkedHamsterName = Optional.empty();
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
            BlockState blockState = state.getAnimatable().getCachedState();
            if (blockState.get(HamsterBedBlock.OCCUPIED)) {
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

    public Optional<Text> getLinkedHamsterName() {
        return this.linkedHamsterName;
    }

    public boolean isWanderModeActive() {
        if (world instanceof ServerWorld serverWorld && linkedHamsterUuid.isPresent()) {
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
        markDirty();
        // Force a block update to sync with client immediately
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
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
        markDirty();
    }

    public void applyRepellentEffect() {
        this.setAllowSleep(false);
    }


    public HamsterBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), pos, state);
    }

    public void setLinkedHamster(UUID uuid, Text name, WanderDistance distance) {
        this.linkedHamsterUuid = Optional.of(uuid);
        this.linkedHamsterName = Optional.of(name);
        this.wanderDistance = distance;
        markDirty();
    }

    public WanderDistance getWanderDistance() {
        return this.wanderDistance;
    }

    public void toggleWanderMode(PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld && linkedHamsterUuid.isPresent()) {
            Entity entity = serverWorld.getEntity(linkedHamsterUuid.get());
            if (entity instanceof HamsterEntity hamster) {
                boolean newMode = !hamster.isWanderModeActive();
                hamster.setWanderModeActive(newMode);

                // If disabling wander mode while hamster is in bed, wake it up
                if (!newMode && getCachedState().get(HamsterBedBlock.OCCUPIED) && hamster.isSleeping()) {
                    HamsterBedUtil.wakeUpFromBed(hamster, true); // Manual wakeup
                }

                Text status = newMode ? Text.literal("ENABLED") : Text.literal("DISABLED");
                player.sendMessage(Text.translatable("message.adorablehamsterpets.wander_mode_set", hamster.getName(), status), true);
                world.playSound(null, getPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.BLOCKS, 0.5f, newMode ? 1.2f : 0.8f);
            }
        }
    }

    public void cycleWanderDistance(PlayerEntity player) {
        WanderDistance[] values = WanderDistance.values();
        this.wanderDistance = values[(this.wanderDistance.ordinal() + 1) % values.length];
        markDirty();
        if (linkedHamsterName.isPresent()) {
            player.sendMessage(Text.translatable("message.adorablehamsterpets.wander_distance_set", linkedHamsterName.get(), this.wanderDistance.asString()), true);
            world.playSound(null, getPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.BLOCKS, 0.5f, 1.0f);
        }
    }

    public boolean lureHamsterToBed(PlayerEntity player, ItemStack lureItem) {
        if (world instanceof ServerWorld serverWorld && linkedHamsterUuid.isPresent()) {
            Entity entity = serverWorld.getEntity(linkedHamsterUuid.get());
            if (entity instanceof HamsterEntity hamster) {
                // Re-enable wander mode for this specific hamster/bed pair
                if (!hamster.isWanderModeActive()) {
                    hamster.setWanderModeActive(true);
                }

                if (hamster.isSitting() || hamster.isSleeping()) {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.lure_to_bed_fail").formatted(Formatting.RED), true);
                    return false;
                }

                hamster.lureToBed();

                // Feedback
                SoundEvent lureSound = ModSounds.getDynamicItemSound(lureItem);
                float volume = ModSounds.getDynamicSoundVolume(lureSound);
                world.playSound(null, getPos(), lureSound, SoundCategory.BLOCKS, volume, 1.0f);
                ParticleEffectsUtil.spawnParticles(
                        world,
                        getPos(),
                        0.7,
                        new ItemStackParticleEffect(ParticleTypes.ITEM, lureItem),
                        8,
                        0.25, 0.25, 0.25, 0.05
                );

                return true;
            }
        }
        return false;
    }

    public void unlinkHamster(PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld && linkedHamsterUuid.isPresent()) {
            UUID uuidToUnlink = linkedHamsterUuid.get();
            Text hamsterNameToUnlink = linkedHamsterName.orElse(Text.literal("A hamster"));

            // Wake up the hamster if it's sleeping in the bed
            if (getCachedState().get(HamsterBedBlock.OCCUPIED)) {
                Entity entity = serverWorld.getEntity(uuidToUnlink);
                if (entity instanceof HamsterEntity hamster && hamster.isSleeping()) {
                    HamsterBedUtil.wakeUpFromBed(hamster, true); // Manual wakeup
                }
            }

            // Clear data on the block entity
            this.linkedHamsterUuid = Optional.empty();
            this.linkedHamsterName = Optional.empty();
            this.wanderDistance = Configs.AHP.defaultWanderDistance.get(); // Reset to default
            markDirty();

            // Update the hamster entity
            Entity entity = serverWorld.getEntity(uuidToUnlink);
            if (entity instanceof HamsterEntity hamster) {
                hamster.setWanderModeActive(false);
                hamster.setLinkedBedPos(Optional.empty());
            }

            // Feedback
            player.sendMessage(Text.translatable("message.adorablehamsterpets.bed_unlinked", hamsterNameToUnlink).formatted(Formatting.YELLOW), true);
            world.playSound(null, getPos(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        linkedHamsterUuid.ifPresent(uuid -> nbt.putUuid("LinkedHamsterUuid", uuid));
        linkedHamsterName.ifPresent(name -> nbt.putString("LinkedHamsterName", Text.Serialization.toJsonString(name, registryLookup)));
        nbt.putString("WanderDistance", wanderDistance.asString());
        nbt.putBoolean("IsNewlyPlaced", this.isNewlyPlaced);
        nbt.putBoolean("RespawnEnabled", this.respawnEnabled);
        nbt.putBoolean("AllowSleep", this.allowSleep);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.containsUuid("LinkedHamsterUuid")) {
            this.linkedHamsterUuid = Optional.of(nbt.getUuid("LinkedHamsterUuid"));
        } else {
            this.linkedHamsterUuid = Optional.empty();
        }

        if (nbt.contains("LinkedHamsterName")) {
            this.linkedHamsterName = Optional.ofNullable(Text.Serialization.fromJson(nbt.getString("LinkedHamsterName"), registryLookup));
        } else {
            this.linkedHamsterName = Optional.empty();
        }

        String distanceStr = nbt.getString("WanderDistance").toUpperCase(Locale.ROOT);
        try {
            this.wanderDistance = distanceStr.isEmpty() ? Configs.AHP.defaultWanderDistance.get() : WanderDistance.valueOf(distanceStr);
        } catch (IllegalArgumentException e) {
            this.wanderDistance = Configs.AHP.defaultWanderDistance.get();
        }

        this.isNewlyPlaced = nbt.contains("IsNewlyPlaced") ? nbt.getBoolean("IsNewlyPlaced") : false;
        this.allowSleep = !nbt.contains("AllowSleep") || nbt.getBoolean("AllowSleep");
        this.respawnEnabled = nbt.getBoolean("RespawnEnabled");

        // Force sleep to false if bed is upside down
        if (this.getCachedState().contains(HamsterBedBlock.UPSIDE_DOWN) && this.getCachedState().get(HamsterBedBlock.UPSIDE_DOWN)) {
            this.allowSleep = false;
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, HamsterBedBlockEntity be) {
        if (be.failSoundTimer > 0) {
            be.failSoundTimer--;

            // Note: 5 ticks apart.
            // Start at 6.
            // Tick 6 -> 5: Play First Sound
            // Tick 1 -> 0: Play Second Sound

            if (be.failSoundTimer == 6) {
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            } else if (be.failSoundTimer == 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 0.2f);
            }
        }
    }
}