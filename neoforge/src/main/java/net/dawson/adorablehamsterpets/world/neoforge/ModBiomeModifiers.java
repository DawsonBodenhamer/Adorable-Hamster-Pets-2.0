package net.dawson.adorablehamsterpets.world.neoforge;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Handles the registration of custom Biome Modifier serializers for the NeoForge platform.
 * This class ensures that NeoForge can recognize and process the custom biome modifiers.
 */
public class ModBiomeModifiers {
    /**
     * A {@link DeferredRegister} for biome modifier serializers, which tells NeoForge how to
     * read and write biome modifiers from data files.
     */
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AdorableHamsterPets.MOD_ID);

    /**
     * A {@link DeferredHolder} for our custom configurable hamster spawn modifier.
     * This registers a serializer that points to the {@link ConfigurableHamsterSpawnModifier},
     * allowing it to be referenced in JSON files with the ID "adorablehamsterpets:configurable_hamster_spawns".
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfigurableHamsterSpawnModifier>> CONFIGURABLE_HAMSTER_SPAWN =
            BIOME_MODIFIER_SERIALIZERS.register(
                    "configurable_hamster_spawns",
                    () -> ConfigurableHamsterSpawnModifier.CODEC
            );

    // --- Wild Bush and Sunflower Modifier Registration ---
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfigurableFeatureModifier>> CONFIGURABLE_FEATURE_MODIFIER =
            BIOME_MODIFIER_SERIALIZERS.register("configurable_feature_modifier", () -> ConfigurableFeatureModifier.CODEC);

    /**
     * Registers this class's {@link DeferredRegister} to the mod's event bus,
     * allowing NeoForge to process the registrations at the correct time.
     *
     * @param eventBus The mod-specific event bus provided by the NeoForge entrypoint.
     */
    public static void register(IEventBus eventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(eventBus);
    }
}