package net.dawson.adorablehamsterpets.client.link;

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

/**
 * A client-side singleton that manages dynamic remote links.
 * <p>
 * Fetches a JSON manifest from the remote repository containing a map of
 * Link IDs to URLs. Operates asynchronously to prevent game freezes and
 * falls back to a local cache or hardcoded defaults if offline.
 */
public class RemoteLinkManager {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final RemoteLinkManager INSTANCE = new RemoteLinkManager();
    private static final Gson GSON = new Gson();
    private static final String REMOTE_URL = "https://raw.githubusercontent.com/DawsonBodenhamer/AdorableHamsterPets-Source/develop/links/links.json";

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    private Map<String, String> links = Collections.emptyMap();
    private boolean initialized = false;
    private Path cacheFilePath;
    private HttpClient httpClient;

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private RemoteLinkManager() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public void refreshLinksOnce() {
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
                        Type type = new TypeToken<Map<String, String>>(){}.getType();
                        Map<String, String> newLinks = GSON.fromJson(response.body(), type);
                        if (newLinks != null) {
                            this.links = newLinks;
                            saveCache();
                            AdorableHamsterPets.LOGGER.info("[RemoteLinks] Successfully refreshed remote links.");
                        }
                    } else {
                        AdorableHamsterPets.LOGGER.warn("[RemoteLinks] Failed to fetch links from remote (Status {}).", response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    AdorableHamsterPets.LOGGER.warn("[RemoteLinks] Exception while fetching links from remote: {}. Using cached data if available.", e.toString());
                    return null;
                });
    }

    /**
     * Retrieves a dynamic URL by its ID.
     *
     * @param linkId The identifier of the link (e.g., "punchy_showcase").
     * @param fallbackUrl The default URL to return if the ID is missing or the map is empty.
     * @return The requested URL, or the fallback.
     */
    public String getLink(String linkId, String fallbackUrl) {
        return this.links.getOrDefault(linkId, fallbackUrl);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void initialize() {
        this.httpClient = HttpClient.newHttpClient();
        Path configDir = Platform.getConfigFolder().resolve(AdorableHamsterPets.MOD_ID);
        this.cacheFilePath = configDir.resolve("links.cache.json");

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            AdorableHamsterPets.LOGGER.error("[RemoteLinks] Failed to create config directory.", e);
        }

        loadCache();
    }

    private void loadCache() {
        if (Files.exists(this.cacheFilePath)) {
            try (FileReader reader = new FileReader(this.cacheFilePath.toFile())) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> cached = GSON.fromJson(reader, type);
                if (cached != null) {
                    this.links = cached;
                }
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.error("[RemoteLinks] Failed to load cached links or file was corrupted.", e);
            }
        }
    }

    private void saveCache() {
        try (FileWriter writer = new FileWriter(this.cacheFilePath.toFile())) {
            GSON.toJson(this.links, writer);
        } catch (IOException e) {
            AdorableHamsterPets.LOGGER.error("[RemoteLinks] Failed to save links cache.", e);
        }
    }
}