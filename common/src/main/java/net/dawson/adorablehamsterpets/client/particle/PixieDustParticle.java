package net.dawson.adorablehamsterpets.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A dense, short-lived, sparkling particle that dynamically tints itself
 * based on its assigned PixieDustParticleTheme palette.
 */
public class PixieDustParticle extends TextureSheetParticle {

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

    protected PixieDustParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteProvider, PixieDustParticleTheme theme) {
        super(world, x, y, z, vx, vy, vz);
        this.theme = theme;
        this.setSprite(spriteProvider.get(this.random));

        this.lifetime = MIN_LIFETIME + this.random.nextInt(MAX_LIFETIME - MIN_LIFETIME + 1);
        this.baseScale = BASE_SCALE_MIN + this.random.nextFloat() * BASE_SCALE_VARIANCE;
        this.quadSize = this.baseScale;

        this.xd = vx + (this.random.nextDouble() - 0.5) * VELOCITY_SPREAD;
        this.yd = vy + (this.random.nextDouble() - 0.5) * VELOCITY_SPREAD;
        this.zd = vz + (this.random.nextDouble() - 0.5) * VELOCITY_SPREAD;

        this.gravity = GRAVITY;
        this.hasPhysics = false;

        updateColor();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Shimmer: pick a new color from the palette every tick
        updateColor();

        // Shrink over time
        this.quadSize = this.baseScale * (1.0f - ((float)this.age / this.lifetime));

        // Fade out
        this.alpha = Math.max(0.0f, this.alpha - ALPHA_FADE_AMOUNT);

        // Move and apply air resistance
        this.move(this.xd, this.yd, this.zd);
        this.xd *= VELOCITY_DECAY;
        this.yd *= VELOCITY_DECAY;
        this.zd *= VELOCITY_DECAY;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float tint) {
        // Calculate relative brightness of current color
        float colorBrightness = Math.max(this.rCol, Math.max(this.gCol, this.bCol));

        // Clamp it so darker colors get boosted to minimum threshold
        float adjustedBrightness = Math.max(MIN_BRIGHTNESS_CLAMP, colorBrightness);

        // Map the adjusted brightness to block light level (0-240)
        int dynamicBlockLight = (int) (adjustedBrightness * 240.0f);

        // Get the actual ambient world light
        int worldLight = super.getLightColor(tint);

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

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final PixieDustParticleTheme theme;

        public Factory(SpriteSet sprites, PixieDustParticleTheme theme) {
            this.sprites = sprites;
            this.theme = theme;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new PixieDustParticle(world, x, y, z, vx, vy, vz, this.sprites, this.theme);
        }
    }
}