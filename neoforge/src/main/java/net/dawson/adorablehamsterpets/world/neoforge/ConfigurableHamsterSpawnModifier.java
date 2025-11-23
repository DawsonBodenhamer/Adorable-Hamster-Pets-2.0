package net.dawson.adorablehamsterpets.world.neoforge;

import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * A custom biome modifier that adds hamster spawns to biomes based on runtime configuration.
 * This modifier is triggered by a simple JSON file and performs its logic in Java, allowing
 * spawn weights and counts to be controlled by the user's config file.
 */
public class ConfigurableHamsterSpawnModifier implements BiomeModifier {
    /**
     * The codec for this biome modifier. A "unit" codec is used because this modifier
     * does not read any data from its JSON definition; all logic is self-contained.
     */
    public static final MapCodec<ConfigurableHamsterSpawnModifier> CODEC =
            MapCodec.unit(ConfigurableHamsterSpawnModifier::new);

    public void modify(RegistryEntry<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // We only want to add spawns, so we only act during the ADD phase.
        if (phase != Phase.ADD) {
            return;
        }

        // The NeoForge modifier directly checks the biome entry against the parsed config sets.
        // This avoids the need for a BiomeContext object.
        if (ModEntitySpawns.shouldSpawnInBiomeNeoForge(biome)) {
            var spawnBuilder = builder.getMobSpawnSettings();
            var spawnEntry = new SpawnSettings.SpawnEntry(
                    ModEntities.HAMSTER.get(),
                    Configs.AHP.spawnWeight.get(),
                    1, // minCount is always 1
                    Configs.AHP.maxGroupSize.get()
            );

            // Add the new spawn entry to the CREATURE spawn group.
            spawnBuilder.spawn(SpawnGroup.CREATURE, spawnEntry);
        }
    }

    /**
     * Returns the registered codec that serializes this modifier.
     * This is required by the {@link BiomeModifier} interface.
     *
     * @return The unique {@link MapCodec} for this modifier type.
     */
    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        // Return the unique codec so NeoForge knows how to handle this modifier.
        return ModBiomeModifiers.CONFIGURABLE_HAMSTER_SPAWN.get();
    }
}