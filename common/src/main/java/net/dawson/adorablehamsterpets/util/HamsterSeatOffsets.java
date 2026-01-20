package net.dawson.adorablehamsterpets.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Shared seat math for hamster passengers.
 *
 * <p>Provides a unified, scale-aware seat offset for client rendering (bone space) and a vertical correction
 * for server-side attachment/camera math.
 */
public final class HamsterSeatOffsets {
    private HamsterSeatOffsets() {}

    // --- Slope and Intercepts ---
    private static final double SEAT_Y_INTERCEPT = 0.12;
    private static final double SEAT_Y_SLOPE = -0.56;

    private static final double SEAT_Z_INTERCEPT = 0.0875;
    private static final double SEAT_Z_SLOPE = 0.3125;

    // --- Global Fine-Tuning ---
    private static final double SEAT_Y_NUDGE = -0.02; // More negative = down
    private static final double SEAT_Z_NUDGE = 0.02; // More positive = backwards

    /**
     * Vertical seat correction for server-side attachment/camera.
     * <p>Returns the passenger-only Y correction, normalized by {@code mountScale} so server attachment stays consistent
     * with the render path (which cancels mount scale before applying visual offsets).
     */
    public static double physicsSeatAdjustY(LivingEntity passenger, float mountScale) {
        // 1.20.1 Polyfill: Calculate scale (Current Height / Base Type Height)
        float baseHeight = passenger.getType().getDimensions().height;
        float s = (baseHeight > 0) ? passenger.getHeight() / baseHeight : 1.0f;

        return seatY(s) / Math.max(mountScale, 1.0e-6f);
    }

    /**
     * Visual seat offset for bone-locked rendering.
     * <p>Mount-aware in Y: the intercept scales with {@code mountScale} to keep the rider on top of larger mounts,
     * while the passenger term is passenger-only. Z is visual-only.
     */
    public static Vec3d visualSeatOffset(LivingEntity passenger, float mountScale) {
        // 1.20.1 Polyfill: Calculate scale (Current Height / Base Type Height)
        float baseHeight = passenger.getType().getDimensions().height;
        float s = (baseHeight > 0) ? passenger.getHeight() / baseHeight : 1.0f;

        return new Vec3d(0.0, seatY(s, mountScale), seatZ(s));
    }
    /**
     * Mount-aware Y seat curve (blocks).
     * <p>Separates mount geometry from rider size: intercept scales with {@code mountScale} (mount lift),
     * while the slope scales with {@code passengerScale} (feet-origin correction).
     */
    private static double seatY(float passengerScale, float mountScale) {
        // Intercept = mount-side lift, so it scales with mount size
        // Slope = passenger-side correction, so it stays passenger-only
        return (SEAT_Y_INTERCEPT * mountScale) + (SEAT_Y_SLOPE * passengerScale) + SEAT_Y_NUDGE;
    }

    /**
     * Canonical passenger-only Y curve (blocks).
     * Used for physics/camera math; does not incorporate mount scaling.
     */
    private static double seatY(float passengerScale) {
        return (SEAT_Y_INTERCEPT + (SEAT_Y_SLOPE * passengerScale)) + SEAT_Y_NUDGE;
    }

    /**
     * Canonical passenger-only Z curve (blocks).
     * Visual-only; do not apply in server attachment or the camera will orbit as the mount yaws.
     */
    private static double seatZ(float passengerScale) {
        return SEAT_Z_INTERCEPT + (SEAT_Z_SLOPE * passengerScale) + SEAT_Z_NUDGE;
    }
}
