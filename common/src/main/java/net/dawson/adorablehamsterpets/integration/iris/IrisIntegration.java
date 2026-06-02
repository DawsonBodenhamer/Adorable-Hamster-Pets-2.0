package net.dawson.adorablehamsterpets.integration.iris;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.render.HamsterPBRTexture;
import net.irisshaders.iris.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.pbr.loader.PBRTextureLoaderRegistry;
import net.minecraft.resource.ResourceManager;

/**
 * Safely manages the integration between Adorable Hamster Pets and Iris Shaders.
 */
public class IrisIntegration {

    /**
     * Called during client initialization.
     * Safely checks for the Iris mod before attempting to execute any Iris-specific code.
     */
    public static void init() {
        if (Platform.isModLoaded("iris")) {
            try {
                IrisWrapper.registerPbrLoader();
                AdorableHamsterPets.LOGGER.info("[AHP] Successfully integrated with Iris PBR registry.");
            } catch (Throwable t) {
                AdorableHamsterPets.LOGGER.error("[AHP] Failed to integrate with Iris PBR registry.", t);
            }
        }
    }

    /**
     * Inner wrapper class to prevent Java from attempting to load Iris API classes
     * if the mod is not installed, which would cause a NoClassDefFoundError crash.
     */
    private static class IrisWrapper {
        static void registerPbrLoader() {
            // Register a custom loader specifically for our HamsterPBRTexture class
            PBRTextureLoaderRegistry.INSTANCE.register(
                    HamsterPBRTexture.class,
                    new PBRTextureLoader<HamsterPBRTexture>() {
                        @Override
                        public void load(HamsterPBRTexture texture, ResourceManager resourceManager, PBRTextureConsumer consumer) {
                            // Feed the normal and specular maps to Iris when it asks for them
                            if (texture.getNormalTexture() != null) {
                                consumer.acceptNormalTexture(texture.getNormalTexture());
                            }
                            if (texture.getSpecularTexture() != null) {
                                consumer.acceptSpecularTexture(texture.getSpecularTexture());
                            }
                        }
                    }
            );
        }
    }
}