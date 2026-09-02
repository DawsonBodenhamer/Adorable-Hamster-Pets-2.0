package net.dawson.adorablehamsterpets.component;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import com.mojang.serialization.Codec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.config.WanderDistance;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import java.util.UUID;

public class ModDataComponentTypes {
    // Architectury's DeferredRegister
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    // Each component registered via the register(...) method and stored in a RegistrySupplier
    public static final RegistrySupplier<DataComponentType<UUID>> LINKED_HAMSTER_UUID =
            DATA_COMPONENT_TYPES.register("linked_hamster_uuid",
                    () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.AUTHLIB_CODEC).cacheEncoding().build());

    public static final RegistrySupplier<DataComponentType<Component>> LINKED_HAMSTER_NAME =
            DATA_COMPONENT_TYPES.register("linked_hamster_name",
                    () -> DataComponentType.<Component>builder().persistent(ComponentSerialization.CODEC).cacheEncoding().build());

    public static final RegistrySupplier<DataComponentType<WanderDistance>> WANDER_DISTANCE =
            DATA_COMPONENT_TYPES.register("wander_distance",
                    () -> DataComponentType.<WanderDistance>builder().persistent(WanderDistance.CODEC).cacheEncoding().build());

    public static final RegistrySupplier<DataComponentType<WoodVariant>> WOOD_VARIANT =
            DATA_COMPONENT_TYPES.register("wood_variant",
                    () -> DataComponentType.<WoodVariant>builder().persistent(WoodVariant.CODEC).cacheEncoding().build());

    public static final RegistrySupplier<DataComponentType<UUID>> ACORN_RING_IDENTITY =
            DATA_COMPONENT_TYPES.register("acorn_ring_identity",
                    () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.AUTHLIB_CODEC).cacheEncoding().build());

    public static final RegistrySupplier<DataComponentType<String>> ACORN_RING_LAST_LOCATION =
            DATA_COMPONENT_TYPES.register("acorn_ring_last_location",
                    () -> DataComponentType.<String>builder().persistent(Codec.STRING).cacheEncoding().build());

    // Called from AdorableHamsterPets.initRegistries() to perform the actual registration
    public static void registerDataComponentTypes() {
        DATA_COMPONENT_TYPES.register();
    }
}
