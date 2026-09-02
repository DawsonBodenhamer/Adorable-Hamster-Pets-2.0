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
import net.dawson.adorablehamsterpets.util.AcornRingUtil;
import net.dawson.adorablehamsterpets.util.ParticleEffectsUtil;
import net.dawson.adorablehamsterpets.util.PetOwnershipUtil;
import net.dawson.adorablehamsterpets.util.TreeHeistUtil;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.CaveHamsterSpawner;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
            if (!world.isClientSide()) {
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
        TickEvent.SERVER_POST.register(AcornRingUtil::onServerTick);
        TickEvent.SERVER_POST.register(CaveHamsterSpawner::onServerTick);

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

    private static EventResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        Level world = player.level();
        BlockState state = world.getBlockState(pos);

        // --- Hide & Seek Intercept ---
        if (!world.isClientSide()) {
            HamsterAbstractHiddenEntity occupant = HamsterAbstractHiddenEntity.getOccupant(world, pos);
            if (occupant instanceof HamsterBlockHiderEntity hider && hider.isOwnedBy(player)) {
                hider.finishHiding(true, player);
                player.swing(hand, true);
                return EventResult.interruptTrue();
            }
            // Allow non-owners normal interaction
        }

        // 26.2 port: the lectern used to open the Patchouli guide book here.
        // Patchouli has no 26.2 build, so the interception is dropped and the
        // lectern behaves normally.

        // --- Hamster Bed Unlink ---
        ItemStack stack = player.getItemInHand(hand);

        // Only care about specific unlink combination: sneaking + holding repellent
        if (player.isShiftKeyDown() && ConfigDataCache.isBedAvoidanceFood(stack)) {
            if (state.getBlock() instanceof HamsterBedBlock) {
                if (!world.isClientSide()) {
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
            if (!world.isClientSide() && player instanceof PlayerEntityAccessor accessor) {
                if (accessor.hasAnyShoulderHamster()) {
                    accessor.adorablehamsterpets$startPrecisionTreeHeist(pos);
                    return EventResult.interruptTrue();
                }
            } else if (world.isClientSide() && ((PlayerEntityAccessor) player).hasAnyShoulderHamster()) {
                // Prevent placing item
                return EventResult.interruptTrue();
            }
        }

        // --- Sapling to Dead Bush Conversion ---
        if (stack.is(Items.SHEARS) && state.is(BlockTags.SAPLINGS)) {
            if (!world.isClientSide()) {
                world.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), Block.UPDATE_ALL);
                world.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 1.0f);

                if (player instanceof ServerPlayer serverPlayer && !serverPlayer.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
                }

                ParticleEffectsUtil.spawnParticles(
                        world,
                        Vec3.atCenterOf(pos),
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        15,
                        new Vec3(0.2, 0.2, 0.2),
                        0.05
                );
            }
            // Stop further interaction
            return EventResult.interruptTrue();
        }

        return EventResult.pass();
    }

    private static EventResult onLeftClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        Level world = player.level();

        // --- Hide & Seek Intercept ---
        if (!world.isClientSide()) {
            HamsterAbstractHiddenEntity occupant = HamsterAbstractHiddenEntity.getOccupant(world, pos);
            if (occupant instanceof HamsterBlockHiderEntity hider && hider.isOwnedBy(player)) {
                hider.finishHiding(true, player);
                player.swing(hand, true);
                return EventResult.interruptTrue();
            }
            // Allow non-owners to break block normally
        }

        return EventResult.pass();
    }

    private static CompoundEventResult<ItemStack> onRightClickItem(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // --- Precision Tree Heist Dynamic Exit ---
        if (ConfigDataCache.isLureItem(stack)) {
            Level world = player.level();
            boolean updated = false;

            if (world instanceof ServerLevel serverWorld) {
                for (Entity entity : serverWorld.getAllEntities()) {
                    if (entity instanceof HamsterTreeSearcherEntity searcher && searcher.isOwnedBy(player)) {
                        searcher.setForcedExitYaw(player.getYRot());
                        updated = true;
                    }
                }
                if (updated) {
                    player.displayClientMessage(Component.translatable("message.adorablehamsterpets.precision_tree_heist_exit_direction_set").withStyle(ChatFormatting.AQUA), true);                }
            } else {
                // Client side prediction check
                for (Entity entity : world.getEntitiesOfClass(HamsterTreeSearcherEntity.class, player.getBoundingBox().inflate(64.0), e -> true)) {
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
    private static void onOpenMenu(Player player, AbstractContainerMenu menu) {
        if (player.level().isClientSide()) {
            return;
        }

        // Use a set to avoid scanning the same inventory multiple times
        Set<Container> inventories = new HashSet<>();
        for (Slot slot : menu.slots) {
            Container inv = ((SlotAccessor) slot).adorablehamsterpets$getInventory();
            if (inv != null) {
                inventories.add(inv);
            }
        }

        // Run the upgrade logic on each unique inventory found
        for (Container inv : inventories) {
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
        if (victim.level().isClientSide()) {
            return EventResult.pass();
        }

        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();

        // Ring restraint covers direct normal attacks only, not projectiles or hazards.
        if (attacker instanceof Player player
                && direct == player
				&& AcornRingUtil.blocksDirectPlayerAttack(player, victim)) {
            return EventResult.interruptFalse();
        }

        HamsterEntity hamster = null;
        if (direct instanceof HamsterEntity h && h.isTame()) {
            hamster = h;
        } else if (attacker instanceof HamsterEntity h && h.isTame()) {
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

        if (victim instanceof HamsterEntity victimHamster && victimHamster.isTame()) {
            UUID victimOwnerUuid = PetOwnershipUtil.resolveOwnerUuid(victimHamster);
            UUID attackerOwnerUuid = attacker instanceof LivingEntity livingAttacker
                    ? PetOwnershipUtil.resolveOwnerUuid(livingAttacker)
                    : null;
            if (victimOwnerUuid != null && victimOwnerUuid.equals(attackerOwnerUuid)) {
                return EventResult.interruptFalse();
            }
        }

        // Last-resort guard for owned-pet AI and projectiles that bypass normal target state.
		LivingEntity responsiblePet = AcornRingUtil.responsiblePet(attacker);
		if (responsiblePet != null && AcornRingUtil.protects(responsiblePet, victim)) {
            return EventResult.interruptFalse();
        }

        return EventResult.pass();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static void popOutHiddenHamster(ServerLevel world, BlockPos pos, Player player) {
        for (Entity entity : world.getAllEntities()) {
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
