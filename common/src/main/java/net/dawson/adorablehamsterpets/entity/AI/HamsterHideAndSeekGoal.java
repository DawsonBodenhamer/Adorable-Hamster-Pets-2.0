package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterAbstractHiddenEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterBlockHiderEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterPhysicsUtil;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class HamsterHideAndSeekGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants & Enums
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int SEARCH_RADIUS = 12;
    private static final int LUNGE_DURATION_TICKS = 5;
    private static final int MAX_MOVE_TIMEOUT = 200;
    private static final Set<BlockPos> TARGETED_BLOCKS = new HashSet<>();

    private enum State {
        MOVING_TO_BLOCK,
        POUNCING
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private final World world;

    private State currentState;
    private BlockPos targetBlock;
    private Vec3d pounceStartPos;

    private int lungeTicks = 0;
    private int moveTimeout = 0;
    private int checkTimer = 0;
    private boolean isFinished = false;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterHideAndSeekGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        if (this.world.isClient() || !Configs.AHP.enableHideAndSeek) return false;

        // Basic cooldown and state restrictions
        if (this.hamster.getWorld().getTime() < this.hamster.hideAndSeekCooldownEndTick) return false;

        if (!this.hamster.isTamed()
                || this.hamster.isSitting()
                || this.hamster.isFrozenMovement()
                || this.hamster.isSleeping()
                || this.hamster.isKnockedOut()
                || this.hamster.isSulking()
                || this.hamster.isHoldingMouthItem()
                || this.hamster.isCelebratingBaby()
                || this.hamster.isCelebratingDiamond()
                || this.hamster.isPlayingTag()
        ) {
            return false;
        }

        // Throttle expensive block scanning
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = this.getTickCount(20);

        // Scale denominator to match check frequency
        int denominator = Math.max(1, Configs.AHP.hideAndSeekChanceDenominator.get() / 20);
        if (this.hamster.getRandom().nextInt(denominator) != 0) {
            return false;
        }

        Optional<BlockPos> target = findHidingSpot();
        if (target.isPresent()) {
            this.targetBlock = target.get();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        if (this.isFinished) return false;

        if (this.targetBlock == null) return false;
        if (this.hamster.isSitting() || this.hamster.isKnockedOut()) return false;

        // Check block validity continuously
        BlockState state = this.world.getBlockState(this.targetBlock);
        boolean isValid = ConfigDataCache.isHideAndSeekBlock(state) ||
                (Configs.AHP.allowInventoryHiding && this.world.getBlockEntity(this.targetBlock) instanceof Inventory);

        return isValid;
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
        this.hamster.setHiding(true);

        // Call dibs on block
        TARGETED_BLOCKS.add(this.targetBlock);

        this.currentState = State.MOVING_TO_BLOCK;
        this.moveTimeout = 0;
        this.isFinished = false;

        Path path = this.hamster.getNavigation().findPathTo(this.targetBlock, 1);
        if (path != null) {
            this.hamster.getNavigation().startMovingAlong(path, 1.5D);
        } else {
            this.isFinished = true;
        }
    }

    @Override
    public void stop() {
        this.hamster.getNavigation().stop();
        this.hamster.setHiding(false);

        // Determine if game stopped naturally (player/event interruption) or if block was broken
        boolean blockIsValid = false;
        if (this.targetBlock != null) {
            BlockState state = this.world.getBlockState(this.targetBlock);
            blockIsValid = ConfigDataCache.isHideAndSeekBlock(state) ||
                    (Configs.AHP.allowInventoryHiding && this.world.getBlockEntity(this.targetBlock) instanceof Inventory);
        }

        // Only apply cooldown if block was valid
        if (blockIsValid) {
            this.hamster.hideAndSeekCooldownEndTick = this.world.getTime() + (Configs.AHP.hideAndSeekCooldownSeconds.get() * 20L);
        }

        if (this.targetBlock != null) {
            TARGETED_BLOCKS.remove(this.targetBlock); // Release block
            this.targetBlock = null;
        }

        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }

    @Override
    public void tick() {
        switch (this.currentState) {
            case MOVING_TO_BLOCK -> {
                this.moveTimeout++;
                if (this.moveTimeout > MAX_MOVE_TIMEOUT) {
                    this.isFinished = true; // Give up if timed out
                    return;
                }

                HamsterMovementUtil.facePosition(this.hamster, this.targetBlock.getX() + 0.5, this.targetBlock.getY() + 0.5, this.targetBlock.getZ() + 0.5);

                if (this.hamster.getBlockPos().isWithinDistance(this.targetBlock, 1.5)) {
                    this.currentState = State.POUNCING;
                    this.lungeTicks = LUNGE_DURATION_TICKS;
                    this.pounceStartPos = this.hamster.getPos();
                    this.hamster.getNavigation().stop();
                    this.hamster.triggerAnimOnServer("mainController", "anim_hamster_pounce");
                } else if (this.hamster.getNavigation().isIdle()) {
                    // Try repathing if stuck, abort immediately if unreachable
                    Path path = this.hamster.getNavigation().findPathTo(this.targetBlock, 1);
                    if (path != null) {
                        this.hamster.getNavigation().startMovingAlong(path, 1.5D);
                    } else {
                        this.isFinished = true;
                        return;
                    }
                }
            }
            case POUNCING -> {
                this.lungeTicks--;

                if (this.targetBlock != null && this.lungeTicks >= 0) {
                    HamsterMovementUtil.facePosition(this.hamster, this.targetBlock.getX() + 0.5, this.targetBlock.getY() + 0.5, this.targetBlock.getZ() + 0.5);
                }

                if (this.pounceStartPos != null && this.lungeTicks >= 0) {
                    Vec3d interpolatedPos = HamsterPhysicsUtil.calculatePouncePosition(this.pounceStartPos, Vec3d.ofBottomCenter(this.targetBlock), this.lungeTicks, LUNGE_DURATION_TICKS);
                    this.hamster.setPosition(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
                }

                if (this.lungeTicks == 0) {
                    // Spawn block hider entity, transfer state
                    if (this.world instanceof ServerWorld serverWorld) {
                        BlockState state = serverWorld.getBlockState(this.targetBlock);
                        HamsterBlockHiderEntity hider = ModEntities.HAMSTER_BLOCK_HIDER.get().create(serverWorld);

                        if (hider != null) {
                            NbtCompound fullNbt = new NbtCompound();
                            this.hamster.writeNbt(fullNbt);

                            int duration = this.hamster.getRandom().nextBetween(
                                    Configs.AHP.hideAndSeekMinDurationSeconds.get() * 20,
                                    Configs.AHP.hideAndSeekMaxDurationSeconds.get() * 20
                            );

                            hider.initializeHiding(this.targetBlock, duration, fullNbt);
                            serverWorld.spawnEntity(hider);

                            // Output randomized snarky message
                            if (this.hamster.getOwner() instanceof ServerPlayerEntity owner) {
                                MiscUtil.MessagingUtil.sendRandomizedSequentialMessage(
                                        owner,
                                        null, // No first-time tracking needed
                                        "message.adorablehamsterpets.hide_and_seek_game_start",
                                        4,
                                        "hide_and_seek_messages"
                                );
                            }

                            // Visuals & Audio
                            ParticleEffectsUtil.spawnParticles(
                                    serverWorld,
                                    Vec3d.ofCenter(this.targetBlock),
                                    ParticleTypes.POOF,
                                    25,
                                    new Vec3d(0.3, 0.3, 0.3),
                                    0.05
                            );

                            // Spawn contextual block particles
                            ParticleEffectsUtil.spawnParticles(
                                    serverWorld,
                                    Vec3d.ofCenter(this.targetBlock),
                                    MiscUtil.BlockStateUtil.getHidingSpotParticle(state),
                                    30,
                                    new Vec3d(0.4, 0.4, 0.4),
                                    0.0
                            );

                            SoundEvent sound = ModSounds.getDynamicBlockSound(state);
                            serverWorld.playSound(null, this.targetBlock, sound, SoundCategory.NEUTRAL, 1.0F, 0.8F);
                        }
                    }

                    this.hamster.discard();

                    if (this.targetBlock != null) {
                        TARGETED_BLOCKS.remove(this.targetBlock);
                        this.targetBlock = null;
                    }
                }
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private Optional<BlockPos> findHidingSpot() {
        BlockPos center = this.hamster.getBlockPos();
        List<BlockPos> validSpots = new ArrayList<>();
        int occupiedCount = 0;
        int blacklistedCount = 0;
        int invalidCount = 0;

        for (BlockPos pos : BlockPos.iterateOutwards(center, SEARCH_RADIUS, 3, SEARCH_RADIUS)) {
            // Check if block is already claimed/occupied
            if (TARGETED_BLOCKS.contains(pos) || HamsterAbstractHiddenEntity.isBlockOccupied(this.world, pos)) {
                occupiedCount++;
                continue;
            }

            BlockState state = this.world.getBlockState(pos);

            if (ConfigDataCache.isHideAndSeekBlacklisted(state)) {
                blacklistedCount++;
                continue;
            }

            boolean isValid = ConfigDataCache.isHideAndSeekBlock(state);

            if (!isValid && Configs.AHP.allowInventoryHiding) {
                if (this.world.getBlockEntity(pos) instanceof Inventory) {
                    isValid = true;
                }
            }

            if (isValid) {
                validSpots.add(pos.toImmutable());
            } else {
                invalidCount++;
            }
        }

        // Sort by farthest first
        validSpots.sort(Comparator.comparingDouble((BlockPos p) -> p.getSquaredDistance(center)).reversed());

        // Find first one with valid path
        for (BlockPos pos : validSpots) {
            if (this.hamster.getNavigation().findPathTo(pos, 1) != null) {
                return Optional.of(pos);
            }
        }

        return Optional.empty();
    }
}