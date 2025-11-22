package net.dawson.adorablehamsterpets.entity.AI;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.FollowOwnerGoalAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class HamsterFollowOwnerGoal extends FollowOwnerGoal {
    private final HamsterEntity hamster;
    private static final double BUFFED_FOLLOW_SPEED = 1.5D;

    public HamsterFollowOwnerGoal(HamsterEntity hamster, double speed, float minDistance, float maxDistance) {
        // In 1.20.1, the boolean is 'leavesAllowed'. We set it to true as a safe default.
        super(hamster, speed, minDistance, maxDistance, true);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }

        // --- 2. Apply  custom conditions ---
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond() ||
                this.hamster.isCelebratingChase() ||
                this.hamster.isWanderModeActive()) {
            return false;
        }

        // The super.canStart() already checks the base minDistance.
        if (this.hamster.hasGreenBeanBuff()) {
            LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
            if (owner != null) {
                // If buffed, require a larger distance to start following.
                return !(this.hamster.squaredDistanceTo(owner) < (double) ((((FollowOwnerGoalAccessor) this).getMinDistance() + 5.0F) * (((FollowOwnerGoalAccessor) this).getMinDistance() + 5.0F)));
            }
        }

        return true;
    }

    @Override
    public boolean shouldContinue() {
        // Custom state checks that should stop the goal immediately.
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() ||
                this.hamster.isSulking() || this.hamster.isCelebratingDiamond() || this.hamster.isCelebratingChase()) {
            return false;
        }

        // Let the vanilla logic handle the rest.
        return super.shouldContinue();
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
        AdorableHamsterPets.LOGGER.trace("[FollowGoal-{}] start: Goal has started. IsBuffed: {}", this.hamster.getId(), this.hamster.hasGreenBeanBuff());
    }

    @Override
    public void tick() {
        FollowOwnerGoalAccessor accessor = (FollowOwnerGoalAccessor) this;
        LivingEntity owner = accessor.getOwner();
        if (owner == null) return;

        this.hamster.getLookControl().lookAt(owner, 10.0F, this.hamster.getMaxLookPitchChange());

        int currentTicks = accessor.getUpdateCountdownTicks() - 1;
        accessor.setUpdateCountdownTicks(currentTicks);

        if (currentTicks <= 0) {
            accessor.setUpdateCountdownTicks(this.getTickCount(10));

            // --- Replicated Vanilla Teleport Logic ---
            if (this.hamster.squaredDistanceTo(owner) >= 144.0) {
                this.tryTeleport();
            } else {
                // --- Custom Pathfinding Logic ---
                if (this.hamster.hasGreenBeanBuff()) {
                    // "Zoomies" pathfinding
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

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    // --- Private Helper Methods for Teleportation (Replicated from Vanilla) ---
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