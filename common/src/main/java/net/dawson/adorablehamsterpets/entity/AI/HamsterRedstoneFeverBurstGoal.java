package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterPlacementUtil;
import net.dawson.adorablehamsterpets.util.RedstoneFeverUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public final class HamsterRedstoneFeverBurstGoal extends Goal {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final double BURST_NAVIGATION_SPEED = 1.3D;
    private static final double BURST_ORBIT_RADIUS = 2.0D;
    private static final int MAX_CONSECUTIVE_WAYPOINT_FAILURES = 8;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ──────────────────────────────────────────────────────────────────────────────*/

    private final HamsterEntity hamster;
    private int cooldownTicks;
    private int remainingTicks;
    private int consecutiveWaypointFailures;
    private Vec3d anchor = Vec3d.ZERO;
    private double angle;
    private double direction;

    /* ────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ─────────────────────────────────────────────────────────────────────────────*/

    public HamsterRedstoneFeverBurstGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
        this.scheduleNext();
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ──────────────────────────────────────────────────────────────────────────────*/

    @Override
    public boolean canStart() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }
        return this.hamster.hasRedstoneFever()
                && Configs.AHP_MAIN.enableRedstoneFeverEnergyBursts
                && !HamsterMovementUtil.shouldNotMove(this.hamster)
                && this.hamster.isOnGround()
                && !this.hamster.isTouchingWater()
                && !this.hamster.isInLava()
                && this.hamster.isAlive()
                && !this.hamster.isRemoved();
    }

    @Override
    public boolean shouldContinue() {
        return this.remainingTicks > 0
                && this.hamster.hasRedstoneFever()
                && Configs.AHP_MAIN.enableRedstoneFeverEnergyBursts
                && !HamsterMovementUtil.shouldNotMove(this.hamster)
                && this.isAnchorStable()
                && !this.hamster.isTouchingWater()
                && !this.hamster.isInLava()
                && this.hamster.isAlive()
                && !this.hamster.isRemoved();
    }

    @Override
    public void start() {
        // --- 1. Lock Burst Shape ---
        this.anchor = this.hamster.getPos();
        this.angle = this.hamster.getRandom().nextDouble() * Math.PI * 2.0D;
        this.direction = this.hamster.getRandom().nextBoolean() ? 1.0D : -1.0D;
        this.consecutiveWaypointFailures = 0;
        this.hamster.setRedstoneFeverBurstActive(true);

        // --- 2. Normalize Duration Settings ---
        int min = Math.min(
                Configs.AHP_MAIN.redstoneFeverMinBurstDurationSeconds.get(),
                Configs.AHP_MAIN.redstoneFeverMaxBurstDurationSeconds.get());
        int max = Math.max(
                Configs.AHP_MAIN.redstoneFeverMinBurstDurationSeconds.get(),
                Configs.AHP_MAIN.redstoneFeverMaxBurstDurationSeconds.get());
        this.remainingTicks = this.hamster.getRandom().nextBetween(min, max) * 20;

        // --- 3. Present Burst ---
        this.hamster.triggerAnimOnServer(
                "mainController", "anim_hamster_freak_out");
        SoundEvent hiss = ModSounds.getRandomSoundFrom(
                ModSounds.HAMSTER_HISS_SOUNDS, this.hamster.getRandom());
        if (hiss != null) {
            this.hamster.playSound(hiss, 0.4F, this.hamster.getSoundPitch());
        }
        RedstoneFeverUtil.spawnRedstoneParticles(this.hamster, 24, 0.2F);
    }

    @Override
    public void tick() {
        // --- 1. Advance Orbit ---
        this.remainingTicks--;
        Vec3d orbitTarget = orbitTarget(this.anchor, this.angle);

        if (this.remainingTicks % 4 != 0 && !this.hamster.getNavigation().isIdle()) return;
        this.angle += this.direction * Math.toRadians(38.0D);

        // --- 2. Resolve Safe Waypoint ---
        BlockPos idealCell = BlockPos.ofFloored(orbitTarget);
        if (!HamsterPlacementUtil.isSafeSpawnLocation(
                idealCell, this.hamster.getWorld(), this.hamster)) {
            this.consecutiveWaypointFailures++;
            if (this.consecutiveWaypointFailures >= MAX_CONSECUTIVE_WAYPOINT_FAILURES) {
                this.remainingTicks = 0;
            }
            return;
        }

        // --- 3. Navigate ---
        this.consecutiveWaypointFailures = 0;
        this.hamster.getNavigation().startMovingTo(
                orbitTarget.x, orbitTarget.y, orbitTarget.z, BURST_NAVIGATION_SPEED);
    }

    @Override
    public void stop() {
        this.remainingTicks = 0;
        this.hamster.getNavigation().stop();
        this.hamster.setRedstoneFeverBurstActive(false);
        this.scheduleNext();
    }

    /* ───────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ─────────────────────────────────────────────────────────────────────────────────*/

    private void scheduleNext() {
        // Normalize reversed config pairs before random selection
        int[] intervalBounds = normalizeIntervalBounds(
                Configs.AHP_MAIN.redstoneFeverMinBurstIntervalSeconds.get(),
                Configs.AHP_MAIN.redstoneFeverMaxBurstIntervalSeconds.get());
        // Recovery stretches intervals without disabling bursts before full cure
        double severity = this.hamster.hasRedstoneFever() ? RedstoneFeverUtil.getSeverity(this.hamster) : 1.0D;
        int baseSeconds = this.hamster.getRandom().nextBetween(intervalBounds[0], intervalBounds[1]);
        this.cooldownTicks = (int) Math.round(baseSeconds * 20.0D / Math.max(0.1D, severity));
    }

    static int[] normalizeIntervalBounds(int first, int second) {
        return new int[] {Math.min(first, second), Math.max(first, second)};
    }

    private boolean isAnchorStable() {
        return isAnchorStable(this.hamster.getPos(), this.anchor);
    }

    static Vec3d orbitTarget(Vec3d anchor, double angle) {
        return new Vec3d(
                anchor.x + Math.cos(angle) * BURST_ORBIT_RADIUS,
                anchor.y,
                anchor.z + Math.sin(angle) * BURST_ORBIT_RADIUS);
    }

    static boolean isAnchorStable(Vec3d position, Vec3d anchor) {
        return (position.x - anchor.x) * (position.x - anchor.x) + (position.z - anchor.z) * (position.z - anchor.z) <= (BURST_ORBIT_RADIUS + 1.5D) * (BURST_ORBIT_RADIUS + 1.5D);
    }
}
