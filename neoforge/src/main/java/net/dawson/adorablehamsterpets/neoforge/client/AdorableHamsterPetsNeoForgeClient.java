package net.dawson.adorablehamsterpets.neoforge.client;

/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.block.ModBlockEntities;
import net.dawson.adorablehamsterpets.block.client.HamsterBedRenderer;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.particle.HamsterBeddingParticle;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.feature.HamsterShoulderFeatureRenderer;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.particle.SimpleParticleType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class AdorableHamsterPetsNeoForgeClient {

    private AdorableHamsterPetsNeoForgeClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // General setup.
        event.enqueueWork(AdorableHamsterPetsClient::init);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Register particle factory for all variants.
        for (RegistrySupplier<SimpleParticleType> particleSupplier : ModParticles.BEDDING_PARTICLES.values()) {
            event.registerSpriteSet(particleSupplier.get(), HamsterBeddingParticle.Factory::new);
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
        event.register(ModKeyBindings.DISMOUNT_HAMSTER_KEY);
        event.register(ModKeyBindings.FORCE_MOUNT_HAMSTER_KEY);
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