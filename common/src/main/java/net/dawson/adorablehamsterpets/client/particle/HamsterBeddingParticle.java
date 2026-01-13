package net.dawson.adorablehamsterpets.client.particle;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.particles.common.HamsterBeddingParticleBehavior;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.IndoorOutdoorDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/**
 * A particle representing a piece of hamster bedding (a leaf).
 * This particle has two distinct physics behaviors:
 * <p>
 * 1.  <b>Standard Physics:</b> A simple gravity-and-friction model used when particles are spawned
 *     from bed interactions.
 * 2.  <b>Floaty Physics:</b> A complex simulation for particles spawned from the Hamster Bedding item
 *     or a dispenser. This includes a gentle pendulum-like sway and a deterministic, spatially-coherent
 *     wind gust model that creates realistic, synchronized movement among nearby particles.
 */
public class HamsterBeddingParticle extends SpriteBillboardParticle {

    // --- Constants ---
    /** A magic number used in the 'vy' field to signal that this particle should use the "floaty" physics simulation. */
    public static final double BEDDING_ITEM_FLAG = -0.000123;

    // --- Universal Drift ---
    private static final float UNIVERSAL_DRIFT_ACCEL = 0.002f;
    private static final float DRIFT_PERIOD_TICKS = 3 * 60 * 20f; // 3 minutes

    // --- Sway Physics Constants (for floaty mode) ---
    private static final float SWAY_ROTATION_AMPLITUDE = 0.5f;
    private static final float SWAY_ROTATION_SPEED_MOD = 2.8f;
    private static final float UPWARD_BOOST_AT_APEX = 0.004f;
    private static final float HORIZ_SPEED_CAP = 0.08f;

    // --- Gust Physics Constants (for floaty mode) ---
    private static final int   GUST_WINDOW_TICKS   = 100;    // Time window to evaluate gust events.
    private static final float GUST_PROB_PER_WIN   = 0.35f;  // Chance a gust event exists in a window.
    private static final int   GUST_MIN_LEN        = 20;
    private static final int   GUST_MAX_LEN        = 40;
    private static final int   GUST_CELL_BLOCKS    = 12;    // Spatial coherence grid size.
    private static final float GUST_UP_ACCEL       = 0.025f;
    private static final float GUST_HORIZ_ACCEL    = 0.06f;
    private static final float GUST_SPIN_IMPULSE   = 2.0f;
    private static final float GUST_SPIN_DAMP      = 0.85f;
    private static final float COUPLING_MIN        = 0.12f;  // Minimum particle response to a gust.
    private static final float COUPLING_SPAN       = 0.78f;
    private static final int   PER_PARTICLE_DELAY_MAX = 8;   // Staggers the start of a particle's gust response.
    private static final float WIND_DRAG           = 0.10f;  // How strongly particles are pulled toward the wind's target speed.
    private static final float WIND_TARGET_SPEED   = 0.09f;  // Blocks per tick at full strength
    private static final float GUST_RISE_FRAC      = 0.30f;  // The first 30% of a gust's duration is its ramp-up swayPhaseOffset.
    private static final float EXTRA_HCAP          = 0.075f; // Additional horizontal speed allowed during a full gust.

    // --- Sound Management ---
    private static final Set<Long> playedGustSoundsThisTick = new HashSet<>();
    private static final Deque<Long> soundStartTimes = new ArrayDeque<>();
    private static final int MAX_CONCURRENT_SOUNDS = 3;
    private static final int SOUND_DURATION_TICKS = 65; // ~3 seconds
    private static long lastTick = -1L;

    // --- Fields ---
    // --- State ---
    private final boolean useFloatyPhysics;
    private long lastAppliedGustKey = Long.MIN_VALUE;

    // --- Sway Physics ---
    private final float swayFrequency;
    private final float swayAcceleration;
    private final float swayPhaseOffset;
    private final float swayDirectionX, swayDirectionZ;
    private final float constantRollVelocity;

    // --- Gust Physics ---
    private float gustSpinVel = 0f;
    private float gustCoupling = 0f;
    private int gustDelayTicks = 0;

    public HamsterBeddingParticle(ClientWorld world,
                                  double x, double y, double z,
                                  double vx, double vy, double vz,
                                  SpriteProvider sprites) {
        super(world, x, y, z, vx, vy, vz);

        // Use floaty physics if spawned from the Hamster Bedding item
        this.useFloatyPhysics = (vy == BEDDING_ITEM_FLAG);

        // --- Set Physics based on Spawn Type ---
        if (useFloatyPhysics) {
            // "Floaty" physics for item/dispenser use.
            this.velocityY = 0;
            this.gravityStrength = 0.07f;
            this.velocityMultiplier = 0.92f;
            this.maxAge = 140 + world.random.nextInt(200);

            float theta = world.random.nextFloat() * MathHelper.TAU;
            this.swayDirectionX = MathHelper.cos(theta);
            this.swayDirectionZ = MathHelper.sin(theta);

            this.swayFrequency = 0.09f + world.random.nextFloat() * 0.05f; // Period ~ 2π/ω = 45–80 ticks
            this.swayAcceleration = 0.002f + world.random.nextFloat() * 0.0025f; // Swing radius ≈ swayAcceleration / ω^2  → ~0.15–0.35 blocks
            this.swayPhaseOffset = world.random.nextFloat() * MathHelper.TAU;
            this.constantRollVelocity = (world.random.nextFloat() - 0.5f) * 0.12f; // Slow constant roll
        } else {
            // Standard physics for bed interactions.
            this.gravityStrength = HamsterBeddingParticleBehavior.GRAVITY;
            this.velocityMultiplier = HamsterBeddingParticleBehavior.FRICTION;
            this.maxAge = HamsterBeddingParticleBehavior.LIFETIME_MIN
                    + world.random.nextInt(HamsterBeddingParticleBehavior.LIFETIME_EXTRA);

            this.swayDirectionX = 0f;
            this.swayDirectionZ = 0f;
            this.swayFrequency = 0f;
            this.swayAcceleration = 0f;
            this.swayPhaseOffset = 0f;
            this.constantRollVelocity = 0f;
        }

        // --- Set Visuals ---
        this.setSprite(sprites.getSprite(this.random));
        this.setBoundingBoxSpacing(HamsterBeddingParticleBehavior.SIZE_X, HamsterBeddingParticleBehavior.SIZE_Y);
    }

    @Override
    public void tick() {
        long worldTime = this.world.getTime();
        // --- Sound Management ---
        // Prune old sounds and clear per-tick tracker
        if (worldTime != lastTick) {
            playedGustSoundsThisTick.clear();
            // Remove any sound start times that are older than the sound's duration
            while (!soundStartTimes.isEmpty() && worldTime - soundStartTimes.peekFirst() > SOUND_DURATION_TICKS) {
                soundStartTimes.pollFirst();
            }
            lastTick = worldTime;
        }

        // --- Particle Physics ---
        if (useFloatyPhysics) {
            // --- Universal Drift When Outdoors ---
            float universalDriftAngle;
            if (Configs.AHP.enableDynamicDriftAngle.get()) {
                // Dynamic, time-based rotation
                float timeWithPartial = worldTime + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
                universalDriftAngle = (timeWithPartial / DRIFT_PERIOD_TICKS) * MathHelper.TAU;
            } else {
                // Static angle from config
                universalDriftAngle = (float) Math.toRadians(Configs.AHP.staticDriftAngle.get());
            }

            float driftDirX = MathHelper.cos(universalDriftAngle);
            float driftDirZ = MathHelper.sin(universalDriftAngle);

            boolean isOutdoor = IndoorOutdoorDetector.isOutdoor(this.world, this.x, this.y, this.z);

            // Apply drift only if outdoors
            if (isOutdoor) {
                this.velocityX += driftDirX * UNIVERSAL_DRIFT_ACCEL;
                this.velocityZ += driftDirZ * UNIVERSAL_DRIFT_ACCEL;
            }

            // --- Sway Physics ---
            float phase = (this.age + this.swayPhaseOffset) * this.swayFrequency;
            float sinPhase = MathHelper.sin(phase);
            float cosPhase = MathHelper.cos(phase);
            float positionInSwing = Math.abs(cosPhase);
            // Swing faster in the middle and slower at the edges
            float speedMultiplier = 0.5f + 1.5f * positionInSwing;
            float modifiedAcceleration = sinPhase * this.swayAcceleration * speedMultiplier;

            this.velocityX += this.swayDirectionX * modifiedAcceleration;
            this.velocityZ += this.swayDirectionZ * modifiedAcceleration;

            // Add a gradual upward boost that is strongest at the apex of the swing to create a 'U' shape motion.
            // The boost is proportional to the fourth power of the particle's position in its swing.
            this.velocityY += UPWARD_BOOST_AT_APEX * (float)Math.pow(positionInSwing, 4);

            // --- Gust Simulation ---
            float gustStrengthLocal = 0f;
            if (isOutdoor) { // Reuse the isOutdoor check
                Gust gust = sampleGust(this.world, this.x, this.z);

                if (gust.active) {
                    // Initialize gust response for this particle if it's a new gust event.
                    if (gust.key != lastAppliedGustKey) {
                        // Play sound once per unique gust event per tick, up to MAX_CONCURRENT_SOUNDS simultaneously
                        if (soundStartTimes.size() < MAX_CONCURRENT_SOUNDS && playedGustSoundsThisTick.add(gust.key)) {
                            float vol = Configs.AHP.leafGustVolume.get();
                            if (vol > 0) {
                                this.world.playSound(this.x, this.y, this.z, ModSounds.GENTLE_BREEZE.get(), SoundCategory.AMBIENT, vol, 1.0f, false);
                                soundStartTimes.addLast(worldTime);
                            }
                        }

                        Random pr = new Random(mix64(gust.key, System.identityHashCode(this)));
                        this.gustCoupling = COUPLING_MIN + pr.nextFloat() * COUPLING_SPAN;
                        this.gustCoupling = (float) Math.sqrt(this.gustCoupling); // Bias toward stronger coupling, most particles affected.
                        this.gustDelayTicks = pr.nextInt(PER_PARTICLE_DELAY_MAX + 1);
                        this.lastAppliedGustKey = gust.key;
                    }

                    int delayedTicksSinceGustStart = Math.max(0, gust.ticksSinceGustStart - this.gustDelayTicks);
                    float gustProgress = delayedTicksSinceGustStart / (float) gust.gustDurationTicks;
                    float rise = smooth01(Math.min(1f, gustProgress / GUST_RISE_FRAC));
                    float decay = (float) Math.exp(-3.0f * gustProgress);
                    gustStrengthLocal = rise * decay * this.gustCoupling;

                    // Apply a one-time spin impulse when the particle first feels the gust.
                    if (delayedTicksSinceGustStart == 0 && gust.ticksSinceGustStart >= this.gustDelayTicks) {
                        this.gustSpinVel += GUST_SPIN_IMPULSE * this.gustCoupling;
                    }

                    // --- Interpolate Gust Angle ---
                    float easeOutFactor = 1.0f - (float) Math.pow(1.0f - gustProgress, 3.0); // Cubic ease-out
                    float finalGustDirX = MathHelper.lerp(easeOutFactor, driftDirX, gust.gustDirX);
                    float finalGustDirZ = MathHelper.lerp(easeOutFactor, driftDirZ, gust.gustDirZ);

                    // Apply drag along the interpolated wind angle toward the wind's target speed.
                    float along = (float)(this.velocityX * finalGustDirX + this.velocityZ * finalGustDirZ);
                    float target = WIND_TARGET_SPEED * gustStrengthLocal;
                    float corr = (target - along) * WIND_DRAG;
                    this.velocityX += finalGustDirX * corr;
                    this.velocityZ += finalGustDirZ * corr;

                    // Apply upward and lateral push from the gust using the interpolated direction.
                    this.velocityY += gustStrengthLocal * GUST_UP_ACCEL;
                    this.velocityX += finalGustDirX * gustStrengthLocal * GUST_HORIZ_ACCEL;
                    this.velocityZ += finalGustDirZ * gustStrengthLocal * GUST_HORIZ_ACCEL;
                }
            }

            // --- Velocity & Rotation Update with Dynamic Horizontal Cap ---
            float dynamicHorizontalCap = HORIZ_SPEED_CAP + gustStrengthLocal * EXTRA_HCAP;
            float horizontalSpeedSquared = (float)(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
            if (horizontalSpeedSquared > dynamicHorizontalCap * dynamicHorizontalCap) {
                float scale = dynamicHorizontalCap / MathHelper.sqrt(horizontalSpeedSquared);
                this.velocityX *= scale;
                this.velocityZ *= scale;
            }

            // --- Roll update ---
            this.prevAngle = this.angle;
            this.gustSpinVel *= GUST_SPIN_DAMP;
            // Angular velocity from sway is now based on cos(phase) to make it fastest at the apex of the swing, creating a 'twist'.
            float swayAngularVelocity = cosPhase * this.swayFrequency * SWAY_ROTATION_AMPLITUDE * SWAY_ROTATION_SPEED_MOD;
            this.angle += this.constantRollVelocity + swayAngularVelocity + this.gustSpinVel;
        }

        super.tick();

        // --- Finalization ---
        // Settle quickly after landing on a block.
        if (this.onGround) {
            this.maxAge = Math.min(this.maxAge, this.age + 10);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    // --- Deterministic Wind Model ---
    /**
     * Represents a single gust event active in the current window at a given position.
     */
    private record Gust(boolean active, long key, int ticksSinceGustStart, int gustDurationTicks, float gustDirX, float gustDirZ) {}

    /**
     * Deterministically samples a wind gust based on the current world time and particle position.
     * This creates spatially and temporally coherent wind effects for groups of particles.
     */
    private static Gust sampleGust(ClientWorld world, double x, double z) {
        long worldTime = world.getTime();
        long gustWindowIndex = Math.floorDiv(worldTime, GUST_WINDOW_TICKS);

        int cellX = MathHelper.floor((float)x) / GUST_CELL_BLOCKS;
        int cellZ = MathHelper.floor((float)z) / GUST_CELL_BLOCKS;

        long baseSeed = mix64(cellX, cellZ, gustWindowIndex);
        Random r = new Random(baseSeed);

        if (r.nextFloat() >= GUST_PROB_PER_WIN) {
            return new Gust(false, baseSeed, 0, 0, 0f, 0f);
        }

        int tickInCurrentWindow = (int)(worldTime % GUST_WINDOW_TICKS);
        int maxStartTickInWindow = Math.max(1, GUST_WINDOW_TICKS - GUST_MAX_LEN - 1);
        int gustStartTick = 1 + r.nextInt(maxStartTickInWindow);
        int gustDurationTicks = GUST_MIN_LEN + r.nextInt(GUST_MAX_LEN - GUST_MIN_LEN + 1);

        if (tickInCurrentWindow < gustStartTick || tickInCurrentWindow > gustStartTick + gustDurationTicks) {
            long key = baseSeed ^ gustStartTick;
            return new Gust(false, key, 0, gustDurationTicks, 0f, 0f);
        }

        float theta = r.nextFloat() * MathHelper.TAU;
        float gustDirX = MathHelper.cos(theta);
        float gustDirZ = MathHelper.sin(theta);
        long key = (baseSeed ^ gustStartTick);
        int ticksSinceGustStart = tickInCurrentWindow - gustStartTick;

        return new Gust(true, key, ticksSinceGustStart, gustDurationTicks, gustDirX, gustDirZ);
    }

    /** A small, fast mixing function for generating stable randomness per cell/window. */
    private static long mix64(long seedA, long seedB) {
        long x = seedA * 0x9E3779B97F4A7C15L + seedB + 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }

    private static long mix64(long currentSwayAcceleration, long b, long c) {
        return mix64(mix64(currentSwayAcceleration, b), c);
    }

    /** A smoothing function (ease-in, ease-out) for gust strength envelopes. */
    private static float smooth01(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    // --- Factory ---
    /**
     * The factory for creating instances of {@link HamsterBeddingParticle}.
     */
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new HamsterBeddingParticle(world, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}