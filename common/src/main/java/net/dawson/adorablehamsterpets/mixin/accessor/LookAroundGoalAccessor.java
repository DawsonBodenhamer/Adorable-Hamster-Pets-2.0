package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LookAroundGoal.class)
public interface LookAroundGoalAccessor {
    @Accessor("mob")
    MobEntity getMob();

    @Accessor("deltaX")
    double getDeltaX();

    @Accessor("deltaZ")
    double getDeltaZ();

    @Accessor("lookTime")
    int getLookTime();

    @Accessor("lookTime")
    void setLookTime(int lookTime);
}