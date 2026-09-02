package net.dawson.adorablehamsterpets.client.announcements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Manages fetching, caching, and displaying remote announcements and update notifications.
 * This class is a singleton and handles all client-side state related to announcements.
 */
public class AnnouncementManager {
    // --- 1. Constants, Static Fields, and Nested Types ---
    public static final AnnouncementManager INSTANCE = new AnnouncementManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Active development repository
    private static final String PRIMARY_URL = "https://raw.githubusercontent.com/DawsonBodenhamer/AdorableHamsterPets-Source/develop/announcements/";

    // FDeprecated public repository
    private static final String FALLBACK_URL = "https://raw.githubusercontent.com/DawsonBodenhamer/AdorableHamsterPets-Public/main/announcements/";

    /**
     * A record representing a pending notification, containing the reason it's pending and the announcement itself.
     */
    public record PendingNotification(String reason, Announcement announcement) {
        public static final String UPDATE_AVAILABLE_ANNOUNCEMENT = "update_available_announcement";
        public static final String REGULAR_ANNOUNCEMENT = "regular_announcement";
        public static final String WHATS_NEW_ANNOUNCEMENT = "whats_new_announcement";
    }

    /**
     * Generates the primary tooltip text for a given notification.
     *
     * @param notification The notification to generate text for.
     * @return The formatted Text component for the tooltip.
     */
    public static Component getTooltipTextForNotification(PendingNotification notification) {
        return switch (notification.reason()) {
            case PendingNotification.UPDATE_AVAILABLE_ANNOUNCEMENT ->
                    Component.translatable("tooltip.adorablehamsterpets.hud.update_available_announcement", notification.announcement().semver());
            case PendingNotification.REGULAR_ANNOUNCEMENT ->
                    Component.translatable("tooltip.adorablehamsterpets.hud.regular_announcement");
            default -> Component.translatable("tooltip.adorablehamsterpets.hud.whats_new", notification.announcement().semver());
        };
    }

    // --- 2. Instance Fields ---
    // --- Core State ---
    private ClientAnnouncementState clientState;
    private AnnouncementManifest manifest;

    // --- Initialization & Network ---
    private boolean initialized = false;
    private boolean hasRefreshedThisSession = false;
    private boolean manifestJustLoaded = false;
    private boolean manifestLoaded = false;
    private HttpClient httpClient;
    private CompletableFuture<Void> activeRefreshFuture = CompletableFuture.completedFuture(null);

    // --- Session State ---
    private final Set<String> sessionSnoozedIds = new HashSet<>();
    private final Set<Identifier> deferredReadMarks = new HashSet<>();
    private boolean patchouliStateSynced = false;

    // --- File Paths ---
    private Path stateFilePath;
    private Path manifestCacheFilePath;

    // --- 3. Constructor ---
    /**
     * Private constructor to enforce the singleton pattern.
     * Initializes with default empty states.
     */
    private AnnouncementManager() {
        this.clientState = ClientAnnouncementState.createDefault();
        this.manifest = AnnouncementManifest.empty();
    }

    // --- 4. Public API ---
    // --- Lifecycle & Initialization ---
    /**
     * Ensures the manifest is only fetched once per game session. This is the primary entry point for triggering a refresh.
     *
     * @return A CompletableFuture that completes when the manifest fetch is finished.
     */
    public CompletableFuture<Void> refreshManifestOnce() {
        ensureInitialized();
        if (!hasRefreshedThisSession) {
            hasRefreshedThisSession = true;
            return refreshManifest();
        }
        // If already refreshed, return the active (or last completed) future.
        return activeRefreshFuture;
    }

    /**
     * Asynchronously fetches the markdown content for a specific announcement.
     * <p>
     * Logic:
     * 1. Try fetching from the Primary Source (Active Dev Repo).
     * 2. If 404/Error, try fetching from the Fallback Source (Old Public Repo).
     * 3. If both fail, return the offline error message.
     *
     * @param relativePath The path to the markdown file relative to the announcements directory.
     * @return A CompletableFuture containing the markdown content as a string.
     */
    public CompletableFuture<String> fetchMarkdown(String relativePath) {
        ensureInitialized();

        HttpRequest primaryRequest = HttpRequest.newBuilder()
                .uri(URI.create(PRIMARY_URL + relativePath))
                .GET()
                .build();

        // --- Step 1: Try Primary Source ---
        return httpClient.sendAsync(primaryRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        // Success on Primary
                        return CompletableFuture.completedFuture(response.body());
                    } else {
                        // Primary failed (probably 404), Log warning and try fallback
                        AdorableHamsterPets.LOGGER.warn("[Announcements] Failed to fetch markdown from Primary '{}' (Status {}). Attempting fallback...", relativePath, response.statusCode());

                        HttpRequest fallbackRequest = HttpRequest.newBuilder()
                                .uri(URI.create(FALLBACK_URL + relativePath))
                                .GET()
                                .build();

                        // --- Step 2: Try Fallback Source ---
                        return httpClient.sendAsync(fallbackRequest, HttpResponse.BodyHandlers.ofString())
                                .thenApply(fallbackResponse -> {
                                    if (fallbackResponse.statusCode() == 200) {
                                        AdorableHamsterPets.LOGGER.info("[Announcements] Successfully fetched markdown from Fallback '{}'.", relativePath);
                                        return fallbackResponse.body();
                                    } else {
                                        // Fallback failed too
                                        AdorableHamsterPets.LOGGER.warn("[Announcements] Failed to fetch markdown from Fallback '{}' (Status {}).", relativePath, fallbackResponse.statusCode());
                                        return getOfflineMessage();
                                    }
                                });
                    }
                })
                .exceptionally(e -> {
                    // Network error on either request
                    AdorableHamsterPets.LOGGER.warn("[Announcements] Exception while fetching markdown (Primary or Fallback) for '{}': {}", relativePath, e.toString());
                    return getOfflineMessage();
                });
    }

    private String getOfflineMessage() {
        return """
        # Oops! Looks like this doesn't exist.
        
        There was supposed to be a really fancy announcement message here, so either you've misplaced your internet connection, or I misplaced the message.
        
        You can always [join the Discord](https://discord.gg/w54mk5bqdf) to see the latest announcements there!
        """;
    }

    // --- State Querying ---
    /**
     * Calculates the list of currently pending notifications for the user.
     * This is the core logic that determines if the notification icon should be shown.
     *
     * @return A list of {@link PendingNotification} records, sorted with the newest first.
     */
    public List<PendingNotification> getPendingNotifications() {
        ensureInitialized();
        if (!this.manifestLoaded) {
            return Collections.emptyList(); // Guard against race condition
        }

        AdorableHamsterPets.LOGGER.trace("[Announcements] Running getPendingNotifications check...");
        List<PendingNotification> pending = new ArrayList<>();
        if (manifest == null || manifest.messages().isEmpty()) {
            AdorableHamsterPets.LOGGER.error("[Announcements] -> Check failed: Manifest is null or empty.");
            return pending; // Nothing to do if manifest is not loaded
        }

        Semver installedVersion = Semver.parse(Platform.getMod(AdorableHamsterPets.MOD_ID).getVersion().toString());
        Semver lastAckVersion = Semver.parse(clientState.last_acknowledged_update());
        Instant now = Instant.now();

        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Versions: Installed={}, LastAck={}", installedVersion, lastAckVersion);
        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Snooze IDs: {}.", clientState.snoozed_ids());

        for (Announcement message : manifest.messages()) {
            // Check if it has been seen, snoozed (days), or snoozed (session)
            if (clientState.seen_ids().contains(message.id())
                    || clientState.snoozed_ids().getOrDefault(message.id(), Instant.EPOCH).isAfter(now)
                    || sessionSnoozedIds.contains(message.id())) {
                continue;
            }

            Semver messageVersion = Semver.parse(message.semver());
            String kind = message.kind();

            // --- Backwards Compatibility Shim ---
            // If JSON says "update", resolve it to specific types based on version comparison
            if ("update".equals(kind)) {
                if (message.id().startsWith("update-")) {
                    kind = "update_available";
                } else {
                    kind = messageVersion.compareTo(installedVersion) > 0 ? "update_available" : "patch_notes";
                }
            }

            // --- 1. Update Available ---
            // Only show if the message version is strictly newer than installed
            if ("update_available".equals(kind)) {
                if (messageVersion.compareTo(installedVersion) > 0) {
                    pending.add(new PendingNotification(PendingNotification.UPDATE_AVAILABLE_ANNOUNCEMENT, message));
                    AdorableHamsterPets.LOGGER.trace("[Announcements] -> ADDED (Update Available): id='{}', semver='{}'", message.id(), message.semver());
                }
            }

            // --- 2. Patch Notes (What's New) ---
            // Only show if the message version = the current installed version and is newer than the last acknowledged version
            else if ("patch_notes".equals(kind)) {
                if (messageVersion.compareTo(installedVersion) <= 0 && messageVersion.compareTo(lastAckVersion) > 0) {
                    pending.add(new PendingNotification(PendingNotification.WHATS_NEW_ANNOUNCEMENT, message));
                    AdorableHamsterPets.LOGGER.trace("[Announcements] -> ADDED (What's New): id='{}', semver='{}'", message.id(), message.semver());
                }
            }

            // --- 3. Regular Announcement ---
            else if ("announcement".equals(kind)) {
                pending.add(new PendingNotification(PendingNotification.REGULAR_ANNOUNCEMENT, message));
                AdorableHamsterPets.LOGGER.trace("[Announcements] -> ADDED (Regular): id='{}'", message.id());
            }
        }

        // --- 4. Sort by publication date, newest first ---
        pending.sort(Comparator.comparing((PendingNotification p) -> p.announcement().published()).reversed());
        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Final pending count: {}", pending.size());
        return pending;
    }

    /**
     * Gets the current client-side announcement state record, which contains seen IDs, snoozed IDs, etc.
     *
     * @return The current ClientAnnouncementState record.
     */
    public ClientAnnouncementState getClientState() {
        ensureInitialized();
        return this.clientState;
    }

    /**
     * Retrieves a specific announcement by its unique ID from the current manifest.
     *
     * @param id The ID of the announcement to find.
     * @return The {@link Announcement} if found, otherwise null.
     */
    public Announcement getAnnouncementById(String id) {
        ensureInitialized();
        return manifest.messages().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the complete list of all announcements from the current manifest.
     *
     * @return A list of all {@link Announcement} objects.
     */
    public List<Announcement> getAllManifestMessages() {
        ensureInitialized();
        return manifest != null ? manifest.messages() : List.of();
    }

    /**
     * Determines the canonical "reason" for a notification based on its properties and the current game state.
     *
     * @param announcementId The ID of the announcement to check.
     * @return The reason string (e.g., "update_available_announcement").
     */
    public String getCanonicalReasonForAnnouncement(String announcementId) {
        ensureInitialized();
        Announcement announcement = getAnnouncementById(announcementId);
        if (announcement == null) return "unknown"; // Fallback for safety

        String kind = announcement.kind();

        // --- Backwards Compatibility Shim ---
        if ("update".equals(kind)) {
            if (announcement.id().startsWith("update-")) {
                kind = "update_available";
            } else {
                // Safety check: verify it is actually an update
                Semver installedVersion = Semver.parse(Platform.getMod(AdorableHamsterPets.MOD_ID).getVersion().toString());
                Semver messageVersion = Semver.parse(announcement.semver());
                kind = messageVersion.compareTo(installedVersion) > 0 ? "update_available" : "patch_notes";
            }
        }

        if ("update_available".equals(kind)) {
            return PendingNotification.UPDATE_AVAILABLE_ANNOUNCEMENT;
        }

        if ("patch_notes".equals(kind)) {
            return PendingNotification.WHATS_NEW_ANNOUNCEMENT;
        }

        return PendingNotification.REGULAR_ANNOUNCEMENT;
    }

    /**
     * Checks if the initial synchronization of read states with Patchouli has been completed for this session.
     *
     * @return True if the sync is complete, false otherwise.
     */
    public boolean isPatchouliStateSynced() {
        return this.patchouliStateSynced;
    }

    // --- State Modification ---
    /**
     * Marks a specific announcement as "seen" by the user and persists this state to disk.
     *
     * @param id The ID of the announcement to mark as seen.
     */
    public void markAsSeen(String id) {
        ensureInitialized();
        Set<String> newSeenIds = new HashSet<>(clientState.seen_ids());
        if (newSeenIds.add(id)) {
            clientState = new ClientAnnouncementState(
                    newSeenIds,
                    clientState.snoozed_ids(),
                    clientState.last_acknowledged_update(),
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
        }
    }

    /**
     * Marks all available announcements as read. This is triggered by a user action.
     */
    public void markAllAsRead() {
        ensureInitialized();

        // 26.2 port: no guide book to mirror read state into.

        // Create a mutable copy of the seen IDs to modify
        Set<String> newSeenIds = new HashSet<>(clientState.seen_ids());
        boolean changed = false;

        for (Announcement announcement : getAllManifestMessages()) {
            // Add the ID to the set. The return value of add() tells us if it was a new addition.
            if (newSeenIds.add(announcement.id())) {
                changed = true;
            }

            // Acknowledge both types of update-related messages
            if ("update_available".equals(announcement.kind()) || "patch_notes".equals(announcement.kind())) {
                setLastAcknowledgedUpdate(announcement.semver());
            }

        }

        // Only save the state if something actually changed
        if (changed) {
            clientState = new ClientAnnouncementState(
                    newSeenIds,
                    clientState.snoozed_ids(),
                    clientState.last_acknowledged_update(),
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
            AdorableHamsterPets.LOGGER.info("Marked all announcements as read via config action.");
        }
    }

    /**
     * Updates the last acknowledged update version if the new version is greater.
     *
     * @param version The semantic version string to acknowledge.
     */
    public void setLastAcknowledgedUpdate(String version) {
        ensureInitialized();
        Semver currentAck = Semver.parse(clientState.last_acknowledged_update());
        Semver newAck = Semver.parse(version);
        if (newAck.compareTo(currentAck) > 0) {
            clientState = new ClientAnnouncementState(
                    clientState.seen_ids(),
                    clientState.snoozed_ids(),
                    newAck.toString(),
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
        }
    }

    /**
     * Snoozes a specific announcement for a configured number of days. This state is persisted.
     *
     * @param id   The ID of the announcement to snooze.
     * @param days The number of days to snooze for.
     */
    public void setSnooze(String id, int days) {
        ensureInitialized();
        Instant snoozeUntil = Instant.now().plus(days, ChronoUnit.DAYS);
        Map<String, Instant> newSnoozedIds = new HashMap<>(clientState.snoozed_ids());
        newSnoozedIds.put(id, snoozeUntil);

        clientState = new ClientAnnouncementState(
                clientState.seen_ids(),
                newSnoozedIds,
                clientState.last_acknowledged_update(),
                clientState.manifest_etag(),
                clientState.manifest_last_modified()
        );
        saveState();
    }

    /**
     * Snoozes a specific announcement for the duration of the current game session only. This state is not persisted.
     *
     * @param id The ID of the announcement to snooze for the session.
     */
    public void snoozeForSession(String id) {
        ensureInitialized();
        sessionSnoozedIds.add(id);
    }

    /**
     * Resets all client-side announcement state, including seen, snoozed, and acknowledged versions.
     */
    public void resetClientState() {
        ensureInitialized();
        this.clientState = ClientAnnouncementState.createDefault();
        saveState();
        PatchouliIntegration.clearAllVirtualEntriesFromHistory();
        AdorableHamsterPets.LOGGER.info("Client announcement state has been reset.");
    }

    // --- Patchouli Integration ---
    /**
     * Queues a virtual entry's ID to be marked as read in Patchouli's data at a later, safer time.
     *
     * @param entryId The Identifier of the virtual BookEntry.
     */
    public void queueDeferredReadMark(Identifier entryId) {
        ensureInitialized();
        this.deferredReadMarks.add(entryId);
        AdorableHamsterPets.LOGGER.trace("[Announcements] Queued deferred read mark for entry: {}", entryId);
    }

    /**
     * Processes all queued deferred read marks. Called when the client joins a world.
     */
    public void processDeferredReadMarks() {
        ensureInitialized();
        if (deferredReadMarks.isEmpty()) {
            return;
        }

        AdorableHamsterPets.LOGGER.trace("[Announcements] Processing {} deferred read marks...", deferredReadMarks.size());
        // 26.2 port: without the book these marks have nowhere to go, but the
        // queue is still drained so it cannot grow unbounded.
        int successCount = deferredReadMarks.size();

        if (successCount > 0) {
            AdorableHamsterPets.LOGGER.trace("[Announcements] Successfully processed {} deferred read marks.", successCount);
        }

        deferredReadMarks.clear();
    }

    // --- 5. Private Implementation ---
    // --- Initialization & Lifecycle ---
    /**
     * Initializes the manager's components. This is called lazily on first access.
     */
    private void initialize() {
        AdorableHamsterPets.LOGGER.trace("[Announcements] Initializing AnnouncementManager...");
        this.httpClient = HttpClient.newHttpClient();
        Path configDir = Platform.getConfigFolder().resolve(AdorableHamsterPets.MOD_ID);
        this.stateFilePath = configDir.resolve("announcements.json");
        this.manifestCacheFilePath = configDir.resolve("manifest.cache.json");
        AdorableHamsterPets.LOGGER.trace("[Announcements] State file path resolved to: {}", stateFilePath.toAbsolutePath());
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: Failed to create config directory for announcements at {}", configDir.toAbsolutePath(), e);
        }
        loadState();
        loadCachedManifest();
        processExpiredSnoozes();
        AdorableHamsterPets.LOGGER.trace("[Announcements] Initialization complete.");
    }

    /**
     * Ensures that the manager is initialized before any of its methods are used.
     */
    private void ensureInitialized() {
        if (!initialized) {
            initialize();
            initialized = true;
        }
    }

    /**
     * Acknowledges that the manifest has been loaded and processed for the current UI context.
     */
    public void acknowledgeManifestLoad() {
        ensureInitialized();
        this.manifestJustLoaded = false;
    }

    // --- Network & Caching ---
    /**
     * Ensures the manifest is refreshed if needed, returning a future that completes when the refresh is done.
     * This method is the primary entry point for triggering a refresh.
     *
     * @return A CompletableFuture that completes when the manifest fetch is finished.
     */
    public CompletableFuture<Void> refreshManifest() {
        ensureInitialized();

        if (!activeRefreshFuture.isDone()) {
            return activeRefreshFuture;
        }

        // If the user is on the latest version of the mod, they should be pulling from the source repo
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(PRIMARY_URL + "manifest.json"))
                .GET();

        clientState.manifest_etag().ifPresent(etag -> requestBuilder.header("If-None-Match", etag));
        clientState.manifest_last_modified().ifPresent(lastModified -> requestBuilder.header("If-Modified-Since", lastModified));

        activeRefreshFuture = httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    AdorableHamsterPets.LOGGER.trace("[Announcements] Manifest fetch completed with status code {}. Current screen: {}", response.statusCode(), Minecraft.getInstance().screen);
                    if (response.statusCode() == 200) { // OK
                        AdorableHamsterPets.LOGGER.trace("[Announcements] Fetched new manifest.");
                        AnnouncementManifest.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(response.body(), com.google.gson.JsonElement.class))
                                .resultOrPartial(AdorableHamsterPets.LOGGER::error)
                                .ifPresent(newManifest -> {
                                    this.manifest = newManifest;
                                    this.manifestLoaded = true;
                                    saveManifestToCache();

                                    Optional<String> etag = response.headers().firstValue("ETag");
                                    Optional<String> lastModified = response.headers().firstValue("Last-Modified");

                                    this.clientState = new ClientAnnouncementState(
                                            clientState.seen_ids(),
                                            clientState.snoozed_ids(),
                                            clientState.last_acknowledged_update(),
                                            etag,
                                            lastModified
                                    );
                                    saveState();
                                    this.manifestJustLoaded = true;

                                    if (Minecraft.getInstance().level != null) {
                                        Minecraft.getInstance().execute(() -> {
                                            acknowledgeManifestLoad();
                                        });
                                    }
                                });
                    } else if (response.statusCode() == 304) { // Not Modified
                        AdorableHamsterPets.LOGGER.trace("[Announcements] Manifest is up to date (304 Not Modified).");
                    } else {
                        AdorableHamsterPets.LOGGER.warn("[Announcements] Failed to fetch manifest, status code: {}", response.statusCode());
                    }
                }).exceptionally(e -> {
                    AdorableHamsterPets.LOGGER.warn("[Announcements] Exception while fetching manifest: {}. Using cached version or offline fallback.", e.toString());
                    // If the manifest is STILL empty (meaning no cache was loaded), create the fallback.
                    if (this.manifest == null || this.manifest.messages().isEmpty()) {
                        this.manifest = createOfflineFallbackManifest();
                        this.manifestLoaded = true;
                        this.manifestJustLoaded = true; // Signal that a "new" manifest is ready

                        if (Minecraft.getInstance().level != null) {
                            Minecraft.getInstance().execute(() -> {
                                acknowledgeManifestLoad();
                            });
                        }
                    }
                    return null;
                });
        return activeRefreshFuture;
    }

    // --- File I/O (State Persistence) ---
    /**
     * Loads the client's announcement state from announcements.json.
     */
    private void loadState() {
        AdorableHamsterPets.LOGGER.trace("[Announcements] Attempting to load state from {}...", stateFilePath.toAbsolutePath());
        if (Files.exists(stateFilePath)) {
            AdorableHamsterPets.LOGGER.trace("[Announcements] announcements.json found. Reading file.");
            try (FileReader reader = new FileReader(stateFilePath.toFile())) {
                ClientAnnouncementState.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(reader, com.google.gson.JsonElement.class))
                        .resultOrPartial(AdorableHamsterPets.LOGGER::error)
                        .ifPresent(state -> this.clientState = state);
            } catch (IOException e) {
                AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: Failed to load announcement state from existing file.", e);
            }
        } else {
            AdorableHamsterPets.LOGGER.trace("[Announcements] announcements.json not found. Creating default state file.");
            saveState(); // Create default file if it doesn't exist
        }
    }

    /**
     * Saves the current client state to announcements.json.
     */
    private void saveState() {
        AdorableHamsterPets.LOGGER.trace("[Announcements] Attempting to save state...");
        ClientAnnouncementState.CODEC.encodeStart(JsonOps.INSTANCE, this.clientState)
                .resultOrPartial(error -> AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: Failed to encode client state to JSON: {}", error))
                .ifPresent(jsonElement -> {
                    AdorableHamsterPets.LOGGER.trace("[Announcements] State encoded successfully. Writing to file: {}", stateFilePath.toAbsolutePath());
                    try (FileWriter writer = new FileWriter(stateFilePath.toFile())) {
                        GSON.toJson(jsonElement, writer);
                        AdorableHamsterPets.LOGGER.trace("[Announcements] Successfully saved announcement state.");
                    } catch (IOException e) {
                        AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: FAILED TO SAVE ANNOUNCEMENT STATE TO FILE.", e);
                    }
                });
    }

    /**
     * Loads the announcement manifest from the local cache file.
     */
    private void loadCachedManifest() {
        if (Files.exists(manifestCacheFilePath)) {
            try (FileReader reader = new FileReader(manifestCacheFilePath.toFile())) {
                AnnouncementManifest.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(reader, com.google.gson.JsonElement.class))
                        .resultOrPartial(AdorableHamsterPets.LOGGER::error)
                        .ifPresent(cachedManifest -> {
                            this.manifest = cachedManifest;
                            this.manifestLoaded = true; // Mark as loaded from cache
                            AdorableHamsterPets.LOGGER.trace("[Announcements] Loaded cached manifest with {} messages.", manifest.messages().size());
                        });
            } catch (IOException e) {
                AdorableHamsterPets.LOGGER.error("[Announcements] Failed to load cached manifest.", e);
            }
        }
    }

    /**
     * Saves the current in-memory manifest to the local cache file.
     */
    private void saveManifestToCache() {
        AnnouncementManifest.CODEC.encodeStart(JsonOps.INSTANCE, this.manifest)
                .resultOrPartial(error -> AdorableHamsterPets.LOGGER.error("[Announcements] Failed to encode manifest for caching: {}", error))
                .ifPresent(jsonElement -> {
                    try (FileWriter writer = new FileWriter(manifestCacheFilePath.toFile())) {
                        GSON.toJson(jsonElement, writer);
                        AdorableHamsterPets.LOGGER.trace("[Announcements] Successfully saved manifest to cache.");
                    } catch (IOException e) {
                        AdorableHamsterPets.LOGGER.error("[Announcements] FAILED TO SAVE MANIFEST TO CACHE.", e);
                    }
                });
    }

    // --- Internal Logic & Syncing ---
    /**
     * Iterates through snoozed announcements and removes any whose snooze period has expired.
     */
    private void processExpiredSnoozes() {
        Instant now = Instant.now();
        Map<String, Instant> newSnoozedIds = new HashMap<>(clientState.snoozed_ids());

        // Use removeIf for safe concurrent modification
        newSnoozedIds.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue())) {
                // Snooze has expired
                Identifier entryId = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "announcement_" + entry.getKey());
                PatchouliIntegration.setEntryAsUnread(entryId);
                return true; // Remove this entry from the map
            }
            return false;
        });

        if (newSnoozedIds.size() != clientState.snoozed_ids().size()) {
            clientState = new ClientAnnouncementState(
                    clientState.seen_ids(),
                    newSnoozedIds,
                    clientState.last_acknowledged_update(),
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
        }
    }

    /**
     * Synchronizes the read state of pending notifications with Patchouli's data.
     */
    public void syncPatchouliReadState() {
        ensureInitialized();

        // 26.2 port: there is no book to wait for or sync against, so this
        // settles immediately instead of retrying every tick forever.
        this.patchouliStateSynced = true;

        List<PendingNotification> pendingNotifications = getPendingNotifications();
        if (pendingNotifications.isEmpty()) {
            AdorableHamsterPets.LOGGER.trace("[Announcements] -> No pending notifications to sync.");
            return;
        }

        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Found {} pending notifications to sync.", pendingNotifications.size());

        for (PendingNotification notification : pendingNotifications) {
            Identifier entryId = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "announcement_" + notification.announcement().id());
            AdorableHamsterPets.LOGGER.trace("[Announcements] -> Syncing entry: {}", entryId);
            boolean success = PatchouliIntegration.setEntryAsUnread(entryId);
            if (success) {
                AdorableHamsterPets.LOGGER.trace("[Announcements] -> Successfully marked {} as unread.", entryId);
            } else {
                // This might log if the entry was already unread.
                AdorableHamsterPets.LOGGER.error("[Announcements] -> Could not mark {} as unread (was it already unread?).", entryId);
            }
        }
    }

    // --- Fallbacks ---
    /**
     * Creates a hardcoded, in-memory Announcement to be used as a fallback when offline.
     *
     * @return A default Announcement object.
     */
    private Announcement createOfflineFallbackAnnouncement() {
        return new Announcement(
                "offline-fallback",
                "No Internet Connection",
                "announcement",
                "0.0.0",
                "offline-fallback.md",  // markdown
                ZonedDateTime.now()              // published
        );
    }

    /**
     * Creates a manifest containing only the offline fallback announcement.
     *
     * @return A default AnnouncementManifest object.
     */
    private AnnouncementManifest createOfflineFallbackManifest() {
        return new AnnouncementManifest("0.0.0", List.of(createOfflineFallbackAnnouncement()));
    }
}