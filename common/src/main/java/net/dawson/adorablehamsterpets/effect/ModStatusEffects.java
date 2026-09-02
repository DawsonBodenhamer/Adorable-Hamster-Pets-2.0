package net.dawson.adorablehamsterpets.effect;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public final class ModStatusEffects {

    public static final DeferredRegister<MobEffect> STATUS_EFFECTS =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, Registries.MOB_EFFECT);

    public static final RegistrySupplier<MobEffect> FEATHER_YEETING =
            STATUS_EFFECTS.register("feather_yeeting", FeatherYeetingStatusEffect::new);

    private ModStatusEffects() {
    }

    public static void register() {
        STATUS_EFFECTS.register();
    }
}
