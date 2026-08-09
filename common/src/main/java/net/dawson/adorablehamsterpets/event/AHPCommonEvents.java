package net.dawson.adorablehamsterpets.event;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.event.api.v2.OnUpdateServerListener;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.custom.HamsterBedBlock;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.command.HamsterSpawnCommandUtil;
import net.dawson.adorablehamsterpets.config.ConfigDataCache;
import net.dawson.adorablehamsterpets.entity.custom.HamsterAbstractHiddenEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterBlockHiderEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.mixin.accessor.SlotAccessor;
import net.dawson.adorablehamsterpets.util.AcornRingContractUtil;
import net.dawson.adorablehamsterpets.util.AcornRingEquipmentLifecycle;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.dawson.adorablehamsterpets.util.PetOwnershipUtil;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Central handler for common, cross-loader events.
 */
public class AHPCommonEvents {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Registration
     * ────────────────────────────────────────────────────────────────────────────*/

    public static void init() {
        PlayerEvent.OPEN_MENU.register(AHPCommonEvents::onOpenMenu);
        EntityEvent.LIVING_HURT.register(AHPCommonEvents::onLivingHurt);
        InteractionEvent.RIGHT_CLICK_BLOCK.register(AHPCommonEvents::onRightClickBlock);
        InteractionEvent.LEFT_CLICK_BLOCK.register(AHPCommonEvents::onLeftClickBlock);

        // Catch block breaks
        BlockEvent.BREAK.register((world, pos, state, player, xp) -> {
            if (!world.isClient()) {
                HamsterAbstractHiddenEntity occupant = HamsterAbstractHiddenEntity.getOccupant(world, pos);
                if (occupant instanceof HamsterBlockHiderEntity hider && hider.isOwnedBy(player)) {
                    hider.finishHiding(true, player);
                    return EventResult.interruptFalse(); // Cancel the break, "find" hamster
                }
            }
            return EventResult.pass();
        });

        InteractionEvent.RIGHT_CLICK_ITEM.register(AHPCommonEvents::onRightClickItem);
        TickEvent.SERVER_POST.register(HamsterSpawnCommandUtil::onServerTick);
        TickEvent.SERVER_POST.register(AcornRingEquipmentLifecycle::onServerTick);

        // Trigger the genetics report on headless servers
        LifecycleEvent.SERVER_STARTED.register(server -> {
            HamsterPaletteManager.triggerInitialReport();
        });

        // Config reload listener
        ConfigApiJava.event().onUpdateServer((OnUpdateServerListener) (id, config, player) -> {
            if (id.getNamespace().equals(AdorableHamsterPets.MOD_ID)) {
                // Reparse cached tags and rules if any configs change
                ConfigDataCache.parseConfig();
                ModEntitySpawns.parseConfig();
                ModWorldGeneration.parseConfig();
                AdorableHamsterPets.LOGGER.info("Reloaded Adorable Hamster Pets config caches on server.");
            }
        });
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Event Handlers / Callbacks
     * ────────────────────────────────────────────────────────────────────────────*/

    private static EventResult onRightClickBlock(PlayerEntity player, Hand hand, BlockPos pos, Direction face) {
        World world = player.getWorld();
        BlockState state = world.getBlockState(pos);

        // --- Hide & Seek Intercept ---
        if (!world.isClient()) {
            HamsterAbstractHiddenEntity occupant = HamsterAbstractHiddenEntity.getOccupant(world, pos);
            if (occupant instanceof HamsterBlockHiderEntity hider && hider.isOwnedBy(player)) {
                hider.finishHiding(true, player);
                player.swingHand(hand, true);
                return EventResult.interruptTrue();
            }
            // Allow non-owners normal interaction
        }

        // --- Lectern Intercept ---
        // Intercept the read action and route it through the Patchouli API
        if (state.isOf(Blocks.LECTERN) && state.get(LecternBlock.HAS_BOOK)) {
            // Let sneaking players take book out normally
            if (!player.isSneaking()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof LecternBlockEntity lectern) {
                    ItemStack bookStack = lectern.getBook();

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

        // --- Hamster Bed Unlink ---
        ItemStack stack = player.getStackInHand(hand);

        // Only care about specific unlink combination: sneaking + holding repellent
        if (player.isSneaking() && ConfigDataCache.isBedAvoidanceFood(stack)) {
            if (state.getBlock() instanceof HamsterBedBlock) {
                if (!world.isClient()) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof HamsterBedBlockEntity bedEntity) {
                        bedEntity.unlinkHamster(player);
                    }
                }
                // Stop vanilla eating
                return EventResult.interruptTrue();
            }
        }

        // --- Precision Tree Heist ---
        if (ConfigDataCache.isLureItem(stack) && TreeHeistUtil.isValidHeistStartBlock(state)) {
            if (!world.isClient() && player instanceof PlayerEntityAccessor accessor) {
                if (accessor.hasAnyShoulderHamster()) {
                    accessor.adorablehamsterpets$startPrecisionTreeHeist(pos);
                    return EventResult.interruptTrue();
                }
            } else if (world.isClient() && ((PlayerEntityAccessor) player).hasAnyShoulderHamster()) {
                // Prevent placing item
                return EventResult.interruptTrue();
            }
        }

        // --- Sapling to Dead Bush Conversion ---
        if (stack.isOf(Items.SHEARS) && state.isIn(BlockTags.SAPLINGS)) {
            if (!world.isClient()) {
                world.setBlockState(pos, Blocks.DEAD_BUSH.getDefaultState(), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0f, 1.0f);

                if (player instanceof ServerPlayerEntity serverPlayer && !serverPlayer.getAbilities().creativeMode) {
                    // 1.20.1: lambda consumer callback
                    stack.damage(1, serverPlayer, p -> p.sendToolBreakStatus(hand));
                }

                ParticleEffectsUtil.spawnParticles(
                        world,
                        Vec3d.ofCenter(pos),
                        new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                        15,
                        new Vec3d(0.2, 0.2, 0.2),
                        0.05
                );
            }
            // Stop further interaction
            return EventResult.interruptTrue();
        }

        return EventResult.pass();
    }

    private static EventResult onLeftClickBlock(PlayerEntity player, Hand hand, BlockPos pos, Direction face) {
        World world = player.getWorld();

        // --- Hide & Seek Intercept ---
        if (!world.isClient()) {
            HamsterAbstractHiddenEntity occupant = HamsterAbstractHiddenEntity.getOccupant(world, pos);
            if (occupant instanceof HamsterBlockHiderEntity hider && hider.isOwnedBy(player)) {
                hider.finishHiding(true, player);
                player.swingHand(hand, true);
                return EventResult.interruptTrue();
            }
            // Allow non-owners to break block normally
        }

        return EventResult.pass();
    }

    private static CompoundEventResult<ItemStack> onRightClickItem(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // --- Precision Tree Heist Dynamic Exit ---
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
                // Client side prediction check
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

        // Use a set to avoid scanning the same inventory multiple times
        Set<Inventory> inventories = new HashSet<>();
        for (Slot slot : menu.slots) {
            // Use the Mixin Accessor to get the inventory object.
            // This works on both Fabric (inventory) and Forge (container) thanks to the remapper.
            Inventory inv = ((SlotAccessor) slot).adorablehamsterpets$getInventory();
            if (inv != null) {
                inventories.add(inv);
            }
        }

        // Run the upgrade logic on each unique inventory found
        for (Inventory inv : inventories) {
            AdorableHamsterPets.replaceOldBooksInInventory(inv);
        }
    }

    /**
     * An event listener that fires just before a living entity takes damage.
     * Applies same-owner hamster exclusions, Acorn Ring player restraint, and the
     * last-resort Acorn Ring contract guard before damage is dealt.
     *
     * @param victim The living entity about to be hurt.
     * @param source The source of the damage.
     * @param amount The amount of damage.
     * @return {@link EventResult#interruptFalse()} to cancel the damage, or
     * {@link EventResult#pass()} to allow it.
     */
    private static EventResult onLivingHurt(LivingEntity victim, DamageSource source, float amount) {
        if (victim.getWorld().isClient()) {
            return EventResult.pass();
        }

        Entity direct = source.getSource();
        Entity attacker = source.getAttacker();

        // Ring restraint covers direct normal attacks only, not projectiles or hazards.
        if (attacker instanceof PlayerEntity player
                && direct == player
                && AcornRingContractUtil.blocksDirectPlayerAttack(player, victim)) {
            return EventResult.interruptFalse();
        }

        HamsterEntity hamster = null;
        if (direct instanceof HamsterEntity h && h.isTamed()) {
            hamster = h;
        } else if (attacker instanceof HamsterEntity h && h.isTamed()) {
            hamster = h;
        }

        // Preserve the existing same-owner friendly-fire exclusions involving hamsters.
        if (hamster != null) {
            UUID hamsterOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(hamster);
            UUID victimOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(victim);
            if (hamsterOwnerUuid != null && hamsterOwnerUuid.equals(victimOwnerUuid)) {
                return EventResult.interruptFalse();
            }
        }

        if (victim instanceof HamsterEntity victimHamster && victimHamster.isTamed()) {
            UUID victimOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(victimHamster);
            UUID attackerOwnerUuid = attacker instanceof LivingEntity livingAttacker
                    ? PetOwnershipUtil.resolveOwnerUuid(livingAttacker)
                    : null;
            if (victimOwnerUuid != null && victimOwnerUuid.equals(attackerOwnerUuid)) {
                return EventResult.interruptFalse();
            }
        }

        // Last-resort guard for owned-pet AI and projectiles that bypass normal target state.
        LivingEntity responsiblePet = AcornRingContractUtil.responsiblePet(attacker);
        if (responsiblePet != null && AcornRingContractUtil.protects(responsiblePet, victim)) {
            return EventResult.interruptFalse();
        }

        return EventResult.pass();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static void popOutHiddenHamster(ServerWorld world, BlockPos pos, PlayerEntity player) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof HamsterBlockHiderEntity hider) {
                if (hider.getAnchorPos() != null && hider.getAnchorPos().equals(pos)) {
                    if (hider.isOwnedBy(player)) {
                        hider.finishHiding(true, player);
                        return;
                    }
                }
            }
        }
    }

}
