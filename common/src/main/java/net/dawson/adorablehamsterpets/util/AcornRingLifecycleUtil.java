package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Applies the cross-API Acorn Ring ownership policy to the live optional inventories.
 */
public final class AcornRingLifecycleUtil {

    public record EquippedRing(AcornRingLocation location, ItemStack stack, Consumer<ItemStack> replace) {

        public void clear() {
            replace.accept(ItemStack.EMPTY);
        }
    }

    public static void reconcile(
            ServerPlayerEntity player,
            List<EquippedRing> equippedRings,
            Set<UUID> removedIdentities,
            @Nullable AcornRingLocation preferredLocation) {
        if (equippedRings.isEmpty()) {
            return;
        }

        removeIdentitiesBeingUnequipped(player, equippedRings, removedIdentities);

        List<EquippedRing> legacyRings = equippedRings.stream()
                .filter(ring -> AcornRingStackIdentity.getId(ring.stack()) == null)
                .toList();
        if (legacyRings.size() > 1) {
            reconcileLegacyDuplicates(player, legacyRings, preferredLocation);
        } else if (legacyRings.size() == 1) {
            assignIdentity(legacyRings.getFirst());
        }

        Map<UUID, List<EquippedRing>> ringsByIdentity = new HashMap<>();
        for (EquippedRing ring : equippedRings) {
            UUID id = AcornRingStackIdentity.getId(ring.stack());
            if (id != null) {
                ringsByIdentity.computeIfAbsent(id, ignored -> new ArrayList<>()).add(ring);
            }
        }

        for (List<EquippedRing> rings : ringsByIdentity.values()) {
            EquippedRing owner = chooseOwner(rings, preferredLocation);
            for (EquippedRing ring : rings) {
                if (ring != owner) {
                    ring.clear();
                }
            }
            setLocationIfChanged(owner);
        }
    }

    static AcornRingLocation chooseLegacyOwner(
            List<AcornRingLocation> locations,
            @Nullable AcornRingLocation preferredLocation) {
        if (preferredLocation != null && locations.contains(preferredLocation)) {
            return preferredLocation;
        }

        return locations.stream()
                .min(Comparator.comparingInt(AcornRingLocation::legacyPriority))
                .orElse(null);
    }

    static AcornRingLocation chooseMarkedOwner(
            List<EquippedRing> rings,
            @Nullable AcornRingLocation preferredLocation) {
        if (preferredLocation != null) {
            for (EquippedRing ring : rings) {
                if (ring.location() == preferredLocation) {
                    return ring.location();
                }
            }
        }

        for (EquippedRing ring : rings) {
            if (AcornRingStackIdentity.getLastLocation(ring.stack()) == ring.location()) {
                return ring.location();
            }
        }

        return rings.stream()
                .map(EquippedRing::location)
                .min(Comparator.comparingInt(AcornRingLocation::legacyPriority))
                .orElse(null);
    }

    private static void removeIdentitiesBeingUnequipped(
            ServerPlayerEntity player,
            List<EquippedRing> equippedRings,
            Set<UUID> removedIdentities) {
        if (removedIdentities.isEmpty()) {
            return;
        }

        Set<EquippedRing> staleRings = new HashSet<>();
        for (EquippedRing ring : equippedRings) {
            UUID id = AcornRingStackIdentity.getId(ring.stack());
            if (id != null
                    && removedIdentities.contains(id)
                    && hasOrdinaryInventoryIdentity(player, id)) {
                staleRings.add(ring);
            }
        }

        staleRings.forEach(EquippedRing::clear);
        equippedRings.removeAll(staleRings);
    }

    private static boolean hasOrdinaryInventoryIdentity(ServerPlayerEntity player, UUID identity) {
        for (int index = 0; index < player.getInventory().size(); index++) {
            ItemStack stack = player.getInventory().getStack(index);
            if (stack.isOf(ModItems.ACORN_RING.get())
                    && identity.equals(AcornRingStackIdentity.getId(stack))) {
                return true;
            }
        }
        return false;
    }

    private static void reconcileLegacyDuplicates(
            ServerPlayerEntity player,
            List<EquippedRing> legacyRings,
            @Nullable AcornRingLocation preferredLocation) {
        AcornRingLocation ownerLocation = chooseLegacyOwner(
                legacyRings.stream().map(EquippedRing::location).toList(),
                preferredLocation);
        EquippedRing owner = legacyRings.stream()
                .filter(ring -> ring.location() == ownerLocation)
                .findFirst()
                .orElse(legacyRings.getFirst());
        assignIdentity(owner);

        for (EquippedRing ring : legacyRings) {
            if (ring == owner) {
                continue;
            }

            ItemStack recoveredStack = ring.stack().copy();
            AcornRingStackIdentity.setId(recoveredStack, UUID.randomUUID());
            AcornRingStackIdentity.setLastLocation(recoveredStack, ring.location());
            ring.clear();
            player.getInventory().offerOrDrop(recoveredStack);
        }
    }

    private static EquippedRing chooseOwner(
            List<EquippedRing> rings,
            @Nullable AcornRingLocation preferredLocation) {
        AcornRingLocation ownerLocation = chooseMarkedOwner(rings, preferredLocation);
        return rings.stream()
                .filter(ring -> ring.location() == ownerLocation)
                .findFirst()
                .orElse(rings.getFirst());
    }

    private static void assignIdentity(EquippedRing ring) {
        AcornRingStackIdentity.ensureId(ring.stack());
        setLocationIfChanged(ring);
    }

    private static void setLocationIfChanged(EquippedRing ring) {
        if (AcornRingStackIdentity.getLastLocation(ring.stack()) != ring.location()) {
            AcornRingStackIdentity.setLastLocation(ring.stack(), ring.location());
        }
    }

    private AcornRingLifecycleUtil() {}
}
