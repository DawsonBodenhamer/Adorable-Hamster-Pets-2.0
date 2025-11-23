package net.dawson.adorablehamsterpets.entity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<HamsterEntity>> HAMSTER = ENTITY_TYPES.register(
            Identifier.of(AdorableHamsterPets.MOD_ID, "hamster"),
            () -> EntityType.Builder.create(HamsterEntity::new, SpawnGroup.CREATURE)
                    .setDimensions(0.5F, 0.5F)
                    .build("hamster")
    );

    public static void register() {
        ENTITY_TYPES.register();
    }
}