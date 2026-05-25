package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the scheduling of delayed tasks for Hamster entities, primarily used for
 * stopping triggerable animations after their duration has elapsed.
 */
public class HamsterAnimationScheduler {

    private record ScheduledTask(long executionTick, String debugName, Runnable action) {}

    private final List<ScheduledTask> tasks = new ArrayList<>();

    // --- Animation Duration Map ---
    // Durations are in ticks (Animation Length + small 3 tick buffer)
    private static final Map<String, Integer> ANIMATION_DURATIONS = new HashMap<>();
    static {
        ANIMATION_DURATIONS.put("crash", 32);
        ANIMATION_DURATIONS.put("wakeup_from_ko", 18);
        ANIMATION_DURATIONS.put("standing_headshake", 25);
        ANIMATION_DURATIONS.put("sitting_headshake", 25);
        ANIMATION_DURATIONS.put("moving_headshake", 25);
        ANIMATION_DURATIONS.put("attack", 23);
        ANIMATION_DURATIONS.put("quick_bounce_on_back_legs", 17);
        ANIMATION_DURATIONS.put("sit1", 13);
        ANIMATION_DURATIONS.put("sit2", 13);
        ANIMATION_DURATIONS.put("sit3", 13);
        ANIMATION_DURATIONS.put("standup1", 13);
        ANIMATION_DURATIONS.put("standup2", 13);
        ANIMATION_DURATIONS.put("standup3", 13);
        ANIMATION_DURATIONS.put("wakeup1", 13);
        ANIMATION_DURATIONS.put("wakeup2", 13);
        ANIMATION_DURATIONS.put("wakeup3", 13);
        ANIMATION_DURATIONS.put("anim_hamster_sit_settle_sleep1", 23);
        ANIMATION_DURATIONS.put("anim_hamster_sit_settle_sleep2", 23);
        ANIMATION_DURATIONS.put("anim_hamster_sit_settle_sleep3", 23);
        ANIMATION_DURATIONS.put("anim_hamster_stand_settle_sleep1", 35);
        ANIMATION_DURATIONS.put("anim_hamster_stand_settle_sleep2", 35);
        ANIMATION_DURATIONS.put("anim_hamster_stand_settle_sleep3", 35);
        ANIMATION_DURATIONS.put("anim_hamster_sulk", 63);
        ANIMATION_DURATIONS.put("anim_hamster_pounce_on_item", 23);
        ANIMATION_DURATIONS.put("anim_hamster_quick_bounce", 33);
        ANIMATION_DURATIONS.put("anim_hamster_cheek_unload", 43);
        ANIMATION_DURATIONS.put("anim_hamster_crouch_and_investigate", 63);
        ANIMATION_DURATIONS.put("anim_hamster_receiving_pets", 183);
    }

    /**
     * Processes all scheduled tasks. Should be called from the entity's tick method.
     *
     * @param currentTime The current world time.
     */
    public void tick(long currentTime) {
        // 1. Identify and remove tasks to run
        List<ScheduledTask> tasksToRun = new ArrayList<>();

        tasks.removeIf(task -> {
            if (currentTime >= task.executionTick()) {
                tasksToRun.add(task);
                return true;
            }
            return false;
        });

        // 2. Execute collected tasks
        for (ScheduledTask task : tasksToRun) {
            try {
                task.action().run();
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.error("Error executing scheduled animation task '{}'", task.debugName(), e);
            }
        }
    }

    /**
     * Schedules a "stop" command for a triggerable animation based on its pre-defined duration.
     *
     * @param currentTime    The current world time.
     * @param controllerName The name of the GeckoLib controller.
     * @param animName       The name of the animation to stop.
     * @param entity         The hamster entity instance.
     */
    public void scheduleAnimationStop(long currentTime, String controllerName, String animName, HamsterEntity entity) {
        Integer duration = ANIMATION_DURATIONS.get(animName);
        if (duration != null) {
            long executionTick = currentTime + duration;

            tasks.add(new ScheduledTask(executionTick, animName, () -> {
                entity.stopTriggeredAnim(controllerName, animName);
                AdorableHamsterPets.LOGGER.trace("[HamsterEntity {}] Executed scheduled stop for animation: '{}'", entity.getId(), animName);
            }));

            AdorableHamsterPets.LOGGER.trace("[HamsterEntity {}] Scheduled stop for animation '{}' in {} ticks (at tick {}).", entity.getId(), animName, duration, executionTick);
        } else {
            AdorableHamsterPets.LOGGER.debug("[HamsterEntity {}] No duration found for triggerable animation '{}'. Cancellation not scheduled.", entity.getId(), animName);
        }
    }

    /**
     * Schedules a generic runnable task.
     *
     * @param executionTick The world time tick to run the task.
     * @param debugName     A name for debug/logging purposes.
     * @param action        The action to run.
     */
    public void scheduleTask(long executionTick, String debugName, Runnable action) {
        tasks.add(new ScheduledTask(executionTick, debugName, action));
    }
}