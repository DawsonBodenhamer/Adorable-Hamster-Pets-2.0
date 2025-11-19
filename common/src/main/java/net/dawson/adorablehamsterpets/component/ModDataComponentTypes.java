package net.dawson.adorablehamsterpets.component;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Uuids;

import java.util.UUID;

public class ModDataComponentTypes {
    // Architectury's DeferredRegister
    public static final DeferredRegister<ComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.DATA_COMPONENT_TYPE);

    // Each component registered via the register(...) method and stored in a RegistrySupplier
    public static final RegistrySupplier<ComponentType<UUID>> LINKED_HAMSTER_UUID =
            DATA_COMPONENT_TYPES.register("linked_hamster_uuid",
                    () -> ComponentType.<UUID>builder().codec(Uuids.CODEC).cache().build());

    public static final RegistrySupplier<ComponentType<Text>> LINKED_HAMSTER_NAME =
            DATA_COMPONENT_TYPES.register("linked_hamster_name",
                    () -> ComponentType.<Text>builder().codec(TextCodecs.CODEC).cache().build());

    public static final RegistrySupplier<ComponentType<WanderDistance>> WANDER_DISTANCE =
            DATA_COMPONENT_TYPES.register("wander_distance",
                    () -> ComponentType.<WanderDistance>builder().codec(WanderDistance.CODEC).cache().build());

    public static final RegistrySupplier<ComponentType<WoodVariant>> WOOD_VARIANT =
            DATA_COMPONENT_TYPES.register("wood_variant",
                    () -> ComponentType.<WoodVariant>builder().codec(WoodVariant.CODEC).cache().build());

    // Called from AdorableHamsterPets.initRegistries() to perform the actual registration
    public static void registerDataComponentTypes() {
        DATA_COMPONENT_TYPES.register();
    }
}
