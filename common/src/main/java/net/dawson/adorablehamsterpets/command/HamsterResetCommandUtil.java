package net.dawson.adorablehamsterpets.command;

import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class HamsterResetCommandUtil {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ──────────────────────────────────────────────────────────────────────────────*/

    public static int reset(ServerCommandSource source, Collection<? extends Entity> targets) {
        // --- 1. Resolve and Reset Targets ---
        List<? extends Entity> resolved = resolveTargets(source, targets);
        int reset = 0;
        int invalid = 0;
        int failed = 0;
        for (Entity entity : resolved) {
            if (!(entity instanceof HamsterEntity hamster)) {
                invalid++;
            } else if (resetToWildState(source.getWorld(), hamster)) {
                reset++;
            } else {
                failed++;
            }
        }

        // --- 2. Report Result ---
        int finalReset = reset;
        int finalInvalid = invalid;
        int finalFailed = failed;
        source.sendFeedback(() -> Text.literal("Hamsters reset: " + finalReset
                + "; invalid: " + finalInvalid
                + "; failed: " + finalFailed), true);
        return reset;
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────────*/

    private static boolean resetToWildState(ServerWorld world, HamsterEntity hamster) {
        HamsterGenome preservedGenome = hamster.getGenome();
        int preservedPersonality = hamster.getDataTracker().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
        DefaultedList<ItemStack> preservedCheekLoot = DefaultedList.ofSize(
                HamsterInventoryUtil.CHEEK_POUCH_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < HamsterInventoryUtil.CHEEK_POUCH_SIZE; slot++) {
            preservedCheekLoot.set(slot, hamster.getItems().get(slot).copy());
        }

        HamsterEntity replacement = ModEntities.HAMSTER.get().create(world);
        if (replacement == null) return false;

        replacement.refreshPositionAndAngles(
                hamster.getX(),
                hamster.getY(),
                hamster.getZ(),
                hamster.getYaw(),
                hamster.getPitch());
        replacement.initialize(
                world,
                world.getLocalDifficulty(replacement.getBlockPos()),
                SpawnReason.COMMAND,
                null,
                null);
        replacement.setGenome(preservedGenome);
        replacement.getDataTracker().set(
                HamsterEntity.ANIMATION_PERSONALITY_ID, preservedPersonality);
        for (int slot = 0; slot < HamsterInventoryUtil.CHEEK_POUCH_SIZE; slot++) {
            replacement.getItems().set(slot, preservedCheekLoot.get(slot));
        }
        HamsterInventoryUtil.updateCheekStates(replacement);
        replacement.setHeadYaw(hamster.getHeadYaw());
        replacement.setBodyYaw(hamster.bodyYaw);

        if (!world.spawnEntity(replacement)) return false;
        hamster.discard();
        return true;
    }

    private static List<? extends Entity> resolveTargets(
            ServerCommandSource source, Collection<? extends Entity> targets) {
        if (!targets.isEmpty()) return List.copyOf(targets);
        // No argument selects nearest live hamster
        return source.getWorld().getEntitiesByClass(
                        HamsterEntity.class,
                        new Box(source.getPosition(), source.getPosition()).expand(16.0D),
                        Entity::isAlive).stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(source.getPosition())))
                .map(List::of)
                .orElseGet(List::of);
    }

    /* ───────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ───────────────────────────────────────────────────────────────────────────────*/

    private HamsterResetCommandUtil() {}
}
