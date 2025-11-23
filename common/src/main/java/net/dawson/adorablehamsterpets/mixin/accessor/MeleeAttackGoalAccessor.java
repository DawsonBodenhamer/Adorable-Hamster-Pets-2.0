package net.dawson.adorablehamsterpets.mixin.accessor;


import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


/**
 * Accessor interface to expose the protected 'cooldown' field from MeleeAttackGoal.
 * This allows us to set the cooldown from our custom goal class in a cross-platform way.
 */
@Mixin(MeleeAttackGoal.class)
public interface MeleeAttackGoalAccessor {
    /**
     * Provides write access to the cooldown field in MeleeAttackGoal.
     * @param cooldown The new value for the cooldown timer.
     */
    @Accessor("cooldown")
    void setCooldown(int cooldown);
}