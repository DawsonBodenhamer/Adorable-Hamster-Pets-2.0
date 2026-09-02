package net.dawson.adorablehamsterpets.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Owns Acorn Ring equipment, contract, identity, and cross-API lifecycle behavior.
 */
public final class AcornRingUtil {

    private static final Set<UUID> DEFERRED_PLAYERS = new HashSet<>();
    private static final Map<UUID, Set<UUID>> DEFERRED_REMOVALS = new HashMap<>();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration and Setup
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void init() {
        registerPlatformCallbacks();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Equipment and Contract Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static boolean isEquipped(Player player) {
        return player.getOffhandItem().is(ModItems.ACORN_RING.get())
                || isEquippedInOptionalSlot(player);
    }

    public static boolean hasMutualContract(Player first, Player second) {
        return !first.getUUID().equals(second.getUUID())
                && hasMutualEquipment(isEquipped(first), isEquipped(second));
    }

    static boolean hasMutualEquipment(boolean firstEquipped, boolean secondEquipped) {
        return firstEquipped && secondEquipped;
    }

    public static boolean hasSupportedOptionalEquipment(
            boolean firstApiAvailable,
            boolean firstApiEquipped,
            boolean secondApiAvailable,
            boolean secondApiEquipped) {
        return (firstApiAvailable && firstApiEquipped)
                || (secondApiAvailable && secondApiEquipped);
    }

    public static boolean isSupportedTrinketsSlot(String group, String slot) {
        return "hand".equals(group) && "ring".equals(slot);
    }

    public static boolean protects(LivingEntity attackingPet, LivingEntity target) {
        if (!(attackingPet.level() instanceof ServerLevel serverWorld)
                || !isEligiblePet(attackingPet)) {
            return false;
        }

        UUID attackingOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(attackingPet);
        UUID targetOwnerUuid = PetOwnershipUtil.resolveTargetOwnerUuid(target);
        return protects(serverWorld, attackingOwnerUuid, targetOwnerUuid);
    }

    public static boolean isEligiblePet(LivingEntity entity) {
        return isEligiblePet(
                Configs.AHP_MAIN.acornRingOnlyProtectsHamsters,
                isHamster(entity),
                PetOwnershipUtil.resolveOwnerUuid(entity) != null);
    }

    static boolean isEligiblePet(boolean onlyHamsters, boolean hamster, boolean hasOwner) {
        return hasOwner && (!onlyHamsters || hamster);
    }

    /** Returns whether an equipped ring prevents its wearer from directly attacking this pet. */
    public static boolean blocksDirectPlayerAttack(Player attacker, LivingEntity target) {
        UUID targetOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(target);
        return blocksDirectPlayerAttack(
                Configs.AHP_MAIN.acornRingPreventsDamageToOwnPets,
                Configs.AHP_MAIN.acornRingPreventsDamageToOtherPets,
                attacker.getUUID(),
                targetOwnerUuid,
                isEquipped(attacker),
                isEquippedOnlineOwner(attacker, targetOwnerUuid));
    }

    static boolean blocksDirectPlayerAttack(
            boolean preventOwnPetDamage,
            boolean preventOtherPetDamage,
            UUID attackerUuid,
            @Nullable UUID targetOwnerUuid,
            boolean attackerEquipped,
            boolean targetOwnerEquipped) {
        if (!attackerEquipped || targetOwnerUuid == null) {
            return false;
        }
        return attackerUuid.equals(targetOwnerUuid)
                ? preventOwnPetDamage
                : preventOtherPetDamage && targetOwnerEquipped;
    }

    @Nullable
    public static LivingEntity responsiblePet(@Nullable Entity attacker) {
        if (!(attacker instanceof LivingEntity living) || attacker instanceof Player) {
            return null;
        }
        return isEligiblePet(living) ? living : null;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Stack Identity Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static UUID getId(ItemStack stack) {
        return getIdPlatform(stack);
    }

    public static Location getLastLocation(ItemStack stack) {
        String serializedName = getLastLocationPlatform(stack);
        return serializedName == null ? null : Location.fromSerializedName(serializedName);
    }

    public static UUID ensureId(ItemStack stack) {
        UUID id = getId(stack);
        if (id == null) {
            id = UUID.randomUUID();
            setId(stack, id);
        }
        return id;
    }

    public static void setId(ItemStack stack, UUID id) {
        setIdPlatform(stack, id);
    }

    public static void setLastLocation(ItemStack stack, Location location) {
        setLastLocationPlatform(stack, location == null ? "" : location.serializedName());
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle Hooks and Reconciliation
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void defer(ServerPlayer player, @Nullable ItemStack removedStack) {
        if (player.level().isClientSide()) {
            return;
        }

        UUID playerId = player.getUUID();
        DEFERRED_PLAYERS.add(playerId);
        if (removedStack != null) {
            UUID removedIdentity = getId(removedStack);
            if (removedIdentity != null) {
                DEFERRED_REMOVALS.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(removedIdentity);
            }
        }
    }

    public static void reconcileImmediately(
            ServerPlayer player,
            Location preferredLocation,
            @Nullable ItemStack currentStack) {
        UUID playerId = player.getUUID();
        if (currentStack != null) {
            UUID currentIdentity = getId(currentStack);
            if (currentIdentity != null) {
                Set<UUID> removals = DEFERRED_REMOVALS.get(playerId);
                if (removals != null) {
                    removals.remove(currentIdentity);
                    if (removals.isEmpty()) {
                        DEFERRED_REMOVALS.remove(playerId);
                    }
                }
            }
        }

        DEFERRED_PLAYERS.remove(playerId);
        if (!reconcilePlatform(player, preferredLocation, Set.of())) {
            DEFERRED_PLAYERS.add(playerId);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            Set<UUID> removedIdentities = DEFERRED_REMOVALS.getOrDefault(playerId, Set.of());
            if (reconcilePlatform(player, null, removedIdentities)) {
                DEFERRED_PLAYERS.remove(playerId);
                DEFERRED_REMOVALS.remove(playerId);
            }
        }

        Set<UUID> activePlayers = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toSet());
        DEFERRED_PLAYERS.removeIf(playerId -> !activePlayers.contains(playerId));
        DEFERRED_REMOVALS.keySet().removeIf(playerId -> !activePlayers.contains(playerId));
    }

    public static void reconcile(
            ServerPlayer player,
            List<EquippedRing> equippedRings,
            Set<UUID> removedIdentities,
            @Nullable Location preferredLocation) {
        if (equippedRings.isEmpty()) {
            return;
        }

        removeIdentitiesBeingUnequipped(player, equippedRings, removedIdentities);

        List<EquippedRing> legacyRings = equippedRings.stream()
                .filter(ring -> getId(ring.stack()) == null)
                .toList();
        if (legacyRings.size() > 1) {
            reconcileLegacyDuplicates(player, legacyRings, preferredLocation);
        } else if (legacyRings.size() == 1) {
            assignIdentity(legacyRings.getFirst());
        }

        Map<UUID, List<EquippedRing>> ringsByIdentity = new HashMap<>();
        for (EquippedRing ring : equippedRings) {
            UUID id = getId(ring.stack());
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Policy Decisions
     * ────────────────────────────────────────────────────────────────────────────*/

    public static List<Resolution> resolveAll(
            List<RingCandidate> candidates,
            @Nullable Location preferredLocation) {
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
            RingCandidate owner = choosePolicyOwner(legacy, preferredLocation);
            Set<Location> staleLocations = legacy.stream()
                    .filter(candidate -> candidate != owner)
                    .map(RingCandidate::currentLocation)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            resolutions.add(new Resolution(
                    null,
                    owner.currentLocation(),
                    staleLocations,
                    true,
                    legacy.size() > 1));
        }

        for (Map.Entry<UUID, List<RingCandidate>> entry : identified.entrySet()) {
            RingCandidate owner = choosePolicyOwner(entry.getValue(), preferredLocation);
            Set<Location> staleLocations = entry.getValue().stream()
                    .filter(candidate -> candidate != owner)
                    .map(RingCandidate::currentLocation)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            resolutions.add(new Resolution(
                    entry.getKey(),
                    owner.currentLocation(),
                    staleLocations,
                    false,
                    false));
        }

        return resolutions;
    }

    static Location chooseLegacyOwner(
            List<Location> locations,
            @Nullable Location preferredLocation) {
        if (preferredLocation != null && locations.contains(preferredLocation)) {
            return preferredLocation;
        }

        return locations.stream()
                .min(Comparator.comparingInt(Location::legacyPriority))
                .orElse(null);
    }

    static Location chooseMarkedOwner(
            List<EquippedRing> rings,
            @Nullable Location preferredLocation) {
        if (preferredLocation != null) {
            for (EquippedRing ring : rings) {
                if (ring.location() == preferredLocation) {
                    return ring.location();
                }
            }
        }

        for (EquippedRing ring : rings) {
            if (getLastLocation(ring.stack()) == ring.location()) {
                return ring.location();
            }
        }

        return rings.stream()
                .map(EquippedRing::location)
                .min(Comparator.comparingInt(Location::legacyPriority))
                .orElse(null);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static boolean isHamster(LivingEntity entity) {
        var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entityId.getNamespace().equals(AdorableHamsterPets.MOD_ID)
                && entityId.getPath().equals("hamster");
    }

    private static boolean protects(
            ServerLevel world, @Nullable UUID attackingOwnerUuid, @Nullable UUID targetOwnerUuid) {
        if (attackingOwnerUuid == null
                || targetOwnerUuid == null
                || attackingOwnerUuid.equals(targetOwnerUuid)) {
            return false;
        }

        ServerPlayer attackingOwner =
                PetOwnershipUtil.resolveOnlineOwner(world, attackingOwnerUuid);
        ServerPlayer targetOwner = PetOwnershipUtil.resolveOnlineOwner(world, targetOwnerUuid);
        return attackingOwner != null
                && targetOwner != null
                && isContractProtected(
                        attackingOwnerUuid,
                        targetOwnerUuid,
                        isEquipped(attackingOwner),
                        isEquipped(targetOwner));
    }

    static boolean isContractProtected(
            UUID attackingOwnerUuid,
            UUID targetOwnerUuid,
            boolean attackingOwnerEquipped,
            boolean targetOwnerEquipped) {
        return !attackingOwnerUuid.equals(targetOwnerUuid)
                && hasMutualEquipment(attackingOwnerEquipped, targetOwnerEquipped);
    }

    private static boolean isEquippedOnlineOwner(
            Player attacker, @Nullable UUID targetOwnerUuid) {
        if (targetOwnerUuid == null
                || targetOwnerUuid.equals(attacker.getUUID())
                || !(attacker.level() instanceof ServerLevel serverWorld)) {
            return false;
        }
        ServerPlayer targetOwner =
                PetOwnershipUtil.resolveOnlineOwner(serverWorld, targetOwnerUuid);
        return targetOwner != null && isEquipped(targetOwner);
    }

    private static void removeIdentitiesBeingUnequipped(
            ServerPlayer player,
            List<EquippedRing> equippedRings,
            Set<UUID> removedIdentities) {
        if (removedIdentities.isEmpty()) {
            return;
        }

        Set<EquippedRing> staleRings = new HashSet<>();
        for (EquippedRing ring : equippedRings) {
            UUID id = getId(ring.stack());
            if (id != null
                    && removedIdentities.contains(id)
                    && hasOrdinaryInventoryIdentity(player, id)) {
                staleRings.add(ring);
            }
        }

        staleRings.forEach(EquippedRing::clear);
        equippedRings.removeAll(staleRings);
    }

    private static boolean hasOrdinaryInventoryIdentity(ServerPlayer player, UUID identity) {
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (stack.is(ModItems.ACORN_RING.get())
                    && identity.equals(getId(stack))) {
                return true;
            }
        }
        return false;
    }

    private static void reconcileLegacyDuplicates(
            ServerPlayer player,
            List<EquippedRing> legacyRings,
            @Nullable Location preferredLocation) {
        Location ownerLocation = chooseLegacyOwner(
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
            setId(recoveredStack, UUID.randomUUID());
            setLastLocation(recoveredStack, ring.location());
            ring.clear();
            player.getInventory().placeItemBackInInventory(recoveredStack);
        }
    }

    private static EquippedRing chooseOwner(
            List<EquippedRing> rings,
            @Nullable Location preferredLocation) {
        Location ownerLocation = chooseMarkedOwner(rings, preferredLocation);
        return rings.stream()
                .filter(ring -> ring.location() == ownerLocation)
                .findFirst()
                .orElse(rings.getFirst());
    }

    private static RingCandidate choosePolicyOwner(
            List<RingCandidate> candidates,
            @Nullable Location preferredLocation) {
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

    private static void assignIdentity(EquippedRing ring) {
        ensureId(ring.stack());
        setLocationIfChanged(ring);
    }

    private static void setLocationIfChanged(EquippedRing ring) {
        if (getLastLocation(ring.stack()) != ring.location()) {
            setLastLocation(ring.stack(), ring.location());
        }
    }

    @ExpectPlatform
    private static boolean isEquippedInOptionalSlot(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static void registerPlatformCallbacks() {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static boolean reconcilePlatform(
            ServerPlayer player,
            @Nullable Location preferredLocation,
            Set<UUID> removedIdentities) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static UUID getIdPlatform(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static void setIdPlatform(ItemStack stack, UUID id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static String getLastLocationPlatform(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static void setLastLocationPlatform(ItemStack stack, String serializedName) {
        throw new AssertionError();
    }

    private AcornRingUtil() {}

    public record EquippedRing(Location location, ItemStack stack, Consumer<ItemStack> replace) {

        public void clear() {
            replace.accept(ItemStack.EMPTY);
        }
    }

    public record RingCandidate(
            @Nullable UUID identity,
            Location currentLocation,
            @Nullable Location lastLocation) {}

    public record Resolution(
            @Nullable UUID identity,
            Location owner,
            Set<Location> staleLocations,
            boolean assignIdentity,
            boolean recoverLegacyExtras) {}

    /** Supported optional equipment locations that can own an Acorn Ring identity. */
    public enum Location {
        TRINKETS_HAND_RING("trinkets:hand/ring", 0),
        CURIOS_RING("curios:ring", 1),
        ACCESSORIES_RING("accessories:ring", 2);

        private final String serializedName;
        private final int legacyPriority;

        Location(String serializedName, int legacyPriority) {
            this.serializedName = serializedName;
            this.legacyPriority = legacyPriority;
        }

        public String serializedName() {
            return serializedName;
        }

        public int legacyPriority() {
            return legacyPriority;
        }

        public static Location fromSerializedName(String serializedName) {
            return Arrays.stream(values())
                    .filter(location -> location.serializedName.equals(serializedName))
                    .findFirst()
                    .orElse(null);
        }
    }
}
