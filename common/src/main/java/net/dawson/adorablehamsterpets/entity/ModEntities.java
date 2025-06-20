package net.dawson.adorablehamsterpets.entity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKeys;

public class ModEntities {

    // --- 1. DeferredRegister for EntityTypes ---
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ENTITY_TYPE);

    // --- 2. EntityType Registration as a RegistrySupplier ---
    public static final RegistrySupplier<EntityType<HamsterEntity>> HAMSTER = ENTITY_TYPES.register("hamster", () ->
            EntityType.Builder.create(HamsterEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5F, 0.5F).build("hamster"));

    // --- 3. Main Registration Call ---
    public static void register() {
        ENTITY_TYPES.register();
    }
}