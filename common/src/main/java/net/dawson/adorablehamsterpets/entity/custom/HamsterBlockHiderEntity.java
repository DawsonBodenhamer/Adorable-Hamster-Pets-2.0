package net.dawson.adorablehamsterpets.entity.custom;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.UUIDUtil;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.dawson.adorablehamsterpets.client.render.BlockJiggleManager;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.MinigameUtil;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class HamsterBlockHiderEntity extends HamsterAbstractHiddenEntity {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int VALIDATION_INTERVAL = 20;

    // --- Feedback Hint Tuning (ticks) ---
    private static final float AMBIENT_START_INTERVAL = 20.0f;      // Starting interval. Higher = less frequent at start
    private static final float AMBIENT_END_INTERVAL = 2.0f;         // Ending interval. Lower = more frequent nearing end
    private static final float BREADCRUMB_START_INTERVAL = 12.0f;   // Starting interval. Higher = wider gaps at start
    private static final float BREADCRUMB_END_INTERVAL = 2.0f;      // Ending interval. Lower = more dense nearing end
    private static final float JIGGLE_START_INTERVAL = 200.0f;      // Starting base jiggle interval (10 sec)
    private static final float JIGGLE_END_INTERVAL = 40.0f;         // Ending base jiggle interval (2 sec)
    private static final int JIGGLE_INTERVAL_VARIANCE = 30;         // Random timing variance (+ or - 1.5 sec)

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private int hideTimer;
    private int maxHideDuration;
    private int validationTimer;
    private int jiggleTimer;
    private int ambientHintTimer;
    private int breadcrumbTimer;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterBlockHiderEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public void initializeHiding(BlockPos targetBlock, int durationTicks, CompoundTag originalHamsterNbt) {
        this.hamsterNbt = originalHamsterNbt;
        this.anchorPos = targetBlock;
        this.hideTimer = durationTicks;
        this.maxHideDuration = durationTicks;
        this.validationTimer = VALIDATION_INTERVAL;
        this.jiggleTimer = this.random.nextIntBetweenInclusive(5, 15); // First jiggle gets random delay
        this.ambientHintTimer = 14;
        this.breadcrumbTimer = 12;
        this.setPos(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5);

        registerOccupancy();
    }

    public void finishHiding(boolean success, @Nullable Player finder) {
        if (this.level().isClientSide()) return;
        ServerLevel serverWorld = (ServerLevel) this.level();

        HamsterEntity newHamster = super.popOut(success);

        if (newHamster != null) {
            newHamster.hideAndSeekCooldownEndTick = serverWorld.getGameTime() + (Configs.AHP_MAIN.hideAndSeekCooldownSeconds.get() * 20L);

            if (!success) {
                // Failure: Timer expired or block broken
                MinigameUtil.executeSulkFailure(newHamster, Vec3.atCenterOf(this.anchorPos));
            }

            // Visuals & Audio
            BlockState state = serverWorld.getBlockState(this.anchorPos);
            SoundEvent dynamicSound = ModSounds.getDynamicBlockSound(state);
            serverWorld.playSound(null, this.anchorPos, dynamicSound, SoundSource.NEUTRAL, 1.8F, 1.2F);
            BlockPos popPos = this.anchorPos;
            newHamster.scheduleTask(serverWorld.getGameTime() + 4, "hide_and_seek_pop", () -> { // 4 tick delay for pop sound
                if (newHamster.isAlive()) {
                    serverWorld.playSound(null, popPos, ModSounds.HAMSTER_POP.get(), SoundSource.NEUTRAL, 0.25F, 1.0F);
                }
            });

            ParticleEffectsUtil.spawnParticles(
                    serverWorld,
                    Vec3.atCenterOf(this.anchorPos),
                    ParticleTypes.POOF,
                    25,
                    new Vec3(0.3, 0.3, 0.3),
                    0.05
            );

            // Spawn contextual block particles
            ParticleEffectsUtil.spawnParticles(
                    serverWorld,
                    Vec3.atCenterOf(this.anchorPos),
                    MiscUtil.BlockStateUtil.getHidingSpotParticle(state),
                    30,
                    new Vec3(0.4, 0.4, 0.4),
                    0.0
            );

            serverWorld.addFreshEntityWithPassengers(newHamster);

            if (success) {
                // Success: Player found hamster
                if (finder instanceof ServerPlayer serverPlayer) {
                    ModCriteria.HIDE_AND_SEEK_FOUND.get().trigger(serverPlayer);
                }

                Item giftItem = MinigameUtil.getRandomMiniGameReward(newHamster);
                if (giftItem != Items.AIR) {
                    newHamster.setFrozenMovement(true);
                    newHamster.setCelebrationTicks(10);

                    // Delay slightly to ensure client rendering entity before sending animation packet
                    newHamster.scheduleTask(serverWorld.getGameTime() + 5, "start_gift_delivery", () -> {
                        MinigameUtil.executeGiftDeliverySequence(newHamster, new ItemStack(giftItem), finder);
                    });
                }

                SoundEvent sparkleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random);
                if (sparkleSound != null) {
                    serverWorld.playSound(null, newHamster.blockPosition(), sparkleSound, SoundSource.NEUTRAL, 0.5F, 1.0F);
                }
            }
        }

        // Trigger exit jiggle on client
        serverWorld.broadcastEntityEvent(this, (byte) 60);
        this.discard();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 60) {
            // Trigger client side matrix deformation manually
            long posLong = this.blockPosition().asLong();
            long seed = (posLong ^ (this.getId() * 0x9E3779B97F4A7C15L));
            BlockJiggleManager.INSTANCE.startJiggle(posLong, this.level().getGameTime(), seed, BlockJiggleManager.HIDE_AND_SEEK_JIGGLE);
        } else {
            super.handleEntityEvent(status);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;

        if (!this.isRegistered && this.anchorPos != null) {
            registerOccupancy();
        }

        if (--this.validationTimer <= 0) {
            this.validationTimer = VALIDATION_INTERVAL;
            if (!isBlockValid()) {
                finishHiding(false, null);
                return;
            }
        }

        // Effects increase in intensity over time (0.0 = start, 1.0 = end)
        float progress = this.maxHideDuration > 0 ? 1.0f - ((float) this.hideTimer / this.maxHideDuration) : 0.0f;

        // --- Ambient Feedback ---
        if (--this.ambientHintTimer <= 0) {
            this.ambientHintTimer = (int) Mth.lerp(progress, AMBIENT_START_INTERVAL, AMBIENT_END_INTERVAL);
            MinigameUtil.executeOngoingBlockLocationHint(
                    this,
                    ModParticles.PIXIE_DUST.get(PixieDustParticleTheme.GOLD).get(),
                    2,
                    0.7,
                    0.7,
                    0.0,
                    0.7
            );
        }

        // --- Breadcrumbs Trail ---
        if (--this.breadcrumbTimer <= 0) {
            this.breadcrumbTimer = (int) Mth.lerp(progress, BREADCRUMB_START_INTERVAL, BREADCRUMB_END_INTERVAL);

            if (this.hamsterNbt != null && this.hamsterNbt.read("Owner", UUIDUtil.CODEC).isPresent()) {
                Player owner = this.level().getPlayerByUUID(this.hamsterNbt.read("Owner", UUIDUtil.CODEC).orElse(null));
                MinigameUtil.executeBreadcrumbHint(
                        this,
                        owner,
                        625.0,
                        0.35,
                        0.65,
                        ModParticles.PIXIE_DUST.get(PixieDustParticleTheme.GOLD).get(),
                        1,
                        new Vec3(0.05, 0.05, 0.05),
                        0.0
                );
            }
        }

        // --- Periodic Feedback ---
        if (--this.jiggleTimer <= 0) {
            int baseInterval = (int) Mth.lerp(progress, JIGGLE_START_INTERVAL, JIGGLE_END_INTERVAL);
            this.jiggleTimer = baseInterval + this.random.nextIntBetweenInclusive(-JIGGLE_INTERVAL_VARIANCE, JIGGLE_INTERVAL_VARIANCE);

            if (this.anchorPos != null) {
                BlockState state = this.level().getBlockState(this.anchorPos);
                SoundEvent sound = ModSounds.getDynamicBlockSound(state);

                MinigameUtil.executePeriodicBlockLocationHint(
                        this,
                        this.anchorPos,
                        sound,
                        0.6F,
                        1.2F,
                        MiscUtil.BlockStateUtil.getHidingSpotParticle(state),
                        15,
                        new Vec3(0.3, 0.3, 0.3),
                        0.2
                );
            }
        }

        if (--this.hideTimer <= 0) {
            finishHiding(false, null);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("HideTimer", this.hideTimer);
        nbt.putInt("MaxHideDuration", this.maxHideDuration);
        nbt.putInt("AmbientHintTimer", this.ambientHintTimer);
        nbt.putInt("BreadcrumbTimer", this.breadcrumbTimer);
        out.store("AdorableHamsterPets.BlockHider", CompoundTag.CODEC, nbt);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        CompoundTag nbt = in.read("AdorableHamsterPets.BlockHider", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        this.hideTimer = nbt.getIntOr("HideTimer", 0);
        this.maxHideDuration = nbt.contains("MaxHideDuration") ? nbt.getIntOr("MaxHideDuration", 0) : Math.max(1, this.hideTimer);
        this.validationTimer = VALIDATION_INTERVAL;

        float progress = this.maxHideDuration > 0 ? 1.0f - ((float) this.hideTimer / this.maxHideDuration) : 0.0f;
        int baseInterval = (int) Mth.lerp(progress, 220.0f, 40.0f);
        this.jiggleTimer = baseInterval + this.random.nextIntBetweenInclusive(-30, 30);

        this.ambientHintTimer = nbt.contains("AmbientHintTimer") ? nbt.getIntOr("AmbientHintTimer", 0) : 14;
        this.breadcrumbTimer = nbt.contains("BreadcrumbTimer") ? nbt.getIntOr("BreadcrumbTimer", 0) : 12;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean isBlockValid() {
        if (this.anchorPos == null) return false;
        BlockState state = this.level().getBlockState(this.anchorPos);

        if (ConfigDataCache.isHideAndSeekBlacklisted(state)) return false;
        if (ConfigDataCache.isHideAndSeekBlock(state)) return true;
        if (Configs.AHP_MAIN.allowInventoryHiding && this.level().getBlockEntity(this.anchorPos) instanceof Container) return true;

        return false;
    }
}