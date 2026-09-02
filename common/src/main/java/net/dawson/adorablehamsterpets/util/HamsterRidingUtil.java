package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpMainConfig;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *                            Static Riding Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    // --- Mounting and State ---

    /**
     * Forces stand up and disables wander mode for full control
     */
    public static void putPlayerOnBack(HamsterEntity hamster, Player player) {
        if (!hamster.hasPassenger(player)) {
            player.startRiding(hamster);
            hamster.setSitting(false, false);

            if (hamster.isOwnedBy(player)) {
                hamster.setWanderModeActive(false);
            }
        }
    }

    /**
     * Only allows steering if tamed and the passenger is the owner
     */
    @Nullable
    public static LivingEntity getControllingPassenger(HamsterEntity hamster) {
        if (hamster.isTame()) {
            Entity firstPassenger = hamster.getFirstPassenger();
            if (firstPassenger instanceof LivingEntity passenger && hamster.isOwnedBy(passenger)) {
                return passenger;
            }
        }
        return null;
    }

    /**
     * Updates the input state from the rider.
     * Called by both the Server (via packet) and Client (via prediction).
     */
    public static void setRiderInput(HamsterEntity hamster, boolean jump, boolean sprint) {
        // Queue only the rising edge so held input cannot retrigger every tick.
        if (jump && !hamster.isRiderJumpHeld()) {
            hamster.setRiderJumpQueued(true);

            // Keep diagnostics server-side to avoid duplicate client prediction logs.
            if (!hamster.level().isClientSide()) {
                AdorableHamsterPets.LOGGER.info(
                        "[AHP JUMP][SERVER] hamsterId={} queuedJump=true", hamster.getId());
            }
        }
        hamster.setRiderJumpHeld(jump);
        hamster.setRiderSprintHeld(sprint);
    }

    /**
     * Clears all driving states when the pilot dismounts to prevent
     * "sticky" inputs or visual glitches after dismounting.
     */
    public static void onPassengerRemoved(
            HamsterEntity hamster, Entity passenger, Entity controller) {
        if (passenger == controller) {
            hamster.setRiderJumpCooldown(0);
            hamster.setRiderJumpHeld(false);
            hamster.setRiderSprintHeld(false);
            hamster.setRiderJumpQueued(false);
            hamster.setSprinting(false);
        }
    }

    // --- Passenger Attachment ---

    /**
     * Calculates the scale-aware position where a passenger attaches to the hamster.
     */
    public static Vec3 getPassengerAttachmentPos(HamsterEntity hamster, Entity passenger) {
        // Vehicle (hamster) height is already scaled at runtime.
        double baseY = hamster.getBbHeight() * 0.85;

        // Passenger-size compensation (applying the attachment scale again causes scale^2 offsets).
        double riderAdjustY =
                passenger instanceof LivingEntity living
                        ? HamsterSeatOffsets.physicsSeatAdjustY(living, hamster.getScale())
                        : 0.0;

        return new Vec3(0.0, baseY + riderAdjustY, 0.0);
    }

    // --- Physics and Movement ---

    /**
     * Manages movement physics and rider inputs.
     * <p>
     * Synchronizes rotation, calculates speed based on config settings, and executes jump
     * logic on both the Client (for prediction) and Server (for sound/authority).
     */
    public static boolean handleTravel(HamsterEntity hamster, Vec3 movementInput) {
        if (!hamster.isAlive()) return false;

        LivingEntity passenger = hamster.getControllingPassenger();
        if (!hamster.isTame() || !(passenger instanceof Player player)) {
            return false;
        }

        // --- Synchronize Rider Orientation ---
        hamster.setYRot(player.getYRot());
        hamster.yRotO = hamster.getYRot();
        hamster.setXRot(player.getXRot() * 0.5F);
        hamster.delegateSetRotation(hamster.getYRot(), hamster.getXRot());
        hamster.yBodyRot = hamster.getYRot();
        hamster.yHeadRot = hamster.yBodyRot;

        // --- Resolve Movement Input and Speed ---
        float forwardSpeed = player.zza;
        float sidewaysSpeed = player.xxa;

        if (forwardSpeed <= 0.0F) {
            forwardSpeed *= 0.25F;
        }

        boolean hasMovement = Math.abs(forwardSpeed) > 1.0e-5 || Math.abs(sidewaysSpeed) > 1.0e-5;
        boolean isSprinting = hamster.isRiderSprintHeld() && hasMovement;

        hamster.setSprinting(isSprinting);

        final AhpMainConfig config = AdorableHamsterPets.MAIN_CONFIG;
        double speedMultiplier =
                isSprinting
                        ? config.ridingSprintSpeedMultiplier.get()
                        : config.ridingBaseSpeedMultiplier.get();

        float attributeSpeed =
                (float) hamster.getAttributeValue(Attributes.MOVEMENT_SPEED);
        float finalSpeed = (float) (attributeSpeed * speedMultiplier);

        // Speed effects remain additive after the configured riding multiplier.
        if (hamster.hasEffect(MobEffects.SPEED)) {
            finalSpeed += 0.1f;
        }
        hamster.setSpeed(finalSpeed);

        // --- Consume Jump Input ---
        if (hamster.getRiderJumpCooldown() > 0) {
            hamster.setRiderJumpCooldown(hamster.getRiderJumpCooldown() - 1);
        } else if (hamster.isRiderJumpQueued()) {
            hamster.setRiderJumpQueued(false); // consume
            tryRiderJump(hamster);
        }

        // --- Apply Predicted or Authoritative Travel ---
        if (hamster.isLocalInstanceAuthoritative()) {
            hamster.delegateTravel(new Vec3(sidewaysSpeed, 0.0, forwardSpeed));
        } else if (player instanceof LocalPlayer) {
            hamster.delegateTravel(new Vec3(sidewaysSpeed, 0.0, forwardSpeed));
        }

        return true;
    }

    /**
     * Executes the jump logic for a ridden hamster.
     * <p>
     * Validates ground state, applies vertical velocity, triggers the
     * jump cooldown, and plays a bounce sound.
     */
    private static void tryRiderJump(HamsterEntity hamster) {
        if (!hamster.onGround() || hamster.isInWater() || hamster.isInLava()) {
            return;
        }

        // Trigger the protected LivingEntity.jump() through the entity wrapper.
        hamster.executeJump();

        // Override the vanilla vertical component with the configured riding impulse.
        Vec3 v = hamster.getDeltaMovement();
        hamster.setDeltaMovement(v.x, RIDER_JUMP_VELOCITY, v.z);
        hamster.needsSync = true;
        hamster.fallDistance = 0.0F;

        // Attribute the bounce sound to the rider when one is still controlling the hamster.
        Player rider =
                (hamster.getControllingPassenger() instanceof Player p) ? p : null;
        float randomPitch = 1.2f + (hamster.getRandom().nextFloat() * 0.4f - 0.2f);
        hamster.level()
                .playSound(
                        rider,
                        hamster.getX(),
                        hamster.getY(),
                        hamster.getZ(),
                        ModSounds.HAMSTER_BOUNCE.get(),
                        SoundSource.PLAYERS,
                        0.6f,
                        randomPitch);

        hamster.setRiderJumpCooldown(RIDER_JUMP_COOLDOWN_TICKS);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private HamsterRidingUtil() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *                               Nested Types
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * <p>Provides a unified, scale-aware seat offset for client rendering (bone space) and a vertical correction
     * for server-side attachment/camera math.
     */
    public final class HamsterSeatOffsets {

        // --- Slope and Intercepts ---
        private static final double SEAT_Y_INTERCEPT = 0.12;
        private static final double SEAT_Y_SLOPE = -0.56;

        private static final double SEAT_Z_INTERCEPT = 0.0875;
        private static final double SEAT_Z_SLOPE = 0.3125;

        // --- Global Fine-Tuning ---
        private static final double SEAT_Y_NUDGE = -0.02; // More negative = down
        private static final double SEAT_Z_NUDGE = 0.02; // More positive = backwards

        private HamsterSeatOffsets() {}

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
        public static Vec3 visualSeatOffset(LivingEntity passenger, float mountScale) {
            float s = passenger.getScale();
            return new Vec3(0.0, seatY(s, mountScale), seatZ(s));
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
