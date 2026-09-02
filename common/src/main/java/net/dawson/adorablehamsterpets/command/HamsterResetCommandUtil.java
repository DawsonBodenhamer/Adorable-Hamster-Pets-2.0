package net.dawson.adorablehamsterpets.command;

import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.util.HamsterInventoryUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class HamsterResetCommandUtil {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ──────────────────────────────────────────────────────────────────────────────*/

    public static int reset(CommandSourceStack source, Collection<? extends Entity> targets) {
        // --- 1. Resolve and Reset Targets ---
        List<? extends Entity> resolved = resolveTargets(source, targets);
        int reset = 0;
        int invalid = 0;
        int failed = 0;
        for (Entity entity : resolved) {
            if (!(entity instanceof HamsterEntity hamster)) {
                invalid++;
            } else if (resetToWildState(source.getLevel(), hamster)) {
                reset++;
            } else {
                failed++;
            }
        }

        // --- 2. Report Result ---
        int finalReset = reset;
        int finalInvalid = invalid;
        int finalFailed = failed;
        source.sendSuccess(() -> Component.literal("Hamsters reset: " + finalReset
                + "; invalid: " + finalInvalid
                + "; failed: " + finalFailed), true);
        return reset;
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────────*/

    private static boolean resetToWildState(ServerLevel world, HamsterEntity hamster) {
        HamsterGenome preservedGenome = hamster.getGenome();
        int preservedPersonality = hamster.getEntityData().get(HamsterEntity.ANIMATION_PERSONALITY_ID);
        NonNullList<ItemStack> preservedCheekLoot = NonNullList.withSize(
                HamsterInventoryUtil.CHEEK_POUCH_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < HamsterInventoryUtil.CHEEK_POUCH_SIZE; slot++) {
            preservedCheekLoot.set(slot, hamster.getItems().get(slot).copy());
        }

        HamsterEntity replacement = ModEntities.HAMSTER.get().create(world);
        if (replacement == null) return false;

        replacement.moveTo(
                hamster.getX(),
                hamster.getY(),
                hamster.getZ(),
                hamster.getYRot(),
                hamster.getXRot());
        replacement.finalizeSpawn(
                world,
                world.getCurrentDifficultyAt(replacement.blockPosition()),
                MobSpawnType.COMMAND,
                null);
        replacement.setGenome(preservedGenome);
        replacement.getEntityData().set(
                HamsterEntity.ANIMATION_PERSONALITY_ID, preservedPersonality);
        for (int slot = 0; slot < HamsterInventoryUtil.CHEEK_POUCH_SIZE; slot++) {
            replacement.getItems().set(slot, preservedCheekLoot.get(slot));
        }
        HamsterInventoryUtil.updateCheekStates(replacement);
        replacement.setYHeadRot(hamster.getYHeadRot());
        replacement.setYBodyRot(hamster.yBodyRot);

        if (!world.addFreshEntity(replacement)) return false;
        hamster.discard();
        return true;
    }

    private static List<? extends Entity> resolveTargets(
            CommandSourceStack source, Collection<? extends Entity> targets) {
        if (!targets.isEmpty()) return List.copyOf(targets);
        // No argument selects nearest live hamster
        return source.getLevel().getEntitiesOfClass(
                        HamsterEntity.class,
                        new AABB(source.getPosition(), source.getPosition()).inflate(16.0D),
                        Entity::isAlive).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(source.getPosition())))
                .map(List::of)
                .orElseGet(List::of);
    }

    /* ───────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ───────────────────────────────────────────────────────────────────────────────*/

    private HamsterResetCommandUtil() {}
}
