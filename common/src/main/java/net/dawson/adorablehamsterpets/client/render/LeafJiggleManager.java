package net.dawson.adorablehamsterpets.client.render;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.dawson.adorablehamsterpets.entity.custom.HamsterTreeSearcherEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.Collection;

public class LeafJiggleManager {
    public static final LeafJiggleManager INSTANCE = new LeafJiggleManager();
    public static final int DURATION_TICKS = 20;

    // Maps BlockPos.asLong() -> Jiggle Instance
    private final Long2ObjectOpenHashMap<Jiggle> activeJiggles = new Long2ObjectOpenHashMap<>();

    // Maps EntityID -> Last BlockPos.asLong() (active searchers only)
    private final Int2LongOpenHashMap searcherLastPositions = new Int2LongOpenHashMap();

    private LeafJiggleManager() {}

    public void onSearcherAdded(HamsterTreeSearcherEntity entity) {
        long posLong = entity.getBlockPos().asLong();
        searcherLastPositions.put(entity.getId(), entity.getBlockPos().asLong());

        // --- Ensure the "Hit Pos" leaf jiggles as soon as the heist starts
        if (entity.getWorld() != null) {
            startJiggle(posLong, entity.getWorld().getTime(), mixSeed(entity.getId(), posLong));
        }
    }

    public void clientTick(MinecraftClient client) {
        if (client.world == null) return;
        long now = client.world.getTime();

        // --- 1. Detect Movements ---
        var iterator = searcherLastPositions.int2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int entityId = entry.getIntKey();

            Entity entity = client.world.getEntityById(entityId);
            if (!(entity instanceof HamsterTreeSearcherEntity searcher) || !searcher.isAlive()) {
                // Cleanup if entity is gone/dead but event missed it
                iterator.remove();
                continue;
            }

            long currentPosLong = searcher.getBlockPos().asLong();
            long lastPosLong = entry.getLongValue();

            // If position changed, trigger jiggle at NEW position
            if (currentPosLong != lastPosLong) {
                entry.setValue(currentPosLong);
                startJiggle(currentPosLong, now, mixSeed(entityId, currentPosLong));
            }
        }

        // --- 2. Expire old jiggles ---
        activeJiggles.values().removeIf(j -> (now - j.startTick) > DURATION_TICKS);
    }

    private void startJiggle(long posLong, long now, long seed) {
        // Overwrite if exists (re-jiggle)
        activeJiggles.put(posLong, new Jiggle(now, seed));
    }

    public Collection<Long2ObjectMap.Entry<Jiggle>> getActiveJiggles() {
        return activeJiggles.long2ObjectEntrySet();
    }

    private static long mixSeed(int entityId, long posLong) {
        long x = posLong ^ (entityId * 0x9E3779B97F4A7C15L);
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }

    public record Jiggle(long startTick, long seed) {}
}