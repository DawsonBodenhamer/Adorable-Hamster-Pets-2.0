package net.dawson.adorablehamsterpets.entity.custom;

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
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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

    public HamsterBlockHiderEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public void initializeHiding(BlockPos targetBlock, int durationTicks, NbtCompound originalHamsterNbt) {
        this.hamsterNbt = originalHamsterNbt;
        this.anchorPos = targetBlock;
        this.hideTimer = durationTicks;
        this.maxHideDuration = durationTicks;
        this.validationTimer = VALIDATION_INTERVAL;
        this.jiggleTimer = this.random.nextBetween(5, 15); // First jiggle gets random delay
        this.ambientHintTimer = 14;
        this.breadcrumbTimer = 12;
        this.setPosition(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5);

        registerOccupancy();
    }

    public void finishHiding(boolean success, @Nullable PlayerEntity finder) {
        if (this.getWorld().isClient) return;
        ServerWorld serverWorld = (ServerWorld) this.getWorld();

        HamsterEntity newHamster = super.popOut(success);

        if (newHamster != null) {
            newHamster.hideAndSeekCooldownEndTick = serverWorld.getTime() + (Configs.AHP_MAIN.hideAndSeekCooldownSeconds.get() * 20L);

            if (!success) {
                // Failure: Timer expired or block broken
                MinigameUtil.executeSulkFailure(newHamster, Vec3d.ofCenter(this.anchorPos));
            }

            // Visuals & Audio
            BlockState state = serverWorld.getBlockState(this.anchorPos);
            SoundEvent dynamicSound = ModSounds.getDynamicBlockSound(state);
            serverWorld.playSound(null, this.anchorPos, dynamicSound, SoundCategory.NEUTRAL, 1.8F, 1.2F);
            BlockPos popPos = this.anchorPos;
            newHamster.scheduleTask(serverWorld.getTime() + 4, "hide_and_seek_pop", () -> { // 4 tick delay for pop sound
                if (newHamster.isAlive()) {
                    serverWorld.playSound(null, popPos, ModSounds.HAMSTER_POP.get(), SoundCategory.NEUTRAL, 0.25F, 1.0F);
                }
            });

            ParticleEffectsUtil.spawnParticles(
                    serverWorld,
                    Vec3d.ofCenter(this.anchorPos),
                    ParticleTypes.POOF,
                    25,
                    new Vec3d(0.3, 0.3, 0.3),
                    0.05
            );

            // Spawn contextual block particles
            ParticleEffectsUtil.spawnParticles(
                    serverWorld,
                    Vec3d.ofCenter(this.anchorPos),
                    MiscUtil.BlockStateUtil.getHidingSpotParticle(state),
                    30,
                    new Vec3d(0.4, 0.4, 0.4),
                    0.0
            );

            serverWorld.spawnEntityAndPassengers(newHamster);

            if (success) {
                // Success: Player found hamster
                if (finder instanceof ServerPlayerEntity serverPlayer) {
                    ModCriteria.HIDE_AND_SEEK_FOUND.trigger(serverPlayer);
                }

                Item giftItem = MinigameUtil.getRandomMiniGameReward(newHamster);
                if (giftItem != Items.AIR) {
                    newHamster.setFrozenMovement(true);
                    newHamster.setCelebrationTicks(10);

                    // Delay slightly to ensure client rendering entity before sending animation packet
                    newHamster.scheduleTask(serverWorld.getTime() + 5, "start_gift_delivery", () -> {
                        MinigameUtil.executeGiftDeliverySequence(newHamster, new ItemStack(giftItem), finder);
                    });
                }

                SoundEvent sparkleSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.random);
                if (sparkleSound != null) {
                    serverWorld.playSound(null, newHamster.getBlockPos(), sparkleSound, SoundCategory.NEUTRAL, 0.5F, 1.0F);
                }
            }
        }

        // Trigger exit jiggle on client
        serverWorld.sendEntityStatus(this, (byte) 60);
        this.discard();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void handleStatus(byte status) {
        if (status == 60) {
            // Trigger client side matrix deformation manually
            long posLong = this.getBlockPos().asLong();
            long seed = (posLong ^ (this.getId() * 0x9E3779B97F4A7C15L));
            BlockJiggleManager.INSTANCE.startJiggle(posLong, this.getWorld().getTime(), seed, BlockJiggleManager.HIDE_AND_SEEK_JIGGLE);
        } else {
            super.handleStatus(status);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) return;

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
            this.ambientHintTimer = (int) MathHelper.lerp(progress, AMBIENT_START_INTERVAL, AMBIENT_END_INTERVAL);
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
            this.breadcrumbTimer = (int) MathHelper.lerp(progress, BREADCRUMB_START_INTERVAL, BREADCRUMB_END_INTERVAL);

            if (this.hamsterNbt != null && this.hamsterNbt.containsUuid("Owner")) {
                PlayerEntity owner = this.getWorld().getPlayerByUuid(this.hamsterNbt.getUuid("Owner"));
                MinigameUtil.executeBreadcrumbHint(
                        this,
                        owner,
                        625.0,
                        0.35,
                        0.65,
                        ModParticles.PIXIE_DUST.get(PixieDustParticleTheme.GOLD).get(),
                        1,
                        new Vec3d(0.05, 0.05, 0.05),
                        0.0
                );
            }
        }

        // --- Periodic Feedback ---
        if (--this.jiggleTimer <= 0) {
            int baseInterval = (int) MathHelper.lerp(progress, JIGGLE_START_INTERVAL, JIGGLE_END_INTERVAL);
            this.jiggleTimer = baseInterval + this.random.nextBetween(-JIGGLE_INTERVAL_VARIANCE, JIGGLE_INTERVAL_VARIANCE);

            if (this.anchorPos != null) {
                BlockState state = this.getWorld().getBlockState(this.anchorPos);
                SoundEvent sound = ModSounds.getDynamicBlockSound(state);

                MinigameUtil.executePeriodicBlockLocationHint(
                        this,
                        this.anchorPos,
                        sound,
                        0.6F,
                        1.2F,
                        MiscUtil.BlockStateUtil.getHidingSpotParticle(state),
                        15,
                        new Vec3d(0.3, 0.3, 0.3),
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
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("HideTimer", this.hideTimer);
        nbt.putInt("MaxHideDuration", this.maxHideDuration);
        nbt.putInt("AmbientHintTimer", this.ambientHintTimer);
        nbt.putInt("BreadcrumbTimer", this.breadcrumbTimer);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.hideTimer = nbt.getInt("HideTimer");
        this.maxHideDuration = nbt.contains("MaxHideDuration", NbtElement.INT_TYPE) ? nbt.getInt("MaxHideDuration") : Math.max(1, this.hideTimer);
        this.validationTimer = VALIDATION_INTERVAL;

        float progress = this.maxHideDuration > 0 ? 1.0f - ((float) this.hideTimer / this.maxHideDuration) : 0.0f;
        int baseInterval = (int) MathHelper.lerp(progress, 220.0f, 40.0f);
        this.jiggleTimer = baseInterval + this.random.nextBetween(-30, 30);

        this.ambientHintTimer = nbt.contains("AmbientHintTimer", NbtElement.INT_TYPE) ? nbt.getInt("AmbientHintTimer") : 14;
        this.breadcrumbTimer = nbt.contains("BreadcrumbTimer", NbtElement.INT_TYPE) ? nbt.getInt("BreadcrumbTimer") : 12;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean isBlockValid() {
        if (this.anchorPos == null) return false;
        BlockState state = this.getWorld().getBlockState(this.anchorPos);

        if (ConfigDataCache.isHideAndSeekBlacklisted(state)) return false;
        if (ConfigDataCache.isHideAndSeekBlock(state)) return true;
        if (Configs.AHP_MAIN.allowInventoryHiding && this.getWorld().getBlockEntity(this.anchorPos) instanceof Inventory) return true;

        return false;
    }
}
