package net.dawson.adorablehamsterpets.client.gui.widgets;

import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.config.IconPositionPreset;
import net.minecraft.util.math.MathHelper;

/**
 * A client-side singleton that manages the animation state for the announcement icon.
 * This centralizes all interpolation and state logic for scale, rotation, and position,
 * ensuring consistent animations whether the icon is rendered on the HUD or as a screen widget.
 */
public class AnnouncementIconAnimator {

    public static final AnnouncementIconAnimator INSTANCE = new AnnouncementIconAnimator();

    // --- Animation Constants ---
    private static final float HOVER_SCALE = 1.3f;
    private static final float CLICK_SCALE = 0.9f;
    private static final float IDLE_SCALE = 1.0f;
    private static final int WIGGLE_INTERVAL_TICKS = 80;
    private static final int WIGGLE_DURATION_TICKS = 5;
    private static final float WIGGLE_MAX_ANGLE_DEGREES = 50.0f;
    private static final int CLICK_ANIMATION_DURATION_TICKS = 2;

    // --- Main Physics Constants ---
    private static final double STIFFNESS = 3.0;               // Spring stiffness; higher = faster snap to target
    private static final double DAMPING = 4.5;                 // Damping; higher = less bounce/overshoot
    private static final double MASS = 7.0;                    // Effective mass; resists acceleration
    private static final float ROTATION_KICK_INTENSITY = 0.2f; // Extra rotational impulse on move

    // --- Settle-Wobble Physics Constants ---
    private static final int WOBBLE_DURATION_TICKS = 30;                // Cosmetic wobble length (ticks)
    private static final float WOBBLE_DECAY_POWER = 2.7f;               // >2.0 decays faster; 1.0 = linear
    private static final float WOBBLE_OSCILLATIONS = 12.0f;             // Total oscillations during the wobble window
    private static final float WOBBLE_BASE_VEL_MULTIPLIER = 50.0f;      // Big constant; we clamp after this
    private static final float WOBBLE_MAX_ICON_FRACTION   = 0.35f;      // Max wobble = 35% of icon's rendered size
    private static final float WOBBLE_MIN_ICON_FRACTION   = 0.08f;      // Floor = 8% of icon size for tiny moves


    // --- State Fields ---
    private double currentX, currentY, currentScale = 1.0, currentAngle = 0.0; // Current state (per tick)
    private double prevX, prevY, prevScale = 1.0, prevAngle = 0.0;             // Previous state (for lerp)
    private double velocityX, velocityY, scaleVelocity, angularVelocity = 0.0; // State velocities
    // Cached UI metric for resolution-independent wobble clamping
    private float iconPixelSize = 16f;     // 16 * hud scale; updated in updateTargetPosition(...)

    // --- Scale State Fields ---
    private double targetX, targetY;
    private float targetScale = 1.0f;
    private float targetAngle = 0.0f;

    private float wiggleAngle = 0.0f;
    private int wiggleTimer = 0;
    private int clickAnimationTicks = 0;

    private int settleWobbleTicks = 0;
    private double wobbleAngle = 0;     // The direction of the wobble in radians
    private double wobbleMagnitude = 0; // The current distance of the wobble from the center
    private double lastVelocityX = 0;   // Velocity from the previous tick
    private double lastVelocityY = 0;   // Velocity from the previous tick

    /**
     * Stores the partial tick value from the last render frame. This is used to
     * compute the time delta between frames and advance the physics simulation
     * proportionally. On every new tick, this value is reset to 0. See
     * {@link #updatePhysicsForRender(float)}.
     */
    private float lastRenderDelta = 0.0f;

    /**
     * Advances the physics simulation by a fractional number of ticks. This allows
     * the spring-damper system to update on every render frame rather than only
     * once per game tick. The existing constants (stiffness, damping, mass)
     * assume a fixed-time step of one tick; by scaling the acceleration and
     * velocity updates by {@code delta}, the animator can smoothly animate
     * between ticks without noticeable jumps. This method should be called once
     * per render with the current tick delta. The method will internally
     * determine how much time has elapsed since the previous render call and
     * integrate the physics accordingly.
     *
     * @param tickDelta The current partial tick value passed to the render method. A value
     *                  between 0.0 and 1.0 indicating how far we are into the current
     *                  game tick.
     */
    public void updatePhysicsForRender(float tickDelta) {
        // Compute how much time (in ticks) has passed since the last render.
        // If the delta decreases, a new tick has started, so use the full delta.
        float deltaTicks = tickDelta - this.lastRenderDelta;
        if (deltaTicks < 0.0f) deltaTicks = tickDelta;
        this.lastRenderDelta = tickDelta;
        if (deltaTicks <= 0.0f) return;

        // Advance each physics property using the scaled time step. Perform Euler integration scaled by the fractional
        // tick value to approximate continuous motion. Updating in this manner between ticks prevents the icon from
        // appearing choppy on high-refresh-rate displays or while certain GUI screens are open.
        // --- Capture pre-step state for overshoot detection ---
        final double errX_before = this.currentX - this.targetX;
        final double errY_before = this.currentY - this.targetY;
        final double velX_before = this.velocityX;
        final double velY_before = this.velocityY;

        // --- Advance physics ---
        updatePhysicsPropertyTimed(this.currentX, this.velocityX, this.targetX, deltaTicks, (val, vel) -> { this.currentX = val; this.velocityX = vel; });
        updatePhysicsPropertyTimed(this.currentY, this.velocityY, this.targetY, deltaTicks, (val, vel) -> { this.currentY = val; this.velocityY = vel; });
        updatePhysicsPropertyTimed(this.currentScale, this.scaleVelocity, this.targetScale, deltaTicks, (val, vel) -> { this.currentScale = val; this.scaleVelocity = vel; });
        updatePhysicsPropertyTimed(this.currentAngle, this.angularVelocity, this.targetAngle, deltaTicks, (val, vel) -> { this.currentAngle = val; this.angularVelocity = vel; });

        // Also update the lastVelocity fields so the settle wobble logic continues to observe the most recent motion.
        // These velocities are captured immediately prior to each tick's physics step in tick(boolean),
        // but need to update them here as well so the wobble triggers based on the continuous motion.
        this.lastVelocityX = this.velocityX;
        this.lastVelocityY = this.velocityY;

        // --- Overshoot-triggered wobble ---
        if (this.settleWobbleTicks == 0) {
            final double errX_after = this.currentX - this.targetX;
            final double errY_after = this.currentY - this.targetY;

            // True if crossed the target on either axis in this frame
            boolean crossedX = (errX_before == 0.0) ? false : (errX_before * errX_after < 0.0);
            boolean crossedY = (errY_before == 0.0) ? false : (errY_before * errY_after < 0.0);
            boolean crossed   = crossedX || crossedY;

            double speed_before = Math.sqrt(velX_before * velX_before + velY_before * velY_before);

            if (crossed) {
                this.settleWobbleTicks = WOBBLE_DURATION_TICKS;
                // Kick opposite the incoming velocity to simulate bounce
                this.wobbleAngle = Math.atan2(-velY_before, -velX_before);
                // Compute a big “raw” kick from speed, then clamp in icon-relative units
                float rawKick = (float)(speed_before * WOBBLE_BASE_VEL_MULTIPLIER);

                // Clamp to [min, max] fractions of the icon's rendered size
                float maxPx = WOBBLE_MAX_ICON_FRACTION * this.iconPixelSize;
                float minPx = WOBBLE_MIN_ICON_FRACTION * this.iconPixelSize;

                this.wobbleMagnitude = Math.max(minPx, Math.min(rawKick, maxPx));
            }
        }
    }

    /**
     * Performs a physics step scaled by {@code delta}. This is analogous to
     * {@link #updatePhysicsProperty(double, double, double, PropertyUpdater)} but
     * multiplies the acceleration, velocity and position updates by the given
     * fractional tick value. Using this helper allows the same spring and
     * damping constants to be reused for both full-tick and partial-tick
     * integration.
     *
     * @param current  The current value of the property (e.g., position or scale).
     * @param velocity The current velocity of the property.
     * @param target   The target value the property is moving towards.
     * @param delta    The fraction of a tick to simulate. A value of 1.0
     *                 corresponds to a full tick, while values less than 1.0
     *                 represent partial ticks.
     * @param updater  A lambda function that updates the calling fields with the new
     *                 value and velocity.
     */
    private void updatePhysicsPropertyTimed(double current, double velocity, double target, double delta, PropertyUpdater updater) {
        // F_spring = -k * x (Hooke's Law)
        double springForce = -STIFFNESS * (current - target);
        // F_damping = -c * v
        double dampingForce = -DAMPING * velocity;
        // a = F / m (Newton's Second Law)
        double acceleration = (springForce + dampingForce) / MASS;
        // Euler integration with scaled time step
        double newVelocity = velocity + acceleration * delta;
        double newValue = current + newVelocity * delta;
        updater.update(newValue, newVelocity);
    }

    private AnnouncementIconAnimator() {}

    /**
     * Calculates and sets the animator's target X and Y coordinates based on the current
     * screen dimensions and the user's HUD configuration settings. This centralized method
     * is used by both the in-game HUD renderer and the Title Screen widget to ensure
     * consistent positioning.
     *
     * @param screenWidth  The current width of the screen.
     * @param screenHeight The current height of the screen.
     */
    public void updateTargetPosition(int screenWidth, int screenHeight) {
        final AhpConfig config = Configs.AHP;
        IconPositionPreset preset = config.hudIconPositionPreset.get();
        double scale = config.hudIconScale.get();
        int iconWidth = 16;
        int iconHeight = 16;

        // Cache for wobble clamping
        this.iconPixelSize = (float) (Math.max(iconWidth, iconHeight) * scale);

        double newTargetX = switch (preset) {
            case TOP_LEFT, BOTTOM_LEFT -> (double) config.hudIconOffsetX.get();
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - (iconWidth * scale) - config.hudIconOffsetX.get();
        };

        double newTargetY = switch (preset) {
            case TOP_LEFT, TOP_RIGHT -> (double) config.hudIconOffsetY.get();
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - (iconHeight * scale) - config.hudIconOffsetY.get();
        };

        // Only start a new physics transition if the target has actually changed.
        if (newTargetX != this.targetX || newTargetY != this.targetY) {
            startTransition(newTargetX, newTargetY);
        }
    }

    /**
     * Updates the physics simulation and discrete animation states. Should be called once per client tick.
     * All visual smoothing is handled in the getRender...() methods.
     * @param isGuiOpen True if any GUI screen is currently open.
     */
    public void tick(boolean isGuiOpen) {
        // --- 1. Store previous state for interpolation ---
        this.prevX = this.currentX;
        this.prevY = this.currentY;
        this.prevScale = this.currentScale;
        this.prevAngle = this.currentAngle;

        // Reset the last render delta so that the first render call of the new tick
        // treats its delta as absolute rather than a difference from the previous frame.
        this.lastRenderDelta = 0.0f;

        // --- 2. Update discrete timers ---
        if (clickAnimationTicks > 0) {
            clickAnimationTicks--;
            targetScale = CLICK_SCALE; // Force target scale during click
        }

        wiggleTimer++;
        if (wiggleTimer > WIGGLE_INTERVAL_TICKS + WIGGLE_DURATION_TICKS) {
            wiggleTimer = 0;
        }

        // --- 3. Settle Wobble Logic ---
        if (settleWobbleTicks > 0) {
            settleWobbleTicks--;
        }

        // --- 4. Calculate Dynamic Targets ---
        // The target angle is dynamically driven by the icon's horizontal velocity.
        // This creates the rotational "wobble" as it moves and settles.
        this.targetAngle = (float) (this.velocityX * ROTATION_KICK_INTENSITY);

        // --- 5. Store final velocity BEFORE running the new physics step ---
        this.lastVelocityX = this.velocityX;
        this.lastVelocityY = this.velocityY;

        // --- 6. Run the physics simulation for a full tick ---
        // Instead of calling updatePhysicsProperty directly for each property (which
        // assumes a fixed time step), delegate to the timed integration helper with
        // delta = 1.0f. This allows the same spring-damper constants to be reused
        // when the simulation is advanced at fractional tick intervals during
        // rendering. See updatePhysicsForRender for the partial updates.
        updatePhysicsPropertyTimed(this.currentX, this.velocityX, this.targetX, 1.0f, (val, vel) -> { this.currentX = val; this.velocityX = vel; });
        updatePhysicsPropertyTimed(this.currentY, this.velocityY, this.targetY, 1.0f, (val, vel) -> { this.currentY = val; this.velocityY = vel; });
        updatePhysicsPropertyTimed(this.currentScale, this.scaleVelocity, this.targetScale, 1.0f, (val, vel) -> { this.currentScale = val; this.scaleVelocity = vel; });
        updatePhysicsPropertyTimed(this.currentAngle, this.angularVelocity, this.targetAngle, 1.0f, (val, vel) -> { this.currentAngle = val; this.angularVelocity = vel; });
    }

    // --- Public Methods to Control State ---

    public void startTransition(double newTargetX, double newTargetY) {
        this.targetX = newTargetX;
        this.targetY = newTargetY;
    }

    public void triggerClickAnimation() {
        this.clickAnimationTicks = CLICK_ANIMATION_DURATION_TICKS;
        this.targetScale = CLICK_SCALE;
    }

    public void setHovered(boolean hovered) {
        // Do not change the target scale if a click animation is active
        if (clickAnimationTicks == 0) {
            this.targetScale = hovered ? HOVER_SCALE : IDLE_SCALE;
        }
    }

    public double getTargetX() {
        return this.targetX;
    }

    public double getTargetY() {
        return this.targetY;
    }

    /**
     * A functional interface to update a property's value and velocity.
     */
    @FunctionalInterface
    private interface PropertyUpdater {
        void update(double value, double velocity);
    }

    /**
     * Calculates the next state of a physical property using a spring-damper model.
     * @param current The current value of the property (e.g., position, scale).
     * @param velocity The current velocity of the property.
     * @param target The target value the property is moving towards.
     * @param updater A lambda function to update the original fields with the new values.
     */
    private void updatePhysicsProperty(double current, double velocity, double target, PropertyUpdater updater) {
        // F_spring = -k * x (Hooke's Law)
        double springForce = -STIFFNESS * (current - target);
        // F_damping = -c * v
        double dampingForce = -DAMPING * velocity;

        // a = F / m (Newton's Second Law)
        double acceleration = (springForce + dampingForce) / MASS;

        // Update velocity and position
        double newVelocity = velocity + acceleration;
        double newValue = current + newVelocity;

        updater.update(newValue, newVelocity);
    }

    /**
     * Instantly updates the icon's target and current position. Used by the HUD renderer
     * when no GUI is open to keep the icon locked to its calculated corner position and
     * prime it for a smooth transition when a GUI opens.
     *
     * @param x The target X position.
     * @param y The target Y position.
     */
    public void updateHudPosition(double x, double y) {
        // Set the target for the physics simulation.
        this.targetX = x;
        this.targetY = y;

        // Instantly snap the current and previous positions to the target.
        // This prevents any visual lag or physics simulation while on the HUD.
        this.currentX = x;
        this.currentY = y;
        this.prevX = x;
        this.prevY = y;

        // Reset velocities to prevent any residual drift from a previous transition.
        this.velocityX = 0;
        this.velocityY = 0;
    }

    // --- Getters for Rendering (with tickDelta interpolation) ---

    /**
     * Gets the interpolated scale for the current frame.
     * @param tickDelta The fraction of a tick that has passed.
     * @return The smoothly interpolated scale.
     */
    public float getRenderScale(float tickDelta) {
        // Advance the physics simulation up to the current partial tick. This call
        // ensures that the underlying state has progressed smoothly between
        // discrete ticks.
        updatePhysicsForRender(tickDelta);
        return (float) this.currentScale;
    }

    /**
     * Gets the interpolated wiggle and physics-driven angle for the current frame.
     * @param tickDelta The fraction of a tick that has passed.
     * @return The smoothly interpolated angle.
     */
    public float getRenderAngle(float tickDelta) {
        // Advance the physics simulation up to the current partial tick.
        updatePhysicsForRender(tickDelta);
        // Use the current angle directly since the simulation has been updated.
        float physicsAngle = (float) this.currentAngle;
        // Maintain the cosmetic wiggle on top of the physics-driven rotation.
        float wiggleTarget = 0.0f;
        if (wiggleTimer > WIGGLE_INTERVAL_TICKS) {
            float progress = (wiggleTimer - WIGGLE_INTERVAL_TICKS + tickDelta) / (float) WIGGLE_DURATION_TICKS;
            wiggleTarget = MathHelper.sin(progress * (float) Math.PI * 2.0f) * WIGGLE_MAX_ANGLE_DEGREES;
        }
        this.wiggleAngle += (wiggleTarget - this.wiggleAngle) * 0.4f * tickDelta;
        return physicsAngle + this.wiggleAngle;
    }

    /**
     * Gets the interpolated X position for the current frame, including the cosmetic settle wobble.
     * @param tickDelta The fraction of a tick that has passed.
     * @return The smoothly interpolated X position.
     */
    public double getRenderX(float tickDelta) {
        // Advance the physics simulation up to the current partial tick.
        updatePhysicsForRender(tickDelta);
        double physicalX = this.currentX;
        if (this.settleWobbleTicks > 0) {
            float progress = (WOBBLE_DURATION_TICKS - (this.settleWobbleTicks - tickDelta)) / (float) WOBBLE_DURATION_TICKS;
            double decay = Math.pow(1.0 - progress, WOBBLE_DECAY_POWER);
            double sineWave = Math.sin(progress * Math.PI * 2.0 * WOBBLE_OSCILLATIONS);
            return physicalX + Math.cos(this.wobbleAngle) * this.wobbleMagnitude * sineWave * decay;
        }
        return physicalX;
    }

    /**
     * Gets the interpolated Y position for the current frame, including the cosmetic settle wobble.
     * @param tickDelta The fraction of a tick that has passed.
     * @return The smoothly interpolated Y position.
     */
    public double getRenderY(float tickDelta) {
        // Advance the physics simulation up to the current partial tick.
        updatePhysicsForRender(tickDelta);
        double physicalY = this.currentY;
        if (this.settleWobbleTicks > 0) {
            float progress = (WOBBLE_DURATION_TICKS - (this.settleWobbleTicks - tickDelta)) / (float) WOBBLE_DURATION_TICKS;
            double decay = Math.pow(1.0 - progress, WOBBLE_DECAY_POWER);
            double sineWave = Math.sin(progress * Math.PI * 2.0 * WOBBLE_OSCILLATIONS);
            return physicalY + Math.sin(this.wobbleAngle) * this.wobbleMagnitude * sineWave * decay;
        }
        return physicalY;
    }
}