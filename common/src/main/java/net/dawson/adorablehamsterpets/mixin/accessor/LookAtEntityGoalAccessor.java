package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LookAtEntityGoal.class)
public interface LookAtEntityGoalAccessor {
    @Accessor("target")
    Entity getTarget();

    @Accessor("lookTime")
    int getLookTime();

    @Accessor("lookTime")
    void setLookTime(int lookTime);

    @Accessor("lookForward")
    boolean getLookForward();
}