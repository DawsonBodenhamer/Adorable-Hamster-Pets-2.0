package net.dawson.adorablehamsterpets.particles.common;

/**
 * Centralized behavior settings for default hamster_bedding particles.
 * */
public final class HamsterBeddingParticleBehavior {
    private HamsterBeddingParticleBehavior() {}

    // Size (blocks)
    public static final float SIZE_X = 1.0f;
    public static final float SIZE_Y = 1.0f;

    // Lifetime range (ticks)
    public static final int LIFETIME_MIN = 20;
    public static final int LIFETIME_EXTRA = 39;

    // Physics
    public static final float GRAVITY = 0.60f;
    public static final float FRICTION = 0.90f; // i.e., Air resistance
}