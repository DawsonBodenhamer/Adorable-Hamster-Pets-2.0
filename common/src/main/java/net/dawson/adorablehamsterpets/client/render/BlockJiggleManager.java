package net.dawson.adorablehamsterpets.client.render;

import it.unimi.dsi.fastutil.ints.Int2LongMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.dawson.adorablehamsterpets.entity.custom.HamsterAbstractHiddenEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import java.util.Collection;

public class BlockJiggleManager {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final BlockJiggleManager INSTANCE = new BlockJiggleManager();
    public static final JiggleConfig TREE_HEIST_JIGGLE = new JiggleConfig(0.05f, 4.0f, 6.0f, 20);
    public static final JiggleConfig HIDE_AND_SEEK_JIGGLE = new JiggleConfig(0.02f, 2.0f, 4.0f, 10);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Instance Fields
     * ────────────────────────────────────────────────────────────────────────────*/

    // Maps BlockPos.asLong() -> Jiggle Instance
    private final Long2ObjectOpenHashMap<Jiggle> activeJiggles = new Long2ObjectOpenHashMap<>();

    // Maps EntityID -> Last BlockPos.asLong() (active searchers only)
    private final Int2LongOpenHashMap hiddenEntityLastPositions = new Int2LongOpenHashMap();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    private BlockJiggleManager() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ────────────────────────────────────────────────────────────────────────────*/

    public void onHiddenEntityAdded(HamsterAbstractHiddenEntity entity) {
        long posLong = entity.blockPosition().asLong();
        this.hiddenEntityLastPositions.put(entity.getId(), posLong);

        // Ensure the hit pos block jiggles as soon as event starts
        if (entity.level() != null) {
            JiggleConfig config = entity instanceof HamsterTreeSearcherEntity ? TREE_HEIST_JIGGLE : HIDE_AND_SEEK_JIGGLE;
            startJiggle(posLong, entity.level().getGameTime(), mixSeed(entity.getId(), posLong), config);
        }
    }

    public void clientTick(Minecraft client) {
        if (client.level == null) return;
        long now = client.level.getGameTime();

        // --- Detect Movements ---
        var iterator = this.hiddenEntityLastPositions.int2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int entityId = entry.getIntKey();

            Entity entity = client.level.getEntity(entityId);
            if (!(entity instanceof HamsterAbstractHiddenEntity hider) || !hider.isAlive()) {
                // Cleanup if entity is gone but event missed it
                iterator.remove();
                continue;
            }

            long currentPosLong = hider.blockPosition().asLong();
            long lastPosLong = entry.getLongValue();

            // Trigger jiggle at new position
            if (currentPosLong != lastPosLong) {
                entry.setValue(currentPosLong);

                // Fetch config based on entity type
                JiggleConfig config = hider instanceof HamsterTreeSearcherEntity ? TREE_HEIST_JIGGLE : HIDE_AND_SEEK_JIGGLE;
                startJiggle(currentPosLong, now, mixSeed(entityId, currentPosLong), config);
            }
        }

        // Expire old jiggles
        this.activeJiggles.values().removeIf(j -> (now - j.startTick) > j.config.duration());
    }

    public void startJiggle(long posLong, long now, long seed, JiggleConfig config) {
        this.activeJiggles.put(posLong, new Jiggle(now, seed, config));
    }

    public boolean hasJiggle(long posLong) {
        return this.activeJiggles.containsKey(posLong);
    }

    public Jiggle getJiggle(long posLong) {
        return this.activeJiggles.get(posLong);
    }

    public Collection<Long2ObjectMap.Entry<Jiggle>> getActiveJiggles() {
        return this.activeJiggles.long2ObjectEntrySet();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers & Inner Classes
     * ────────────────────────────────────────────────────────────────────────────*/

    private static long mixSeed(int entityId, long posLong) {
        long x = posLong ^ (entityId * 0x9E3779B97F4A7C15L);
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }

    public record Jiggle(long startTick, long seed, JiggleConfig config) {}
    public record JiggleConfig(float amplitude, float rotationAmplitude, float oscillationCycles, int duration) {}
}