package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.FollowOwnerGoalAccessor;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class HamsterFollowOwnerGoal extends FollowOwnerGoal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double BUFFED_FOLLOW_SPEED = 1.5D;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterFollowOwnerGoal(HamsterEntity hamster, double speed, float minDistance, float maxDistance) {
        // In 1.20.1, the boolean is leavesAllowed
        super(hamster, speed, minDistance, maxDistance, true);
        this.hamster = hamster;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        // --- Base Logic ---
        if (!super.canStart()) {
            return false;
        }

        // --- Parent Override ---
        // Abort if baby is tracking a living parent
        if (this.hamster.isBaby() && this.hamster.getParentUuid() != null) {
            if (this.hamster.getWorld() instanceof ServerWorld serverWorld) {
                Entity parent = serverWorld.getEntity(this.hamster.getParentUuid());
                if (parent instanceof HamsterEntity parentHamster && parentHamster.isAlive()) {
                    return false;
                }
            }
        }

        // --- State Exclusions ---
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond() ||
                this.hamster.isCelebratingRetrieval() ||
                this.hamster.isPlayingTag() ||
                this.hamster.isCelebratingBaby() ||
                this.hamster.isWanderModeActive()) {
            return false;
        }

        // --- Distance Calculation ---
        // Recalculate minimum follow distance for zoomies
        float minDist = ((FollowOwnerGoalAccessor) this).getMinDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();

        if (owner == null) {
            return false;
        }

        if (this.hamster.hasGreenBeanBuff()) {
            minDist += 5.0F;
        }

        return !(this.hamster.squaredDistanceTo(owner) < (double) (minDist * minDist));
    }

    @Override
    public boolean shouldContinue() {
        // --- State Exclusions ---
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond() ||
                this.hamster.isPlayingTag() ||
                this.hamster.isCelebratingBaby() ||
                this.hamster.isCelebratingRetrieval()) {
            return false;
        }

        // --- Distance Calculation ---
        // Recalculate maximum follow distance for zoomies
        float maxDist = ((FollowOwnerGoalAccessor) this).getMaxDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();

        if (owner == null) {
            return false;
        }

        if (this.hamster.hasGreenBeanBuff()) {
            maxDist += 5.0F;
        }

        return !this.hamster.getNavigation().isIdle() && this.hamster.squaredDistanceTo(owner) > (double) (maxDist * maxDist);
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        // --- Target Resolution ---
        FollowOwnerGoalAccessor accessor = (FollowOwnerGoalAccessor) this;
        LivingEntity owner = accessor.getOwner();

        if (owner == null) {
            return;
        }

        // Evaluate teleport condition (vanilla default: 12 blocks distance squared)
        boolean shouldTeleport = this.hamster.squaredDistanceTo(owner) >= 144.0;

        // --- Facing Logic ---
        if (!shouldTeleport) {
            HamsterMovementUtil.faceEntity(this.hamster, owner);
        }

        // --- Update Timer ---
        int currentTicks = accessor.getUpdateCountdownTicks() - 1;
        accessor.setUpdateCountdownTicks(currentTicks);

        if (currentTicks <= 0) {
            accessor.setUpdateCountdownTicks(this.getTickCount(10));

            // --- Movement Execution ---
            if (shouldTeleport) {
                this.tryTeleport();
            } else {
                if (this.hamster.hasGreenBeanBuff()) {
                    // Zoomies erratic pathfinding
                    Vec3d targetPos = FuzzyTargeting.findTo(this.hamster, 8, 5, Vec3d.ofCenter(owner.getBlockPos()));
                    if (targetPos != null) {
                        this.hamster.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, BUFFED_FOLLOW_SPEED);
                    }
                } else {
                    // Standard pathfinding
                    this.hamster.getNavigation().startMovingTo(owner, accessor.getSpeed());
                }
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void tryTeleport() {
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return;
        BlockPos blockPos = owner.getBlockPos();

        for(int i = 0; i < 10; ++i) {
            int j = this.getRandomInt(-3, 3);
            int k = this.getRandomInt(-1, 1);
            int l = this.getRandomInt(-3, 3);
            if (this.tryTeleportTo(blockPos.getX() + j, blockPos.getY() + k, blockPos.getZ() + l)) {
                return;
            }
        }
    }

    private boolean tryTeleportTo(int x, int y, int z) {
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return false;

        if (Math.abs((double)x - owner.getX()) < 2.0 && Math.abs((double)z - owner.getZ()) < 2.0) {
            return false;
        }
        if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        }

        this.hamster.refreshPositionAndAngles((double)x + 0.5, y, (double)z + 0.5, this.hamster.getYaw(), this.hamster.getPitch());
        this.hamster.getNavigation().stop();
        return true;
    }

    private boolean canTeleportTo(BlockPos pos) {
        WorldView world = this.hamster.getWorld();
        PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(world, pos.mutableCopy());
        if (pathNodeType != PathNodeType.WALKABLE) {
            return false;
        }
        BlockState blockState = world.getBlockState(pos.down());
        if (blockState.getBlock() instanceof LeavesBlock) { // The 'leavesAllowed' check
            return false;
        }
        BlockPos blockPos = pos.subtract(this.hamster.getBlockPos());
        return world.isSpaceEmpty(this.hamster, this.hamster.getBoundingBox().offset(blockPos));
    }

    private int getRandomInt(int min, int max) {
        return this.hamster.getRandom().nextInt(max - min + 1) + min;
    }
}