package net.dawson.adorablehamsterpets.forge.client;

import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
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
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.particle.DefaultParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge-only client initialisation.
 * All class names use Yarn mappings (1.20.1 build 10).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = AdorableHamsterPets.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class AdorableHamsterPetsForgeClient {

    private AdorableHamsterPetsForgeClient() {}

    /* ------------------------------------------------------------ */
    /* Client setup                                                 */
    /* ------------------------------------------------------------ */

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // General Architectury/fabric-style init that must run on the main thread.
        event.enqueueWork(AdorableHamsterPetsClient::init);

        // Register the hamster-inventory screen with its ScreenHandler type.
        event.enqueueWork(() ->
                HandledScreens.register(
                        ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(),
                        HamsterInventoryScreen::new
                )
        );
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Register particle factory for all variants.
        // On 1.20.1, use DefaultParticleType
        for (RegistrySupplier<DefaultParticleType> particleSupplier : ModParticles.BEDDING_PARTICLES.values()) {
            event.registerSpriteSet(particleSupplier.get(), HamsterBeddingParticle.Factory::new);
        }
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Construct the key mapping objects if they haven’t been created yet.
        if (ModKeyBindings.THROW_HAMSTER_KEY == null) {
            ModKeyBindings.init();
        }
        // Manually register the keys with Forge's event.
        event.register(ModKeyBindings.THROW_HAMSTER_KEY);
        event.register(ModKeyBindings.DISMOUNT_HAMSTER_KEY);
    }

    /* ------------------------------------------------------------ */
    /* Renderer & layer registrations                               */
    /* ------------------------------------------------------------ */

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HAMSTER.get(), HamsterRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.HAMSTER_BED_BLOCK_ENTITY.get(), HamsterBedRenderer::new);
    }

    /**
     * Add the shoulder-hamster feature to both vanilla player models.
     * Vanilla (and thus Forge) identify them with the keys
     * {@code "default"} (Steve) and {@code "slim"} (Alex).
     */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Steve
        PlayerEntityRenderer steveRenderer = event.getSkin("default");
        if (steveRenderer != null) {
            steveRenderer.addFeature(new HamsterShoulderFeatureRenderer(steveRenderer));
        }

        // Alex
        PlayerEntityRenderer alexRenderer = event.getSkin("slim");
        if (alexRenderer != null) {
            alexRenderer.addFeature(new HamsterShoulderFeatureRenderer(alexRenderer));
        }
    }
}
