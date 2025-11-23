package net.dawson.adorablehamsterpets.client.state;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderHamsterState;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A client-side data holder attached to each player to manage the state of their shoulder pets.
 * This ensures that all state management is isolated per-player and occurs on the main client thread.
 */
public class ClientShoulderHamsterData {

    // --- Physics Constants ---
    private static final float SPRING_STIFFNESS = 3.0f;
    private static final float DAMPING_FACTOR = 0.7f; // How quickly it stops bouncing
    private static final float BOUNCINESS_FACTOR = 1.7f; // Controls bounce height
    private static final float GRAVITY_FORCE = 0.2f;
    private static final float HAMSTER_MASS = 2.0f;
    private static final float FALL_DRAG_MULTIPLIER = 0.4f; // Slows fall speed
    private static final float SQUASH_STRETCH_INTENSITY = 5.0f;
    private static final float IMPACT_SQUASH_INTENSITY = 5.0f;
    private static final float IMPACT_SQUASH_DECAY = 0.4f; // The speed at which the impact squash wears off

    private final Map<ShoulderLocation, ShoulderHamsterState> hamsterStates = new EnumMap<>(ShoulderLocation.class);
    private final Map<ShoulderLocation, Integer> animationAges = new EnumMap<>(ShoulderLocation.class);
    private final Map<ShoulderLocation, PhysicsState> physicsStates = new EnumMap<>(ShoulderLocation.class);
    private double previousPlayerVelocityY = 0.0;
    private boolean wasPlayerOnGroundLastTick = true;
    private int landingCheckGracePeriod;
    private int landingEventWindow = 0;

    /**
     * A mutable inner class to hold the physics state for a single shoulder pet.
     */
    public static class PhysicsState {
        public float hamsterOffsetY = 0.0f;
        public float previousOffsetY = 0.0f;
        public float hamsterVelocityY = 0.0f;
        public float hamsterScaleY = 1.0f;
        public float previousScaleY = 1.0f;
        public float impactSquashFactor = 0.0f;
        public int soundDelayTicks = 0;
        public int impactCooldown = 0;
        public int jumpCooldown = 0;
    }

    public ClientShoulderHamsterData() {
        this.landingCheckGracePeriod = 20;
    }

    /**
     * Ticks the state machines and animation clocks for all active shoulder pets on this player.
     * This method should be called once per client tick.
     *
     * @param player The client player entity this data is attached to.
     */
    public void clientTick(AbstractClientPlayerEntity player) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

        // --- 1. Detect Player Jump Event ---
        boolean playerJustStartedJumping = this.wasPlayerOnGroundLastTick && !player.isOnGround();
        if (playerJustStartedJumping) {AdorableHamsterPets.LOGGER.trace("[PHYSICS DEBUG] Player JUMP detected at tick {}. Previous Velocity Y: {}", player.getWorld().getTime(), String.format("%.4f", this.previousPlayerVelocityY));}

        boolean playerJustLanded = false;
        if (this.landingCheckGracePeriod > 0) {
            this.landingCheckGracePeriod--;
        } else {
            playerJustLanded = !this.wasPlayerOnGroundLastTick && player.isOnGround();
        }
        // Open the landing event window when the player lands
        if (playerJustLanded) {
            this.landingEventWindow = 7;
            AdorableHamsterPets.LOGGER.trace("[PHYSICS DEBUG] Player LANDING detected. Opening 15-tick impact window.");
        }

        // Decrement the window timer
        if (this.landingEventWindow > 0) {
            this.landingEventWindow--;
        }

        // --- 2. Standard State Ticking ---
        // Determine player movement state once
        boolean isSprinting = player.isSprinting();
        // Player is "walking" if they are moving but not sprinting.
        boolean isWalking = player.getVelocity().horizontalLengthSquared() > 1.0E-7 && !isSprinting;

        // Calculate player's vertical acceleration for this tick
        double playerVelocityY = player.getVelocity().y;
        // Introduce a dead zone for near-zero velocities
        // If the player is on the ground and their vertical velocity is very small, treat it as zero.
        if (player.isOnGround() && Math.abs(playerVelocityY) < 0.1) {
            playerVelocityY = 0.0;
        }

        double playerAccelerationY = playerVelocityY - this.previousPlayerVelocityY;

        // --- 3. Per-Hamster Simulation Loop ---
        // Iterate through all possible locations to update states
        int delay = 1; // Initialize delay counter for staggered sounds
        for (ShoulderLocation location : ShoulderLocation.values()) {
            NbtCompound shoulderNbt = playerAccessor.getShoulderHamster(location);

            if (!shoulderNbt.isEmpty()) {
                // --- 3.1. Standard State Ticking ---
                ShoulderHamsterState state = this.hamsterStates.computeIfAbsent(location, l -> new ShoulderHamsterState());
                state.tick(isSprinting, isWalking);
                int currentAge = this.animationAges.getOrDefault(location, 0);
                this.animationAges.put(location, currentAge + 1);

                // --- 3.2. Physics Simulation Ticking ---
                PhysicsState physics = this.physicsStates.computeIfAbsent(location, l -> new PhysicsState());
                // Store the previous frame's offset for interpolation
                physics.previousOffsetY = physics.hamsterOffsetY;
                physics.previousScaleY = physics.hamsterScaleY;

                // Decay the impact squash over time
                physics.impactSquashFactor = cosineInterpolate(physics.impactSquashFactor, 0.0f, IMPACT_SQUASH_DECAY);

                // Decrement Cooldowns
                if (physics.impactCooldown > 0) physics.impactCooldown--;
                if (physics.jumpCooldown > 0) physics.jumpCooldown--;

                // Apply Jump Cooldown
                if (playerJustStartedJumping) {
                    // Prevent the impact physics from happening at the start of a jump
                    physics.jumpCooldown = 3;
                }

                // Store previous velocity for impact detection
                float previousHamsterVelocityY = physics.hamsterVelocityY;

                // Vertical Bounce Simulation
                float inertialForce = (float) (-playerVelocityY * HAMSTER_MASS);
                float springForce = -SPRING_STIFFNESS * physics.hamsterOffsetY;
                float dampingForce = -DAMPING_FACTOR * physics.hamsterVelocityY;
                float gravityForce = -GRAVITY_FORCE;
                float totalForce = inertialForce + springForce + dampingForce + gravityForce;
                float acceleration = totalForce / HAMSTER_MASS;
                physics.hamsterVelocityY += acceleration;

                // Apply extra drag only when falling
                if (physics.hamsterVelocityY < 0) {
                    physics.hamsterVelocityY *= FALL_DRAG_MULTIPLIER;
                }
                physics.hamsterOffsetY += physics.hamsterVelocityY;

                // Combined Impact Detection and Collision Logic
                if (physics.hamsterOffsetY < 0) {
                    // This block represents the moment of collision.

                    // Check if this is the FIRST impact in a sequence.
                    if (this.landingEventWindow > 0 && physics.impactCooldown == 0 && physics.jumpCooldown == 0) {
                        // 1. Trigger Impact Squash
                        physics.impactSquashFactor = Math.abs(physics.hamsterVelocityY) * IMPACT_SQUASH_INTENSITY;
                        // 2. Set Staggered Sound Delay
                        physics.soundDelayTicks = delay;
                        delay += player.getRandom().nextBetween(1, 2);
                        // 3. Set Impact Cooldown to prevent re-triggering on small bounces
                        physics.impactCooldown = 5;
                    }

                    // Always apply the bounce physics regardless of cooldowns.
                    physics.hamsterOffsetY = 0;
                    physics.hamsterVelocityY *= -BOUNCINESS_FACTOR;
                }

                // Handle Sound Delay Timer
                if (physics.soundDelayTicks > 0) {
                    physics.soundDelayTicks--;
                    if (physics.soundDelayTicks == 0) {
                        // Check Config Before Playing Sound
                        MinecraftClient client = MinecraftClient.getInstance();
                        boolean shouldPlaySound = !(Configs.AHP.silencePhysicsSoundsInFirstPerson && client.options.getPerspective().isFirstPerson());

                        if (shouldPlaySound) {
                            SoundEvent impactSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_SHOULDER_IMPACT_SOUNDS, player.getRandom());
                            if (impactSound != null) {
                                client.getSoundManager().play(
                                        new net.minecraft.client.sound.PositionedSoundInstance(
                                                impactSound,
                                                net.minecraft.sound.SoundCategory.PLAYERS,
                                                1.0f,
                                                0.9f + player.getRandom().nextFloat() * 0.2f,
                                                player.getRandom(),
                                                player.getX(), player.getY(), player.getZ()
                                        )
                                );
                            }
                        }
                    }
                }

                // Squash and Stretch Simulation
                if (physics.impactSquashFactor > 0.001f) {AdorableHamsterPets.LOGGER.trace("[PHYSICS DEBUG]   -> Decaying impact squash for {}: {}", location, String.format("%.4f", physics.impactSquashFactor));}
                // 1. Calculate the base target scale from player acceleration.
                float accelScale = 1.0f - (float)playerAccelerationY * SQUASH_STRETCH_INTENSITY;
                // 2. Combine the acceleration scale with the impact squash.
                float combinedTargetScale = accelScale - physics.impactSquashFactor;
                // 3. Clamp the final combined value.
                float clampedTargetScale = MathHelper.clamp(combinedTargetScale, 0.65f, 1.25f);
                // 4. Smoothly interpolate towards the final, clamped target.
                physics.hamsterScaleY = cosineInterpolate(physics.hamsterScaleY, clampedTargetScale, 0.6f);

            } else {
                // If a slot becomes empty, remove all its associated data
                this.hamsterStates.remove(location);
                this.animationAges.remove(location);
                this.physicsStates.remove(location);
            }
        }
        // --- 4. Update State for Next Tick ---
        this.previousPlayerVelocityY = playerVelocityY;
        this.wasPlayerOnGroundLastTick = player.isOnGround();
    }

    /**
     * Gets the smoothly interpolated vertical offset for rendering.
     *
     * @param location    The shoulder location being rendered.
     * @param partialTick The fraction of a tick that has passed since the last full tick.
     * @return The interpolated vertical offset.
     */
    public float getRenderOffsetY(ShoulderLocation location, float partialTick) {
        PhysicsState state = this.physicsStates.get(location);
        if (state == null) {
            return 0.0f;
        }
        // Use cosine interpolation for a smoother ease-in/ease-out between ticks.
        return cosineInterpolate(state.previousOffsetY, state.hamsterOffsetY, partialTick);
    }

    /**
     * Gets the smoothly interpolated vertical scale for rendering.
     *
     * @param location    The shoulder location being rendered.
     * @param partialTick The fraction of a tick that has passed since the last full tick.
     * @return The interpolated vertical scale factor.
     */
    public float getRenderScaleY(ShoulderLocation location, float partialTick) {
        PhysicsState state = this.physicsStates.get(location);
        if (state == null) {
            return 1.0f;
        }
        // Interpolate using a cosine curve
        return cosineInterpolate(state.previousScaleY, state.hamsterScaleY, partialTick);
    }

    /**
     * Interpolates between two values using a cosine curve for smooth easing.
     *
     * @param start The starting value.
     * @param end   The target value.
     * @param delta The fraction of the way to interpolate (0.0 to 1.0).
     * @return The interpolated value.
     */
    private float cosineInterpolate(float start, float end, float delta) {
        float transition = (1.0f - (float)Math.cos(delta * Math.PI)) * 0.5f;
        return start + transition * (end - start);
    }

    /**
     * Gets the current animation state for a specific shoulder location.
     *
     * @param location The shoulder location.
     * @return The ShoulderHamsterState, or null if no pet is in that slot.
     */
    @Nullable
    public ShoulderHamsterState getHamsterState(ShoulderLocation location) {
        return this.hamsterStates.get(location);
    }

    /**
     * Gets the unique, persistent animation age for a specific shoulder location.
     *
     * @param location The shoulder location.
     * @return The animation age for that slot.
     */
    public int getAnimationAge(ShoulderLocation location) {
        return this.animationAges.getOrDefault(location, 0);
    }
}