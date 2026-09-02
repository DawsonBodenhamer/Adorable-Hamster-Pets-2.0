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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
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
    private final Level world;

    private State currentState;
    private BlockPos targetBlock;
    private Vec3 pounceStartPos;

    private int lungeTicks = 0;
    private int moveTimeout = 0;
    private int checkTimer = 0;
    private boolean isFinished = false;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterHideAndSeekGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        if (this.world.isClientSide() || !Configs.AHP_MAIN.enableHideAndSeek) return false;

        // Basic cooldown and state restrictions
        if (this.hamster.level().getGameTime() < this.hamster.hideAndSeekCooldownEndTick) return false;

        if (!this.hamster.isTame()
                || HamsterMovementUtil.shouldNotMove(this.hamster)
                || this.hamster.isHoldingMouthItem()
                || this.hamster.isPlayingTag()
        ) {
            return false;
        }

        // Throttle expensive block scanning
        if (this.checkTimer > 0) {
            this.checkTimer--;
            return false;
        }
        this.checkTimer = this.adjustedTickDelay(20);

        // Scale denominator to match check frequency
        int denominator = Math.max(1, Configs.AHP_MAIN.hideAndSeekChanceDenominator.get() / 20);
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
    public boolean canContinueToUse() {
        if (this.isFinished) return false;

        if (this.targetBlock == null) return false;
        if (HamsterMovementUtil.shouldNotMove(this.hamster)) return false;

        // Check block validity continuously
        BlockState state = this.world.getBlockState(this.targetBlock);
        boolean isValid = ConfigDataCache.isHideAndSeekBlock(state) ||
                (Configs.AHP_MAIN.allowInventoryHiding && this.world.getBlockEntity(this.targetBlock) instanceof Container);

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

        Path path = this.hamster.getNavigation().createPath(this.targetBlock, 1);
        if (path != null) {
            this.hamster.getNavigation().moveTo(path, 1.5D);
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
                    (Configs.AHP_MAIN.allowInventoryHiding && this.world.getBlockEntity(this.targetBlock) instanceof Container);
        }

        // Only apply cooldown if block was valid
        if (blockIsValid) {
            this.hamster.hideAndSeekCooldownEndTick = this.world.getGameTime() + (Configs.AHP_MAIN.hideAndSeekCooldownSeconds.get() * 20L);
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

                if (this.hamster.blockPosition().closerThan(this.targetBlock, 1.5)) {
                    this.currentState = State.POUNCING;
                    this.lungeTicks = LUNGE_DURATION_TICKS;
                    this.pounceStartPos = this.hamster.position();
                    this.hamster.getNavigation().stop();
                    this.hamster.triggerAnimOnServer("mainController", "anim_hamster_pounce");
                } else if (this.hamster.getNavigation().isDone()) {
                    // Try repathing if stuck, abort immediately if unreachable
                    Path path = this.hamster.getNavigation().createPath(this.targetBlock, 1);
                    if (path != null) {
                        this.hamster.getNavigation().moveTo(path, 1.5D);
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
                    Vec3 interpolatedPos = HamsterPhysicsUtil.calculatePouncePosition(this.pounceStartPos, Vec3.atBottomCenterOf(this.targetBlock), this.lungeTicks, LUNGE_DURATION_TICKS);
                    this.hamster.setPos(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
                }

                if (this.lungeTicks == 0) {
                    // Spawn block hider entity, transfer state
                    if (this.world instanceof ServerLevel serverWorld) {
                        BlockState state = serverWorld.getBlockState(this.targetBlock);
                        HamsterBlockHiderEntity hider = ModEntities.HAMSTER_BLOCK_HIDER.get().create(serverWorld);

                        if (hider != null) {
                            CompoundTag fullNbt = new CompoundTag();
                            this.hamster.saveWithoutId(fullNbt);

                            int duration = this.hamster.getRandom().nextIntBetweenInclusive(
                                    Configs.AHP_MAIN.hideAndSeekMinDurationSeconds.get() * 20,
                                    Configs.AHP_MAIN.hideAndSeekMaxDurationSeconds.get() * 20
                            );

                            hider.initializeHiding(this.targetBlock, duration, fullNbt);
                            serverWorld.addFreshEntity(hider);

                            // Output randomized snarky message
                            if (this.hamster.getOwner() instanceof ServerPlayer owner) {
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
                                    Vec3.atCenterOf(this.targetBlock),
                                    ParticleTypes.POOF,
                                    25,
                                    new Vec3(0.3, 0.3, 0.3),
                                    0.05
                            );

                            // Spawn contextual block particles
                            ParticleEffectsUtil.spawnParticles(
                                    serverWorld,
                                    Vec3.atCenterOf(this.targetBlock),
                                    MiscUtil.BlockStateUtil.getHidingSpotParticle(state),
                                    30,
                                    new Vec3(0.4, 0.4, 0.4),
                                    0.0
                            );

                            SoundEvent sound = ModSounds.getDynamicBlockSound(state);
                            serverWorld.playSound(null, this.targetBlock, sound, SoundSource.NEUTRAL, 1.0F, 0.8F);
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
        BlockPos center = this.hamster.blockPosition();
        List<BlockPos> validSpots = new ArrayList<>();
        int occupiedCount = 0;
        int blacklistedCount = 0;
        int invalidCount = 0;

        for (BlockPos pos : BlockPos.withinManhattan(center, SEARCH_RADIUS, 3, SEARCH_RADIUS)) {
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

            if (!isValid && Configs.AHP_MAIN.allowInventoryHiding) {
                if (this.world.getBlockEntity(pos) instanceof Container) {
                    isValid = true;
                }
            }

            if (isValid) {
                validSpots.add(pos.immutable());
            } else {
                invalidCount++;
            }
        }

        // Sort by farthest first
        validSpots.sort(Comparator.comparingDouble((BlockPos p) -> p.distSqr(center)).reversed());

        // Find first one with valid path
        for (BlockPos pos : validSpots) {
            if (this.hamster.getNavigation().createPath(pos, 1) != null) {
                return Optional.of(pos);
            }
        }

        return Optional.empty();
    }
}
