package net.dawson.adorablehamsterpets.entity.AI.navigation;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A custom navigation implementation for hamsters that avoids unlinked {@link HamsterBedBlock}s whenever possible.
 */
public class HamsterNavigation extends MobNavigation {
    private final HamsterEntity hamster;

    // --- Detour State ---
    @Nullable private BlockPos avoidanceWaypoint = null;
    @Nullable private BlockPos waypointTargetSnapshot = null; // Target position when waypoint was chosen
    @Nullable private Entity waypointEntitySnapshot = null;   // Entity target snapshot
    @Nullable private BlockPos lastUnsafeTarget = null; // The last target that required a detour
    private int unsafeCycles = 0;

    // --- Tuning ---
    private static final int MAX_ALT_ATTEMPTS = 10;
    private static final int ALT_RADIUS = 4;
    private static final double WAYPOINT_REACH_DIST_SQ = 1.5 * 1.5;
    private static final double TARGET_MOVE_REPATH_DIST_SQ = 5 * 5;
    private static final int ALLOW_UNSAFE_AFTER_CYCLES = 6; // Last resort after repeated failures

    /**
     * Constructs a new HamsterNavigation component.
     * @param hamster The hamster entity this navigator belongs to.
     * @param world The world the entity is in.
     */
    public HamsterNavigation(HamsterEntity hamster, World world) {
        super(hamster, world);
        this.hamster = hamster;
        AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] HamsterNavigation constructed for hamster {}", hamster.getUuid());
    }

    // --- High-Level Routing Entrypoints ---
    /**
     * Starts the hamster moving towards a specific coordinate, planning a path that avoids unlinked beds.
     * If a safe detour (waypoint) is already active and still valid for the target, it will continue pathing
     * towards the waypoint. Otherwise, it plans a new path, potentially selecting a new waypoint if the direct
     * route is unsafe.
     *
     * @return {@code true} if a path was successfully started, {@code false} otherwise.
     */
    @Override
    public boolean startMovingTo(double x, double y, double z, double speed) {
        BlockPos target = BlockPos.ofFloored(x, y, z);
        AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] startMovingTo(xyz={}, speed={})  waypoint={}",
                target, speed, avoidanceWaypoint);

        // If there is a valid waypoint, stick to it
        if (isWaypointValidForTarget(target, null)) {
            Path wp = super.findPathTo(avoidanceWaypoint, 0);
            if (wp != null) {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Using existing waypoint {} toward {}", avoidanceWaypoint, target);
                return this.startMovingAlong(wp, speed);
            } else {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Waypoint {} no longer pathable; clearing", avoidanceWaypoint);
                clearWaypoint();
            }
        }

        // Plan a path; this call can select a new waypoint
        Path planned = planPathWithWaypoint(target, null);
        return planned != null && this.startMovingAlong(planned, speed);
    }

    /**
     * Starts the hamster moving towards a target entity, planning a path that avoids unlinked beds.
     * This method functions similarly to {@link #startMovingTo(double, double, double, double)}, but tracks the
     * target entity's movement to invalidate the detour waypoint if the entity moves too far from its
     * original position.
     *
     * @return {@code true} if a path was successfully started, {@code false} otherwise.
     */
    @Override
    public boolean startMovingTo(Entity entity, double speed) {
        BlockPos target = entity.getBlockPos();
        AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] startMovingTo(entity={}, speed={}) waypoint={}",
                entity.getName().getString(), speed, avoidanceWaypoint);

        if (isWaypointValidForTarget(target, entity)) {
            Path wp = super.findPathTo(avoidanceWaypoint, 0);
            if (wp != null) {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Using existing waypoint {} toward entity {}", avoidanceWaypoint, entity.getName().getString());
                return this.startMovingAlong(wp, speed);
            } else {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Waypoint {} no longer pathable; clearing", avoidanceWaypoint);
                clearWaypoint();
            }
        }

        Path planned = planPathWithWaypoint(target, entity);
        return planned != null && this.startMovingAlong(planned, speed);
    }

    /**
     * Finds a path to the given position. This method is intentionally not overridden for detour planning
     * to maintain compatibility with AI goals that call it directly for simple checks. All detour and
     * waypoint logic is handled within the {@code startMovingTo} methods.
     *
     * @return A path to the target, or {@code null} if no path is found.
     */
    @Nullable
    @Override
    public Path findPathTo(BlockPos pos, int range) {
        // Delegate to parent; control planning via startMovingTo
        return super.findPathTo(pos, range);
    }

    // --- Tick Logic ---
    @Override
    public void tick() {
        super.tick();
        // Clear waypoint when reached
        if (avoidanceWaypoint != null) {
            double d2 = this.hamster.squaredDistanceTo(
                    avoidanceWaypoint.getX() + 0.5,
                    avoidanceWaypoint.getY() + 0.1,
                    avoidanceWaypoint.getZ() + 0.5);
            if (d2 <= WAYPOINT_REACH_DIST_SQ) {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Reached waypoint {}; clearing", avoidanceWaypoint);
                clearWaypoint();
            }
        }
    }

    // --- Planning ---
    /**
     * Plans a path to the target, creating a detour waypoint if the direct path is unsafe.
     * If the direct path is blocked by an unlinked hamster bed, this method attempts to find a safe
     * alternative nearby. If a safe alternative is found, it is set as a persistent {@code avoidanceWaypoint}.
     * If no safe path can be found after several attempts for the same target, it will eventually allow the
     * unsafe direct path as a last resort to prevent the hamster from getting stuck.
     */
    @Nullable
    private Path planPathWithWaypoint(BlockPos targetPos, @Nullable Entity targetEntity) {
        AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] planPathWithWaypoint target={}", targetPos);

        Path direct = super.findPathTo(targetPos, 0);
        boolean directUnsafe = direct != null && hamster.isPathThroughUnlinkedBed(direct);

        // If the direct path is safe, reset everything.
        if (!directUnsafe) {
            hamster.pathingFailures = 0;
            hamster.lastFailedTarget = null;
            if (avoidanceWaypoint != null) {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Direct path safe; clearing previous waypoint {}", avoidanceWaypoint);
                clearWaypoint();
            }
            return direct;
        }

        // Increment failure count as soon as a detour is needed
        if (hamster.lastFailedTarget == null || !hamster.lastFailedTarget.equals(targetPos)) {
            hamster.pathingFailures = 1; // First failure for this target
            hamster.lastFailedTarget = targetPos.toImmutable();
        } else {
            hamster.pathingFailures++; // Subsequent failure for same target
        }

        // Check if it should give up before searching for a new waypoint
        if (hamster.pathingFailures >= ALLOW_UNSAFE_AFTER_CYCLES) {
            AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] No safe alternates after {} cycles; allowing direct unsafe path to {}", hamster.pathingFailures, targetPos);
            hamster.pathingFailures = 0;
            hamster.lastFailedTarget = null;
            clearWaypoint();
            return direct; // Allow crossing
        }

        // Try alternates around the target and pick the first safe one
        for (int i = 0; i < MAX_ALT_ATTEMPTS; i++) {
            int dx = hamster.getRandom().nextBetween(-ALT_RADIUS, ALT_RADIUS);
            int dz = hamster.getRandom().nextBetween(-ALT_RADIUS, ALT_RADIUS);
            BlockPos alt = targetPos.add(dx, 0, dz);

            Path altPath = super.findPathTo(alt, 0);
            boolean altUnsafe = altPath == null || hamster.isPathThroughUnlinkedBed(altPath);
            AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Alt attempt {}: {} → {} unsafe={}", i + 1, alt, (altPath == null ? "null" : ("len=" + altPath.getLength())), altUnsafe);

            if (!altUnsafe) {
                setWaypoint(alt, targetPos, targetEntity);
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Using safe waypoint {} toward {}", avoidanceWaypoint, targetPos);
                return altPath;
            }
        }

        AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] No safe alternates this cycle; will retry next tick; refusing unsafe path to {}", targetPos);
        return null; // Do nothing this tick; caller won't start moving
    }

    // --- Waypoint validity ---
    private boolean isWaypointValidForTarget(BlockPos currentTargetPos, @Nullable Entity currentEntityTarget) {
        if (avoidanceWaypoint == null) return false;

        // If target entity moved too far since choosing the waypoint, invalidate
        if (waypointEntitySnapshot != null && currentEntityTarget != null) {
            double movedSq = currentEntityTarget.squaredDistanceTo(
                    waypointEntitySnapshot.getX(), waypointEntitySnapshot.getY(), waypointEntitySnapshot.getZ());
            if (movedSq > TARGET_MOVE_REPATH_DIST_SQ) {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Target entity moved too far; invalidating waypoint {}", avoidanceWaypoint);
                clearWaypoint();
                return false;
            }
        }

        // If static target shifted too far, invalidate
        if (waypointTargetSnapshot != null) {
            if (waypointTargetSnapshot.getSquaredDistance(currentTargetPos) > TARGET_MOVE_REPATH_DIST_SQ) {
                AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Target position moved {}; invalidating waypoint {}",
                        waypointTargetSnapshot, avoidanceWaypoint);
                clearWaypoint();
                return false;
            }
        }

        // If a direct path to the current target is now safe, drop the waypoint
        Path directNow = super.findPathTo(currentTargetPos, 0);
        if (directNow != null && !hamster.isPathThroughUnlinkedBed(directNow)) {
            AdorableHamsterPets.LOGGER.trace("[AHP Nav Debug] Direct path became safe; clearing waypoint {}", avoidanceWaypoint);
            clearWaypoint();
            return false;
        }

        return true;
    }

    private void setWaypoint(BlockPos waypoint, BlockPos targetSnapshot, @Nullable Entity entitySnapshot) {
        this.avoidanceWaypoint = waypoint;
        this.waypointTargetSnapshot = targetSnapshot.toImmutable();
        this.waypointEntitySnapshot = entitySnapshot; // lightweight reference; used only for movement delta
    }

    private void clearWaypoint() {
        this.avoidanceWaypoint = null;
        this.waypointTargetSnapshot = null;
        this.waypointEntitySnapshot = null;
    }
}