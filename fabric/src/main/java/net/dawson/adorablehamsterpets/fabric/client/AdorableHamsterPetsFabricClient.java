package net.dawson.adorablehamsterpets.fabric.client;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticle;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.dawson.adorablehamsterpets.client.render.BlockJiggleRenderer;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.renderer.HamsterBlockHiderRenderer;
import net.dawson.adorablehamsterpets.entity.client.renderer.HamsterProjectileRenderer;
import net.dawson.adorablehamsterpets.entity.client.renderer.HamsterTreeSearcherRenderer;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.SimpleParticleType;

public final class AdorableHamsterPetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AdorableHamsterPetsClient.init();
        AdorableHamsterPetsClient.initScreenHandlers();
        EntityRendererRegistry.register(ModEntities.HAMSTER.get(), HamsterRenderer::new);
        EntityRendererRegistry.register(ModEntities.HAMSTER_TREE_SEARCHER.get(), HamsterTreeSearcherRenderer::new);
        EntityRendererRegistry.register(ModEntities.HAMSTER_BLOCK_HIDER.get(), HamsterBlockHiderRenderer::new);
        EntityRendererRegistry.register(ModEntities.HAMSTER_PROJECTILE.get(), HamsterProjectileRenderer::new);
        AdorableHamsterPetsClient.initBlockEntityRenderers();

        // --- Register keybindings for Fabric ---
        ModKeyBindings.init();
        KeyMappingRegistry.register(ModKeyBindings.THROW_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.TOGGLE_SUPPORTER_CROWN_KEY);
        KeyMappingRegistry.register(ModKeyBindings.DISMOUNT_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.PET_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.RIDE_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.GENETICS_VISUALIZER_VAR_UP_KEY);
        KeyMappingRegistry.register(ModKeyBindings.GENETICS_VISUALIZER_VAR_DOWN_KEY);
        KeyMappingRegistry.register(ModKeyBindings.GENETICS_VISUALIZER_MUT_UP_KEY);
        KeyMappingRegistry.register(ModKeyBindings.GENETICS_VISUALIZER_MUT_DOWN_KEY);
        KeyMappingRegistry.register(ModKeyBindings.TOGGLE_PERFORMANCE_MODE_KEY);

        // --- Register Particle Provider ---
        for (RegistrySupplier<SimpleParticleType> particleSupplier : ModParticles.BEDDING_PARTICLES.values()) {
            ParticleFactoryRegistry.getInstance().register(particleSupplier.get(), HamsterBeddingParticle.Factory::new);
        }

        for (PixieDustParticleTheme theme : PixieDustParticleTheme.values()) {
            RegistrySupplier<SimpleParticleType> supplier = ModParticles.PIXIE_DUST.get(theme);
            ParticleFactoryRegistry.getInstance().register(supplier.get(), provider -> new PixieDustParticle.Factory(provider, theme));
        }

        // --- Register Block Jiggle Renderer ---
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft client = Minecraft.getInstance();

            BlockJiggleRenderer.render(
                    client,
                    context.matrixStack(),
                    context.consumers(),
                    context.camera().getPosition(),
                    context.tickCounter().getGameTimeDeltaPartialTick(client.isPaused())
            );
        });
    }
}