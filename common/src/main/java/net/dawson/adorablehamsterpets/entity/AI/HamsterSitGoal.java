package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.level.block.state.BlockState;

public class HamsterSitGoal extends SitWhenOrderedToGoal {
    private final HamsterEntity hamster;

    public HamsterSitGoal(HamsterEntity hamster) {
        super(hamster);
        this.hamster = hamster;
    }

    @Override
    public boolean canUse() {
        if (this.hamster.isKnockedOut()) {
            return false;
        }

        boolean canStart = super.canUse();

        // If in wander mode, keep wandering as long as bed exists
        if (canStart && this.hamster.isWanderModeActive() && !this.hamster.isOrderedToSit()) {
            LivingEntity owner = this.hamster.getOwner();

            if (owner == null && this.hamster.getLinkedBedPos().isPresent()) {
                GlobalPos globalBedPos = this.hamster.getLinkedBedPos().get();

                if (this.hamster.level().dimension() == globalBedPos.dimension()) {
                    BlockPos bedPos = globalBedPos.pos();

                    // Ensure chunk is loaded
                    if (this.hamster.level().hasChunk(bedPos.getX() >> 4, bedPos.getZ() >> 4)) {
                        BlockState bedState = this.hamster.level().getBlockState(bedPos);
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