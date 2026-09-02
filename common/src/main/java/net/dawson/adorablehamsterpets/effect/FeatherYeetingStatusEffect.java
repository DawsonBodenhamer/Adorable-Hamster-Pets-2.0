package net.dawson.adorablehamsterpets.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class FeatherYeetingStatusEffect extends MobEffect {

    protected FeatherYeetingStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF3CFB9);
    }

    public static long calculateThrowCooldownDuration(
            int baseCooldownTicks,
            boolean hasFeatherYeeting,
            int featherYeetingReductionPercent
    ) {
        if (!hasFeatherYeeting) {
            return baseCooldownTicks;
        }

        long unreducedPercent = 100L - featherYeetingReductionPercent;
        return Math.max(0L, Math.round(baseCooldownTicks * unreducedPercent / 100.0D));
    }
}
