package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

public class HamsterSitGoal extends SitGoal {
    private final HamsterEntity hamster;

    public HamsterSitGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        if (this.hamster.isKnockedOut()) {
            return false;
        }

        boolean canStart = super.canStart();

        // If in wander mode, keep wandering as long as bed exists
        if (canStart && this.hamster.isWanderModeActive() && !this.hamster.isSitting()) {
            LivingEntity owner = this.hamster.getOwner();

            if (owner == null && this.hamster.getLinkedBedPos().isPresent()) {
                GlobalPos globalBedPos = this.hamster.getLinkedBedPos().get();

                if (this.hamster.getWorld().getRegistryKey() == globalBedPos.getDimension()) {
                    BlockPos bedPos = globalBedPos.getPos();

                    // Ensure chunk is loaded
                    if (this.hamster.getWorld().isChunkLoaded(bedPos.getX() >> 4, bedPos.getZ() >> 4)) {
                        BlockState bedState = this.hamster.getWorld().getBlockState(bedPos);
                        if (bedState.getBlock() instanceof HamsterBedBlock) {
                            return false; // Bed is valid, prevent forced sitting
                        }
                    } else {
                        // Chunk unloaded, assume bed is intact
                        return false;
                    }
                }
            }
        }

        return canStart;
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }
}