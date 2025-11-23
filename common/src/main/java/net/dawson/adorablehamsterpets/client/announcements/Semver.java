package net.dawson.adorablehamsterpets.client.announcements;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import org.jetbrains.annotations.NotNull;

public record Semver(int major, int minor, int patch) implements Comparable<Semver> {

    public static Semver parse(String versionString) {
        if (versionString == null || versionString.isBlank()) {
            return new Semver(0, 0, 0);
        }
        try {
            String[] parts = versionString.split("[.\\-+]"); // Split by '.', '-', or '+'
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new Semver(major, minor, patch);
        } catch (NumberFormatException e) {
            AdorableHamsterPets.LOGGER.warn("Failed to parse semver string: '{}'", versionString);
            return new Semver(0, 0, 0);
        }
    }

    @Override
    public int compareTo(@NotNull Semver other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}