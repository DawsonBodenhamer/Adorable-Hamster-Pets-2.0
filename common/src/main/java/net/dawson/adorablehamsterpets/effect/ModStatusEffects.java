package net.dawson.adorablehamsterpets.effect;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.RegistryKeys;

public final class ModStatusEffects {

    public static final DeferredRegister<StatusEffect> STATUS_EFFECTS =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.STATUS_EFFECT);

    public static final RegistrySupplier<StatusEffect> FEATHER_YEETING =
            STATUS_EFFECTS.register("feather_yeeting", FeatherYeetingStatusEffect::new);

    private ModStatusEffects() {
    }

    public static void register() {
        STATUS_EFFECTS.register();
    }
}
