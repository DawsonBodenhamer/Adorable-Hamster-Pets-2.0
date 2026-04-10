package net.dawson.adorablehamsterpets.neoforge.client;

import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.client.HamsterBedRenderer;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticle;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.dawson.adorablehamsterpets.client.render.LeafJiggleRenderer;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.feature.HamsterShoulderFeatureRenderer;
import net.dawson.adorablehamsterpets.entity.client.renderer.HamsterTreeSearcherRenderer;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.particle.SimpleParticleType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;

public final class AdorableHamsterPetsNeoForgeClient {

    private AdorableHamsterPetsNeoForgeClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // General setup.
        event.enqueueWork(() -> {
            AdorableHamsterPetsClient.init();

            // Register world render hook on the game bus
            NeoForge.EVENT_BUS.addListener(AdorableHamsterPetsNeoForgeClient::onRenderLevelStage);
        });
    }

    // --- Render Level Hook ---
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        MinecraftClient client = MinecraftClient.getInstance();

        // Retrieve the buffer source from the client's buffer builders
        VertexConsumerProvider consumers = client.getBufferBuilders().getEntityVertexConsumers();

        LeafJiggleRenderer.render(
                client,
                event.getPoseStack(),
                consumers,
                event.getCamera().getPos(),
                event.getPartialTick().getTickDelta(client.isPaused())
        );
    }

    // --- Register Particle Factory ---
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        for (RegistrySupplier<SimpleParticleType> particleSupplier : ModParticles.BEDDING_PARTICLES.values()) {
            event.registerSpriteSet(particleSupplier.get(), HamsterBeddingParticle.Factory::new);
        }

        for (PixieDustParticleTheme theme : PixieDustParticleTheme.values()) {
            RegistrySupplier<SimpleParticleType> supplier = ModParticles.PIXIE_DUST.get(theme);
            event.registerSpriteSet(supplier.get(), provider -> new PixieDustParticle.Factory(provider, theme));
        }
    }

    /**
     * Register key mappings using the NeoForge event.
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Initialize all key binding objects.
        ModKeyBindings.init();

        // Use the event to register the key mapping
        event.register(ModKeyBindings.THROW_HAMSTER_KEY);
        event.register(ModKeyBindings.TOGGLE_SUPPORTER_CROWN_KEY);
        event.register(ModKeyBindings.DISMOUNT_HAMSTER_KEY);
        event.register(ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY);
        event.register(ModKeyBindings.RIDE_HAMSTER_KEY);
        event.register(ModKeyBindings.GENETICS_VISUALIZER_VAR_UP_KEY);
        event.register(ModKeyBindings.GENETICS_VISUALIZER_VAR_DOWN_KEY);
        event.register(ModKeyBindings.GENETICS_VISUALIZER_MUT_UP_KEY);
        event.register(ModKeyBindings.GENETICS_VISUALIZER_MUT_DOWN_KEY);
        event.register(ModKeyBindings.TOGGLE_PERFORMANCE_MODE_KEY);
    }

    /**
     * Listens for the RegisterMenuScreensEvent to safely register our custom screen factory.
     * This is the correct time to do this on NeoForge.
     * @param event The screen registration event.
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), HamsterInventoryScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HAMSTER.get(), HamsterRenderer::new);
        event.registerEntityRenderer(ModEntities.HAMSTER_TREE_SEARCHER.get(), HamsterTreeSearcherRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), HamsterBedRenderer::new);
    }

    /**
     * Listens for the AddLayers event to safely add the shoulder hamster feature renderer
     * to the default and slim player models. This event runs after layer definitions are registered.
     * @param event The layer addition event.
     */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Get the renderers for both the default ("wide") and "slim" player models
        LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> defaultSkin = event.getSkin(SkinTextures.Model.WIDE);
        LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> slimSkin = event.getSkin(SkinTextures.Model.SLIM);

        if (defaultSkin != null) {
            defaultSkin.addFeature(new HamsterShoulderFeatureRenderer(defaultSkin));
        }

        if (slimSkin != null) {
            slimSkin.addFeature(new HamsterShoulderFeatureRenderer(slimSkin));
        }
    }
}