package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomLookAroundGoal.class)
public interface LookAroundGoalAccessor {
    @Accessor("mob")
    Mob getMob();

    @Accessor("relX")
    double getDeltaX();

    @Accessor("relZ")
    double getDeltaZ();

    @Accessor("lookTime")
    int getLookTime();

    @Accessor("lookTime")
    void setLookTime(int lookTime);
}