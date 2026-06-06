package net.dawson.adorablehamsterpets.entity.custom;

import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract base class for invisible proxy entities that represent a hamster hiding inside a block.
 * Handles global block occupancy registration, NBT storage, and the physics of popping back out.
 */
public abstract class HamsterAbstractHiddenEntity extends Entity {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // Maps a specific block anchor to the ID of the entity hiding inside it
    private static final Map<GlobalPos, Integer> OCCUPIED_BLOCKS = new ConcurrentHashMap<>();

    public static boolean isBlockOccupied(World world, BlockPos anchor) {
        if (anchor == null || world.isClient()) return false;
        GlobalPos key = GlobalPos.create(world.getRegistryKey(), anchor);
        return OCCUPIED_BLOCKS.containsKey(key);
    }

    @Nullable
    public static HamsterAbstractHiddenEntity getOccupant(World world, BlockPos anchor) {
        if (anchor == null || world.isClient()) return null;
        GlobalPos key = GlobalPos.create(world.getRegistryKey(), anchor);
        Integer id = OCCUPIED_BLOCKS.get(key);
        if (id != null) {
            Entity entity = ((ServerWorld) world).getEntityById(id);
            if (entity instanceof HamsterAbstractHiddenEntity hidden) {
                return hidden;
            }
        }
        return null;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    protected NbtCompound hamsterNbt = new NbtCompound();
    protected BlockPos anchorPos = null;
    protected BlockPos forcedExitPos = null;
    protected Float forcedExitYaw = null;
    protected boolean isRegistered = false;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public HamsterAbstractHiddenEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // No client synced data needed for this logic entity
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (!this.getWorld().isClient()) {
            unregisterOccupancy();
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public BlockPos getAnchorPos() {
        return this.anchorPos;
    }

    public void setForcedExitPos(BlockPos pos) {
        this.forcedExitPos = pos;
    }

    public void setForcedExitYaw(float yaw) {
        this.forcedExitYaw = yaw;
    }

    public boolean isOwnedBy(PlayerEntity player) {
        if (this.hamsterNbt != null && this.hamsterNbt.containsUuid("Owner")) {
            return this.hamsterNbt.getUuid("Owner").equals(player.getUuid());
        }
        return false;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("HamsterNBT", this.hamsterNbt);
        if (this.anchorPos != null) {
            nbt.putLong("AnchorPos", this.anchorPos.asLong());
        }
        if (this.forcedExitPos != null) {
            nbt.putLong("ForcedExitPos", this.forcedExitPos.asLong());
        }
        if (this.forcedExitYaw != null) {
            nbt.putFloat("ForcedExitYaw", this.forcedExitYaw);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.hamsterNbt = nbt.getCompound("HamsterNBT");

        if (nbt.contains("AnchorPos")) {
            this.anchorPos = BlockPos.fromLong(nbt.getLong("AnchorPos"));
        } else if (nbt.contains("TreeAnchor")) {
            // Backwards compatibility for pre v3.6.1 tree heists
            this.anchorPos = BlockPos.fromLong(nbt.getLong("TreeAnchor"));
        }

        if (nbt.contains("ForcedExitPos")) {
            this.forcedExitPos = BlockPos.fromLong(nbt.getLong("ForcedExitPos"));
        }
        if (nbt.contains("ForcedExitYaw")) {
            this.forcedExitYaw = nbt.getFloat("ForcedExitYaw");
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Protected Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    protected void registerOccupancy() {
        if (this.getWorld().isClient() || this.anchorPos == null || this.isRegistered) return;

        GlobalPos key = GlobalPos.create(this.getWorld().getRegistryKey(), this.anchorPos);
        OCCUPIED_BLOCKS.put(key, this.getId());
        this.isRegistered = true;
    }

    protected void unregisterOccupancy() {
        if (this.getWorld().isClient() || this.anchorPos == null || !this.isRegistered) return;

        GlobalPos key = GlobalPos.create(this.getWorld().getRegistryKey(), this.anchorPos);
        if (OCCUPIED_BLOCKS.remove(key, this.getId())) {
            this.isRegistered = false;
        }
    }

    /**
     * Reconstructs the HamsterEntity from NBT, places it at a safe exit position,
     * and sets its velocity based on whether the action was successful.
     * The subclass is responsible for spawning it into the world.
     *
     * @param success Whether the hamster pops out excitedly (forward velocity) or fails (drops straight down).
     * @return The fully constructed and positioned HamsterEntity, ready to be spawned.
     */
    protected HamsterEntity popOut(boolean success) {
        if (this.getWorld().isClient()) return null;
        ServerWorld serverWorld = (ServerWorld) this.getWorld();

        unregisterOccupancy();

        // Calculate exit point
        BlockPos startPoint = this.getBlockPos();
        BlockPos exitPos;

        if (this.forcedExitPos != null) {
            exitPos = this.forcedExitPos;
        } else {
            exitPos = TreeHeistUtil.findExitPosition(this.getWorld(), startPoint);
        }

        // Entity reconstruction
        HamsterEntity newHamster = ModEntities.HAMSTER.get().create(serverWorld);
        if (newHamster != null) {
            newHamster.readNbt(this.hamsterNbt);
            newHamster.setFallFlyImmunityTicks(0);

            // Calculate exit yaw
            float exitYaw;
            if (this.forcedExitYaw != null) {
                exitYaw = this.forcedExitYaw;
            } else if (this.anchorPos != null && (exitPos.getX() != this.anchorPos.getX() || exitPos.getZ() != this.anchorPos.getZ())) {
                double dx = exitPos.getX() - this.anchorPos.getX();
                double dz = exitPos.getZ() - this.anchorPos.getZ();
                exitYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            } else {
                exitYaw = this.random.nextFloat() * 360.0F;
            }

            // Apply position
            newHamster.refreshPositionAndAngles(
                    exitPos.getX() + 0.5,
                    exitPos.getY() + 0.1,
                    exitPos.getZ() + 0.5,
                    exitYaw,
                    0
            );

            // Apply velocity
            if (success) {
                Vec3d forward = Vec3d.fromPolar(0, exitYaw).normalize().multiply(0.4);
                newHamster.setVelocity(forward.x, 0.3, forward.z);
            } else {
                newHamster.setVelocity(Vec3d.ZERO);
            }

            // Clear stale states
            newHamster.setKnockedOut(false);
            newHamster.setSitting(false);
            newHamster.setHiding(false);
            newHamster.setActiveCustomGoalName("None");

            newHamster.velocityDirty = true;
        }

        return newHamster;
    }
}