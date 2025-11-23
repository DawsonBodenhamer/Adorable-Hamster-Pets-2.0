package net.dawson.adorablehamsterpets.fabric.datagen;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.AhpConfig;
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
 * 2.  Appends all automatically-generated config-GUI keys from Fzzy Config,
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

        ConfigApiJava.buildTranslations(
                AhpConfig.class,
                Identifier.of(AdorableHamsterPets.MOD_ID, "main"),
                "en_us",
                /* includeDescriptions = */ true,
                safeSingleWriter
        );
    }
}
