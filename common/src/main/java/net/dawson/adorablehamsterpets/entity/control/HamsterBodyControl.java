package net.dawson.adorablehamsterpets.entity.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

/**
 * A custom BodyControl that overrides the vanilla logic to ensure instant, unified rotation
 * for the GeckoLib-based Hamster model, while still respecting the difference between
 * movement-based rotation and look-based rotation.
 * <p>
 * This implementation forces the body to sync with the movement yaw when walking, and
 * instantly sync with the head yaw when standing still. This allows LookControl to
 * function correctly without the undesirable slow interpolation of the vanilla BodyControl.
 */
public class HamsterBodyControl extends BodyRotationControl {
    private final Mob entity;

    public HamsterBodyControl(Mob entity) {
        super(entity);
        this.entity = entity;
    }

    /**
     * Overrides the default body rotation logic to force an immediate sync based on entity state.
     */
    @Override
    public void clientTick() {
        // If the hamster is moving (pathfinding), its body should face the direction of movement.
        if (this.isMoving()) {
            this.entity.yBodyRot = Mth.wrapDegrees(this.entity.getYRot());
        } else {
            // If the hamster is standing still, its body should instantly face where its head is looking.
            // This allows LookControl and AI goals to turn the hamster in place without a slow delay.
            this.entity.yBodyRot = Mth.wrapDegrees(this.entity.yHeadRot);
        }
    }

    /**
     * Checks if the entity has moved significantly since the last tick.
     * This logic is copied directly from the vanilla BodyControl class.
     * @return True if the entity is moving, false otherwise.
     */
    private boolean isMoving() {
        double d = this.entity.getX() - this.entity.xo;
        double e = this.entity.getZ() - this.entity.zo;
        // A very small threshold to detect any horizontal movement.
        return d * d + e * e > 2.5000003E-7F;
    }
}