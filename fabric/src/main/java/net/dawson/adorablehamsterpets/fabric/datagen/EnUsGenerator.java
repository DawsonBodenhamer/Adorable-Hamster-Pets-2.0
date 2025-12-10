package net.dawson.adorablehamsterpets.fabric.datagen;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.AhpRootConfig;
import net.dawson.adorablehamsterpets.config.AhpWorldGenConfig;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Generates the final {@code assets/adorablehamsterpets/lang/en_us.json}.
 * <p>
 * 1.  Copies every entry from {@code en_us_base.json}.<br>
 * 2.  Appends all automatically-generated config-GUI keys from Fzzy Config.
 */
public class EnUsGenerator extends FabricLanguageProvider {

    private static final String BASE_RESOURCE_PATH =
            "assets/adorablehamsterpets/lang/en_us_base.json";

    private static final Gson GSON = new Gson();

    public EnUsGenerator(FabricDataOutput output,
                         CompletableFuture<RegistryWrapper.WrapperLookup> lookup) {
        super(output, "en_us", lookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup registries,
                                     TranslationBuilder builder) {

        /* ------------------------------------------------------------
         * 1)  Load every manual translation from en_us_base.json
         * ------------------------------------------------------------ */
        Set<String> seen = new java.util.HashSet<>();

        try (var stream = getClass().getClassLoader()
                .getResourceAsStream(BASE_RESOURCE_PATH)) {

            if (stream != null) {
                JsonObject obj = GSON.fromJson(
                        new InputStreamReader(stream, StandardCharsets.UTF_8),
                        JsonObject.class);

                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    builder.add(e.getKey(), e.getValue().getAsString());
                    seen.add(e.getKey());
                }
            } else {
                AdorableHamsterPets.LOGGER.warn("Could not locate {}", BASE_RESOURCE_PATH);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read " + BASE_RESOURCE_PATH, ex);
        }

        /* ------------------------------------------------------------
         * 2)  Auto-generate config translations.
         *     If a key already exists, skip it.
         * ------------------------------------------------------------ */
        BiConsumer<String, String> safeSingleWriter = (key, value) -> {
            if (seen.add(key)) {
                builder.add(key, value); // Only add the standard key
            }
        };

        // --- Helper to manually scrape class annotations ---
        // TODO: REVERT THIS WORKAROUND IN FZZY CONFIG 0.7.4+
        // In v0.7.3, ConfigApiJava.buildTranslations() ignores class-level descriptions.
        // This is a bug, scheduled to be fixed in v0.7.4. Once updated, DELETE this 'ManualScraper' interface and lambda entirely.
        // Also remove the @Translation annotations from AhpConfig, AhpRootConfig, and AhpWorldGenConfig.
        ManualScraper scraper = (clazz, idStr) -> {
            // Note: The keys here must match the prefix defined in the @Translation annotation on the config class.
            String baseKey = "adorablehamsterpets." + idStr;

            if (clazz.isAnnotationPresent(Translatable.Name.class)) {
                safeSingleWriter.accept(baseKey, clazz.getAnnotation(Translatable.Name.class).value());
            }
            if (clazz.isAnnotationPresent(Translatable.Desc.class)) {
                safeSingleWriter.accept(baseKey + ".desc", clazz.getAnnotation(Translatable.Desc.class).value());
            }
        };

        // 1. Generate for Root Config
        scraper.scrape(AhpRootConfig.class, "root");
        ConfigApiJava.buildTranslations(
                AhpRootConfig.class,
                Identifier.of(AdorableHamsterPets.MOD_ID, "root"),
                "en_us",
                false,
                safeSingleWriter
        );

        // 2. Generate for Main Config
        scraper.scrape(AhpConfig.class, "main");
        ConfigApiJava.buildTranslations(
                AhpConfig.class,
                Identifier.of(AdorableHamsterPets.MOD_ID, "main"),
                "en_us",
                false,
                safeSingleWriter
        );

        // 3. Generate for WorldGen Config
        scraper.scrape(AhpWorldGenConfig.class, "worldgen");
        ConfigApiJava.buildTranslations(
                AhpWorldGenConfig.class,
                Identifier.of(AdorableHamsterPets.MOD_ID, "worldgen"),
                "en_us",
                false,
                safeSingleWriter
        );
    }

    @FunctionalInterface
    private interface ManualScraper {
        void scrape(Class<?> clazz, String idStr);
    }
}
