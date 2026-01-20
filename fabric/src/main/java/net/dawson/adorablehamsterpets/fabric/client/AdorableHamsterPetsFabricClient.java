package net.dawson.adorablehamsterpets.fabric.client;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.client.render.LeafJiggleRenderer;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

public final class AdorableHamsterPetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AdorableHamsterPetsClient.init();
        AdorableHamsterPetsClient.initScreenHandlers();
        AdorableHamsterPetsClient.initEntityRenderers();
        AdorableHamsterPetsClient.initBlockEntityRenderers();

        // --- Register keybindings for Fabric ---
        ModKeyBindings.init();
        KeyMappingRegistry.register(ModKeyBindings.THROW_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.DISMOUNT_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.RIDE_HAMSTER_KEY);

        // --- Register Particle Provider for all variants ---
        // On 1.20.1, use DefaultParticleType
        for (RegistrySupplier<net.minecraft.particle.DefaultParticleType> particleSupplier : ModParticles.BEDDING_PARTICLES.values()) {
            ParticleFactoryRegistry.getInstance().register(particleSupplier.get(), HamsterBeddingParticle.Factory::new);
        }

        // --- Register Leaf Jiggle Renderer ---
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();

            LeafJiggleRenderer.render(
                    client,
                    context.matrixStack(),
                    context.consumers(),
                    context.camera().getPos(),
                    context.tickDelta() // Use tickDelta on 1.20.1
            );
        });
    }
}