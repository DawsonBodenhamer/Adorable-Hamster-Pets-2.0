package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpMainConfig;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Handles complex rider input mapping and movement physics for mountable hamsters.
 */
public final class HamsterRidingUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int RIDER_JUMP_COOLDOWN_TICKS = 8;
    private static final double RIDER_JUMP_VELOCITY = 0.6D; // ~2 blocks

    private HamsterRidingUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *                           Mounting & State
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- 1. Mount Execution ---
    /**
     * Forces stand up and disables wander mode for full control
     */
    public static void putPlayerOnBack(HamsterEntity hamster, PlayerEntity player) {
        if (!hamster.hasPassenger(player)) {
            player.startRiding(hamster);
            hamster.setSitting(false, false);

            if (hamster.isOwner(player)) {
                hamster.setWanderModeActive(false);
            }
        }
    }

    // --- 2. Controller Validation ---
    /**
     * Only allows steering if tamed and the passenger is the owner
     */
    @Nullable
    public static LivingEntity getControllingPassenger(HamsterEntity hamster) {
        if (hamster.isTamed()) {
            Entity firstPassenger = hamster.getFirstPassenger();
            if (firstPassenger instanceof LivingEntity passenger && hamster.isOwner(passenger)) {
                return passenger;
            }
        }
        return null;
    }

    // --- 3. Input State Mapping ---
    /**
     * Updates the input state from the rider.
     * Called by both the Server (via packet) and Client (via prediction).
     */
    public static void setRiderInput(HamsterEntity hamster, boolean jump, boolean sprint) {
        // Rising edge logic for jump
        if (jump && !hamster.isRiderJumpHeld()) {
            hamster.setRiderJumpQueued(true);

            // Only log on server to avoid console spam
            if (!hamster.getWorld().isClient()) {
                AdorableHamsterPets.LOGGER.info("[AHP JUMP][SERVER] hamsterId={} queuedJump=true", hamster.getId());
            }
        }
        hamster.setRiderJumpHeld(jump);
        hamster.setRiderSprintHeld(sprint);
    }

    // --- 4. State Reset ---
    /**
     * Clears all driving states when the pilot dismounts to prevent
     * "sticky" inputs or visual glitches after dismounting.
     */
    public static void onPassengerRemoved(HamsterEntity hamster, Entity passenger, Entity controller) {
        if (passenger == controller) {
            hamster.setRiderJumpCooldown(0);
            hamster.setRiderJumpHeld(false);
            hamster.setRiderSprintHeld(false);
            hamster.setRiderJumpQueued(false);
            hamster.setSprinting(false);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            Physics & Movement
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- 5. Main Movement Execution ---
    /**
     * Manages movement physics and rider inputs.
     * <p>
     * Synchronizes rotation, calculates speed based on config settings, and executes jump
     * logic on both the Client (for prediction) and Server (for sound/authority).
     */
    public static boolean handleTravel(HamsterEntity hamster, Vec3d movementInput) {
        if (!hamster.isAlive()) return false;

        LivingEntity passenger = hamster.getControllingPassenger();
        if (!hamster.isTamed() || !(passenger instanceof PlayerEntity player)) {
            return false;
        }

        // Sync Mount Rotation to Rider
        hamster.setYaw(player.getYaw());
        hamster.prevYaw = hamster.getYaw();
        hamster.setPitch(player.getPitch() * 0.5F);
        hamster.delegateSetRotation(hamster.getYaw(), hamster.getPitch());
        hamster.bodyYaw = hamster.getYaw();
        hamster.headYaw = hamster.bodyYaw;

        // Read Rider Movement Input
        float forwardSpeed = player.forwardSpeed;
        float sidewaysSpeed = player.sidewaysSpeed;

        // Backward movement penalty
        if (forwardSpeed <= 0.0F) {
            forwardSpeed *= 0.25F;
        }

        // Calculate Sprint State (Requires physical movement)
        boolean hasMovement = Math.abs(forwardSpeed) > 1.0e-5 || Math.abs(sidewaysSpeed) > 1.0e-5;
        boolean isSprinting = hamster.isRiderSprintHeld() && hasMovement;

        // Sync the visual sprinting state (particles/FOV)
        hamster.setSprinting(isSprinting);

        // Calculate Speed Multipliers
        final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;
        double speedMultiplier = isSprinting
                ? config.ridingSprintSpeedMultiplier.get()
                : config.ridingBaseSpeedMultiplier.get();

        float attributeSpeed = (float) hamster.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        float finalSpeed = (float) (attributeSpeed * speedMultiplier);

        // Apply Potion Effects (Additive on top of multiplier)
        if (hamster.hasStatusEffect(StatusEffects.SPEED)) {
            finalSpeed += 0.1f;
        }
        hamster.setMovementSpeed(finalSpeed);

        // Process Jump Logic (Before travel for smooth integration)
        if (hamster.getRiderJumpCooldown() > 0) {
            hamster.setRiderJumpCooldown(hamster.getRiderJumpCooldown() - 1);
        } else if (hamster.isRiderJumpQueued()) {
            hamster.setRiderJumpQueued(false); // consume
            tryRiderJump(hamster);
        }

        // Execute Movement Logic based on side
        if (hamster.isLogicalSideForUpdatingMovement()) {
            hamster.delegateTravel(new Vec3d(sidewaysSpeed, 0.0, forwardSpeed));
        } else if (player instanceof ClientPlayerEntity) {
            hamster.delegateTravel(new Vec3d(sidewaysSpeed, 0.0, forwardSpeed));
        }

        return true;
    }

    // --- 6. Jump Physics Execution ---
    /**
     * Executes the jump logic for a ridden hamster.
     * <p>
     * Validates ground state, applies vertical velocity, triggers the
     * jump cooldown, and plays a bounce sound.
     */
    private static void tryRiderJump(HamsterEntity hamster) {
        if (!hamster.isOnGround() || hamster.isTouchingWater() || hamster.isInLava()) {
            return;
        }

        // Triggers the protected LivingEntity.jump() via wrapper
        hamster.executeJump();

        // Enforce exact jump height
        Vec3d v = hamster.getVelocity();
        hamster.setVelocity(v.x, RIDER_JUMP_VELOCITY, v.z);
        hamster.velocityDirty = true;
        hamster.fallDistance = 0.0F;

        // Play Bounce Sound
        PlayerEntity rider = (hamster.getControllingPassenger() instanceof PlayerEntity p) ? p : null;
        float randomPitch = 1.2f + (hamster.getRandom().nextFloat() * 0.4f - 0.2f);
        hamster.getWorld().playSound(
                rider, hamster.getX(), hamster.getY(), hamster.getZ(),
                ModSounds.HAMSTER_BOUNCE.get(), SoundCategory.PLAYERS,
                0.6f, randomPitch
        );

        hamster.setRiderJumpCooldown(RIDER_JUMP_COOLDOWN_TICKS);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                               Seat Offsets
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
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
            float s = passenger.getScale();
            return seatY(s) / Math.max(mountScale, 1.0e-6f);
        }

        /**
         * Visual seat offset for bone-locked rendering.
         * <p>Mount-aware in Y: the intercept scales with {@code mountScale} to keep the rider on top of larger mounts,
         * while the passenger term is passenger-only. Z is visual-only.
         */
        public static Vec3d visualSeatOffset(LivingEntity passenger, float mountScale) {
            float s = passenger.getScale();
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
}