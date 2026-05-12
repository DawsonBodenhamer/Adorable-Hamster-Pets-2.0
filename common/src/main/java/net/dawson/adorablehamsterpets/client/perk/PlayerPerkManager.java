package net.dawson.adorablehamsterpets.client.perk;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A client-side singleton that manages player-specific perks.
 * <p>
 * Fetches a JSON manifest from the remote repository containing a map of
 * Perk IDs to lists of authorized usernames. Operates asynchronously to prevent
 * game freezes and falls back to a local cache if offline.
 */
public class PlayerPerkManager {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final PlayerPerkManager INSTANCE = new PlayerPerkManager();
    private static final Gson GSON = new Gson();
    private static final String REMOTE_URL = "https://raw.githubusercontent.com/DawsonBodenhamer/AdorableHamsterPets-Source/develop/perks/perks.json";

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private Map<String, Set<String>> perks = Collections.emptyMap();
    private boolean initialized = false;
    private Path cacheFilePath;
    private HttpClient httpClient;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private PlayerPerkManager() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public void refreshManifestOnce() {
        if (!this.initialized) {
            initialize();
            this.initialized = true;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REMOTE_URL))
                .GET()
                .build();

        this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Type type = new TypeToken<Map<String, Set<String>>>(){}.getType();
                        Map<String, Set<String>> newPerks = GSON.fromJson(response.body(), type);
                        if (newPerks != null) {
                            this.perks = newPerks;
                            saveCache();
                            AdorableHamsterPets.LOGGER.info("[Perks] Successfully refreshed player perks.");
                        }
                    } else {
                        AdorableHamsterPets.LOGGER.warn("[Perks] Failed to fetch perks from remote (Status {}).", response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    AdorableHamsterPets.LOGGER.warn("[Perks] Exception while fetching perks from remote: {}. Using cached data if available.", e.toString());
                    return null;
                });
    }

    /**
     * Checks if a specific player has a specific perk.
     * Includes an automatic bypass if running inside a development environment.
     *
     * @param username The Minecraft username of the player (Case-Sensitive).
     * @param perkId The identifier of the perk (e.g., "supporter_crown").
     * @return True if the player has the perk, false otherwise.
     */
    public boolean hasPerk(String username, String perkId) {
        if (Platform.isDevelopmentEnvironment()) {
            return true;
        }

        Set<String> users = this.perks.get(perkId);
        if (users == null) return false;

        return users.contains(username);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    // Sets up client and loads local backup cache
    private void initialize() {
        this.httpClient = HttpClient.newHttpClient();
        Path configDir = Platform.getConfigFolder().resolve(AdorableHamsterPets.MOD_ID);
        this.cacheFilePath = configDir.resolve("perks.cache.json");

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            AdorableHamsterPets.LOGGER.error("[Perks] Failed to create config directory.", e);
        }

        loadCache();
    }

    // Reads the saved perks from disk
    private void loadCache() {
        if (Files.exists(this.cacheFilePath)) {
            try (FileReader reader = new FileReader(this.cacheFilePath.toFile())) {
                Type type = new TypeToken<Map<String, Set<String>>>(){}.getType();
                Map<String, Set<String>> cached = GSON.fromJson(reader, type);
                if (cached != null) {
                    this.perks = cached;
                }
            } catch (IOException e) {
                AdorableHamsterPets.LOGGER.error("[Perks] Failed to load cached perks.", e);
            }
        }
    }

    // Writes the downloaded perks to disk
    private void saveCache() {
        try (FileWriter writer = new FileWriter(this.cacheFilePath.toFile())) {
            GSON.toJson(this.perks, writer);
        } catch (IOException e) {
            AdorableHamsterPets.LOGGER.error("[Perks] Failed to save perks cache.", e);
        }
    }
}