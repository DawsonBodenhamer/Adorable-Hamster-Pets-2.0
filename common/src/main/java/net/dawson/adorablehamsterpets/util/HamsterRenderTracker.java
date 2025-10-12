package net.dawson.adorablehamsterpets.util;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A server-side utility to track which players are currently rendering which hamsters.
 * This is used to determine when to play server-side fallback sounds (like footsteps)
 * versus relying on client-side animation keyframed sounds.
 */
public class HamsterRenderTracker {
    // A thread-safe map where Key = Hamster Entity ID, Value = Set of Player UUIDs rendering it.
    private static final Map<Integer, Set<UUID>> RENDER_TRACKING_MAP = new ConcurrentHashMap<>();

    public static void addPlayer(int hamsterId, UUID playerId) {
        RENDER_TRACKING_MAP.computeIfAbsent(hamsterId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(playerId);
    }

    public static void removePlayer(int hamsterId, UUID playerId) {
        Set<UUID> players = RENDER_TRACKING_MAP.get(hamsterId);
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                RENDER_TRACKING_MAP.remove(hamsterId);
            }
        }
    }

    public static void onPlayerDisconnect(UUID playerId) {
        RENDER_TRACKING_MAP.forEach((hamsterId, playerSet) -> playerSet.remove(playerId));
        RENDER_TRACKING_MAP.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void onEntityUnload(int hamsterId) {
        RENDER_TRACKING_MAP.remove(hamsterId);
    }

    public static boolean isBeingRendered(int hamsterId) {
        Set<UUID> players = RENDER_TRACKING_MAP.get(hamsterId);
        return players != null && !players.isEmpty();
    }
}