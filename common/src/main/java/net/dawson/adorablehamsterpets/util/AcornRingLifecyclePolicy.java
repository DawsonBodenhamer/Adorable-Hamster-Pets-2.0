package net.dawson.adorablehamsterpets.util;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure ownership decisions for the optional Acorn Ring inventories.
 */
public final class AcornRingLifecyclePolicy {

    public record RingCandidate(
            @Nullable UUID identity,
            AcornRingLocation currentLocation,
            @Nullable AcornRingLocation lastLocation) {}

    public record Resolution(
            @Nullable UUID identity,
            AcornRingLocation owner,
            Set<AcornRingLocation> staleLocations,
            boolean assignIdentity,
            boolean recoverLegacyExtras) {}

    public static List<Resolution> resolveAll(
            List<RingCandidate> candidates,
            @Nullable AcornRingLocation preferredLocation) {
        Map<UUID, List<RingCandidate>> identified = new HashMap<>();
        List<RingCandidate> legacy = new ArrayList<>();
        for (RingCandidate candidate : candidates) {
            if (candidate.identity() == null) {
                legacy.add(candidate);
            } else {
                identified.computeIfAbsent(candidate.identity(), ignored -> new ArrayList<>()).add(candidate);
            }
        }

        List<Resolution> resolutions = new ArrayList<>();
        if (!legacy.isEmpty()) {
            RingCandidate owner = chooseOwner(legacy, preferredLocation);
            Set<AcornRingLocation> staleLocations = legacy.stream()
                    .filter(candidate -> candidate != owner)
                    .map(RingCandidate::currentLocation)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            resolutions.add(new Resolution(
                    null,
                    owner.currentLocation(),
                    staleLocations,
                    true,
                    legacy.size() > 1));
        }

        for (Map.Entry<UUID, List<RingCandidate>> entry : identified.entrySet()) {
            RingCandidate owner = chooseOwner(entry.getValue(), preferredLocation);
            Set<AcornRingLocation> staleLocations = entry.getValue().stream()
                    .filter(candidate -> candidate != owner)
                    .map(RingCandidate::currentLocation)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            resolutions.add(new Resolution(
                    entry.getKey(),
                    owner.currentLocation(),
                    staleLocations,
                    false,
                    false));
        }

        return resolutions;
    }

    private static RingCandidate chooseOwner(
            List<RingCandidate> candidates,
            @Nullable AcornRingLocation preferredLocation) {
        if (preferredLocation != null) {
            for (RingCandidate candidate : candidates) {
                if (candidate.currentLocation() == preferredLocation) {
                    return candidate;
                }
            }
        }

        for (RingCandidate candidate : candidates) {
            if (candidate.lastLocation() == candidate.currentLocation()) {
                return candidate;
            }
        }

        return candidates.stream()
                .min(Comparator.comparingInt(candidate -> candidate.currentLocation().legacyPriority()))
                .orElseThrow();
    }

    private AcornRingLifecyclePolicy() {}
}
