package net.dawson.adorablehamsterpets.event;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.SlotAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Central handler for common, cross-loader events.
 */
public class AHPCommonEvents {

    /**
     * Initializes and registers all common event listeners.
     */
    public static void init() {
        PlayerEvent.OPEN_MENU.register(AHPCommonEvents::onOpenMenu);
        EntityEvent.LIVING_HURT.register(AHPCommonEvents::onLivingHurt);
    }

    /**
     * An event listener that fires whenever a player opens any menu (inventory, chest, etc.).
     * It scans all unique inventories within the menu and upgrades any outdated guide books.
     *
     * @param player The player opening the menu.
     * @param menu The menu being opened.
     */
    private static void onOpenMenu(PlayerEntity player, ScreenHandler menu) {
        if (player.getWorld().isClient()) {
            return;
        }

        // Use a Set to avoid scanning the same inventory multiple times
        Set<Inventory> inventories = new HashSet<>();
        for (Slot slot : menu.slots) {
            // Use the Mixin Accessor to get the inventory object.
            // This works on both Fabric (inventory) and NeoForge (container) thanks to the remapper.
            Inventory inv = ((SlotAccessor) slot).adorablehamsterpets$getInventory();
            if (inv != null) {
                inventories.add(inv);
            }
        }

        // Run the upgrade logic on each unique inventory found.
        for (Inventory inv : inventories) {
            AdorableHamsterPets.replaceOldBooksInInventory(inv);
        }
    }

    /**
     * An event listener that fires just before a living entity takes damage.
     * Prevents friendly fire between pets that share the same owner — including our
     * hamsters and vanilla pets (wolves, cats, parrots, horses, etc). Works cross-loader
     * from the common source set by relying on vanilla/Yarn types and simple reflection.
     *
     * @param victim The living entity about to be hurt.
     * @param source The source of the damage.
     * @param amount The amount of damage.
     * @return {@link EventResult#interruptFalse()} to cancel the damage, or
     *         {@link EventResult#pass()} to allow it.
     */
    private static EventResult onLivingHurt(LivingEntity victim, DamageSource source, float amount) {
        // --- 1. Server-side guard ---
        if (victim.getWorld().isClient()) {
            return EventResult.pass();
        }

        // --- 2. Gather the direct and indirect sources of the damage ---
        Entity direct   = source.getSource();     // Immediate cause (e.g., projectile / hamster body)
        Entity attacker = source.getAttacker();   // Credited attacker (e.g., the mob that dealt it)

        // --- 3. Debug logging to verify what entities are involved ---
        AdorableHamsterPets.LOGGER.trace("onLivingHurt: victim={} srcType={} attacker={}({}) direct={}({}) amount={}",
                victim.getType().toString(),
                source.getName(),
                attacker, attacker == null ? "null" : attacker.getClass().getSimpleName(),
                direct,   direct   == null ? "null" : direct.getClass().getSimpleName(),
                amount
        );

        // --- 4. If a tamed hamster is involved as attacker (direct or indirect) ---
        HamsterEntity hamster = null;
        if (direct instanceof HamsterEntity h && h.isTamed()) {
            hamster = h;
        } else if (attacker instanceof HamsterEntity h && h.isTamed()) {
            hamster = h;
        }

        // --- 5. Hamster → pet protection ---
        if (hamster != null) {
            boolean victimIsTameable = victim instanceof TameableEntity;
            AdorableHamsterPets.LOGGER.trace("hamster→pet branch entered: hamsterTamed={} victim instanceof TameableEntity={}",
                    hamster.isTamed(), victimIsTameable);

            // Owner of the hamster (always LivingEntity or null)
            LivingEntity hamsterOwner = hamster.getOwner();

            // Owner of the victim (generic, supports wolves/cats/parrots/horses/mods)
            LivingEntity victimOwner = getPetOwner(victim);

            AdorableHamsterPets.LOGGER.trace(
                    "hamster→pet owners: hamsterOwnerUuid={} victimOwnerUuid={}",
                    hamsterOwner == null ? "null" : hamsterOwner.getUuid(),
                    victimOwner  == null ? "null" : victimOwner.getUuid()
            );

            if (hamsterOwner != null && victimOwner != null) {
                if (sameOwner(hamsterOwner, victimOwner)) {
                    AdorableHamsterPets.LOGGER.trace("hamster→pet: SAME OWNER detected, cancelling damage.");
                    return EventResult.interruptFalse();
                }
            }
        }

        // --- 6. Symmetric protection: pet (any) → hamster ---
        if (victim instanceof HamsterEntity victimHamster && victimHamster.isTamed()) {
            LivingEntity victimOwner   = victimHamster.getOwner();
            LivingEntity attackerOwner = (attacker instanceof LivingEntity leAttacker) ? getPetOwner(leAttacker) : null;

            AdorableHamsterPets.LOGGER.trace(
                    "onLivingHurt: symm hamsterOwnerUuid={} attackerOwnerUuid={}",
                    victimOwner   == null ? "null" : victimOwner.getUuid(),
                    attackerOwner == null ? "null" : attackerOwner.getUuid()
            );

            if (victimOwner != null && attackerOwner != null) {
                if (sameOwner(victimOwner, attackerOwner)) {
                    return EventResult.interruptFalse();
                }
            }
        }

        // --- 7. For all other cases, allow normal damage processing ---
        return EventResult.pass();
    }

    @Nullable
    private static LivingEntity getPetOwner(LivingEntity entity) {
        // --- A. Direct vanilla APIs ---
        // TameableEntity (wolves, cats, parrots, etc.)
        if (entity instanceof TameableEntity tame) {
            return tame.getOwner();
        }

        // AbstractHorseEntity stores only the owner's UUID; resolve it into an entity.
        if (entity instanceof AbstractHorseEntity horse) {
            UUID ownerId = horse.getOwnerUuid();
            if (ownerId != null) {
                return lookupLivingByUuid(entity.getWorld(), ownerId);
            }
        }

        // Some entities (esp. projectiles/custom) may implement the "Ownable" marker that returns an Entity.
        // Only accept it if it is actually a LivingEntity.
        // NOTE: Wolves do NOT implement this interface; this branch is just a safe bonus path.
        if (entity instanceof net.minecraft.entity.Ownable ownable) {
            Entity e = ownable.getOwner();
            return (e instanceof LivingEntity le) ? le : null;   // <-- fixes the “Entity → LivingEntity” type mismatch
        }

        // --- B. Reflection fallback for common mod patterns ---
        // Try a no-arg getOwner() that returns LivingEntity or Entity.
        try {
            Method m = entity.getClass().getMethod("getOwner");
            Object ret = m.invoke(entity);
            if (ret instanceof LivingEntity le) return le;
            if (ret instanceof Entity e) return (e instanceof LivingEntity le) ? le : null;
        } catch (Throwable ignored) {
        }

        // Try getOwnerUuid() / getOwnerUUID() and resolve.
        UUID id = tryGetUuid(entity, "getOwnerUuid");
        if (id == null) id = tryGetUuid(entity, "getOwnerUUID");
        if (id != null) {
            return lookupLivingByUuid(entity.getWorld(), id);
        }

        return null;
    }

    // Resolve a UUID-returning method by name, if present.
    @Nullable
    private static UUID tryGetUuid(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object ret = m.invoke(target);
            return (ret instanceof UUID u) ? u : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // Lookup a LivingEntity by UUID in the current world (players first, then any entity).
    @Nullable
    private static LivingEntity lookupLivingByUuid(World world, UUID id) {
        if (!(world instanceof ServerWorld server)) return null;
        // Players
        Entity player = server.getPlayerByUuid(id);
        if (player instanceof LivingEntity le) return le;
        // Any other entity with that UUID
        Entity any = server.getEntity(id);
        return (any instanceof LivingEntity le) ? le : null;
    }

    // Strict "same owner" check by identity OR UUID match to be resilient to different instances.
    private static boolean sameOwner(LivingEntity a, LivingEntity b) {
        return a == b || a.getUuid().equals(b.getUuid());
    }
}