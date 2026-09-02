package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.MinigameUtil;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class HamsterSniffForOreGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants & Enums
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int PATHING_RECHECK_INTERVAL = 20;

    private enum SeekingState {
        IDLE,
        SCANNING,
        MOVING_TO_ORE,
        WAITING_FOR_PATH,
        CELEBRATING_DIAMOND,
        SULKING_AT_GOLD
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Checks if an ore block is "exposed" by having at least one adjacent air-like block.
     *
     * @param orePos The position of the ore block.
     * @return True if the ore is exposed, false otherwise.
     */
    public static boolean isOreExposed(BlockPos orePos, Level world) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = orePos.relative(direction);
            // Exposed if adjacent block has no collision shape
            if (world.getBlockState(adjacentPos).getCollisionShape(world, adjacentPos, CollisionContext.empty()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private final Level world;

    private BlockPos targetOrePos;
    private boolean isSeekingDisappointingOre;
    private SeekingState currentState = SeekingState.IDLE;
    private int pathingTickTimer;
    private int searchCooldown = 0;

    @Nullable
    private Path path;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterSniffForOreGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canUse() {
        if (this.world.isClientSide || !Configs.AHP_MAIN.enableIndependentDiamondSeeking) {
            return false;
        }

        if (!this.hamster.isPrimedToSeekDiamonds) {
            return false;
        }

        if (HamsterMovementUtil.shouldNotMove(this.hamster)) {
            return false;
        }

        // Block start if in combat
        if (this.hamster.getTarget() != null) {
            return false;
        }

        if (Configs.AHP_MAIN.enableIndependentDiamondSeekCooldown && this.hamster.foundOreCooldownEndTick > this.world.getGameTime()) {
            return false;
        }

        // Prevent scanning every tick
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        boolean foundTarget = findNewTargetOreAndSetState();
        if (!foundTarget) {
            this.searchCooldown = 10; // Check twice per second
        }
        return foundTarget;
    }

    @Override
    public boolean canContinueToUse() {
        // Terminal states
        if (this.currentState == SeekingState.IDLE || this.currentState == SeekingState.CELEBRATING_DIAMOND || this.currentState == SeekingState.SULKING_AT_GOLD) {
            return false;
        }

        // Interruptions
        if (HamsterMovementUtil.shouldNotMove(this.hamster)) {
            return false;
        }

        // Combat interruption
        if (this.hamster.getTarget() != null) {
            return false;
        }

        if (this.targetOrePos == null) return false;

        BlockState targetState = this.world.getBlockState(this.targetOrePos);
        boolean isTargetCelebration = ConfigDataCache.isCelebrationOre(targetState);
        boolean isTargetSulking = ConfigDataCache.isSulkingOre(targetState);

        // Ensure the ore block hasn't been broken or changed
        if (this.isSeekingDisappointingOre) {
            return isTargetSulking;
        } else {
            return isTargetCelebration;
        }
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName() + (this.isSeekingDisappointingOre ? "_Gold" : "_Diamond"));
        this.pathingTickTimer = 0;

        // currentState is already SCANNING from canStart/findNewTargetOreAndSetState
        attemptPathToTarget();
    }

    @Override
    public void stop() {
        this.path = null;
        this.hamster.getNavigation().stop();

        boolean targetOreStillExists = false;

        if (this.targetOrePos != null) {
            BlockState targetState = this.world.getBlockState(this.targetOrePos);
            boolean isTargetCelebration = ConfigDataCache.isCelebrationOre(targetState);
            boolean isTargetSulking = ConfigDataCache.isSulkingOre(targetState);

            if (this.isSeekingDisappointingOre && isTargetSulking) targetOreStillExists = true;
            if (!this.isSeekingDisappointingOre && isTargetCelebration) targetOreStillExists = true;
        }

        // Clear the primed flag if it didn't finish successfully or target broke
        if (this.currentState != SeekingState.CELEBRATING_DIAMOND && this.currentState != SeekingState.SULKING_AT_GOLD && !targetOreStillExists) {
            this.hamster.isPrimedToSeekDiamonds = false;
        }

        if (this.hamster.isCelebratingDiamond() && (this.currentState != SeekingState.CELEBRATING_DIAMOND || !targetOreStillExists)) {
            this.hamster.setCelebratingDiamond(false);
        }

        if (this.hamster.getActiveCustomGoalName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }

        this.currentState = SeekingState.IDLE;
        this.targetOrePos = null;
        this.searchCooldown = 0;
    }

    @Override
    public void tick() {
        if (this.targetOrePos == null) {
            stop();
            return;
        }

        // Sync relative elevation for animation
        this.hamster.setOreTargetAbove(this.targetOrePos.getY() >= this.hamster.getY());

        // Fast turn speed towards target
        HamsterMovementUtil.facePosition(
                this.hamster,
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY() + 0.5,
                this.targetOrePos.getZ() + 0.5
        );

        switch (this.currentState) {
            case MOVING_TO_ORE -> {
                // Spawn particle breadcrumbs
                if (!this.world.isClientSide()) {
                    ParticleEffectsUtil.spawnBreadcrumbs(
                            (ServerLevel) this.world,
                            this.path,
                            ParticleTypes.MYCELIUM,
                            1,
                            0.2,
                            0.0,
                            0.2,
                            3.0
                    );
                }

                if (this.hamster.getNavigation().isDone() || this.hamster.blockPosition().closerThan(this.targetOrePos, 1.5)) {
                    // Only celebrate if hamster close and ore exposed
                    if (this.hamster.blockPosition().closerThan(this.targetOrePos, 1.5) && isOreExposed(this.targetOrePos, this.world)) {
                        onOreReached();
                    } else {
                        // Clear old path and wait for re-evaluation
                        this.path = null;
                        this.currentState = SeekingState.WAITING_FOR_PATH;
                        this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
                    }
                }
            }
            case WAITING_FOR_PATH -> {
                if (this.pathingTickTimer > 0) {
                    this.pathingTickTimer--;
                } else {
                    attemptPathToTarget();
                }
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- 1. Target Evaluation ---
    private boolean findNewTargetOreAndSetState() {
        this.targetOrePos = null;
        this.isSeekingDisappointingOre = false;
        this.hamster.currentOreTarget = null;

        List<BlockPos> exposedCelebrationOres = new ArrayList<>();
        List<BlockPos> buriedCelebrationOres = new ArrayList<>();
        List<BlockPos> buriedSulkingOres = new ArrayList<>();
        int radius = Configs.AHP_MAIN.diamondSeekRadius.get();

        for (BlockPos pos : BlockPos.withinManhattan(this.hamster.blockPosition(), radius, radius, radius)) {
            BlockState state = this.world.getBlockState(pos);

            if (ConfigDataCache.isCelebrationOre(state)) {
                if (isOreExposed(pos, this.world)) {
                    exposedCelebrationOres.add(pos.immutable());
                } else {
                    buriedCelebrationOres.add(pos.immutable());
                }
            } else if (ConfigDataCache.isSulkingOre(state)) {
                if (!isOreExposed(pos, this.world)) {
                    buriedSulkingOres.add(pos.immutable());
                }
            }
        }

        // Make mistake if applicable
        boolean targetIsSulkingOre = !buriedSulkingOres.isEmpty() && this.world.random.nextFloat() < Configs.AHP_MAIN.goldMistakeChance.get();

        if (targetIsSulkingOre) {
            buriedSulkingOres.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(this.hamster.position())));
            this.targetOrePos = buriedSulkingOres.get(0);
            this.isSeekingDisappointingOre = true;
        } else {
            if (!exposedCelebrationOres.isEmpty()) {
                exposedCelebrationOres.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(this.hamster.position())));
                this.targetOrePos = exposedCelebrationOres.get(0);
            } else if (!buriedCelebrationOres.isEmpty()) {
                buriedCelebrationOres.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(this.hamster.position())));
                this.targetOrePos = buriedCelebrationOres.get(0);
            }
        }

        if (this.targetOrePos != null) {
            this.hamster.currentOreTarget = this.targetOrePos;
            this.currentState = SeekingState.SCANNING;
            return true;
        }

        return false;
    }

    private void attemptPathToTarget() {
        if (this.targetOrePos == null) {
            this.currentState = SeekingState.IDLE;
            return;
        }

        this.path = this.hamster.getNavigation().createPath(
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY(),
                this.targetOrePos.getZ() + 0.5,
                0
        );

        if (this.path != null) {
            this.hamster.getNavigation().moveTo(this.path, 0.5D);
            this.currentState = SeekingState.MOVING_TO_ORE;
        } else {
            this.currentState = SeekingState.WAITING_FOR_PATH;
            this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
        }
    }

    // --- 2. Ore Reached Logic ---

    private void onOreReached() {
        this.hamster.getNavigation().stop();
        this.hamster.isPrimedToSeekDiamonds = false;

        if (Configs.AHP_MAIN.enableIndependentDiamondSeekCooldown) {
            this.hamster.foundOreCooldownEndTick = this.world.getGameTime() + Configs.AHP_MAIN.independentOreSeekCooldownTicks.get();
        }

        if (this.isSeekingDisappointingOre) {
            processGoldMistake();
        } else {
            processDiamondFind();
        }
    }

    private void processGoldMistake() {
        this.currentState = SeekingState.SULKING_AT_GOLD;

        if (this.hamster.getOwner() instanceof ServerPlayer owner) {
            if (this.hamster.distanceToSqr(owner) < 36.0) {
                HamsterMovementUtil.faceEntity(this.hamster, owner);
            }

            // Output randomized snarky message for owner and trigger advancement
            MiscUtil.MessagingUtil.sendRandomizedSequentialMessage(
                    owner,
                    ResourceLocation.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "technical/hamster_found_gold_first_time"),
                    "message.adorablehamsterpets.found_gold_mistake",
                    7,
                    "gold_mistake_messages"
            );
            ModCriteria.HAMSTER_FOUND_GOLD.get().trigger(owner);
        }

        MinigameUtil.executeSulkFailure(this.hamster, Vec3.atCenterOf(this.targetOrePos));
    }

    private void processDiamondFind() {
        this.currentState = SeekingState.CELEBRATING_DIAMOND;
        this.hamster.setCelebratingDiamond(true);

        if (this.hamster.getOwner() instanceof ServerPlayer serverPlayerOwner) {
            ModCriteria.HAMSTER_LED_TO_DIAMOND.get().trigger(serverPlayerOwner, this.hamster, this.targetOrePos);
        }
    }
}
