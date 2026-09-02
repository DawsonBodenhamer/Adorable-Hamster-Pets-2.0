package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LookAtPlayerGoal.class)
public interface LookAtEntityGoalAccessor {
    @Accessor("lookAt")
    Entity getTarget();

    @Accessor("lookTime")
    int getLookTime();

    @Accessor("lookTime")
    void setLookTime(int lookTime);

    @Accessor("onlyHorizontal")
    boolean getLookForward();
}