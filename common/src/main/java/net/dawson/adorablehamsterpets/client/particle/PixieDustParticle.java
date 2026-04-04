package net.dawson.adorablehamsterpets.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/**
 * A dense, short-lived, sparkling particle that dynamically tints itself
 * based on its assigned PixieDustParticleTheme palette.
 */
public class PixieDustParticle extends SpriteBillboardParticle {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int MIN_LIFETIME = 8;               // Minimum ticks before particle dies
    private static final int MAX_LIFETIME = 14;              // Maximum ticks before particle dies
    private static final double VELOCITY_SPREAD = 0.03;      // Initial random velocity spread applied to all axes upon spawning
    private static final float VELOCITY_DECAY = 0.85f;       // Multiplier applied to velocity every tick (simulates air drag/friction)
    private static final float GRAVITY = 0.0f;               // Constant downward acceleration per tick (0 = no gravity)
    private static final float BASE_SCALE_MIN = 0.01f;       // Minimum starting size for the particle
    private static final float BASE_SCALE_VARIANCE = 0.015f; // Random additional size added to the base scale
    private static final float ALPHA_FADE_AMOUNT = 0.05f;    // Amount of opacity subtracted per tick (fades out as it dies)
    private static final float MIN_BRIGHTNESS_CLAMP = 0.4f;  // Minimum brightness multiplier to ensure darker colors still glow

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private final PixieDustParticleTheme theme;
    private final float baseScale;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    protected PixieDustParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider spriteProvider, PixieDustParticleTheme theme) {
        super(world, x, y, z, vx, vy, vz);
        this.theme = theme;
        this.setSprite(spriteProvider.getSprite(this.random));

        this.maxAge = MIN_LIFETIME + this.random.nextInt(MAX_LIFETIME - MIN_LIFETIME + 1);
        this.baseScale = BASE_SCALE_MIN + this.random.nextFloat() * BASE_SCALE_VARIANCE;
        this.scale = this.baseScale;

        this.velocityX = vx + (this.random.nextDouble() - 0.5) * VELOCITY_SPREAD;
        this.velocityY = vy + (this.random.nextDouble() - 0.5) * VELOCITY_SPREAD;
        this.velocityZ = vz + (this.random.nextDouble() - 0.5) * VELOCITY_SPREAD;

        this.gravityStrength = GRAVITY;
        this.collidesWithWorld = false;

        updateColor();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        // Shimmer: pick a new color from the palette every tick
        updateColor();

        // Shrink over time
        this.scale = this.baseScale * (1.0f - ((float)this.age / this.maxAge));

        // Fade out
        this.alpha = Math.max(0.0f, this.alpha - ALPHA_FADE_AMOUNT);

        // Move and apply air resistance
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= VELOCITY_DECAY;
        this.velocityY *= VELOCITY_DECAY;
        this.velocityZ *= VELOCITY_DECAY;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        // Calculate relative brightness of current color
        float colorBrightness = Math.max(this.red, Math.max(this.green, this.blue));

        // Clamp it so darker colors get boosted to minimum threshold
        float adjustedBrightness = Math.max(MIN_BRIGHTNESS_CLAMP, colorBrightness);

        // Map the adjusted brightness to block light level (0-240)
        int dynamicBlockLight = (int) (adjustedBrightness * 240.0f);

        // Get the actual ambient world light
        int worldLight = super.getBrightness(tint);

        // Use the higher of the two so it doesn't artificially darken in bright areas
        int finalBlockLight = Math.max(dynamicBlockLight, worldLight & 0xFFFF);

        // Combine with world's sky light (upper 16 bits)
        return finalBlockLight | (worldLight & 0xFFFF0000);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    // Pick new color from theme palette
    private void updateColor() {
        int color = this.theme.getRandomColor(this.random);
        this.setColor((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Inner Classes
     * ────────────────────────────────────────────────────────────────────────────*/

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;
        private final PixieDustParticleTheme theme;

        public Factory(SpriteProvider sprites, PixieDustParticleTheme theme) {
            this.sprites = sprites;
            this.theme = theme;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new PixieDustParticle(world, x, y, z, vx, vy, vz, this.sprites, this.theme);
        }
    }
}