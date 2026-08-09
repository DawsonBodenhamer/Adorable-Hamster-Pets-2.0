package net.dawson.adorablehamsterpets.util;

import java.util.Arrays;

/**
 * Supported optional equipment locations that can own an Acorn Ring identity.
 */
public enum AcornRingLocation {
    TRINKETS_HAND_RING("trinkets:hand/ring", 0),
    CURIOS_RING("curios:ring", 1),
    ACCESSORIES_RING("accessories:ring", 2);

    private final String serializedName;
    private final int legacyPriority;

    AcornRingLocation(String serializedName, int legacyPriority) {
        this.serializedName = serializedName;
        this.legacyPriority = legacyPriority;
    }

    public String serializedName() {
        return serializedName;
    }

    public int legacyPriority() {
        return legacyPriority;
    }

    public static AcornRingLocation fromSerializedName(String serializedName) {
        return Arrays.stream(values())
                .filter(location -> location.serializedName.equals(serializedName))
                .findFirst()
                .orElse(null);
    }
}
