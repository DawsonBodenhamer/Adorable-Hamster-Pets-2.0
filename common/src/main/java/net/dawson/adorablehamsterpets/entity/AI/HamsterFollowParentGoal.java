package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class HamsterFollowParentGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private LivingEntity target;
    private final double speed;
    private int delay;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterFollowParentGoal(HamsterEntity hamster, double speed) {
        this.hamster = hamster;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        if (!this.hamster.isBaby()) return false;
        if (HamsterMovementUtil.shouldNotFollow(this.hamster)) return false;

        UUID parentUuid = this.hamster.getParentUuid();
        if (parentUuid == null) return false;

        if (this.hamster.getWorld() instanceof ServerWorld serverWorld) {
            // Try to find parent in the world
            Entity entity = serverWorld.getEntity(parentUuid);
            if (entity instanceof HamsterEntity parentHamster && entity.isAlive()) {
                this.target = parentHamster;
                // Follow if more than 2 blocks away
                return this.hamster.squaredDistanceTo(this.target) > 4.0;
            } else {
                // Parent not found. Check if they are on nearby player shoulder
                for (PlayerEntity player : serverWorld.getPlayers()) {
                    if (player instanceof PlayerEntityAccessor accessor) {
                        if (isParentOnShoulder(accessor, parentUuid)) {
                            this.target = player;
                            return this.hamster.squaredDistanceTo(this.target) > 4.0;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        if (!this.hamster.isBaby() || this.target == null || !this.target.isAlive()) return false;
        if (HamsterMovementUtil.shouldNotFollow(this.hamster)) return false;

        // If target is Player, ensure the parent is still on their shoulder
        if (this.target instanceof PlayerEntity player) {
            if (!isParentOnShoulder((PlayerEntityAccessor) player, this.hamster.getParentUuid())) {
                return false;
            }
        }

        double distanceSq = this.hamster.squaredDistanceTo(this.target);
        // Continue following as long as distance is between 2 and 16 blocks
        return distanceSq > 4.0 && distanceSq <= 256.0;
    }

    @Override
    public void start() {
        this.delay = 0;
        this.hamster.setActiveCustomGoalName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        this.target = null;
        if (this.hamster.getActiveCustomGoalName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalName("None");
        }
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        // --- Look at parent ---
        HamsterMovementUtil.faceEntity(this.hamster, this.target);

        if (--this.delay <= 0) {
            this.delay = this.getTickCount(10);

            // --- Teleport or Move Logic ---
            if (HamsterMovementUtil.shouldTeleportTo(this.hamster, this.target)) {
                HamsterMovementUtil.tryTeleportTo(this.hamster, this.target);
            } else {
                this.hamster.getNavigation().startMovingTo(this.target, this.speed);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private boolean isParentOnShoulder(PlayerEntityAccessor accessor, UUID parentUuid) {
        if (!accessor.hasAnyShoulderHamster()) return false;
        for (ShoulderLocation loc : ShoulderLocation.values()) {
            NbtCompound nbt = accessor.getShoulderHamster(loc);
            if (!nbt.isEmpty()) {
                Optional<HamsterState> state = HamsterState.fromNbt(nbt);
                if (state.isPresent() && state.get().entityUuid().equals(parentUuid)) {
                    return true;
                }
            }
        }
        return false;
    }
}