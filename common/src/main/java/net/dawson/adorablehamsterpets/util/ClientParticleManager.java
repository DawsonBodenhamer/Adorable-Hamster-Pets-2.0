package net.dawson.adorablehamsterpets.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A generic, client-side manager for maintaining continuous particle effects.
 * <p>
 * Problem: {@code Block#randomDisplayTick} fires inconsistently/randomly, making smooth animations (like spinning rings) impossible.
 * Solution: This manager runs on the reliable {@code ClientTickEvent} (20 ticks/sec).
 * <p>
 * Usage:
 * Call {@link #addOrUpdate(BlockPos, String, Consumer, BiPredicate)} inside your block's {@code randomDisplayTick}.
 * This acts as a "heartbeat". If the heartbeat stops (block broken/unloaded), the effect expires automatically.
 * An optional validity check allows immediate cleanup if the target block is broken.
 */
public class ClientParticleManager {

    public static final ClientParticleManager INSTANCE = new ClientParticleManager();

    // Map Key: "x,y,z:effect_id" -> Emitter Entry
    private final Map<String, EmitterEntry> activeEmitters = new ConcurrentHashMap<>();

    private static final int DEFAULT_TIMEOUT_TICKS = 60; // 3 seconds grace period

    private ClientParticleManager() {}

    /**
     * Registers a new effect or refreshes an existing one.
     * Call this from the block's randomDisplayTick method.
     *
     * @param pos            The position of the emitter.
     * @param effectId       Unique ID for this specific effect.
     * @param logic          The logic to run every tick.
     * @param validityCheck  Predicate to check if effect is still valid.
     */
    public void addOrUpdate(BlockPos pos, String effectId, Consumer<Level> logic, BiPredicate<Level, BlockPos> validityCheck) {
        String key = pos.asLong() + ":" + effectId;

        activeEmitters.compute(key, (k, current) -> {
            if (current == null) {
                // Store immutable pos for the check
                return new EmitterEntry(pos.immutable(), logic, validityCheck, DEFAULT_TIMEOUT_TICKS);
            }
            current.timeoutTicks = DEFAULT_TIMEOUT_TICKS; // Reset grace period
            current.logic = logic; // Update logic (allows live config changes)
            current.validityCheck = validityCheck; // Update check
            return current;
        });
    }

    /**
     * Ticks all active emitters. Call this from the client tick event.
     *
     * @param world The client world.
     */
    public void tick(Level world) {
        if (world == null) return;

        Iterator<Map.Entry<String, EmitterEntry>> iterator = activeEmitters.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, EmitterEntry> entry = iterator.next();
            EmitterEntry emitter = entry.getValue();

            // 1. Check explicit validity
            if (emitter.validityCheck != null && !emitter.validityCheck.test(world, emitter.pos)) {
                iterator.remove();
                continue;
            }

            // 2. Run particle logic
            emitter.logic.accept(world);

            // 3. Decrement timeout
            emitter.timeoutTicks--;

            // 4. Remove if expired
            if (emitter.timeoutTicks <= 0) {
                iterator.remove();
            }
        }
    }

    /**
     * Clears all effects. Call on world unload/disconnect.
     */
    public void clear() {
        activeEmitters.clear();
    }

    // --- Inner Helper Class ---
    private static class EmitterEntry {
        final BlockPos pos;
        Consumer<Level> logic;
        BiPredicate<Level, BlockPos> validityCheck;
        int timeoutTicks;

        EmitterEntry(BlockPos pos, Consumer<Level> logic, BiPredicate<Level, BlockPos> validityCheck, int timeoutTicks) {
            this.pos = pos;
            this.logic = logic;
            this.validityCheck = validityCheck;
            this.timeoutTicks = timeoutTicks;
        }
    }
}