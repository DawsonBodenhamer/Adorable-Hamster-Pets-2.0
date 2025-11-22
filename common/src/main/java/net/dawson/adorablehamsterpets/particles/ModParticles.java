package net.dawson.adorablehamsterpets.particles;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.RegistryKeys;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds all particle type registrations.
 */
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    // Map to link each particle type back to its wood variant
    // For 1.20.1, use DefaultParticleType
    public static final Map<WoodVariant, RegistrySupplier<DefaultParticleType>> BEDDING_PARTICLES = new EnumMap<>(WoodVariant.class);

    static {
        for (WoodVariant variant : WoodVariant.values()) {
            String id = "hamster_bedding_" + variant.asString();
            // In 1.20.1, use new DefaultParticleType(false), using an anonymous subclass to bypass the protected constructor
            BEDDING_PARTICLES.put(variant, PARTICLE_TYPES.register(id, () -> new DefaultParticleType(false) {}));
        }
    }

    public static void register() {
        PARTICLE_TYPES.register();
    }

    /**
     * Gets the appropriate particle type for a given wood variant.
     * @param variant The wood variant.
     * @return The corresponding DefaultParticleType.
     */
    public static DefaultParticleType getForVariant(WoodVariant variant) {
        return BEDDING_PARTICLES.getOrDefault(variant, BEDDING_PARTICLES.get(WoodVariant.OAK)).get();
    }
}