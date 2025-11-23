package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TemptGoal.class)
public interface TemptGoalAccessor {
    @Accessor("cooldown")
    int getCooldown();

    @Accessor("cooldown")
    void setCooldown(int cooldown);
}