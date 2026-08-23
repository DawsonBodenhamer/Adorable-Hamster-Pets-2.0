package net.dawson.adorablehamsterpets.command;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.RedstoneFeverUtil;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class RedstoneFeverCommandUtil {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ──────────────────────────────────────────────────────────────────────────────*/

    public static int apply(ServerCommandSource source, Collection<? extends Entity> targets) {
        // --- 1. Resolve and Classify Targets ---
        List<? extends Entity> resolved = resolveTargets(source, targets);
        int applied = 0;
        int alreadyFevered = 0;
        int rejected = 0;
        int invalid = 0;
        for (Entity entity : resolved) {
            if (!(entity instanceof HamsterEntity hamster)) {
                invalid++;
            } else if (hamster.hasRedstoneFever()) {
                alreadyFevered++;
            } else if (hamster.isTamed()) {
                rejected++;
            } else if (RedstoneFeverUtil.applyFever(hamster, true)) {
                RedstoneFeverUtil.spawnRedstoneParticles(hamster, 20, 0.18F);
                applied++;
            }
        }

        // --- 2. Report Result ---
        int finalApplied = applied;
        int finalAlreadyFevered = alreadyFevered;
        int finalRejected = rejected;
        int finalInvalid = invalid;
        source.sendFeedback(() -> Text.literal("Redstone Fever applied: " + finalApplied
                + "; already fevered: " + finalAlreadyFevered
                + "; tamed/rejected: " + finalRejected
                + "; invalid: " + finalInvalid), true);
        return applied;
    }

    public static int cure(ServerCommandSource source, Collection<? extends Entity> targets) {
        // --- 1. Resolve and Classify Targets ---
        List<? extends Entity> resolved = resolveTargets(source, targets);
        int cured = 0;
        int nonFevered = 0;
        int invalid = 0;
        for (Entity entity : resolved) {
            if (!(entity instanceof HamsterEntity hamster)) {
                invalid++;
            } else if (!hamster.hasRedstoneFever()) {
                nonFevered++;
            } else {
                RedstoneFeverUtil.cureAdministratively(hamster);
                cured++;
            }
        }

        // --- 2. Report Result ---
        int finalCured = cured;
        int finalNonFevered = nonFevered;
        int finalInvalid = invalid;
        source.sendFeedback(() -> Text.literal("Redstone Fever cured: " + finalCured
                + "; not fevered: " + finalNonFevered
                + "; invalid: " + finalInvalid), true);
        return cured;
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ──────────────────────────────────────────────────────────────────────────────*/

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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ───────────────────────────────────────────────────────────────────────────────*/

    private RedstoneFeverCommandUtil() {}
}
