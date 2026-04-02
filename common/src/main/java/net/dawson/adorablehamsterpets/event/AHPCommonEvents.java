package net.dawson.adorablehamsterpets.event;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.command.HamsterSpawnCommandUtil;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.mixin.accessor.SlotAccessor;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;

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
        InteractionEvent.RIGHT_CLICK_BLOCK.register(AHPCommonEvents::onRightClickBlock);
        InteractionEvent.RIGHT_CLICK_ITEM.register(AHPCommonEvents::onRightClickItem);
        TickEvent.SERVER_POST.register(HamsterSpawnCommandUtil::onServerTick);

        // Trigger the genetics report on headless servers
        LifecycleEvent.SERVER_STARTED.register(server -> {
            HamsterPaletteManager.triggerInitialReport();
        });

        // --- Config Reload Listener ---
        ConfigApiJava.event().onUpdateServer((id, config, player) -> {
            if (id.getNamespace().equals(AdorableHamsterPets.MOD_ID)) {
                // Re-parse cached tags and rules if any configs change
                ConfigDataCache.parseConfig();
                ModEntitySpawns.parseConfig();
                ModWorldGeneration.parseConfig();
                AdorableHamsterPets.LOGGER.info("Reloaded Adorable Hamster Pets config caches on server.");
            }
        });
    }

    /**
     * Intercepts block right-clicks to handle specific mod interactions that vanilla logic
     * might otherwise skip or mishandle.
     */
    private static EventResult onRightClickBlock(PlayerEntity player, Hand hand, BlockPos pos, Direction face) {
        World world = player.getWorld();
        BlockState state = world.getBlockState(pos);

        // --- 1. Lectern Intercept ---
        // Intercept the read action and route it through the Patchouli API
        if (state.isOf(Blocks.LECTERN) && state.get(LecternBlock.HAS_BOOK)) {
            // Let sneaking players take the book out normally via vanilla logic
            if (!player.isSneaking()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof LecternBlockEntity lectern) {
                    ItemStack bookStack = lectern.getBook();

                    // Check if the lectern is holding my guide book
                    if (bookStack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
                        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                            PatchouliAPI.get().openBookGUI(serverPlayer, Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
                        }
                        // Interrupt to avoid vanilla written book UI
                        return EventResult.interruptTrue();
                    }
                }
            }
        }

        // --- 2. Hamster Bed Unlink ---
        ItemStack stack = player.getStackInHand(hand);

        // Only care about specific "Unlink" combination: Sneaking + Holding Repellent
        if (player.isSneaking() && ConfigDataCache.isBedAvoidanceFood(stack)) {
            if (state.getBlock() instanceof HamsterBedBlock) {
                if (!world.isClient()) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof HamsterBedBlockEntity bedEntity) {
                        bedEntity.unlinkHamster(player);
                    }
                }
                // Return interruptTrue to indicate success and stop vanilla processing (eating)
                return EventResult.interruptTrue();
            }
        }

        // --- 3. Precision Tree Heist ---
        if (ConfigDataCache.isLureItem(stack) && state.isOf(Blocks.OAK_LEAVES)) {
            if (!world.isClient() && player instanceof PlayerEntityAccessor accessor) {
                if (accessor.hasAnyShoulderHamster()) {
                    accessor.adorablehamsterpets$startPrecisionTreeHeist(pos);
                    return EventResult.interruptTrue();
                }
            } else if (world.isClient() && ((PlayerEntityAccessor) player).hasAnyShoulderHamster()) {
                // Return success on client to swing hand and prevent placing/using item
                return EventResult.interruptTrue();
            }
        }

        return EventResult.pass();
    }

    /**
     * Intercepts item right-clicks (like clicking in the air) to set the dynamic exit direction
     * for a currently active precision tree heist.
     */
    private static CompoundEventResult<ItemStack> onRightClickItem(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (ConfigDataCache.isLureItem(stack)) {
            World world = player.getWorld();
            boolean updated = false;

            if (world instanceof ServerWorld serverWorld) {
                for (Entity entity : serverWorld.iterateEntities()) {
                    if (entity instanceof HamsterTreeSearcherEntity searcher && searcher.isOwnedBy(player)) {
                        searcher.setForcedExitYaw(player.getYaw());
                        updated = true;
                    }
                }
                if (updated) {
                    player.sendMessage(Text.translatable("message.adorablehamsterpets.precision_tree_heist_exit_direction_set").formatted(Formatting.AQUA), true);                }
            } else {
                // Client-side prediction check
                for (Entity entity : world.getEntitiesByClass(HamsterTreeSearcherEntity.class, player.getBoundingBox().expand(64.0), e -> true)) {
                    if (((HamsterTreeSearcherEntity) entity).isOwnedBy(player)) {
                        updated = true;
                        break;
                    }
                }
            }

            if (updated) {
                // Prevent the player from eating the cheese while configuring the heist
                return CompoundEventResult.interruptTrue(stack);
            }
        }
        return CompoundEventResult.pass();
    }

    /**
     * An event listener that fires whenever a player opens any menu (inventory, chest, etc.).
     * It scans all unique inventories within the menu and upgrades any outdated guide books.
     *
     * @param player The player opening the menu.
     * @param menu   The menu being opened.
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
     * {@link EventResult#pass()} to allow it.
     */
    private static EventResult onLivingHurt(LivingEntity victim, DamageSource source, float amount) {
        // --- 1. Server-side guard ---
        if (victim.getWorld().isClient()) {
            return EventResult.pass();
        }

        // --- 2. Gather the direct and indirect sources of the damage ---
        Entity direct = source.getSource();     // Immediate cause (e.g., projectile / hamster body)
        Entity attacker = source.getAttacker();   // Credited attacker (e.g., the mob that dealt it)

        // --- 3. Debug logging to verify what entities are involved ---
        AdorableHamsterPets.LOGGER.trace("onLivingHurt: victim={} srcType={} attacker={}({}) direct={}({}) amount={}",
                victim.getType().toString(),
                source.getName(),
                attacker, attacker == null ? "null" : attacker.getClass().getSimpleName(),
                direct, direct == null ? "null" : direct.getClass().getSimpleName(),
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
                    victimOwner == null ? "null" : victimOwner.getUuid()
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
            LivingEntity victimOwner = victimHamster.getOwner();
            LivingEntity attackerOwner = (attacker instanceof LivingEntity leAttacker) ? getPetOwner(leAttacker) : null;

            AdorableHamsterPets.LOGGER.trace(
                    "onLivingHurt: symm hamsterOwnerUuid={} attackerOwnerUuid={}",
                    victimOwner == null ? "null" : victimOwner.getUuid(),
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