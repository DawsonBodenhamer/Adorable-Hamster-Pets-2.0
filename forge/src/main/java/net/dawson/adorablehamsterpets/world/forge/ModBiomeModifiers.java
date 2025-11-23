package net.dawson.adorablehamsterpets.world.forge;

import com.mojang.serialization.Codec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-side registration of biome-modifier codecs.
 * <p>
 * NOTE:  Forge’s {@linkplain ForgeRegistries.Keys#BIOME_MODIFIER_SERIALIZERS
 * BIOME_MODIFIER_SERIALIZERS} registry stores {@code Codec<? extends BiomeModifier>}
 * – not {@code MapCodec}.  Using {@code Codec} here resolves the generic-mismatch
 * errors your IDE highlighted.
 */
public final class ModBiomeModifiers {

    private ModBiomeModifiers() {}

    /* ------------------------------------------------------------ */
    /* Deferred register                                             */
    /* ------------------------------------------------------------ */

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(
                    ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    AdorableHamsterPets.MOD_ID
            );

    /* ------------------------------------------------------------ */
    /* Entries                                                      */
    /* ------------------------------------------------------------ */

    public static final RegistryObject<Codec<ConfigurableHamsterSpawnModifier>> CONFIGURABLE_HAMSTER_SPAWN =
            BIOME_MODIFIER_SERIALIZERS.register(
                    "configurable_hamster_spawns",
                    ConfigurableHamsterSpawnModifier.CODEC::codec
            );



    public static final RegistryObject<Codec<ConfigurableFeatureModifier>> CONFIGURABLE_FEATURE_MODIFIER =
            BIOME_MODIFIER_SERIALIZERS.register(
                    "configurable_feature_modifier",
                    ConfigurableFeatureModifier.CODEC::codec
            );

    /* ------------------------------------------------------------ */
    /* Hook into the mod-event bus                                  */
    /* ------------------------------------------------------------ */

    public static void register(IEventBus modEventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
